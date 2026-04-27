# reactPlatform

**Figma 디자인에서 React 코드를 자동 생성하고 결재까지 관리하는 Spring Boot 백엔드 서버**

Figma URL을 입력하면 Claude API를 통해 React 화면 코드를 생성하고, 생성된 코드를 결재 워크플로우로 관리한다.
사용자·역할·메뉴 관리와 접근/에러 로그 기능도 함께 제공한다.

---

## 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [System Prompt 구성](#system-prompt-구성)
- [preview-app](#preview-app)
- [디렉터리 구조](#디렉터리-구조)
- [빠른 시작](#빠른-시작)
  - [사전 준비](#사전-준비)
  - [환경변수 설정](#환경변수-설정)
  - [실행](#실행)
- [환경변수 목록](#환경변수-목록)
  - [공통](#공통)
  - [배포 전략](#배포-전략)
  - [로그](#로그)
- [API 문서](#api-문서)

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.7 |
| Security | Spring Security |
| Persistence | MyBatis + Oracle DB (HikariCP) |
| View | Thymeleaf + Bootstrap 5.3 + jQuery |
| Cache | Caffeine |
| Log | Logback + H2 (인메모리 로그) + Logstash (선택) |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven + Spotless (코드 포맷) + JaCoCo (커버리지) |

---

## 주요 기능

### React 코드 생성
- Figma URL 입력 → Figma API로 디자인 노드 추출
- Claude API(claude-sonnet-4-6)로 React `.tsx` 코드 자동 생성
- 생성된 코드 유효성 검사 (`CodeValidator`)
- 코드 미리보기 및 결재 요청

### 결재 워크플로우
- 생성 코드 승인/반려 처리
- 승인 시 배포 전략에 따라 자동 배포 실행
- 생성 이력 조회 및 Excel 다운로드

### 배포 전략 (Strategy 패턴)

승인된 코드는 `REACT_DEPLOY_MODE` 환경변수에 따라 아래 두 전략 중 하나로 배포된다.

| 모드 | 전략 | 용도 |
|------|------|------|
| `local` | `LocalFileDeployStrategy` | 서버 로컬 파일시스템에 `.tsx` 파일 직접 저장 (개발 환경 기본값) |
| `git-pr` | `GitPrDeployStrategy` | GitHub 레포에 브랜치 생성 + PR 자동 생성 (스테이징/운영) |

두 전략 모두 **UI 컴포넌트 파일**과 **Container Scaffold** 파일을 함께 생성한다.

### 배포 관리
- 배포 이력 조회 (배포 상태·PR URL 포함)
- 수동 재배포 (승인된 코드 재실행)

### 관리 기능
- 사용자 관리 (계정 생성·수정·삭제, 상태 관리)
- 역할 관리 (역할별 메뉴 권한 할당)
- 메뉴 관리 (메뉴 트리 구성, 접근 레벨 설정)

### 로깅
- 접근 로그·에러 로그 Oracle DB 저장
- H2 콘솔(`/h2-console`)에서 인메모리 로그 조회
- Logstash 연동 지원 (선택)

---

## System Prompt 구성

Claude API 호출 시 전달하는 system prompt는 `reactive-springware` 패키지에서 추출한 md 파일 3개를 조합하여 만든다.

### 파일 생성 (reactive-springware)

`reactive-springware`의 스크립트를 실행하면 `generated/` 디렉토리에 md 파일이 생성된다.

```bash
# reactive-springware 디렉토리에서 실행
npm run generate:prompts
```

내부적으로 아래 3개의 스크립트가 순서대로 실행된다.

| 스크립트 | 입력 | 출력 | 역할 |
|----------|------|------|------|
| `extract-components.ts` | `component-library/**/types.ts` | `generated/component-types.md` | 컴포넌트 TypeScript 인터페이스를 마크다운 레퍼런스로 변환 |
| `extract-design-tokens.ts` | `design-tokens/globals.css` | `generated/design-tokens.md` | CSS 변수(`@theme`, 브랜드별, 도메인별 토큰)를 마크다운 테이블로 변환 |
| `extract-page-generation-rules.ts` | `docs/page-generation-rules.md` | `generated/page-generation-rules.md` | 코드 생성 규칙 문서를 `generated/`로 복사 |

### 파일 배치 (reactPlatform)

생성된 md 파일을 `src/main/resources/prompts/`에 배치한다.

```
src/main/resources/prompts/
├── page-generation-rules.md  ← generated/page-generation-rules.md
├── component-types.md  ← generated/component-types.md
└── design-tokens.md    ← generated/design-tokens.md
```

### 로딩 및 조합 (PromptLoader / PromptBuilder)

`PromptLoader`는 애플리케이션 시작 시(`@PostConstruct`) 3개의 파일을 읽어 메모리에 캐싱한다.
파일이 없으면 경고 로그만 남기고 빈 문자열로 처리하므로, 일부 파일이 누락되어도 기동은 정상적으로 된다.

`PromptBuilder`는 캐싱된 내용을 아래 순서로 조합하여 단일 system prompt 문자열을 반환한다.

```
역할 정의 (하드코딩)
  ↓
--- page-generation-rules.md (코드 생성 규칙 + Figma → React 컴포넌트 매핑) ---
  ↓
--- Component Library (컴포넌트 인터페이스) ---
  ↓
--- Design Tokens (CSS 변수 레퍼런스 — 하드코딩 금지) ---
```

빈 섹션은 자동으로 건너뛰어 불필요한 헤더가 노출되지 않는다.

---

## preview-app

생성된 React 코드를 브라우저에서 즉시 확인하기 위한 독립적인 Vite + React 19 + TypeScript 프로젝트.
Thymeleaf 화면의 `<iframe>`으로 삽입되며, `postMessage`를 통해 생성 코드를 수신해 렌더링한다.

### 통신 흐름

```
reactPlatform (Thymeleaf)
  │
  │  postMessage({ type: 'UPDATE_CODE', code: reactCode, codeId })
  ↓
preview-app (iframe)
  │
  ├─ 1. patchImports()   — import 구문 → window.__components 접근으로 교체
  ├─ 2. patchExport()    — export default → var __Component = ... 으로 교체
  ├─ 3. Babel.transform()— CDN Babel로 TSX → JavaScript 트랜스파일
  ├─ 4. new Function()   — 컴포넌트 함수 추출
  └─ 5. ReactDOM.createRoot().render() — 화면에 마운트
```

### 컴포넌트 레지스트리

preview-app은 `window.__components`에 사용 가능한 컴포넌트를 미리 등록해 둔다(`componentRegistry.ts`).
Claude가 생성한 코드의 `import` 구문이 이 레지스트리를 바라보도록 패치되므로, 실제 번들 없이 컴포넌트를 사용할 수 있다.

등록 대상: `reactive-springware` 컴포넌트 + `lucide-react` 아이콘

### 오류 처리

오류 포착은 두 단계로 구분된다.

| 단계 | 오류 유형 | 처리 방식 |
|------|-----------|-----------|
| 트랜스파일 | Babel 파싱 오류, `new Function` 실패 | 동기 `try-catch` |
| 런타임 렌더링 | undefined 컴포넌트, props 타입 불일치 등 | `ErrorBoundary` (React 클래스형 컴포넌트) |

렌더링 오류가 발생하면 `POST /api/react-generate/render-error`로 codeId와 오류 메시지를 서버에 기록한다.

### 빌드

preview-app 빌드 결과물은 `src/main/resources/static/preview-app/`에 위치해야 Spring Boot가 `/preview-app/**` 경로로 서빙할 수 있다.
git clone 후 해당 디렉토리가 비어 있거나 `index.html`이 없으면 미리보기 iframe이 표시되지 않으므로, 아래 명령으로 직접 빌드한다.

```bash
# 프로젝트 루트의 preview-app 디렉토리에서 실행
cd ../preview-app
npm install
npm run build
```

빌드가 완료되면 `reactPlatform/src/main/resources/static/preview-app/` 아래에 `index.html`과 assets 파일이 생성된다.
이후 Spring Boot를 재시작하면 미리보기가 정상 동작한다.

> **참고**: `reactive-springware` 컴포넌트를 소스 alias로 직접 참조하여 번들링하므로,
> `reactive-springware` 쪽 컴포넌트가 변경된 경우에도 preview-app을 다시 빌드해야 반영된다.

---

## 디렉터리 구조

```
reactPlatform/
├── src/main/java/com/example/reactplatform/
│   ├── domain/
│   │   ├── reactgenerate/       # React 코드 생성
│   │   │   ├── ai/              #   Claude API 클라이언트 + 프롬프트 빌더
│   │   │   ├── figma/           #   Figma API 클라이언트 + 디자인 파서
│   │   │   ├── deploy/          #   배포 전략 인터페이스 + 설정
│   │   │   │   ├── gitpr/       #     GitHub PR 배포 전략
│   │   │   │   └── local/       #     로컬 파일 배포 전략
│   │   │   ├── validator/       #   생성 코드 유효성 검사
│   │   │   ├── dto/             #   요청·응답 DTO
│   │   │   ├── enums/           #   상태 열거형 (ReactGenerateStatus 등)
│   │   │   ├── mapper/          #   MyBatis 매퍼 인터페이스
│   │   │   ├── service/         #   코드 생성 서비스
│   │   │   └── controller/      #   REST + 화면 컨트롤러
│   │   ├── reactapproval/       # 승인 워크플로우 (승인·반려·이력 조회)
│   │   │   ├── controller/
│   │   │   └── service/
│   │   ├── reactdeploy/         # 배포 이력 조회·재배포
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   └── service/
│   │   ├── user/                # 사용자 관리
│   │   ├── role/                # 역할·메뉴 권한 관리
│   │   └── menu/                # 메뉴 관리
│   └── global/
│       ├── security/            # Spring Security 설정·인증
│       ├── auth/                # 로그인·로그아웃
│       ├── log/                 # 접근·에러 로그
│       └── exception/           # 전역 예외 처리
├── src/main/resources/
│   ├── application.yml          # 공통 설정 (포트 8082)
│   ├── application-oracle.yml   # Oracle DB 설정
│   ├── mapper/oracle/           # MyBatis XML 매퍼
│   ├── prompts/                 # Claude 프롬프트 파일 (page-generation-rules.md 등)
│   ├── static/                  # JS·CSS 정적 자산
│   └── templates/               # Thymeleaf 화면 템플릿
├── .env.example                 # 환경변수 예시 파일
└── pom.xml
```

---

## 빠른 시작

### 사전 준비

- Java 17 이상
- Oracle DB 접속 정보
- [Figma Personal Access Token](https://www.figma.com/settings) (`figd_` 로 시작)
- [Claude API Key](https://console.anthropic.com/) (`sk-ant-api03-` 로 시작)

### 환경변수 설정

```bash
# 1. 예시 파일 복사
cp .env.example .env

# 2. .env 파일에 실제 값 입력
#    (DB_URL, DB_USERNAME, DB_PASSWORD, DB_SCHEMA, FIGMA_ACCESS_TOKEN, CLAUDE_API_KEY 필수)
```

### 실행

```bash
# .env 로드 후 실행 (bash/zsh)
set -a && source .env && set +a
./mvnw spring-boot:run

# 또는 IntelliJ Run Configuration → Environment variables 에 .env 파일 지정
```

서버가 기동되면 `http://localhost:8082` 로 접속한다.

---

## 환경변수 목록

### 공통

| 변수 | 필수 | 기본값 | 설명 |
|------|:----:|--------|------|
| `DB_URL` | O | `jdbc:oracle:thin:@localhost:1521:XE` | Oracle JDBC URL |
| `DB_USERNAME` | O | — | DB 사용자 |
| `DB_PASSWORD` | O | — | DB 비밀번호 |
| `DB_SCHEMA` | O | — | 접속 스키마 |
| `FIGMA_ACCESS_TOKEN` | O | — | Figma Personal Access Token |
| `CLAUDE_API_KEY` | O | — | Claude API Key |
| `APP_TITLE` | — | `React Gen` | 화면 상단 타이틀 |
| `REMEMBER_ME_KEY` | — | `react-gen-remember-me-key` | Remember-me 쿠키 서명 키 |
| `AUTHORITY_SOURCE` | — | `user-menu` | 권한 소스 (`user-menu` / `role-menu`) |

### 배포 전략

| 변수 | 필수 | 기본값 | 설명 |
|------|:----:|--------|------|
| `REACT_DEPLOY_MODE` | — | `local` | 배포 전략 (`local` / `git-pr`) |
| **local 모드** | | | |
| `REACT_DEPLOY_LOCAL_COMPONENT_DIR` | — | `../demo/front/src/reactplatform/generated` | UI 컴포넌트 저장 경로 |
| `REACT_DEPLOY_LOCAL_CONTAINER_DIR` | — | `../demo/front/src/reactplatform/containers` | Container Scaffold 저장 경로 |
| **git-pr 모드** | | | |
| `GITHUB_TOKEN` | git-pr 시 O | — | GitHub Personal Access Token (repo 쓰기 권한 필요) |
| `GITHUB_REPO_OWNER` | git-pr 시 O | — | 대상 레포 소유자 (조직명 또는 계정명) |
| `GITHUB_REPO_NAME` | git-pr 시 O | — | 대상 레포 이름 |
| `GITHUB_BASE_BRANCH` | — | `main` | PR 대상 브랜치 |
| `GITHUB_COMPONENT_PATH` | — | `src/reactplatform/generated` | 레포 내 UI 컴포넌트 경로 |
| `GITHUB_CONTAINER_PATH` | — | `src/reactplatform/containers` | 레포 내 Container Scaffold 경로 |

### 로그

| 변수 | 필수 | 기본값 | 설명 |
|------|:----:|--------|------|
| `LOG_DEST_CONSOLE` | — | `true` | 콘솔 로그 출력 여부 |
| `LOG_DEST_H2` | — | `false` | H2 로그 저장 여부 |
| `LOG_DEST_RDB_ACCESS` | — | `true` | Oracle 접근 로그 저장 여부 |
| `LOG_DEST_RDB_ERROR` | — | `true` | Oracle 에러 로그 저장 여부 |
| `LOG_DEST_LOGSTASH` | — | `false` | Logstash 전송 여부 |
| `LOGSTASH_HOST` | — | `localhost` | Logstash 호스트 |
| `LOGSTASH_PORT` | — | `5000` | Logstash 포트 |

---

## API 문서

서버 실행 후 Swagger UI에서 REST API 명세를 확인할 수 있다.

```
http://localhost:8082/swagger-ui/index.html
```
