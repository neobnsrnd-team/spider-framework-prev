<!-- 이 파일은 scripts/extract-component-map.ts로 자동 생성됩니다. 직접 수정하지 마세요. -->
<!-- 원본: docs/component-map.md -->
# Component Map — Figma → React 매핑 전략

> **대상 Figma 파일**: Hana Bank App (`eRnV2DPVtHbGn5HSISS65O`)
> **작성 기준**: 기존 코드베이스(`packages/@shared/components`) 분석 + theme.json v2.0.0
> **패키지 네임스페이스**: `@neobnsrnd-team/reactive-springware`
> **최종 수정**: 2026-03-26

---

## 목차

1. [Mapping Strategy 개요](#1-mapping-strategy-개요)
2. [디자인 토큰 → React Props 매핑 규칙](#2-디자인-토큰--react-props-매핑-규칙)
3. [레이아웃 요소 표준 처리 지침](#3-레이아웃-요소-표준-처리-지침)
4. [컴포넌트 계층 구조 및 네이밍 컨벤션](#4-컴포넌트-계층-구조-및-네이밍-컨벤션)
6. [확정된 도메인 설계 방침](#6-확정된-도메인-설계-방침)

---

## 1. Mapping Strategy 개요

### 1.1 매핑 원칙

| 원칙 | 설명 |
|------|------|
| **1:1 컴포넌트 매핑** | Figma의 Component/Variant 1개 = React 컴포넌트 1개 |
| **토큰 우선** | 하드코딩 금지. 모든 색상·간격·타이포는 CSS 변수(`var(--*)`) 또는 Tailwind 토큰 클래스 사용 |
| **브랜드 중립** | 컴포넌트는 은행별 브랜드를 알지 못함. `data-brand` 속성으로 외부에서 주입 |
| **Variant → prop** | Figma Variant 옵션 = TypeScript Union 타입의 prop으로 1:1 변환 |
| **Auto Layout → Flex/Grid** | Figma Auto Layout = Stack / Inline / Grid 컴포넌트로 대응 |

### 1.2 매핑 결정 흐름

```
Figma 레이어 확인
      │
      ├─ Component Set (Variant 존재)?  ──→  React 컴포넌트 (variant prop)
      │
      ├─ Frame + Auto Layout만?  ──────────→  Stack / Inline / Grid 유틸 컴포넌트
      │
      ├─ 반복 패턴(List 구조)?  ──────────→  ListItem + map()
      │
      ├─ 화면 전체 프레임?  ────────────→  PageLayout / HomePageLayout / BlankPageLayout
      │
      └─ 1회성 장식 요소?  ─────────────→  인라인 JSX (컴포넌트화 하지 않음)
```

### 1.3 Figma 페이지 구조 → 컴포넌트 분류

Hana Bank App 기준으로 식별된 화면 유형:

| Figma 화면 유형 | layoutType | 사용 레이아웃 컴포넌트 |
|----------------|-----------|----------------------|
| 홈/메인 대시보드 | `home` | `HomePageLayout` |
| 계좌 목록·상세 | `page` | `PageLayout` |
| 로그인·온보딩 | `no-header` | `BlankPageLayout` |
| 이체·폼 화면 | `page` | `PageLayout` |

---

## 2. 디자인 토큰 → React Props 매핑 규칙

### 2.1 Color 토큰 매핑

#### 브랜드 컬러 (은행별 가변)

Figma에서 `Primary / Brand` 계열 색상은 CSS 변수로 처리. **컴포넌트 prop에 색상 값을 직접 전달하지 않는다.**

| Figma 색상 역할 | CSS 변수 | Tailwind 클래스 | prop 노출 여부 |
|---------------|----------|----------------|--------------|
| 브랜드 메인 | `--color-brand` | `bg-brand`, `text-brand` | ❌ (토큰 고정) |
| 브랜드 대안 | `--color-brand-alt` | `text-brand-alt` | ❌ |
| 브랜드 전경색 | `--color-brand-fg` | `text-brand-fg` | ❌ |
| 브랜드 5% 투명도 | `--color-brand-5` | `bg-brand-5` | ❌ |
| 브랜드 그림자 | `--color-brand-shadow` | `shadow-brand` | ❌ |

#### 시맨틱 컬러 (공통 고정)

| Figma 색상 역할 | CSS 변수 | Tailwind 클래스 | prop 예시 |
|---------------|----------|----------------|---------|
| 위험/에러 | `--color-danger` | `text-danger`, `bg-danger-surface` | `variant="danger"` |
| 성공 | `--color-success` | `text-success`, `bg-success-surface` | `variant="success"` |
| 경고 | `--color-warning` | `text-warning-text` | `variant="warning"` |
| 텍스트 헤딩 | `--color-text-heading` | `text-text-heading` | — |
| 텍스트 보조 | `--color-text-muted` | `text-text-muted` | — |
| 서피스 기본 | `--color-surface` | `bg-surface` | — |
| 테두리 기본 | `--color-border` | `border-border` | — |

#### 매핑 규칙 요약

```
Figma 색상 → 용도 파악 → 브랜드 가변? → CSS 변수 클래스 사용 (prop 미노출)
                        ↓ 아니오
                      시맨틱? → variant prop ('success' | 'danger' | 'warning')
                        ↓ 아니오
                      고정값 → 해당 Tailwind 유틸 클래스 직접 사용
```

### 2.2 Typography 토큰 매핑

#### 폰트 패밀리

| Figma 폰트 | 적용 대상 | Tailwind 클래스 |
|-----------|---------|----------------|
| Noto Sans KR | 일반 텍스트 | `font-sans` (기본값, 별도 지정 불필요) |
| Manrope | 금액·숫자 | `font-numeric` |
| Material Symbols | 아이콘 | `font-icon` |

#### 폰트 사이즈 → prop 변환

Text 컴포넌트의 `variant` prop 기준:

| Figma 텍스트 스타일 | px | Tailwind | `variant` prop 값 |
|-------------------|-----|----------|-----------------|
| Heading / Title | 24px | `text-2xl` | `heading` |
| Subheading | 20px | `text-xl` | `subheading` |
| Body Large | 18px | `text-lg` | `body-lg` |
| Body Base | 16px | `text-base` | `body` |
| Body Small | 14px | `text-sm` | `body-sm` |
| Caption | 12px | `text-xs` | `caption` |

#### 폰트 웨이트

| Figma 스타일 | font-weight | Tailwind |
|------------|------------|---------|
| Regular | 400 | `font-normal` |
| Medium | 500 | `font-medium` |
| Bold | 700 | `font-bold` |

### 2.3 Spacing 토큰 매핑

Figma Auto Layout의 `gap` / `padding` 값을 theme.json 스케일에 매핑:

| Figma 값 | 토큰명 | Tailwind | prop/className |
|---------|--------|---------|---------------|
| 4px | `xs` | `gap-xs`, `p-xs` | `gap="xs"` (Stack/Grid) |
| 8px | `sm` | `gap-sm`, `p-sm` | `gap="sm"` |
| 12px | `md` | `gap-md`, `p-md` | `gap="md"` |
| 16px | `standard` | `gap-standard`, `p-standard` | `gap="lg"` (Stack에서는 `lg`로 매핑 주의) |
| 20px | `lg` | `gap-lg`, `p-lg` | `gap="lg"` |
| 24px | `xl` | `gap-xl`, `p-xl` | `gap="xl"` |
| 32px | `2xl` | `gap-2xl`, `p-2xl` | `gap="2xl"` |

> **주의**: Figma에서 `16px` gap이 나오면 Tailwind `gap-standard`(=`p-standard`)이지만,
> Stack/Inline의 `gap` prop 스케일에는 `standard` 키가 없으므로 `className="gap-standard"`으로 처리.

### 2.4 Border Radius 토큰 매핑

| Figma 값 | 토큰명 | Tailwind |
|---------|--------|---------|
| 4px | `xs` | `rounded-xs` |
| 8px | `sm` | `rounded-sm` (= `rounded-lg` Tailwind 기본과 혼동 주의) |
| 12px | `md` | `rounded-md` (= `rounded-xl` Tailwind 기본) |
| 16px | `lg` | `rounded-lg` (= `rounded-2xl` Tailwind 기본) |
| 24px | `xl` | `rounded-xl` |
| 9999px | `full` | `rounded-full` |

> **혼동 방지**: 이 프로젝트의 `rounded-*`는 theme.json 커스텀 스케일로 Tailwind 기본값과 다름.
> 반드시 `theme.json common.borderRadius` 기준으로 적용.

---

## 3. 레이아웃 요소 표준 처리 지침

### 3.1 컴포넌트화 기준

```
Figma 레이아웃 요소
        │
        ├─ 3개 이상 화면에서 동일 구조 반복?  ──→  컴포넌트화 (biz/ 또는 modules/)
        │
        ├─ 순수 간격·정렬만 담당?  ─────────→  Stack / Inline / Grid 조합
        │
        ├─ 단일 화면 전용 섹션?  ────────→  페이지 내 인라인 JSX (<section>, <div>)
        │
        └─ 텍스트+아이콘 단순 조합?  ────→  인라인 JSX (컴포넌트화 하지 않음)
```

### 3.2 모호한 레이아웃 요소별 처리 지침

#### A. 섹션 헤더 (타이틀 + 우측 링크)

```
Figma: [제목 텍스트]  [전체보기 →]
```

- **처리**: `Inline` 컴포넌트로 래핑, `justify="between"` 적용
- **컴포넌트화 기준**: 3곳 이상 반복 시 `SectionHeader` 모듈 컴포넌트로 추출

```tsx
// 인라인 처리 (반복 2회 이하)
<Inline justify="between" align="center" className="mb-md">
  <Text variant="subheading">최근 거래</Text>
  <button className="text-xs text-brand-text">전체보기</button>
</Inline>

// 컴포넌트화 (반복 3회 이상) → modules/SectionHeader.tsx 생성
<SectionHeader title="최근 거래" actionLabel="전체보기" onAction={handleAction} />
```

#### B. 구분선(Divider)

```
Figma: ──────────────────
```

- **처리**: `<hr className="border-border-subtle my-sm" />` 인라인 사용
- **컴포넌트화 금지**: 단순 HR 요소는 컴포넌트로 만들지 않음

#### C. 빈 여백 섹션 (Spacer)

```
Figma: 명시적 높이를 가진 빈 Frame
```

- **처리**: Tailwind `h-*` 또는 `py-*` 클래스로 인라인 처리
- **예외**: `pb-nav`(80px 바텀 네비 여백)는 레이아웃 컴포넌트가 자동 처리

#### D. 배경 색상 섹션

```
Figma: 배경이 다른 색상인 컨테이너 Frame
```

- **처리**: `bg-surface-subtle`, `bg-brand-5` 등 시맨틱 토큰 클래스 적용
- **하드코딩 금지**: `bg-[#f5f8f8]` 같은 직접 색상값 사용 금지

#### E. 카드 내부 행(Row) 패턴

```
Figma: [레이블]  [값]
       [레이블]  [값]
```

- **처리**: `CardRow` 서브 컴포넌트 사용
- **단독 행**: `Inline justify="between"` 인라인 처리 허용

#### F. 아이콘 + 텍스트 조합

```
Figma: [아이콘] [텍스트]
```

- **처리**: `Inline gap="sm" align="center"` 인라인 래핑
- **컴포넌트화 기준**: 아이콘+텍스트 패턴이 인터랙션(클릭)을 가질 경우 `ListItem` 재사용

#### G. 그라데이션 배너

```
Figma: 브랜드 그라데이션 배경의 프로모션 배너
```

- **처리**: `BrandBanner variant="promo"` 사용
- **커스텀 배너**: 구조가 크게 다를 경우만 신규 컴포넌트 생성

#### H. 탭 네비게이션

```
Figma: [탭1] [탭2] [탭3]  (선택된 탭 하단 인디케이터)
```

- **처리**: `Tab` 컴포넌트 사용 (`variant="underline"` 또는 `"pill"`)
- **바텀 글로벌 탭**: `BrandBottomNav` 컴포넌트 사용 (커스텀 금지)

### 3.3 레이아웃 컴포넌트 선택 기준표

| 상황 | 사용 컴포넌트 |
|-----|------------|
| 세로 나열 (리스트, 폼 필드) | `Stack gap="md"` |
| 가로 나열 (버튼 행, 배지 그룹) | `Inline gap="sm"` |
| 좌우 분리 (레이블-값, 타이틀-액션) | `Inline justify="between"` |
| 2열 그리드 (퀵메뉴, 통계 카드) | `Grid cols={2} gap="sm"` |
| 전체 페이지 래퍼 | `PageLayout` / `HomePageLayout` / `BlankPageLayout` |
| 최대 너비 제한 컨테이너 | `Container` |

---

## 4. 컴포넌트 계층 구조 및 네이밍 컨벤션

### 4.1 패키지 네임스페이스 규칙

```
@neobnsrnd-team/reactive-springware
├── core/        → 원자(Atom) 컴포넌트
├── modules/     → 분자(Molecule) 컴포넌트
├── biz/         → 도메인 특화 컴포넌트
└── layout/      → 레이아웃 컴포넌트
```

### 4.2 컴포넌트 네이밍 규칙

| 규칙 | 예시 |
|-----|-----|
| PascalCase | `AccountCard`, `TransferForm` |
| 도메인 접두사 금지 (core/modules) | ~~`BankingButton`~~ → `Button` |
| 도메인 접두사 허용 (biz/) | `BrandBanner`, `SearchAccordion` |
| 서브 컴포넌트: 부모명 + 역할 | `Card`, `CardHeader`, `CardRow` |
| 훅: `use` + 동사 | `useModal`, `useBankTheme` |

### 4.3 Props 네이밍 규칙

| 패턴 | 규칙 | 예시 |
|-----|-----|-----|
| 외형 변형 | `variant` | `variant="primary"` |
| 크기 | `size` | `size="md"` |
| 상태 | boolean prop (접두사 없음) | `loading`, `disabled`, `fullWidth` |
| 이벤트 | `on` + PascalCase | `onClick`, `onClose`, `onChange` |
| 자식 슬롯 | 역할명 | `leftIcon`, `rightIcon`, `action` |
| 렌더링 태그 | `as` | `as="section"` |

### 4.4 Figma Variants → React Props 구조 도출 규칙

#### 기본 원칙

Figma Component Set의 **Variant 속성명**은 다음 규칙으로 React prop 이름과 타입으로 변환한다.

| Figma Variant 속성 패턴 | → React prop | 타입 형태 |
|------------------------|-------------|---------|
| `Type` / `Variant` / `Style` | `variant` | `'primary' \| 'outline' \| ...` |
| `Size` | `size` | `'sm' \| 'md' \| 'lg'` |
| `State` | 분리 (아래 참조) | boolean 또는 union |
| `Has Icon` / `Icon` | `leftIcon?`, `rightIcon?` | `React.ReactNode` |
| `Full Width` | `fullWidth?` | `boolean` |
| `Direction` / `Orientation` | `direction?` | `'horizontal' \| 'vertical'` |

#### `State` 속성 처리 규칙

Figma의 `State` variant는 성격에 따라 prop 형태를 다르게 결정한다.

| Figma State 값 | → React 변환 | 이유 |
|---------------|-------------|-----|
| `Default` | (기본값, prop 불필요) | 명시 불필요 |
| `Hover` / `Active` / `Focus` | CSS pseudo-class | JS prop화 금지 — CSS로 처리 |
| `Disabled` | `disabled?: boolean` | HTML 표준 속성 상속 |
| `Loading` | `loading?: boolean` | 비동기 UX 전용 prop |
| `Error` / `Success` / `Warning` | `state?: 'error' \| 'success' \| 'warning'` | 유효성 검사 상태 묶음 |
| `Empty` / `Filled` | 내부 파생 상태 (prop 노출 안 함) | value 유무로 자동 결정 |
| `Checked` / `Selected` | `checked?: boolean` / `selected?: boolean` | 제어 컴포넌트 패턴 |

#### Figma Variant 값 → prop Union 값 변환표

Figma의 영문 Variant 값은 **kebab-case 소문자**로 변환한다.

| Figma Variant 값 | → `variant` prop 값 |
|-----------------|-------------------|
| `Primary` / `Filled` | `'primary'` |
| `Secondary` / `Outlined` / `Stroke` | `'outline'` |
| `Tertiary` / `Ghost` / `Text` | `'ghost'` |
| `Destructive` / `Error` | `'danger'` |
| `Promo` / `Brand` | `'promo'` |
| `Info` / `Neutral` | `'info'` |
| `Success` | `'success'` |
| `Warning` | `'warning'` |

#### 크기 Variant 표준화

| Figma Size 값 | → `size` prop 값 |
|--------------|----------------|
| `XSmall` / `XS` | `'xs'` (필요 시) |
| `Small` / `S` | `'sm'` |
| `Medium` / `M` / `Default` | `'md'` |
| `Large` / `L` | `'lg'` |
| `XLarge` / `XL` | `'xl'` (필요 시) |

#### Storybook `title` 경로 네이밍 규칙

스토리 `title`은 `카테고리/컴포넌트명` 형식. Figma 패널 구조와 1:1 대응되도록 설계.

| 카테고리 | title 패턴 | 예시 |
|---------|-----------|-----|
| core/ | `'Core/컴포넌트명'` | `'Core/Button'` |
| modules/ | `'Modules/컴포넌트명'` | `'Modules/Modal'` |
| biz/ | `'Biz/컴포넌트명'` | `'Biz/AccountSummaryCard'` |
| layout/ | `'Layout/컴포넌트명'` | `'Layout/Stack'` |

Story 이름(export 변수명)은 Figma Variant 값 또는 사용 시나리오를 **PascalCase**로 표현:

```typescript
// ✅ 올바른 Story 이름 예시
export const Primary: Story = {};          // Figma "Primary" variant
export const Outline: Story = {};          // Figma "Outlined" variant
export const Sizes: Story = {};            // 크기 비교
export const Loading: Story = {};          // 상태 시나리오
export const WithIcons: Story = {};        // 슬롯 활용 시나리오
export const Controlled: Story = {};       // 인터랙션 시나리오 (useState 포함)

// ❌ 금지
export const story1: Story = {};           // 의미 없는 이름
export const ButtonPrimary: Story = {};    // 컴포넌트명 중복 — title에 이미 포함됨
```

### 4.5 컴포넌트 분류 기준

```
core/    → 단일 HTML 요소 수준. 자체 상태 없음.
           예: Button, Input, Badge, Avatar, Text, Spinner

modules/ → 2개 이상 core 조합. 도메인 무관.
           예: Card, ListItem, Modal, DatePicker, Pagination

biz/     → 금융 도메인 특화. 브랜드 컨텍스트 의존 가능.
           예: BrandBanner, SearchAccordion, AccountSummaryCard

layout/  → 페이지 전체 구조 담당.
           예: PageLayout, HomePageLayout, Stack, Grid
```

---

## 6. 확정된 도메인 설계 방침

> 이전 미결 항목이 모두 결정됨 (2026-03-26). 아래 방침은 개발 표준으로 확정.

---

### 6.1 계좌 카드 변형 (Account Card Variants) ✅ 확정

**방침**: `AccountSummaryCard` 단일 컴포넌트로 모든 계좌 유형 대응.

- `type` prop(`'deposit' | 'savings' | 'loan'`)으로 내부 요소 분기
- 금액은 `number`로 수신 → 컴포넌트 내부 `Intl.NumberFormat('ko-KR')` 처리
- `badgeText` prop 존재 시에만 배지 노출 (미전달 시 배지 영역 렌더링 안 함)

```typescript
export type AccountType =
  | 'deposit'
  | 'savings'
  | 'loan'
  | 'foreignDeposit'
  | 'retirement'
  | 'securities';

export interface AccountSummaryCardProps {
  type:            AccountType;
  accountName:     string;          // 예: '급여 통장', '청약저축'
  accountNumber:   string;          // 예: '123-456-789012'
  balance:         number;          // 원화 단위 숫자. 내부 포맷: Intl.NumberFormat
  balanceDisplay?: string;          // balance 대신 표시할 포맷 문자열 (외화 등)
  balanceLabel?:   string;          // 기본: '잔액' (대출은 '대출잔액' 등)
  badgeText?:      string;          // 미전달 시 배지 미노출
  moreButton?:     'chevron' | 'ellipsis'; // 우측 더보기 버튼 아이콘 형태
  onMoreClick?:    () => void;
  onClick?:        () => void;
  actions?:        React.ReactNode; // 이체·내역 버튼 슬롯
  className?:      string;
}
```

**타입별 분기 기준**:

| `type` | 금액 레이블 기본값 | 배지 용도 예시 | 특이사항 |
|--------|--------------|-------------|---------|
| `deposit` | `'잔액'` | `'주거래'`, `'급여'` | 없음 |
| `savings` | `'납입금액'` | `'D-30'` (만기일) | 진행률 바 표시 가능 |
| `loan` | `'대출잔액'` | `'변동금리'` | 금액 색상 danger 계열 |

---

### 6.2 이체 폼 구조 (Transfer Form Structure) ✅ 확정

**방침**: 단일 페이지 폼(Single Page Form). Stepper 미사용.

- 입력 필드를 세로 `Stack`으로 나열, 이전 입력 완료 시 다음 필드 순차 활성화
- 최종 확인은 **페이지 이동 없이** `Modal` 또는 `BottomSheet`로 처리
  - 모바일(390px): `BottomSheet` 사용
  - 태블릿 이상: `Modal` 사용 (`size="md"`)

```
이체 화면 구조:
  PageLayout (title="이체하기")
  └─ Stack gap="lg"
      ├─ Input (받는 계좌)
      ├─ Input (금액) — 이전 필드 완료 후 활성화
      ├─ Input (메모, optional)
      └─ Button (이체하기) → BottomSheet / Modal 열기

확인 오버레이:
  BottomSheet | Modal
  ├─ 이체 요약 (CardRow × 3)
  └─ [취소] [이체 확인] 버튼 행
```

---

### 6.3 거래 내역 그룹핑 (Transaction Grouping) ✅ 확정

**방침**: 날짜별 스티키 그룹 헤더 상시 노출.

- 날짜 형식 표준: `YYYY.MM.DD` (예: `2026.03.26`)
- API 응답은 flat 배열. 프론트엔드에서 날짜별 객체 배열로 변환 후 렌더링

**데이터 변환 구조**:

```tsx
// API 응답 (flat)
interface TransactionItem {
  id:        string;
  date:      string;   // ISO 8601: '2026-03-26T14:30:00Z'
  title:     string;
  amount:    number;
  balance:   number;
  type:      'deposit' | 'withdrawal' | 'transfer';
}

// 프론트 변환 후 (grouped)
interface TransactionGroup {
  date:   string;             // 표시용 'YYYY.MM.DD'
  items:  TransactionItem[];
}

// 렌더링 패턴
transactionGroups.map(group => (
  <section key={group.date}>
    <p className="text-xs text-text-muted sticky top-0 bg-surface py-xs px-standard">
      {group.date}
    </p>
    {group.items.map(item => <TransactionListItem key={item.id} {...item} />)}
  </section>
))
```

---

### 6.4 홈 배너 슬라이더 (Home Banner Slider) ✅ 확정

**방침**: 단일 배너 / 멀티 캐러셀 가변 대응. 데이터 수에 따라 동작 자동 결정.

| 배너 수 | 인디케이터 | 자동 재생 | 스와이프 |
|--------|---------|---------|--------|
| 1개 | 비활성화 (미노출) | 비활성화 | 비활성화 |
| 2개 이상 | 활성화 | 활성화 (3초 간격) | 활성화 |

```typescript
// BannerCarousel — 신규 제안 (biz/)
export interface BannerCarouselItem {
  id:           string;
  variant?:     BannerVariant;   // 기본: 'promo'
  title:        string;
  description?: string;
  action?:      React.ReactNode;
  onClose?:     () => void;
}

export interface BannerCarouselProps {
  items:           BannerCarouselItem[];
  /** 자동 재생 간격(ms). 기본: 3000. items.length < 2이면 무시 */
  autoPlayInterval?: number;
  className?:      string;
}
```

---

### 6.5 빈 상태 화면 (Empty State) ✅ 확정

**방침**: `EmptyState` 공통 컴포넌트 사용. 도메인별 콘텐츠는 props로 주입.

- 컴포넌트 자체는 브랜드·도메인 무관 (`modules/`)
- `illustration`, `title`, `description`, `action`을 외부에서 주입하여 도메인별 교체

```typescript
// EmptyState — modules/ (기존 컴포넌트 인터페이스 확인 후 갱신)
export interface EmptyStateProps {
  /** SVG 또는 img 요소. 도메인별 일러스트 주입 */
  illustration?: React.ReactNode;
  title:         string;
  description?:  string;
  /** CTA 버튼 등 액션 슬롯 */
  action?:       React.ReactNode;
  className?:    string;
}
```

**도메인별 사용 예시**:

```tsx
// 계좌 없음
<EmptyState
  illustration={<AccountEmptyIllust />}
  title="등록된 계좌가 없어요"
  description="계좌를 추가하면 잔액과 거래 내역을 확인할 수 있어요"
  action={<Button onClick={onAddAccount}>계좌 추가하기</Button>}
/>

// 거래 내역 없음
<EmptyState
  illustration={<TransactionEmptyIllust />}
  title="거래 내역이 없어요"
/>
```

---

### 6.6 확정된 개발 표준

이전 결정 미정 항목들의 최종 확정값:

| 항목 | 확정 방침 |
|-----|---------|
| **금액 포맷팅** | 컴포넌트 내부 `Intl.NumberFormat('ko-KR')` 처리. 외부에서 `number` 타입으로 전달 |
| **날짜 포맷** | 네이티브 `Intl.DateTimeFormat` 사용. `date-fns` 미도입 (의존성 최소화) |
| **에러 상태 관리** | 상위 훅(`useFormValidation`)에서 통합 처리 후 컴포넌트에 `validationState` prop 전달 |
| **아이콘 라이브러리** | `lucide-react` 단일 사용 유지. Material Symbols 혼용 금지 |

---

### 6.8 계좌 선택 카드 (Account Selector Card) ✅ 확정

**방침**: 잔액을 표시하지 않고 계좌를 선택·변경하는 전용 카드. `AccountSummaryCard`와 역할이 다르므로 별도 컴포넌트로 분리.

**AccountSummaryCard vs AccountSelectorCard 사용 기준**:

| 구분 | `AccountSummaryCard` | `AccountSelectorCard` |
|-----|---------------------|----------------------|
| 주요 사용처 | 홈, 계좌 목록 | 거래내역 조회, 이체 등 상세 화면 |
| 잔액 표시 | ✅ 필수 (`balance: number`) | ❌ 없음 |
| 계좌 변경 | ❌ | ✅ `onAccountChange` 드롭다운 |
| 우측 아이콘 버튼 | ❌ | ✅ `onIconClick` |
| 모서리 반경 | `rounded-xl` (12px) | `rounded-3xl` (24px, Figma 기준) |

```typescript
// AccountSelectorCard — biz/
export interface AccountSelectorCardProps {
  accountName:       string;            // 예: '하나 주거래 통장'
  accountNumber:     string;            // 예: '123-456-789012'. 마스킹 없이 표시
  icon?:             React.ReactNode;   // 우측 원형 버튼 아이콘. 기본: Landmark
  /** 계좌명 클릭 시 콜백. 미전달 시 드롭다운 화살표 숨김 */
  onAccountChange?:  () => void;
  /** 우측 원형 아이콘 버튼 클릭 시 콜백 */
  onIconClick?:      () => void;
  iconAriaLabel?:    string;            // 우측 아이콘 버튼 접근성 레이블
  availableBalance?: string;            // 가용 잔액 표시 문자열 (이체 화면 등)
  className?:        string;
}
```

**사용 예시**:

```tsx
<AccountSelectorCard
  accountName="하나 주거래 통장"
  accountNumber="123-456-789012"
  onAccountChange={handleOpenAccountPicker}  // BottomSheet 등으로 계좌 변경
  onIconClick={handleGoToAccountDetail}
/>
```

---

### 6.9 거래내역 검색 필터 (Transaction Search Filter) ✅ 확정

**방침**: 접기/펼치기 아코디언 구조. 폼 내부 상태를 독립 관리하며 조회 버튼 클릭 시에만 `onSearch` 호출.

- 퀵 기간 탭(1·3·6·12개월) 선택 시 오늘 기준으로 `startDate`·`endDate` 자동 계산
- 날짜 직접 선택 시 `DatePicker` 컴포넌트 사용. 퀵 탭 선택 해제
- 정렬 순서(`최근순` / `과거순`), 거래 유형(`전체` / `입금` / `출금`) 드롭다운 제공
- 날짜 상태는 ISO string(`YYYY-MM-DD`)으로 관리. `DatePicker` 전달 시 `isoToDate()`로 변환
  - `T00:00:00` 로컬 시간으로 파싱해 시간대 오프셋으로 날짜가 밀리는 것 방지

```typescript
// TransactionSearchFilter — modules/
export type QuickPeriod    = '1m' | '3m' | '6m' | '12m';
export type SortOrder      = 'recent' | 'old';
export type TransactionType = 'all' | 'deposit' | 'withdrawal';

export interface TransactionSearchParams {
  startDate:       string;           // ISO 'YYYY-MM-DD'
  endDate:         string;           // ISO 'YYYY-MM-DD'
  sortOrder:       SortOrder;
  transactionType: TransactionType;
}

export interface TransactionSearchFilterProps {
  /** 현재 적용된 검색 조건. 초기값 및 외부 동기화에 사용 */
  value:            TransactionSearchParams;
  /** 조회 버튼 클릭 시 호출. 폼 내부 상태 기준으로 전달 */
  onSearch:         (params: TransactionSearchParams) => void;
  defaultExpanded?: boolean;         // 기본: false (접힌 상태)
  className?:       string;
}
```

**사용 예시**:

```tsx
const [searchParams, setSearchParams] = useState<TransactionSearchParams>({
  startDate: '2023-10-01',
  endDate:   '2023-11-01',
  sortOrder: 'recent',
  transactionType: 'all',
});

<TransactionSearchFilter
  value={searchParams}
  onSearch={setSearchParams}  // 조회 버튼 클릭 시에만 상태 갱신
/>
```

---

---

### 6.10 앱 브랜드 헤더 (App Brand Header) ✅ 확정

**방침**: 로그인·온보딩 화면(`BlankPageLayout`) 최상단에 배치하는 브랜드 로고 전용 헤더.
`PageLayout`의 헤더(뒤로가기 + 타이틀 구조)와 역할이 다르므로 별도 컴포넌트로 분리.

**PageLayout 헤더 vs AppBrandHeader 사용 기준**:

| 구분 | `PageLayout` 헤더 | `AppBrandHeader` |
|-----|-----------------|-----------------|
| 사용처 | 계좌 목록·상세, 이체 폼 등 일반 페이지 | 로그인·온보딩 화면 |
| 레이아웃 | 뒤로가기(좌) + 타이틀(중앙) + 액션(우) | 브랜드 이니셜 배지 + 브랜드명 중앙 정렬 |
| 네비게이션 | ✅ 뒤로가기 버튼 포함 | ❌ 네비게이션 없음 |

```typescript
// AppBrandHeader — layout/
export interface AppBrandHeaderProps {
  /** 브랜드 이니셜 — 원형 배지 내부 표시 (예: 'H') */
  brandInitial: string;
  /** 브랜드명 (예: '하나은행') */
  brandName:    string;
  className?:   string;
}
```

**사용 예시**:

```tsx
<BlankPageLayout>
  <AppBrandHeader brandInitial="H" brandName="하나은행" />
  {/* 로그인 폼 콘텐츠 */}
</BlankPageLayout>
```

---

### 6.11 텍스트 레이블 구분선 (Divider With Label) ✅ 확정

**방침**: 좌우 수평선 + 중앙 텍스트로 구성된 섹션 구분선.
로그인 화면 "다른 로그인 방식"처럼 두 영역의 시각적 경계를 표시할 때 사용.

```typescript
// DividerWithLabel — modules/
export interface DividerWithLabelProps {
  /** 구분선 중앙에 표시할 텍스트 (예: '다른 로그인 방식', '또는') */
  label:      string;
  className?: string;
}
```

**사용 예시**:

```tsx
<DividerWithLabel label="다른 로그인 방식" />
```

---

### 6.12 로그인 페이지 구성 방침 ✅ 확정

**Figma 원본**: Hana Bank App — node-id: 1-911

**컴포넌트 조합**:

```
BlankPageLayout
├─ AppBrandHeader          brandInitial="H" brandName="하나은행"
├─ Stack (flex-1, px-standard)
│   ├─ Stack gap="xs"      타이틀 섹션
│   │   ├─ Text as="h1" variant="heading" className="text-3xl"   → "로그인"
│   │   └─ Text variant="body" color="muted"                     → 부제목
│   ├─ Stack gap="lg"      입력 폼
│   │   ├─ Input label="아이디" type="text" fullWidth
│   │   └─ Input label="비밀번호" type="password" validationState="error"
│   │             helperText="..." rightElement={<EyeOff />} fullWidth
│   ├─ Inline justify="center"  텍스트 링크 행
│   │   ├─ Button variant="ghost" size="sm"   → 아이디 찾기
│   │   ├─ (수직 구분선 div)
│   │   ├─ Button variant="ghost" size="sm"   → 비밀번호 변경
│   │   ├─ (수직 구분선 div)
│   │   └─ Button variant="ghost" size="sm"   → 회원가입
│   └─ Button variant="primary" size="lg" fullWidth  → 로그인
└─ Stack (px-standard, pb-2xl)  대체 로그인
    ├─ DividerWithLabel label="다른 로그인 방식"
    └─ QuickMenuGrid cols={3}   간편 비밀번호 / 생체인증 / QR 로그인
```

**타이틀 크기 처리**:
- Figma 원본: 30px. `Text` `heading` variant는 `text-2xl`(24px)
- `className="text-3xl"` override로 30px 재현 (`text-3xl` = Tailwind 표준 30px)

---

---

### 6.13 홈 대시보드 페이지 구성 방침 ✅ 확정

**Figma 원본**:
- 해당금융 탭: Hana Bank App — node-id: 1-202
- 다른금융 탭: Hana Bank App — node-id: 1-336

**신규 컴포넌트 없이** 기존 컴포넌트 100% 조합으로 구현 가능.

**탭별 콘텐츠 분기**:

| 탭 | 주요 컴포넌트 | 특이사항 |
|----|-------------|---------|
| 해당금융 | `AccountSummaryCard` + 소형 연결 유도 `Card` | `Card className="bg-brand-5 border-brand-10 rounded-3xl"` |
| 다른금융 | `Card` + `EmptyState` + `Button` | `Card className="border-dashed bg-surface-raised"` |
| 자산관리 | 미구현 — `EmptyState` 플레이스홀더 | 추후 Figma 제공 시 구현 |

**공통 영역** (모든 탭):
- `QuickMenuGrid cols={3}` — 전계좌조회·이체·내역조회
- `BannerCarousel variant="promo"` — 프로모션 배너
- `SectionHeader` + `NoticeItem × 3` — 공지 및 혜택

**컴포넌트 조합**:

```
HomePageLayout
  title="Hana Bank"  logo={<Building2 />}  hasNotification  withBottomNav
├─ Text as="h2" variant="heading"    → "안녕하세요, 김하나님!"  {/* greeting prop 제거 → 본문 내 직접 표시 */}
├─ TabNav                            → 해당금융 / 다른금융 / 자산관리
├─ Stack gap="md" px-standard
│   ├─ [mine]  AccountSummaryCard    → 계좌 카드
│   │          Card (brand-5, dashed border, rounded-3xl)  → 연결 유도 소형 배너
│   ├─ [other] Card (border-dashed)
│   │          └─ EmptyState + Button  → 계좌 미연결 빈상태
│   ├─ [asset] EmptyState            → 미구현 플레이스홀더
│   ├─ QuickMenuGrid cols={3}        → 퀵 액션 그리드
│   ├─ BannerCarousel                → 프로모션 배너
│   └─ SectionHeader + NoticeItem×3  → 공지 및 혜택
└─ BottomNav activeId="home"         → 하단 고정 탭바
```

**헤더 기본 rightAction** (User·Bell·Menu 3버튼 내장):
- `hasNotification={true}` 전달 시 벨 아이콘 우측 상단에 `bg-danger-badge` 빨간 뱃지 자동 표시
- 커스텀 우측 영역이 필요한 경우 `rightAction` prop으로 직접 전달

```tsx
{/* 기본 사용 — 알림 뱃지 있음 */}
<HomePageLayout title="Hana Bank" logo={<HanaLogo />} hasNotification withBottomNav>
  ...
</HomePageLayout>

{/* rightAction 커스텀 override */}
<HomePageLayout title="Hana Bank" rightAction={<MyCustomActions />} withBottomNav>
  ...
</HomePageLayout>
```

---

---

### 6.14 2026-04 신규 확정 컴포넌트 TypeScript Interface

> 2026-03-26 이후 추가된 컴포넌트 인터페이스 정의.

#### Biz / Card 도메인

```typescript
// ── CardSummaryCard ───────────────────────────────────────────
export type CardType = 'credit' | 'check' | 'prepaid';

export interface CardSummaryCardProps {
  type:         CardType;
  cardName:     string;          // 예: '하나 머니 체크카드'
  cardNumber:   string;          // 마스킹된 카드번호. 예: '1234 **** **** 5678'
  amount:       number;          // credit: 당월 사용금액, check/prepaid: 잔액
  limitAmount?: number;          // credit 전용. 표시 시 "사용금액/한도" 형태
  badgeText?:   string;          // 미전달 시 배지 미노출
  onClick?:     () => void;
  actions?:     React.ReactNode; // 결제내역·충전 버튼 슬롯
  className?:   string;
}

// ── SummaryCard ───────────────────────────────────────────────
export type SummaryCardVariant = 'asset' | 'spending';

export interface SummaryCardAction {
  label:    string;
  onClick:  () => void;
  active?:  boolean;
}

export interface SummaryCardProps {
  variant:    SummaryCardVariant; // 'asset': 자산합계, 'spending': 지출합계
  title:      string;
  amount:     number;
  icon?:      React.ReactNode;   // 우측 원형 bg-brand-10 배경 아이콘 슬롯
  actions?:   SummaryCardAction[];
  onClick?:   () => void;
  hidden?:    boolean;           // true: 금액 마스킹 처리
  className?: string;
}

// ── StatementHeroCard ─────────────────────────────────────────
export interface StatementHeroCardProps {
  amount:     number;   // 청구·결제 금액
  dueDate:    string;   // 결제 예정일. 예: '2026.04.14'
  label?:     string;   // 기본: '이번달 결제금액'
  onDetail?:  () => void;
  hidden?:    boolean;  // true: 금액 마스킹 처리
  className?: string;
}

// ── LoanMenuBar ───────────────────────────────────────────────
// 드래그 패닝·터치 스와이프로 가로 스크롤. 스크롤바 숨김 처리 포함.
export interface LoanMenuBarItem {
  id:      string;
  icon:    React.ReactNode;
  label:   string;
  onClick: () => void;
}

export interface LoanMenuBarProps {
  items:      LoanMenuBarItem[];
  className?: string;
}

// ── QuickShortcutCard ─────────────────────────────────────────
export interface QuickShortcutCardProps {
  title:      string;
  subtitle:   string;
  icon?:      React.ReactNode;
  onClick?:   () => void;
  className?: string;
}

// ── CardPaymentSummary ────────────────────────────────────────
export interface CardPaymentSummaryProps {
  dateFull:       string;   // 출금예정일. 예: '2026.04.08'
  dateYM:         string;   // 청구 년월. 예: '26년 4월'
  dateMD:         string;   // 기준일(오늘). 예: '04.08'
  totalAmount:    number;
  revolving?:     number;   // 기본: 0
  cardLoan?:      number;   // 기본: 0
  cashAdvance?:   number;   // 기본: 0
  paymentAccount: string;   // 예: '하나은행 123-456789-01234'
  paymentDate:    string;   // 예: '매월 14일'
  className?:     string;
}

// ── CardPaymentItem ───────────────────────────────────────────
export interface CardPaymentItemProps {
  icon:             React.ReactNode;
  iconBgClassName?: string;       // 기본: 'bg-brand-10'
  cardEnName:       string;       // 카드 영문명. 예: 'HANA MONEY CHECK'
  cardName:         string;       // 카드 한글명 또는 가맹점명
  amount:           number;       // 음수: 취소/환불 (brand 색상으로 표시)
  onDetailClick?:   () => void;   // 미전달 시 "상세보기" 버튼 미노출
  onClick?:         () => void;   // 전달 시 행 전체가 버튼
  className?:       string;
}

// ── BillingPeriodLabel ────────────────────────────────────────
export interface BillingPeriodLabelProps {
  startDate:  string;   // 예: '2025.03.01'
  endDate:    string;   // 예: '2025.03.31'
  className?: string;
}
```

#### Biz / Insurance 도메인

```typescript
// ── InsuranceSummaryCard ──────────────────────────────────────
export type InsuranceType   = 'life' | 'health' | 'car';
export type InsuranceStatus = 'active' | 'pending' | 'expired';

export interface InsuranceSummaryCardProps {
  type:             InsuranceType;
  insuranceName:    string;
  contractNumber:   string;
  premium:          number;          // 월 보험료 (원)
  nextPaymentDate?: string;          // 다음 납부일. 예: '2026.04.14'
  status:           InsuranceStatus;
  badgeText?:       string;
  onClick?:         () => void;
  actions?:         React.ReactNode;
  className?:       string;
}
```

#### Biz / Common 도메인

```typescript
// ── UserProfile ───────────────────────────────────────────────
export interface UserProfileProps {
  name:                   string;    // 예: '김하나'
  lastLogin?:             string;    // 마지막 로그인 일시 문자열
  onProfileManageClick?:  () => void; // 내 정보 관리 클릭 — 전달 시 설정 드롭다운에 표시
  onLogoutClick?:         () => void; // 로그아웃 클릭 — 전달 시 설정 드롭다운에 표시
  className?:             string;
}
```

#### Layout 신규

```typescript
// ── BottomNav ─────────────────────────────────────────────────
export interface BottomNavItem {
  id:          string;
  icon:        React.ReactNode;
  activeIcon?: React.ReactNode;  // 미전달 시 icon 재사용
  label:       string;
  onClick:     () => void;
}

export interface BottomNavProps {
  items:      BottomNavItem[];
  activeId:   string;
  className?: string;
}

// ── Section ───────────────────────────────────────────────────
export interface SectionProps {
  title?:       string;
  badge?:       number;          // 섹션 헤더 우측 알림 배지
  actionLabel?: string;          // 우측 텍스트 링크 레이블
  onAction?:    () => void;
  children:     React.ReactNode;
  gap?:         'xs' | 'sm' | 'md' | 'lg' | 'xl'; // 기본: 'md'
  className?:   string;
}
```

#### Modules / Banking 신규

```typescript
// ── AccountSelectItem ─────────────────────────────────────────
export interface AccountSelectItemProps {
  icon?:         React.ReactNode;
  accountName:   string;
  accountNumber: string;
  balance:       string;         // 포맷된 잔액 문자열
  selected?:     boolean;
  onClick?:      () => void;
  className?:    string;
}

// ── NumberKeypad ──────────────────────────────────────────────
// 보안 숫자 키패드. 숫자 배열을 외부에서 주입하여 순서를 제어할 수 있음.
export interface NumberKeypadProps {
  digits:         number[];      // 표시할 숫자 배열 (섞기 구현 시 외부에서 전달)
  onDigitPress:   (digit: number) => void;
  onDelete:       () => void;
  onShuffle:      () => void;    // 숫자 순서 섞기 버튼
  className?:     string;
}

// ── PinDotIndicator ───────────────────────────────────────────
export interface PinDotIndicatorProps {
  length?:     number;   // 총 자릿수. 기본: 6
  filledCount: number;   // 입력된 자릿수
  className?:  string;
}

// ── TransactionList ───────────────────────────────────────────
// 날짜 헤더를 자동 생성하며 그룹핑. §6.3 그룹핑 패턴 구현체.
export type DateHeaderFormat = 'month-day' | 'year-month-day';

export interface TransactionItem {
  id:       string;
  type:     'deposit' | 'withdrawal' | 'transfer';
  title:    string;
  date:     string;    // ISO 8601
  amount:   number;
  balance?: number;
}

export interface TransactionListProps {
  items:             TransactionItem[];
  loading?:          boolean;
  emptyMessage?:     string;
  onItemClick?:      (item: TransactionItem) => void;
  dateHeaderFormat?: DateHeaderFormat; // 기본: 'month-day'
  className?:        string;
}

// ── TransferForm ──────────────────────────────────────────────
// §6.2 단일 폼 방침 구현체. 확인은 BottomSheet/Modal 없이 onSubmit 위임.
export interface TransferFormData {
  toAccount: string;
  amount:    number;
  memo:      string;
}

export interface TransferFormProps {
  availableBalance: number;
  onSubmit:         (data: TransferFormData) => void;
  submitting?:      boolean;
  className?:       string;
}
```

#### Modules / Common 신규

```typescript
// ── ActionLinkItem ────────────────────────────────────────────
export interface ActionLinkItemProps {
  icon:             React.ReactNode;
  iconBgClassName?: string;
  label:            string;
  size?:            'md' | 'sm';    // 기본: 'md'
  showBorder?:      boolean;
  onClick?:         () => void;
  className?:       string;
}

// ── AlertBanner ───────────────────────────────────────────────
export type AlertBannerIntent = 'warning' | 'danger' | 'success' | 'info';

export interface AlertBannerProps {
  intent?:    AlertBannerIntent;  // 기본: 'info'
  children:   React.ReactNode;
  icon?:      React.ReactNode;    // 미전달 시 intent별 기본 아이콘 사용
  className?: string;
}

// ── BalanceToggle ─────────────────────────────────────────────
// 잔액 숨김/표시 토글 버튼. hidden 상태 관리는 외부 Hook 담당.
export interface BalanceToggleProps {
  hidden:    boolean;
  onToggle:  () => void;
  className?: string;
}

// ── Checkbox ──────────────────────────────────────────────────
export interface CheckboxProps {
  checked:    boolean;
  onChange:   (checked: boolean) => void;
  label?:     React.ReactNode;
  ariaLabel?: string;
  shape?:     'square' | 'circle'; // 기본: 'square'. 'circle': 원형 체크박스
  disabled?:  boolean;
  id?:        string;
  className?: string;
}

// ── DropdownMenu ──────────────────────────────────────────────
export interface DropdownMenuItem {
  label:    string;
  icon?:    React.ReactNode;
  onClick:  () => void;
  variant?: 'default' | 'danger'; // 'danger': 빨간 텍스트 (로그아웃·삭제 등)
}

export interface DropdownMenuProps {
  children:   React.ReactNode; // 드롭다운을 여는 트리거 요소
  items:      DropdownMenuItem[];
  align?:     'left' | 'right'; // 패널 정렬. 기본: 'right'
  className?: string;
}

// ── CollapsibleSection ────────────────────────────────────────
export interface CollapsibleSectionProps {
  header:           React.ReactNode;
  children:         React.ReactNode;
  defaultExpanded?: boolean;        // 기본: false
  headerAlign?:     'center' | 'left'; // 기본: 'center'
  className?:       string;
}

// ── ErrorState ────────────────────────────────────────────────
export interface ErrorStateProps {
  title?:       string;             // 기본: '오류가 발생했어요'
  description?: string;
  onRetry?:     () => void;         // 미전달 시 재시도 버튼 미노출
  retryLabel?:  string;             // 기본: '다시 시도'
  className?:   string;
}

// ── InfoRow ───────────────────────────────────────────────────
export interface InfoRowProps {
  label:           string;
  value:           string | React.ReactNode;
  valueClassName?: string;
  showBorder?:     boolean;         // 하단 구분선
  className?:      string;
}

// ── NoticeItem ────────────────────────────────────────────────
export interface NoticeItemProps {
  icon:             React.ReactNode;
  iconBgClassName?: string;
  title:            string;
  description?:     string;
  onClick?:         () => void;
  showDivider?:     boolean;
  className?:       string;
}

// ── SelectableItem ────────────────────────────────────────────
export interface SelectableItemProps {
  icon:       React.ReactNode;
  label:      string;
  selected?:  boolean;
  onClick?:   () => void;
  className?: string;
}

// ── SidebarNav ────────────────────────────────────────────────
export interface SidebarNavItem {
  id:    string;
  label: string;
}

export interface SidebarNavProps {
  items:        SidebarNavItem[];
  activeId:     string;
  onItemChange: (id: string) => void;
  className?:   string;
}

// ── SuccessHero ───────────────────────────────────────────────
export interface SuccessHeroProps {
  recipientName: string;   // 수취인명
  amount:        string;   // 포맷된 금액 문자열
  subtitle?:     string;   // 추가 안내 문구
  className?:    string;
}
```

---

*모든 미결 항목 해소 완료 — 2026-03-27*
*2026-04-08: §6.7 구현 현황 전면 갱신, §6.14 신규 컴포넌트 인터페이스 추가*

*본 문서는 Figma 디자인 열람 및 개발 진행에 따라 지속적으로 업데이트됩니다.*
