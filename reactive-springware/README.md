# Reactive Springware

금융 앱 특화 React 컴포넌트 라이브러리 + 디자인 토큰 시스템

Figma 디자인을 Claude Code에 입력하면, 이 라이브러리의 컴포넌트만 사용한 구조화된 React 코드를 자동으로 생성합니다.

---

## 목차

- [프로젝트 구조](#프로젝트-구조)
- [컴포넌트 라이브러리](#컴포넌트-라이브러리)
- [디자인 토큰 시스템](#디자인-토큰-시스템)
- [CSS 아키텍처](#css-아키텍처)
- [스크립트](#스크립트)
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
│   ├── styles.css            # Tailwind 빌드 엔트리 (build:css 입력 파일)
│   ├── dist/
│   │   └── styles.css        # 컴파일된 CSS 배포물 (소비 프로젝트가 직접 import)
│   └── index.ts              # 모든 컴포넌트 중앙 export (CSS import 없음)
├── design-tokens/
│   └── globals.css           # 전체 CSS 변수 정의 (브랜드·도메인 토큰 + Tailwind @theme)
├── figma-plugin/             # Figma 플러그인 (Figma 캔버스 ↔ 컴포넌트 동기화)
│   ├── commands/             # 커맨드 핸들러 (createComponents, createIcons, createVariables)
│   ├── utils/                # 유틸 (tokens, helpers, icons)
│   ├── components/           # 카테고리별 Figma 컴포넌트 생성 함수
│   └── dist/                 # 빌드 결과물 (main.js)
├── generated/                # 스크립트로 자동 추출된 Claude 컨텍스트 파일
├── scripts/                  # 추출 스크립트 (component-types, design-tokens, page-rules, generate-figma-icons)
├── lib/
│   └── cn.ts                 # tailwind-merge + clsx 유틸리티
├── docs/
│   ├── component-checklist.md     # 신규 컴포넌트 추가 체크리스트
│   ├── figma-plugin-guide.md      # Figma 플러그인 구조 및 사용 가이드
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
| **브랜드** | hana, ibk, kb, nh, shinhan, woori |
| **도메인** | banking, card, giro, insurance |

### globals.css 업데이트

Figma Variables가 변경된 경우 Token Studio로 `temp.json`을 export하여 Claude에게 전달하면 `globals.css`를 갱신합니다.

---

## CSS 아키텍처

### dist/styles.css — 유일한 CSS 배포물

소비 프로젝트는 `component-library/dist/styles.css` **하나만** import합니다.
이 파일은 `npm run build:css`로 생성되는 **순수 CSS 빌드물**입니다.

```bash
npm run build:css
```

| 파일 | 역할 |
|------|------|
| `component-library/styles.css` | Tailwind 빌드 엔트리 (`@import "tailwindcss"` + `@import globals.css`) |
| `component-library/dist/styles.css` | **배포물** — 컴파일된 순수 CSS (디자인 토큰 + Tailwind 유틸리티) |

### 왜 index.ts에서 CSS를 import하지 않는가

`@tailwindcss/vite` 플러그인이 활성화된 프로젝트에서, 같은 CSS 파일이 JS `import`와 CSS `@import` 두 경로로 동시에 처리되면 **이중 처리(double-processing)** 가 발생합니다.

두 번째 처리 시 Tailwind가 `@layer theme` 변수를 `initial`로 리셋하여 **브랜드 색상이 전혀 표시되지 않는** 문제가 생깁니다. `dist/styles.css`는 이미 컴파일된 순수 CSS이므로 `@tailwindcss/vite`가 재처리하지 않아 안전합니다.

### 소비 프로젝트별 CSS import

| 프로젝트 | Tailwind 엔트리 | dist/styles.css import 위치 |
|----------|----------------|------------------------------|
| `demo/front` | `src/index.css` | `@import "@cl/dist/styles.css"` (index.css) |
| `react-cms` | `src/index.css` | `@import "@cl/dist/styles.css"` (user-scope.css, 캔버스 격리) |
| `preview-app` | `src/index.css` | `@import "@cl/dist/styles.css"` (index.css) |
| Storybook | `.storybook/preview.ts` | `import '../component-library/dist/styles.css'` |

---

## 스크립트

| 명령어 | 설명 |
|--------|------|
| `npm run build:css` | `globals.css` + Tailwind → `component-library/dist/styles.css` 빌드. **globals.css 수정 후 반드시 실행** |
| `npm run build:css:watch` | `build:css`의 파일 감시(watch) 모드. 변경 시 자동으로 재빌드 |
| `npm run build:plugin` | figma-plugin 번들링 → `figma-plugin/dist/main.js` 생성. **figma-plugin 수정 후 실행** |
| `npm run watch:plugin` | `build:plugin`의 파일 감시 모드. 수정 시 자동으로 재빌드 |
| `npm run generate:icons` | lucide-react 전체 아이콘 SVG 추출 → `figma-plugin/utils/icons.ts` 자동 생성 |
| `npm run generate:prompts` | 컴포넌트·디자인 토큰·페이지 규칙을 `generated/` 폴더에 마크다운으로 추출. **컴포넌트 추가·수정 후 반드시 실행** |
| `npm run typecheck` | TypeScript 타입 오류 검사 (파일 출력 없음) |
| `npm run lint` | ESLint 정적 분석 |
| `npm run storybook` | Storybook 개발 서버 실행 (`http://localhost:6006`) |
| `npm run build-storybook` | Storybook 정적 빌드 |

### 주요 작업별 실행 순서

**globals.css 수정 후**

```bash
npm run build:css           # dist/styles.css 재빌드
```

**컴포넌트 추가·수정 후**

```bash
npm run generate:prompts    # generated/ 갱신 (Claude API System Prompt 소스)
npm run build:css           # 새 컴포넌트의 Tailwind 클래스가 dist/styles.css에 포함되도록 재빌드
```

**figma-plugin 수정 후**

```bash
npm run build:plugin        # figma-plugin/dist/main.js 재빌드 후 Figma에서 재실행
```

**lucide-react 버전 업 후**

```bash
npm run generate:icons      # figma-plugin/utils/icons.ts 재생성
npm run build:plugin        # figma-plugin 번들 재빌드
```

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

**CSS import (`src/index.css`):**

```css
@import "tailwindcss";
@import "@cl/dist/styles.css";
```

### react-cms

CMS 빌더. 컴포넌트를 블록·레이아웃·오버레이로 등록해 페이지를 시각적으로 구성하며,
저장 시 `demo/front`에 페이지 파일과 라우트를 자동 생성합니다.

**vite.config.ts alias 설정:**

```ts
'@cl': resolve(__dirname, '../reactive-springware/component-library'),
'@lib': resolve(__dirname, '../reactive-springware/lib'),
```

**CSS 구조:**
- CMS 셸 스타일: `src/index.css`가 `@tailwindcss/vite` 엔트리 역할
- 캔버스·미리보기 영역: `user-scope.css`의 `@import "@cl/dist/styles.css"`로 격리 적용

**브랜드 테마 설정 (`.env`):**

```bash
VITE_CMS_BRAND=hana   # hana | kb | ibk | woori | shinhan | nh (미설정 시 hana)
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

**CSS import (`src/index.css`):**

```css
@import "tailwindcss";
@import "@cl/dist/styles.css";
```

생성된 컴포넌트에 `data-brand` / `data-domain` 속성이 포함되어 있으면 브랜드·도메인 CSS 변수가 자동 적용됩니다.

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
1. temp.json 파일을 Claude에게 전달하며 globals.css 재생성 요청

2. Claude가 temp.json을 파싱하여 globals.css 전체 재생성
   ├─ [data-brand='*'] 블록 — brand.* 토큰에서 생성
   ├─ [data-domain='*'] 블록 — domain.* 토큰에서 생성
   ├─ @theme 블록 — semantic.* / primitives 토큰에서 생성
   └─ @layer base 블록 — 전역 기본 스타일 유지

3. temp.json 삭제
```

