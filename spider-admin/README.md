# Spider Admin

Spring Boot 기반의 미들웨어 관리 콘솔. 메시지·트랜잭션·배치·에러·서비스 등 미들웨어 구성 요소를 웹 UI로 관리한다.

## 목차

- [개요](#개요)
- [기술 스택](#기술-스택)
- [사전 요구사항](#사전-요구사항)
- [빠른 시작](#빠른-시작)
- [프로젝트 구조](#프로젝트-구조)
- [테스트](#테스트)
- [CI/CD](#cicd)
- [API 문서](#api-문서)
- [참고 문서](#참고-문서)

---

## 개요

Spider 미들웨어 프레임워크의 **중앙 관리 콘솔**. AP 서버에 내장된 spider-link 엔진의 메타 설정을 DB로 관리하고, 웹 UI를 통해 운영 중인 시스템을 실시간으로 제어한다.

### 시스템 위치

```
브라우저 (관리자)
    │  HTTP / Thymeleaf SSR
    ▼
Spider Admin  ──── Oracle DB (FWK_* 메타 테이블)
    │
    │  TCP 관리 명령 (JsonCommandRequest)
    │  HTTP REST (SQL 리로드, 메타 캐시 갱신)
    ▼
AP 서버 (spider-link 내장)
    ├── biz-channel
    ├── biz-auth
    └── biz-transfer
```

### 주요 관리 기능

| 영역 | 기능 |
|------|------|
| **메시지·전문** | 전문 구조·필드·핸들러 등록, 파싱 테스트, 송수신 이력 조회 |
| **트랜잭션** | 거래 조회·정지·이력, 전문 매핑, 전송 데이터 관리 |
| **서비스·컴포넌트** | FWK_SERVICE·FWK_COMPONENT CRUD, 서비스 관계 설정 |
| **WAS 인스턴스** | WAS 그룹·인스턴스·속성 관리, 상태 모니터링 |
| **게이트웨이·기관** | 게이트웨이·기관 시스템·전송 설정, 리스너 매핑 |
| **설정** | DB 속성(FWK_PROPERTY)·XML 속성·SQL 쿼리 관리 및 실시간 리로드 |
| **배치** | 배치 앱 등록, 실행 이력, 실행 중 목록 조회 |
| **CMS** | 페이지 A/B 테스트·승인·에셋·배포·통계, React CMS 승인·배포 |
| **에러** | 에러코드·핸들링 규칙 관리, 발생 이력 조회 |
| **검증** | 검증 규칙·검증기 관리 |
| **사용자·권한** | 사용자·역할·메뉴·접근 IP 관리 |
| **운영 편의** | 로그레벨 동적 변경, 캐시 리로드, 긴급공지 배포, 관리자 접근 이력 |

### spider-link와의 연동

- **TCP 관리 명령**: Admin → AP 서버로 `JsonCommandRequest`를 전송하여 메타 캐시 갱신·로그레벨 변경 등을 실시간 적용한다.
- **HTTP 리로드**: SQL 쿼리 저장·수정 시 `SpiderLinkReloadClient`가 spider-link의 `/api/internal/sql/reload`를 호출하여 재기동 없이 반영한다.
- **메타 DB 공유**: Admin이 FWK_MESSAGE·FWK_SERVICE·FWK_COMPONENT 등 메타 테이블을 관리하면, spider-link가 이를 읽어 동작한다.

---

## 기술 스택

| 구분                | 기술                                                          |
|-------------------|-------------------------------------------------------------|
| Backend           | Java 17, Spring Boot 3.4, Spring Security 6, MyBatis 3      |
| Frontend          | Thymeleaf 3, Bootstrap 5.3, jQuery 3.7, Select2, SortableJS |
| Database          | Oracle 11g+ (primary), MySQL 8.0+ (프로필 존재, 매퍼 미구현)          |
| Cache             | Caffeine (in-memory)                                        |
| Logging           | Logback → H2 (DB appender) / Logstash (ELK)                 |
| API Docs          | SpringDoc OpenAPI (Swagger UI)                              |
| Build             | Maven 3 (Wrapper), Spotless + Palantir Java Format          |
| E2E Test          | Playwright 1.50 (TypeScript)                                |
| Architecture Test | ArchUnit 1.3                                                |
| Code Quality      | SonarCloud, JaCoCo, ESLint                                  |
| CI/CD             | GitHub Actions                                              |

## 사전 요구사항

- **JDK 17+**
- **Node.js 20+** (E2E 테스트·ESLint 실행 시)
- **Oracle** 데이터베이스 (MySQL 프로필은 존재하나 매퍼 미구현)
- **Docker** (선택 — ELK 스택, PostgreSQL/pgvector)

## 빠른 시작

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env 파일을 열어 DB 접속 정보 등을 입력
```

주요 환경변수:

| 변수 | 설명 | 예시 |
|------|------|------|
| `DB_URL` | JDBC URL | `jdbc:oracle:thin:@localhost:1521:XE` |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 | |
| `DB_SCHEMA` | 스키마 | `SPIDER` |
| `APP_TITLE` | 브라우저 타이틀 | `Neo Spider Admin` |
| `REMEMBER_ME_KEY` | Remember-me 시크릿 | |
| `AUTHORITY_SOURCE` | 권한 소스 | `user-menu` |

전체 목록은 [.env.example](.env.example) 참조.

### 2. 데이터베이스 초기화

```bash
# Oracle
sqlplus spider/spider1234@XE @docs/sql/oracle/01_create_tables.sql
sqlplus spider/spider1234@XE @docs/sql/oracle/03_insert_initial_data.sql

# MySQL
mysql -u spider -p spider < docs/sql/mysql/01_create_tables.sql
mysql -u spider -p spider < docs/sql/mysql/03_insert_initial_data.sql
```

### 3. 빌드 & 실행

```bash
# 빌드
./mvnw clean package

# 실행 (Oracle 프로필)
./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle

# 실행 (MySQL 프로필)
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

서버 기동 후 http://localhost:8080 접속.

### 4. ELK 스택 (선택)

```bash
docker-compose up -d    # Elasticsearch + Logstash + Kibana + PostgreSQL
```

- Kibana: http://localhost:5601
- Elasticsearch: http://localhost:9200

## 프로젝트 구조

```
src/main/java/com/example/spideradmin/
├── global/                     # 공통 인프라
│   ├── aop/                    #   AOP (감사 로그 등)
│   ├── auth/                   #   인증 서비스
│   ├── client/                 #   외부 HTTP 클라이언트
│   ├── common/                 #   베이스 클래스, 핸들러
│   ├── config/                 #   MyBatis, Async, RestTemplate, Swagger, WebMvc
│   ├── dto/                    #   공통 DTO (ApiResponse, PageRequest 등)
│   ├── exception/              #   예외 계층 (BaseException → NotFoundException 등)
│   ├── log/                    #   감사 로깅
│   ├── page/                   #   페이지네이션
│   ├── security/               #   Spring Security 설정
│   └── util/                   #   유틸리티
│
└── domain/                     # 비즈니스 도메인 (61개 모듈)
    ├── user/ & role/ & accessuser/                        #   사용자·역할·접근
    ├── menu/                                              #   메뉴·권한
    ├── org/ & orgcode/                                    #   조직·조직코드
    ├── code/ & codegroup/ & codetemplate/ & codegen/      #   공통코드·템플릿·생성
    ├── article/ & board/                                  #   게시판·첨부파일
    ├── batch/                                             #   배치 앱·이력·실행 관리
    ├── bizapp/ & bizgroup/                                #   비즈니스 앱·그룹
    ├── message/ & messagefield/ & messagehandler/         #   메시지·필드·핸들러
    ├── messageinstance/ & messageparsing/ & messagetest/  #   메시지 이력·파싱·테스트
    ├── transaction/ & trxmessage/ & transdata/            #   트랜잭션·전문·전송 데이터
    ├── service/                                           #   서비스(FWK_SERVICE)
    ├── listener/ & listenertrx/                           #   리스너·리스너-트랜잭션 매핑
    ├── gateway/ & gwsystem/ & transport/                  #   게이트웨이·시스템·전송
    ├── wasinstance/ & wasgroup/ & wasproperty/            #   WAS 인스턴스·그룹·속성
    ├── property/ & xmlproperty/ & sqlquery/               #   설정·XML속성·SQL쿼리
    ├── errorcode/ & errorhandle/ & errorhistory/          #   에러코드·핸들링·이력
    ├── validation/ & validator/                           #   검증 규칙·검증기
    ├── component/ & datasource/                           #   컴포넌트·데이터소스
    ├── cms* (6개: cmsabtest, cmsapproval, cmsasset,       #   CMS (A/B테스트·승인·에셋·
    │          cmsdashboard, cmsdeployment, cmsstatistics) #        대시보드·배포·통계)
    ├── reactcmsadminapproval/ & reactcmsadmindeployment/  #   React CMS 승인·배포
    ├── reactcmsdashboard/                                 #   React CMS 대시보드
    ├── emergencynotice/                                   #   긴급공지·배포
    ├── adminhistory/                                      #   관리자 접근 이력
    ├── dashboard/                                         #   메인 대시보드
    ├── monitor/                                           #   모니터링
    ├── loglevel/                                          #   로그레벨 동적 변경
    ├── reload/                                            #   캐시 리로드
    ├── proxyresponse/                                     #   프록시 응답 테스트 데이터
    ├── workgroup/ & worklist/                             #   워크그룹·워크리스트
    └── ...

src/main/resources/
├── templates/pages/               # Thymeleaf 템플릿 (페이지별 디렉터리)
├── static/js/components/          # 공통 JS 컴포넌트
│   │                              #   ComboManager, DataTable, LoadingOverlay,
│   │                              #   MessageBrowseModal, Modal, PageManager,
│   │                              #   Pagination, PrintUtil, SearchForm,
│   │                              #   SessionHandler, Toast, TrxDetailModal,
│   │                              #   WasSelectReloadModal
├── static/js/utils/               # 공통 JS 유틸리티
│   │                              #   event-bus, html-utils, sql-param-utils, theme-manager
├── mapper/oracle/                 # MyBatis SQL 매퍼 (현재 Oracle만 구현)
├── application.yml                # 메인 설정
├── application-oracle.yml         # Oracle 프로필
├── application-mysql.yml          # MySQL 프로필
├── application-ci.yml             # CI 프로필
└── menu-resource-permissions.yml  # 메뉴·리소스 권한 정의
```

각 도메인 모듈은 `controller/` · `service/` · `mapper/` · `dto/` 계층으로 구성된다.

## 테스트

### Unit Tests

```bash
./mvnw test
```

JaCoCo 커버리지 리포트: `target/site/jacoco/jacoco.xml`

### E2E Tests (Playwright)

```bash
npm ci
npm run test:e2e            # headless 전체 실행
npm run test:e2e:headed     # 브라우저 표시
npm run test:e2e:ui         # Playwright UI 모드
npm run test:e2e:report     # HTML 리포트 열기
```

E2E 프로젝트 실행 순서: `auth-setup` → `readonly-setup` → `smoke` → `api` → `pages`

상세 가이드: [e2e/README.md](e2e/README.md)

### Linting

```bash
npm run lint                # ESLint 검사
npm run lint:fix            # 자동 수정
./mvnw spotless:check       # Java 포맷 검사
./mvnw spotless:apply       # Java 포맷 자동 적용
```

## CI/CD

GitHub Actions (`ci.yml`) — `main` 브랜치 push/PR 시 자동 실행:

1. **Build** — Maven 컴파일, Spotless 포맷 검사, 단위 테스트 (JaCoCo)
2. **E2E** — Oracle 컨테이너 기동 → Spring Boot 앱 실행 → Playwright 테스트
3. **Quality** — SonarCloud 분석, ESLint

## API 문서

서버 기동 후 Swagger UI에서 전체 API를 확인할 수 있다:

http://localhost:8080/swagger-ui/index.html

## 참고 문서

| 문서 | 경로 |
|------|------|
| ERD | [docs/ERD.pdf](docs/ERD.pdf) |
| DDL (Oracle) | [docs/sql/oracle/01_create_tables.sql](docs/sql/oracle/01_create_tables.sql) |
| DDL (MySQL) | [docs/sql/mysql/01_create_tables.sql](docs/sql/mysql/01_create_tables.sql) |
| 인덱스 DDL | `docs/sql/{oracle,mysql}/02_create_indexes.sql` |
| 초기 데이터 | `docs/sql/{oracle,mysql}/03_insert_initial_data.sql` |
| E2E 가이드 | [e2e/README.md](e2e/README.md) |
