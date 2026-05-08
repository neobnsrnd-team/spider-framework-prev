# react-cms

React 기반 비주얼 CMS 빌더 및 런타임 렌더링 엔진.

블록 팔레트에서 컴포넌트를 드래그&드롭하여 페이지를 구성하고, 저장 시 사용자가 지정한 위치에 React JSX 코드를 생성한다. 라우트 등록은 호출 측 개발자가 직접 수행한다.

---

## 목차

- [주요 기능](#주요-기능)
- [디렉토리 구조](#디렉토리-구조)
- [아키텍처 개요](#아키텍처-개요)
- [개발 환경 실행](#개발-환경-실행)
- [핵심 개념](#핵심-개념)
  - [BlockDefinition](#blockdefinition)
  - [LayoutTemplate](#layouttemplate)
  - [OverlayTemplate](#overlaytemplate)
  - [PropField](#propfield)
  - [Action / BlockInteraction](#action--blockinteraction)
- [빌더 사용법](#빌더-사용법)
  - [CMSApp](#cmsapp)
  - [CMSBuilder](#cmsbuilder)
- [런타임 사용법](#런타임-사용법)
  - [CMSRuntimeProvider](#cmsruntimeprovider)
  - [PageRenderer](#pagerenderer)
- [코드 생성](#코드-생성)
- [Vite 플러그인 (cmsBankPlugin)](#vite-플러그인-cmsbankplugin)
- [CSS 격리 (UserScopeWrapper)](#css-격리-userscopewrapper)
- [타입 레퍼런스](#타입-레퍼런스)

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 비주얼 에디터 | 드래그&드롭으로 블록을 배치하고 실시간으로 속성 편집 |
| 레이아웃 시스템 | 헤더/푸터 크롬을 레이아웃 템플릿으로 분리하여 관리 |
| 오버레이 관리 | 바텀시트·모달 등 오버레이를 페이지 단위로 구성 |
| 상호작용 바인딩 | 블록 이벤트에 `openOverlay` / `navigate` / `closeOverlay` 액션 연결 |
| JSX 코드 생성 | 편집 결과를 독립 실행 가능한 React 컴포넌트 파일로 자동 생성 |
| 페이지 파일 저장 | 저장 시 Vite 플러그인이 사용자가 지정한 위치에 `.tsx` 파일을 생성 (라우트 등록은 직접) |
| CSS 격리 | `@scope` 기반으로 외부 스타일과 CMS 스타일을 격리 |
| 런타임 렌더링 | 저장된 CMSPage JSON을 빌더 없이 앱에서 렌더링 |

---

## 디렉토리 구조

```
react-cms/
├── src/
│   ├── cms-core/                  # CMS 엔진 핵심 (외부 export 대상)
│   │   ├── index.ts               # 공개 API 진입점
│   │   ├── types.ts               # 전체 타입 정의
│   │   ├── CMSApp.tsx             # 빌더 앱 루트 (Router + Provider 포함)
│   │   ├── CMSBuilder.tsx         # 에디터 UI (팔레트 · 캔버스 · 속성 패널)
│   │   ├── CMSRuntimeProvider.tsx # 런타임 전용 경량 Provider
│   │   ├── SavePageModal.tsx      # 페이지 저장 모달 (pageName · savePath 입력)
│   │   ├── UserScopeWrapper.tsx   # @scope 기반 CSS 격리 래퍼
│   │   ├── context.ts             # React Context 정의
│   │   ├── useCMSContextValues.ts # Context 값 파생 훅
│   │   ├── api/
│   │   │   └── defaultSave.ts     # 기본 저장 핸들러 (/__cms/create-page 호출)
│   │   ├── canvas/
│   │   │   ├── LayoutCanvas.tsx   # 메인 편집 캔버스
│   │   │   ├── OverlayCanvas.tsx  # 오버레이 편집 캔버스
│   │   │   └── BlockControls.tsx  # 블록 선택·이동·삭제 컨트롤
│   │   ├── codegen/
│   │   │   ├── exportCode.ts      # CMSPage → JSX 코드 생성 (codegenProps/icon-picker 변환 포함)
│   │   │   └── exportJson.ts      # CMSPage → JSON 직렬화
│   │   ├── inspector/
│   │   │   ├── RightSidebar.tsx   # 우측 속성 패널
│   │   │   ├── PropsEditor.tsx    # 블록 props 편집기
│   │   │   ├── LayoutEditor.tsx   # 레이아웃 props 편집기
│   │   │   ├── OverlayEditor.tsx  # 오버레이 편집기
│   │   │   ├── ArrayField.tsx     # 배열·중첩 배열 필드 공용 컴포넌트
│   │   │   ├── FieldControl.tsx   # leaf 필드 컨트롤(string/number/boolean/select/icon-picker)
│   │   │   └── IconPicker.tsx     # 아이콘 선택기 (초기 120개 + "더 보기" 확장)
│   │   ├── palette/
│   │   │   ├── LeftSidebar.tsx    # 좌측 팔레트 패널
│   │   │   ├── BlockPalette.tsx   # 블록 목록
│   │   │   └── OverlayPalette.tsx # 오버레이 목록
│   │   ├── preview/
│   │   │   └── PreviewPage.tsx    # 전체 화면 미리보기
│   │   ├── runtime/
│   │   │   └── renderPage.tsx     # CMSPage 런타임 렌더러
│   │   ├── state/
│   │   │   ├── builderStore.ts    # 빌더 상태 (블록 · 레이아웃 · 오버레이)
│   │   │   ├── overlayStore.tsx   # 런타임 오버레이 열기/닫기 상태
│   │   │   └── useDragSort.ts     # @dnd-kit 기반 드래그 정렬 훅
│   │   └── utils/
│   │       └── icon.tsx           # resolveIcon: kebab-case → Lucide ReactNode 변환
│   ├── cms-meta/                  # 프로젝트별 CMS 설정 (커스터마이징 대상)
│   │   ├── index.ts
│   │   ├── blocks.tsx             # 사용 가능한 블록 목록 (codegenProps 정의 포함)
│   │   ├── layouts.tsx            # 레이아웃 템플릿 + 렌더러
│   │   └── overlays.tsx           # 오버레이 템플릿
│   ├── cms-admin/                 # 어드민 모드 권한 가드·인증
│   │   ├── CmsAuthGuard.tsx       # /__cms/api/me 권한 확인 라우트 가드
│   │   ├── current-user.ts        # 현재 사용자 컨텍스트
│   │   ├── NotAuthorizedPage.tsx  # 권한 부족 시 표시 페이지
│   │   └── __tests__/             # 인증 모듈 테스트
│   ├── db/                        # Oracle DB 연동 (서버 사이드 전용)
│   │   ├── connection.ts          # 커넥션 풀 (Thick 모드, CLOB 자동 변환)
│   │   ├── repository/
│   │   │   └── page.repository.ts # CMS 페이지 CRUD
│   │   └── types.ts
│   ├── lib/                       # 공용 유틸리티
│   │   ├── env.ts                 # 서버 사이드 환경변수 (fail-fast)
│   │   └── client-env.ts          # 클라이언트 환경변수 (cmsBase 등)
│   ├── shared/
│   │   └── icons/                 # 아이콘 레지스트리
│   ├── vite-plugin/
│   │   └── cmsBankPlugin.ts       # 페이지 파일 생성 Vite 플러그인
│   ├── assets/                    # 정적 자산 (hero·로고)
│   ├── cms.config.ts              # CMS 앱 설정 진입점
│   ├── main.tsx                   # Vite dev 앱 진입점
│   ├── savePage.ts                # 외부 사용 가능한 저장 함수
│   ├── index.css                  # CMS 셸 전역 스타일 (@theme 색상 토큰)
│   └── user-scope.css             # @scope 기반 사용자 영역 스타일
├── public/
├── vite.config.ts
└── package.json
```

---

## 아키텍처 개요

react-cms는 **빌더**와 **런타임** 두 가지 모드로 동작한다.

```
┌─────────────────────────────────────────────────────┐
│                    CMSApp (빌더)                      │
│                                                     │
│  LeftSidebar     LayoutCanvas      RightSidebar     │
│  (블록 팔레트)  ←  드래그&드롭  →  (속성 편집)        │
│                                                     │
│  저장 → generateJSX → defaultSave → cmsBankPlugin   │
│                  ↓                       ↓          │
│            JSX 코드 문자열      savePath에 .tsx 작성 │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│              CMSRuntimeProvider (런타임)              │
│                                                     │
│  CMSPage JSON → PageRenderer → 블록 컴포넌트 렌더링  │
│                             → 오버레이 열기/닫기      │
│                             → 레이아웃 크롬 렌더링    │
└─────────────────────────────────────────────────────┘
```

### 저장 흐름

```
SavePageModal (pageName, savePath 입력)
  → generateJSX(CMSPage) → JSX 코드 문자열
  → defaultSave → POST /__cms/create-page
  → cmsBankPlugin (Vite dev 미들웨어)
      └── {savePath}/{PageName}.tsx 생성

※ 라우트 등록은 자동 수행하지 않는다. 생성된 페이지를 라우터에 노출하려면
   호출 측 프로젝트의 라우터 파일에 import 문과 라우트 항목을 직접 추가한다.
```

---

## 개발 환경 실행

### .env 설정

실행 전 프로젝트 루트에 `.env` 파일을 생성한다. `.env.example`을 복사해 사용한다.

```bash
cp .env.example .env
```

| 변수 | 설명 | 기본값 |
|---|---|---|
| `VITE_CMS_BRAND` | CMS 셸 브랜드 테마 색상 | `hana` |
| `ORACLE_USER` / `ORACLE_PASSWORD` 등 | Oracle DB 접속 정보 | — |
| `SPIDER_ADMIN_API_URL` | Spider Admin API 베이스 URL (어드민 모드에서 세션 인증에 사용) | — |

**`VITE_CMS_BRAND`** 값에 따라 CMS 빌더 UI의 주요 색상(헤더, 버튼 등)이 변경된다.

```bash
VITE_CMS_BRAND=hana   # hana | kb | ibk | woori | shinhan | nh
```

### 실행

#### 단독 실행 (파일시스템 저장)

```bash
cd react-cms
npm install
npm run dev
```

빌더 UI는 `http://localhost:5273/builder` 에서 접근한다. 페이지는 파일시스템(`demo/front`)에 저장된다.

> Vite dev server 포트는 `vite.config.ts`에서 `5273`으로 고정한다. 다른 Vite 프로젝트(데모 등)와 동시 실행 시 충돌을 피하기 위함이며, spider-admin nginx 프록시 설정도 같은 값을 가리킨다.

> `cmsBankPlugin`이 `react-cms` Vite 서버에 `/__cms/create-page` 엔드포인트를 등록하므로,
> 저장 기능을 사용하려면 `demo/front`가 실행 중일 필요 없이 `react-cms` dev 서버만 실행하면 된다.
> 단, 생성된 파일이 반영되려면 `demo/front` dev 서버도 함께 실행해야 한다.

#### 어드민 모드 (spider-admin + Oracle DB 저장)

```bash
cd react-cms
npm run dev:proxy
```

spider-admin(`:8080`)과 nginx 프록시(`:9000`)가 실행 중이어야 세션 쿠키가 공유되어 로그인 사용자로 저장된다.
빌더 UI는 `http://localhost:9000/react-cms/builder` 에서 접근한다.

---

## 핵심 개념

### BlockDefinition

팔레트에 등록할 블록 하나를 정의한다.

```typescript
interface BlockDefinition {
  meta: BlockMeta;
  component: React.ComponentType<Record<string, unknown>>;
  /**
   * 코드 생성 시 props 변환 함수.
   * component 함수 내부에 props 변환 로직(타입 변환·기본값 주입 등)이 있다면
   * 동일한 변환을 반환해야 코드 생성 결과가 캔버스 동작과 일치한다.
   * { __jsx: "..." } 마커를 반환하면 raw JSX expression으로 출력된다.
   * 미정의 시 propSchema의 icon-picker 필드는 자동으로 JSX로 변환된다.
   */
  codegenProps?: (props: Record<string, unknown>) => Record<string, unknown>;
  /** codegenProps가 생성하는 JSX 안에서 참조하는 추가 컴포넌트명 (import에 자동 포함) */
  codegenImports?: string[];
}

interface BlockMeta {
  name: string;                              // 블록 식별자 (고유)
  category: BlockCategory;                   // "core" | "biz" | ...
  domain?: BlockDomain;                      // 도메인 그룹핑용 레이블
  defaultProps: Record<string, unknown>;     // 캔버스 추가 시 초기값
  propSchema: Record<string, PropField>;     // 우측 패널 편집 폼 스키마
}
```

#### codegenProps 사용 예시

`component` 함수가 캔버스 미리보기용 변환을 수행하는 경우, 동일한 변환을 코드 생성에도 적용해야 한다.

```typescript
// OtpInput: select가 반환하는 문자열 → 숫자 변환
{
  meta: { propSchema: { length: { type: "select", options: ["4", "6"], default: "6" } } },
  component:    (p) => <OtpInput {...p} length={Number(p.length) as 4 | 6} />,
  codegenProps: (p) => ({ ...p, length: Number(p.length) }),
}

// Card: header/rows를 자식 JSX로 변환 + 추가 컴포넌트 import
{
  component: (p) => <Card>...<CardHeader />...<CardRow /></Card>,
  codegenProps: (p) => ({
    interactive: p.interactive,
    children:    { __jsx: `<CardHeader title="${p.title}" /><CardRow ... />` },
  }),
  codegenImports: ["CardHeader", "CardRow"],
}

// NumberKeypad: JSON에 저장되지 않는 필수 props 주입
{
  meta: { propSchema: { onDigitPress: { type: "event" } } },
  codegenProps: () => ({ digits: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] }),
}
```

> `propSchema`에 `type: "event"` 필드가 있고 interaction에 바인딩되지 않으면, 코드 생성 시 `() => {}` noop이 자동 주입된다 (필수 콜백 타입 에러 방지).

#### 카테고리별 export 그룹 패턴

`blocks.tsx`는 BlockDefinition을 카테고리별 배열로 묶어 export하는 컨벤션을 따른다.
CMSApp에는 합쳐서 전달하되, 인스펙터 팔레트는 `BlockMeta.category`로 그룹핑해 표시한다.

```typescript
// src/cms-meta/blocks.tsx
export const coreBlocks: BlockDefinition[] = [
  BadgeDefinition, ButtonDefinition, InputDefinition, /* ... */
];

export const bizBlocks: BlockDefinition[] = [
  // biz/common
  BannerCarouselDefinition, UserProfileDefinition,
  // biz/banking
  AccountSummaryCardDefinition,
  // biz/card
  CardSummaryCardDefinition, CardVisualDefinition, /* ... */
  // biz/insurance
  InsuranceSummaryCardDefinition,
];

export const moduleBlocks: BlockDefinition[] = [
  // modules/common
  CardDefinition, CollapsibleSectionDefinition, DatePickerDefinition, /* ... */
  // modules/banking
  AmountInputDefinition, NumberKeypadDefinition, OtpInputDefinition, /* ... */
];

export const blocks: BlockDefinition[] = [...coreBlocks, ...bizBlocks, ...moduleBlocks];
```

신규 블록 추가 시:
1. `meta.category`에 `"core" | "biz" | "modules"` 중 하나 지정
2. `meta.domain`에 도메인 그룹(`"common" | "banking" | "card" | "insurance"`) 지정 (해당하는 경우)
3. 해당 카테고리 배열에 BlockDefinition 추가

**예시**

```typescript
// src/cms-meta/blocks.tsx
export const blocks: BlockDefinition[] = [
  {
    meta: {
      name: "Button",
      category: "core",
      defaultProps: { label: "버튼", variant: "primary" },
      propSchema: {
        label:   { type: "string",  label: "텍스트", default: "버튼" },
        variant: { type: "select",  label: "스타일", options: ["primary", "ghost"], default: "primary" },
      },
    },
    component: ({ label, variant, ...rest }) => (
      <Button variant={variant as any} {...rest}>{label as string}</Button>
    ),
  },
];
```

---

### LayoutTemplate

페이지의 헤더/푸터 크롬을 정의한다. `renderer` 함수가 `{ header?, footer? }` 슬롯을 반환한다.

```typescript
interface LayoutTemplate {
  id: string;
  label: string;
  description?: string;
  componentName?: string;                              // JSX 생성 시 사용할 컴포넌트명
  defaultProps?: Record<string, unknown>;              // 레이아웃 props 초기값
  propSchema?: Record<string, PropField>;              // 우측 패널 레이아웃 편집 폼
  renderer?: (layoutProps: Record<string, unknown>) => LayoutSlots;
  /**
   * 코드 생성 시 layoutProps를 실제 컴포넌트 API 형태로 변환.
   * BlockDefinition.codegenProps와 동일 패턴 — { __jsx: "..." } 마커로 raw JSX 출력 가능.
   * 미정의 시 propSchema의 icon-picker 필드는 자동으로 JSX로 변환된다.
   */
  codegenProps?: (props: Record<string, unknown>) => Record<string, unknown>;
  /** codegenProps가 생성하는 JSX 안에서 참조하는 추가 컴포넌트명 (import에 자동 포함) */
  codegenImports?: string[];
}

interface LayoutSlots {
  header?: React.ReactNode;
  footer?: React.ReactNode;
}
```

#### codegenProps 사용 예시 (LayoutTemplate)

레이아웃 헤더 아이콘에 brand 색상을 입히는 등, 캔버스/미리보기 렌더와 코드 생성 결과를 일치시키고 싶을 때 사용한다.

```typescript
{
  id: "home",
  componentName: "HomePageLayout",
  propSchema: {
    logo: { type: "icon-picker", label: "로고 아이콘", default: "landmark" },
  },
  renderer: (p) => ({
    header: <HomeHeader title={...} logo={p.logo as string | undefined} />,
  }),
  // 코드 생성 시 logo를 brand 컬러 className 포함 JSX로 변환
  codegenProps: (p) => {
    const logoName = (p.logo as string | undefined) ?? "";
    const { logo: _logo, ...rest } = p;
    return logoName
      ? { ...rest, logo: { __jsx: `<${kebabToPascal(logoName)} className="size-4 text-brand" />` } }
      : rest;
  },
}
```

**자동 처리되는 부분 (codegenProps 불필요)**:
- `propSchema`의 `icon-picker` 필드 → `<IconName className="size-5" />`로 자동 변환
- 변환된 JSXExpr에서 lucide 아이콘 이름 자동 수집해 `lucide-react` import에 추가

**예시**

```typescript
// src/cms-meta/layouts.tsx
export const layouts: LayoutTemplate[] = [
  {
    id: "home",
    label: "Home Page Layout",
    componentName: "HomePageLayout",
    defaultProps: { title: "홈", withBottomNav: true, activeId: "home" },
    propSchema: {
      title:         { type: "string",  label: "타이틀", default: "홈" },
      withBottomNav: { type: "boolean", label: "하단 탭", default: true },
      activeId:      { type: "select",  label: "활성 탭", options: ["home", "asset"], default: "home" },
    },
    renderer: (p) => ({
      header: <HomeHeader title={p.title as string} />,
      footer: (p.withBottomNav as boolean)
        ? <HomeFooter activeId={p.activeId as string} />
        : undefined,
    }),
  },
];
```

---

### OverlayTemplate

바텀시트·모달 등 오버레이 컴포넌트를 정의한다. 블록을 내부에 배치할 수 있다.

```typescript
interface OverlayTemplate {
  id: string;
  label: string;
  type: string;                                        // CMSOverlay.type과 매칭
  defaultId: string;                                   // 생성 시 기본 overlay id
  blocks: CMSBlock[];                                  // 오버레이 내 초기 블록
  props?: Record<string, unknown>;
  propSchema?: Record<string, PropField>;
  componentName?: string;
  renderer?: React.ComponentType<OverlayRendererProps>;
}
```

---

### PropField

블록·레이아웃의 속성 편집 폼을 스키마로 정의한다. 타입에 따라 우측 패널에 다른 UI가 렌더링된다.

| type | UI | 설명 |
|---|---|---|
| `"string"` | 텍스트 입력 | 단순 문자열 |
| `"number"` | 숫자 입력 | 숫자 값 |
| `"boolean"` | 토글 | 참/거짓 |
| `"select"` | 드롭다운 | `options` 배열 중 선택 |
| `"icon-picker"` | 아이콘 선택기 | lucide-react 아이콘명 (kebab-case 문자열 반환) |
| `"group"` | 섹션 그룹 | 중첩 객체, `fields`로 하위 정의 |
| `"array"` | 동적 목록 | `itemFields`로 항목 구조 정의. 항목 내 `"array"` 필드로 2단계 중첩 가능 |
| `"event"` | 액션 선택기 | 클릭 이벤트에 Action 바인딩 |

```typescript
// group 예시
propSchema: {
  label: {
    type: "group",
    label: "레이블",
    default: { text: "제목", size: "md" },
    fields: {
      text: { type: "string", label: "텍스트", default: "제목" },
      size: { type: "select", label: "크기", options: ["sm", "md", "lg"], default: "md" },
    },
  },
}

// array 예시
propSchema: {
  items: {
    type: "array",
    label: "항목",
    default: [{ label: "항목 1", value: "1" }],
    itemFields: {
      label: { type: "string", label: "레이블", default: "" },
      value: { type: "string", label: "값",     default: "" },
    },
  },
}

// array 2단계 중첩 예시 — itemFields 안에 다시 array 정의 가능
propSchema: {
  menus: {
    type: "array",
    label: "메뉴",
    default: [{ title: "메뉴 1", items: [] }],
    itemFields: {
      title: { type: "string", label: "메뉴명", default: "" },
      items: {
        type: "array",
        label: "하위 항목",
        default: [],
        itemFields: {
          label: { type: "string", label: "레이블", default: "" },
          path:  { type: "string", label: "경로",   default: "" },
        },
      },
    },
  },
}
```

#### icon-picker 사용 방법

`icon-picker` 필드는 CMS 인스펙터에서 kebab-case 아이콘명(예: `"bell"`)을 반환한다.
`blocks.tsx` / `layouts.tsx`에서 이를 실제 Lucide 컴포넌트 ReactNode로 변환할 때는 `@cms-core`에서 export하는 `resolveIcon()` 유틸을 사용한다 (구현: `cms-core/utils/icon.tsx`).

```typescript
import { resolveIcon } from "@cms-core";

// resolveIcon(iconName, className?) → ReactNode | null
component: (p) => (
  <MyComponent
    icon={resolveIcon((p as any).icon, "size-5 text-text-muted")}
  />
),

// propSchema 정의
propSchema: {
  icon: { type: "icon-picker", label: "아이콘", default: "bell" },
}
```

> `resolveIcon`은 `lucide-react` 전체 아이콘에서 PascalCase → kebab-case 정방향 맵을 빌드해 조회하므로
> `default` 값은 반드시 kebab-case(`"bell"`, `"arrow-right"`)로 지정해야 한다.
> 빈 문자열이면 `null`을 반환하므로 `?? ` 대신 `||` fallback을 권장한다 (`?? `는 빈 문자열에서 작동하지 않음).

코드 생성 시에도 동일하게 동작한다. `propSchema`에 `icon-picker`가 정의되어 있으면 `codegenProps`가 없어도 자동으로 `<IconName className="size-5" />` JSX로 변환되며, 사용된 아이콘은 `lucide-react` import에 자동 포함된다. 빈 문자열은 prop 자체가 생략되어 컴포넌트의 default가 적용된다.

#### 함께 export되는 유틸리티

```typescript
import { resolveIcon, kebabToPascal, toKebabIcon, ALL_ICON_NAMES } from "@cms-core";
```

| export | 용도 |
|---|---|
| `resolveIcon(name, className?)` | kebab-case → Lucide ReactNode |
| `kebabToPascal(name)` | `"chevron-right"` → `"ChevronRight"` (codegenProps에서 raw JSX 생성 시 사용) |
| `toKebabIcon(name)` | `"ChevronRight"` → `"chevron-right"` |
| `ALL_ICON_NAMES` | 사용 가능한 PascalCase 아이콘 이름 배열 (IconPicker와 동일 source) |

---

### Action / BlockInteraction

블록 이벤트(`onClick` 등)에 연결할 수 있는 액션을 정의한다.

```typescript
type Action =
  | { type: "openOverlay";  target: string }   // 특정 오버레이 열기
  | { type: "closeOverlay"                  }  // 현재 오버레이 닫기
  | { type: "navigate";     path: string    }; // 페이지 이동

// 이벤트명 → Action 매핑
type BlockInteraction = Record<string, Action>;
```

블록 propSchema에 `type: "event"` 필드가 있으면 우측 패널에서 액션을 선택할 수 있다.

---

## 빌더 사용법

### CMSApp

빌더 UI 전체를 포함하는 루트 컴포넌트. React Router(`/builder`, `/preview`)를 내부에서 설정한다.

```typescript
interface CMSAppProps {
  blocks: BlockDefinition[];
  overlays?: OverlayTemplate[];
  layouts?: LayoutTemplate[];
  onSave?: (page: CMSPage, params: SavePageParams) => void | Promise<void>;
  basename?: string;
  stylesheetContent?: string;               // 인라인 CSS 문자열
  stylesheet?: string;                      // 외부 CSS URL
  stylesheetScope?: Record<string, string>; // data 속성으로 주입할 CSS 변수
  codegenConfig?: CMSCodegenConfig;
}
```

```typescript
// src/cms.config.ts
import { CMSApp } from "@cms-core";
import { blocks } from "./cms-meta/blocks";
import { overlays } from "./cms-meta/overlays";
import { layouts } from "./cms-meta/layouts";

export function App() {
  return (
    <CMSApp
      blocks={blocks}
      overlays={overlays}
      layouts={layouts}
      codegenConfig={{ blockImportFrom: "@cl" }}
    />
  );
}
```

### CMSBuilder

빌더 UI만 단독으로 사용할 때 쓴다. Provider는 외부에서 직접 설정해야 한다.

```typescript
interface CMSBuilderProps {
  /** 페이지 저장 핸들러. 생략 시 defaultSave(파일시스템) 사용 */
  onSave?: (page: CMSPage, params: SavePageParams) => void | Promise<void>;
  /** 초기 페이지 데이터. edit 모드에서 DB에서 불러온 CMSPage를 전달 */
  initialPage?: CMSPage;
  /**
   * 빌더 모드.
   * 'create': 새 페이지 생성 (기본값) — 초기화 버튼이 빈 상태로 리셋.
   * 'edit': 기존 페이지 수정 — 초기화 버튼이 initialPage 상태로 복원.
   */
  mode?: "create" | "edit";
  /** 편집 모드에서 저장 모달의 초기 페이지명 */
  initialPageName?: string;
  /** 현재 페이지 승인 상태 (WORK / PENDING / APPROVED / REJECTED) */
  approveState?: string;
  /** 반려 사유 — REJECTED 상태일 때 배너에 표시 */
  rejectedReason?: string | null;
}
```

---

## 런타임 사용법

### CMSRuntimeProvider

빌더 없이 CMS 페이지를 렌더링만 할 때 사용하는 경량 Provider.
외부 앱의 루트에 배치한다.

```typescript
interface CMSRuntimeProviderProps {
  blocks: BlockDefinition[];
  overlays?: OverlayTemplate[];
  layouts?: LayoutTemplate[];
  stylesheet?: string;
  stylesheetScope?: Record<string, string>;
  children: React.ReactNode;
}
```

```typescript
// 외부 앱 루트
import { CMSRuntimeProvider } from "@cms-core";
import { blocks, overlays, layouts } from "@/cms";

<CMSRuntimeProvider blocks={blocks} overlays={overlays} layouts={layouts}>
  <App />
</CMSRuntimeProvider>
```

### PageRenderer

CMSPage JSON을 받아 실제 화면을 렌더링한다. `CMSRuntimeProvider` 하위에서 사용해야 한다.

```typescript
import { PageRenderer } from "@cms-core";

// cmsPageData: API 또는 로컬 JSON에서 불러온 CMSPage 객체
<PageRenderer page={cmsPageData} />
```

---

## 코드 생성

`generateJSX`는 `CMSPage` 객체를 받아 독립 실행 가능한 React 컴포넌트 코드 문자열을 반환한다.

```typescript
import { generateJSX } from "@cms-core";

const code = generateJSX(page, layouts, codegenConfig, overlayTemplates);
```

**생성 결과 예시**

```typescript
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { HomePageLayout } from "@cl/layout/HomePageLayout";
import { Stack, Button, Typography } from "@cl";

export default function MyPage() {
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <>
      <HomePageLayout title="홈" withBottomNav activeId="home">
        <Stack gap="md" className="px-standard">
          <Typography variant="heading">안녕하세요</Typography>
          <Button onClick={() => setConfirmOpen(true)}>확인</Button>
        </Stack>
      </HomePageLayout>
      <ConfirmSheet open={confirmOpen} onClose={() => setConfirmOpen(false)} />
    </>
  );
}
```

**`CMSCodegenConfig`**

```typescript
interface CMSCodegenConfig {
  blockImportFrom?: string;    // 블록 컴포넌트 import 경로 (기본: "@neobnsrnd-team/cms-ui")
  layoutImportFrom?: string;   // 레이아웃 컴포넌트 import 경로 (미지정 시 blockImportFrom 사용)
}
```

---

## Vite 플러그인 (cmsBankPlugin)

`/__cms/create-page` POST 요청을 Vite dev 서버에서 수신하여 클라이언트(`SavePageModal`)가 입력한
`savePath` 경로에 `.tsx` 파일을 작성한다. **라우트 등록은 자동 수행하지 않는다** — 생성된
페이지를 라우팅하려면 호출 측 프로젝트의 라우터 파일에 직접 추가해야 한다.

```typescript
// react-cms/vite.config.ts
import { cmsBankPlugin } from './src/vite-plugin/cmsBankPlugin';

export default defineConfig({
  plugins: [cmsBankPlugin()],
});
```

**저장 위치 결정**

- `SavePageModal`에서 사용자가 입력한 `savePath`(Vite 프로젝트 root 기준 상대 경로)에 컴포넌트 파일이 작성된다.
- 예) `react-cms/`에서 실행 시 `savePath = "../demo/front/src/pages/cms"`이면
      `<repo>/demo/front/src/pages/cms/{PageName}.tsx`에 파일이 생성된다.
- 절대 경로(`/foo`, `C:\foo` 등)는 보안상 차단되며, 디렉토리는 필요 시 자동 생성된다.

> CMSBuilder에 `defaultSavePath` 프롭을 전달해 모달 초기값을 지정할 수 있다(기본 빈 값).
> admin 연동 모드에서는 DB 저장이므로 모달의 저장 위치 입력란이 숨겨진다(`requireSavePath={false}`).

---

## CSS 격리 (UserScopeWrapper)

CMS 캔버스에서 외부 앱 스타일과 CMS 에디터 스타일이 충돌하지 않도록 `@scope` CSS를 적용한다.

```typescript
interface StylesheetConfig {
  stylesheet?: string;                      // 외부 CSS URL
  stylesheetContent?: string;               // 인라인 CSS 문자열 (우선 적용)
  stylesheetScope?: Record<string, string>; // data 속성으로 주입할 CSS 변수
}
```

- `stylesheetContent`가 있으면 URL fetch 없이 인라인으로 주입한다.
- CSS 내 `:root`는 자동으로 `:scope`로 변환된다.
- 줄 시작의 `[data-brand="*"]` / `[data-domain="*"]` 선택자는 `:scope[data-brand="*"]` / `:scope[data-domain="*"]`으로 변환된다. `stylesheetScope`로 스코프 루트에 `data-brand` / `data-domain` 속성이 설정되므로, 브랜드·도메인 CSS 변수가 스코프 루트 자신을 올바르게 타겟하기 위해 필요하다.
- `@import url(...)` 구문은 `<style>` 최상단으로 분리하여 브라우저 파싱 오류를 방지한다.

**portal 컴포넌트 스타일 적용**

DatePicker 달력처럼 `document.body`에 portal로 렌더링되는 컴포넌트는 `@scope` 범위 밖에 렌더링되어 스타일이 적용되지 않는다.
이를 해결하려면 `main.tsx`에서 `data-cms-user-scope` 속성을 가진 portal 전용 호스트 div를 `document.body` 직하에 생성하고, 해당 요소를 `portalContainer` prop으로 전달한다.

```typescript
// main.tsx — portal 호스트 생성
const cmsPortalHost = document.createElement("div");
cmsPortalHost.id = "cms-portal-host";
cmsPortalHost.setAttribute("data-cms-user-scope", "");
cmsPortalHost.setAttribute("data-brand", import.meta.env.VITE_CMS_BRAND ?? "hana");
document.body.appendChild(cmsPortalHost);
```

---

## 타입 레퍼런스

### CMSPage

```typescript
type CMSPage = {
  layoutType?: string;
  layoutProps?: Record<string, unknown>;
  blocks: CMSBlock[];
  overlays?: CMSOverlay[];
};
```

### CMSBlock

```typescript
type CMSBlock = {
  id: string;
  component: string;               // BlockMeta.name과 매칭
  props: Record<string, unknown>;
  padding: BlockPadding;           // { top, right, bottom, left } — "none" | "xs" | "sm" | "md" | "lg" | "xl"
  interaction?: BlockInteraction;
};
```

### CMSOverlay

```typescript
type CMSOverlay = {
  id: string;
  type: string;                    // OverlayTemplate.type과 매칭
  blocks: CMSBlock[];
  props?: Record<string, unknown>;
};
```

### SavePageParams

```typescript
interface SavePageParams {
  pageName: string;   // PascalCase 컴포넌트명 (예: "MyDashboardPage")
  savePath?: string;  // 파일 저장 위치 (Vite 프로젝트 root 기준 상대 경로). DB 저장 모드에서는 미전송
  code?: string;      // 직접 생성한 코드를 전달할 때 사용 (없으면 generateJSX 호출)
}
```
