# 로그레벨 조정 — WAS 전파 설계 문서

## 1. 배경 및 요구사항

PDF 제안서 p.21에 명시된 핵심 요구사항:

> 로그 레벨 실시간 변경 (WAS 재시작 불필요)  
> Spring 이전 구버전 Spider에서도 동일하게 지원되던 핵심 기능

---

## 2. AS-IS 구현 방식

### 2-1. 처리 클래스

| 파일 | 역할 |
|------|------|
| `LogManageSvcAction.java` | 로그레벨 변경 요청 수신 및 처리 |
| `ReloadUtil.java` | WAS 인스턴스 목록 조회 후 전파 실행 |
| `ManagementClient.java` | 개별 WAS에 TCP 소켓으로 명령 전송 |
| `ManagementClientWorker.java` | TCP 소켓 연결 및 `ManagementContext` 직렬화 전송 |

### 2-2. 전파 흐름

```
Admin UI (로그레벨 변경)
  → LogManageSvcAction.doSaveLog()
      → DataMap { loggerName, level, gubun: "log_config_level" }
      → ReloadUtil.reload(ALL_WAS_CONFIG, map)
          → was_config.xml에서 WAS 인스턴스 목록 조회 (FW11, PW11, ...)
          → 각 인스턴스의 ManagementClient.doProcess(instanceId, map)
              → TCP 소켓 연결 (MANAGEMENT_SERVER_IP:MANAGEMENT_SERVER_PORT)
              → ManagementContext 직렬화 전송
              → 각 WAS 인스턴스가 in-memory Logback 즉시 변경
```

### 2-3. 등록된 WAS 인스턴스 예시

| ID | 역할 | 관리 TCP 포트 |
|----|------|-------------|
| FW11 | 채널/Frontend WAS | 50005 |
| PW11 | 업무/Processing WAS | 50004 |

- WAS 인스턴스는 `was_config.properties.xml` (설정 파일)로 관리
- 비즈니스 포트와 **관리 포트를 분리** 운영
- `ALL_WAS_CONFIG` 타겟은 모든 등록 인스턴스에 브로드캐스트

> **참고:** AS-IS의 `LogManageSvcAction` 코드는 현재 전체 주석 처리(비활성) 상태로 남아 있음.  
> 즉 AS-IS에서도 실제로는 동작하지 않던 기능임.

---

## 3. TO-BE 아키텍처

### 3-1. 전체 서비스 구성

```
Admin (HTTP 8080, TCP 9999)           ← F/w Admin 서버
  ├── spider-link (HTTP 8082, TCP 9996) ← 연계 미들웨어
  │     └── → demo/backend (TCP 9997)
  ├── batch-was (HTTP 8081, TCP 9998)  ← 배치 서버
  └── [bizApp — Admin과 직접 연결 없음]
        ├── biz-channel (HTTP 9998)   ← 채널AP (React 프론트 수신)
        ├── biz-auth (TCP 19100 only) ← 인증AP (HTTP 서버 없음)
        ├── biz-transfer (TCP 19200 only) ← 이체AP (HTTP 서버 없음)
        └── mock-core (TCP 19300 only) ← 계정계 Mock (HTTP 서버 없음)
```

### 3-2. AS-IS WAS 인스턴스 → TO-BE 대응 관계

AS-IS가 설정 파일(`was_config.properties.xml`)로 관리하던 것을 TO-BE는 DB(`FWK_WAS_INSTANCE` 테이블)로 동적 관리한다.

| AS-IS | TO-BE 대응 | HTTP 포트 | TCP 포트 |
|-------|-----------|-----------|---------|
| FW11 (채널 WAS) | spider-link | 8082 | 9996 |
| PW11 (업무 AP WAS) | batch-was | 8081 | 9998 |
| — (신규) | biz-channel | 9998 | — |
| — (신규) | biz-auth | ❌ 없음 | 19100 |
| — (신규) | biz-transfer | ❌ 없음 | 19200 |

### 3-3. TO-BE 전파 방식 변경

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| 통신 프로토콜 | TCP 소켓 (ManagementClient) | HTTP REST |
| WAS 목록 관리 | `was_config.properties.xml` | `FWK_WAS_INSTANCE` DB 테이블 |
| 실패 처리 | 일부 실패 시 나머지 계속 진행 | 예외 삼킴, warn 로그만 출력 |
| 보안 | 내부망 TCP | `InternalApiInterceptor` (허용 IP 목록) |

---

## 4. 현재 구현 상태

### 4-1. 구현 완료

| 서비스 | 구현 내용 |
|--------|---------|
| **admin** | `LogLevelController` → Logback 즉시 변경 후 `SpiderLogLevelClient` 호출 |
| **admin** | `SpiderLogLevelClient` — HTTP fire-and-forget, 실패 시 warn 로그만 |
| **spider-link** | `LogLevelReloadController` — `POST /api/internal/log/level`, `/additivity` |

### 4-2. 전파 흐름 (현재)

```
Admin UI (로그레벨 변경)
  → PATCH /api/log-level/level
      → LogLevelService.updateLevel()     [Admin 자신의 Logback 즉시 변경]
      → SpiderLogLevelClient.syncLevel()  [POST http://spider-link:8082/api/internal/log/level]
          → spider-link Logback 즉시 변경
          → 실패 시: warn 로그만, Admin은 200 반환
```

### 4-3. SpiderLogLevelClient 설계 원칙

`SpiderLinkReloadClient` (SQL Reload)와 동일한 패턴을 따른다:

1. **트랜잭션 분리** — Service 완료(Admin Logback 변경) 후 Controller에서 호출
2. **예외 비전파** — spider-link 미기동·네트워크 오류 시 warn 로그만, Admin 응답 200 보장
3. **설정 외부화** — `spiderlink.log-level-url` 환경변수로 관리

```yaml
# admin/src/main/resources/application.yml
spiderlink:
  reload-url: ${SPIDERLINK_RELOAD_URL:http://localhost:8082/api/internal/sql/reload}
  log-level-url: ${SPIDERLINK_LOG_LEVEL_URL:http://localhost:8082/api/internal/log}  # 추가
```

---

## 5. 미구현 서비스 및 향후 과제

### 5-1. HTTP 서버가 있는 미구현 서비스

**batch-was** (HTTP 8081): spider-link와 동일한 방식으로 구현 가능.  
`LogLevelReloadController`를 batch-was에도 추가하고 `SpiderLogLevelClient`에 호출 추가.

### 5-2. HTTP 서버가 없는 서비스 (아키텍처 결정 필요)

**biz-auth**, **biz-transfer**는 `spring.main.web-application-type: none`으로 설정되어 HTTP 서버가 없다.  
현재 아키텍처에서는 HTTP REST 방식으로 로그레벨을 전파할 수 없다.

AS-IS는 이 문제를 비즈니스 TCP 포트와 **별도의 관리 전용 TCP 포트**를 분리해 해결했다.  
(FW11 비즈니스 55011 + 관리 50005 / PW11 비즈니스 56011 + 관리 50004)

TO-BE에서 biz-auth/biz-transfer에 전파하려면 팀 협의 후 아래 방안 중 하나를 선택해야 한다:

| 방안 | 내용 | 난이도 |
|------|------|--------|
| **A. 관리용 HTTP 포트 추가** | `web-application-type: none → servlet` 변경, TCP 서버와 HTTP 서버 공존 | 중 |
| **B. TCP 관리 채널 추가** | AS-IS ManagementServer 방식 복원, 기존 TCP 서버에 관리 커맨드 분기 처리 추가 | 높음 |
| **C. 범위 유지** | HTTP 서버 있는 서비스만 전파 대상으로 유지 (biz-auth/biz-transfer 제외) | 없음 |

> **현 시점 결론:** biz-auth/biz-transfer 전파는 아키텍처 결정 후 별도 구현. Admin UI에서의 로그레벨 변경은 Admin + spider-link까지만 즉시 반영되며, biz-auth/biz-transfer는 서버 직접 접속 방식으로 운영 대응.

### 5-3. FWK_WAS_INSTANCE 기반 동적 전파 (장기 과제)

현재 `SpiderLogLevelClient`는 단일 URL(`spiderlink.log-level-url`)을 고정 호출한다.  
AS-IS 설계 의도에 맞게 발전시키려면:

1. `FWK_WAS_INSTANCE` 테이블에서 활성 인스턴스 목록 조회
2. 각 인스턴스의 IP + HTTP 관리 포트로 동적 전파
3. 실패 인스턴스 로깅 후 나머지 계속 진행

이 구조로 전환하면 Admin UI에서 WAS 인스턴스를 추가/삭제하는 것만으로 전파 대상이 자동으로 반영된다.

---

## 6. 관련 파일 목록

| 파일 | 역할 |
|------|------|
| `admin/.../loglevel/controller/LogLevelController.java` | 레벨 변경 후 SpiderLogLevelClient 호출 |
| `admin/.../loglevel/service/LogLevelService.java` | Admin 자신의 Logback 변경 |
| `admin/.../global/client/SpiderLogLevelClient.java` | spider-link HTTP 동기화 클라이언트 |
| `admin/src/main/resources/application.yml` | `spiderlink.log-level-url` 설정 |
| `spider-link/.../loglevel/LogLevelReloadController.java` | spider-link 내부 로그레벨 변경 API |
| `admin/e2e/api/log-level.spec.ts` | API 계약 테스트 (WAS 동기화 내성 포함) |
| `admin/e2e/pages/log-level-manage.spec.ts` | UI E2E 테스트 |
