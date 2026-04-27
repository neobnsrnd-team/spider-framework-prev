# React 페이지 컴포넌트 생성 규칙

## 목적

Figma 디자인을 기반으로 **React 페이지 컴포넌트 파일 1개(`.tsx`)** 를 생성한다.

생성 결과물의 목표:

- 순수 UI만 담당한다. API 호출·비즈니스 로직을 포함하지 않는다.
- 생성 직후 미리보기가 가능해야 한다.
- 개발자가 컴포넌트 내부를 수정하지 않고 props만 넘겨 실제 데이터를 연동할 수 있어야 한다.

---

## 1. 생성 파일 구조 — Props + Mock Props 패턴

파일은 반드시 아래 4개 블록 순서로 구성한다.

```tsx
// ① Props 인터페이스 — named export
export interface PaymentPageProps {
  billingAmount: number;
  accountBalance: number;
  cardName: string;
}

// ② 페이지 컴포넌트 — named export, 순수 UI
export function PaymentPage({ billingAmount, accountBalance, cardName }: PaymentPageProps) {
  return (
    <div data-brand="hana" data-domain="banking">
      {/* UI */}
    </div>
  );
}

// ③ Mock Props — 인터페이스 타입으로 선언하여 일치 보장
const mockProps: PaymentPageProps = {
  billingAmount: 285000,
  accountBalance: 3000000,
  cardName: '하나 머니 체크카드',
};

// ④ Preview — default export, 미리보기 진입점
export default function Preview() {
  return <PaymentPage {...mockProps} />;
}
```

### Mock Props 작성 기준

- 타입은 반드시 Props 인터페이스로 명시한다. (`const mockProps: PaymentPageProps = {}`)
- 금액은 실제처럼 보이는 숫자를 사용한다. (`285000`, `3000000`)
- 문자열은 실제 서비스에서 사용할 법한 값을 사용한다. (`'하나 머니 체크카드'`, `'123-456-789012'`)
- 배열은 2~3개 항목으로 구성하여 목록 UI가 렌더링되도록 한다.

---

## 2. Import 규칙

```tsx
// UI 컴포넌트 — @cl에서만 import
import { Button, Stack, Inline, Typography } from '@cl';

// 아이콘 — lucide-react에서만 import
import { Search, ChevronRight, X } from 'lucide-react';

// React hooks — UI 상태가 필요한 경우만
import { useState } from 'react';
```

import 순서: `@cl` → `lucide-react` → `react`

- `@cl` 이외의 UI 라이브러리 추가 금지
- HTML 태그 직접 사용 금지 (`div`, `button`, `p`, `h1` 등)
- 단, `data-brand` 래퍼 역할의 최상위 `div` 1개는 예외 허용
- **JSX에서 실제로 렌더링되는 컴포넌트만 import할 것. 사용하지 않는 import 금지.**
  - 먼저 JSX를 완성한 뒤, 사용된 컴포넌트만 import 구문에 포함할 것
  - component-types.md에 나열된 컴포넌트라도 해당 파일에서 렌더링하지 않으면 import 금지

---

## 3. 브랜드 규칙

모든 페이지 컴포넌트 루트에 `data-brand`와 `data-domain`을 반드시 명시한다.

```tsx
<div data-brand="hana" data-domain="banking">
  <PageLayout title="...">...</PageLayout>
</div>
```

### 브랜드 매핑표

| 브랜드      | `data-brand` | 대표 색상      |
| ----------- | ------------ | -------------- |
| 하나은행    | `hana`       | 청록 `#008485` |
| IBK기업은행 | `ibk`        | 파랑 `#0068b7` |
| KB국민은행  | `kb`         | 골드 `#ffbc00` |
| NH농협은행  | `nh`         | 초록 `#00a859` |
| 신한은행    | `shinhan`    | 블루 `#0046ff` |
| 우리은행    | `woori`      | 블루 `#0067ac` |
| 카드        | `card`       | 파랑 `#1a56db` |
| 보험        | `insurance`  | 빨강 `#e03a1e` |
| 지로        | `giro`       | 보라 `#5b21b6` |

### `data-domain` 값

| `data-brand`                                       | `data-domain` |
| -------------------------------------------------- | ------------- |
| `hana` / `ibk` / `kb` / `nh` / `shinhan` / `woori` | `banking`     |
| `card`                                             | `card`        |
| `insurance`                                        | `insurance`   |
| `giro`                                             | `giro`        |

---

## 4. 레이아웃 규칙

### Figma Auto Layout → React 변환표

| Figma                    | React                            | 주의사항                             |
| ------------------------ | -------------------------------- | ------------------------------------ |
| Auto Layout (Vertical)   | `<Stack gap="..." />`            | `direction` prop 없음                |
| Auto Layout (Horizontal) | `<Inline gap="..." />`           | `Row` 사용 금지                      |
| Grid (N열)               | `<Grid cols={N} />`              | `columns` 아님, `cols`               |
| 테두리 있는 컨테이너     | `<Card />`                       |                                      |
| 섹션 (제목 + 콘텐츠)     | `<Section title="..." />`        | title 있으면 SectionHeader 자동 포함 |
| 섹션 헤더 행만           | `<SectionHeader title="..." />`  |                                      |
| 일반 페이지              | `<PageLayout title="..." />`     |                                      |
| 홈 화면                  | `<HomePageLayout title="..." />` |                                      |
| 헤더 없는 화면           | `<BlankPageLayout />`            | 로그인·온보딩 전용                   |

❌ `<Stack direction="horizontal" />` — 수평은 `Inline` 사용  
❌ `<Row />` `<Column />` — 존재하지 않음  
❌ `<Grid columns={4} />` — prop명은 `cols`

### Spacing 토큰 변환표

| Figma px | `gap` prop  | `className`   | 주요 용도      |
| -------- | ----------- | ------------- | -------------- |
| 4px      | `gap="xs"`  | `gap-xs`      | 밀집 요소      |
| 8px      | `gap="sm"`  | `gap-sm`      | 소형 간격      |
| 12px     | `gap="md"`  | `gap-md`      | 기본 간격      |
| 20px     | `gap="lg"`  | `gap-lg`      | 섹션 내 여백   |
| 24px     | `gap="xl"`  | `gap-xl`      | 섹션 간 여백   |
| 32px     | `gap="2xl"` | `gap-2xl`     | 대형 블록      |
| 16px     | —           | `px-standard` | 좌우 패딩 전용 |

> `gap` prop 허용값: `xs | sm | md | lg | xl | 2xl`  
> `standard`는 gap prop으로 사용 불가 — `className="gap-standard"` 으로만 사용

### 페이지 타입별 구조

**일반 페이지**

```tsx
<div data-brand="hana" data-domain="banking">
  <PageLayout title="이체하기" onBack={onBack}>
    <Stack gap="md">{/* 콘텐츠 */}</Stack>
  </PageLayout>
</div>
```

**홈 화면**

```tsx
<div data-brand="hana" data-domain="banking">
  {/* BottomNav는 HomePageLayout이 withBottomNav prop으로 내부에서 렌더링 — 별도 <BottomNav> 추가 금지 */}
  <HomePageLayout
    title="하나은행"
    logo={<Building2 />}
    withBottomNav
    activeId="home"
    bottomNavItems={bottomNavItems}
  >
    <Stack gap="md">{/* 콘텐츠 */}</Stack>
  </HomePageLayout>
</div>
```

**로그인·온보딩**

```tsx
<div data-brand="hana" data-domain="banking">
  <BlankPageLayout>
    <AppBrandHeader brandInitial="H" brandName="하나은행" />
    {/* 콘텐츠 */}
  </BlankPageLayout>
</div>
```

### 레이아웃 패딩 주의사항

`PageLayout`과 `HomePageLayout`의 main 영역에 `px-standard py-md` 패딩이 내장되어 있다.  
내부 Stack에 별도 패딩 추가 금지.

```tsx
// ✅
<HomePageLayout>
  <Stack gap="lg">...</Stack>
</HomePageLayout>

// ❌ 패딩 중복
<HomePageLayout>
  <Stack gap="lg" className="px-standard">...</Stack>
</HomePageLayout>
```

### 컴포넌트 계층

Page → Layout → Section → Component 순서를 반드시 지킨다.  
Component 내부에 Page를 중첩하는 구조 금지.

---

## 5. 스타일 규칙

**허용**

```tsx
className = 'px-standard pb-standard'; // 디자인 토큰 기반
className = 'bg-brand-5 rounded-xl'; // 토큰 기반 색상·반경
className = 'text-text-muted'; // 시맨틱 색상 토큰
className = 'transition-colors duration-150'; // 토큰 기반 애니메이션
```

**금지**

```tsx
style={{ color: '#333', padding: 16 }}     // inline style 전면 금지
className="text-[#333] mt-[20px]"          // 임의 값 Tailwind 금지
className="bg-[#f5f8f8]"                   // 하드코딩 색상 금지
```

애니메이션 외부 라이브러리 추가 금지 (`framer-motion`, `react-spring` 등).

---

## 6. 상태 규칙

탭 선택, 아코디언 열림/닫힘 등 **순수 UI 상태**만 `useState` 허용.

```tsx
// ✅ 허용 — UI 상태
const [activeTab, setActiveTab] = useState('deposit');
const [isOpen, setIsOpen] = useState(false);
```

**금지**

```tsx
// ❌ API 호출
useEffect(() => {
  fetch('/api/accounts');
}, []);

// ❌ React Query
const { data } = useQuery({ queryKey: ['accounts'], queryFn: fetchAccounts });
```

---

## 7. 컴포넌트 선택 우선순위 및 Figma 매핑

1. `biz/` — 도메인 특화 컴포넌트 (가장 구체적)
2. `modules/` — 도메인 무관 모듈 컴포넌트
3. `layout/` — 레이아웃 컴포넌트
4. `core/` — 원자 컴포넌트

목록에 없는 컴포넌트를 임의로 새로 만들지 않는다. 가장 유사한 기존 컴포넌트를 사용한다.

### Figma UI 요소 → React 컴포넌트 빠른 참조

| Figma UI 요소        | React 컴포넌트                            | 비고                                                                               |
| -------------------- | ----------------------------------------- | ---------------------------------------------------------------------------------- |
| 버튼                 | `Button`                                  | variant: `primary \| outline \| ghost \| danger`                                   |
| 입력 필드            | `Input`                                   |                                                                                    |
| 셀렉트 박스          | `Select`                                  | size prop 없음                                                                     |
| 텍스트 레이블        | `Typography`                              | variant: `heading \| subheading \| body-lg \| body \| body-sm \| caption`          |
| 아이콘               | `lucide-react`                            | @cl에 아이콘 컴포넌트 없음                                                         |
| 뱃지/태그            | `Badge`                                   | variant: `primary \| brand \| success \| danger \| warning \| neutral`             |
| 체크박스             | `Checkbox`                                |                                                                                    |
| 라디오 버튼          | `Radio`                                   |                                                                                    |
| 토글/스위치          | `Toggle`                                  |                                                                                    |
| 구분선               | `<hr className="border-border-subtle" />` | 별도 컴포넌트 없음                                                                 |
| 카드 컨테이너        | `Card`                                    | 테두리 있는 컨테이너                                                               |
| 목록 항목 (클릭)     | `ListItem`                                |                                                                                    |
| 레이블-값 행         | `InfoRow`                                 | label·value 모두 string만; JSX 포함 시 `LabelValueRow`                             |
| 레이블-값 행 (JSX)   | `LabelValueRow`                           | value에 Badge·Button 등 JSX 허용                                                   |
| 모달                 | `Modal`                                   |                                                                                    |
| 바텀시트             | `BottomSheet`                             | 모바일(390px) 기본                                                                 |
| 탭 네비게이션        | `TabNav`                                  | variant: `underline \| pill`                                                       |
| 빈 상태 화면         | `EmptyState`                              |                                                                                    |
| 에러 상태 화면       | `ErrorState`                              |                                                                                    |
| 알림 배너            | `AlertBanner`                             | intent: `warning \| danger \| success \| info`                                     |
| 계좌 카드            | `AccountSummaryCard`                      | type: `deposit \| savings \| loan \| foreignDeposit \| retirement \| securities`   |
| 카드(신용/체크) 카드 | `CardSummaryCard`                         | type: `credit \| check \| prepaid`                                                 |
| 보험 카드            | `InsuranceSummaryCard`                    | type: `life \| health \| car`                                                      |
| 배너 슬라이더        | `BannerCarousel`                          |                                                                                    |
| 그라데이션 배너      | `BrandBanner`                             | `<Card variant="brand">` 사용 금지                                                 |
| 바텀 글로벌 탭       | `HomePageLayout` props                    | `withBottomNav`, `activeId`, `bottomNavItems`로 제어. 별도 `<BottomNav>` 추가 금지 |
| 단계 표시기          | `StepIndicator`                           | props: `total`, `current`                                                          |
| 검색창               | `SearchInput`                             |                                                                                    |
| 은행 선택 그리드     | `BankSelectGrid`                          | prop명: `columns` (`cols` 아님)                                                    |

### Figma Variant 값 → React prop 값 변환

| Figma Variant 값                    | `variant` prop |
| ----------------------------------- | -------------- |
| `Primary` / `Filled`                | `'primary'`    |
| `Secondary` / `Outlined` / `Stroke` | `'outline'`    |
| `Tertiary` / `Ghost` / `Typography` | `'ghost'`      |
| `Destructive` / `Error`             | `'danger'`     |
| `Info`                              | `'info'`       |
| `Neutral`                           | `'neutral'`    |
| `Success`                           | `'success'`    |
| `Warning`                           | `'warning'`    |

---

## 8. 주요 Props 오류 레퍼런스

코드 생성 전 반드시 확인한다. 실제로 자주 발생한 오류 목록이다.

| 잘못된 코드                            | 올바른 코드                                          |
| -------------------------------------- | ---------------------------------------------------- |
| `<Grid columns={4}>`                   | `<Grid cols={4}>`                                    |
| `<Button variant="secondary">`         | `<Button variant="outline">`                         |
| `<Badge variant="info">`               | `<Badge variant="neutral">`                          |
| `<AlertBanner intent="primary">`       | `<AlertBanner intent="info">`                        |
| `<AlertBanner intent="error">`         | `<AlertBanner intent="danger">`                      |
| `<Card variant="brand">`               | `<BrandBanner>` 컴포넌트 사용                        |
| `<Stack direction="horizontal">`       | `<Inline>`                                           |
| `<BottomNav icons={}>`                 | `<BottomNav items={[{ id, icon, label, onClick }]}>` |
| `<Typography variant="title-xl">`      | `<Typography variant="heading">`                     |
| `<Typography variant="h1">`            | `<Typography variant="heading">`                     |
| `<Typography color="gray">`            | `<Typography color="muted">`                         |
| `<Typography color="primary">`         | `<Typography color="brand">`                         |
| `<Select size="md">`                   | `<Select>` (size prop 없음)                          |
| `<InfoRow value={<Badge />}>`          | `<LabelValueRow value={<Badge />}>`                  |
| `<TabNav variant="segment">`           | `<TabNav variant="pill">`                            |
| `<CardPillTab selected={true}>`        | `<CardPillTab isSelected={true}>`                    |
| `<SelectableListItem selected={true}>` | `<SelectableListItem isSelected={true}>`             |
| `<BankSelectGrid cols={4}>`            | `<BankSelectGrid columns={4}>`                       |
| `<StepIndicator step={2}>`             | `<StepIndicator total={4} current={2}>`              |

> ⚠️ `InfoRow`는 `label`, `value` 모두 `string`만 허용. JSX(아이콘, 버튼 등)가 필요하면 `LabelValueRow` 사용.  
> ⚠️ `BottomNav`의 각 item에 `onClick`은 필수. 누락 시 TypeScript 오류.

### 컴포넌트별 허용 variant

| 컴포넌트                       | 허용 값                                                                                      |
| ------------------------------ | -------------------------------------------------------------------------------------------- |
| `Button` variant               | `primary` \| `outline` \| `ghost` \| `danger`                                                |
| `Badge` variant                | `primary` \| `brand` \| `success` \| `danger` \| `warning` \| `neutral`                      |
| `Typography` variant           | `heading` \| `subheading` \| `body-lg` \| `body` \| `body-sm` \| `caption`                   |
| `Typography` color             | `heading` \| `base` \| `label` \| `secondary` \| `muted` \| `brand` \| `danger` \| `success` |
| `AlertBanner` intent           | `warning` \| `danger` \| `success` \| `info`                                                 |
| `TabNav` variant               | `underline` \| `pill`                                                                        |
| `BannerCarousel` items.variant | `promo` \| `info` \| `warning`                                                               |
| `AccountSummaryCard` type      | `deposit` \| `savings` \| `loan` \| `foreignDeposit` \| `retirement` \| `securities`         |
| `CardSummaryCard` type         | `credit` \| `check` \| `prepaid`                                                             |
| `InsuranceSummaryCard` type    | `life` \| `health` \| `car`                                                                  |
| `InsuranceSummaryCard` status  | `active` \| `pending` \| `expired`                                                           |

---

## 9. 금지 규칙

| 금지 항목                                            | 이유                                                          |
| ---------------------------------------------------- | ------------------------------------------------------------- |
| HTML 태그 직접 사용 (`div`, `button`, `p` 등)        | 디자인 시스템 일관성 붕괴                                     |
| inline style                                         | 디자인 토큰 추적 불가                                         |
| 하드코딩 색상·수치                                   | 테마 변경 시 전체 수정 필요                                   |
| API 호출·React Query·useEffect 데이터 패칭           | 순수 UI 원칙 위반                                             |
| `@cl` 이외 UI 라이브러리 추가                        | `@cl`이 유일한 UI 소스                                        |
| 존재하지 않는 컴포넌트 임의 생성                     | 디자인 시스템 이탈                                            |
| 파일 분리 (여러 파일 생성)                           | 단일 tsx 파일이 생성 단위                                     |
| `HomePageLayout` 사용 시 `<BottomNav>`를 별도로 추가 | `withBottomNav` prop이 내부에서 렌더링하므로 이중 렌더링 발생 |
| HomePageLayout 내부에 패딩 추가                      | 내장 패딩과 중복                                              |
| 애니메이션 외부 라이브러리 추가                      | 번들 크기·디자인 시스템 일관성 훼손                           |

---

## 10. 불명확한 경우 처리 원칙

판단이 불명확한 경우 **임의로 결정하되, 해당 위치에 주석으로 가정 내용을 명시**한다.  
확인 질문을 응답으로 보내지 않는다. 항상 tsx 파일을 생성한다.

```tsx
// data-brand: Figma에서 브랜드 식별 불가 → 기본값 hana 적용
<div data-brand="hana" data-domain="banking">
```

```tsx
const mockProps: AccountPageProps = {
  // 실제 데이터 구조 불명확 → 일반적인 형태로 작성
  accountName: '하나 주거래 통장',
  balance: 3000000,
};
```

### 기본값 정책

| 상황                      | 처리                                                           |
| ------------------------- | -------------------------------------------------------------- |
| 브랜드 불명확             | `data-brand="hana"`, `data-domain="banking"` 적용 후 주석 명시 |
| 컴포넌트 매핑 불명확      | 섹션 7 빠른 참조표에서 가장 유사한 컴포넌트 선택               |
| 금액 mock                 | 실제처럼 보이는 숫자 사용 (`285000`, `3000000`)                |
| 목록 mock                 | 2~3개 항목으로 구성                                            |
| 날짜 mock                 | `'2026.04.22'` 형식 사용                                       |
| Figma 섹션 일부 구현 불가 | 생략하지 않고 빈 상태로 구조 유지 후 주석으로 이유 명시        |
