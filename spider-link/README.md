# spider-link

AP 서버에 내장되는 공통 연계엔진 Maven 라이브러리.
`SpiderTcpServer`를 중심으로 TCP 커맨드 수신·처리·응답 흐름을 제공하며, FWK 메타테이블 기반 동적 라우팅을 지원한다.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.4 |
| ORM | MyBatis 3 |
| Database | Oracle (optional) |
| Build | Maven 3 (Wrapper) |
| Packaging | 라이브러리 JAR + 실행 JAR(`*-exec.jar`) 동시 생성 |

## 사용 방법

### 내장 방식 (권장)

AP 서버(`biz-channel`, `biz-auth`, `biz-transfer` 등)의 `pom.xml`에 의존성을 추가한다.

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spider-link</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

SpiderLinkAutoConfiguration이 자동 활성화되며(`@ConditionalOnClass(JdbcTemplate.class)`),
AP 서버의 `@Configuration`에서 `SpiderTcpServer` Bean을 직접 등록하여 포트·핸들러를 구성한다.

```java
@Bean
public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> bizChannelTcpServer(
        @Value("${tcp.server.port}") int port,
        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher,
        ObjectMapper objectMapper) {
    return new SpiderTcpServer<>(port, 20, 100, new JsonMessageCodec(objectMapper), dispatcher);
}
```

### Standalone 방식 (레거시)

```bash
mvn install -f spider-link/pom.xml
java -jar spider-link-exec.jar
```

`DemoTcpConfig`가 `tcp.demo-server.enabled=true`일 때만 활성화되며, standalone 모드에서만 사용한다.

## 핵심 컴포넌트

### SpiderTcpServer

TCP 서버의 핵심. `ApplicationRunner`를 구현하여 컨텍스트 로드 완료 후 자동 기동한다.

- `ThreadPoolExecutor(ArrayBlockingQueue)` 기반 스레드 풀로 OOM 방지
- `MessageCodec`으로 역직렬화 → `CommandDispatcher`에 위임 → 직렬화 응답
- `MessageInstanceRecorder` 주입 시 전문 이력을 `FWK_MESSAGE_INSTANCE` 테이블에 비동기 기록
- `@PreDestroy`에서 ServerSocket 닫기 + 30초 graceful shutdown

```
수신 → codec.decode() → dispatcher.dispatch() → commandHandler.handle() → codec.encode() → 송신
```

### CommandHandler (SPI 인터페이스)

```java
public interface CommandHandler<REQ, RES> {
    boolean supports(String command);
    RES handle(String command, REQ request);
}
```

`CommandDispatcher`가 `supports()`로 적합한 핸들러를 선택하여 위임한다.

### MetaDrivenCommandHandler

FWK 메타테이블을 기반으로 커맨드를 동적으로 실행한다.

```
FWK_LISTENER_TRX_MESSAGE
    → FWK_SERVICE
        → FWK_SERVICE_RELATION
            → FWK_COMPONENT (COMPONENT_TYPE: S/U/B)
```

| COMPONENT_TYPE | 동작 |
|----------------|------|
| `S` | SELECT 쿼리 실행 |
| `U` | UPDATE/INSERT/DELETE 실행 |
| `B` | Biz 클래스 리플렉션 호출 |

`refreshCommands()`: Admin `POST /api/management/reload?gubun=request_app_mapping` 호출로 재기동 없이 커맨드 캐시 갱신.

### 지원 프로토콜 (MessageCodec)

| 코덱 | 포맷 | 사용 구간 |
|------|------|----------|
| `JsonMessageCodec` | 4바이트 길이 프리픽스 + UTF-8 JSON | Admin ↔ AP 서버 |
| `FixedLengthMessageCodec` | 고정 길이 바이너리 | biz-auth/transfer ↔ mock-core |
| `ObjectStreamMessageCodec` | Java 직렬화 | Admin ↔ batch-was |

### AutoConfiguration 자동 등록 Bean

`SpiderLinkAutoConfiguration`이 자동 등록하는 Bean:

| Bean | 역할 |
|------|------|
| `SocketPoolManager` | AP 서버 간 TCP 소켓 풀 관리 |
| `MessageLogQueue` | 비동기 전문 이력 큐 컨슈머 |
| `MessageInstanceRecorder` | `FWK_MESSAGE_INSTANCE` 이력 기록기 |

## API 엔드포인트

| 메서드 | 경로 | 용도 |
|--------|------|------|
| `POST` | `/api/management/reload` | 메타 캐시 갱신 (`gubun=request_app_mapping` 또는 `message`) |
| `GET` | `/actuator/health` | 헬스체크 |

`/api/management/**`는 `InternalApiInterceptor`가 localhost(127.0.0.1, ::1)만 허용한다.

## Project Structure

```
src/main/java/com/example/spiderlink/
├── config/
│   ├── SpiderLinkAutoConfiguration.java   # AutoConfiguration 진입점
│   ├── SpiderLinkMessageConfig.java       # MessageStructurePool Bean
│   ├── DemoTcpConfig.java                 # standalone 모드용 TCP 서버 설정 (레거시)
│   ├── InternalApiInterceptor.java        # /api/management/** IP 제한
│   └── WebMvcConfig.java
├── domain/
│   ├── message/                           # FWK_MESSAGE 메타 (전문 구조 정의)
│   ├── messageinstance/                   # FWK_MESSAGE_INSTANCE 이력 기록
│   ├── meta/                              # FWK_SERVICE·RELATION·COMPONENT 메타 조회
│   └── sqlquery/                          # 동적 SQL 로딩 및 캐시
├── infra/
│   ├── http/
│   │   ├── InternalManagementController.java
│   │   └── SocketPoolStatusController.java
│   └── tcp/
│       ├── server/SpiderTcpServer.java    # 핵심 TCP 서버
│       ├── handler/
│       │   ├── CommandHandler.java        # SPI 인터페이스
│       │   ├── CommandDispatcher.java
│       │   └── MetaDrivenCommandHandler.java
│       ├── codec/                         # Json / FixedLength / ObjectStream
│       ├── client/                        # TcpClient + SocketPool
│       ├── model/                         # ManagementContext, JsonCommandRequest/Response
│       └── parser/                        # FixedLengthParser, MessageStructure
└── SpiderLinkApplication.java             # standalone 실행 진입점 (레거시)
```

## 빌드 & 설치

다른 모듈에서 참조하기 전에 반드시 로컬 저장소에 먼저 설치해야 한다.

```bash
cd spider-link
./mvnw clean install
```

실행 JAR만 필요하면:

```bash
./mvnw spring-boot:run
```

## 주요 설정 (Standalone 모드, `application.yml`)

| 설정 키 | 기본값 | 설명 |
|--------|--------|------|
| `server.port` | `8082` | HTTP 포트 (Actuator 헬스체크용) |
| `tcp.server.port` | `9996` | Admin이 커맨드를 전송하는 TCP 포트 |
| `tcp.server.handler-pool-size` | `20` | 핸들러 스레드 풀 크기 |
| `tcp.ext.host/port` | `localhost:19100` | 외부 시스템 TCP 연동 대상 |
| `internal-api.allowed-ips` | `127.0.0.1`, `::1` | 관리 API 허용 IP |
