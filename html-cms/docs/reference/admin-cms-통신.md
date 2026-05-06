# Spider Admin ↔ CMS 통신 상세

> 작성일: 2026-05-06
> 대상: Spider Admin(Spring Boot)과 CMS(Next.js) 간의 서버 간 통신 구조
> 시스템 전체 구조는 [`시스템-아키텍처.md`](./시스템-아키텍처.md), 기능별 흐름은 [`기술-프로세스.md`](./기술-프로세스.md) 참고

---

## 한 줄 요약

**Spider Admin은 자체 비즈니스 로직(인증·승인·이력)만 처리하고, 파일·DB 작업은 CMS REST API에 위임한다. 통신은 HTTP + JSON/multipart, 인증은 공유 시크릿 `x-deploy-token` 한 가지로 통일.**

---

## 통신 방식 개요

| 항목 | 값 |
|---|---|
| 프로토콜 | HTTP (운영은 nginx 경유) |
| 포맷 | `application/json` 또는 `multipart/form-data` |
| 인증 | `x-deploy-token` 헤더 (공유 시크릿) |
| 클라이언트 | Spring `RestClient` (Spider Admin) |
| 서버 | Next.js Route Handler (CMS) |
| 호출 방향 | **단방향: Spider Admin → CMS** |

**CMS는 Spider Admin을 호출하지 않는다.** 단, CMS의 사용자 인증 검증을 위해 `fetchJavaAdminApi('/api/auth/me')`로 Spider Admin의 사용자 조회 API를 호출하는 경우는 있다 (브라우저 세션 기반). 이는 데이터 변경이 아닌 단순 조회.

---

## RestClient 두 가지 — 용도별 타임아웃 분리

Spider Admin은 CMS 호출용 RestClient를 **두 개**로 분리해 사용한다.

### `cmsBuilderRestClient` — 업로드용 (60초)

```java
@Bean
public RestClient cmsBuilderRestClient() {
    return buildClient(
            properties.getConnectTimeoutSeconds(),       // 5초
            properties.getReadTimeoutSeconds())           // 60초 — 대용량 이미지 고려
        .mutate()
        .defaultHeader("x-deploy-token", properties.getDeploySecret())
        .build();
}
```

대용량 이미지(예: 20MB) 업로드를 고려해 **read-timeout 60초**.

### `cmsBuilderDeployRestClient` — 배포·삭제용 (10초)

```java
@Bean
public RestClient cmsBuilderDeployRestClient() {
    return buildClient(
            properties.getDeployConnectTimeoutSeconds(),  // 5초
            properties.getDeployReadTimeoutSeconds())     // 10초
        .mutate()
        .defaultHeader("x-deploy-token", properties.getDeploySecret())
        .build();
}
```

파일 이동·삭제 같은 가벼운 호출은 **read-timeout 10초**. 짧게 둔 이유:
- 파일 이동은 CMS 내부 디스크 I/O라 수 초 내 끝나야 정상
- 승인 API는 Saga 보상 롤백이 뒤에 붙어 있어, 호출이 길어지면 사용자 UI도 그만큼 지연됨

설정값은 `application.yml`의 `cms.builder.*`에서 환경별로 조정한다.

```yaml
# application.yml
cms:
  builder:
    base-url: http://133.186.135.23
    upload-path: /cms/api/builder/upload
    connect-timeout-seconds: 5
    read-timeout-seconds: 60
    deploy-connect-timeout-seconds: 5
    deploy-read-timeout-seconds: 10
    deploy-secret: ${CMS_DEPLOY_SECRET}
```

---

## 인증 — `x-deploy-token`

Spider Admin과 CMS는 같은 `DEPLOY_SECRET` 값을 공유한다. RestClient 생성 시 `defaultHeader`로 자동 주입되므로 호출부에서는 헤더 신경 쓸 필요가 없다.

### Spider Admin 측 (자동 주입)

```java
.mutate()
.defaultHeader("x-deploy-token", properties.getDeploySecret())
.build();
```

### CMS 측 (검증)

```typescript
import { timingSafeEqual } from 'crypto';
import { DEPLOY_SECRET } from '@/lib/env';

function isValidToken(token: string | null): boolean {
    if (!DEPLOY_SECRET || !token) return false;
    try {
        const expected = Buffer.from(DEPLOY_SECRET, 'utf8');
        const received = Buffer.from(token, 'utf8');
        if (expected.length !== received.length) return false;
        return timingSafeEqual(expected, received);  // 타이밍 공격 방지
    } catch {
        return false;
    }
}
```

`timingSafeEqual()`은 문자열 비교가 일찍 종료되지 않도록 보장해, 응답 시간 차이를 분석해 토큰을 추측하는 공격(타이밍 어택)을 막는다.

### 인증 모드 분기 — 서버 간 호출 vs 브라우저 세션

CMS의 일부 엔드포인트(예: `/cms/api/builder/upload`)는 **두 가지 호출자**를 모두 받는다:

```typescript
const tokenValid = isValidToken(req.headers.get('x-deploy-token'));

if (tokenValid) {
    // 서버 간 호출 (Spider Admin) — form data의 userId/userName을 직접 사용
    if (!bodyUserId) return contentBuilderErrorResponse('서버 간 호출 시 userId가 필요합니다.');
    userId = bodyUserId;
    userName = bodyUserName ?? bodyUserId;
} else {
    // 브라우저 세션 호출 — JWT 쿠키 기반 인증
    const currentUser = await getCurrentUser();
    if (!canAccessCmsEdit(currentUser)) return contentBuilderErrorResponse('Permission denied.');
    userId = currentUser.userId;
    userName = currentUser.userName;
}
```

토큰 유효 시 → 서버 간 호출로 인식, 요청 바디의 사용자 정보를 신뢰. 토큰 없거나 무효 시 → 브라우저 호출로 인식, JWT 세션 검증.

---

## 호출 엔드포인트 목록

Spider Admin이 호출하는 모든 CMS API는 한 곳(`CmsBuilderClient`)에 모여 있다.

| 메서드 | CMS 엔드포인트 | 사용처 | 클라이언트 |
|---|---|---|---|
| POST | `/cms/api/builder/upload` | 이미지 업로드 (multipart 중계) | `cmsBuilderRestClient` (60초) |
| DELETE | `/cms/api/assets/{assetId}` | 이미지 삭제 | `cmsBuilderRestClient` |
| POST | `/cms/api/assets/{assetId}/deploy` | 이미지 승인 후 파일 이동 | `cmsBuilderDeployRestClient` (10초) |
| GET | `/cms/api/assets/{assetId}/image` | 이미지 바이너리 조회 (Admin이 사용자에게 중계) | `cmsBuilderDeployRestClient` |

페이지 승인·배포 트리거는 spider-admin이 직접 호출하지 않고, 별도 도메인(`cmsdeployment`, `cmspage`)에서 처리한다. 만료 처리에서는 `CmsDeployService`가 운영 서버의 `/cms/api/deploy/receive`를 호출한다.

---

## 파일 전송 — multipart/form-data 중계

이미지 업로드는 **spider-admin이 파일을 디스크에 쓰지 않고 메모리에서 그대로 CMS로 흘려보내는 중계 방식**이다.

```
[브라우저]
  multipart/form-data (file + assetName + ...)
       │
       ▼
[Spider Admin: CmsAssetUploadController]
  - MultipartFile 받음 (Spring이 디스크 또는 메모리에 임시 저장)
  - 업로더 ID/이름은 SecurityContext에서 추출 (클라이언트 입력 신뢰 X)
       │
       ▼
[CmsBuilderClient.upload()]
  - file.getResource()로 스트림 참조만 사용 — 전체 바이트 힙 복사 안 함
  - UTF-8 명시 (한글 파일명 깨짐 방지)
  - cmsBuilderRestClient POST → CMS
       │
       ▼
[CMS: /cms/api/builder/upload]
  - 디스크에 저장 (ASSET_UPLOAD_DIR/{assetId}_{filename})
  - SPW_CMS_ASSET INSERT
```

### 핵심 — `MultipartFile.getResource()`

```java
private HttpEntity<Resource> toFilePart(MultipartFile file) {
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
    return new HttpEntity<>(file.getResource(), partHeaders);  // ★ 핵심
}
```

`getResource()`는 내부 InputStream을 직접 참조하는 Resource를 반환한다. 전체 파일을 byte[]로 힙에 복사하지 않으므로 **20MB 이미지를 다건 업로드해도 OOM/GC 압박이 없다.**

### 한글 파일명 처리 — UTF-8 명시

Spring `FormHttpMessageConverter`의 String 파트 기본 인코딩은 **ISO-8859-1**이다. 한글 같은 멀티바이트 문자는 깨진다. 명시적으로 UTF-8 HttpEntity로 감싸야 한다.

```java
private HttpEntity<String> toTextPart(String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
    return new HttpEntity<>(value, headers);
}

// 사용
form.add("assetName", toTextPart(assetName));  // 한글 가능
form.add("userName", toTextPart(userName));    // 한글 가능
```

### multipart 실제 HTTP body

```
POST /cms/api/builder/upload HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
x-deploy-token: <DEPLOY_SECRET>

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="photo.png"
Content-Type: image/png

[PNG 바이너리 그대로]
------WebKitFormBoundary
Content-Disposition: form-data; name="assetName"
Content-Type: text/plain; charset=UTF-8

프로필이미지
------WebKitFormBoundary
Content-Disposition: form-data; name="userId"
Content-Type: text/plain; charset=UTF-8

user123
------WebKitFormBoundary--
```

파일 파트는 **바이너리 그대로**, 텍스트 파트는 UTF-8로 인코딩되어 하나의 HTTP 요청에 같이 실린다. Base64 인코딩 같은 것은 사용하지 않는다.

---

## 응답 처리 패턴

CMS는 ContentBuilder 호환을 위해 **HTTP 200을 반환하면서 body의 `ok` 필드로 성공/실패를 표시**하는 경우가 있다. spider-admin은 이를 인지해 분기 처리한다.

```java
CmsBuilderUploadApiResponse response = cmsBuilderRestClient
        .post()
        .uri(properties.getUploadPath())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(form)
        .retrieve()
        .body(CmsBuilderUploadApiResponse.class);

if (response == null) {
    throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 응답이 비어 있습니다.");
}
if (!response.isSuccess()) {  // ★ HTTP 200이어도 ok=false면 실패
    String errMsg = response.getError() != null ? response.getError() : "CMS 업로드 실패";
    throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, errMsg);
}
```

### CMS 응답 형식 두 가지

| 헬퍼 함수 | 응답 body | HTTP status | 사용처 |
|---|---|---|---|
| `successResponse(data)` | `{ ok: true, data }` | 200 | 일반 |
| `errorResponse(msg, status)` | `{ ok: false, error }` | 4xx/5xx | 일반 |
| `contentBuilderErrorResponse(msg)` | `{ ok: false, error }` | **200** | ContentBuilder 호환 |

`/cms/api/builder/upload`는 ContentBuilder 호환 응답을 쓰므로 spider-admin이 항상 `ok` 필드를 봐야 한다.

### DELETE·deploy 같은 가벼운 호출

body가 비어있거나 비JSON일 수 있어 `toBodilessEntity()`로 받고, **HTTP status 코드만으로 판정**한다. 4xx/5xx는 `RestClientException`으로 자동 전파된다.

```java
cmsBuilderRestClient
        .delete()
        .uri(DELETE_PATH_TEMPLATE, assetId)
        .retrieve()
        .toBodilessEntity();  // body 무시, status로만 판정
```

---

## 에러 처리·보상 트랜잭션 (Saga)

CMS 호출 실패는 **`BaseException(ErrorType.EXTERNAL_SERVICE_ERROR)` (HTTP 502)** 로 일관되게 래핑되어 호출자에게 전파된다.

```java
} catch (RestClientException e) {
    log.error("CMS Builder 호출 중 오류: ...", e);
    throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 서버와 통신할 수 없습니다. 잠시 후 다시 시도하세요.", e);
}
```

### Saga 보상 — 이미지 승인 흐름

이미지 승인은 **DB 상태 변경 + CMS 파일 이동**이라는 두 시스템 작업이 묶여 있다. 단일 `@Transactional`로 묶을 수 없으므로(이유는 [`기술-프로세스.md`](./기술-프로세스.md#2-이미지-승인-saga-패턴) 참고) Saga 패턴을 쓴다.

```java
public void approve(String assetId, String modifierId, String modifierName) {
    // ① 독립 TX로 DB UPDATE 커밋 — CMS가 APPROVED를 읽을 수 있게
    transactionTemplate.executeWithoutResult(status -> {
        assertTransition(assetId, STATE_PENDING, STATE_APPROVED);
        cmsAssetMapper.updateState(assetId, STATE_PENDING, STATE_APPROVED, ...);
    });

    // ② CMS 파일 배포 호출
    try {
        cmsBuilderClient.deployAsset(assetId);
    } catch (BaseException deployEx) {
        // ③ 보상 — DB 상태 되돌리기
        try {
            transactionTemplate.executeWithoutResult(status ->
                cmsAssetMapper.updateState(assetId, STATE_APPROVED, STATE_PENDING, ...));
        } catch (RuntimeException revertEx) {
            // 보상 실패 — 데이터 불일치, error 로깅 (수동 복구 알림)
            log.error("승인 롤백 실패. 수동 확인 필요: assetId={}", assetId, revertEx);
        }
        throw deployEx;
    }
}
```

핵심:
- 정상 경로: ① 커밋 → ② 성공 → 종료
- 실패 경로: ① 커밋 → ② 실패 → ③ 보상 롤백 → 예외 전파
- 최악의 경로: ① 커밋 → ② 실패 → ③ 보상도 실패 → **데이터-파일 불일치 상태**, 로그로 알림

이중 실패는 매우 드물지만 0이 아니므로 운영자가 인지할 수 있도록 `error` 레벨 로그를 남긴다.

---

## 호출 흐름 요약 다이어그램

### 이미지 업로드

```
[브라우저]
  POST /api/cms-admin/asset-uploads
  multipart/form-data
       │
       ▼
[Spider Admin]
  ① 인증·검증 (Spring Security + Validator)
  ② 업로더 정보 추출 (@AuthenticationPrincipal)
  ③ multipart 재구성 (UTF-8 명시 + getResource() 스트리밍)
       │
       │  POST /cms/api/builder/upload
       │  x-deploy-token: ...
       │  multipart/form-data
       ▼
[CMS]
  ④ 토큰 검증 → 서버 간 호출로 인식
  ⑤ 파일 디스크 저장 + DB INSERT
       │
       │  { ok: true, data: { assetId, url } }
       ▼
[Spider Admin]
       │  { assetId, url }
       ▼
[브라우저]
```

### 이미지 승인 (Saga)

```
[Spider Admin: 결재자 승인 클릭]
       │
       ▼
   ① TX1: DB UPDATE PENDING → APPROVED (커밋)
       │
       ▼
   ② POST /cms/api/assets/{assetId}/deploy
       │  ┌──────────────────────────┐
       │  │ [CMS]                    │
       │  │ ASSET_STATE 검증          │
       │  │ /uploads → /deployed/img │
       │  │ DB ASSET_PATH 갱신       │
       │  │ /uploads 원본 삭제       │
       │  └──────────────────────────┘
       │
       ▼
   정상: 종료
   실패: ③ TX2: APPROVED → PENDING 보상 롤백 → 예외 전파
```

---

## 자주 마주치는 함정

### 1. 한글 파일명·사용자명 깨짐

원인: `FormHttpMessageConverter` 기본 인코딩(ISO-8859-1)
해결: `HttpEntity<String>` + `text/plain;charset=UTF-8` 명시

### 2. 토큰 비교를 `String.equals()`로 하면 위험

문자열 비교는 첫 다른 문자에서 즉시 종료된다. 응답 시간을 측정해 토큰을 추측하는 타이밍 어택에 노출됨. **반드시 `timingSafeEqual()` 사용.**

### 3. 단일 `@Transactional`로 CMS 호출 감싸기

```java
// ❌ 잘못된 패턴
@Transactional
public void wrongApprove() {
    updateState(PENDING → APPROVED);     // 아직 커밋 안 됨
    cmsBuilderClient.deployAsset(...);   // CMS는 PENDING을 읽음 → 거절
}
```

CMS는 별도 트랜잭션·별도 DB 커넥션이라 호출자의 미커밋 변경을 못 본다. 반드시 분리 TX + 보상 패턴으로.

### 4. 업로드용 RestClient로 deploy를 호출

업로드는 60초 타임아웃이라 CMS 응답이 느릴 때 사용자 UI가 길게 멈춘다. **deploy 같은 가벼운 호출은 짧은 타임아웃 RestClient를 따로 써야 함.**

### 5. ContentBuilder 호환 API의 응답 처리

HTTP 200이어도 body `ok=false`면 실패. RestClient의 `retrieve()`는 4xx/5xx만 자동 예외화하므로 **`ok` 필드를 명시적으로 확인**해야 한다.

---

## 관련 파일

### Spider Admin

| 위치 | 파일 |
|---|---|
| 클라이언트 | `domain/cmsasset/client/CmsBuilderClient.java` |
| 응답 DTO | `domain/cmsasset/client/dto/CmsBuilderUploadApiResponse.java` |
| RestClient 빈 | `domain/cmsasset/config/CmsBuilderConfig.java` |
| 설정값 | `domain/cmsasset/config/CmsBuilderProperties.java` |
| 비즈니스 로직 | `domain/cmsasset/service/CmsAssetService.java` |
| 컨트롤러 | `domain/cmsasset/controller/CmsAssetUploadController.java` |
| 컨트롤러 | `domain/cmsasset/controller/CmsAssetApprovalController.java` |
| 페이지 배포 | `domain/cmsdeployment/service/CmsDeployService.java` |

### CMS

| 위치 | 파일 |
|---|---|
| 업로드 수신 | `src/app/api/builder/upload/route.ts` |
| 에셋 삭제 | `src/app/api/assets/[assetId]/route.ts` |
| 에셋 배포 | `src/app/api/assets/[assetId]/deploy/route.ts` |
| 에셋 이미지 | `src/app/api/assets/[assetId]/image/route.ts` |
| 응답 헬퍼 | `src/lib/api-response.ts` |
| 토큰 검증 | 각 route.ts 내 `isValidToken()` (공통화 예정) |
| 환경변수 | `src/lib/env.ts` (`DEPLOY_SECRET`) |

---

## 관련 문서

- [`시스템-아키텍처.md`](./시스템-아키텍처.md) — 전체 시스템 구성과 통신 경로
- [`기술-프로세스.md`](./기술-프로세스.md) — 기능별 내부 흐름 (Saga 상세 등)
- [`서버-포트-분리-설계.md`](./서버-포트-분리-설계.md) — CMS :3000/:3001 분리 이유
- [`워크플로-분석.md`](./워크플로-분석.md) — 콘텐츠 제작·승인·배포 논리 흐름
