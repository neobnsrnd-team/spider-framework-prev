/**
 * @file blocks.tsx
 * @description CMS BlockDefinition 목록.
 * component-library의 각 컴포넌트를 CMS 팔레트에 등록한다.
 *
 * - category: "core" | "biz" | "modules"
 * - domain: biz 컴포넌트의 하위 도메인 (common | banking | card | insurance)
 * - propSchema: CMS 인스펙터에서 편집 가능한 속성 정의
 */
/* eslint-disable @typescript-eslint/no-explicit-any */
// CMS 일반 prop 시스템(Record<string, unknown>)과 각 컴포넌트의 구체적인 Props 타입을
// 연결하는 브릿지 파일이므로 `as any` 캐스팅이 불가피하다.
import type { BlockDefinition } from "@cms-core";
import * as LucideIcons from "lucide-react";
import { MoreVertical } from "lucide-react";

/**
 * kebab-case 이름 → Lucide 컴포넌트 역방향 조회 맵.
 * PascalCase → kebab 변환 시 연속 대문자("AArrowDown" → "aarrow-down")가 손실되어
 * 역변환으로는 원본을 복원할 수 없으므로, 모듈 초기화 시 맵을 직접 빌드한다.
 * IconPicker와 동일한 변환식을 사용해 저장 키와 조회 키를 일치시킨다.
 */
// lucide-react 아이콘은 React.forwardRef() 반환값이므로 typeof가 "object"다.
// IconPicker와 동일한 조건(대문자 시작 + object + $$typeof 보유)으로 필터링한다.
const lucideIconMap: Record<string, React.ComponentType<React.SVGProps<SVGSVGElement>>> = Object.fromEntries(
  (Object.entries(LucideIcons) as [string, unknown][])
    .filter(([name, val]) =>
      /^[A-Z]/.test(name) &&
      typeof val === "object" &&
      val !== null &&
      "$$typeof" in (val as object)
    )
    .map(([name, val]) => [
      name.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase(),
      val,
    ])
);

/**
 * icon-picker가 반환하는 kebab-case 이름을 lucide-react 엘리먼트로 변환한다.
 * 이름이 비어있거나 존재하지 않으면 null을 반환한다.
 */
function resolveIcon(name: string, className = "size-5"): React.ReactNode {
  if (!name) return null;
  const Icon = lucideIconMap[name];
  return Icon ? <Icon className={className} /> : null;
}
import {
  // ── Core ──────────────────────────────────────────────────────────────────
  Badge,
  Button,
  Input,
  Select,
  Typography,

  // ── Biz / Common ──────────────────────────────────────────────────────────
  BannerCarousel,
  BrandBanner,
  QuickMenuGrid,
  UserProfile,

  // ── Biz / Banking ─────────────────────────────────────────────────────────
  AccountSelectorCard,
  AccountSummaryCard,

  // ── Biz / Card ────────────────────────────────────────────────────────────
  AccountSelectCard,
  BillingPeriodLabel,
  CardBenefitSummary,
  CardChipItem,
  CardInfoPanel,
  CardLinkedBalance,
  CardManagementPanel,
  CardPaymentActions,
  CardPaymentItem,
  CardPaymentSummary,
  CardPerformanceBar,
  CardPillTab,
  CardSummaryCard,
  CardVisual,
  LoanMenuBar,
  PaymentAccountCard,
  QuickShortcutCard,
  StatementHeroCard,
  StatementTotalCard,
  SummaryCard,
  UsageTransactionItem,

  // ── Biz / Insurance ───────────────────────────────────────────────────────
  InsuranceSummaryCard,

  // ── Modules / Common ──────────────────────────────────────────────────────
  ActionLinkItem,
  AlertBanner,
  BalanceToggle,
  BankSelectGrid,
  Card,
  Checkbox,
  CollapsibleSection,
  DatePicker,
  Divider,
  DividerWithLabel,
  DropdownMenu,
  EmptyState,
  ErrorState,
  InfoRow,
  LabelValueRow,
  NoticeItem,
  RecentRecipientItem,
  SectionHeader,
  SelectableItem,
  SelectableListItem,
  SidebarNav,
  StepIndicator,
  SuccessHero,
  TabNav,
  TransferLimitInfo,

  // ── Modules / Banking ─────────────────────────────────────────────────────
  AccountSelectItem,
  AmountInput,
  NumberKeypad,
  OtpInput,
  PinDotIndicator,
  TransactionList,
  TransactionSearchFilter,
  TransferForm,
} from "@cl";

// ═════════════════════════════════════════════════════════════════════════════
// Core
// ═════════════════════════════════════════════════════════════════════════════

const BadgeDefinition: BlockDefinition = {
  meta: {
    name: "Badge",
    category: "core",
    defaultProps: { variant: "brand", children: "배지", dot: false },
    propSchema: {
      children: { type: "string",  label: "텍스트",    default: "배지" },
      variant:  { type: "select",  label: "색상 변형", default: "brand", options: ["primary", "brand", "success", "danger", "warning", "neutral"] },
      dot:      { type: "boolean", label: "점(dot) 표시", default: false },
    },
  },
  component: (p) => <Badge {...(p as any)} />,
};

const ButtonDefinition: BlockDefinition = {
  meta: {
    name: "Button",
    category: "core",
    defaultProps: { variant: "primary", size: "md", children: "버튼", fullWidth: false, loading: false, disabled: false },
    propSchema: {
      children:  { type: "string",  label: "버튼 텍스트", default: "버튼" },
      variant:   { type: "select",  label: "변형",       default: "primary", options: ["primary", "outline", "ghost", "danger"] },
      size:      { type: "select",  label: "크기",       default: "md",      options: ["sm", "md", "lg"] },
      fullWidth: { type: "boolean", label: "전체 너비 (w-full)", default: false },
      loading:   { type: "boolean", label: "로딩 중",            default: false },
      disabled:  { type: "boolean", label: "비활성화",           default: false },
      onClick:   { type: "event",   label: "클릭" },
    },
  },
  component: (p) => <Button {...(p as any)} />,
};

const InputDefinition: BlockDefinition = {
  meta: {
    name: "Input",
    category: "core",
    defaultProps: { label: "레이블", placeholder: "입력하세요", helperText: "", validationState: "default", size: "md", fullWidth: false, disabled: false },
    propSchema: {
      label:           { type: "string",  label: "레이블",        default: "레이블" },
      placeholder:     { type: "string",  label: "플레이스홀더",  default: "입력하세요" },
      helperText:      { type: "string",  label: "도움말 텍스트", default: "" },
      validationState: { type: "select",  label: "검증 상태",     default: "default", options: ["default", "error", "success"] },
      size:            { type: "select",  label: "크기",          default: "md",      options: ["md", "lg"] },
      fullWidth:       { type: "boolean", label: "전체 너비",     default: false },
      disabled:        { type: "boolean", label: "비활성화",      default: false },
      onChange:        { type: "event",   label: "값 변경" },
    },
  },
  component: (p) => <Input {...(p as any)} />,
};

const SelectDefinition: BlockDefinition = {
  meta: {
    name: "Select",
    category: "core",
    defaultProps: { value: "all", options: [{ value: "all", label: "전체" }, { value: "deposit", label: "입금" }, { value: "withdraw", label: "출금" }] },
    propSchema: {
      value:        { type: "string", label: "선택된 값", default: "all" },
      options:      { type: "array",  label: "옵션 목록", default: [{ value: "all", label: "전체" }, { value: "deposit", label: "입금" }, { value: "withdraw", label: "출금" }], itemFields: { value: { type: "string", label: "값", default: "" }, label: { type: "string", label: "표시 텍스트", default: "" } } },
      onChange:     { type: "event",  label: "값 변경" },
    },
  },
  component: (p) => <Select {...(p as any)} />,
};

const TypographyDefinition: BlockDefinition = {
  meta: {
    name: "Typography",
    category: "core",
    defaultProps: { children: "텍스트", variant: "body", weight: "normal", color: "base" },
    propSchema: {
      children: { type: "string", label: "텍스트", default: "텍스트" },
      variant:  { type: "select", label: "변형",   default: "body",   options: ["heading", "subheading", "body-lg", "body", "body-sm", "caption"] },
      weight:   { type: "select", label: "굵기",   default: "normal", options: ["normal", "medium", "bold"] },
      color:    { type: "select", label: "색상",   default: "base",   options: ["heading", "base", "label", "secondary", "muted", "brand", "danger", "success"] },
    },
  },
  component: (p) => <Typography {...(p as any)} />,
};

// ═════════════════════════════════════════════════════════════════════════════
// Biz / Common
// ═════════════════════════════════════════════════════════════════════════════

const BannerCarouselDefinition: BlockDefinition = {
  meta: {
    name: "BannerCarousel",
    category: "biz",
    domain: "common",
    defaultProps: { items: [{ id: "1", variant: "promo", title: "배너 제목 1", description: "배너 설명 1" }, { id: "2", variant: "info", title: "배너 제목 2", description: "배너 설명 2" }] },
    propSchema: {
      items: {
        type: "array", label: "배너 항목",
        default: [{ id: "1", variant: "promo", title: "배너 제목 1", description: "배너 설명 1" }, { id: "2", variant: "info", title: "배너 제목 2", description: "배너 설명 2" }],
        itemFields: {
          id:          { type: "string", label: "ID",   default: "" },
          variant:     { type: "select", label: "변형", default: "promo", options: ["promo", "info", "warning"] },
          title:       { type: "string", label: "제목", default: "" },
          description: { type: "string", label: "설명", default: "" },
        },
      },
    },
  },
  component: (p) => <BannerCarousel {...(p as any)} />,
};

const BrandBannerDefinition: BlockDefinition = {
  meta: {
    name: "BrandBanner",
    category: "biz",
    domain: "common",
    defaultProps: { title: "브랜드 타이틀", subtitle: "서브 타이틀", icon: "" },
    propSchema: {
      icon:     { type: "icon-picker", label: "아이콘",     default: "" },
      title:    { type: "string",      label: "타이틀",     default: "브랜드 타이틀" },
      subtitle: { type: "string",      label: "서브 타이틀", default: "서브 타이틀" },
      onClick:  { type: "event",       label: "클릭" },
    },
  },
  // BrandBanner 아이콘은 브랜드 컬러 배경 위에 올라가므로 text-white가 필요하다.
  component: (p) => <BrandBanner {...(p as any)} icon={resolveIcon((p as any).icon, "size-5 text-white")} />,
};

const QuickMenuGridDefinition: BlockDefinition = {
  meta: {
    name: "QuickMenuGrid",
    category: "biz",
    domain: "common",
    defaultProps: {
      cols: "4",
      items: [
        { id: "1", icon: "credit-card", label: "카드 결제", badge: 0, iconShape: "circle" },
        { id: "2", icon: "landmark",    label: "계좌 이체", badge: 0, iconShape: "circle" },
        { id: "3", icon: "wallet",      label: "내 지갑",   badge: 0, iconShape: "circle" },
        { id: "4", icon: "refresh-cw",  label: "환전",      badge: 0, iconShape: "circle" },
      ],
    },
    propSchema: {
      cols:  { type: "select", label: "열 수", default: "4", options: ["2", "3", "4"] },
      items: {
        type: "array", label: "메뉴 항목",
        default: [{ id: "1", icon: "credit-card", label: "카드 결제", badge: 0, iconShape: "circle" }],
        itemFields: {
          id:        { type: "string",      label: "ID",         default: "" },
          icon:      { type: "icon-picker", label: "아이콘",     default: "circle-dot" },
          label:     { type: "string",      label: "레이블",     default: "" },
          badge:     { type: "number",      label: "배지 숫자",  default: 0 },
          iconShape: { type: "select",      label: "아이콘 형태", default: "circle", options: ["circle", "rounded"] },
        },
      },
    },
  },
  // cols select는 문자열을 반환하므로 숫자로 변환해 전달한다.
  // items[].icon은 kebab-case 문자열이므로 ReactNode로 변환하고, onClick은 CMS 미리보기용 noop을 주입한다.
  component: (p) => {
    type RawItem = { id: string; icon?: string; label: string; badge?: number; iconShape?: 'circle' | 'rounded' };
    const rawItems = ((p as any).items ?? []) as RawItem[];
    const items = rawItems.map((item) => ({
      ...item,
      icon: resolveIcon(item.icon ?? "", "size-5"),
      onClick: () => {},
    }));
    return <QuickMenuGrid {...(p as any)} cols={Number((p as any).cols) as 2 | 3 | 4} items={items} />;
  },
};

const UserProfileDefinition: BlockDefinition = {
  meta: {
    name: "UserProfile",
    category: "biz",
    domain: "common",
    defaultProps: { name: "홍길동", lastLogin: "2026.04.17 09:30" },
    propSchema: {
      name:                  { type: "string", label: "이름",         default: "홍길동" },
      lastLogin:             { type: "string", label: "마지막 로그인", default: "" },
      onProfileManageClick:  { type: "event",  label: "내 정보 관리 클릭" },
      onLogoutClick:         { type: "event",  label: "로그아웃 클릭" },
    },
  },
  // 캔버스에서는 이벤트 핸들러가 전달되지 않아 설정 버튼이 숨겨지므로
  // noop을 기본값으로 주입해 미리보기에서도 버튼이 노출되도록 한다.
  component: (p) => (
    <UserProfile
      {...(p as any)}
      onProfileManageClick={(p as any).onProfileManageClick ?? (() => {})}
      onLogoutClick={(p as any).onLogoutClick ?? (() => {})}
    />
  ),
};

// ═════════════════════════════════════════════════════════════════════════════
// Biz / Banking
// ═════════════════════════════════════════════════════════════════════════════

const AccountSelectorCardDefinition: BlockDefinition = {
  meta: {
    name: "AccountSelectorCard",
    category: "biz",
    domain: "banking",
    defaultProps: { accountName: "하나 자유적금", accountNumber: "123-456789-01011", availableBalance: "1,234,567원", icon: "" },
    propSchema: {
      icon:             { type: "icon-picker", label: "아이콘 (미선택 시 WalletMinimal)", default: "" },
      accountName:      { type: "string",      label: "계좌명",          default: "하나 자유적금" },
      accountNumber:    { type: "string",      label: "계좌번호",         default: "123-456789-01011" },
      availableBalance: { type: "string",      label: "출금 가능 금액",   default: "1,234,567원" },
      onAccountChange:  { type: "event",       label: "계좌 변경 클릭" },
      onIconClick:      { type: "event",       label: "아이콘 클릭" },
    },
  },
  // 캔버스에서는 onAccountChange가 전달되지 않아 계좌명 옆 ChevronDown이 숨겨지므로 noop을 주입한다.
  component: (p) => (
    <AccountSelectorCard
      {...(p as any)}
      icon={resolveIcon((p as any).icon, "size-5") ?? undefined}
      onAccountChange={(p as any).onAccountChange ?? (() => {})}
    />
  ),
};

const AccountSummaryCardDefinition: BlockDefinition = {
  meta: {
    name: "AccountSummaryCard",
    category: "biz",
    domain: "banking",
    defaultProps: {
      type: "deposit", accountName: "하나 자유입출금", accountNumber: "123-456789-01011",
      balance: 1234567, balanceLabel: "잔액", badgeText: "", moreButton: "chevron",
      actionButtons: [{ label: "이체", variant: "outline" }, { label: "내역", variant: "primary" }],
    },
    propSchema: {
      type:         { type: "select", label: "계좌 유형",      default: "deposit", options: ["deposit", "savings", "loan", "foreignDeposit", "retirement", "securities"] },
      accountName:  { type: "string", label: "계좌명",          default: "하나 자유입출금" },
      accountNumber: { type: "string", label: "계좌번호",       default: "123-456789-01011" },
      balance:      { type: "number", label: "잔액 (숫자)",     default: 0 },
      balanceLabel: { type: "string", label: "잔액 레이블",     default: "잔액" },
      badgeText:    { type: "string", label: "배지 텍스트",     default: "" },
      moreButton:   { type: "select", label: "더보기 버튼", default: "chevron", options: ["chevron", "ellipsis", "undefined"] },
      actionButtons: {
        type: "array", label: "하단 버튼",
        default: [{ label: "이체", variant: "outline" }, { label: "내역", variant: "primary" }],
        itemFields: {
          label:   { type: "string", label: "버튼 텍스트", default: "" },
          variant: { type: "select", label: "변형",       default: "outline", options: ["primary", "outline"] },
        },
      },
      onMoreClick: { type: "event", label: "더보기 클릭" },
      onClick:     { type: "event", label: "카드 클릭" },
    },
  },
  // actionButtons 배열을 Button 컴포넌트 목록으로 변환해 actions 슬롯에 주입한다.
  // moreButton "undefined" 문자열은 실제 undefined로 변환해 버튼을 숨긴다.
  component: (p) => {
    type ActionBtn = { label: string; variant: string };
    const rawBtns = ((p as any).actionButtons ?? []) as ActionBtn[];
    const actions = rawBtns.length > 0
      ? <>{rawBtns.map((btn, i) => <Button key={i} size="sm" variant={btn.variant as any}>{btn.label}</Button>)}</>
      : undefined;
    const moreButton = (p as any).moreButton === "undefined" ? undefined : (p as any).moreButton;
    return <AccountSummaryCard {...(p as any)} moreButton={moreButton} actions={actions} />;
  },
};

// ═════════════════════════════════════════════════════════════════════════════
// Biz / Card
// ═════════════════════════════════════════════════════════════════════════════

const AccountSelectCardDefinition: BlockDefinition = {
  meta: {
    name: "AccountSelectCard",
    category: "biz",
    domain: "card",
    defaultProps: { bankName: "하나은행", maskedAccount: "123-****-01011", isSelected: false },
    propSchema: {
      bankName:      { type: "string",  label: "은행명",       default: "하나은행" },
      maskedAccount: { type: "string",  label: "마스킹 계좌번호", default: "123-****-01011" },
      isSelected:    { type: "boolean", label: "선택 여부",    default: false },
      onClick:       { type: "event",   label: "클릭" },
    },
  },
  component: (p) => <AccountSelectCard {...(p as any)} />,
};

const BillingPeriodLabelDefinition: BlockDefinition = {
  meta: {
    name: "BillingPeriodLabel",
    category: "biz",
    domain: "card",
    defaultProps: { startDate: "2026.03.16", endDate: "2026.04.15" },
    propSchema: {
      startDate: { type: "string", label: "시작일 (예: 2026.03.16)", default: "2026.03.16" },
      endDate:   { type: "string", label: "종료일 (예: 2026.04.15)", default: "2026.04.15" },
    },
  },
  component: (p) => <BillingPeriodLabel {...(p as any)} />,
};

const CardBenefitSummaryDefinition: BlockDefinition = {
  meta: {
    name: "CardBenefitSummary",
    category: "biz",
    domain: "card",
    defaultProps: {
      points: 12500,
      benefits: [
        { label: "이번달 할인", amount: 5000, unit: "원" },
        { label: "캐시백",     amount: 7500, unit: "원" },
      ],
    },
    propSchema: {
      points: { type: "number", label: "포인트", default: 0 },
      benefits: {
        type: "array", label: "혜택 항목",
        default: [{ label: "이번달 할인", amount: 5000, unit: "원" }],
        itemFields: {
          label:  { type: "string", label: "혜택 레이블", default: "" },
          amount: { type: "number", label: "금액",        default: 0 },
          unit:   { type: "string", label: "단위 (원/P)", default: "원" },
        },
      },
      onPointDetail:   { type: "event", label: "포인트 상세 클릭" },
      onBenefitDetail: { type: "event", label: "혜택 상세 클릭" },
    },
  },
  component: (p) => <CardBenefitSummary {...(p as any)} />,
};

const CardChipItemDefinition: BlockDefinition = {
  meta: {
    name: "CardChipItem",
    category: "biz",
    domain: "card",
    defaultProps: { name: "하나카드", maskedNumber: "1234" },
    propSchema: {
      name:         { type: "string", label: "카드명",       default: "하나카드" },
      maskedNumber: { type: "string", label: "마스킹 카드번호", default: "1234" },
    },
  },
  component: (p) => <CardChipItem {...(p as any)} />,
};

const CardInfoPanelDefinition: BlockDefinition = {
  meta: {
    name: "CardInfoPanel",
    category: "biz",
    domain: "card",
    defaultProps: {
      sections: [
        {
          title: "결제정보",
          rows: [
            { label: "결제 계좌", value: "하나은행 123-****-1234" },
            { label: "결제일",   value: "매월 15일" },
          ],
        },
        {
          title: "이용한도",
          rows: [
            { label: "총 한도",   value: "500만원" },
            { label: "잔여 한도", value: "320만원" },
          ],
        },
      ],
    },
    propSchema: {
      sections: {
        type: "array", label: "섹션 목록",
        default: [{ title: "결제정보", rows: [] }, { title: "이용한도", rows: [] }],
        itemFields: {
          title: { type: "string", label: "섹션 제목", default: "" },
          rows: {
            type: "array", label: "행 목록",
            default: [],
            itemFields: {
              label: { type: "string", label: "레이블", default: "" },
              value: { type: "string", label: "값",    default: "" },
            },
          },
        },
      },
    },
  },
  component: (p) => <CardInfoPanel {...(p as any)} />,
};

const CardLinkedBalanceDefinition: BlockDefinition = {
  meta: {
    name: "CardLinkedBalance",
    category: "biz",
    domain: "card",
    defaultProps: { balance: 1234567, hidden: false },
    propSchema: {
      balance:  { type: "number",  label: "연결 계좌 잔액 (원화 숫자)", default: 0 },
      hidden:   { type: "boolean", label: "잔액 숨김 여부",             default: false },
      onToggle: { type: "event",   label: "잔액 표시/숨김 토글" },
    },
  },
  component: (p) => <CardLinkedBalance {...(p as any)} />,
};

const CardManagementPanelDefinition: BlockDefinition = {
  meta: {
    name: "CardManagementPanel",
    category: "biz",
    domain: "card",
    defaultProps: {
      rows: [
        { label: "카드정보 확인", subText: "1234 **** **** 5678" },
        { label: "결제계좌", subText: "하나은행 123-****-5678" },
        { label: "카드 비밀번호 설정" },
        { label: "해외 결제 신청" },
      ],
    },
    propSchema: {
      rows: {
        type: "array", label: "관리 항목",
        default: [{ label: "카드정보 확인", subText: "1234 **** **** 5678" }, { label: "결제계좌", subText: "하나은행 123-****-5678" }],
        itemFields: {
          label:   { type: "string", label: "레이블",     default: "" },
          subText: { type: "string", label: "보조 텍스트", default: "" },
        },
      },
    },
  },
  component: (p) => <CardManagementPanel {...(p as any)} />,
};

const CardPaymentActionsDefinition: BlockDefinition = {
  meta: {
    name: "CardPaymentActions",
    category: "biz",
    domain: "card",
    defaultProps: {},
    propSchema: {
      onInstallment:      { type: "event", label: "할부 조회 클릭" },
      onImmediatePayment: { type: "event", label: "즉시결제 클릭" },
      onRevolving:        { type: "event", label: "리볼빙 클릭" },
    },
  },
  component: (p) => <CardPaymentActions {...(p as any)} />,
};

const CardPaymentItemDefinition: BlockDefinition = {
  meta: {
    name: "CardPaymentItem",
    category: "biz",
    domain: "card",
    defaultProps: { icon: "credit-card", cardEnName: "HANA CARD", cardName: "하나카드", amount: 150000 },
    propSchema: {
      icon:          { type: "icon-picker", label: "아이콘",        default: "credit-card" },
      cardEnName:    { type: "string",      label: "카드 영문명",    default: "HANA CARD" },
      cardName:      { type: "string",      label: "카드명",         default: "하나카드" },
      amount:        { type: "number",      label: "결제 금액 (원)", default: 0 },
      onDetailClick: { type: "event",       label: "상세 클릭" },
      onClick:       { type: "event",       label: "카드 클릭" },
    },
  },
  component: (p) => (
    <CardPaymentItem
      {...(p as any)}
      icon={resolveIcon((p as any).icon)}
      onDetailClick={() => {}}
    />
  ),
};

const CardPaymentSummaryDefinition: BlockDefinition = {
  meta: {
    name: "CardPaymentSummary",
    category: "biz",
    domain: "card",
    defaultProps: { dateFull: "2026.04.15", dateYM: "26년 4월", dateMD: "04.15", totalAmount: 350000, revolving: 0, cardLoan: 0, cashAdvance: 0, hideDateButton: false },
    propSchema: {
      dateFull:       { type: "string",  label: "출금예정일 (예: 2026.04.15)", default: "2026.04.15" },
      dateYM:         { type: "string",  label: "청구 년월 (예: 26년 4월)",    default: "26년 4월" },
      dateMD:         { type: "string",  label: "월일 (예: 04.15)",            default: "04.15" },
      totalAmount:    { type: "number",  label: "총 결제금액 (원)",             default: 0 },
      revolving:      { type: "number",  label: "리볼빙 금액 (원)",             default: 0 },
      cardLoan:       { type: "number",  label: "카드론 금액 (원)",             default: 0 },
      cashAdvance:    { type: "number",  label: "현금서비스 금액 (원)",         default: 0 },
      hideDateButton: { type: "boolean", label: "날짜 선택 버튼 숨김",          default: false },
      onRevolving:    { type: "event",   label: "리볼빙 클릭" },
      onCardLoan:     { type: "event",   label: "카드론 클릭" },
      onCashAdvance:  { type: "event",   label: "현금서비스 클릭" },
      onDateClick:    { type: "event",   label: "날짜 클릭" },
    },
  },
  component: (p) => <CardPaymentSummary {...(p as any)} />,
};

const CardPerformanceBarDefinition: BlockDefinition = {
  meta: {
    name: "CardPerformanceBar",
    category: "biz",
    domain: "card",
    defaultProps: { cardName: "하나카드", currentAmount: 350000, targetAmount: 500000, benefitDescription: "캐시백 1% 달성까지" },
    propSchema: {
      cardName:           { type: "string", label: "카드명",              default: "하나카드" },
      currentAmount:      { type: "number", label: "현재 금액 (원)",       default: 0 },
      targetAmount:       { type: "number", label: "목표 금액 (원)",       default: 0 },
      benefitDescription: { type: "string", label: "혜택 설명",            default: "" },
      onDetail:           { type: "event",  label: "상세 클릭" },
    },
  },
  component: (p) => <CardPerformanceBar {...(p as any)} />,
};

const CardPillTabDefinition: BlockDefinition = {
  meta: {
    name: "CardPillTab",
    category: "biz",
    domain: "card",
    defaultProps: { label: "하나 머니 체크카드", isSelected: false },
    propSchema: {
      label:      { type: "string",  label: "탭 레이블", default: "하나 머니 체크카드" },
      isSelected: { type: "boolean", label: "선택 여부", default: false },
      onClick:    { type: "event",   label: "클릭" },
    },
  },
  component: (p) => <CardPillTab {...(p as any)} />,
};

const CardSummaryCardDefinition: BlockDefinition = {
  meta: {
    name: "CardSummaryCard",
    category: "biz",
    domain: "card",
    defaultProps: {
      type: "credit", cardName: "하나카드", cardNumber: "**** **** **** 1234",
      amount: 350000, limitAmount: 3000000, badgeText: "",
      actionButtons: [{ label: "결제내역", variant: "outline" }],
    },
    propSchema: {
      type:        { type: "select",  label: "카드 유형",     default: "credit", options: ["credit", "check", "prepaid"] },
      cardName:    { type: "string",  label: "카드명",        default: "하나카드" },
      cardNumber:  { type: "string",  label: "마스킹 카드번호", default: "**** **** **** 1234" },
      amount:      { type: "number",  label: "이용 금액 (원)", default: 0 },
      limitAmount: { type: "number",  label: "한도 금액 (원)", default: 0 },
      badgeText:   { type: "string",  label: "배지 텍스트",   default: "" },
      actionButtons: {
        type: "array", label: "하단 버튼",
        default: [{ label: "결제내역", variant: "outline" }],
        itemFields: {
          label:   { type: "string", label: "버튼 텍스트", default: "" },
          variant: { type: "select", label: "변형",       default: "outline", options: ["primary", "outline"] },
        },
      },
      onClick:     { type: "event",   label: "카드 클릭" },
    },
  },
  // actionButtons 배열을 Button 컴포넌트 목록으로 변환해 actions 슬롯에 주입한다.
  component: (p) => {
    type ActionBtn = { label: string; variant: string };
    const rawBtns = ((p as any).actionButtons ?? []) as ActionBtn[];
    const actions = rawBtns.length > 0
      ? <>{rawBtns.map((btn, i) => <Button key={i} size="sm" variant={btn.variant as any}>{btn.label}</Button>)}</>
      : undefined;
    return <CardSummaryCard {...(p as any)} actions={actions} />;
  },
};

/** 브랜드별 플레이스홀더 그라데이션 — 실제 카드 이미지가 없을 때 CMS 미리보기용 */
const CARD_BRAND_GRADIENTS: Record<string, [string, string]> = {
  VISA:       ['#1A1F71', '#F7A823'], // 네이비 → 골드
  Mastercard: ['#EB001B', '#F79E1B'], // 레드 → 오렌지
  AMEX:       ['#007B5E', '#00B4D8'], // 그린 → 스카이블루
  JCB:        ['#003087', '#009F6B'], // 블루 → 그린
  UnionPay:   ['#C0392B', '#922B21'], // 레드 → 다크레드
};

const CardVisualDefinition: BlockDefinition = {
  meta: {
    name: "CardVisual",
    category: "biz",
    domain: "card",
    defaultProps: { brand: "VISA", cardName: "하나 머니 체크카드", compact: false },
    propSchema: {
      brand:    { type: "select",  label: "카드 브랜드", default: "VISA", options: ["VISA", "Mastercard", "AMEX", "JCB", "UnionPay"] },
      cardName: { type: "string",  label: "카드명",      default: "하나 머니 체크카드" },
      compact:  { type: "boolean", label: "소형 표시",   default: false },
    },
  },
  // cardImage는 ReactNode라 CMS에서 직접 편집 불가 — 브랜드별 그라데이션 플레이스홀더를 주입한다.
  component: (p) => {
    const brand = (p as any).brand as string;
    const [from, to] = CARD_BRAND_GRADIENTS[brand] ?? ['#555', '#888'];
    const cardImage = (
      <div
        style={{ background: `linear-gradient(135deg, ${from}, ${to})`, width: '100%', height: '100%' }}
        className="flex items-end p-md"
      >
        <span className="text-white font-bold text-lg">{(p as any).cardName}</span>
      </div>
    );
    return <CardVisual {...(p as any)} cardImage={cardImage} />;
  },
};

const LoanMenuBarDefinition: BlockDefinition = {
  meta: {
    name: "LoanMenuBar",
    category: "biz",
    domain: "card",
    defaultProps: {
      items: [
        { label: "단기카드대출", icon: "credit-card" },
        { label: "장기카드대출", icon: "banknote" },
        { label: "리볼빙",       icon: "refresh-cw" },
      ],
    },
    propSchema: {
      items: {
        type: "array", label: "메뉴 항목",
        default: [
          { label: "단기카드대출", icon: "credit-card" },
          { label: "장기카드대출", icon: "banknote" },
          { label: "리볼빙",       icon: "refresh-cw" },
        ],
        itemFields: {
          label: { type: "string",      label: "레이블", default: "" },
          icon:  { type: "icon-picker", label: "아이콘", default: "credit-card" },
        },
      },
    },
  },
  // items[].icon은 kebab-case 아이콘 이름 → ReactNode로 변환 후 주입한다.
  // 스토리북 기준 아이콘 크기 14px → Tailwind size-3.5
  component: (p) => {
    type RawItem = { label: string; icon: string };
    const rawItems = ((p as any).items ?? []) as RawItem[];
    const items = rawItems.map((item, i) => ({
      id: String(i),
      label: item.label,
      icon: resolveIcon(item.icon, "size-3.5"),
      onClick: () => {},
    }));
    return <LoanMenuBar items={items} />;
  },
};

const PaymentAccountCardDefinition: BlockDefinition = {
  meta: {
    name: "PaymentAccountCard",
    category: "biz",
    domain: "card",
    defaultProps: { title: "결제 계좌", hours: "09:00~18:00", icon: "landmark" },
    propSchema: {
      title: { type: "string",      label: "제목",     default: "결제 계좌" },
      hours: { type: "string",      label: "운영 시간", default: "09:00~18:00" },
      icon:  { type: "icon-picker", label: "아이콘",   default: "landmark" },
    },
  },
  component: (p) => (
    <PaymentAccountCard
      {...(p as any)}
      icon={resolveIcon((p as any).icon ?? "landmark")}
    />
  ),
};

const QuickShortcutCardDefinition: BlockDefinition = {
  meta: {
    name: "QuickShortcutCard",
    category: "biz",
    domain: "card",
    defaultProps: { title: "내 쿠폰", subtitle: "3장 사용가능", icon: "ticket" },
    propSchema: {
      title:    { type: "string",      label: "제목",   default: "내 쿠폰" },
      subtitle: { type: "string",      label: "부제목", default: "3장 사용가능" },
      icon:     { type: "icon-picker", label: "아이콘", default: "ticket" },
      onClick:  { type: "event",       label: "클릭" },
    },
  },
  component: (p) => (
    <QuickShortcutCard {...(p as any)} icon={resolveIcon((p as any).icon ?? "ticket")} />
  ),
};

const StatementHeroCardDefinition: BlockDefinition = {
  meta: {
    name: "StatementHeroCard",
    category: "biz",
    domain: "card",
    defaultProps: { amount: 350000, dueDate: "2026.04.15", label: "이번 달 결제 예정", hidden: false },
    propSchema: {
      amount:   { type: "number",  label: "결제 금액 (원)", default: 0 },
      dueDate:  { type: "string",  label: "결제 예정일",   default: "" },
      label:    { type: "string",  label: "레이블",        default: "이번 달 결제 예정" },
      hidden:   { type: "boolean", label: "금액 마스킹",   default: false },
      onDetail: { type: "event",   label: "상세 클릭" },
    },
  },
  component: (p) => <StatementHeroCard {...(p as any)} />,
};

const StatementTotalCardDefinition: BlockDefinition = {
  meta: {
    name: "StatementTotalCard",
    category: "biz",
    domain: "card",
    defaultProps: { amount: 350000, badge: "없음" },
    propSchema: {
      amount:            { type: "number", label: "총 결제금액 (원)", default: 0 },
      badge:             { type: "select", label: "배지",            default: "없음", options: ["없음", "예정"] },
      onDetailClick:     { type: "event",  label: "상세 클릭" },
      onInstallment:     { type: "event",  label: "할부 클릭" },
      onImmediatePayment: { type: "event", label: "즉시결제 클릭" },
      onRevolving:       { type: "event",  label: "리볼빙 클릭" },
    },
  },
  // badge 빈 문자열("")은 "배지 없음" 의도이나 컴포넌트 타입이 '예정' | undefined라
  // 빈 문자열을 그대로 전달하면 타입 불일치 — undefined로 정규화한다.
  component: (p) => <StatementTotalCard {...(p as any)} badge={(p as any).badge === "없음" ? undefined : (p as any).badge} />,
};

const SummaryCardDefinition: BlockDefinition = {
  meta: {
    name: "SummaryCard",
    category: "biz",
    domain: "card",
    defaultProps: {
      variant: "asset", title: "총 자산", amount: 42850000, hidden: false,
      icon: "building2",
      actionButtons: [
        { label: "내 계좌", active: false },
        { label: "금융진단", active: false },
        { label: "보험진단", active: false },
      ],
    },
    propSchema: {
      variant: { type: "select",      label: "변형",     default: "asset", options: ["asset", "spending"] },
      title:   { type: "string",      label: "제목",     default: "총 자산" },
      amount:  { type: "number",      label: "금액 (원)", default: 0 },
      hidden:  { type: "boolean",     label: "금액 숨김", default: false },
      icon:    { type: "icon-picker", label: "아이콘",   default: "building2" },
      actionButtons: {
        type: "array", label: "하단 버튼",
        default: [
          { label: "내 계좌", active: false },
          { label: "금융진단", active: false },
          { label: "보험진단", active: false },
        ],
        itemFields: {
          label:  { type: "string",  label: "버튼 텍스트", default: "" },
          active: { type: "boolean", label: "활성 상태",   default: false },
        },
      },
      onClick: { type: "event", label: "클릭" },
    },
  },
  // icon: 스토리북 기준 36px → size-9
  // actionButtons[].onClick은 배열 내 이벤트라 CMS에서 편집 불가 — noop 주입
  component: (p) => {
    type RawAction = { label: string; active?: boolean };
    const rawActions = ((p as any).actionButtons ?? []) as RawAction[];
    const actions = rawActions.map(a => ({ label: a.label, active: a.active ?? false, onClick: () => {} }));
    return (
      <SummaryCard
        {...(p as any)}
        icon={resolveIcon((p as any).icon ?? "building-2", "size-9")}
        actions={actions.length > 0 ? actions : undefined}
      />
    );
  },
};

const UsageTransactionItemDefinition: BlockDefinition = {
  meta: {
    name: "UsageTransactionItem",
    category: "biz",
    domain: "card",
    defaultProps: { tx: { id: "1", merchant: "스타벅스", amount: 6500, date: "2026.04.17", type: "approved", approvalNumber: "12345678", status: "정상", cardName: "하나카드" } },
    propSchema: {
      tx: {
        type: "group", label: "거래 정보",
        fields: {
          id:             { type: "string", label: "ID",       default: "" },
          merchant:       { type: "string", label: "가맹점",    default: "" },
          amount:         { type: "number", label: "금액 (원)", default: 0 },
          date:           { type: "string", label: "날짜",      default: "" },
          type:           { type: "string", label: "유형",      default: "approved" },
          approvalNumber: { type: "string", label: "승인번호",  default: "" },
          status:         { type: "string", label: "상태",      default: "정상" },
          cardName:       { type: "string", label: "카드명",    default: "" },
        },
      },
      onClick: { type: "event", label: "클릭" },
    },
  },
  component: (p) => <UsageTransactionItem {...(p as any)} />,
};

// ═════════════════════════════════════════════════════════════════════════════
// Biz / Insurance
// ═════════════════════════════════════════════════════════════════════════════

const InsuranceSummaryCardDefinition: BlockDefinition = {
  meta: {
    name: "InsuranceSummaryCard",
    category: "biz",
    domain: "insurance",
    defaultProps: { type: "life", insuranceName: "하나 생명보험", contractNumber: "2024-001-123456", premium: 150000, nextPaymentDate: "2026.05.01", status: "active" },
    propSchema: {
      type:            { type: "select",  label: "보험 유형",                    default: "life",   options: ["life", "health", "car"] },
      insuranceName:   { type: "string",  label: "보험 상품명",                  default: "하나 생명보험" },
      contractNumber:  { type: "string",  label: "계약번호 / 증권번호",           default: "2024-001-123456" },
      premium:         { type: "number",  label: "월 납입 보험료 (원화 숫자)",   default: 0 },
      nextPaymentDate: { type: "string",  label: "다음 납입일 (예: 2026.05.01)", default: "" },
      status:          { type: "select",  label: "계약 상태",                    default: "active", options: ["active", "pending", "expired"] },
      badgeText:       { type: "string",  label: "배지 텍스트 override (비워두면 status 기본값 사용)", default: "" },
      onClick:         { type: "event",   label: "카드 클릭" },
    },
  },
  // badgeText 빈 문자열("")은 "override 없음" 의도 — 컴포넌트 타입이 string | undefined라
  // 빈 문자열을 그대로 전달하면 status 기본 배지 텍스트가 무시되므로 undefined로 정규화한다.
  component: (p) => <InsuranceSummaryCard {...(p as any)} badgeText={(p as any).badgeText || undefined} />,
};

// ═════════════════════════════════════════════════════════════════════════════
// Modules / Common
// ═════════════════════════════════════════════════════════════════════════════

const ActionLinkItemDefinition: BlockDefinition = {
  meta: {
    name: "ActionLinkItem",
    category: "modules",
    domain: "common",
    defaultProps: { label: "링크 항목", size: "md", showBorder: false, iconBgClassName: "bg-primary-light", icon: "ChevronRight" },
    propSchema: {
      label:           { type: "string",      label: "레이블",         default: "링크 항목" },
      size:            { type: "select",      label: "크기",           default: "md", options: ["sm", "md"] },
      showBorder:      { type: "boolean",     label: "하단 구분선",     default: false },
      iconBgClassName: { type: "string",      label: "아이콘 배경 클래스", default: "" },
      icon:            { type: "icon-picker", label: "아이콘",         default: "ChevronRight" },
      onClick:         { type: "event",       label: "클릭" },
    },
  },
  component: (p) => <ActionLinkItem {...(p as any)} />,
};

const AlertBannerDefinition: BlockDefinition = {
  meta: {
    name: "AlertBanner",
    category: "modules",
    domain: "common",
    defaultProps: { children: "알림 내용을 입력하세요.", intent: "info", icon: "Info" },
    propSchema: {
      children: { type: "string",      label: "내용",   default: "알림 내용" },
      intent:   { type: "select",      label: "의도",   default: "info", options: ["warning", "danger", "success", "info"] },
      icon:     { type: "icon-picker", label: "아이콘", default: "Info" },
    },
  },
  component: (p) => <AlertBanner {...(p as any)} />,
};

const BalanceToggleDefinition: BlockDefinition = {
  meta: {
    name: "BalanceToggle",
    category: "modules",
    domain: "common",
    defaultProps: { hidden: false },
    propSchema: {
      hidden:   { type: "boolean", label: "잔액 숨김 여부", default: false },
      onToggle: { type: "event",   label: "토글 클릭" },
    },
  },
  component: (p) => <BalanceToggle {...(p as any)} />,
};

const BankSelectGridDefinition: BlockDefinition = {
  meta: {
    name: "BankSelectGrid",
    category: "modules",
    domain: "common",
    defaultProps: { selectedCode: "", banks: [] },
    propSchema: {
      selectedCode: { type: "string", label: "선택된 은행 코드", default: "" },
      onSelect:     { type: "event",  label: "은행 선택" },
    },
  },
  component: (p) => <BankSelectGrid {...(p as any)} />,
};

const CardDefinition: BlockDefinition = {
  meta: {
    name: "Card",
    category: "modules",
    domain: "common",
    defaultProps: { interactive: false, noPadding: false },
    propSchema: {
      interactive: { type: "boolean", label: "클릭 인터랙션", default: false },
      noPadding:   { type: "boolean", label: "패딩 없음",     default: false },
      onClick:     { type: "event",   label: "클릭" },
    },
  },
  component: (p) => <Card {...(p as any)} />,
};

const CheckboxDefinition: BlockDefinition = {
  meta: {
    name: "Checkbox",
    category: "modules",
    domain: "common",
    defaultProps: { checked: false, label: "체크박스", ariaLabel: "", disabled: false, shape: "square" },
    propSchema: {
      checked:   { type: "boolean", label: "체크 여부",      default: false },
      label:     { type: "string",  label: "레이블",         default: "체크박스" },
      ariaLabel: { type: "string",  label: "접근성 레이블",  default: "" },
      shape:     { type: "select",  label: "체크박스 모양",  default: "square", options: ["square", "circle"] },
      disabled:  { type: "boolean", label: "비활성화",       default: false },
      onChange:  { type: "event",   label: "체크 상태 변경" },
    },
  },
  component: (p) => <Checkbox {...(p as any)} />,
};

const CollapsibleSectionDefinition: BlockDefinition = {
  meta: {
    name: "CollapsibleSection",
    category: "modules",
    domain: "common",
    defaultProps: { defaultExpanded: false, headerAlign: "left" },
    propSchema: {
      defaultExpanded: { type: "boolean", label: "기본 펼침 여부", default: false },
      headerAlign:     { type: "select",  label: "헤더 정렬",      default: "left", options: ["left", "center"] },
    },
  },
  component: (p) => <CollapsibleSection {...(p as any)} />,
};

const DatePickerDefinition: BlockDefinition = {
  meta: {
    name: "DatePicker",
    category: "modules",
    domain: "common",
    defaultProps: { mode: "single", label: "날짜 선택", placeholder: "날짜를 선택하세요", disabled: false },
    propSchema: {
      mode:        { type: "select",  label: "선택 모드",       default: "single", options: ["single", "range"] },
      label:       { type: "string",  label: "레이블",          default: "날짜 선택" },
      placeholder: { type: "string",  label: "플레이스홀더",    default: "날짜를 선택하세요" },
      disabled:    { type: "boolean", label: "비활성화",        default: false },
      onChange:    { type: "event",   label: "날짜 변경 (single)" },
      onRangeChange: { type: "event", label: "날짜 범위 변경 (range)" },
    },
  },
  component: (p) => <DatePicker {...(p as any)} />,
};

const DividerDefinition: BlockDefinition = {
  meta: {
    name: "Divider",
    category: "modules",
    domain: "common",
    defaultProps: {},
    propSchema: {},
  },
  component: (_p) => <Divider />,
};

const DividerWithLabelDefinition: BlockDefinition = {
  meta: {
    name: "DividerWithLabel",
    category: "modules",
    domain: "common",
    defaultProps: { label: "또는" },
    propSchema: {
      label: { type: "string", label: "구분선 레이블", default: "또는" },
    },
  },
  component: (p) => <DividerWithLabel {...(p as any)} />,
};

const DropdownMenuDefinition: BlockDefinition = {
  meta: {
    name: "DropdownMenu",
    category: "modules",
    domain: "common",
    defaultProps: { align: "right", items: [{ label: "메뉴 항목 1" }, { label: "삭제", variant: "danger" }] },
    propSchema: {
      align: { type: "select", label: "패널 정렬", default: "right", options: ["left", "right"] },
      items: {
        type: "array", label: "메뉴 항목",
        default: [{ label: "메뉴 항목 1" }, { label: "삭제", variant: "danger" }],
        itemFields: {
          label:   { type: "string",      label: "레이블",    default: "" },
          icon:    { type: "icon-picker", label: "아이콘",    default: "" },
          variant: { type: "select",      label: "스타일 변형", default: "default", options: ["default", "danger"] },
        },
      },
    },
  },
  // DropdownMenu는 children(트리거 요소)이 필수이므로 CMS 미리보기용 기본 버튼을 주입한다.
  component: (p) => (
    <DropdownMenu {...(p as any)}>
      <button type="button" className="p-2 rounded hover:bg-surface-subtle">
        <MoreVertical className="size-5" />
      </button>
    </DropdownMenu>
  ),
};

const EmptyStateDefinition: BlockDefinition = {
  meta: {
    name: "EmptyState",
    category: "modules",
    domain: "common",
    defaultProps: { title: "데이터가 없습니다", description: "" },
    propSchema: {
      title:       { type: "string", label: "제목", default: "데이터가 없습니다" },
      description: { type: "string", label: "설명", default: "" },
    },
  },
  component: (p) => <EmptyState {...(p as any)} />,
};

const ErrorStateDefinition: BlockDefinition = {
  meta: {
    name: "ErrorState",
    category: "modules",
    domain: "common",
    defaultProps: { title: "오류가 발생했습니다", description: "", retryLabel: "다시 시도" },
    propSchema: {
      title:       { type: "string", label: "제목",       default: "오류가 발생했습니다" },
      description: { type: "string", label: "설명",       default: "" },
      retryLabel:  { type: "string", label: "재시도 레이블", default: "다시 시도" },
      onRetry:     { type: "event",  label: "재시도 클릭" },
    },
  },
  component: (p) => <ErrorState {...(p as any)} />,
};

const InfoRowDefinition: BlockDefinition = {
  meta: {
    name: "InfoRow",
    category: "modules",
    domain: "common",
    defaultProps: { label: "레이블", value: "값", showBorder: true },
    propSchema: {
      label:          { type: "string",  label: "레이블",          default: "레이블" },
      value:          { type: "string",  label: "값",              default: "값" },
      valueClassName: { type: "string",  label: "값 추가 클래스",   default: "" },
      showBorder:     { type: "boolean", label: "하단 구분선 표시", default: true },
    },
  },
  component: (p) => <InfoRow {...(p as any)} />,
};

const LabelValueRowDefinition: BlockDefinition = {
  meta: {
    name: "LabelValueRow",
    category: "modules",
    domain: "common",
    defaultProps: { label: "레이블", value: "값" },
    propSchema: {
      label: { type: "string", label: "레이블", default: "레이블" },
      value: { type: "string", label: "값",     default: "값" },
    },
  },
  component: (p) => <LabelValueRow {...(p as any)} />,
};

const NoticeItemDefinition: BlockDefinition = {
  meta: {
    name: "NoticeItem",
    category: "modules",
    domain: "common",
    defaultProps: { title: "공지사항", description: "", showDivider: true, icon: "Bell", iconBgClassName: "" },
    propSchema: {
      icon:           { type: "icon-picker", label: "아이콘",         default: "Bell" },
      iconBgClassName: { type: "string",     label: "아이콘 배경 클래스", default: "" },
      title:          { type: "string",      label: "제목",           default: "공지사항" },
      description:    { type: "string",      label: "설명",           default: "" },
      showDivider:    { type: "boolean",     label: "구분선 표시",     default: true },
      onClick:        { type: "event",       label: "클릭" },
    },
  },
  component: (p) => <NoticeItem {...(p as any)} />,
};

const RecentRecipientItemDefinition: BlockDefinition = {
  meta: {
    name: "RecentRecipientItem",
    category: "modules",
    domain: "common",
    defaultProps: { name: "홍길동", bankName: "하나은행", maskedAccount: "123-****-01011" },
    propSchema: {
      name:          { type: "string", label: "수취인명",        default: "홍길동" },
      bankName:      { type: "string", label: "은행명",          default: "하나은행" },
      maskedAccount: { type: "string", label: "마스킹 계좌번호", default: "123-****-01011" },
      onClick:       { type: "event",  label: "클릭" },
    },
  },
  component: (p) => <RecentRecipientItem {...(p as any)} />,
};

const SectionHeaderDefinition: BlockDefinition = {
  meta: {
    name: "SectionHeader",
    category: "modules",
    domain: "common",
    defaultProps: { title: "섹션 제목", badge: 0, actionLabel: "" },
    propSchema: {
      title:       { type: "string", label: "제목",         default: "섹션 제목" },
      badge:       { type: "number", label: "배지 숫자",     default: 0 },
      actionLabel: { type: "string", label: "액션 레이블",   default: "" },
      onAction:    { type: "event",  label: "액션 클릭" },
    },
  },
  component: (p) => <SectionHeader {...(p as any)} />,
};

const SelectableItemDefinition: BlockDefinition = {
  meta: {
    name: "SelectableItem",
    category: "modules",
    domain: "common",
    defaultProps: { label: "선택 항목", selected: false, icon: "Check" },
    propSchema: {
      icon:     { type: "icon-picker", label: "아이콘",   default: "Check" },
      label:    { type: "string",      label: "레이블",   default: "선택 항목" },
      selected: { type: "boolean",     label: "선택 여부", default: false },
      onClick:  { type: "event",       label: "클릭" },
    },
  },
  component: (p) => <SelectableItem {...(p as any)} />,
};

const SelectableListItemDefinition: BlockDefinition = {
  meta: {
    name: "SelectableListItem",
    category: "modules",
    domain: "common",
    defaultProps: { label: "목록 항목", isSelected: false },
    propSchema: {
      label:      { type: "string",  label: "레이블",   default: "목록 항목" },
      isSelected: { type: "boolean", label: "선택 여부", default: false },
      onClick:    { type: "event",   label: "클릭" },
    },
  },
  component: (p) => <SelectableListItem {...(p as any)} />,
};

const SidebarNavDefinition: BlockDefinition = {
  meta: {
    name: "SidebarNav",
    category: "modules",
    domain: "common",
    defaultProps: { activeId: "home", items: [{ id: "home", label: "홈" }] },
    propSchema: {
      activeId: { type: "string", label: "활성 탭 ID", default: "home" },
      items: {
        type: "array", label: "탭 항목",
        default: [{ id: "home", label: "홈" }],
        itemFields: {
          id:    { type: "string", label: "ID",     default: "" },
          label: { type: "string", label: "레이블", default: "" },
        },
      },
      onItemChange: { type: "event", label: "탭 변경" },
    },
  },
  component: (p) => <SidebarNav {...(p as any)} />,
};

const StepIndicatorDefinition: BlockDefinition = {
  meta: {
    name: "StepIndicator",
    category: "modules",
    domain: "common",
    defaultProps: { current: 1, total: 3 },
    propSchema: {
      current: { type: "number", label: "현재 단계", default: 1 },
      total:   { type: "number", label: "전체 단계", default: 3 },
    },
  },
  component: (p) => <StepIndicator {...(p as any)} />,
};

const SuccessHeroDefinition: BlockDefinition = {
  meta: {
    name: "SuccessHero",
    category: "modules",
    domain: "common",
    defaultProps: { recipientName: "홍길동", amount: "100,000원", subtitle: "이체가 완료되었습니다." },
    propSchema: {
      recipientName: { type: "string", label: "수취인명",   default: "홍길동" },
      amount:        { type: "string", label: "금액 텍스트", default: "100,000원" },
      subtitle:      { type: "string", label: "부제목",      default: "이체가 완료되었습니다." },
    },
  },
  component: (p) => <SuccessHero {...(p as any)} />,
};

const TabNavDefinition: BlockDefinition = {
  meta: {
    name: "TabNav",
    category: "modules",
    domain: "common",
    defaultProps: { activeId: "all", variant: "underline", fullWidth: false, items: [{ id: "all", label: "전체" }] },
    propSchema: {
      activeId:  { type: "string",  label: "활성 탭 ID", default: "all" },
      variant:   { type: "select",  label: "변형",        default: "underline", options: ["underline", "pill"] },
      fullWidth: { type: "boolean", label: "전체 너비",   default: false },
      items: {
        type: "array", label: "탭 항목",
        default: [{ id: "all", label: "전체" }],
        itemFields: {
          id:    { type: "string", label: "ID",     default: "" },
          label: { type: "string", label: "레이블", default: "" },
        },
      },
      onTabChange: { type: "event", label: "탭 변경" },
    },
  },
  component: (p) => <TabNav {...(p as any)} />,
};

const TransferLimitInfoDefinition: BlockDefinition = {
  meta: {
    name: "TransferLimitInfo",
    category: "modules",
    domain: "common",
    defaultProps: { perTransferLimit: 10000000, dailyLimit: 50000000 },
    propSchema: {
      perTransferLimit: { type: "number", label: "1회 이체 한도 (원)", default: 10000000 },
      dailyLimit:       { type: "number", label: "1일 이체 한도 (원)", default: 50000000 },
      usedAmount:       { type: "number", label: "오늘 이체 누적 (원, 선택)", default: 0 },
    },
  },
  component: (p) => <TransferLimitInfo {...(p as any)} />,
};

// ═════════════════════════════════════════════════════════════════════════════
// Modules / Banking
// ═════════════════════════════════════════════════════════════════════════════

const AccountSelectItemDefinition: BlockDefinition = {
  meta: {
    name: "AccountSelectItem",
    category: "modules",
    domain: "banking",
    defaultProps: { accountName: "하나 자유입출금", accountNumber: "123-456789-01011", balance: "1,234,567원", selected: false, icon: "Landmark" },
    propSchema: {
      accountName:   { type: "string",      label: "계좌명",    default: "하나 자유입출금" },
      accountNumber: { type: "string",      label: "계좌번호",   default: "123-456789-01011" },
      balance:       { type: "string",      label: "잔액",      default: "0원" },
      selected:      { type: "boolean",     label: "선택 여부", default: false },
      icon:          { type: "icon-picker", label: "아이콘",    default: "Landmark" },
      onClick:       { type: "event",       label: "클릭" },
    },
  },
  component: (p) => <AccountSelectItem {...(p as any)} />,
};

const AmountInputDefinition: BlockDefinition = {
  meta: {
    name: "AmountInput",
    category: "modules",
    domain: "banking",
    defaultProps: { label: "이체 금액", placeholder: "금액을 입력하세요", helperText: "", hasError: false, maxAmount: 0, transferLimitText: "", disabled: false },
    propSchema: {
      label:             { type: "string",  label: "레이블",           default: "이체 금액" },
      placeholder:       { type: "string",  label: "플레이스홀더",     default: "금액을 입력하세요" },
      helperText:        { type: "string",  label: "도움말 텍스트",     default: "" },
      hasError:          { type: "boolean", label: "오류 상태",         default: false },
      maxAmount:         { type: "number",  label: "최대 금액 (원)",    default: 0 },
      transferLimitText: { type: "string",  label: "이체 한도 텍스트", default: "" },
      disabled:          { type: "boolean", label: "비활성화",          default: false },
      onChange:          { type: "event",   label: "값 변경" },
    },
  },
  component: (p) => <AmountInput {...(p as any)} />,
};

// NumberKeypad의 digits는 외부에서 셔플된 숫자 배열을 필수로 받는 구조.
// CMS 빌더 미리보기에서는 실제 셔플 없이 순서대로 고정 주입한다.
const DEFAULT_KEYPAD_DIGITS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];

const NumberKeypadDefinition: BlockDefinition = {
  meta: {
    name: "NumberKeypad",
    category: "modules",
    domain: "banking",
    defaultProps: {},
    propSchema: {
      onDigitPress: { type: "event", label: "숫자 입력" },
      onDelete:     { type: "event", label: "삭제" },
      onShuffle:    { type: "event", label: "배열 섞기" },
    },
  },
  component: (_p) => (
    <NumberKeypad
      digits={DEFAULT_KEYPAD_DIGITS}
      onDigitPress={() => {}}
      onDelete={() => {}}
      onShuffle={() => {}}
    />
  ),
};

const OtpInputDefinition: BlockDefinition = {
  meta: {
    name: "OtpInput",
    category: "modules",
    domain: "banking",
    defaultProps: { length: 6, error: false, disabled: false, masked: true },
    propSchema: {
      length:     { type: "select",  label: "자리 수",   default: "6", options: ["4", "6"] },
      error:      { type: "boolean", label: "오류 상태", default: false },
      disabled:   { type: "boolean", label: "비활성화",  default: false },
      masked:     { type: "boolean", label: "마스킹",    default: true },
      onComplete: { type: "event",   label: "입력 완료" },
      onChange:   { type: "event",   label: "값 변경" },
    },
  },
  // length select는 문자열을 반환하므로 OtpLength(4 | 6) 숫자로 변환해 전달한다.
  component: (p) => <OtpInput {...(p as any)} length={Number((p as any).length) as 4 | 6} />,
};

const PinDotIndicatorDefinition: BlockDefinition = {
  meta: {
    name: "PinDotIndicator",
    category: "modules",
    domain: "banking",
    defaultProps: { length: 6, filledCount: 0 },
    propSchema: {
      length:      { type: "number", label: "전체 자리 수",   default: 6 },
      filledCount: { type: "number", label: "입력된 자리 수", default: 0 },
    },
  },
  component: (p) => <PinDotIndicator {...(p as any)} />,
};

const TransactionListDefinition: BlockDefinition = {
  meta: {
    name: "TransactionList",
    category: "modules",
    domain: "banking",
    defaultProps: { items: [], loading: false, emptyMessage: "거래 내역이 없습니다.", dateHeaderFormat: "month-day" },
    propSchema: {
      loading:          { type: "boolean", label: "로딩 중",       default: false },
      emptyMessage:     { type: "string",  label: "빈 목록 메시지", default: "거래 내역이 없습니다." },
      dateHeaderFormat: { type: "select",  label: "날짜 헤더 형식", default: "month-day", options: ["month-day", "year-month-day"] },
      onItemClick:      { type: "event",   label: "항목 클릭" },
    },
  },
  component: (p) => <TransactionList {...(p as any)} />,
};

// TransactionSearchFilter는 controlled 컴포넌트로 value가 필수.
// CMS 빌더 미리보기에서는 오늘 기준 30일 범위를 기본값으로 주입한다.
const DEFAULT_SEARCH_PARAMS = {
  startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
  endDate: new Date().toISOString().slice(0, 10),
  sortOrder: "recent" as const,
  transactionType: "all" as const,
};

const TransactionSearchFilterDefinition: BlockDefinition = {
  meta: {
    name: "TransactionSearchFilter",
    category: "modules",
    domain: "banking",
    defaultProps: { defaultExpanded: false },
    propSchema: {
      defaultExpanded: { type: "boolean", label: "기본 펼침 여부", default: false },
      onSearch:        { type: "event",   label: "검색 실행" },
    },
  },
  component: (p) => (
    <TransactionSearchFilter
      value={DEFAULT_SEARCH_PARAMS}
      onSearch={() => {}}
      {...(p as any)}
    />
  ),
};

const TransferFormDefinition: BlockDefinition = {
  meta: {
    name: "TransferForm",
    category: "modules",
    domain: "banking",
    defaultProps: { availableBalance: 0, submitting: false },
    propSchema: {
      availableBalance: { type: "number",  label: "출금 가능 금액 (원)", default: 0 },
      submitting:       { type: "boolean", label: "이체 처리 중",        default: false },
      onSubmit:         { type: "event",   label: "이체 실행" },
    },
  },
  component: (p) => <TransferForm {...(p as any)} />,
};

// ═════════════════════════════════════════════════════════════════════════════
// 카테고리별 블록 그룹 및 전체 내보내기
// ═════════════════════════════════════════════════════════════════════════════

export const coreBlocks: BlockDefinition[] = [
  BadgeDefinition,
  ButtonDefinition,
  InputDefinition,
  SelectDefinition,
  TypographyDefinition,
];

export const bizBlocks: BlockDefinition[] = [
  // biz/common
  BannerCarouselDefinition,
  BrandBannerDefinition,
  QuickMenuGridDefinition,
  UserProfileDefinition,
  // biz/banking
  AccountSelectorCardDefinition,
  AccountSummaryCardDefinition,
  // biz/card
  AccountSelectCardDefinition,
  BillingPeriodLabelDefinition,
  CardBenefitSummaryDefinition,
  CardChipItemDefinition,
  CardInfoPanelDefinition,
  CardLinkedBalanceDefinition,
  CardManagementPanelDefinition,
  CardPaymentActionsDefinition,
  CardPaymentItemDefinition,
  CardPaymentSummaryDefinition,
  CardPerformanceBarDefinition,
  CardPillTabDefinition,
  CardSummaryCardDefinition,
  CardVisualDefinition,
  LoanMenuBarDefinition,
  PaymentAccountCardDefinition,
  QuickShortcutCardDefinition,
  StatementHeroCardDefinition,
  StatementTotalCardDefinition,
  SummaryCardDefinition,
  UsageTransactionItemDefinition,
  // biz/insurance
  InsuranceSummaryCardDefinition,
];

export const moduleBlocks: BlockDefinition[] = [
  // modules/common
  ActionLinkItemDefinition,
  AlertBannerDefinition,
  BalanceToggleDefinition,
  BankSelectGridDefinition,
  CardDefinition,
  CheckboxDefinition,
  CollapsibleSectionDefinition,
  DatePickerDefinition,
  DividerDefinition,
  DividerWithLabelDefinition,
  DropdownMenuDefinition,
  EmptyStateDefinition,
  ErrorStateDefinition,
  InfoRowDefinition,
  LabelValueRowDefinition,
  NoticeItemDefinition,
  RecentRecipientItemDefinition,
  SectionHeaderDefinition,
  SelectableItemDefinition,
  SelectableListItemDefinition,
  SidebarNavDefinition,
  StepIndicatorDefinition,
  SuccessHeroDefinition,
  TabNavDefinition,
  TransferLimitInfoDefinition,
  // modules/banking
  AccountSelectItemDefinition,
  AmountInputDefinition,
  NumberKeypadDefinition,
  OtpInputDefinition,
  PinDotIndicatorDefinition,
  TransactionListDefinition,
  TransactionSearchFilterDefinition,
  TransferFormDefinition,
];

/** 전체 카테고리 블록 통합 목록 */
export const blocks: BlockDefinition[] = [
  ...coreBlocks,
  ...bizBlocks,
  ...moduleBlocks,
];
