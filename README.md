# Spider-Framework

안전하고 유연한 금융 서비스를 위한 SPA + Spring Boot 기반 미들웨어 프레임워크.

## 목차

- [개요](#개요)
- [핵심 구성요소](#핵심-구성요소)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [프로젝트 역할 요약](#프로젝트-역할-요약)
- [빌드 순서 및 의존 관계](#빌드-순서-및-의존-관계)
- [시스템 통신 흐름](#시스템-통신-흐름)
- [각 모듈 상세 가이드](#각-모듈-상세-가이드)

---

## 개요

Spider-Framework는 금융 AP 서버 환경에서 반복적으로 필요한 인프라 문제를 라이브러리로 해결하는 프레임워크다.

기존 금융 시스템에서 새로운 서비스를 추가하려면 AP 서버마다 TCP 통신 코드, 배치 실행 코드, 이력 관리 코드를 반복 작성해야 했다. Spider-Framework는 이 공통 관심사를 `spider-link`, `spider-batch` 라이브러리로 분리하여, AP 서버는 **비즈니스 로직만** 구현하면 되는 구조를 제공한다.

**FWK 메타 기반 동적 라우팅**이 핵심이다. 커맨드 라우팅 규칙을 DB 메타테이블(`FWK_SERVICE`, `FWK_COMPONENT` 등)에 정의하면, 코드 변경·재배포 없이 `admin`에서 reload API 호출만으로 서비스를 추가하거나 수정할 수 있다.

### 무엇이 구현되었나

| 구성요소 | 구현 내용 |
|----------|----------|
| TCP 연계엔진 | `spider-link` 라이브러리 — AP 서버에 내장되어 JSON / 고정길이 / ObjectStream 프로토콜의 TCP 서버를 제공 |
| 메타 기반 라우팅 | `MetaDrivenCommandHandler` — FWK 메타테이블 체인으로 커맨드를 동적 실행. 재기동 없이 커맨드 추가·수정 가능 |
| 배치 오케스트레이션 | `spider-batch` 라이브러리 — `JobProvider` SPI 구현만으로 배치 실행·이력·TCP 연동·REST API가 자동 제공됨 |
| 관리 콘솔 | `admin` — FWK 메타·메뉴·권한·사용자·긴급공지 등 플랫폼 전체를 웹 UI로 관리 |
| 프론트엔드 컴포넌트 | `reactive-springware` — 금융 앱 특화 React 컴포넌트 라이브러리 + 디자인 토큰 시스템. 프론트엔드 프로젝트가 path alias로 직접 참조 |
| POC 시연 앱 | `demo/bizApp` + `demo/front` — spider-link 내장형 금융 AP 서버군(인증·이체·채널·계정계 Mock)과 카드 서비스 프론트엔드 |

---

## 핵심 구성요소

```
┌──────────────────────────────────────────────────────────────────┐
│                        Spider-Framework                          │
│                                                                  │
│  ┌──────────────┐   ┌────────────────┐   ┌────────────────────┐  │
│  │ spider-link  │   │ spider-batch   │   │ reactive-          │  │
│  │ TCP 연계엔진  │   │ 배치 라이브러리  │   │ springware         │  │
│  │  (라이브러리) │   │  (라이브러리)   │   │ component-library  │  │
│  └──────┬───────┘   └──────┬─────────┘   └────────┬───────────┘  │
│         │ Maven 내장        │ Maven 내장            │ path alias    │
└─────────┼──────────────────┼───────────────────────┼─────────────┘
          ↓                  ↓                       ↓
    AP 서버들              batch-was           demo/front
  (biz-auth,                                  react-cms 등
  biz-transfer,
  biz-channel)
```

- **spider-link** — AP 서버에 Maven 의존성으로 내장. `SpiderTcpServer` Bean만 등록하면 표준 TCP 서버를 갖춘다.
- **spider-batch** — batch-was에 내장. `JobProvider` 인터페이스 구현만으로 배치 잡을 등록할 수 있다.
- **reactive-springware** — `component-library/`가 `demo/front`, `react-cms` 등에 path alias(`@cl`)로 참조된다. Storybook으로 컴포넌트를 브라우징하고, Claude Code가 이 컴포넌트 정의를 기준으로 프론트엔드 코드를 생성한다.
- **admin** — FWK 메타테이블을 관리하는 운영 콘솔. 메타 변경 후 reload API를 호출하면 AP 서버 재기동 없이 라우팅이 갱신된다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.4, Spring Security 6, MyBatis 3 |
| Batch | Spring Batch 5.x, Quartz 3.x |
| Database | Oracle 11g+ |
| Distributed Lock | Redisson 3.27 (Redis) |
| TCP | 자체 구현 SpiderTcpServer (JSON / FixedLength / ObjectStream 프로토콜) |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4 |
| Build | Maven 3 (Wrapper), npm |
| CI/CD | GitHub Actions, SonarCloud |

---

## 프로젝트 구조

```
Spider-Framework/
│
├── admin/                   관리자 콘솔 (Spring Boot + Thymeleaf)
│   └── domain/              52개 비즈니스 모듈 (batch, gateway, monitor, user 등)
│
├── spider-link/             TCP 연계엔진 라이브러리 — AP 서버에 내장
│
├── spider-batch/            배치 오케스트레이션 라이브러리 — batch-was에 내장
│
├── batch-was/               배치 실행 WAS (Quartz 스케줄러, Redis 분산락)
│
├── reactive-springware/     금융 앱 특화 React 컴포넌트 라이브러리 + 디자인 토큰
│   └── component-library/   → demo/front 등에서 @cl path alias로 직접 참조
│
├── demo/
│   ├── bizApp/              POC 시연용 금융 AP 서버군 (spider-link 내장형 멀티모듈)
│   │   ├── biz-common/      커맨드 상수 공유 라이브러리
│   │   ├── mock-core/       계정계 Mock 서버 (TCP 19300)
│   │   ├── biz-auth/        인증AP (TCP 19100)
│   │   ├── biz-transfer/    이체AP (TCP 19200)
│   │   └── biz-channel/     채널AP — front & admin과 통신 (HTTP 18080, TCP 19400)
│   └── front/               POC 시연용 프론트엔드 (React + Vite)
│
├── react-cms/               리액트 기반 WYSIWYG CMS 툴 (React + Vite)
├── reactPlatform/           리액트 코드 생성 & 결재 관리 서버
├── preview-app/             reactPlatform 미리보기 앱 (React + Vite)
│
└── .github/                 CI/CD (GitHub Actions), 이슈/PR 템플릿
```

---

## 프로젝트 역할 요약

| 프로젝트 | 역할 |
|----------|------|
| `admin` | 플랫폼 전체 관리 콘솔 (FWK 메타·메뉴·권한·긴급공지 등) |
| `spider-link` | AP 서버 공통 TCP 연계엔진 **라이브러리** — 각 AP 서버에 내장 |
| `spider-batch` | 배치 오케스트레이션 **라이브러리** — batch-was에 내장 |
| `batch-was` | 배치 잡 실행 WAS (Quartz 스케줄러, Redis 분산락) |
| `reactive-springware` | 금융 앱 특화 React 컴포넌트 **라이브러리** + 디자인 토큰 시스템 |
| `demo/bizApp` | POC 시연용 금융 AP 서버군 (채널/인증/이체/계정계 Mock) |
| `demo/front` | POC 시연용 프론트엔드 (React, `@cl`로 component-library 참조) |
| `react-cms` | 리액트 기반 WYSIWYG CMS 툴 |
| `reactPlatform` | 리액트 컴포넌트 코드 자동 생성 + 결재 처리 |
| `preview-app` | reactPlatform에서 생성된 코드 미리보기 앱 |

---

## 빌드 순서 및 의존 관계

일부 모듈은 Maven 로컬 저장소에 먼저 설치되어야 하는 **라이브러리 의존 관계**가 있다.

```
spider-link ──► spider-batch ──► batch-was
    │
    ├──────────────────────────► demo/bizApp (biz-auth, biz-transfer, biz-channel)
    └──────────────────────────► admin
```

| 순서 | 모듈 | 명령 | 이유 |
|------|------|------|------|
| 1 | `spider-link` | `mvn install -f spider-link/pom.xml` | spider-batch·admin·bizApp 서버가 의존 |
| 2 | `spider-batch` | `mvn install -f spider-batch/pom.xml` | batch-was가 의존 |
| 3 | 나머지 | `mvn package -f <module>/pom.xml` | 위 두 라이브러리가 로컬 저장소에 있어야 빌드 가능 (환경변수 설정·상세 실행 방법은 각 프로젝트 README 참조) |

> **CI/CD**: GitHub Actions(`ci.yml`)가 위 순서를 자동으로 보장한다.  
> **로컬 최초 세팅 또는 라이브러리 변경 시**에는 위 순서대로 직접 `mvn install`을 실행해야 한다.

---

## 시스템 통신 흐름

```
[Admin: 8080]
    ├── TCP 9998  ──────────────────────► [batch-was: 8081]
    └── TCP 19400 ──────────────────────► [biz-channel]  ← 긴급공지

[demo/front (React)]
    └── HTTP 18080 ──────────────────────► [biz-channel: 18080]
                                               ├── TCP 19100 ──► [biz-auth]
                                               │                     └── TCP 19300 ──► [mock-core]
                                               └── TCP 19200 ──► [biz-transfer]
                                                                     └── TCP 19300 ──► [mock-core]
```

---

## 각 모듈 상세 가이드

| 모듈 | README |
|------|--------|
| admin | [admin/README.md](admin/README.md) |
| spider-link | [spider-link/README.md](spider-link/README.md) |
| spider-batch | [spider-batch/README.md](spider-batch/README.md) |
| batch-was | [batch-was/README.md](batch-was/README.md) |
| demo/bizApp | [demo/bizApp/README.md](demo/bizApp/README.md) |
