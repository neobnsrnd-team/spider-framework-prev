# Component Types

> 이 파일은 `scripts/extract-components.ts`로 자동 생성됩니다. 직접 수정하지 마세요.
> Claude API system prompt에 포함되어 컴포넌트 API 레퍼런스로 사용됩니다.

## 사용 규칙

- 아래 컴포넌트만 사용할 것. 목록에 없는 컴포넌트는 존재하지 않음
- import 경로: `@neobnsrnd-team/reactive-springware`
- TypeScript로 작성하고 props interface를 포함할 것

## Core (원자 컴포넌트 — Button, Input, Badge 등 단일 HTML 요소 수준)

### Badge

```typescript
import React from 'react';

/** primary: 시스템 공통 파란색 | brand: 현재 은행 브랜드색 */
export type BadgeVariant = 'primary' | 'brand' | 'success' | 'danger' | 'warning' | 'neutral';

export interface BadgeProps {
  /** 배지 색상 변형. 기본: 'neutral' */
  variant?:   BadgeVariant;
  /** dot 모드일 때는 children 없이도 사용 가능 */
  children?:  React.ReactNode;
  /** true이면 텍스트 없이 점(dot)만 표시 */
  dot?:       boolean;
  className?: string;
}
```

### Button

```typescript
import React from 'react';

export type ButtonVariant = 'primary' | 'outline' | 'ghost' | 'danger';
export type ButtonSize    = 'sm' | 'md' | 'lg';
/**
 * 버튼 내부 콘텐츠 정렬 방향.
 * - `center` (기본): 텍스트+아이콘을 가운데 정렬
 * - `between`: 아코디언 트리거 등에서 좌우 분리
 */
export type ButtonJustify = 'center' | 'between';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** 버튼 외형 변형. 기본: 'primary' */
  variant?:   ButtonVariant;
  /** 버튼 크기. 기본: 'md' */
  size?:      ButtonSize;
  /** true이면 로딩 스피너 표시 및 aria-busy 처리 */
  loading?:   boolean;
  /** true이면 정방형 아이콘 전용 버튼 (텍스트 숨김) */
  iconOnly?:  boolean;
  leftIcon?:  React.ReactNode;
  rightIcon?: React.ReactNode;
  /** true이면 w-full 적용 */
  fullWidth?: boolean;
  /** 내부 정렬. 기본: 'center' */
  justify?:   ButtonJustify;
}

export interface ButtonGroupProps {
  children:   React.ReactNode;
  className?: string;
}
```

### Input

```typescript
import React from 'react';

export type InputSize            = 'md' | 'lg';
export type InputValidationState = 'default' | 'error' | 'success';

export interface InputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /** 입력 필드 레이블 */
  label?:           string;
  /** 안내 문구 또는 에러 메시지. validationState='error' 시 빨간색으로 표시 */
  helperText?:      string;
  /** 유효성 상태. 기본: 'default' */
  validationState?: InputValidationState;
  /**
   * 입력 필드 높이. 기본: 'md'.
   * HTMLInputElement의 size(number)와 충돌하므로 Omit 후 재정의.
   */
  size?:            InputSize;
  leftIcon?:        React.ReactNode;
  /** 우측 버튼/단위 슬롯 (인증번호 전송, 단위 텍스트 등) */
  rightElement?:    React.ReactNode;
  /** true이면 w-full 적용 */
  fullWidth?:       boolean;
  /**
   * 입력값 포맷 패턴. `#`은 숫자 한 자리, 그 외 문자는 구분자로 그대로 삽입된다.
   * 숫자만 입력받고, 최대 자릿수는 패턴의 `#` 개수로 자동 제한된다.
   *
   * @example
   * '###-######-#####'  // 하나은행  → 012-345678-90123
   * '######-##-######'  // KB국민은행 → 012345-67-890123
   */
  formatPattern?:   string;
  /**
   * 한국 휴대폰번호 포맷 적용 여부.
   * 자릿수에 따라 포맷이 동적으로 전환된다.
   * - 10자리: 010-XXX-XXXX  (3-3-4)
   * - 11자리: 010-XXXX-XXXX (3-4-4)
   * `formatPattern`과 함께 사용하면 `phoneFormat`이 우선 적용된다.
   */
  phoneFormat?:     boolean;
}
```

### Select

```typescript
export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps {
  /** 드롭다운 선택지 목록 */
  options:    SelectOption[];
  /** 현재 선택된 값 */
  value:      string;
  /** 선택 변경 핸들러 */
  onChange:   (value: string) => void;
  /** 접근성 레이블 (aria-label) */
  'aria-label'?: string;
  className?: string;
}
```

### Typography

```typescript
import React from 'react';

export type TypographyVariant = 'heading' | 'subheading' | 'body-lg' | 'body' | 'body-sm' | 'caption';
export type TypographyWeight  = 'normal' | 'medium' | 'bold';
export type TypographyColor   = 'heading' | 'base' | 'label' | 'secondary' | 'muted' | 'brand' | 'danger' | 'success';

export interface TypographyProps {
  /** 텍스트 크기 변형. 기본: 'body' */
  variant?:   TypographyVariant;
  /** 폰트 굵기. 기본: variant별 기본값 적용 */
  weight?:    TypographyWeight;
  /** 텍스트 색상. 기본: 'base' */
  color?:     TypographyColor;
  /** true이면 font-numeric(Manrope) 적용 — 금액·숫자 표시용 */
  numeric?:   boolean;
  /** 렌더링할 HTML 태그. 기본: 'p' */
  as?:        React.ElementType;
  children:   React.ReactNode;
  className?: string;
}

/* 하위 호환성 — 기존 TextProps 참조가 있는 고객 코드를 위해 alias 유지 */
export type TextProps    = TypographyProps;
export type TextVariant  = TypographyVariant;
export type TextWeight   = TypographyWeight;
export type TextColor    = TypographyColor;
```

## Layout (페이지 구조 컴포넌트 — PageLayout, Stack, Grid, Inline 등)

### AppBrandHeader

```typescript
export interface AppBrandHeaderProps {
  /**
   * 브랜드 이니셜 — 원형 배지 내부에 표시.
   * 예: 'H' (하나은행), 'K' (국민은행), 'S' (신한은행)
   */
  brandInitial: string;
  /**
   * 브랜드명 — 이니셜 배지 오른쪽에 표시.
   * 예: '하나은행'
   */
  brandName: string;
  className?: string;
}
```

### BlankPageLayout

```typescript
import React from 'react';

export interface BlankPageLayoutProps extends React.HTMLAttributes<HTMLDivElement> {
  /**
   * 콘텐츠 세로 정렬 방식.
   * - 'top': 상단 정렬 (기본값 — 온보딩 스크롤 화면)
   * - 'center': 수직 중앙 정렬 (로그인 폼 등 단일 블록 화면)
   */
  align?: 'top' | 'center';
  className?: string;
}
```

### BottomNav

```typescript
import React from 'react';

export interface BottomNavItem {
  /** 항목 고유 식별자 (activeId 비교에 사용) */
  id:          string;
  /** 비활성 상태 아이콘 */
  icon:        React.ReactNode;
  /** 활성 상태 아이콘. 미전달 시 icon 재사용 */
  activeIcon?: React.ReactNode;
  /** 탭 레이블 텍스트 */
  label:       string;
  /** 탭 클릭 핸들러 */
  onClick:     () => void;
}

export interface BottomNavProps {
  /** 탭 항목 목록 */
  items:      BottomNavItem[];
  /** 현재 활성 탭 id */
  activeId:   string;
  className?: string;
}
```

### Grid

```typescript
import React from 'react';

import type { StackGap } from '../Stack/types';

export interface GridProps {
  children:      React.ReactNode;
  /**
   * 모바일 기준 열 수. 기본: 2.
   */
  cols?:         1 | 2 | 3 | 4;
  /**
   * 태블릿(md 이상) 기준 열 수. 미전달 시 cols 값 유지.
   */
  tabletCols?:   2 | 3 | 4;
  /** 셀 간격. 기본: 'sm' */
  gap?:          StackGap;
  className?:    string;
}
```

### HomePageLayout

```typescript
import React from 'react';

export interface HomePageLayoutProps extends React.HTMLAttributes<HTMLDivElement> {
  /** 헤더 타이틀 (앱 이름 또는 서비스명, 예: '하나은행') */
  title: string;
  /**
   * 타이틀 좌측에 표시할 로고 아이콘 슬롯.
   * 미전달 시 로고 영역 렌더링 안 함
   */
  logo?: React.ReactNode;
  /**
   * 헤더 우측 슬롯.
   * 미전달 시 기본 프로필·벨·메뉴 3개 아이콘 버튼 표시
   */
  rightAction?: React.ReactNode;
  /**
   * 알림 뱃지 표시 여부.
   * rightAction을 직접 전달한 경우 무시됨.
   * 기본: false
   */
  hasNotification?: boolean;
  /** 하단 글로벌 탭바 영역 여백 자동 추가 여부. 기본: true */
  withBottomNav?: boolean;
  className?: string;
}
```

### Inline

```typescript
import React from 'react';

import type { StackGap } from '../Stack/types';

export type InlineAlign   = 'start' | 'center' | 'end' | 'baseline' | 'stretch';
export type InlineJustify = 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly';

export interface InlineProps {
  children:   React.ReactNode;
  /** 요소 간 간격. 기본: 'sm' */
  gap?:       StackGap;
  /** 교차축(세로) 정렬. 기본: 'center' */
  align?:     InlineAlign;
  /** 주축(가로) 정렬. 기본: 'start' */
  justify?:   InlineJustify;
  /** true이면 flex-wrap 적용 */
  wrap?:      boolean;
  /** 렌더링할 HTML 태그. 기본: 'div' */
  as?:        React.ElementType;
  className?: string;
}
```

### ModalSlideOver

```typescript
import type { ReactNode } from 'react';

export interface ModalSlideOverProps {
  /** 모달 내부에 렌더링할 콘텐츠 */
  children: ReactNode;
  /** 백드롭 클릭 시 호출. 미전달 시 백드롭 클릭으로 닫기 비활성 */
  onClose?: () => void;
  /** 슬라이드 방향 (기본: 'right') */
  direction?: 'right' | 'bottom';
  /** z-index 레벨 (기본: 50) */
  zIndex?: number;
}
```

### PageLayout

```typescript
import React from 'react';

export interface PageLayoutProps extends React.HTMLAttributes<HTMLDivElement> {
  /** 상단 헤더 타이틀 */
  title:         string;
  /** 전달 시 헤더 좌측에 뒤로가기(<) 버튼 표시 */
  onBack?:       () => void;
  /** 헤더 우측 슬롯 (알림·설정·닫기 버튼 등) */
  rightAction?:  React.ReactNode;
  /**
   * 화면 하단 고정 액션 바 슬롯 (iOS 스타일 하단 버튼 영역).
   * 전달 시 화면 하단에 blur 배경 고정 바가 렌더링되며,
   * 본문 스크롤 영역에 동일한 높이의 spacer가 추가되어
   * 마지막 콘텐츠가 고정 바에 가려지지 않는다.
   */
  bottomBar?:    React.ReactNode;
  className?:    string;
}
```

### Section

```typescript
import React from 'react';

export interface SectionProps {
  /** 섹션 제목. 전달 시 SectionHeader 노출, 미전달 시 제목 없이 콘텐츠만 렌더링 */
  title?: string;
  /** 제목 옆 숫자 배지 (항목 수 등) */
  badge?: number;
  /** 우측 액션 레이블 (예: '전체보기') */
  actionLabel?: string;
  /** 액션 클릭 핸들러 */
  onAction?: () => void;
  /** 섹션 내부 콘텐츠 */
  children: React.ReactNode;
  /** SectionHeader와 children 사이, 그리고 children 내부 항목 간격. 기본: 'md' */
  gap?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}
```

### Stack

```typescript
import React from 'react';

export type StackGap   = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
export type StackAlign = 'start' | 'center' | 'end' | 'stretch';

export interface StackProps {
  children:   React.ReactNode;
  /** 자식 요소 간 간격. 기본: 'md' */
  gap?:       StackGap;
  /** 교차축(가로) 정렬. 기본: 'stretch' */
  align?:     StackAlign;
  /** 렌더링할 HTML 태그. 기본: 'div' */
  as?:        React.ElementType;
  className?: string;
}
```

## Modules (분자 컴포넌트 — 2개 이상 Core 조합, 도메인 무관)

### AccountSelectItem

```typescript
import React from 'react';

export interface AccountSelectItemProps {
  /**
   * 아이콘 슬롯.
   * lucide-react 아이콘 컴포넌트를 전달한다.
   * 선택 상태에 따라 컨테이너 배경·색상이 자동 전환된다.
   */
  icon?: React.ReactNode;
  /** 계좌 명칭 (예: \"하나 주거래 통장\") */
  accountName: string;
  /** 계좌번호 (예: \"123-456-789012\") */
  accountNumber: string;
  /**
   * 잔액 표시 문자열 (예: \"3,000,000원\").
   * 포맷 처리는 호출자에서 완료 후 전달한다.
   */
  balance: string;
  /**
   * 선택 상태. 기본값: false
   * - true : 브랜드 배경(bg-brand-5) + 브랜드 아이콘 원 + 우측 체크 아이콘
   * - false: 기본 배경 + 중립 아이콘 원
   */
  selected?: boolean;
  /** 클릭 핸들러 */
  onClick?: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### ActionLinkItem

```typescript
import React from 'react';

export interface ActionLinkItemProps {
  /**
   * 아이콘 슬롯.
   * lucide-react Icon 컴포넌트 또는 이미지 요소를 전달한다.
   * @example <Share2 className="size-5" />
   */
  icon: React.ReactNode;
  /**
   * 아이콘 배경 원의 추가 className.
   * 브랜드별 아이콘 배경색을 지정할 때 사용한다.
   * 기본값: 'bg-brand-10 text-brand-text' (브랜드 청록색 반투명)
   * @example "bg-[#fee500]" — 카카오톡 노란색 배경
   */
  iconBgClassName?: string;
  /** 링크 레이블 텍스트 */
  label: string;
  /**
   * 상하 패딩 크기. 기본값: 'md'.
   * - 'md': py-standard (16px) — 카드형 단독 액션 버튼
   * - 'sm': py-sm (8px)       — 목록 내 행처럼 촘촘하게 나열할 때
   */
  size?: 'sm' | 'md';
  /**
   * 카드 테두리 표시 여부. 기본값: true.
   * false로 설정하면 border·shadow가 제거되어 목록 내 행(row) 형태로 사용 가능.
   * @example showBorder={false} — 전체 메뉴 화면의 메뉴 목록 행
   */
  showBorder?: boolean;
  /** 클릭 이벤트 핸들러 */
  onClick?: () => void;
  /** 추가 CSS className */
  className?: string;
}
```

### AlertBanner

```typescript
import React from 'react';

/**
 * 배너의 의미·색상 조합을 결정하는 intent.
 * - 'warning' : 주의·경고 (amber/노란색)
 * - 'danger'  : 오류·위험 (red)
 * - 'success' : 완료·성공 (green)
 * - 'info'    : 안내·정보 (blue)
 */
export type AlertBannerIntent = 'warning' | 'danger' | 'success' | 'info';

export interface AlertBannerProps {
  /**
   * 배너 의미·색상 조합. 기본값: 'warning'
   * Figma 이체 확인 화면의 주의 배너는 'warning'을 사용한다.
   */
  intent?: AlertBannerIntent;
  /**
   * 배너 텍스트 콘텐츠.
   * 문자열 또는 ReactNode(강조 span 포함) 모두 허용한다.
   */
  children: React.ReactNode;
  /**
   * 좌측 아이콘 슬롯 override.
   * 미전달 시 intent별 기본 아이콘이 자동 사용된다.
   * @example icon={<Lock className="size-4" />}
   */
  icon?: React.ReactNode;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### AmountInput

```typescript
export interface AmountInputProps {
  /** 원화 단위 숫자 값. 부모에서 관리하는 controlled 값 */
  value:       number | null;
  /** 값 변경 시 호출. 유효한 숫자 또는 null 전달 */
  onChange:    (value: number | null) => void;
  /** 입력 필드 레이블. 기본: '금액' */
  label?:      string;
  /** 도움말 또는 에러 메시지 */
  helperText?: string;
  /** 에러 상태 여부 */
  hasError?:   boolean;
  /** 빠른 금액 선택 버튼 목록 (단위: 원). 예: [10000, 50000, 100000] */
  quickAmounts?: number[];
  /** 최대 입력 가능 금액 (원). 초과 시 hasError로 처리 */
  maxAmount?:  number;
  /**
   * 이체 한도 안내 텍스트. 금액 입력 행 우측에 표시된다.
   * 예: '1회 5,000,000원 / 1일 10,000,000원'
   */
  transferLimitText?: string;
  disabled?:   boolean;
  /** 플레이스홀더. 기본: '금액을 입력하세요' */
  placeholder?: string;
  className?:  string;
}
```

### BalanceToggle

```typescript
export interface BalanceToggleProps {
  /** true = 잔액 숨김 상태, false = 잔액 표시 상태 */
  hidden: boolean
  /** 토글 클릭 시 호출되는 핸들러 */
  onToggle: () => void
  className?: string
}
```

### BankSelectGrid

```typescript
/** 은행 목록 단일 항목 */
export interface BankItem {
  /** 은행 식별 코드. 예: 'hana', 'kb', 'shinhan' */
  code: string;
  /** 화면에 표시할 은행명. 예: '하나은행' */
  name: string;
  /** 은행 로고/아이콘 (ReactNode). 미전달 시 기본 아이콘 표시 */
  icon?: React.ReactNode;
}

export interface BankSelectGridProps {
  /** 선택 가능한 은행 목록 */
  banks: BankItem[];
  /** 현재 선택된 은행 코드 */
  selectedCode?: string;
  /** 은행 선택 핸들러 */
  onSelect: (code: string) => void;
  /** 한 행에 표시할 열 수 (기본: 4) */
  columns?: 3 | 4;
}
```

### BottomSheet

```typescript
import React from 'react';

/**
 * 시트 최대 높이 프리셋.
 * - 'auto': 콘텐츠 높이에 맞춤 (최대 90dvh)
 * - 'half': 화면 절반(50dvh)
 * - 'full': 전체 화면(90dvh)
 */
export type BottomSheetSnap = 'auto' | 'half' | 'full';

export interface BottomSheetProps {
  /** 시트 열림 여부 */
  open: boolean;
  /** 시트 닫기 핸들러 (백드롭 클릭, ESC 키, 닫기 버튼 공통) */
  onClose: () => void;
  /** 시트 상단 타이틀. 미전달 시 타이틀 영역 렌더링 안 함 */
  title?: string;
  /** 본문 슬롯 */
  children?: React.ReactNode;
  /** 하단 고정 버튼 영역 슬롯 */
  footer?: React.ReactNode;
  /**
   * 시트 최대 높이 프리셋. 기본: 'auto'
   * 콘텐츠가 프리셋 높이를 초과하면 본문 영역이 내부 스크롤로 전환
   */
  snap?: BottomSheetSnap;
  /**
   * 백드롭 클릭으로 닫기 비활성화 여부.
   * 필수 액션이 있는 시트(예: 약관 동의)에서 true로 설정.
   * 기본: false
   */
  disableBackdropClose?: boolean;
  /**
   * 헤더 우측 X(닫기) 버튼 숨김 여부. 기본: false
   * Footer 버튼으로만 닫기를 처리하는 시트(예: 이체 확인)에서 true로 설정한다.
   * true로 설정해도 ESC 키·백드롭 클릭 닫기는 유지된다.
   */
  hideCloseButton?: boolean;
  className?: string;
}
```

### Card

```typescript
import React from 'react';

export interface CardProps {
  children:     React.ReactNode;
  /** true이면 hover/active 인터랙션 스타일 활성화 */
  interactive?: boolean;
  /** 전달 시 <button> 태그로 렌더링 */
  onClick?:     () => void;
  /**
   * true이면 카드 내부 padding을 제거한다.
   * CardHighlight 등 카드 전체 너비를 차지하는 섹션을 구성할 때 사용한다.
   * 이 경우 내부 콘텐츠에서 직접 padding을 지정해야 한다.
   */
  noPadding?:   boolean;
  className?:   string;
}

export interface CardHeaderProps {
  title:      string;
  subtitle?:  string;
  /** 헤더 우측 버튼/링크 슬롯 */
  action?:    React.ReactNode;
  /** 헤더 좌측 아이콘 슬롯 */
  icon?:      React.ReactNode;
}

export interface CardRowProps {
  label:            string;
  value:            string;
  /** 금액 강조 등 값 텍스트 스타일 override */
  valueClassName?:  string;
}

/**
 * 우측에 임의 ReactNode를 배치하는 카드 행 컴포넌트.
 * value가 단순 문자열이 아닌 경우(편집 아이콘, 액션 버튼 포함 행 등)에 사용한다.
 * 예) 메모 편집 행, 상대계좌 + 이체하기 버튼 행
 */
export interface CardActionRowProps {
  /** 행 좌측 레이블 */
  label:      string;
  /** 행 우측에 자유롭게 배치할 ReactNode */
  children:   React.ReactNode;
  className?: string;
}

/**
 * 카드 하단의 강조 섹션 컴포넌트.
 * 이체 후 잔액 등 핵심 수치를 브랜드 색상 배경으로 강조 표시할 때 사용한다.
 * Card의 noPadding prop과 함께 사용하면 카드 전체 너비를 채울 수 있다.
 */
export interface CardHighlightProps {
  /** 좌측 레이블 텍스트 (예: "이체 후 잔액") */
  label:            string;
  /** 우측 값 텍스트 (예: "2,900,000원") */
  value:            string;
  /** 값 텍스트 스타일 override */
  valueClassName?:  string;
}
```

### Checkbox

```typescript
import React from 'react';

export interface CheckboxProps {
  /** 체크 상태 */
  checked: boolean;
  /** 상태 변경 핸들러 */
  onChange: (checked: boolean) => void;
  /** 우측 레이블 텍스트 또는 ReactNode */
  label?: React.ReactNode;
  /**
   * label prop이 없을 때 스크린리더용 접근성 이름.
   * label이 없으면 반드시 전달해야 WCAG 접근성 기준을 충족한다.
   */
  ariaLabel?: string;
  /**
   * 체크박스 모양. 기본: 'square'
   * - 'square' : 둥근 모서리 사각형 (기본값, 일반 체크박스)
   * - 'circle' : 원형 (라디오 버튼 느낌의 단독 선택 등에 사용)
   */
  shape?: 'square' | 'circle';
  /** 비활성화 여부 */
  disabled?: boolean;
  /**
   * 체크박스 input의 id.
   * 전달 시 label htmlFor와 연결되어 레이블 클릭으로도 체크 상태 변경 가능.
   */
  id?: string;
  className?: string;
}
```

### CollapsibleSection

```typescript
import type React from 'react';

export interface CollapsibleSectionProps {
  /**
   * 항상 노출되는 헤더 영역.
   * 클릭 시 콘텐츠 표시/숨김이 토글된다.
   */
  header: React.ReactNode;
  /** 펼침 상태에서만 표시되는 콘텐츠 */
  children: React.ReactNode;
  /**
   * 초기 펼침 여부.
   * @default true — 기본적으로 펼쳐진 상태로 렌더링
   */
  defaultExpanded?: boolean;
  /**
   * 헤더 텍스트 영역 정렬.
   * - 'left'  : 텍스트를 왼쪽 정렬 (화살표는 항상 우측 끝 고정)
   * - 'center': 텍스트를 가운데 정렬
   * @default 'center'
   */
  headerAlign?: 'left' | 'center';
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### DatePicker

```typescript
import type React from 'react';

export type DatePickerMode = 'single' | 'range';

export interface DatePickerProps {
  /** 선택 모드. 기본: 'single' */
  mode?:          DatePickerMode;
  /** single 모드에서 사용하는 선택된 날짜 */
  value?:         Date | null;
  /** range 모드에서 사용하는 시작·종료 날짜 */
  rangeValue?:    [Date | null, Date | null];
  onChange?:      (date: Date | null) => void;
  onRangeChange?: (range: [Date | null, Date | null]) => void;
  minDate?:       Date;
  maxDate?:       Date;
  placeholder?:   string;
  label?:         string;
  disabled?:      boolean;
  className?:     string;
  /**
   * 외부에서 달력 열림 상태를 제어할 때 사용 (제어 모드).
   * 이 prop이 제공되면 내장 트리거 버튼을 렌더링하지 않는다.
   */
  open?:          boolean;
  /** 달력 열림 상태 변경 콜백 (제어 모드 전용) */
  onOpenChange?:  (open: boolean) => void;
  /**
   * 달력 패널 위치 계산의 기준이 되는 외부 트리거 요소 (제어 모드 전용).
   * 미제공 시 내장 triggerRef를 사용한다.
   */
  anchorRef?:     React.RefObject<HTMLElement | null>;
}
```

### DividerWithLabel

```typescript
export interface DividerWithLabelProps {
  /** 구분선 중앙에 표시할 텍스트 (예: '다른 로그인 방식') */
  label: string;
  className?: string;
}
```

### DropdownMenu

```typescript
import React from 'react';

/** 드롭다운 메뉴 단일 항목 */
export interface DropdownMenuItem {
  /** 표시 레이블 */
  label: string;
  /** 좌측 아이콘 (선택) */
  icon?: React.ReactNode;
  /** 항목 클릭 핸들러 */
  onClick: () => void;
  /**
   * 항목 스타일 변형. 기본: 'default'
   * - 'default' : 일반 텍스트 색상
   * - 'danger'  : 위험 액션(예: 로그아웃·삭제)에 사용, semantic-danger 색상 적용
   */
  variant?: 'default' | 'danger';
}

export interface DropdownMenuProps {
  /** 드롭다운을 열고 닫는 트리거 요소 */
  children: React.ReactNode;
  /** 드롭다운에 표시할 항목 목록 */
  items: DropdownMenuItem[];
  /**
   * 패널 정렬 방향. 기본: 'right'
   * - 'right' : 트리거 우측 기준 좌측으로 패널 정렬 (우측 끝 버튼에 적합)
   * - 'left'  : 트리거 좌측 기준 우측으로 패널 정렬
   */
  align?: 'left' | 'right';
  className?: string;
}
```

### EmptyState

```typescript
import React from 'react';

export interface EmptyStateProps {
  /** SVG 또는 img 요소. 도메인별 일러스트를 외부에서 주입 */
  illustration?: React.ReactNode;
  title:         string;
  description?:  string;
  /** CTA 버튼 등 액션 슬롯 */
  action?:       React.ReactNode;
  className?:    string;
}
```

### ErrorState

```typescript
export interface ErrorStateProps {
  /** 사용자에게 표시할 에러 메시지 */
  title?: string;
  /** 상세 설명 (선택) */
  description?: string;
  /** 재시도 버튼 클릭 핸들러. 전달 시 재시도 버튼 노출 */
  onRetry?: () => void;
  /** 재시도 버튼 레이블. 기본: '다시 시도' */
  retryLabel?: string;
  className?: string;
}
```

### InfoRow

```typescript
export interface InfoRowProps {
  /** 행 좌측 레이블 텍스트 (예: "출금계좌") */
  label: string;
  /** 행 우측 값 텍스트 (예: "하나 123-456-789012") */
  value: string;
  /**
   * 값 텍스트에 추가할 Tailwind 클래스.
   * 수수료 0원처럼 특정 행 값에 브랜드·강조 색상을 적용할 때 사용한다.
   * @example valueClassName="text-brand-text"
   * @example valueClassName="text-base"  — 금액처럼 한 단계 큰 텍스트
   */
  valueClassName?: string;
  /**
   * 하단 구분선 표시 여부. 기본값: true
   * 목록 마지막 행이나 구분선이 불필요한 경우 false로 설정한다.
   */
  showBorder?: boolean;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### LabelValueRow

```typescript
import type React from 'react';

export interface LabelValueRowProps {
  /** 좌측 레이블 텍스트 (caption 크기, muted 색상) */
  label: string;
  /**
   * 우측 값 영역.
   * 문자열 또는 스타일이 필요한 경우 ReactNode를 전달한다.
   * (예: 금액에 numeric 폰트·색상 적용 시 <span> 전달)
   */
  value: React.ReactNode;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### Modal

```typescript
import React from 'react';

export type ModalSize      = 'sm' | 'md' | 'lg' | 'fullscreen';
/**
 * 모달 헤더 타이틀 정렬 방향.
 * - 'left'  (기본): 타이틀 좌측 정렬, X 버튼 우측 배치 (일반 확인 모달)
 * - 'center': 타이틀 중앙 정렬, X 버튼 절대 배치 우측 (경고/안내 모달)
 */
export type ModalTitleAlign = 'left' | 'center';

export interface ModalProps {
  /** 모달 표시 여부 */
  open:              boolean;
  /** 닫기 요청 핸들러 (ESC 키, 배경 클릭 포함) */
  onClose:           () => void;
  /** 헤더 제목. 생략 시 닫기 버튼만 표시 */
  title?:            string;
  /** 본문 영역. 내용이 길면 내부 스크롤 */
  children:          React.ReactNode;
  /** 하단 버튼 영역. Button 조합 권장 */
  footer?:           React.ReactNode;
  /**
   * 데스크톱 기준 모달 최대 너비.
   * 모바일에서는 항상 전체 너비 Bottom Sheet.
   * @default 'md'
   */
  size?:             ModalSize;
  /** true이면 배경 클릭으로 닫기 비활성화 */
  disableBackdropClose?: boolean;
  /**
   * 헤더 타이틀 정렬.
   * - 'left'  (기본): 타이틀 좌측, X 버튼 우측 (일반 확인 모달)
   * - 'center': 타이틀 중앙, X 버튼 절대 우측 (경고·안내 모달)
   * @default 'left'
   */
  titleAlign?:       ModalTitleAlign;
  className?:        string;
}
```

### NoticeItem

```typescript
import React from 'react';

export interface NoticeItemProps {
  /**
   * 아이콘 슬롯.
   * lucide-react Icon 요소를 전달한다.
   */
  icon:             React.ReactNode;
  /**
   * 아이콘 배경 원의 추가 className.
   * 기본값: 'bg-brand-5 text-brand-text'
   * @example "bg-[#ecfdf5] text-success-text" — 초록색 배경
   */
  iconBgClassName?: string;
  /** 공지 제목 */
  title:            string;
  /** 공지 부제목 (미전달 시 미노출) */
  description?:     string;
  /** 항목 클릭 핸들러 */
  onClick?:         () => void;
  /**
   * 하단 구분선 표시 여부.
   * 목록의 마지막 항목에는 false를 전달한다.
   * 기본값: true
   */
  showDivider?:     boolean;
  className?:       string;
}
```

### NumberKeypad

```typescript
export interface NumberKeypadProps {
  /**
   * 키패드에 표시할 숫자 배열 (0~9, 셔플 상태).
   * - digits[0..8]: 3×3 그리드 (행 우선 순서)
   * - digits[9]: 하단 행 중앙 버튼
   * 길이는 반드시 10이어야 한다.
   */
  digits: number[];
  /**
   * 숫자 버튼 클릭 시 호출.
   * @param digit - 클릭된 숫자 (0~9)
   */
  onDigitPress: (digit: number) => void;
  /** 지우기(⌫) 버튼 클릭 시 호출 */
  onDelete: () => void;
  /** 재배열 버튼 클릭 시 호출 — 고객이 digits를 다시 셔플해서 전달해야 한다 */
  onShuffle: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### OtpInput

```typescript
/** OTP 자릿수. 기본 6자리, 필요 시 4자리 PIN 용도로도 사용 */
export type OtpLength = 4 | 6;

export interface OtpInputProps {
  /** OTP 자릿수. 기본: 6 */
  length?: OtpLength;
  /** 입력 완료(length개 모두 입력) 시 호출. 완성된 OTP 문자열 전달 */
  onComplete?: (otp: string) => void;
  /** 각 자릿수 변경 시 호출 */
  onChange?: (otp: string) => void;
  /** 에러 상태 여부 (빨간 테두리 표시) */
  error?: boolean;
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 숫자 마스킹 여부 (비밀번호 스타일). 기본: false */
  masked?: boolean;
  /** 추가 클래스 */
  className?: string;
}
```

### PinConfirmSheet

```typescript
export interface PinConfirmSheetProps {
  /** 시트 열림 여부 */
  open: boolean;
  /** 닫기 핸들러 */
  onClose: () => void;
  /**
   * PIN 입력 완료 핸들러.
   * pinLength 자리가 모두 입력되면 자동 호출된다.
   * @param pin - 입력된 PIN 문자열
   */
  onConfirm: (pin: string) => void;
  /** 시트 상단 타이틀. 기본: '비밀번호 입력' */
  title?: string;
  /** PIN 자릿수. 기본: 4 */
  pinLength?: number;
}
```

### PinDotIndicator

```typescript
export interface PinDotIndicatorProps {
  /**
   * 전체 도트 수 (비밀번호 자릿수).
   * 기본값: 4 — 계좌 비밀번호 4자리에 맞춤
   */
  length?: number;
  /**
   * 채워진(입력된) 도트 수.
   * 0 ≤ filledCount ≤ length 범위여야 한다.
   */
  filledCount: number;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### RecentRecipientItem

```typescript
export interface RecentRecipientItemProps {
  /** 수취인명. 예: '홍길동' */
  name: string;
  /** 은행명. 예: '하나은행' */
  bankName: string;
  /** 마스킹된 계좌번호. 예: '123-****-5678' */
  maskedAccount: string;
  /** 항목 클릭 핸들러 (해당 수취인 정보로 이체 폼 자동 입력) */
  onClick: () => void;
}
```

### SectionHeader

```typescript
export interface SectionHeaderProps {
  /** 섹션 제목 텍스트 */
  title: string;
  /**
   * 제목 우측에 표시할 계좌/항목 수 배지 (예: 2 → '2').
   * 미전달 시 배지 미노출.
   */
  badge?: number;
  /** 우측 액션 레이블 (예: '전체보기'). 미전달 시 액션 영역 미노출 */
  actionLabel?: string;
  /** 액션 클릭 핸들러. actionLabel과 함께 전달해야 동작 */
  onAction?: () => void;
  /** 추가 클래스 */
  className?: string;
}
```

### SelectableItem

```typescript
import React from 'react';

export interface SelectableItemProps {
  /**
   * 아이콘 슬롯.
   * lucide-react 아이콘 컴포넌트를 전달한다.
   * 선택 상태에 따라 컨테이너 배경·아이콘 색상이 자동 전환된다.
   */
  icon: React.ReactNode;
  /** 표시할 레이블 텍스트 (예: "하나은행", "KB국민은행") */
  label: string;
  /**
   * 선택 상태. 기본값: false
   * - true : 브랜드 배경(bg-brand-5) + 브랜드 아이콘 원 + 브랜드 텍스트
   * - false: 중립 배경(bg-surface-subtle) + 회색 아이콘 원 + label 텍스트
   */
  selected?: boolean;
  /** 클릭 핸들러 */
  onClick?: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### SelectableListItem

```typescript
export interface SelectableListItemProps {
  /** 표시 레이블 */
  label: string;
  /** 선택 여부 */
  isSelected: boolean;
  /** 클릭 핸들러 */
  onClick: () => void;
}
```

### SidebarNav

```typescript
/** 사이드바 네비게이션 개별 항목 */
export interface SidebarNavItem {
  /** 항목 고유 식별자 */
  id: string;
  /** 표시할 레이블 텍스트 */
  label: string;
}

export interface SidebarNavProps {
  /** 네비게이션 항목 목록 */
  items: SidebarNavItem[];
  /** 현재 활성화된 항목 id */
  activeId: string;
  /**
   * 항목 클릭 시 호출되는 콜백.
   * 선택된 항목의 id를 인자로 전달한다.
   */
  onItemChange: (id: string) => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### StepIndicator

```typescript
export interface StepIndicatorProps {
  /** 전체 단계 수 */
  total: number;
  /** 현재 진행 중인 단계 (1-based) */
  current: number;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### SuccessHero

```typescript
export interface SuccessHeroProps {
  /**
   * 받는 사람 이름 (예: "홍길동").
   * 타이틀에 "{recipientName}님께" 형태로 삽입된다.
   */
  recipientName: string;
  /**
   * 이체 금액 문자열 (예: "50,000원").
   * 타이틀에 "{amount} 이체 완료" 형태로 삽입된다.
   */
  amount: string;
  /**
   * 부제목 텍스트.
   * 기본값: "성공적으로 이체되었습니다."
   */
  subtitle?: string;
  /** 추가 CSS className */
  className?: string;
}
```

### TabNav

```typescript
export interface TabNavItem {
  /** 탭 고유 식별자 */
  id:    string;
  /** 탭 레이블 텍스트 */
  label: string;
}

/**
 * 탭 스타일 변형.
 * - 'underline': 활성 탭 하단 인디케이터 라인 (기본값). 상단 내비게이션 탭에 사용.
 * - 'pill': 활성 탭에 둥근 배경 채움. 상품 카테고리·필터 탭에 사용.
 */
export type TabNavVariant = 'underline' | 'pill';

export interface TabNavProps {
  /** 탭 목록 */
  items:        TabNavItem[];
  /** 현재 활성 탭 id */
  activeId:     string;
  /**
   * 탭 변경 핸들러.
   * @param id - 클릭된 탭 id
   */
  onTabChange:  (id: string) => void;
  /**
   * 탭 스타일 변형.
   * @default 'underline'
   */
  variant?:     TabNavVariant;
  /**
   * 탭 버튼이 컨테이너 전체 너비를 균등하게 채울지 여부.
   * true: 각 탭이 flex-1로 균등 분할 (Figma의 "full width" 탭 패턴)
   * false: 탭이 콘텐츠 너비만큼만 차지 (기본값)
   * @default false
   */
  fullWidth?:   boolean;
  className?:   string;
}
```

### TransactionList

```typescript
/** API 응답 flat 배열의 단일 거래 항목 */
export interface TransactionItem {
  id:       string;
  /** ISO 8601 형식. 예: '2026-03-26T14:30:00Z' */
  date:     string;
  title:    string;
  amount:   number;
  balance?: number;
  type:     'deposit' | 'withdrawal' | 'transfer';
}

/** 날짜별로 그룹핑된 거래 내역 (프론트엔드 변환 후 구조) */
export interface TransactionGroup {
  /** 표시용 날짜 문자열 (dateHeaderFormat에 따라 형식이 결정됨) */
  date:   string;
  items:  TransactionItem[];
}

/**
 * 날짜 그룹 헤더 표시 형식.
 * - 'month-day'      : 'MM월 DD일'       (기본값, 연도 생략)
 * - 'year-month-day' : 'YYYY년 MM월 DD일' (연도 포함)
 */
export type DateHeaderFormat = 'month-day' | 'year-month-day';

export type TransactionType = 'deposit' | 'withdrawal' | 'transfer';

export interface TransactionListItemProps {
  type:      TransactionType;
  title:     string;
  /** 표시용 날짜 문자열 */
  date:      string;
  /** 원화 단위 숫자. 컴포넌트 내부에서 Intl.NumberFormat으로 포맷 */
  amount:    number;
  /** 거래 후 잔액 */
  balance?:  number;
  onClick?:  () => void;
}

export interface TransactionListProps {
  /** API 응답 flat 배열. 컴포넌트 내부에서 날짜별로 그룹핑 */
  items:      TransactionItem[];
  /** 로딩 상태 */
  loading?:   boolean;
  /** 빈 목록 표시 메시지. 기본: '거래 내역이 없어요' */
  emptyMessage?: string;
  /**
   * 거래 항목 클릭 핸들러.
   * 전달 시 각 항목이 <button>으로 렌더링되며 hover/active 인터랙션이 활성화된다.
   * 예: 항목 클릭 → 거래 상세 바텀시트 오픈
   */
  onItemClick?: (item: TransactionItem) => void;
  /**
   * 날짜 그룹 헤더 표시 형식.
   * - 'month-day'      : 'MM월 DD일'       (기본값)
   * - 'year-month-day' : 'YYYY년 MM월 DD일'
   */
  dateHeaderFormat?: DateHeaderFormat;
  className?: string;
}
```

### TransactionSearchFilter

```typescript
/**
 * 퀵 기간 선택 옵션.
 * 선택 시 오늘 기준으로 startDate·endDate를 자동 계산한다.
 */
export type QuickPeriod = '1m' | '3m' | '6m' | '12m';

/**
 * 거래 내역 정렬 순서.
 * - recent: 최근순 (기본값)
 * - old: 과거순
 */
export type SortOrder = 'recent' | 'old';

/**
 * 거래 유형 필터.
 * - all: 전체 (기본값)
 * - deposit: 입금만
 * - withdrawal: 출금만
 */
export type TransactionType = 'all' | 'deposit' | 'withdrawal';

/** 조회 버튼 클릭 시 상위로 전달되는 검색 파라미터 */
export interface TransactionSearchParams {
  /** ISO 날짜 형식 'YYYY-MM-DD' */
  startDate: string;
  /** ISO 날짜 형식 'YYYY-MM-DD' */
  endDate: string;
  sortOrder: SortOrder;
  transactionType: TransactionType;
}

export interface TransactionSearchFilterProps {
  /** 현재 적용된 검색 조건. 초기값 및 외부 동기화에 사용 */
  value: TransactionSearchParams;
  /**
   * 조회 버튼 클릭 시 호출.
   * 폼 내부 상태(localParams)를 기준으로 호출되므로
   * 조회 전까지는 외부 value에 영향을 주지 않는다.
   */
  onSearch: (params: TransactionSearchParams) => void;
  /** 초기 펼침 여부. 기본: false (접힌 상태) */
  defaultExpanded?: boolean;
  className?: string;
}
```

### TransferForm

```typescript
export interface TransferFormData {
  /** 받는 계좌번호 */
  toAccount: string;
  /** 이체 금액 (원) */
  amount:    number;
  /** 메모 (선택) */
  memo:      string;
}

export interface TransferFormProps {
  /** 이체 가능 최대 금액 (내 계좌 잔액) */
  availableBalance?: number;
  /** 폼 제출 핸들러. 최종 확인 전에 호출됨 */
  onSubmit:          (data: TransferFormData) => void;
  /** 폼 제출 처리 중 여부 (버튼 로딩 상태) */
  submitting?:       boolean;
  className?:        string;
}
```

### TransferLimitInfo

```typescript
export interface TransferLimitInfoProps {
  /** 1회 이체 한도 (원). 예: 1000000 */
  perTransferLimit: number;
  /** 1일 이체 한도 (원). 예: 5000000 */
  dailyLimit: number;
  /** 오늘 이미 이체한 누적 금액 (원). 전달 시 잔여 한도 함께 표시 */
  usedAmount?: number;
}
```

## Biz (도메인 특화 컴포넌트 — 금융 비즈니스 로직 포함)

### biz/banking

#### AccountSelectorCard

```typescript
import React from 'react';

export interface AccountSelectorCardProps {
  /** 계좌명. 예: '하나 주거래 통장' */
  accountName: string;
  /** 계좌번호. 예: '123-456-789012'. 마스킹 없이 그대로 표시 */
  accountNumber: string;
  /**
   * 우측 원형 버튼 내 아이콘.
   * 미전달 시 WalletMinimal 아이콘을 기본으로 사용한다.
   */
  icon?: React.ReactNode;
  /**
   * 계좌명 영역 클릭 시 콜백.
   * 계좌 변경 드롭다운 혹은 BottomSheet를 열 때 사용한다.
   */
  onAccountChange?: () => void;
  /** 우측 원형 아이콘 버튼 클릭 시 콜백. 예: 계좌 상세 이동 */
  onIconClick?: () => void;
  /**
   * 우측 원형 아이콘 버튼의 aria-label.
   * 미전달 시 기본값 '계좌 상세' 사용.
   * 카드 상세·이체 등 다른 용도로 재사용 시 실제 기능에 맞게 전달한다.
   */
  iconAriaLabel?: string;
  /**
   * 출금가능금액 표시 문자열.
   * 전달 시 계좌번호 아래에 브랜드 색상으로 렌더링된다.
   * 예: '출금가능금액: 3,000,000원'
   */
  availableBalance?: string;
  className?: string;
}
```

#### AccountSummaryCard

```typescript
import React from 'react';

/**
 * 계좌 유형.
 * - deposit:       예금(입출금) 계좌
 * - savings:       적금·저축 계좌
 * - loan:          대출 계좌 — 금액 danger 색상, 기본 레이블 '대출잔액'
 * - foreignDeposit: 외화예금 계좌 — balanceDisplay로 외화 포맷 전달
 * - retirement:    퇴직연금 계좌 — 기본 레이블 '적립금'
 * - securities:    증권 계좌 — 기본 레이블 '평가금액'
 */
export type AccountType =
  | 'deposit'
  | 'savings'
  | 'loan'
  | 'foreignDeposit'
  | 'retirement'
  | 'securities';

export interface AccountSummaryCardProps {
  /** 계좌 유형. 유형별 금액 레이블·색상·배지 용도가 달라짐 */
  type:           AccountType;
  /** 예: '급여 통장', '청약저축' */
  accountName:    string;
  /** 예: '123-456-789012'. 화면에 마스킹 없이 그대로 표시 */
  accountNumber:  string;
  /** 원화 단위 숫자. 컴포넌트 내부에서 Intl.NumberFormat('ko-KR') 처리 */
  balance:        number;
  /**
   * 화면 표시용 잔액 문자열.
   * 전달 시 balance의 내부 포맷 변환을 건너뛰고 이 값을 그대로 표시.
   * 다중 통화(USD·EUR 등) 또는 Repository에서 이미 포맷한 경우에 사용.
   * (예: '$1,000.00', '총 $1,000.00 (약 1,350,000원)')
   */
  balanceDisplay?: string;
  /**
   * 금액 영역 레이블. 미전달 시 type별 기본값 사용.
   * - deposit: '잔액'
   * - savings: '납입금액'
   * - loan: '대출잔액'
   */
  balanceLabel?:  string;
  /**
   * 배지 텍스트. 미전달 시 배지 영역 렌더링 안 함.
   * - deposit: '주거래', '급여' 등
   * - savings: 'D-30' (만기일) 등
   * - loan: '변동금리', '고정금리' 등
   */
  badgeText?:     string;
  /**
   * 카드 우측 상단 더보기 버튼 종류.
   * - 'chevron': 다음 화면으로 이동하는 > 아이콘
   * - 'ellipsis': 추가 옵션 메뉴를 여는 ... 아이콘
   * 미전달 시 더보기 버튼 미노출.
   */
  moreButton?:    'chevron' | 'ellipsis';
  /** 더보기 버튼 클릭 핸들러. moreButton과 함께 전달해야 동작 */
  onMoreClick?:   () => void;
  onClick?:       () => void;
  /** 이체·내역 버튼 슬롯. 각 버튼은 균등 너비로 확장됨 */
  actions?:       React.ReactNode;
  className?:     string;
}
```

### biz/card

#### AccountSelectCard

```typescript
export interface AccountSelectCardProps {
  /** 은행명. 예: '하나은행' */
  bankName: string;
  /** 마스킹된 계좌번호. 예: '123-****-5678' */
  maskedAccount: string;
  /** 선택 여부 */
  isSelected: boolean;
  /** 클릭 핸들러 */
  onClick: () => void;
}
```

#### BillingPeriodLabel

```typescript
export interface BillingPeriodLabelProps {
  /** 이용기간 시작일. 예: '2025.03.01' */
  startDate:  string;
  /** 이용기간 종료일. 예: '2025.03.31' */
  endDate:    string;
  className?: string;
}
```

#### CardBenefitSummary

```typescript
/** 혜택 항목 단일 정보 */
export interface BenefitItem {
  /** 혜택 레이블. 예: '이번달 할인', '캐시백' */
  label: string;
  /** 혜택 금액 또는 포인트 (원 또는 P). 예: 12500 */
  amount: number;
  /** 단위 표시. 예: '원', 'P' (기본: '원') */
  unit?: string;
}

export interface CardBenefitSummaryProps {
  /** 보유 포인트 잔액 */
  points: number;
  /** 이번달 혜택 항목 목록 (할인·캐시백 등) */
  benefits: BenefitItem[];
  /** 포인트 상세 클릭 핸들러 */
  onPointDetail?: () => void;
  /** 혜택 상세 클릭 핸들러 */
  onBenefitDetail?: () => void;
}
```

#### CardChipItem

```typescript
export interface CardChipItemProps {
  /** 카드명. 예: '하나 머니 체크카드' */
  name: string;
  /** 마스킹된 카드번호. 예: '1234-****-****-5678' */
  maskedNumber: string;
}
```

#### CardInfoPanel

```typescript
export interface CardInfoRow {
  /** 좌측 레이블. 예: '결제 계좌', '결제일' */
  label: string;
  /**
   * 우측 값. '\n' 포함 시 줄바꿈으로 표시.
   * 예: '하나은행\n123456****1234'
   */
  value: string;
}

export interface CardInfoSection {
  /** 섹션 제목. 예: '결제정보', '카드 이용기간' */
  title: string;
  rows: CardInfoRow[];
}

export interface CardInfoPanelProps {
  sections: CardInfoSection[];
  className?: string;
}
```

#### CardLinkedBalance

```typescript
export interface CardLinkedBalanceProps {
  /** 연결계좌 잔액 (원) */
  balance:    number;
  /** true: 금액 마스킹 표시 */
  hidden:     boolean;
  /** 보기/숨기기 버튼 클릭 */
  onToggle:   () => void;
  className?: string;
}
```

#### CardManagementPanel

```typescript
/** 카드 관리 네비게이션 단일 행 데이터 */
export interface CardManagementNavRow {
  /** 행 레이블 */
  label:    string;
  /** 우측 보조 텍스트 (카드번호·계좌번호 등). 미전달 시 미노출 */
  subText?: string;
  /** 행 클릭 핸들러 */
  onClick?: () => void;
}

export interface CardManagementPanelProps {
  /** 네비게이션 행 목록. 순서대로 렌더링되며 개수 제한 없음 */
  rows:       CardManagementNavRow[];
  className?: string;
}
```

#### CardPaymentActions

```typescript
export interface CardPaymentActionsProps {
  /** 분할납부 버튼 클릭 핸들러 */
  onInstallment?: () => void;
  /** 즉시결제 버튼 클릭 핸들러 */
  onImmediatePayment?: () => void;
  /** 일부결제금액이월약정(리볼빙) 버튼 클릭 핸들러 */
  onRevolving?: () => void;
  className?: string;
}
```

#### CardPaymentItem

```typescript
import React from 'react';

export interface CardPaymentItemProps {
  /** 가맹점·서비스 아이콘 (lucide-react 등 React 노드) */
  icon:               React.ReactNode;
  /**
   * 아이콘 배경 Tailwind 클래스.
   * 미전달 시 기본 bg-brand-10 사용.
   * 예: 'bg-surface-subtle', 'bg-danger-surface'
   */
  iconBgClassName?:   string;
  /** 카드 영문명. 예: 'HANA MONEY CHECK' */
  cardEnName:         string;
  /** 카드 한글명 또는 가맹점명. 예: '하나 머니 체크카드' */
  cardName:           string;
  /** 결제 금액 (원). 양수: 결제, 음수: 취소/환불 */
  amount:             number;
  /** "상세보기" 버튼 클릭 핸들러. 미전달 시 버튼 미노출 */
  onDetailClick?:     () => void;
  /** 행 전체 클릭 핸들러 */
  onClick?:           () => void;
  className?:         string;
}
```

#### CardPaymentSummary

```typescript
export interface CardPaymentSummaryProps {
  /** 출금예정일. 예: '2026.04.08' */
  dateFull: string;
  /** 청구 년월. 예: '26년 4월' */
  dateYM: string;
  /** 오늘날짜. 예: '04.08' */
  dateMD: string;
  /** 총 청구금액 (원) */
  totalAmount: number;
  /** 리볼빙 금액 (원). 0이면 표시하지 않음 */
  revolving?: number;
  /** 카드론 금액 (원). 0이면 표시하지 않음 */
  cardLoan?: number;
  /** 현금서비스 금액 (원). 0이면 표시하지 않음 */
  cashAdvance?: number;
  /** 리볼빙(일부결제금액이월약정) 버튼 클릭 핸들러. 미전달 시 버튼 비활성 */
  onRevolving?: () => void;
  /** 카드론(장기카드대출) 버튼 클릭 핸들러. 미전달 시 버튼 비활성 */
  onCardLoan?: () => void;
  /** 현금서비스(단기카드대출) 버튼 클릭 핸들러. 미전달 시 버튼 비활성 */
  onCashAdvance?: () => void;
  /** 날짜(년월) 영역 클릭 핸들러. 전달 시 날짜 선택 모달 등을 열 수 있음 */
  onDateClick?: () => void;
  className?: string;
}
```

#### CardPerformanceBar

```typescript
export interface CardPerformanceBarProps {
  /** 카드명. 예: '하나 머니 체크카드' */
  cardName: string;
  /** 이번달 이용금액 (원) */
  currentAmount: number;
  /** 혜택 달성을 위한 목표 실적 금액 (원) */
  targetAmount: number;
  /** 목표 달성 시 제공되는 혜택 설명. 예: '전월 실적 달성 시 캐시백 1%' */
  benefitDescription?: string;
  /** 실적 상세 클릭 핸들러 */
  onDetail?: () => void;
}
```

#### CardPillTab

```typescript
export interface CardPillTabProps {
  /** 탭 레이블 (카드명 등) */
  label: string;
  /** 선택 여부 */
  isSelected: boolean;
  /** 클릭 핸들러 */
  onClick: () => void;
}
```

#### CardSummaryCard

```typescript
import React from 'react';

/**
 * 카드 유형.
 * - credit: 신용카드 — 사용금액 + 한도 표시, warning/danger 색상 분기
 * - check: 체크카드 — 연결 계좌 잔액 표시
 * - prepaid: 선불카드 — 충전 잔액 표시
 */
export type CardType = 'credit' | 'check' | 'prepaid';

export interface CardSummaryCardProps {
  /** 카드 유형. 유형별 레이블·색상이 달라짐 */
  type:             CardType;
  /** 카드 상품명. 예: '하나 머니 체크카드', '1Q카드' */
  cardName:         string;
  /**
   * 마스킹된 카드 번호. 예: '1234 **** **** 5678'
   * 컴포넌트는 그대로 표시하며 마스킹 처리는 호출자 책임
   */
  cardNumber:       string;
  /**
   * 기준 금액.
   * - credit: 당월 사용금액
   * - check/prepaid: 잔액
   */
  amount:           number;
  /**
   * 한도 금액 (credit 타입 전용).
   * 전달 시 "사용금액 / 한도" 형태로 표시하며 한도 대비 사용률 색상 분기.
   */
  limitAmount?:     number;
  /**
   * 배지 텍스트. 미전달 시 배지 미노출.
   * - credit: '포인트 적립', '캐시백' 등 혜택 유형
   * - check: '주거래' 등
   * - prepaid: '잔액 부족' 경고 배지 등
   */
  badgeText?:       string;
  onClick?:         () => void;
  /** 결제내역·충전 버튼 슬롯 */
  actions?:         React.ReactNode;
  className?:       string;
}
```

#### CardVisual

```typescript
import React from 'react';

export type CardBrand = 'VISA' | 'Mastercard' | 'AMEX' | 'JCB' | 'UnionPay';

export interface CardVisualProps {
  /** 카드 이미지 슬롯 (img 또는 SVG 컴포넌트) */
  cardImage:  React.ReactNode;
  /** 카드 브랜드 */
  brand:      CardBrand;
  /** 카드명. 예: '하나 머니 체크카드' */
  cardName:   string;
  /**
   * compact 모드.
   * true: 스크롤로 카드 영역을 벗어났을 때 스티키 헤더용 — 브랜드+카드명 한 줄 말줄임.
   * false(기본): 카드 이미지 + 브랜드 + 카드명 풀 레이아웃.
   */
  compact?:   boolean;
  className?: string;
}
```

#### LoanMenuBar

```typescript
import React from 'react'

export interface LoanMenuBarItem {
  id: string
  /** 메뉴 아이콘 (lucide-react ReactNode) */
  icon: React.ReactNode
  /** 메뉴 레이블 텍스트 */
  label: string
  /** 메뉴 클릭 핸들러 */
  onClick: () => void
}

export interface LoanMenuBarProps {
  /** 메뉴 항목 목록. 보통 단기카드대출 / 장기카드대출 / 리볼빙 3종 */
  items: LoanMenuBarItem[]
  className?: string
}
```

#### PaymentAccountCard

```typescript
import type React from 'react';

export interface PaymentAccountCardProps {
  /** 결제 계좌 명칭. 예: '하나은행 결제계좌' */
  title: string;
  /** 출금 가능 시간. 예: '365일 06:00~23:30' */
  hours: string;
  /** 당행/타행을 구분하는 아이콘 (lucide-react 컴포넌트) */
  icon: React.ReactNode;
}
```

#### QuickShortcutCard

```typescript
import React from 'react'

export interface QuickShortcutCardProps {
  /** 메인 타이틀 (e.g. "내 쿠폰", "카드 신청") */
  title: string
  /** 서브 텍스트 (e.g. "3장 사용가능", "맞춤형 추천") */
  subtitle: string
  /**
   * 우측 아이콘 슬롯 (lucide-react ReactNode)
   * 전달하지 않으면 아이콘 영역이 렌더링되지 않는다.
   */
  icon?: React.ReactNode
  /** 카드 클릭 핸들러 */
  onClick?: () => void
  className?: string
}
```

#### StatementHeroCard

```typescript
export interface StatementHeroCardProps {
  /** 원화 명세서 금액 (정수, 자동 포맷) */
  amount: number;
  /**
   * 결제일 표시 텍스트
   * @example "12월 25일"
   */
  dueDate: string;
  /**
   * 카드 상단 설명 레이블
   * @default "이번 달 명세서"
   */
  label?: string;
  /** 상세 화살표 클릭 핸들러 (전달 시 화살표 아이콘 노출) */
  onDetail?: () => void;
  /** true이면 금액을 마스킹 처리하여 숨긴다 */
  hidden?: boolean;
  className?: string;
}
```

#### StatementTotalCard

```typescript
export interface StatementTotalCardProps {
  /** 총 결제금액 (원) */
  amount: number;
  /**
   * 금액 상단 배지 텍스트.
   * - '예정': 결제 예정 상태
   * - 미전달: 배지 미노출
   */
  badge?: '예정';
  /** 금액 우측 화살표 클릭 → 이용내역 화면 이동 */
  onDetailClick?: () => void;
  /** 분할납부 버튼 클릭 핸들러 */
  onInstallment?: () => void;
  /** 즉시결제 버튼 클릭 핸들러 */
  onImmediatePayment?: () => void;
  /** 일부결제금액이월약정(리볼빙) 버튼 클릭 핸들러 */
  onRevolving?: () => void;
  className?: string;
}
```

#### SummaryCard

```typescript
import React from 'react';

/**
 * asset  — 총 자산 카드. 좌측 액센트 바 없음, 금액 강조 색상 브랜드
 * spending — 이번 달 소비 카드. 좌측 도메인 액센트 바 적용, 금액 기본 색상
 */
export type SummaryCardVariant = 'asset' | 'spending';

export interface SummaryCardAction {
  /** 버튼 레이블 */
  label: string;
  /** 버튼 클릭 핸들러 */
  onClick: () => void;
  /**
   * 활성 상태. true 시 도메인 액센트 배경(--domain-card-accent)으로 강조
   * @default false
   */
  active?: boolean;
}

export interface SummaryCardProps {
  /** 카드 유형 — 레이아웃과 색상에 영향 */
  variant: SummaryCardVariant;
  /** 한글 메인 제목 (e.g. "총 자산", "이번 달 소비") */
  title: string;
  /** 원화 금액 (정수, 자동 포맷) */
  amount: number;
  /** 우측 상단 아이콘 슬롯 */
  icon?: React.ReactNode;
  /** 하단 액션 버튼 목록 */
  actions?: SummaryCardAction[];
  /** 카드 전체 클릭 핸들러 */
  onClick?: () => void;
  /** true이면 금액을 마스킹 처리하여 숨긴다 */
  hidden?: boolean;
  className?: string;
}
```

#### UsageHistoryFilterSheet

```typescript
export interface CardOption {
  value: string;
  label: string;
}

export type ApprovalType = 'approved' | 'confirmed';
export type CardType     = 'all' | 'credit' | 'check';
export type RegionType   = 'all' | 'domestic' | 'overseas';
export type UsageType    = 'all' | 'lump' | 'installment' | 'cashAdvance' | 'cancel';
export type PeriodType   = 'thisMonth' | '1month' | '3months' | 'custom';

export interface SearchFilter {
  approval:      ApprovalType;
  cardType:      CardType;
  /** 선택된 카드 value. 'all' 이면 전체 */
  selectedCard:  string;
  region:        RegionType;
  usageType:     UsageType;
  period:        PeriodType;
  /** period === 'custom' 일 때 선택된 월. 예: '2026-03' */
  customMonth?:  string;
}

export interface UsageHistoryFilterSheetProps {
  open: boolean;
  onClose: () => void;
  cardOptions: CardOption[];
  /** 필터 확정 시 호출. filter.customMonth에 선택 월이 담긴다. */
  onApply: (filter: SearchFilter) => void;
}
```

#### UsageTransactionItem

```typescript
/** 가맹점 상세 정보 — 상세 BottomSheet 아코디언에 표시 */
export interface MerchantInfo {
  address?:      string;
  phone?:        string;
  businessType?: string;
}

/** 이용내역 단건 */
export interface Transaction {
  id: string;
  /** 사용처(가맹점명) */
  merchant: string;
  /** 결제 금액(원). 음수: 취소/환불 */
  amount: number;
  /** 거래일. 예: '2026.04.08' */
  date: string;
  /** 거래구분. 예: '일시불' | '할부(3개월)' | '단기카드대출' | '취소' */
  type: string;
  /** 승인번호 */
  approvalNumber: string;
  /** 거래상태. 예: '승인' | '결제확정' | '취소' */
  status: string;
  /** 이용카드명 */
  cardName: string;
  /** 가맹점 상세 정보 */
  merchantInfo?: MerchantInfo;
}

export interface UsageTransactionItemProps {
  tx: Transaction;
  /**
   * 전달 시 행이 버튼으로 렌더링되며 클릭 시 상세 BottomSheet를 노출한다.
   * 미전달 시 행은 div로 렌더링되고 상세 BottomSheet를 노출하지 않는다.
   */
  onClick?: () => void;
}
```

### biz/common

#### BannerCarousel

```typescript
import React from 'react';

export type BannerVariant = 'promo' | 'info' | 'warning';

export interface BannerCarouselItem {
  id:           string;
  /** 배너 색상 변형. 기본: 'promo' */
  variant?:     BannerVariant;
  title:        string;
  description?: string;
  /** 우측 CTA 슬롯 */
  action?:      React.ReactNode;
  /** 전달 시 닫기(×) 버튼 표시 */
  onClose?:     () => void;
}

export interface BannerCarouselProps {
  items:               BannerCarouselItem[];
  /**
   * 자동 재생 간격(ms). 기본: 3000.
   * items.length < 2이면 자동 재생 비활성화.
   */
  autoPlayInterval?:   number;
  className?:          string;
}
```

#### BrandBanner

```typescript
import React from 'react';

export interface BrandBannerProps {
  /**
   * 배너 소제목 — 주요 타이틀 위에 표시되는 작은 텍스트.
   * (예: '개인 맞춤 혜택')
   */
  subtitle?: string;
  /**
   * 배너 주요 타이틀.
   * (예: '나만을 위한 특별한 한아멤버스')
   */
  title: string;
  /**
   * 우측 아이콘 슬롯 — 흰색 반투명 원형 컨테이너 내부에 렌더링됨.
   * 미전달 시 아이콘 영역 미표시.
   */
  icon?: React.ReactNode;
  /**
   * 배너 클릭 핸들러.
   * 전달 시 button 태그로 렌더링되어 클릭 가능 배너가 됨.
   */
  onClick?: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

#### QuickMenuGrid

```typescript
import React from 'react';

export interface QuickMenuItem {
  id:      string;
  icon:    React.ReactNode;
  label:   string;
  onClick: () => void;
  /** 알림 배지 숫자. 0 또는 미전달 시 배지 미노출 */
  badge?:  number;
  /**
   * 아이콘 컨테이너 형태.
   * - 'circle'  : 원형 (기본값)
   * - 'rounded' : 각이 둥근 사각형
   */
  iconShape?: 'circle' | 'rounded';
}

export interface QuickMenuGridProps {
  items:      QuickMenuItem[];
  /**
   * 열 수. 기본: 4.
   * 아이템 수에 따라 4열(기본) 또는 3열 권장.
   */
  cols?:      2 | 3 | 4;
  className?: string;
}
```

#### UserProfile

```typescript
export interface UserProfileProps {
  /** 표시할 사용자 이름 (예: '김하나님') */
  name: string;
  /**
   * 최근 접속 일시 문자열 (예: '2023.11.01 10:30:15').
   * 미전달 시 최근 접속 행 미표시.
   */
  lastLogin?: string;
  /**
   * 내 정보 관리 클릭 핸들러.
   * onProfileManageClick 또는 onLogoutClick 중 하나라도 전달되면 설정 버튼이 표시된다.
   */
  onProfileManageClick?: () => void;
  /**
   * 로그아웃 클릭 핸들러.
   * onProfileManageClick 또는 onLogoutClick 중 하나라도 전달되면 설정 버튼이 표시된다.
   */
  onLogoutClick?: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
```

### biz/insurance

#### InsuranceSummaryCard

```typescript
import React from 'react';

/**
 * 보험 유형.
 * - life: 생명보험 — 사망·만기 보장
 * - health: 건강보험 — 실손·암 보장
 * - car: 자동차보험 — 차량 보장
 */
export type InsuranceType = 'life' | 'health' | 'car';

/**
 * 보험 계약 상태.
 * - active: 정상 유지 중
 * - pending: 납입 유예
 * - expired: 만기 또는 해지
 */
export type InsuranceStatus = 'active' | 'pending' | 'expired';

export interface InsuranceSummaryCardProps {
  /** 보험 유형. 유형별 아이콘·레이블이 달라짐 */
  type:              InsuranceType;
  /** 보험 상품명. 예: '하나생명 통합종신보험', '하나손해 운전자보험' */
  insuranceName:     string;
  /**
   * 계약 번호 또는 증권 번호.
   * 예: '2024-001-123456'
   */
  contractNumber:    string;
  /**
   * 월 납입 보험료 (원화 단위 숫자).
   * 내부에서 Intl.NumberFormat('ko-KR') 처리.
   */
  premium:           number;
  /**
   * 다음 납입일. 예: '2026.04.01'
   * 표시용 문자열로 직접 전달 (포맷은 호출자 책임)
   */
  nextPaymentDate?:  string;
  /** 계약 상태. 상태별 배지 색상이 달라짐 */
  status:            InsuranceStatus;
  /**
   * 배지 텍스트 override. 미전달 시 status 기반 기본 텍스트 사용.
   * - active: '정상'
   * - pending: '유예'
   * - expired: '만기'
   */
  badgeText?:        string;
  onClick?:          () => void;
  /** 보장 내역·청구 버튼 슬롯 */
  actions?:          React.ReactNode;
  className?:        string;
}
```
