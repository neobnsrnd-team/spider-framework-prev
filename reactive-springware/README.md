# Reactive Springware

금융 앱 특화 React 컴포넌트 라이브러리 + 디자인 토큰 시스템

Figma 디자인을 Claude Code에 입력하면, 이 라이브러리의 컴포넌트만 사용한 구조화된 React 코드를 자동으로 생성합니다.

---

## 목차

- [프로젝트 구조](#프로젝트-구조)
- [컴포넌트 라이브러리](#컴포넌트-라이브러리)
- [디자인 토큰 시스템](#디자인-토큰-시스템)
- [Storybook](#storybook)
- [Generated 파일](#generated-파일)
- [연동 프로젝트](#연동-프로젝트)
- [컴포넌트 추가 가이드](#컴포넌트-추가-가이드)
- [디자인 토큰 업데이트](#디자인-토큰-업데이트)

---

## 프로젝트 구조

```
reactive-springware/
├── component-library/        # React 컴포넌트
│   ├── core/                 # 원자 컴포넌트 (Button, Input, Badge, Select, Typography)
│   ├── layout/               # 레이아웃 컴포넌트 (PageLayout, Stack, Grid 등)
│   ├── modules/              # 분자 컴포넌트
│   │   ├── common/           # 도메인 무관 (Modal, BottomSheet, TabNav 등 30개+)
│   │   └── banking/          # 뱅킹 특화 (TransferForm, OtpInput, NumberKeypad 등)
│   ├── biz/                  # 비즈니스 컴포넌트
│   │   ├── common/           # 공통 (BannerCarousel, QuickMenuGrid 등)
│   │   ├── banking/          # 뱅킹 (AccountSummaryCard, AccountSelectorCard)
│   │   ├── card/             # 카드 (CardVisual, CardPaymentSummary 등 24개)
│   │   └── insurance/        # 보험 (InsuranceSummaryCard)
│   ├── pages/                # 샘플 페이지 컴포넌트 (banking 10개 / card 11개 / common 3개)
│   └── index.ts              # 모든 컴포넌트 중앙 export
├── design-tokens/
│   ├── globals.css           # 자동 생성 CSS (직접 수정 금지)
│   └── figma-tokens/         # Token Studio JSON 원본
├── generated/                # 스크립트로 자동 추출된 Claude 컨텍스트 파일
├── scripts/                  # 추출 스크립트 (component-types, design-tokens, page-rules)
├── lib/
│   └── cn.ts                 # tailwind-merge + clsx 유틸리티
├── docs/
│   ├── component-checklist.md     # 신규 컴포넌트 추가 체크리스트
│   └── page-generation-rules.md   # 페이지 코드 생성 규칙
└── .storybook/               # Storybook 설정
```

---

## 컴포넌트 라이브러리

### 카테고리 구조

컴포넌트는 4개 레이어로 구성됩니다. 상위 레이어는 하위 레이어의 컴포넌트를 조합해 만듭니다.

```
pages  ←  biz  ←  modules  ←  core / layout
```

| 레이어 | 위치 | 설명 |
|--------|------|------|
| **core** | `core/` | 가장 작은 원자 단위. Button, Input, Badge, Select, Typography |
| **layout** | `layout/` | 페이지 골격. PageLayout, HomePageLayout, Stack, Grid, Inline, Section, BottomNav 등 |
| **modules** | `modules/` | 여러 core를 조합한 재사용 단위. Modal, BottomSheet, TabNav, DatePicker 등 |
| **biz** | `biz/` | 도메인 비즈니스 로직이 포함된 컴포넌트. AccountSummaryCard, CardVisual 등 |
| **pages** | `pages/` | 실제 화면 단위 샘플. biz + layout 조합 |

### 컴포넌트 파일 구조

```
ComponentName/
  ├── index.tsx                    # 컴포넌트 구현
  ├── types.ts                     # Props 인터페이스
  └── ComponentName.stories.tsx    # Storybook 스토리
```

### Import

```ts
import { Button, Modal, AccountSummaryCard } from '@cl';
// vite.config.ts에서 '@cl' → reactive-springware/component-library 로 alias 설정
```

---

## 디자인 토큰 시스템

### 토큰 레이어 구조

CSS 변수는 4개 레이어로 적용됩니다.

```
[data-domain="banking"]  →  도메인 고정값 (배경색 등)
[data-brand="hana"]      →  브랜드 가변 색상 (6개 은행 + 카드/지로)
@theme                   →  공통 시맨틱 토큰
@layer base              →  전역 기본 스타일
```

### 지원 브랜드 / 도메인

| 구분 | 항목 |
|------|------|
| **브랜드** | hana, ibk, kb, nh, shinhan, woori, card |
| **도메인** | banking, card, giro, insurance |

### globals.css는 자동 생성 파일

`design-tokens/globals.css`는 직접 수정하지 않습니다.
`figma-tokens/*.json` → 변환 스크립트 → `globals.css` 순서로만 업데이트됩니다.

### figma-tokens 파일 분류

| 파일 | 포함 토큰 |
|------|-----------|
| `primitives.json` | spacing, radius, text, font, shadow, transition, breakpoint, nav, z |
| `semantic.json` | color.brand, color.surface, color.text, color.danger, color.success 등 |
| `brand.{키}.json` | 브랜드별 색상 (hana, ibk, kb, nh, shinhan, woori, card) |
| `domain.{키}.json` | 도메인별 토큰 (banking, card, giro, insurance) |

---

## Storybook

```bash
npm run storybook
```

브라우저에서 `http://localhost:6006` 접속. 모든 컴포넌트의 Props, 상태, 변형을 브라우저에서 확인할 수 있습니다.

---

## Generated 파일

`scripts/` 하위 추출 스크립트가 `generated/` 폴더에 마크다운 파일을 자동 생성합니다.
이 파일들은 **reactPlatform에서 Claude API 호출 시 System Prompt로 주입**되어, Claude가 이 라이브러리의 컴포넌트와 규칙을 기반으로 코드를 생성하도록 안내합니다.

```bash
npm run generate:prompts
```

| 파일 | 설명 |
|------|------|
| `generated/component-types.md` | 모든 컴포넌트 Props API 레퍼런스 |
| `generated/design-tokens.md` | 모든 CSS 변수·토큰 레퍼런스 |
| `generated/page-generation-rules.md` | 페이지 생성 규칙 |

> 컴포넌트 추가·수정 후에는 반드시 `npm run generate:prompts`를 실행해 갱신합니다.

---

## 연동 프로젝트

이 라이브러리는 npm 배포 없이 **vite 경로 alias**로 직접 참조합니다.

### demo/front

최종 사용자 앱. react-cms에서 생성한 페이지 또는 수동 개발한 화면이 실행됩니다.

**vite.config.ts alias 설정:**

```ts
'@cl': resolve(__dirname, '../../reactive-springware/component-library'),
'@lib': resolve(__dirname, '../../reactive-springware/lib'),
```

### react-cms

CMS 빌더. 컴포넌트를 블록·레이아웃·오버레이로 등록해 페이지를 시각적으로 구성하며,
저장 시 `demo/front`에 페이지 파일과 라우트를 자동 생성합니다.

**vite.config.ts alias 설정:**

```ts
'@cl': resolve(__dirname, '../reactive-springware/component-library'),
'@lib': resolve(__dirname, '../reactive-springware/lib'),
```

### reactPlatform + preview-app

`generated/` 폴더의 파일을 Claude API System Prompt로 주입해 React 코드를 생성합니다.
생성된 코드는 **preview-app**에서 미리보기로 확인할 수 있습니다.

preview-app은 빌드 결과물을 reactPlatform(Spring Boot)의 정적 리소스 경로로 출력하며,
`/preview-app/**` 경로로 서빙됩니다.

**preview-app/vite.config.ts alias 설정:**

```ts
'@cl': resolve(__dirname, '../reactive-springware/component-library'),
'@lib': resolve(__dirname, '../reactive-springware/lib'),
```

> 컴포넌트·토큰·페이지 규칙이 변경되면 `npm run generate:prompts`로 갱신 후 preview-app을 재빌드합니다.

---

## 컴포넌트 추가 가이드

신규 컴포넌트를 추가할 때는 반드시 아래 체크리스트를 따릅니다.

```
docs/component-checklist.md
```

---

## 디자인 토큰 업데이트

Figma Variables → Token Studio export → `temp.json` 파일을 받은 경우 아래 절차를 따릅니다.

```
1. temp.json 파싱
   └─ 최상위 키(primitives / semantic / brand.* / domain.*)를 기준으로 분류

2. figma-tokens/*.json 업데이트 (카테고리별 분배)
   ├─ primitives.json  — spacing, radius, text, font, shadow, transition, breakpoint, nav, z
   ├─ semantic.json    — color.*
   ├─ brand.{키}.json  — 브랜드별 토큰
   └─ domain.{키}.json — 도메인별 토큰

3. figma-tokens/*.json → globals.css 변환
   └─ globals.css 자동 재생성

4. temp.json 삭제
```

토큰 시스템 상세 아키텍처는 `design-tokens/README.md`를 참고하세요.
