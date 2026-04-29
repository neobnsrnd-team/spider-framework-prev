# demo/bizApp

하나카드 POC 시연용 금융 AP 서버군. `spider-link`를 라이브러리로 내장한 멀티모듈 Maven 프로젝트.
채널AP(HTTP 게이트웨이), 인증AP, 이체AP, 계정계 Mock으로 구성되며, Admin이 TCP로 공지를 브로드캐스트하는 구조다.

## 아키텍처

```
[프론트엔드]
  React (demo/front)
       │ HTTP (18080)
       ▼
[채널AP] biz-channel ────── TCP 19400 ◄── Admin (긴급공지 명령)
       │
       ├── TCP 19100 ──► [인증AP] biz-auth
       │                          │ TCP 19300
       │                          ▼
       │                   [계정계] mock-core
       │
       └── TCP 19200 ──► [이체AP] biz-transfer
                                  │ TCP 19300
                                  ▼
                           [계정계] mock-core
```

## 모듈 구성

| 모듈 | 포트 | 역할 |
|------|------|------|
| `biz-common` | — | AP 서버 간 TCP 커맨드 상수 라이브러리 |
| `mock-core` | TCP 19300 | 계정계 레거시 Mock (고정 길이 바이너리 TCP) |
| `biz-auth` | TCP 19100, HTTP 19180 | 인증AP (AUTH_LOGIN, AUTH_ME) |
| `biz-transfer` | TCP 19200, HTTP 19280 | 이체AP (카드 조회·이용내역·즉시결제 등) |
| `biz-channel` | HTTP 18080, TCP 19400 | 채널AP (REST API 게이트웨이, JWT, SSE 공지) |

## 빌드 순서 및 사전 조건

```bash
# 1. spider-link 설치 (필수 선행 작업)
mvn install -f spider-link/pom.xml

# 2. 각 모듈 빌드 (루트에서 일괄 빌드)
cd demo/bizApp
./mvnw clean package

# 또는 모듈별 개별 빌드
./mvnw clean package -pl biz-channel
```

## 모듈 상세

### biz-common

AP 서버 간 TCP 커맨드 이름을 상수로 정의한 공유 라이브러리.

```java
// BizCommands.java
AUTH_LOGIN, AUTH_ME                                          // biz-auth (19100)
TRANSFER_CARD_LIST, TRANSFER_TRANSACTIONS,                  // biz-transfer (19200)
TRANSFER_PAYMENT_STMT, TRANSFER_PAYABLE_AMT,
TRANSFER_IMMEDIATE_PAY
CORE_USER_AUTH, CORE_USER_QUERY, CORE_CARD_LIST, ...        // mock-core (19300)
```

---

### mock-core

계정계 레거시 시스템을 모사하는 Mock 서버. 고정 길이 바이너리 TCP 프로토콜로 통신한다.

- **프로토콜**: 4바이트 길이 프리픽스(int, big-endian) + 고정 길이 바이너리 (첫 20바이트 = 커맨드명)
- **DB**: Oracle D_SPIDERLINK 스키마 (BCrypt 패스워드 검증: spring-security-crypto)

| 핸들러 | 처리 커맨드 |
|--------|------------|
| `CoreUserAuthHandler` | `CORE_USER_AUTH` — 사용자 인증 (BCrypt 검증) |
| `CoreUserQueryHandler` | `CORE_USER_QUERY` — 사용자 정보 조회 |
| `CoreCardListHandler` | `CORE_CARD_LIST` — 카드 목록 |
| `CoreTransactionsHandler` | `CORE_TRANSACTIONS` — 이용내역 |
| `CorePaymentStmtHandler` | `CORE_PAYMENT_STMT` — 청구 명세서 |
| `CorePayableAmtHandler` | `CORE_PAYABLE_AMT` — 즉시결제 가능금액 |
| `CoreImmediatePayHandler` | `CORE_IMMEDIATE_PAY` — 즉시결제 처리 |

```bash
# 실행
cd mock-core
./mvnw spring-boot:run
```

---

### biz-auth

인증AP. biz-channel로부터 `AUTH_*` 커맨드를 수신하여 mock-core(19300)로 중계한다.

- **TCP 19100**: spider-link `SpiderTcpServer` 내장, `MetaDrivenCommandHandler`로 FWK 메타 기반 처리
- **HTTP 19180**: Admin reload API (`POST /api/management/reload`) 수신

```bash
cp biz-auth/.env.example biz-auth/.env   # DB, TCP 포트 설정
./mvnw spring-boot:run -pl biz-auth
```

주요 환경변수:

| 변수 | 설명 |
|------|------|
| `TCP_SERVER_PORT` | TCP 서버 포트 (기본 19100) |
| `HTTP_SERVER_PORT` | HTTP 포트 (기본 19180) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Oracle 접속 정보 |

---

### biz-transfer

이체AP. biz-channel로부터 `TRANSFER_*` 커맨드를 수신하여 mock-core(19300)로 중계한다.

- **TCP 19200**: spider-link `SpiderTcpServer` 내장, `MetaDrivenCommandHandler` + `TransferImmediatePayHandler`(PIN 검증) 병행
- **HTTP 19280**: Admin reload API 수신

```bash
cp biz-transfer/.env.example biz-transfer/.env
./mvnw spring-boot:run -pl biz-transfer
```

---

### biz-channel

채널AP. React 프론트엔드의 HTTP 요청을 받아 인증AP/이체AP로 TCP 중계한다. JWT 인증과 SSE 공지를 담당한다.

- **HTTP 18080**: REST API 서버 (JWT 인증)
- **TCP 19400**: spider-link `SpiderTcpServer` 내장, Admin의 긴급공지 명령 수신

```bash
cp biz-channel/.env.example biz-channel/.env
./mvnw spring-boot:run -pl biz-channel
```

#### REST API

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/api/auth/login` | 불필요 | 로그인 → JWT 액세스 토큰 + httpOnly refreshToken 쿠키 |
| `GET` | `/api/auth/me` | JWT | 현재 사용자 정보 |
| `POST` | `/api/auth/refresh` | refreshToken 쿠키 | 액세스 토큰 갱신 |
| `POST` | `/api/auth/logout` | JWT | 로그아웃 + 쿠키 만료 |
| `GET` | `/api/cards` | JWT | 카드 목록 |
| `GET` | `/api/transactions` | JWT | 이용내역 |
| `GET` | `/api/payment-statement` | JWT | 청구 명세서 |
| `GET` | `/api/cards/{cardId}/payable-amount` | JWT | 즉시결제 가능금액 |
| `POST` | `/api/cards/{cardId}/immediate-pay` | JWT | 즉시결제 |
| `DELETE` | `/api/cards/{cardId}/pin-attempts` | JWT | PIN 시도 횟수 초기화 |
| `GET` | `/api/notices/sse` | 불필요 | SSE 스트림 연결 (`text/event-stream`) |
| `GET` | `/api/notices/preview` | 불필요 | 현재 공지 조회 |

## 전체 기동 순서

```bash
# 1. 공통 선행 작업 (최초 1회)
mvn install -f spider-link/pom.xml

# 2. mock-core 기동
cd demo/bizApp && ./mvnw spring-boot:run -pl mock-core &

# 3. biz-auth 기동
./mvnw spring-boot:run -pl biz-auth &

# 4. biz-transfer 기동
./mvnw spring-boot:run -pl biz-transfer &

# 5. biz-channel 기동 (마지막)
./mvnw spring-boot:run -pl biz-channel &
```

## 의존관계

```
biz-common (라이브러리)
    ▲
    ├── mock-core
    ├── biz-auth   + spider-link
    ├── biz-transfer + spider-link
    └── biz-channel  + spider-link
```

## DB 스키마

| 테이블 | 모듈 | 설명 |
|--------|------|------|
| `D_ACCOUNT` | mock-core | 사용자 계정 (BCrypt 패스워드) |
| `D_CARD` | mock-core | 카드 정보 |
| `D_CARD_USAGE` | mock-core | 카드 이용내역 |
| `FWK_LISTENER_TRX_MESSAGE` | biz-auth, biz-transfer | spider-link 메타 (커맨드 등록) |
| `FWK_SERVICE`, `FWK_COMPONENT` | biz-auth, biz-transfer | spider-link 메타 (라우팅) |
