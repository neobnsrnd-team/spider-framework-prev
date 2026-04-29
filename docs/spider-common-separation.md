# spider-common 모듈 분리 설계 문서

## 1. 배경 — AS-IS와의 비교

### AS-IS (ASIS / DGB Spider Framework) 모듈 구조

ASIS와 DGB 프로젝트의 Spider Framework는 라이브러리 계층을 두 모듈로 명확히 분리하고 있다.

```
src_common/          ← 공통 관리/인프라 계층
  ├── TCP 프로토콜 모델 (ManagementContext 등)
  ├── 관리 커맨드 처리 (Management* 클래스)
  └── 로그 관리 (LogManager)

src_link/            ← 메시지 엔진 계층 (src_common 의존)
  ├── TCP 서버 (SpiderTcpServer)
  ├── TCP 클라이언트 (TcpClient)
  ├── 메시지 코덱 (Json/ObjectStream)
  └── 메타 기반 라우팅 (MetaDrivenCommandHandler)
```

각 AP 서버(WAS)는 두 라이브러리를 모두 내장한다.
`src_common`은 독립적으로도 사용 가능하며, `src_link`는 `src_common`에 의존한다.

---

### TO-BE 이전 (POC_HNC 초기) 구조

POC_HNC 초기 구현에서는 `src_common`에 해당하는 기능이 `spider-link` 단일 모듈 안에 혼재되어 있었다.

```
spider-link/
  ├── infra/tcp/model/         ← AS-IS src_common 영역 (TCP 프로토콜 모델)
  ├── infra/tcp/handler/       ← AS-IS src_common 영역 (CommandHandler 프레임워크)
  ├── domain/loglevel/         ← AS-IS src_common 영역 (로그레벨 조정)
  ├── domain/management/       ← AS-IS src_common 영역 (관리 커맨드 처리)
  ├── config/                  ← AS-IS src_common 영역 (내부 API 인터셉터)
  │
  ├── infra/tcp/server/        ← AS-IS src_link 영역 (TCP 서버)
  ├── infra/tcp/client/        ← AS-IS src_link 영역 (TCP 클라이언트)
  ├── infra/tcp/codec/         ← AS-IS src_link 영역 (메시지 코덱)
  ├── infra/tcp/parser/        ← AS-IS src_link 영역 (고정 길이 파싱)
  └── domain/messageinstance/  ← AS-IS src_link 영역 (메시지 이력)
```

**문제점**
- 관리/인프라 기능(Management, LogLevel)과 메시지 엔진(TcpServer, Codec)이 단일 모듈에 뒤섞임
- `spider-batch`처럼 TCP 서버 기능은 필요 없고 관리 기능만 필요한 소비자도 `spider-link` 전체에 의존해야 함
- AS-IS가 의도한 계층 분리 원칙을 위반

---

## 2. 분리 목적

| 목적 | 설명 |
|------|------|
| **AS-IS 철학 반영** | ASIS/DGB의 src_common + src_link 분리 구조를 POC_HNC에 동일하게 적용 |
| **단일 책임** | 관리/인프라(공통)와 메시지 엔진(링크 전용)을 명확히 구분 |
| **의존성 최소화** | TCP 서버가 없는 batch-was도 관리 기능만 가볍게 사용 가능 |
| **독립 버전 관리** | 두 모듈을 독립적으로 버전업·배포 가능 |

---

## 3. TO-BE 분리 구조

### 모듈 개요

```
spider-common   (공통 관리/인프라 라이브러리, 순수 JAR)
      ↑
spider-link     (메시지 엔진 라이브러리, spider-common 의존)
```

### 3-1. spider-common

**패키지**: `com.example.spidercommon`  
**역할**: TCP 프로토콜 모델 정의, 관리 커맨드 처리, 로그레벨 조정 기능 제공  
**배포 형태**: 순수 JAR (spring-boot-maven-plugin 미사용)

```
spider-common/src/main/java/com/example/spidercommon/
│
├── infra/tcp/
│   ├── model/
│   │   ├── HasCommand.java              TCP 커맨드 식별 인터페이스
│   │   ├── CommandRequest.java          커맨드 요청 기본 타입
│   │   ├── JsonCommandRequest.java      JSON 기반 커맨드 요청 (command + payload)
│   │   ├── JsonCommandResponse.java     JSON 기반 커맨드 응답 (success/error/data)
│   │   └── ManagementContext.java       Admin→WAS 관리 명령 전달 모델 (ObjectStream 직렬화)
│   └── handler/
│       ├── CommandHandler.java          커맨드 처리기 인터페이스
│       └── CommandDispatcher.java       커맨드→핸들러 라우팅 (supports() 패턴)
│
├── domain/
│   ├── loglevel/
│   │   ├── LogLevelApplier.java         Logback LoggerContext 런타임 조작 (applyLevel/applyAdditivity)
│   │   └── LogLevelReloadController.java HTTP GET /internal/log/level — 현재 로거 목록 조회
│   └── management/
│       ├── executor/
│       │   ├── ManagementExecutor.java      관리 실행기 전략 인터페이스 (supports + execute)
│       │   ├── LogLevelExecutor.java        gubun=log_config_level 처리
│       │   └── LogAdditivityExecutor.java   gubun=log_config_additivity 처리
│       ├── ManagementReloadCommandHandler.java  TCP MANAGEMENT_RELOAD 커맨드 수신·처리
│       └── ManagementReloadHttpController.java  HTTP POST /api/management/reload 수신·처리
│
└── config/
    ├── InternalApiInterceptor.java          /api/internal/**, /api/management/** 내부망 접근 제한
    ├── WebMvcConfig.java                    인터셉터 등록
    ├── SpiderCommonAutoConfiguration.java   LogLevelApplier, Executor, Handler 자동 등록
    └── SpiderCommonServletAutoConfiguration.java  Servlet 환경 한정 Controller/Interceptor 등록
```

**AutoConfiguration 등록 파일**
```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  com.example.spidercommon.config.SpiderCommonAutoConfiguration
  com.example.spidercommon.config.SpiderCommonServletAutoConfiguration
```

`SpiderCommonServletAutoConfiguration`은 `@ConditionalOnWebApplication(type=SERVLET)`이 적용되어
TCP 전용 WAS(web-application-type=none)에서는 HTTP 컨트롤러가 등록되지 않는다.

---

### 3-2. spider-link

**패키지**: `com.example.spiderlink`  
**역할**: TCP 메시지 엔진 — 고정 길이/JSON 메시지 파싱·라우팅·이력 기록  
**배포 형태**: 실행 가능 JAR + 라이브러리 JAR (exec 분리)

```
spider-link/src/main/java/com/example/spiderlink/
│
├── infra/tcp/
│   ├── server/
│   │   └── SpiderTcpServer.java         멀티스레드 TCP 서버 (CommandDispatcher 위임)
│   ├── client/
│   │   └── TcpClient.java               TCP 클라이언트 (ObjectStream/JSON 전송)
│   ├── codec/
│   │   ├── MessageCodec.java            코덱 인터페이스
│   │   ├── JsonMessageCodec.java        JSON 인코딩/디코딩
│   │   └── ObjectStreamMessageCodec.java  Java 직렬화 인코딩/디코딩 (역직렬화 필터 포함)
│   ├── parser/
│   │   ├── FixedLengthParser.java       고정 길이 전문 파싱
│   │   └── ...                          파서 지원 클래스들
│   ├── handler/
│   │   └── MetaDrivenCommandHandler.java  DB 메타 기반 동적 라우팅 핸들러
│   └── biz/
│       ├── Biz.java                     비즈 처리 인터페이스
│       └── TcpCallBiz.java              TCP 호출 비즈 구현
│
├── domain/
│   ├── messageinstance/
│   │   └── MessageInstanceRecorder.java  메시지 이력 DB 기록
│   ├── sqlquery/
│   │   └── SqlQueryLoader.java           SQL 쿼리 메타 로드·리로드
│   └── ...
│
└── config/
    └── SpiderLinkAutoConfiguration.java  MessageInstanceRecorder 자동 등록
```

---

### 3-3. 의존 관계 전체 그림

```
                    spider-common
                   (공통 관리/인프라)
                        │
          ┌─────────────┼─────────────────┐
          ↓             ↓                 ↓
      spider-link   spider-batch       (직접 의존)
    (메시지 엔진)    (배치 오케스트레이션)
          │             │
          └──────┬───────┘
                 ↓
         소비자 (AP 서버들)
    ┌────────────┴──────────────┐
    ↓                           ↓
  demo/bizApp                 admin
(biz-auth, biz-transfer,   (관리 콘솔)
 biz-channel, mock-core)
```

**pom.xml 의존성 선언 기준**

| 모듈 | spider-common | spider-link |
|------|:---:|:---:|
| spider-link | ✅ 직접 | — |
| spider-batch | ✅ 직접 | ✅ 직접 |
| demo/bizApp (각 서버) | — (전이) | ✅ 직접 |
| admin | ✅ 직접 | ✅ 직접 |

`demo/bizApp` 하위 모듈은 spider-link를 통해 spider-common을 전이 의존하므로
spider-common을 별도로 선언하지 않아도 된다.
`admin`과 `spider-batch`는 spider-common 클래스를 직접 참조하므로 명시적으로 선언한다.

---

## 4. 핵심 설계 결정

### ManagementReloadCommandHandler의 위치

초기에는 `ManagementReloadCommandHandler`가 `CommandHandler` 인터페이스(spider-link)를 구현하므로
spider-link에 두는 것이 자연스러워 보였다.
그러나 이 경우 spider-common의 `ManagementExecutor` 계열 클래스들이 spider-link를 역방향 참조하는
순환 의존이 발생한다.

**해결**: `CommandHandler` 인터페이스와 `CommandDispatcher`를 spider-common으로 함께 이동.
TCP 프로토콜 프레임워크 전체(모델 + 핸들러 인터페이스)를 spider-common에 두고,
spider-link는 그 위에서 구체적인 엔진 구현만 담당한다.

```
[AS-IS 문제]
spider-common → ManagementReloadCommandHandler → CommandHandler (spider-link) → 순환!

[TO-BE 해결]
spider-common → CommandHandler (spider-common 내부)
spider-link   → CommandHandler (spider-common)
```

### ASIS LogManager vs. POC LogLevelApplier

ASIS의 `LogManager`는 로그 레벨 조회·변경·영속화를 담당하는 독립 컴포넌트이다.
POC_HNC는 현재 `LogLevelApplier`가 Logback `LoggerContext`를 직접 조작하는 단순 유틸리티 수준이며,
영속화(DB 저장·조회)는 admin에서 담당한다.

이 차이는 POC_HNC가 Admin 콘솔과 WAS를 명확히 분리한 아키텍처를 취하기 때문이며,
분리 단계에서 LogManager 기능 전체를 이식하지 않고 핵심 런타임 조작 기능만 spider-common에 포함한 것은 의도된 결정이다.

---

## 5. 빌드 순서

```
# 1단계: spider-common (의존성 없음, 항상 가장 먼저)
cd spider-common
mvn install -DskipTests

# 2단계: spider-link (spider-common에 의존)
cd spider-link
mvn install -DskipTests

# 3단계: 각 소비자 (spider-link 또는 spider-batch에 의존)
cd demo/bizApp && mvn install -DskipTests
cd batch-was   && mvn install -DskipTests
cd admin       && mvn install -DskipTests
```

**재빌드 판단 기준**

| 변경 내용 | spider-common | spider-link | 소비자 |
|-----------|:---:|:---:|:---:|
| spider-common 내부 로직 변경 | ✅ 재빌드 | 불필요 | ✅ 재빌드 |
| spider-common public API 변경 (spider-link가 사용) | ✅ 재빌드 | ✅ 재빌드 | ✅ 재빌드 |
| spider-link 내부 로직 변경 | 불필요 | ✅ 재빌드 | ✅ 재빌드 |

---

## 6. 패키지 네이밍

| 모듈 | 루트 패키지 |
|------|------------|
| spider-common | `com.example.spidercommon` |
| spider-link | `com.example.spiderlink` |

분리 전 spider-link의 패키지(`com.example.spiderlink.infra.tcp.model.*` 등)에서
spider-common으로 이동된 클래스들은 패키지가 `com.example.spidercommon.*`으로 변경됐다.
Java ObjectStream 역직렬화 필터에 사용되는 FQCN 문자열도 함께 변경 적용됐다.

```java
// ObjectStreamMessageCodec.java — 역직렬화 허용 목록 (FQCN 문자열)
"com.example.spidercommon.infra.tcp.model.ManagementContext;java.lang.String;java.util.**;!*"
```
