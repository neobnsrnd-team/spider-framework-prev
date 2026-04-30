# 로그레벨 조정 WAS Reload — 설계 및 운영 가이드

> **작성일:** 2026-04-28  
> **대상 프로젝트:** admin, spider-link, biz-auth, biz-channel (및 모든 WAS)  
> **상태:** 구현 완료

---

## 목차

1. [개요](#1-개요)
2. [배경 및 요구사항](#2-배경-및-요구사항)
3. [아키텍처](#3-아키텍처)
4. [흐름도](#4-흐름도)
5. [컴포넌트 상세](#5-컴포넌트-상세)
6. [DB 설계](#6-db-설계)
7. [API 명세](#7-api-명세)
8. [운영 프로세스](#8-운영-프로세스)
9. [확장 가이드](#9-확장-가이드)
10. [관련 파일 목록](#10-관련-파일-목록)

---

## 1. 개요

Admin에서 Logback 로거의 레벨·Additivity를 런타임에 변경하고, 변경 사항을 등록된 WAS 인스턴스에 실시간으로 Reload하는 기능이다. WAS 재시작 없이 로그 설정을 적용할 수 있다.

### 핵심 설계 원칙

| 원칙 | 내용 |
|------|------|
| **분리된 저장** | Admin 자체 변경(PATCH)과 WAS Reload(POST)를 명시적으로 분리 — 사용자가 Reload 대상과 시점을 직접 선택 |
| **설정 기반 분기** | 코드 수정 없이 DB(`FWK_PROPERTY`)의 `COMM_TYPE` 값만 변경하여 통신 방식 전환 |
| **단일 Reload 경로** | HTTP WAS, TCP WAS 모두 `ReloadService.executeReload()` 하나로 처리 |
| **전략 패턴** | `ManagementExecutor` 인터페이스 — 신규 관리 명령은 구현체 추가만으로 확장 |
| **Auto-Configuration** | TCP WAS는 spider-link 의존성 추가만으로 관리 명령 수신 준비 완료 (별도 코드 작성 불필요) |

---

## 2. 배경 및 요구사항

### 2-1. AS-IS (구 Spider Framework)

구 Spider Framework는 `ManagementClient`가 각 WAS의 관리 전용 TCP 포트에 직접 접속하여 `ManagementContext` 객체를 직렬화 전송하는 방식이었다.

```
Admin UI (로그레벨 변경)
  → LogManageSvcAction.doSaveLog()
      → ReloadUtil.reload(ALL_WAS_CONFIG, map)
          → was_config.xml에서 WAS 인스턴스 목록 조회
          → ManagementClient.doProcess(instanceId, map)
              → TCP 소켓 연결 (MANAGEMENT_SERVER_IP:MANAGEMENT_SERVER_PORT)
              → ManagementContext 직렬화 전송 (Java ObjectStream)
```

**한계점**

- WAS 인스턴스 목록이 XML 설정 파일에 고정 — 동적 추가/삭제 불가
- 모든 WAS에 TCP 관리 포트가 별도로 필요
- `LogManageSvcAction` 코드가 전체 주석 처리 상태 — 실제로는 동작하지 않던 기능

### 2-2. TO-BE 설계 방향

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| WAS 목록 관리 | `was_config.xml` (파일) | `FWK_WAS_INSTANCE` DB 테이블 |
| 통신 프로토콜 | TCP 소켓 단일 | HTTP 또는 TCP — WAS별 선택 |
| 통신 방식 결정 | 고정 (모두 TCP) | `FWK_PROPERTY.COMM_TYPE` 값으로 동적 결정 |
| Reload 트리거 | 변경 즉시 자동 Reload | 사용자가 대상 WAS를 선택하여 명시적 Reload |
| 신규 WAS 연동 | XML 수정 + 코드 수정 | DB 등록 + 의존성 추가 |

---

## 3. 아키텍처

### 3-1. 전체 서비스 구성

```
┌──────────────────────────────────────────────────────────────────────┐
│  Admin  (HTTP :8080)                                                 │
│                                                                      │
│  [UI] 로그레벨 조정 메뉴                                              │
│    ├── 체크박스로 로거 행 선택                                         │
│    ├── "저장" → Admin 자신의 Logback 즉시 변경                         │
│    └── "선택 WAS Reload" → WAS 선택 모달 → Reload 실행               │
│                                                                      │
│  [API]                                                               │
│    PATCH /api/log-level/level      → Admin Logback 변경              │
│    PATCH /api/log-level/additivity → Admin Logback 변경              │
│    POST  /api/log-level/propagate  → WAS Reload                      │
│                         │                                            │
│                    ReloadService                                      │
│                    FWK_PROPERTY에서 COMM_TYPE 조회                    │
│                         │                                            │
│           ┌─────────────┴─────────────┐                             │
│           │ COMM_TYPE=HTTP             │ COMM_TYPE=TCP               │
│           │ RestTemplate               │ TcpClient.sendJson()        │
│           │ POST /api/management/reload│ command: MANAGEMENT_RELOAD  │
└───────────┼────────────────────────────┼─────────────────────────────┘
            │                            │
            ▼                            ▼
┌───────────────────┐        ┌───────────────────────┐
│  HTTP WAS         │        │  TCP WAS              │
│  (spider-link,    │        │  (biz-auth,           │
│   biz-channel)    │        │   biz-transfer 등)    │
│                   │        │                       │
│  LogLevelReload   │        │  ManagementReload     │
│  Controller       │        │  CommandHandler       │
│  (/api/internal/  │        │  → LogLevelExecutor   │
│   log/level)      │        │  → LogLevelApplier    │
│                   │        │                       │
│  (AutoConfig 자동 │        │  (AutoConfig 자동      │
│   등록)           │        │   등록)               │
└───────────────────┘        └───────────────────────┘
```

### 3-2. WAS별 통신 방식 정리

| WAS | HTTP 서버 | TCP 서버 | COMM_TYPE | 비고 |
|-----|-----------|----------|-----------|------|
| spider-link | :8082 | :9996 | HTTP | — |
| biz-channel | :9998 | — | HTTP | — |
| biz-auth | ❌ | :19100 | **TCP** | `web-application-type=none` |
| biz-transfer | ❌ | :19200 | **TCP** | `web-application-type=none` |
| batch-was | :8081 | :9998 | HTTP | — |

### 3-3. 컴포넌트 관계도

```
[admin]                              [spider-link]
                                     
LogLevelController                   LogLevelApplier ◄──────────────┐
  ├── LogLevelService                   applyLevel()                 │
  │     └── Logback 직접 변경           applyAdditivity()            │
  └── LogLevelPropagationService                                      │
        └── ReloadService            LogLevelReloadController         │
              ├── RestTemplate         (HTTP WAS용)                   │
              │     └─ HTTP Reload ────▶ /api/internal/log/level      │
              │                                                        │
              └── TcpClient           ManagementExecutor (interface)   │
                    └─ TCP Reload ─────▶ ├── LogLevelExecutor ─────────┘
                                        └── LogAdditivityExecutor ───┘
                                        │
                                        ManagementReloadCommandHandler
                                          └── executor.execute(params)

FWK_PROPERTY (DB)
  {instanceId}.MANAGEMENT_SERVER_IP    ← 전 WAS 공통 등록
  {instanceId}.MANAGEMENT_SERVER_PORT  ← 전 WAS 공통 등록
  {instanceId}.COMM_TYPE               ← TCP WAS만 등록 (biz-auth 등)
                                          미등록 시 코드 기본값 "HTTP" 적용
```

---

## 4. 흐름도

### 4-1. Admin 자신의 로그레벨 변경 (PATCH)

```
사용자 (브라우저)
    │
    │  PATCH /api/log-level/level
    │  { "logName": "com.example.svc", "level": "DEBUG" }
    ▼
LogLevelController.updateLevel()
    │  @PreAuthorize("LOG_LEVEL:W")
    ▼
LogLevelService.updateLevel()
    │  LoggerContext.exists(logName) 또는 LoggerFactory.getLogger(logName)
    │  logger.setLevel(Level.toLevel(levelStr))   ← null이면 레벨 제거(상속)
    ▼
Admin Logback 즉시 반영
    │
    ▼
HTTP 200 OK  ← WAS Reload와 무관하게 항상 반환
             ← WAS Reload는 사용자가 별도로 POST /propagate 호출
```

### 4-2. WAS Reload — 사용자 인터랙션 흐름

```
[로그레벨 조정 화면]
    │
    │  1. Reload할 로거 행을 체크박스로 선택
    │     (헤더 체크박스 = 현재 페이지 전체 선택)
    │
    │  2. "선택 WAS Reload" 버튼 클릭
    │     └─ 체크 항목 없으면 경고 토스트 표시 후 중단
    ▼
[WAS 선택 모달 — WasSelectReloadModal]
    │
    │  3. WAS 그룹 필터로 목록 범위 조정
    │  4. Reload할 WAS 카드의 체크박스 선택
    │  5. "Reload 실행" 클릭
    ▼
propagateToWas(instanceIds, checkedRows)
    │
    │  선택된 각 로거에 대해 Promise.all() 병렬 API 호출
    │  ├── gubun=log_config_level     → POST /api/log-level/propagate
    │  └── gubun=log_config_additivity → POST /api/log-level/propagate
    ▼
[결과 화면 — WAS별 성공/실패 표시]
    │
    └── 실패 WAS: "상세" 링크로 오류 메시지 확인
```

### 4-3. WAS Reload — 서버 처리 흐름

```
POST /api/log-level/propagate
{
  "instanceIds": ["biz-auth", "biz-channel"],
  "gubun": "log_config_level",
  "logName": "com.example.svc",
  "level": "DEBUG"
}
    │
    ▼
LogLevelController.propagate()
    │  @PreAuthorize("LOG_LEVEL:W")
    ▼
LogLevelPropagationService.propagate()
    │  ① gubun 유효성 검증
    │     log_config_level, log_config_additivity 만 허용
    │     그 외 → InvalidInputException (HTTP 400)
    │
    │  ② additionalParams 구성
    │     { logName: "com.example.svc", level: "DEBUG" }
    ▼
ReloadService.executeReload()
    │  ReloadType.fromCode("log_config_level") → ReloadType.LOG_LEVEL
    │
    │  instanceIds 순회 → 각 인스턴스별 처리
    ▼
executeReloadForInstance("biz-auth", LOG_LEVEL, params)
    │
    ├─ FWK_WAS_INSTANCE 조회: instanceId="biz-auth"
    ├─ FWK_PROPERTY 조회: biz-auth.MANAGEMENT_SERVER_IP  → "192.168.1.10"
    ├─ FWK_PROPERTY 조회: biz-auth.MANAGEMENT_SERVER_PORT → "19100"
    └─ FWK_PROPERTY 조회: biz-auth.COMM_TYPE             → "TCP"
    │
    │  body = { gubun: "log_config_level",
    │            logName: "com.example.svc", level: "DEBUG" }
    │
    ├──────────── COMM_TYPE = "HTTP" ────────────────────────────────┐
    │                                                                 │
    │  executeReloadViaHttp()                                         │
    │  RestTemplate.exchange(                                         │
    │    "http://192.168.1.xx:50005/api/management/reload",          │
    │    POST, body)                                                  │
    │  응답.success=true → WasReloadResult(success=true)             │
    │                                                                 │
    └──────────── COMM_TYPE = "TCP" ─────────────────────────────────┘
    │
    │  executeReloadViaTcp()
    │  TcpClient.sendJson("192.168.1.10", 19100,
    │    JsonCommandRequest {
    │      command: "MANAGEMENT_RELOAD",
    │      payload: { gubun: "log_config_level",
    │                 logName: "com.example.svc",
    │                 level: "DEBUG" }
    │    })
    │  응답.success=true → WasReloadResult(success=true)
    │
    ▼
ReloadResultResponse {
  reloadType: "log_config_level",
  results: [
    { instanceId: "biz-auth",    success: true  },
    { instanceId: "biz-channel", success: true  }
  ]
}
    │
    ▼
HTTP 200 OK + 결과 데이터
```

### 4-4. TCP WAS 수신 처리 흐름 (biz-auth 기준)

```
TcpClient.sendJson(host="192.168.1.10", port=19100)
    │
    │  전송 포맷: [4바이트 길이(int)] + [UTF-8 JSON]
    │  {
    │    "command": "MANAGEMENT_RELOAD",
    │    "payload": {
    │      "gubun": "log_config_level",
    │      "logName": "com.example.biz",
    │      "level": "DEBUG"
    │    }
    │  }
    ▼
biz-auth SpiderTcpServer (port :19100)
    │  JsonMessageCodec으로 역직렬화
    │  → JsonCommandRequest 객체 생성
    ▼
CommandDispatcher.dispatch(command="MANAGEMENT_RELOAD", request)
    │  handlers 목록 순회 → supports("MANAGEMENT_RELOAD") 검사
    ▼
ManagementReloadCommandHandler.handle()
    │  ← spider-link SpiderLinkAutoConfiguration으로 자동 등록
    │
    │  payload.get("gubun") → "log_config_level"
    │  executors 목록 순회 → supports("log_config_level") 검사
    ▼
LogLevelExecutor.execute(params)
    │  ← spider-link SpiderLinkAutoConfiguration으로 자동 등록
    │
    │  params.get("logName") → "com.example.biz"
    │  params.get("level")   → "DEBUG"
    ▼
LogLevelApplier.applyLevel("com.example.biz", "DEBUG")
    │  LoggerContext.exists("com.example.biz") → 없으면 생성
    │  logger.setLevel(Level.DEBUG)
    ▼
biz-auth Logback 즉시 반영
    │
    ▼
JsonCommandResponse {
  "command": "MANAGEMENT_RELOAD",
  "success": true,
  "message": "log_config_level 처리 완료"
}
    │  [4바이트 길이(int)] + [UTF-8 JSON] 응답
    ▼
TcpClient → WasReloadResult(success=true)
```

### 4-5. spider-link Auto-Configuration 등록 흐름

```
Spring Boot 기동 (biz-auth, biz-channel 등 spider-link 의존 모듈)
    │
    │  META-INF/spring/
    │  org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │  └── com.example.spiderlink.config.SpiderLinkAutoConfiguration
    ▼
SpiderLinkAutoConfiguration 로드
    │
    ├──▶ @Bean LogLevelApplier              (@ConditionalOnMissingBean)
    │        Logback 런타임 조작 공통 로직
    │        HTTP·TCP 양쪽에서 공유
    │
    ├──▶ @Bean LogLevelExecutor             (@ConditionalOnMissingBean)
    │        gubun="log_config_level" 처리
    │        → LogLevelApplier.applyLevel() 위임
    │
    ├──▶ @Bean LogAdditivityExecutor        (@ConditionalOnMissingBean)
    │        gubun="log_config_additivity" 처리
    │        → LogLevelApplier.applyAdditivity() 위임
    │
    ├──▶ @Bean ManagementReloadCommandHandler
    │        (@ConditionalOnBean(ManagementExecutor.class))
    │        (@ConditionalOnMissingBean)
    │        command="MANAGEMENT_RELOAD" TCP 수신 처리
    │        → ManagementExecutor 목록에서 gubun 매칭 후 위임
    │
    └──▶ @Bean MessageInstanceRecorder
             (@ConditionalOnBean(JdbcTemplate.class))
             거래 이력 기록 (기존 기능)

BizAuthConfig
    │  List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers 주입
    │  ↑ ManagementReloadCommandHandler 포함
    ▼
new CommandDispatcher(handlers)
    └── TCP 서버에서 MANAGEMENT_RELOAD 수신 시 자동 라우팅
```

---

## 5. 컴포넌트 상세

### 5-1. admin

#### LogLevelController
- **경로:** `domain/loglevel/controller/LogLevelController.java`
- **역할:** 로그레벨 변경 및 WAS Reload REST 엔드포인트 제공
- **권한:** `LOG_LEVEL:R` (전체), `LOG_LEVEL:W` (변경·Reload)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/log-level` | 전체 로거 목록 조회 |
| PATCH | `/api/log-level/level` | Admin 자신의 로그 레벨 변경 |
| PATCH | `/api/log-level/additivity` | Admin 자신의 Additivity 변경 |
| POST | `/api/log-level/propagate` | 선택 WAS 인스턴스에 Reload |

#### LogLevelService
- **경로:** `domain/loglevel/service/LogLevelService.java`
- **역할:** Admin 자신의 Logback LoggerContext 직접 조작
- **특이사항:** 변경 내용이 DB에 저장되지 않으므로 서버 재시작 시 초기화됨

#### LogLevelPropagationService
- **경로:** `domain/loglevel/service/LogLevelPropagationService.java`
- **역할:** gubun 유효성 검증 후 ReloadService에 위임
- **검증:** `log_config_level`, `log_config_additivity` 외 gubun은 `InvalidInputException` 발생

#### ReloadService
- **경로:** `domain/reload/service/ReloadService.java`
- **역할:** 인스턴스별 COMM_TYPE을 조회하여 HTTP 또는 TCP로 분기 Reload
- **핵심 메서드:**
  - `executeReload(request)` — 공개 진입점, instanceIds 순회
  - `executeReloadViaTcp(...)` — TcpClient로 MANAGEMENT_RELOAD 커맨드 전송
  - `executeReloadViaHttp(...)` — RestTemplate으로 관리 엔드포인트 POST

#### TcpClient (기존)
- **경로:** `infra/tcp/client/TcpClient.java`
- **역할:** TCP JSON 통신 클라이언트
- **프로토콜:** `[4바이트 길이(int)] + [UTF-8 JSON]`
- **타임아웃:** 연결 2초, 읽기 60초

### 5-2. spider-link

#### LogLevelApplier
- **경로:** `domain/loglevel/LogLevelApplier.java`
- **역할:** Logback 런타임 조작 공통 로직 — HTTP 컨트롤러와 TCP 실행기 양쪽에서 공유
- **주요 메서드:**
  - `applyLevel(logName, levelStr)` — null이면 명시적 레벨 제거(부모 상속)
  - `applyAdditivity(logName, additivity)` — "Y" → true, "N" → false

#### ManagementReloadCommandHandler
- **경로:** `domain/management/ManagementReloadCommandHandler.java`
- **역할:** TCP `MANAGEMENT_RELOAD` 커맨드 수신 → `gubun` 값으로 `ManagementExecutor` 선택 및 위임
- **커맨드:** `MANAGEMENT_RELOAD`
- **payload 필수 필드:** `gubun`

#### ManagementExecutor (인터페이스)
- **경로:** `domain/management/executor/ManagementExecutor.java`
- **역할:** 관리 명령 실행기 전략 인터페이스
- **구현체:**
  - `LogLevelExecutor` — `gubun=log_config_level`
  - `LogAdditivityExecutor` — `gubun=log_config_additivity`
- **확장:** 각 WAS에서 `@Component`로 구현체 등록 시 자동으로 처리 대상에 포함

#### SpiderLinkAutoConfiguration
- **경로:** `config/SpiderLinkAutoConfiguration.java`
- **역할:** 위 컴포넌트들을 Spring Boot Auto-Configuration으로 자동 등록
- **진입점:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

## 6. DB 설계

### 6-1. 관련 테이블

#### FWK_WAS_INSTANCE — WAS 인스턴스 목록

Reload 대상 WAS 인스턴스를 관리하는 기존 테이블. 별도 변경 없음.

#### FWK_PROPERTY — 인스턴스별 통신 설정

기존 `MANAGEMENT_SERVER_IP`, `MANAGEMENT_SERVER_PORT`와 동일한 방식으로 `{instanceId}.COMM_TYPE` 데이터를 추가하여 통신 방식을 관리한다.

| PROPERTY_GROUP_ID | PROPERTY_ID | PROPERTY_NAME | DEFAULT_VALUE |
|-------------------|-------------|---------------|---------------|
| `was_config` | `{instanceId}.MANAGEMENT_SERVER_IP` | 관리 서버 IP | `localhost` |
| `was_config` | `{instanceId}.MANAGEMENT_SERVER_PORT` | 관리 서버 포트 | `50005` |
| `was_config` | `{instanceId}.COMM_TYPE` | 통신 방식 | `HTTP` (기본값) |

### 6-2. 등록 예시

```sql
-- biz-auth: TCP 전용 WAS
INSERT INTO FWK_PROPERTY (
    PROPERTY_GROUP_ID, PROPERTY_ID, PROPERTY_NAME, PROPERTY_DESC,
    DATA_TYPE, VALID_DATA, DEFAULT_VALUE,
    LAST_UPDATE_USER_ID, LAST_UPDATE_DTIME
) VALUES (
    'was_config', 'biz-auth.COMM_TYPE',
    'biz-auth 통신방식', 'biz-auth WAS 관리 명령 통신방식 (HTTP/TCP)',
    'C', 'HTTP,TCP', 'TCP',
    'system', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')
);

-- HTTP WAS는 COMM_TYPE 별도 등록 불필요 (기본값 HTTP 사용)
```

### 6-3. COMM_TYPE 기본값 동작

| 등록 여부 | 동작 |
|----------|------|
| `COMM_TYPE = TCP` 등록 | TCP 경로로 Reload |
| `COMM_TYPE = HTTP` 등록 | HTTP 경로로 Reload |
| **미등록** | **HTTP 경로로 Reload (기본값)** |

---

## 7. API 명세

### 7-1. POST /api/log-level/propagate

**권한:** `LOG_LEVEL:W`

**요청 본문**

```json
// 레벨 Reload
{
  "instanceIds": ["biz-auth", "biz-channel"],
  "gubun": "log_config_level",
  "logName": "com.example.svc",
  "level": "DEBUG"
}

// Additivity Reload
{
  "instanceIds": ["biz-auth"],
  "gubun": "log_config_additivity",
  "logName": "com.example.svc",
  "additivity": "N"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `instanceIds` | `string[]` | ✅ | Reload 대상 WAS 인스턴스 ID 목록 |
| `gubun` | `string` | ✅ | `log_config_level` 또는 `log_config_additivity` |
| `logName` | `string` | ✅ | 대상 로거 이름 (패키지명 또는 클래스명) |
| `level` | `string` | — | `ERROR`/`WARN`/`INFO`/`DEBUG`/`TRACE`/`OFF`, `null`이면 부모 상속 |
| `additivity` | `string` | — | `Y` 또는 `N` |

**응답 본문 (HTTP 200)**

```json
{
  "success": true,
  "message": "Reload가 완료되었습니다",
  "data": {
    "reloadType": "log_config_level",
    "results": [
      { "instanceId": "biz-auth",    "instanceName": "인증AP",  "success": true  },
      { "instanceId": "biz-channel", "instanceName": "채널AP",  "success": false,
        "errorMessage": "biz-channel 서버에 연결 중 오류가 발생하였습니다.[host=...,port=...]" }
    ]
  }
}
```

> 개별 WAS Reload가 실패해도 HTTP 응답 자체는 200을 반환한다.  
> 실패 여부는 `results[].success` 값으로 판단한다.

### 7-2. TCP 커맨드 — MANAGEMENT_RELOAD

Admin → TCP WAS 간 전송 포맷

```
[4바이트 길이(int)] + [UTF-8 JSON]

{
  "command": "MANAGEMENT_RELOAD",
  "payload": {
    "gubun":   "log_config_level",
    "logName": "com.example.svc",
    "level":   "DEBUG"
  }
}
```

응답

```json
{
  "command": "MANAGEMENT_RELOAD",
  "success": true,
  "message": "log_config_level 처리 완료",
  "payload": { "logName": "com.example.svc", "level": "DEBUG" }
}
```

---

## 8. 운영 프로세스

### 프로세스 1 — Admin 로그레벨 변경 (자기 자신)

```
1. [Admin UI] 로그레벨 조정 메뉴 진입
2. 검색 조건 입력 후 "조회" — 로거 이름 키워드, 레벨 필터
3. 대상 로거의 Level 드롭다운에서 원하는 레벨 선택
4. "저장" 클릭
5. 확인 팝업에서 확인
6. Admin 자신의 Logback에 즉시 반영
   └─ WAS는 변경되지 않음 (WAS Reload는 별도 프로세스)
```

### 프로세스 2 — WAS Reload

```
1. [Admin UI] 로그레벨 조정 메뉴에서 Reload할 로거 행을 체크박스로 선택
   └─ 헤더 체크박스: 현재 페이지 전체 선택/해제
2. "선택 WAS Reload" 버튼 클릭
3. [WAS 선택 모달] Reload할 WAS 그룹 필터 (선택)
4. 개별 WAS 카드의 체크박스로 대상 선택
5. "Reload 실행" 클릭
6. 결과 화면 확인
   ├─ 성공: "성공" 표시
   └─ 실패: "실패" 표시 + "상세" 링크로 오류 메시지 확인
```

### 프로세스 3 — 신규 TCP WAS 추가

```
1. [DB] FWK_WAS_INSTANCE 테이블에 신규 WAS 등록
   ← DBA 또는 Admin UI의 WAS 관리 메뉴 사용

2. [DB] FWK_PROPERTY 테이블에 통신 설정 INSERT
   ← 개발자가 DB에서 직접 수행

   INSERT INTO FWK_PROPERTY VALUES
     ('was_config', '{instanceId}.MANAGEMENT_SERVER_IP',   ..., '{ip}'),
     ('was_config', '{instanceId}.MANAGEMENT_SERVER_PORT', ..., '{tcpPort}'),
     ('was_config', '{instanceId}.COMM_TYPE',              ..., 'TCP');

3. [WAS 프로젝트] spider-link 의존성 추가
   pom.xml:
   <dependency>
     <groupId>com.example</groupId>
     <artifactId>spider-link</artifactId>
   </dependency>

4. SpiderLinkAutoConfiguration이 자동으로 아래 빈 등록
   ├─ LogLevelApplier
   ├─ LogLevelExecutor          (gubun=log_config_level)
   ├─ LogAdditivityExecutor     (gubun=log_config_additivity)
   └─ ManagementReloadCommandHandler (command=MANAGEMENT_RELOAD)

5. BizAuthConfig (또는 해당 WAS의 설정 클래스)에서
   List<CommandHandler> 주입 → CommandDispatcher 생성
   ← ManagementReloadCommandHandler 자동 포함

6. Admin UI의 "선택 WAS Reload"에서 즉시 사용 가능
   ← 추가 코드 작성 불필요
```

### 프로세스 4 — 신규 HTTP WAS 추가

```
1. [DB] FWK_WAS_INSTANCE 등록
2. [DB] FWK_PROPERTY에 IP, PORT 등록
   ← COMM_TYPE 등록 불필요 (기본값 HTTP 적용)

3. [WAS 프로젝트] spider-link 의존성 추가
   ← LogLevelReloadController (/api/internal/log/level)
      이 AutoConfig으로 자동 등록됨

4. [WAS 프로젝트] InternalApiInterceptor 허용 IP 설정
   application.yml:
   internal.allowed-ips: ${INTERNAL_ALLOWED_IPS:127.0.0.1,{admin-server-ip}}

5. Admin UI에서 즉시 사용 가능
```

### 프로세스 5 — 장애 대응

#### Reload 실패 시

```
증상: WAS 선택 모달 결과 화면에서 "실패" 표시
조치:
  1. "상세" 링크 클릭 → 오류 메시지 확인
     ├─ "연결 중 오류" → WAS 서버 기동 상태 확인
     ├─ "인스턴스를 찾을 수 없습니다" → FWK_WAS_INSTANCE 등록 확인
     └─ "지원하지 않는 gubun" → ManagementExecutor 빈 등록 확인
  2. WAS 서버 재기동 후 재시도
  3. 긴급 시: 해당 WAS 서버에 직접 접속하여 로그레벨 변경
```

#### Admin 변경은 성공, WAS Reload는 실패하는 경우

```
원인: Admin과 WAS가 각각 독립적으로 Logback을 관리함
      서버 재시작 시 WAS의 로그레벨도 초기화됨

조치:
  1. WAS 서버 상태 확인 후 재Reload
  2. Reload가 지속적으로 실패하면 네트워크/방화벽 정책 확인
  3. FWK_PROPERTY의 IP, PORT, COMM_TYPE 값 확인
```

---

## 9. 확장 가이드

### 9-1. 신규 관리 명령 타입 추가 (WAS 프로젝트에서)

```java
// 1. ManagementExecutor 구현체 작성
@Component
public class BatchReloadExecutor implements ManagementExecutor {

    @Override
    public boolean supports(String gubun) {
        return "batch_reload".equals(gubun);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String jobName = (String) params.get("jobName");
        // 배치 리로드 처리
        return Map.of("jobName", jobName, "status", "reloaded");
    }
}
```

```java
// 2. admin ReloadType에 신규 gubun 추가
BATCH_RELOAD("batch_reload", "배치 리로드", "배치 설정을 WAS에 Reload한다.", true)
```

```
3. Admin UI의 운영정보 Reload 메뉴에 자동으로 목록 추가
   ← visible=true 설정 시
   ← HTTP, TCP 양쪽 경로 모두 자동 지원
```

### 9-2. 기본 제공 외 로그 관련 명령 추가

```java
// 예: 로거 전체 초기화 명령
@Component
public class LoggerResetExecutor implements ManagementExecutor {

    private final LogLevelApplier logLevelApplier;

    @Override
    public boolean supports(String gubun) {
        return "log_reset_all".equals(gubun);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        // LogLevelApplier 를 활용하거나 직접 LoggerContext 조작
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLoggerList().forEach(l -> l.setLevel(null));
        return Map.of("status", "all loggers reset to inherited");
    }
}
```

### 9-3. 기존 WAS 구현체 재정의

Auto-Configuration은 `@ConditionalOnMissingBean`을 사용하므로, WAS 프로젝트에서 같은 타입의 빈을 직접 정의하면 Auto-Configuration 빈이 등록되지 않는다.

```java
// spider-link의 LogLevelApplier 대신 커스텀 구현 사용
@Bean
public LogLevelApplier logLevelApplier() {
    return new CustomLogLevelApplier(); // LogLevelApplier 상속 또는 동일 타입
}
```

---

## 10. 관련 파일 목록

### admin

| 경로 | 역할 |
|------|------|
| `domain/loglevel/controller/LogLevelController.java` | REST 엔드포인트 (PATCH 변경 + POST Reload) |
| `domain/loglevel/service/LogLevelService.java` | Admin Logback 직접 변경 |
| `domain/loglevel/service/LogLevelPropagationService.java` | WAS Reload 서비스 |
| `domain/loglevel/dto/LogLevelPropagateRequest.java` | Reload 요청 DTO |
| `domain/loglevel/dto/LogLevelUpdateRequest.java` | 레벨 변경 요청 DTO |
| `domain/loglevel/dto/AdditivityUpdateRequest.java` | Additivity 변경 요청 DTO |
| `domain/reload/service/ReloadService.java` | HTTP/TCP 분기 Reload 실행 |
| `domain/reload/enums/ReloadType.java` | LOG_LEVEL, LOG_ADDITIVITY 포함 |
| `infra/tcp/client/TcpClient.java` | TCP JSON 통신 클라이언트 |
| `infra/tcp/model/JsonCommandRequest.java` | TCP 요청 모델 |
| `infra/tcp/model/JsonCommandResponse.java` | TCP 응답 모델 |
| `resources/templates/pages/log-level-manage/` | 로그레벨 조정 UI 템플릿 |
| `resources/static/js/components/WasSelectReloadModal.js` | WAS 선택 모달 컴포넌트 |
| `docs/sql/oracle/03_insert_initial_data.sql` | COMM_TYPE 초기 데이터 |
| `e2e/api/log-level.spec.ts` | API 계약 테스트 |

### spider-link

| 경로 | 역할 |
|------|------|
| `config/SpiderLinkAutoConfiguration.java` | 빈 자동 등록 |
| `resources/META-INF/spring/...AutoConfiguration.imports` | Auto-Configuration 진입점 |
| `domain/loglevel/LogLevelApplier.java` | Logback 조작 공통 로직 |
| `domain/loglevel/LogLevelReloadController.java` | HTTP WAS용 내부 API |
| `domain/management/ManagementReloadCommandHandler.java` | TCP 커맨드 수신 핸들러 |
| `domain/management/executor/ManagementExecutor.java` | 전략 인터페이스 |
| `domain/management/executor/LogLevelExecutor.java` | log_config_level 처리 |
| `domain/management/executor/LogAdditivityExecutor.java` | log_config_additivity 처리 |
