# spider-common

spider-link·spider-batch·admin이 공통으로 참조하는 공유 인프라 **Maven 라이브러리**.
TCP 프로토콜 모델, 관리 명령 처리(로그레벨·메타 리로드), IP 기반 내부 API 보호를 제공한다.

## 목차

- [역할](#역할)
- [내장 방법](#내장-방법)
- [핵심 컴포넌트](#핵심-컴포넌트)
- [API 엔드포인트](#api-엔드포인트-servlet-환경)
- [Project Structure](#project-structure)
- [빌드 & 설치](#빌드--설치)

---

## 역할

원래 spider-link에 있던 공유 코드를 분리한 모듈이다. 여러 WAS가 동일한 클래스를 참조해야 할 때 발생하는 중복 정의 문제(특히 Java 직렬화에서 `serialVersionUID` 불일치)를 방지한다.

| 책임 영역 | 내용 |
|----------|------|
| TCP 프로토콜 모델 | `JsonCommandRequest`, `JsonCommandResponse`, `ManagementContext`, `CommandHandler`, `CommandDispatcher` |
| 관리 명령 처리 | `MANAGEMENT_RELOAD` TCP 커맨드, `/api/management/reload` HTTP 엔드포인트, 로그레벨·Additivity 조정 |
| 내부 API 보호 | `InternalApiInterceptor` — `/api/internal/**`, `/api/management/**` IP 화이트리스트 |
| AutoConfiguration | Servlet 환경 유무에 따라 bean을 분리 등록 (`SpiderCommonAutoConfiguration` / `SpiderCommonServletAutoConfiguration`) |

## 내장 방법

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spider-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Spring Boot AutoConfiguration에 의해 자동으로 Bean이 등록된다.

## 핵심 컴포넌트

### TCP 프로토콜 모델 (`infra.tcp.model`)

| 클래스 | 용도 |
|--------|------|
| `JsonCommandRequest` | Admin ↔ AP 서버 TCP 요청 (`command`, `requestId`, `payload<Map>`) |
| `JsonCommandResponse` | TCP 응답 (`success`, `message`, `error`, `payload`) |
| `ManagementContext` | Admin ↔ spider-batch 배치 실행 명령 (Java ObjectStream 직렬화, `serialVersionUID=1L`) |
| `HasCommand` | `getCommand()` 마커 인터페이스 — `CommandDispatcher` 라우팅 기준 |
| `CommandRequest<T>` | `JsonCommandRequest`가 상속하는 추상 기반 클래스 |

### CommandDispatcher / CommandHandler (`infra.tcp.handler`)

```java
// AP 서버 @Configuration에서 직접 생성 (Spring Bean 아님)
CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
        new CommandDispatcher<>(List.of(handler1, handler2));
```

- `CommandHandler<REQ, RES>` — AP 서버가 구현하는 SPI 인터페이스
  - `supports(command)`: 처리 가능 여부 판단
  - `handle(command, request)`: 커맨드 처리 및 응답 반환
- `CommandDispatcher` — `supports()`로 핸들러를 선택하여 위임하는 전략 라우터

### 관리 명령 처리 (`domain.management`)

`MANAGEMENT_RELOAD` TCP 커맨드를 받으면 `gubun` 필드로 `ManagementExecutor`를 선택하여 실행한다.

| `gubun` 값 | 실행기 | 동작 |
|------------|--------|------|
| `log_config_level` | `LogLevelExecutor` | Logback 로거 레벨 런타임 변경 |
| `log_config_additivity` | `LogAdditivityExecutor` | Logback 로거 Additivity 런타임 변경 |

AP 서버는 `ManagementExecutor`를 구현·등록하여 자체 리로드 동작을 추가할 수 있다.

### 내부 API 보호 (`config.InternalApiInterceptor`)

`/api/internal/**`, `/api/management/**` 경로를 허용된 IP에서만 접근할 수 있도록 제한한다.

```yaml
internal-api:
  allowed-ips:
    - 127.0.0.1
    - ::1
```

### AutoConfiguration 분리 전략

| 클래스 | 활성화 조건 | 등록 Bean |
|--------|------------|-----------|
| `SpiderCommonAutoConfiguration` | 항상 | `LogLevelApplier`, `LogLevelExecutor`, `LogAdditivityExecutor`, `ManagementReloadCommandHandler` |
| `SpiderCommonServletAutoConfiguration` | `@ConditionalOnWebApplication(SERVLET)` | `LogLevelReloadController`, `ManagementReloadHttpController`, `InternalApiInterceptor`, `WebMvcConfig` |

TCP-only WAS에서 Servlet 의존성이 없을 때 `NoClassDefFoundError`가 발생하지 않도록 Servlet Bean을 별도 클래스로 분리한다.

## API 엔드포인트 (Servlet 환경)

| 메서드 | 경로 | 용도 |
|--------|------|------|
| `POST` | `/api/management/reload` | 관리 명령 실행 (`gubun` + `params`) |
| `POST` | `/api/internal/log/level` | 로그 레벨 변경 |
| `POST` | `/api/internal/log/additivity` | 로그 Additivity 변경 |

모든 엔드포인트는 `InternalApiInterceptor`로 IP 보호된다.

## Project Structure

```
src/main/java/com/example/spidercommon/
├── config/
│   ├── SpiderCommonAutoConfiguration.java         # 핵심 Bean 자동 등록
│   ├── SpiderCommonServletAutoConfiguration.java  # Servlet Bean 자동 등록 (분리)
│   ├── InternalApiInterceptor.java                # /api/internal·management/** IP 제한
│   └── WebMvcConfig.java                          # 인터셉터 경로 등록
├── domain/
│   ├── loglevel/
│   │   ├── LogLevelApplier.java                   # Logback 런타임 레벨·Additivity 조작
│   │   └── LogLevelReloadController.java          # POST /api/internal/log/**
│   └── management/
│       ├── ManagementReloadCommandHandler.java    # TCP MANAGEMENT_RELOAD 핸들러
│       ├── ManagementReloadHttpController.java    # POST /api/management/reload
│       └── executor/
│           ├── ManagementExecutor.java            # 전략 인터페이스 (SPI)
│           ├── LogLevelExecutor.java              # log_config_level 실행기
│           └── LogAdditivityExecutor.java         # log_config_additivity 실행기
└── infra/tcp/
    ├── handler/
    │   ├── CommandHandler.java                    # AP 서버 SPI 인터페이스
    │   └── CommandDispatcher.java                 # 커맨드 → 핸들러 전략 라우터
    └── model/
        ├── HasCommand.java                        # getCommand() 마커 인터페이스
        ├── CommandRequest.java                    # 추상 기반 클래스
        ├── JsonCommandRequest.java                # Admin ↔ AP 서버 TCP 요청
        ├── JsonCommandResponse.java               # TCP 응답
        └── ManagementContext.java                 # Admin ↔ spider-batch 배치 명령 (ObjectStream)
```

## 빌드 & 설치

spider-link·spider-batch보다 먼저 로컬 저장소에 설치해야 한다.

```bash
cd spider-common
./mvnw clean install
```
