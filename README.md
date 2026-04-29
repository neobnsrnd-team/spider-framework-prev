# POC HNC

하나카드 POC(Proof of Concept) 프로젝트 모노레포.

## 디렉터리 구조

```
POC_HNC/
│
├── [Backend — Spring Boot 3.4 / Maven]
│   ├── admin/           관리자 콘솔 서버 (메뉴·권한·사용자·플랫폼 운영, 긴급공지)
│   │
│   ├── spider-link/     공통 연계엔진 라이브러리 (SpiderTcpServer, MetaDrivenCommandHandler)
│   │                    → 각 AP 서버에 내장되어 TCP 서버 인프라를 제공
│   │
│   ├── spider-batch/    배치 오케스트레이션 라이브러리 (Spring Batch 5.x 기반)
│   │                    → batch-was에 내장되어 배치 실행·이력·TCP 연동을 제공
│   │
│   ├── batch-was/       배치 실행 WAS (spider-batch 내장, Quartz 스케줄러, Redis 분산락)
│   │
│   ├── reactPlatform/   리액트 코드 생성 & 결재 관리 서버
│   │
│   └── demo/
│       ├── bizApp/      하나카드 POC 시연용 금융 AP 서버군 (spider-link 내장형)
│       │   ├── biz-common/   커맨드 상수 공유 라이브러리
│       │   ├── mock-core/    계정계 Mock (TCP 19300, 고정 길이 바이너리)
│       │   ├── biz-auth/     인증AP (TCP 19100)
│       │   ├── biz-transfer/ 이체AP (TCP 19200)
│       │   └── biz-channel/  채널AP (HTTP 18080, TCP 19400)
│       └── front/       시연용 프론트엔드 (React)
│
├── [Frontend — React / TypeScript / Vite]
│   ├── react-cms/           CMS 대시보드 UI
│   ├── preview-app/         POC 시연 데모 앱 UI
│   └── reactive-springware/ 리액트 컴포넌트 코드 생성 플랫폼 (디자인 토큰 포함)
│
└── [설정]
    ├── .github/             CI/CD (GitHub Actions), 이슈/PR 템플릿
    └── CLAUDE.md            프로젝트 작업 규칙
```

## 프로젝트 역할 요약

| 프로젝트 | 역할 |
|----------|------|
| `admin` | 플랫폼 전체 관리 콘솔 (Spring Boot + Thymeleaf) |
| `spider-link` | AP 서버 공통 TCP 연계엔진 **라이브러리** — 각 AP 서버에 내장 |
| `spider-batch` | 배치 오케스트레이션 **라이브러리** — batch-was에 내장 |
| `batch-was` | 배치 잡 실행 WAS (Quartz 스케줄러, Redis 분산락) |
| `reactPlatform` | 리액트 컴포넌트 코드 자동 생성 + 결재 처리 |
| `demo/bizApp` | 하나카드 POC 시연용 AP 서버군 (채널/인증/이체/계정계 Mock) |
| `react-cms` | CMS 대시보드 프론트엔드 |
| `preview-app` | POC 시연용 프론트엔드 |
| `reactive-springware` | 디자인 토큰 기반 컴포넌트 코드 생성 플랫폼 |

## 빌드 순서 및 의존 관계

일부 모듈은 Maven 로컬 저장소에 먼저 설치되어야 하는 **라이브러리 의존 관계**가 있다.

```
spider-link ──► spider-batch ──► batch-was
    │
    └──────────────────────────► demo/bizApp (biz-auth, biz-transfer, biz-channel)
    └──────────────────────────► admin
```

| 순서 | 모듈 | 명령 | 이유 |
|------|------|------|------|
| 1 | `spider-link` | `mvn install -f spider-link/pom.xml` | spider-batch·admin·bizApp 서버가 의존 |
| 2 | `spider-batch` | `mvn install -f spider-batch/pom.xml` | batch-was가 의존 |
| 3 | 나머지 | `mvn package -f <module>/pom.xml` | 위 두 라이브러리가 로컬 저장소에 있어야 빌드 가능 |

> **CI/CD**: GitHub Actions(`ci.yml`)가 위 순서를 자동으로 보장한다.
> **로컬 최초 세팅 또는 라이브러리 변경 시**에는 위 순서대로 직접 `mvn install`을 실행해야 한다.

## 시스템 통신 흐름

```
[Admin: 8080]
    ├── TCP 9998  ──────────────────────► [batch-was: 8081]
    └── TCP 19400 ──────────────────────► [biz-channel: 18080]  ← 긴급공지

[React 프론트엔드]
    └── HTTP 18080 ──────────────────────► [biz-channel]
                                               ├── TCP 19100 ──► [biz-auth]
                                               │                     └── TCP 19300 ──► [mock-core]
                                               └── TCP 19200 ──► [biz-transfer]
                                                                     └── TCP 19300 ──► [mock-core]
```

## 각 모듈 상세 가이드

| 모듈 | README |
|------|--------|
| admin | [admin/README.md](admin/README.md) |
| spider-link | [spider-link/README.md](spider-link/README.md) |
| spider-batch | [spider-batch/README.md](spider-batch/README.md) |
| batch-was | [batch-was/README.md](batch-was/README.md) |
| demo/bizApp | [demo/bizApp/README.md](demo/bizApp/README.md) |
