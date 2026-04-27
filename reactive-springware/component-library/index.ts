/**
 * @file index.ts
 * @description 컴포넌트 라이브러리 중앙 진입점.
 *
 * 카테고리별 모든 컴포넌트와 타입을 re-export한다.
 * npm 배포 없이 vite alias '@cl'로 직접 참조한다.
 *
 * @example
 * import { Button, Modal } from '@cl';
 */

// 연동 프로젝트(demo/front, react-cms 등)가 '@cl' alias로 이 파일을 참조할 때
// 각 프로젝트의 tailwindcss() 플러그인이 globals.css를 처리하고,
// globals.css 내부의 @source 지시어로 컴포넌트 파일을 스캔해 사용된 Tailwind 클래스를 출력한다.
import '../design-tokens/globals.css';

/* ── Core (원자 컴포넌트) ─────────────────────────────────────── */
export * from './core/Badge';
export * from './core/Button';
export * from './core/Input';
export * from './core/Select';
export * from './core/Typography';

/* ── Modules / Common (도메인 무관 분자 컴포넌트) ──────────────── */
export * from './modules/common/BalanceToggle';
export * from './modules/common/ActionLinkItem';
export * from './modules/common/AlertBanner';
export * from './modules/common/BankSelectGrid';
export * from './modules/common/BottomSheet';
export * from './modules/common/Card';
export * from './modules/common/Checkbox';
export * from './modules/common/CollapsibleSection';
export * from './modules/common/DatePicker';
export * from './modules/common/Divider';
export * from './modules/common/DividerWithLabel';
export * from './modules/common/DropdownMenu';
export * from './modules/common/EmptyState';
export * from './modules/common/ErrorState';
export * from './modules/common/InfoRow';
export * from './modules/common/LabelValueRow';
export * from './modules/common/Modal';
export * from './modules/common/NoticeItem';
export * from './modules/common/PinConfirmSheet';
export * from './modules/common/RecentRecipientItem';
export * from './modules/common/SectionHeader';
export * from './modules/common/SelectableItem';
export * from './modules/common/SelectableListItem';
export * from './modules/common/SidebarNav';
export * from './modules/common/StepIndicator';
export * from './modules/common/SuccessHero';
export * from './modules/common/TabNav';
export * from './modules/common/TransferLimitInfo';

/* ── Modules / Banking (뱅킹 전용 분자 컴포넌트) ─────────────── */
export * from './modules/banking/AccountSelectItem';
export * from './modules/banking/AmountInput';
export * from './modules/banking/NumberKeypad';
export * from './modules/banking/OtpInput';
export * from './modules/banking/PinDotIndicator';
export * from './modules/banking/TransactionList';
export * from './modules/banking/TransactionSearchFilter';
export * from './modules/banking/TransferForm';

/* ── Layout (레이아웃 컴포넌트) ─────────────────────────────── */
export * from './layout/AppBrandHeader';
export * from './layout/BlankPageLayout';
export * from './layout/BottomNav';
export * from './layout/Grid';
export * from './layout/HomePageLayout';
export * from './layout/Inline';
export * from './layout/ModalSlideOver';
export * from './layout/PageLayout';
export * from './layout/Section';
export * from './layout/Stack';

/* ── Biz / Common (홈 화면 공통 컴포넌트) ────────────────────── */
export * from './biz/common/BannerCarousel';
export * from './biz/common/BrandBanner';
export * from './biz/common/QuickMenuGrid';
export * from './biz/common/UserProfile';

/* ── Biz / Banking (뱅킹 계좌 요약 컴포넌트) ─────────────────── */
export * from './biz/banking/AccountSelectorCard';
export * from './biz/banking/AccountSummaryCard';

/* ── Biz / Card (카드 도메인 컴포넌트) ───────────────────────── */
export * from './biz/card/AccountSelectCard';
export * from './biz/card/BillingPeriodLabel';
export * from './biz/card/CardBenefitSummary';
export * from './biz/card/CardChipItem';
export * from './biz/card/CardInfoPanel';
export * from './biz/card/CardLinkedBalance';
export * from './biz/card/CardManagementPanel';
export * from './biz/card/CardPaymentActions';
export * from './biz/card/CardPaymentItem';
export * from './biz/card/CardPaymentSummary';
export * from './biz/card/CardPerformanceBar';
export * from './biz/card/CardPillTab';
export * from './biz/card/CardSummaryCard';
export * from './biz/card/CardVisual';
export * from './biz/card/LoanMenuBar';
export * from './biz/card/PaymentAccountCard';
export * from './biz/card/QuickShortcutCard';
export * from './biz/card/StatementHeroCard';
export * from './biz/card/StatementTotalCard';
export * from './biz/card/SummaryCard';
export * from './biz/card/UsageHistoryFilterSheet';
export * from './biz/card/UsageTransactionItem';

/* ── Biz / Insurance (보험 도메인 컴포넌트) ──────────────────── */
export * from './biz/insurance/InsuranceSummaryCard';

/* ── Pages / Common (공통 페이지) ────────────────────────────── */
export * from './pages/common/HomeDashboardPage';
export * from './pages/common/FullMenuPage';
export * from './pages/common/LoginPage';

/* ── Pages / Card (카드 페이지) ──────────────────────────────── */
export * from './pages/card/CardDashboardPage';
export * from './pages/card/HanaCardMenuPage';
export * from './pages/card/ImmediatePayCompletePage';
export * from './pages/card/ImmediatePayMethodPage';
export * from './pages/card/ImmediatePayPage';
export * from './pages/card/ImmediatePayRequestPage';
export * from './pages/card/ImmediatePaymentPage';
export * from './pages/card/MyCardManagementPage';
export * from './pages/card/PaymentStatementPage';
export * from './pages/card/UsageHistoryPage';
export * from './pages/card/UserManagementPage';

/* ── Pages / Banking (뱅킹 페이지) ──────────────────────────── */
export * from './pages/banking/AccountDetailPage';
export * from './pages/banking/AccountPasswordPage';
export * from './pages/banking/AccountSelectPage';
export * from './pages/banking/AllAccountsPage';
export * from './pages/banking/BankSelectPage';
export * from './pages/banking/TransactionDetailPage';
export * from './pages/banking/TransactionHistoryPage';
export * from './pages/banking/TransferConfirmPage';
export * from './pages/banking/TransferInputPage';
export * from './pages/banking/TransferSuccessPage';
