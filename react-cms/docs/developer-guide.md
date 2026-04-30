# react-cms 개발자 가이드

---

## 목차

1. [사전 요건](#1-사전-요건)
2. [환경 세팅](#2-환경-세팅)
3. [실행 모드 선택](#3-실행-모드-선택)
4. [블록 추가하기](#4-블록-추가하기)
5. [레이아웃 추가하기](#5-레이아웃-추가하기)
6. [오버레이 추가하기](#6-오버레이-추가하기)
7. [저장 핸들러 커스터마이징](#7-저장-핸들러-커스터마이징)
8. [인증 연동](#8-인증-연동)
9. [DB 테이블](#9-db-테이블)

---

## 1. 사전 요건

react-cms를 실행하기 전에 반드시 `reactive-springware`의 CSS를 먼저 빌드해야 한다.  
react-cms는 `reactive-springware`에서 빌드된 CSS를 의존하므로, 빌드 없이 실행하면 스타일이 누락되거나 비정상 동작한다.

```bash
# 1단계 — CSS 빌드 (reactive-springware)
cd reactive-springware
npm run build:css

# 2단계 — react-cms 실행
cd ../react-cms
npm run dev
```

---

## 2. 환경 세팅

### Oracle Instant Client 설치

react-cms는 Oracle DB에 직접 연결하므로 Oracle Instant Client가 필요하다.

- [Oracle Instant Client 다운로드](https://www.oracle.com/database/technologies/instant-client/downloads.html)
- ZIP을 압축 해제한 후 경로를 `.env`의 `ORACLE_CLIENT_DIR`에 지정한다 (Windows에서 PATH 인식 문제가 있을 경우).

```
ORACLE_CLIENT_DIR=C:/oracle/instantclient_21_3
```

### .env 설정

`.env.example`을 복사해 `.env`를 생성한 뒤 각 값을 채운다.

```bash
cp .env.example .env
```

| 변수 | 설명 | 필수 |
|---|---|---|
| `VITE_CMS_BRAND` | 빌더 UI 브랜드 테마 (`hana` \| `kb` \| `ibk` \| `woori` \| `shinhan` \| `nh`) | 권장 |
| `ORACLE_USER` | Oracle DB 사용자 | ✅ |
| `ORACLE_PASSWORD` | Oracle DB 비밀번호 | ✅ |
| `ORACLE_HOST` | Oracle DB 호스트 | ✅ |
| `ORACLE_PORT` | Oracle DB 포트 (기본: `1521`) | — |
| `ORACLE_SERVICE` | Oracle 서비스명 (기본: `XE`) | — |
| `ORACLE_SCHEMA` | 테이블 소유 스키마 | ✅ |
| `ORACLE_CLIENT_DIR` | Instant Client 경로 (Windows PATH 문제 시) | — |
| `AUTH_BYPASS` | 개발용 인증 우회 (`true` / `false`, 기본: `false`) | — |
| `SPIDER_ADMIN_API_URL` | Spider Admin API URL (운영 인증 시 필수) | 운영 ✅ |

---

## 3. 실행 모드 선택

react-cms는 두 가지 모드로 실행할 수 있다.

| 모드 | 명령 | 인증 | 저장 방식 |
|---|---|---|---|
| 단독 실행 | `npm run dev` | 없음 | 파일 시스템 (`demo/front`) |
| Admin 연동 | `npm run dev:proxy` | Spider Admin API | Oracle DB |

모드는 `BASE_URL` 값으로 자동 판별된다 (`src/lib/client-env.ts`).

- `BASE_URL=/` → 단독 실행
- `BASE_URL=/react-cms/` → Admin 연동

### Admin 연동 모드 nginx 설정

`npm run dev:proxy`는 Vite를 `VITE_BASE=/react-cms/`로 실행한다.  
nginx가 `/react-cms/` 경로를 Vite dev 서버로 프록시해야 한다.  
로컬 구성은 `admin/nginx/cms-local-proxy-windows.conf`를 참고한다.

### 개발 중 인증 우회

Admin 연동 모드에서 Spider Admin 없이 테스트하려면 `.env`에서 인증 우회를 활성화한다.

```
AUTH_BYPASS=true
```

브라우저에서 쿠키를 설정해 역할을 분기한다.

| 쿠키 값 | 역할 |
|---|---|
| `cms_bypass_role=react-adm` | 관리자 (`REACT-CMS:R`, `REACT-CMS:W`) |
| 미설정 | 일반 사용자 (`REACT-CMS:R`, `REACT-CMS:W`) |

> 운영 환경에서는 반드시 `AUTH_BYPASS=false`로 유지해야 한다.

---

## 4. 블록 추가하기

블록은 `src/cms-meta/blocks.tsx`에 `BlockDefinition` 객체로 등록한다.

### 기본 구조

```typescript
import type { BlockDefinition } from "@cms-core";
import { MyComponent } from "@cl";

const MyComponentDefinition: BlockDefinition = {
  meta: {
    name: "MyComponent",        // 블록 식별자 — CMSBlock.component와 매칭
    category: "core",           // "core" | "biz" | "modules"
    domain: "common",           // biz·modules 카테고리의 하위 그룹 (선택)
    defaultProps: {             // 팔레트에서 캔버스에 추가될 때 초기값
      label: "텍스트",
      variant: "primary",
    },
    propSchema: {               // 우측 속성 패널 편집 폼 스키마
      label:   { type: "string", label: "텍스트", default: "텍스트" },
      variant: { type: "select", label: "변형",   default: "primary", options: ["primary", "ghost"] },
      onClick: { type: "event",  label: "클릭" },
    },
  },
  component: (p) => <MyComponent {...(p as any)} />,
};
```

작성한 뒤 파일 하단의 해당 카테고리 배열에 추가한다.

```typescript
export const coreBlocks: BlockDefinition[] = [
  // ... 기존 블록들
  MyComponentDefinition,
];
```

### propSchema 필드 타입

| type | 렌더링 UI | 주요 옵션 |
|---|---|---|
| `"string"` | 텍스트 입력 | `default` |
| `"number"` | 숫자 입력 | `default` |
| `"boolean"` | 토글 | `default` |
| `"select"` | 드롭다운 | `options`, `default` |
| `"icon-picker"` | 아이콘 선택기 | `default` (lucide 아이콘명) |
| `"event"` | 액션 선택기 | — |
| `"group"` | 중첩 객체 | `fields`, `default` |
| `"array"` | 동적 목록 | `itemFields`, `default` |

**`group` 예시**

```typescript
propSchema: {
  label: {
    type: "group",
    label: "레이블",
    default: { text: "제목", size: "md" },
    fields: {
      text: { type: "string", label: "텍스트", default: "제목" },
      size: { type: "select", label: "크기",   options: ["sm", "md", "lg"], default: "md" },
    },
  },
}
```

**`array` 예시**

```typescript
propSchema: {
  items: {
    type: "array",
    label: "항목 목록",
    default: [{ label: "항목 1", value: "1" }],
    itemFields: {
      label: { type: "string", label: "레이블", default: "" },
      value: { type: "string", label: "값",     default: "" },
    },
  },
}
```

### ReactNode props가 있는 컴포넌트

`icon`, `children` 등 `ReactNode` 타입 prop은 CMS propSchema로 편집할 수 없다.  
빌더 미리보기용으로 고정값을 주입하는 패턴을 사용한다.

```typescript
import { Landmark } from "lucide-react";

const MyCardDefinition: BlockDefinition = {
  meta: {
    name: "MyCard",
    category: "biz",
    defaultProps: { title: "제목" },
    propSchema: {
      title: { type: "string", label: "제목", default: "제목" },
    },
  },
  // icon은 ReactNode라 propSchema 편집 불가 → 고정 아이콘 주입
  component: (p) => (
    <MyCard icon={<Landmark className="size-5" />} {...(p as any)} />
  ),
};
```

### `as any` 캐스팅에 대해

CMS의 범용 prop 시스템(`Record<string, unknown>`)과 컴포넌트의 구체적인 Props 타입을 연결하는 브릿지이므로 `as any` 캐스팅이 불가피하다.  
`blocks.tsx` 파일 상단에 `/* eslint-disable @typescript-eslint/no-explicit-any */`가 선언되어 있다.

---

## 5. 레이아웃 추가하기

레이아웃은 `src/cms-meta/layouts.tsx`에 `LayoutTemplate` 객체로 등록한다.  
`renderer` 함수가 헤더·푸터 슬롯을 반환하고, `export` 된 래퍼 컴포넌트는 `generateJSX`가 생성한 코드에서 import하여 사용한다.

### 기본 구조

```typescript
import type { LayoutTemplate } from "@cms-core";

export const layouts: LayoutTemplate[] = [
  {
    id:            "my-layout",             // 레이아웃 식별자
    label:         "My Layout",             // 팔레트 표시명
    description:   "내 레이아웃",            // 선택
    componentName: "MyLayout",              // generateJSX가 생성할 컴포넌트명
    defaultProps: {
      title: "페이지 제목",
    },
    propSchema: {
      title: { type: "string", label: "타이틀", default: "페이지 제목" },
    },
    // CMS 빌더 캔버스·미리보기에서 헤더·푸터를 렌더링
    renderer: (p) => ({
      header: <MyHeader title={p.title as string} />,
      footer: undefined,
    }),
  },
];
```

### 생성된 코드에서 사용할 래퍼 컴포넌트

`generateJSX`는 `componentName`을 기준으로 import 구문을 생성한다.  
래퍼 컴포넌트를 동일 파일에서 `export`하면 생성된 코드에서 그대로 사용할 수 있다.

```typescript
export function MyLayout({
  title = "페이지 제목",
  children,
}: {
  title?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
      <MyHeader title={title} />
      <main className="flex-1">{children}</main>
    </div>
  );
}
```

### 지원 레이아웃

현재 등록된 레이아웃은 다음 세 가지다.

| id | componentName | 설명 |
|---|---|---|
| `home` | `HomePageLayout` | 홈 헤더 + 콘텐츠 + BottomNav |
| `page` | `PageLayout` | 뒤로가기 헤더 + 콘텐츠 + 선택적 하단 버튼 |
| `blank` | `BlankLayout` | 헤더·푸터 없음 |

---

## 6. 오버레이 추가하기

오버레이는 `src/cms-meta/overlays.tsx`에 `OverlayTemplate` 객체로 등록한다.

### 기본 구조

```typescript
import type { OverlayTemplate, OverlayRendererProps } from "@cms-core";
import { MySheet } from "@cl";

function MySheetRenderer({ open, onClose, children, container, props }: OverlayRendererProps) {
  if (!open) return null;

  const title = props?.title as string | undefined;

  return (
    <MySheet
      open={open}
      onClose={onClose}
      title={title}
      container={container ?? undefined}  // portal 타깃을 CMS 미리보기 영역으로 교체
    >
      {children}
    </MySheet>
  );
}

export const overlays: OverlayTemplate[] = [
  {
    id:            "tpl_my_sheet",
    label:         "내 시트",
    description:   "설명",
    type:          "MySheet",           // CMSOverlay.type과 매칭
    defaultId:     "mySheet",           // 생성 시 기본 overlay id
    componentName: "MySheet",           // generateJSX에서 사용할 컴포넌트명
    blocks:        [],
    props: {
      title: "시트 제목",
    },
    propSchema: {
      title: { type: "string", label: "제목", default: "시트 제목" },
    },
    renderer: MySheetRenderer,
  },
];
```

### `container` prop의 역할

`BottomSheet`, `Modal` 등은 내부적으로 `createPortal(content, document.body)`를 사용한다.  
CMS 캔버스에서는 오버레이가 CMS 영역 밖(`document.body`)으로 벗어나면 미리보기가 깨진다.  
`container` prop으로 CMS 미리보기 요소를 portal 타깃으로 교체해 이 문제를 방지한다.

항상 `container={container ?? undefined}` 패턴으로 전달해야 한다.

### 고정값이 필요한 컴포넌트

컴포넌트가 필수 props로 외부 데이터(카드 목록, 셔플된 숫자 배열 등)를 요구하는 경우,  
CMS 빌더 미리보기용으로 샘플 고정값을 주입한다.

```typescript
const DEFAULT_CARD_OPTIONS = [
  { value: 'card1', label: '하나 머니 체크카드 (1234)' },
  { value: 'card2', label: '하나 1Q 신용카드 (5678)' },
];

function MyFilterSheetRenderer({ open, onClose, container }: OverlayRendererProps) {
  if (!open) return null;
  return (
    <MyFilterSheet
      open={open}
      onClose={onClose}
      cardOptions={DEFAULT_CARD_OPTIONS}  // 고정 샘플 주입
      onApply={() => { onClose(); }}
      container={container ?? undefined}
    />
  );
}
```

### 지원 오버레이

| id | type | 설명 |
|---|---|---|
| `tpl_bottomsheet` | `BottomSheet` | 내부 블록 자유 구성 바텀시트 |
| `tpl_bottomsheet_content` | `BottomSheet` | 텍스트 구성 바텀시트 |
| `tpl_modal` | `Modal` | 내부 블록 자유 구성 모달 |
| `tpl_modal_content` | `Modal` | 텍스트 구성 모달 |
| `tpl_usage_history_filter` | `UsageHistoryFilterSheet` | 카드 이용내역 검색 필터 시트 |
| `tpl_pin_confirm` | `PinConfirmSheet` | PIN 입력 시트 |
| `tpl_modal_slide_over` | `ModalSlideOver` | 슬라이드 오버 패널 |

---

## 7. 저장 핸들러 커스터마이징

`CMSBuilder`의 `onSave` prop으로 저장 로직을 교체할 수 있다.

```typescript
<CMSBuilder
  onSave={async (page, params) => {
    const { pageName, uri, code } = params;
    // 커스텀 저장 로직
    await myApi.savePage({ pageName, uri, code, page });
  }}
/>
```

`onSave`를 생략하면 `src/savePage.ts`의 기본 핸들러가 사용된다.  
기본 핸들러는 실행 모드에 따라 자동으로 분기한다.

| 모드 | 동작 |
|---|---|
| Admin 연동 | `POST /__cms/api/save` → Oracle DB 저장 |
| 단독 실행 | `POST /__cms/create-page` → `demo/front` 파일 생성 |

### DB 저장 시 pageId 캐싱

Admin 연동 모드에서 저장 성공 시 반환된 `pageId`를 localStorage에 캐싱한다.  
동일한 `pageName`으로 재저장하면 `pageId`를 읽어 INSERT 대신 UPDATE가 실행된다.

```
localStorage key 형식: cms_page_id_{pageName}
예: cms_page_id_MyDashboardPage = "uuid-1234-..."
```

---

## 8. 인증 연동

### Spider Admin 권한 구조

| 권한 | 설명 |
|---|---|
| `REACT-CMS:R` | 빌더·대시보드 읽기 접근 |
| `REACT-CMS:W` | 페이지 저장·삭제 (읽기 포함) |

Spider Admin에서 사용자에게 위 권한을 부여해야 한다.  
`REACT-CMS:W` 보유자는 읽기도 자동으로 허용된다.

### 서버 사이드 권한 검증

Vite 플러그인(API 엔드포인트) 안에서 `src/cms-admin/current-user.ts`의 헬퍼를 사용한다.

```typescript
import { requireCmsWrite, requireCmsRead, canAdminScreen } from "../cms-admin/current-user";

// 쓰기 권한 필수 — 없으면 UnauthorizedError throw
const user = await requireCmsWrite(req.headers.cookie ?? "");

// 읽기 권한 필수
const user = await requireCmsRead(req.headers.cookie ?? "");

// 관리자 화면 접근 가능 여부 (cms_admin 또는 ADMIN 역할)
if (canAdminScreen(user)) { ... }
```

### 클라이언트 사이드 인증 가드

`src/cms-admin/CmsAuthGuard.tsx`가 `/__cms/api/me`를 호출해 `REACT-CMS:R` 권한을 검증한다.  
Admin 연동 모드에서만 라우트에 삽입된다 (`main.tsx` 참고).

---

## 9. DB 테이블

### SPW_CMS_PAGE

react-cms는 기존 테이블 구조를 그대로 사용한다. 직접 테이블을 생성하거나 수정하지 않는다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `PAGE_ID` | VARCHAR2 | UUID, 기본 키 |
| `PAGE_NAME` | VARCHAR2 | 페이지명 (PascalCase) |
| `PAGE_HTML` | CLOB | CMS 빌더 직렬화 JSON (재편집용) |
| `PAGE_DESC` | CLOB | 생성된 React JSX 코드 |
| `PAGE_TYPE` | VARCHAR2 | `'REACT'` 고정 |
| `VIEW_MODE` | VARCHAR2 | `'mobile'` \| `'web'` \| `'responsive'` |
| `APPROVE_STATE` | VARCHAR2 | `'WORK'` \| `'PENDING'` \| `'APPROVED'` \| `'REJECTED'` |
| `USE_YN` | CHAR(1) | `'Y'` \| `'N'` |
| `IS_PUBLIC` | CHAR(1) | `'Y'` \| `'N'` |
| `CREATE_USER_ID` | VARCHAR2 | 생성자 ID |
| `CREATE_USER_NAME` | VARCHAR2 | 생성자명 |
| `LAST_MODIFIER_ID` | VARCHAR2 | 최종 수정자 ID |
| `LAST_MODIFIER_NAME` | VARCHAR2 | 최종 수정자명 |
| `CREATE_DATE` | DATE | 생성일시 |
| `LAST_MODIFIED_DTIME` | TIMESTAMP | 최종 수정일시 |

### DB 변경 규칙

테이블 신규·변경·삭제가 필요한 경우 **개발자가 DB에서 직접 수행**해야 한다.  
쿼리는 `admin/docs/sql/oracle/` 하위 `.sql` 파일에 기록해 이력을 관리한다.
