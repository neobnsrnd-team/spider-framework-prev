# POC_HNC 전체 서버 기동 가이드

> **사전 요건**  
> - Java 17 설치 및 `JAVA_HOME` 설정  
> - Maven Wrapper (`mvnw`) 포함 — **별도 Maven 설치 불필요** (첫 실행 시 자동 다운로드)  
> - Node.js 20+ · npm 설치  
> - Docker Desktop 설치 (Redis용)  
> - Oracle DB 기동 및 DDL 적용 완료  
> - Oracle Instant Client 설치 (html-cms 전용, 후술)

> **⚠️ Windows mvnw 실행 방법 — 터미널마다 다름**  
> | 터미널 | 명령어 예시 |  
> |--------|------------|  
> | Git Bash | `./mvnw spring-boot:run` |  
> | PowerShell | `.\mvnw spring-boot:run` |  
> | CMD | `mvnw spring-boot:run` |  
>  
> 이 가이드의 코드블록은 **Git Bash 기준**으로 작성되었습니다.  
> PowerShell/CMD 사용 시 `./mvnw` → `.\mvnw` (또는 `mvnw`)로 바꿔 입력하세요.

---

## 목차

1. [전체 서버 구성](#1-전체-서버-구성)
2. [포트 매핑 한눈에 보기](#2-포트-매핑-한눈에-보기)
3. [의존 관계도 및 기동 순서](#3-의존-관계도-및-기동-순서)
4. [STEP 0 — 인프라 기동 (Redis)](#step-0--인프라-기동-redis)
5. [STEP 1 — 라이브러리 빌드 (Maven 로컬 저장소 설치)](#step-1--라이브러리-빌드-maven-로컬-저장소-설치)
6. [STEP 2 — .env 파일 세팅](#step-2--env-파일-세팅)
7. [STEP 3 — 백엔드 서버 기동](#step-3--백엔드-서버-기동)
8. [STEP 4 — 프론트엔드 기동](#step-4--프론트엔드-기동)
9. [전체 기동 체크리스트](#전체-기동-체크리스트)
10. [자주 있는 문제 해결](#자주-있는-문제-해결)

---

## 1. 전체 서버 구성

| 구분 | 모듈 경로 | 역할 | 타입 |
|------|-----------|------|------|
| **라이브러리** | `spider-common/` | TCP 프로토콜 모델, 관리 명령 공통 | Maven JAR |
| **라이브러리** | `spider-link/` | AP 서버 내장형 TCP 연계 엔진 | Maven JAR |
| **라이브러리** | `spider-batch/` | 배치 오케스트레이션 공통 | Maven JAR |
| **라이브러리** | `demo/bizApp/biz-common/` | bizApp 모듈 간 TCP 커맨드 상수 | Maven JAR |
| **백엔드** | `spider-admin/` | 중앙 관리 콘솔 (Spring Boot + Thymeleaf) | HTTP :8080 |
| **백엔드** | `batch-was/` | 배치 실행 서버 | HTTP :8081 |
| **백엔드** | `reactPlatform/` | React 컴포넌트 코드 생성·결재 | HTTP :8082 |
| **백엔드** | `demo/bizApp/mock-core/` | 계정계 Mock (고정길이 바이너리 TCP) | TCP :19300 |
| **백엔드** | `demo/bizApp/biz-auth/` | 인증 서버 | TCP :19100 / HTTP :19180 |
| **백엔드** | `demo/bizApp/biz-transfer/` | 이체 서버 | TCP :19200 / HTTP :19280 |
| **백엔드** | `demo/bizApp/biz-channel/` | 채널 게이트웨이 (HTTP→TCP 중계) | HTTP :18080 / TCP :19400 |
| **프론트** | `demo/front/` | React 데모 뱅킹 앱 (Vite) | HTTP :5173 |
| **프론트** | `html-cms/` | HTML CMS (Next.js) | HTTP :3000 |
| **프론트** | `react-cms/` | React CMS (Vite) | HTTP :5174 |
| **인프라** | `batch-was/docker-compose.yml` | Redis (배치 분산 락) | :6379 |

---

## 2. 포트 매핑 한눈에 보기

| 포트 | 서버 | 프로토콜 |
|------|------|----------|
| 1521 | Oracle DB | JDBC — **사전 기동 필요** |
| 3000 | html-cms (Next.js) | HTTP |
| 5173 | demo/front (Vite) | HTTP |
| 5174 | react-cms (Vite) | HTTP |
| 6379 | Redis | TCP |
| 8080 | spider-admin | HTTP |
| 8081 | batch-was | HTTP |
| 8082 | reactPlatform | HTTP |
| 9090 | Prometheus | HTTP (선택, 모니터링) |
| 9999 | spider-admin | TCP — Admin 명령 수신 |
| 9998 | batch-was | TCP — Admin ↔ Batch 통신 |
| 18080 | biz-channel | HTTP — React 프론트 요청 수신 |
| 19100 | biz-auth | TCP — spider-link 내장 |
| 19180 | biz-auth | HTTP — Admin reload API |
| 19200 | biz-transfer | TCP — spider-link 내장 |
| 19280 | biz-transfer | HTTP — Admin reload API |
| 19300 | mock-core | TCP — 계정계 Mock |
| 19400 | biz-channel | TCP — Admin 공지 커맨드 수신 |

---

## 3. 의존 관계도 및 기동 순서

```
[인프라]
  Oracle DB (1521) ──────────────────────────────────────────┐
  Redis (6379) ─────────────────────────────────────┐        │
                                                    │        │
[STEP 3-1]                                          │        │
  mock-core (TCP:19300) ◄── Oracle DB               │        │
      │                                             │        │
      ├─── [STEP 3-2, 병렬 가능]                     │        │
      │     biz-auth (TCP:19100, HTTP:19180)         │        │
      │     biz-transfer (TCP:19200, HTTP:19280)     │        │
      │         │                                   │        │
      │     [STEP 3-3]                              │        │
      │     biz-channel (HTTP:18080, TCP:19400)      │        │
      │                                             │        │
      └─── [STEP 3-4, biz-channel과 병렬 가능]       │        │
            batch-was (HTTP:8081, TCP:9998) ◄────── ┘        │
                 │                                           │
            [STEP 3-5]                                       │
            spider-admin (HTTP:8080, TCP:9999) ◄────────────┘
                 │
            [STEP 3-6]
            reactPlatform (HTTP:8082)

[STEP 4 - 프론트]
  demo/front (HTTP:5173) ◄── biz-channel 기동 후
  html-cms (HTTP:3000)   ◄── spider-admin 기동 후
  react-cms (HTTP:5174)  ◄── spider-admin 기동 후
```

**권장 기동 순서 요약**

| 단계 | 기동 대상 | 병렬 가능 여부 |
|------|-----------|--------------|
| STEP 0 | Redis | — |
| STEP 3-1 | mock-core | — |
| STEP 3-2 | biz-auth, biz-transfer | ✅ 병렬 |
| STEP 3-3 | biz-channel | — |
| STEP 3-4 | batch-was | ✅ (biz-channel과 병렬 가능) |
| STEP 3-5 | spider-admin | — |
| STEP 3-6 | reactPlatform | ✅ (demo/front과 병렬 가능) |
| STEP 4 | demo/front, html-cms, react-cms | ✅ 병렬 |

---

## STEP 0 — 인프라 기동 (Redis)

`batch-was`가 Quartz 중복 실행 방지를 위해 Redis 분산 락을 사용합니다.  
Redis는 `batch-was/docker-compose.yml`에 정의되어 있습니다.

```bash
# batch-was 디렉토리로 이동
cd batch-was

# Redis만 기동 (최소 구성)
docker compose up -d redis

# Prometheus + Grafana까지 포함한 모니터링 스택 전체 기동 시
docker compose up -d
```

기동 확인:
```bash
docker ps | grep redis
# spider-batch-redis 컨테이너가 Up 상태인지 확인
```

---

## STEP 1 — 라이브러리 빌드 (Maven 로컬 저장소 설치)

실행 서버들이 `pom.xml`에서 로컬 Maven 저장소(`~/.m2`)를 참조합니다.  
**서버 빌드 전, 아래 순서대로 반드시 설치해야 합니다.**

| 순서 | 모듈 | 경로 | 의존 대상 |
|------|------|------|-----------|
| ① | `spider-common` | `spider-common/` | (없음) |
| ② | `spider-link` | `spider-link/` | spider-common |
| ③ | `spider-batch` | `spider-batch/` | spider-common, spider-link |
| ④ | `biz-common` | `demo/bizApp/biz-common/` | (없음) |

---

### A. 터미널 (Maven CLI)

프로젝트 루트(`C:\POC_HNC`)에서 실행합니다.

```bash
# ① spider-common 설치
cd spider-common
./mvnw clean install -DskipTests
cd ..

# ② spider-link 설치
cd spider-link
./mvnw clean install -DskipTests
cd ..

# ③ spider-batch 설치
cd spider-batch
./mvnw clean install -DskipTests
cd ..

# ④ biz-common 설치
cd demo/bizApp/biz-common
./mvnw clean install -DskipTests
cd ../../..
```

> **루트 aggregator POM으로 한 번에 빌드하는 방법**
> ```bash
> # 프로젝트 루트에서
> ./mvnw clean install -DskipTests
> ```

---

### B. IntelliJ IDEA

1. 우측 **Maven** 탭 클릭
2. 아래 순서로 각 모듈을 펼쳐 **Lifecycle → install** 더블클릭

   ```
   ① spider-common  → Lifecycle → install
   ② spider-link    → Lifecycle → install
   ③ spider-batch   → Lifecycle → install
   ④ biz-common     → Lifecycle → install
   ```

   또는 모듈 우클릭 → **Run Maven** → **install**

3. 각 모듈 `Build Output`에서 **BUILD SUCCESS** 확인 후 다음 진행

---

## STEP 2 — .env 파일 세팅

각 서버 디렉토리의 `.env.example`을 복사하여 `.env`를 생성합니다.  
`.env`는 `.gitignore`에 포함되어 git 추적이 제외되므로 직접 생성해야 합니다.

### 일괄 복사 명령어

프로젝트 루트에서 실행합니다 (bash/zsh):

```bash
cp spider-admin/.env.example              spider-admin/.env
cp batch-was/.env.example                 batch-was/.env
cp reactPlatform/.env.example             reactPlatform/.env
cp demo/bizApp/mock-core/.env.example     demo/bizApp/mock-core/.env
cp demo/bizApp/biz-auth/.env.example      demo/bizApp/biz-auth/.env
cp demo/bizApp/biz-transfer/.env.example  demo/bizApp/biz-transfer/.env
cp demo/bizApp/biz-channel/.env.example   demo/bizApp/biz-channel/.env
cp html-cms/.env.example                  html-cms/.env
cp react-cms/.env.example                 react-cms/.env
```

### 각 모듈 .env 필수 입력 항목

#### `spider-admin/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` | `jdbc:oracle:thin:@호스트:1521:XE` |
| `DB_USERNAME` | Oracle 사용자명 |
| `DB_PASSWORD` | Oracle 비밀번호 |
| `DB_SCHEMA` | Oracle 스키마명 |
| `REMEMBER_ME_KEY` | 세션 기억 비밀키 (임의 문자열) |
| `TCP_SERVER_PORT` | `9999` (Admin TCP 서버 포트) |
| `BATCH_WAS_TCP_PORT` | `9998` |
| `BATCH_WAS_HTTP_PORT` | `8081` |
| `BIZ_CHANNEL_TCP_HOST` | `localhost` |
| `BIZ_CHANNEL_TCP_PORT` | `19400` |
| `CMS_DEPLOY_SECRET` | html-cms의 `DEPLOY_SECRET`과 동일한 값 |

#### `batch-was/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` | spider-admin과 동일한 Oracle DB |
| `DB_USERNAME` / `DB_PASSWORD` / `DB_SCHEMA` | Oracle 접속 정보 |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `BATCH_WAS_API_KEY` | (선택) HTTP 직접 호출 시 인증키. 미설정 시 인증 비활성화 |

#### `reactPlatform/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `DB_SCHEMA` | Oracle 접속 정보 |
| `FIGMA_ACCESS_TOKEN` | Figma 계정 → Settings → Security → Personal access tokens |
| `CLAUDE_API_KEY` | Anthropic Console에서 발급한 API 키 |
| `REACT_DEPLOY_MODE` | `local` (로컬 파일 저장) 또는 `git-pr` (GitHub PR 생성) |

#### `demo/bizApp/mock-core/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Oracle 접속 정보 |
| `TCP_SERVER_PORT` | `19300` |

#### `demo/bizApp/biz-auth/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Oracle 접속 정보 |
| `HTTP_SERVER_PORT` | `19180` |
| `TCP_SERVER_PORT` | `19100` |
| `MOCK_CORE_HOST` | `localhost` |
| `MOCK_CORE_PORT` | `19300` |
| `APP_ORG_ID` | `DEMO` |

#### `demo/bizApp/biz-transfer/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Oracle 접속 정보 |
| `HTTP_SERVER_PORT` | `19280` |
| `TCP_SERVER_PORT` | `19200` |
| `MOCK_CORE_HOST` | `localhost` |
| `MOCK_CORE_PORT` | `19300` |
| `APP_ORG_ID` | `DEMO` |

#### `demo/bizApp/biz-channel/.env`

| 변수 | 설명 |
|------|------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Oracle 접속 정보 |
| `SERVER_PORT` | `18080` |
| `TCP_SERVER_PORT` | `19400` |
| `BIZ_AUTH_HOST` / `BIZ_AUTH_PORT` | `localhost` / `19100` |
| `BIZ_TRANSFER_HOST` / `BIZ_TRANSFER_PORT` | `localhost` / `19200` |
| `JWT_SECRET` | JWT 서명 비밀키 (임의 문자열, 32자 이상 권장) |
| `JWT_REFRESH_SECRET` | Refresh Token 비밀키 (JWT_SECRET과 다른 값) |
| `ADMIN_SECRET` | spider-admin과 공유하는 공지 커맨드 인증 비밀키 |

#### `html-cms/.env`

| 변수 | 설명 |
|------|------|
| `ORACLE_USER` / `ORACLE_PASSWORD` | Oracle 사용자명/비밀번호 |
| `ORACLE_HOST` | Oracle 호스트 |
| `ORACLE_PORT` | `1521` |
| `ORACLE_SERVICE` | `XE` |
| `ORACLE_SCHEMA` | Oracle 스키마명 |
| `DEPLOY_SECRET` | spider-admin의 `CMS_DEPLOY_SECRET`과 동일한 값 |
| `SCHEDULER_SECRET` | 만료 스케줄러 인증 토큰 (임의 문자열) |
| `CMS_ADMIN_BASE_URL` | `http://localhost:8080` (spider-admin URL) |

#### `react-cms/.env`

| 변수 | 설명 |
|------|------|
| `ORACLE_USER` / `ORACLE_PASSWORD` | Oracle 사용자명/비밀번호 |
| `ORACLE_HOST` | Oracle 호스트 |
| `ORACLE_PORT` | `1521` |
| `ORACLE_SERVICE` | `XE` |
| `ORACLE_SCHEMA` | Oracle 스키마명 |
| `SPIDER_ADMIN_API_URL` | `http://localhost:8080` (어드민 세션 공유용) |

### 서버 간 공유해야 하는 값

| 변수 | spider-admin | biz-channel | html-cms | 설명 |
|------|:---:|:---:|:---:|------|
| DB 접속 정보 | ✅ | ✅ | ✅ | 동일한 Oracle DB |
| `CMS_DEPLOY_SECRET` = `DEPLOY_SECRET` | ✅ | — | ✅ | CMS 배포 API 인증 |
| `ADMIN_SECRET` | (설정) | ✅ | — | Admin → biz-channel 공지 인증 |

---

## STEP 3 — 백엔드 서버 기동

### 3-1. mock-core (계정계 Mock) — TCP :19300

**의존성:** Oracle DB

#### A. 터미널 (Maven CLI)

```bash
cd demo/bizApp/mock-core

# bash/zsh: .env 로드 후 실행
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run

# 또는 JAR 빌드 후 실행
./mvnw clean package -DskipTests
java -jar target/mock-core-0.0.1-SNAPSHOT.jar
```

#### B. IntelliJ IDEA — Run Configuration

1. `demo/bizApp/mock-core/src/main/java/com/example/mockcore/MockCoreApplication.java` 열기
2. 상단 드롭다운 → **Edit Configurations...**
3. `+` → **Spring Boot**
4. 항목 입력:

   | 항목 | 값 |
   |------|----|
   | Name | `mock-core` |
   | Main class | `com.example.mockcore.MockCoreApplication` |
   | Working directory | `$PROJECT_DIR$/demo/bizApp/mock-core` |
   | Environment variables | `.env` 파일 내용 입력 (또는 EnvFile 플러그인으로 파일 지정) |

5. **OK** → ▶ 실행

**기동 확인:** 로그에서 `TCP Server started on port 19300` 확인

---

### 3-2. biz-auth · biz-transfer — **병렬 기동 가능**

**의존성:** mock-core (TCP :19300)

#### A. 터미널 (Maven CLI)

```bash
# [터미널 1] biz-auth
cd demo/bizApp/biz-auth
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run

# [터미널 2] biz-transfer
cd demo/bizApp/biz-transfer
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

#### B. IntelliJ IDEA — Run Configuration

**biz-auth:**

| 항목 | 값 |
|------|----|
| Name | `biz-auth` |
| Main class | `com.example.bizauth.BizAuthApplication` |
| Working directory | `$PROJECT_DIR$/demo/bizApp/biz-auth` |
| Environment variables | `demo/bizApp/biz-auth/.env` 내용 입력 |

**biz-transfer:**

| 항목 | 값 |
|------|----|
| Name | `biz-transfer` |
| Main class | `com.example.biztransfer.BizTransferApplication` |
| Working directory | `$PROJECT_DIR$/demo/bizApp/biz-transfer` |
| Environment variables | `demo/bizApp/biz-transfer/.env` 내용 입력 |

**기동 확인:**
- biz-auth: `TCP Server started on port 19100`
- biz-transfer: `TCP Server started on port 19200`

---

### 3-3. biz-channel (채널 게이트웨이) — HTTP :18080 / TCP :19400

**의존성:** biz-auth (TCP :19100), biz-transfer (TCP :19200)

#### A. 터미널 (Maven CLI)

```bash
cd demo/bizApp/biz-channel
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run

# 또는 JAR 실행
./mvnw clean package -DskipTests
java -jar target/biz-channel-0.0.1-SNAPSHOT.jar
```

#### B. IntelliJ IDEA — Run Configuration

| 항목 | 값 |
|------|----|
| Name | `biz-channel` |
| Main class | `com.example.bizchannel.BizChannelApplication` |
| Working directory | `$PROJECT_DIR$/demo/bizApp/biz-channel` |
| Environment variables | `demo/bizApp/biz-channel/.env` 내용 입력 |

**기동 확인:** `Started BizChannelApplication on port 18080`

---

### 3-4. batch-was (배치 실행 서버) — HTTP :8081 / TCP :9998

**의존성:** Oracle DB, Redis (TCP :6379)

#### A. 터미널 (Maven CLI)

```bash
cd batch-was
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run

# 또는 JAR 실행
./mvnw clean package -DskipTests
java -jar target/batch-was-0.0.1-SNAPSHOT.jar
```

#### B. IntelliJ IDEA — Run Configuration

| 항목 | 값 |
|------|----|
| Name | `batch-was` |
| Main class | `com.example.spiderbatch.BatchWasApplication` |
| Working directory | `$PROJECT_DIR$/batch-was` |
| Environment variables | `batch-was/.env` 내용 입력 |

**기동 확인:**
- `Started BatchWasApplication on port 8081`
- 헬스체크: `GET http://localhost:8081/actuator/health` → `{"status":"UP"}`

---

### 3-5. spider-admin (중앙 관리 콘솔) — HTTP :8080 / TCP :9999

**의존성:** Oracle DB, batch-was (HTTP :8081, TCP :9998), biz-channel (TCP :19400)

#### A. 터미널 (Maven CLI)

```bash
cd spider-admin
export $(grep -v '^#' .env | xargs)

# Oracle 프로필 활성화 필수
./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle

# PowerShell 사용 시 (따옴표 필요)
./mvnw spring-boot:run "-Dspring-boot.run.profiles=oracle"

# JAR 실행 시
./mvnw clean package -DskipTests
java -jar target/spider-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=oracle
```

#### B. IntelliJ IDEA — Run Configuration

| 항목 | 값 |
|------|----|
| Name | `spider-admin` |
| Main class | `com.example.spider_admin.SpiderAdminApplication` |
| Active profiles | `oracle` |
| Working directory | `$PROJECT_DIR$/spider-admin` |
| Environment variables | `spider-admin/.env` 내용 입력 |

> Active profiles 입력란: `Run Configuration` 창 → **Spring Boot** 탭 → **Active profiles** 필드에 `oracle` 입력

**기동 확인:**
- `Started SpiderAdminApplication on port 8080`
- 접속: http://localhost:8080

---

### 3-6. reactPlatform (React 코드 생성 서버) — HTTP :8082

**의존성:** Oracle DB, spider-admin (TCP :9999)

#### A. 터미널 (Maven CLI)

```bash
cd reactPlatform
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle

# PowerShell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=oracle"

# JAR 실행
./mvnw clean package -DskipTests
java -jar target/reactPlatform-0.0.1-SNAPSHOT.jar --spring.profiles.active=oracle
```

#### B. IntelliJ IDEA — Run Configuration

| 항목 | 값 |
|------|----|
| Name | `reactPlatform` |
| Main class | `com.example.reactplatform.ReactPlatformApplication` |
| Active profiles | `oracle` |
| Working directory | `$PROJECT_DIR$/reactPlatform` |
| Environment variables | `reactPlatform/.env` 내용 입력 |

**기동 확인:** `Started ReactPlatformApplication on port 8082`

---

### IntelliJ IDEA Compound 실행 구성 (전체 서버 한 번에 기동)

위에서 만든 Run Configuration들을 묶어 버튼 한 번으로 전체 기동할 수 있습니다.

1. **Edit Configurations...** → `+` → **Compound**
2. Name: `POC_HNC 전체 기동` 입력
3. `+` 버튼으로 아래 구성 추가:
   - `mock-core`
   - `biz-auth`
   - `biz-transfer`
   - `biz-channel`
   - `batch-was`
   - `spider-admin`
   - `reactPlatform`
4. **OK** → 드롭다운에서 `POC_HNC 전체 기동` 선택 후 ▶ 클릭

> **주의:** Compound는 등록 순서 보장 없이 동시 실행됩니다.  
> mock-core보다 biz-auth가 먼저 뜰 경우 초기 연결 오류 로그가 출력될 수 있으나,  
> 수 초 내에 모두 준비되므로 개발 환경에서는 무방합니다.

---

## STEP 4 — 프론트엔드 기동

### 4-1. demo/front (React 뱅킹 앱) — HTTP :5173

**의존성:** biz-channel (HTTP :18080)

```bash
cd demo/front
npm install          # 최초 1회
npm run dev
```

접속: http://localhost:5173

---

### 4-2. html-cms (Next.js HTML CMS) — HTTP :3000

**의존성:** Oracle DB, spider-admin (HTTP :8080)

> **사전 준비: Oracle Instant Client 설치**  
> `html-cms`는 Node.js `oracledb` 패키지를 **Thick 모드**로 사용합니다.  
> Oracle Instant Client가 없으면 DB 연결에 실패합니다.
>
> - **Windows:** [Oracle Instant Client Basic 64-bit 다운로드](https://www.oracle.com/database/technologies/instant-client/downloads.html) → 압축 해제 → 폴더 경로를 시스템 **PATH** 환경변수에 추가  
>   또는 `html-cms/.env`에 `ORACLE_CLIENT_DIR=C:/oracle/instantclient_21_3` 직접 지정  
> - **Mac:** `brew install instantclient-basic`

```bash
cd html-cms

# .env 파일 편집 (ORACLE_USER, ORACLE_PASSWORD, ORACLE_HOST 등 필수)
# cp html-cms/.env.example html-cms/.env 은 STEP 2에서 완료

npm install          # 최초 1회

# E2E 테스트용 브라우저 바이너리 설치 (E2E 테스트를 실행하지 않는다면 생략)
# npx playwright install

npm run dev          # 개발 서버 기동
```

접속: http://localhost:3000/cms

---

### 4-3. react-cms (Vite React CMS) — HTTP :5174

**의존성:** Oracle DB, spider-admin (HTTP :8080)

> **사전 준비:** html-cms와 동일하게 Oracle Instant Client가 필요합니다.

```bash
cd react-cms

# .env 편집 (ORACLE_USER, ORACLE_PASSWORD, ORACLE_HOST, SPIDER_ADMIN_API_URL 필수)

npm install          # 최초 1회
npm run dev          # 일반 개발 모드 (http://localhost:5174)

# spider-admin 세션 공유 모드 (nginx 프록시 :9000 경유)
# npm run dev:proxy
```

접속: http://localhost:5174

---

## 전체 기동 체크리스트

아래 항목을 순서대로 확인합니다.

**인프라**
- [ ] Oracle DB 접속 가능 (1521)
- [ ] Redis 컨테이너 기동 — `docker ps | grep redis`에서 `Up` 상태 확인

**백엔드**
- [ ] **mock-core** — 로그에 `TCP Server started on port 19300`
- [ ] **biz-auth** — 로그에 `TCP Server started on port 19100`
- [ ] **biz-transfer** — 로그에 `TCP Server started on port 19200`
- [ ] **biz-channel** — `GET http://localhost:18080/actuator/health` → `UP`
- [ ] **batch-was** — `GET http://localhost:8081/actuator/health` → `UP`
- [ ] **spider-admin** — http://localhost:8080 로그인 화면 접속 확인
- [ ] **reactPlatform** — http://localhost:8082 접속 확인

**프론트엔드**
- [ ] **demo/front** — http://localhost:5173 React 앱 화면 확인
- [ ] **html-cms** — http://localhost:3000/cms CMS 에디터 화면 확인
- [ ] **react-cms** — http://localhost:5174 React CMS 화면 확인

---

## 자주 있는 문제 해결

### `.env` 로드가 안 될 때 (Windows CMD/PowerShell)

CMD/PowerShell에서는 `export $(cat .env)` 문법이 지원되지 않습니다.  
**IntelliJ EnvFile 플러그인 사용**을 권장합니다.

1. IntelliJ → **Plugins** → `EnvFile` 검색 → 설치
2. Run Configuration 창 → **EnvFile** 탭 → **Enable EnvFile** 체크
3. `+` → 해당 모듈의 `.env` 파일 경로 지정

또는 PowerShell에서 아래 방식으로 직접 로드합니다:

```powershell
# PowerShell — .env 변수를 현재 세션에 로드
Get-Content .env | Where-Object { $_ -notmatch '^#' -and $_ -match '=' } | ForEach-Object {
    $key, $val = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($key.Trim(), $val.Trim(), 'Process')
}
./mvnw spring-boot:run "-Dspring-boot.run.profiles=oracle"
```

---

### 라이브러리 수정 후 실행 서버에 반영이 안 될 때

라이브러리(`spider-common`, `spider-link`, `spider-batch`, `biz-common`)를 수정한 경우  
변경된 라이브러리만 다시 `install`하고 이를 사용하는 서버를 **재기동**해야 합니다.

```bash
# 예: spider-link 수정 시
cd spider-link && ./mvnw clean install -DskipTests && cd ..

# spider-link에 의존하는 biz-auth, biz-transfer, biz-channel, mock-core 재기동
```

---

### Oracle DB 연결 실패 (Spring Boot)

`application-oracle.yml`은 `${DB_URL}` 환경변수를 참조합니다.  
`.env`의 `DB_URL` 형식을 확인합니다.

```bash
# SID 방식
DB_URL=jdbc:oracle:thin:@localhost:1521:XE

# 서비스명 방식 (//표기)
DB_URL=jdbc:oracle:thin:@//localhost:1521/XE
```

---

### html-cms Oracle Instant Client 오류 (Windows)

```
NJS-045: cannot load a 32-bit Oracle Client library into a 64-bit process
```

→ **64-bit** Basic 패키지를 다운로드했는지 확인합니다.  
→ `html-cms/.env`에 `ORACLE_CLIENT_DIR` 절대 경로를 명시합니다:

```bash
ORACLE_CLIENT_DIR=C:/oracle/instantclient_21_3
```

---

### biz-channel JWT 관련 오류

`JWT_SECRET` / `JWT_REFRESH_SECRET`이 비어 있으면 서버 기동 시 오류가 발생합니다.  
임의의 32자 이상 문자열을 설정합니다.

```bash
# 예시 (openssl 사용)
openssl rand -hex 32
```

---

### Compound 실행 시 포트 충돌

이미 실행 중인 서버가 있을 경우 포트 충돌이 발생합니다.

```bash
# Windows — 포트 사용 프로세스 확인
netstat -ano | findstr :8080

# 해당 PID 종료
taskkill /PID <PID> /F
```
