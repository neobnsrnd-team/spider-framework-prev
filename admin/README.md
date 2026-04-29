# Spider Admin

Spring Boot 기반의 미들웨어 관리 콘솔. 메시지·트랜잭션·배치·에러·서비스 등 미들웨어 구성 요소를 웹 UI로 관리한다.

## 목차

- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [CI/CD](#cicd)
- [API Documentation](#api-documentation)
- [Documentation](#documentation)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.4, Spring Security 6, MyBatis 3 |
| Frontend | Thymeleaf 3, Bootstrap 5.3, jQuery 3.7, Select2, SortableJS |
| Database | Oracle 11g+ (primary), MySQL 8.0+ (프로필 존재, 매퍼 미구현) |
| Cache | Caffeine (in-memory) |
| Logging | Logback → H2 (DB appender) / Logstash (ELK) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3 (Wrapper), Spotless + Palantir Java Format |
| E2E Test | Playwright 1.50 (TypeScript) |
| Architecture Test | ArchUnit 1.3 |
| Code Quality | SonarCloud, JaCoCo, ESLint |
| CI/CD | GitHub Actions |

## Prerequisites

- **JDK 17+**
- **Node.js 20+** (E2E 테스트·ESLint 실행 시)
- **Oracle** 데이터베이스 (MySQL 프로필은 존재하나 매퍼 미구현)
- **Docker** (선택 — ELK 스택, PostgreSQL/pgvector)

## Quick Start

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

## Project Structure

```
src/main/java/com/example/admin_demo/
├── global/                     # 공통 인프라
│   ├── auth/                   #   인증 서비스
│   ├── common/                 #   베이스 클래스, 핸들러
│   ├── config/                 #   MyBatis, Async, RestTemplate, Swagger, WebMvc
│   ├── dto/                    #   공통 DTO (ApiResponse, PageRequest 등)
│   ├── exception/              #   예외 계층 (BaseException → NotFoundException 등)
│   ├── log/                    #   감사 로깅
│   ├── page/                   #   페이지네이션
│   ├── security/               #   Spring Security 설정
│   └── util/                   #   유틸리티
│
└── domain/                     # 비즈니스 도메인 (47개 모듈)
    ├── user/ & role/ & accessuser/       #   사용자·역할·접근
    ├── menu/                             #   메뉴·권한
    ├── org/ & orgcode/                   #   조직·조직코드
    ├── code/ & codegroup/                #   공통코드
    ├── article/ & board/                 #   게시판
    ├── batch/                            #   배치 관리
    ├── bizapp/ & bizgroup/               #   비즈니스 앱·그룹
    ├── message* (6개)                    #   메시지·핸들러·파싱·테스트
    ├── transaction/ & transdata/         #   트랜잭션
    ├── service/                          #   서비스(FWK_SERVICE)
    ├── errorcode/ & errorhandle/ & errorhistory/ #   에러코드·핸들링·이력
    ├── emergencynotice/                  #   긴급공지
    └── ...                              #   gateway, monitor, datasource 등

src/main/resources/
├── templates/pages/            # Thymeleaf 템플릿 (페이지별 디렉터리)
├── static/js/components/       # 공통 JS 컴포넌트 (DataTable, Modal, Pagination 등)
├── static/js/utils/            # 공통 JS 유틸리티 (html-utils, theme-manager)
├── mapper/oracle/              # MyBatis SQL 매퍼 (현재 Oracle만 구현)
├── application.yml             # 메인 설정
├── application-oracle.yml      # Oracle 프로필
├── application-mysql.yml       # MySQL 프로필
├── application-ci.yml          # CI 프로필
└── menu-resource-permissions.yml  # 메뉴·리소스 권한 정의
```

각 도메인 모듈은 `controller/` · `service/` · `mapper/` · `dto/` 계층으로 구성된다.

## Testing

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

## API Documentation

서버 기동 후 Swagger UI에서 전체 API를 확인할 수 있다:

http://localhost:8080/swagger-ui/index.html

## Documentation

| 문서 | 경로 |
|------|------|
| ERD | [docs/ERD.pdf](docs/ERD.pdf) |
| DDL (Oracle) | [docs/sql/oracle/01_create_tables.sql](docs/sql/oracle/01_create_tables.sql) |
| DDL (MySQL) | [docs/sql/mysql/01_create_tables.sql](docs/sql/mysql/01_create_tables.sql) |
| 인덱스 DDL | `docs/sql/{oracle,mysql}/02_create_indexes.sql` |
| 초기 데이터 | `docs/sql/{oracle,mysql}/03_insert_initial_data.sql` |
| E2E 가이드 | [e2e/README.md](e2e/README.md) |
