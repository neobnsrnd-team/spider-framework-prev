# POC HNC

하나카드 POC(Proof of Concept) 프로젝트 모노레포.

## 디렉터리 구조

```
 POC_HNC/
  │                                                                                                                         
  ├── [Backend — Spring Boot 3.4 / Maven]
  │   ├── admin/               관리자 콘솔 서버 (메뉴·권한·사용자·플랫폼 운영)                                              
  │   │   └── domain/          52개 비즈니스 모듈 (batch, gateway, monitor, user, wasinstance 등)
  │   │                                        
  │   ├── spider-link/         공통 연계엔진 라이브러리 (각 AP 서버에 내장)
  │   │
  │   ├── spider-batch/        Spring Batch 실행 서버
  │   │
  │   └── reactPlatform/       리액트 코드 생성 & 결재 관리 서버
  │   │
  │   └── demo/bizApp          하나카드 POC 시연용 backend 서버
  │
  ├── [Frontend — React / TypeScript / Vite]
  │   ├── react-cms/            CMS 대시보드 UI
  │   └── preview-app/          POC 시연 데모 앱 UI
  │   ├── reactive-springware/  리액트 컴포넌트 코드 생성 플랫폼 (디자인 토큰 포함)
  │   └── demo/front            하나카드 POC 시연용 (front)
  │
  └── [설정]
      ├── .github/             CI/CD (GitHub Actions), 이슈/PR 템플릿
      └── CLAUDE.md            프로젝트 작업 규칙

  ---
  프로젝트별 역할 요약

  ┌─────────────────────┬───────────────────────────────────────────────────┐
  │      프로젝트        │                       역할                        │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ admin               │ 플랫폼 전체 관리 콘솔, 가장 규모가 큰 백엔드         │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ spider-link         │ 서버 간 공통 연계 기능을 제공하는 공유 라이브러리     │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ batch-was           │ 배치 잡 실행 및 이력 관리 서버                      │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ reactPlatform       │ 리액트 컴포넌트 코드 자동 생성 + 결재 처리           │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ react-cms           │ CMS 대시보드 프론트엔드                             │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ preview-app         │ POC 시연용 프론트엔드                               │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ reactive-springware │ 디자인 토큰 기반 컴포넌트 코드 생성 플랫폼           │
  ├─────────────────────┼───────────────────────────────────────────────────┤
  │ demo                │ 하나카드 POC 시연용 풀스택 앱                       │
  └─────────────────────┴───────────────────────────────────────────────────┘
```

## 빌드 순서 및 의존 관계

일부 모듈은 Maven 로컬 저장소에 먼저 설치되어야 하는 **라이브러리 의존 관계**가 있다.

```
spider-link  →  spider-batch  →  admin
    ↓
  (모든 bizApp 서버)
```

| 순서 | 모듈 | 명령 | 이유 |
|------|------|------|------|
| 1 | spider-link | `mvn install -f spider-link/pom.xml` | spider-batch·admin·bizApp 서버가 의존 |
| 2 | spider-batch | `mvn install -f spider-batch/pom.xml` | spider-link에 의존, admin이 의존 |
| 3 | admin 등 | `mvn package -f admin/pom.xml` | 위 두 라이브러리가 로컬 저장소에 있어야 빌드 가능 |

> **CI/CD**: GitHub Actions(`ci.yml`)가 위 순서를 자동으로 보장하므로, CI 환경에서는 수동 실행 불필요.  
> **로컬 최초 세팅 또는 spider-link·spider-batch 변경 시**에는 위 순서대로 직접 `mvn install`을 실행해야 한다.

## 각 모듈 개요

### admin
`reactive-springware` 및 `demo`의 관리자 서버. Spring Boot 3.4 + Thymeleaf 기반 미들웨어 관리 콘솔로, 메뉴·권한·사용자·긴급공지 등 플랫폼 운영에 필요한 기능을 제공한다.

→ 상세 가이드: [admin/README.md](admin/README.md)

### demo
하나카드 POC 시연을 위한 데모 애플리케이션. `backend`와 `front`로 구성된다.

### reactive-springware
리액트 컴포넌트 코드를 생성해주는 플랫폼.
