/// <reference types="@figma/plugin-typings" />
/**
 * @file createComponents.ts
 * @description React component-library의 모든 컴포넌트를 카테고리별 섹션으로 생성·배치한다.
 *
 * 캔버스 레이아웃 (섹션 순서 → 섹션 내 알파벳 순 정렬):
 * ● Core            — Badge,
 *                     Button (Variant×Size×Mode 60개 단일 ComponentSet),
 *                     Input (Size×Mode 12개 단일 ComponentSet),
 *                     Select, Typography
 * ● Layout          — AppBrandHeader > BottomNav > Stack > Inline > Grid > Section >
 *                     BlankPageLayout > PageLayout(18 variants) > HomePageLayout(2 variants) > ModalSlideOver
 * ● Modules/Common  — ActionLinkItem, AlertBanner, BalanceToggle,
 *                     BankSelectGrid, BankSelectGridItem, BottomSheet, Card, Checkbox,
 *                     CollapsibleSection, DatePicker, Divider, DividerWithLabel, DropdownMenu, DropdownMenu/Item,
 *                     EmptyState, ErrorState, InfoRow, LabelValueRow, Modal, NoticeItem,
 *                     PinConfirmSheet, RecentRecipientItem, SectionHeader,
 *                     SelectableItem, SelectableListItem, SidebarNav, SidebarNav/Item, StepIndicator,
 *                     SuccessHero, TabNav, TabNav/Item, TransferLimitInfo
 * ● Modules/Banking — AccountSelectItem, AmountInput, NumberKeypad, OtpInput,
 *                     PinDotIndicator, TransactionList, TransactionSearchFilter, TransferForm
 * ● Biz/Common      — BannerCarousel, BrandBanner, QuickMenuGrid, QuickMenuGrid/Item, UserProfile
 * ● Biz/Banking     — AccountSelectorCard, AccountSummaryCard
 * ● Biz/Card        — AccountSelectCard, BillingPeriodLabel,
 *                     CardBenefitSummary, CardChipItem, CardInfoPanel, CardLinkedBalance,
 *                     CardManagementPanel, CardPaymentActions, CardPaymentItem, CardPaymentSummary,
 *                     CardPerformanceBar, CardPillTab, CardSummaryCard, CardVisual,
 *                     LoanMenuBar, PaymentAccountCard, QuickShortcutCard,
 *                     StatementHeroCard, StatementTotalCard, SummaryCard,
 *                     UsageHistoryFilterSheet, UsageTransactionItem
 * ● Biz/Insurance   — InsuranceSummaryCard
 *
 * @returns 완료 메시지 문자열
 */

import { FONT_FAMILY } from '../utils/tokens';

/* core */
import { createBadge }            from '../components/core/createBadge';
import { createButton }            from '../components/core/createButton';
import { createInput }             from '../components/core/createInput';
import { createSelect }           from '../components/core/createSelect';
import { createTypography }       from '../components/core/createTypography';

/* layout */
import { createAppBrandHeader }     from '../components/layout/createAppBrandHeader';
import { createBottomNav }          from '../components/layout/createBottomNav';
import { createModalSlideOver }     from '../components/layout/createModalSlideOver';
import { createBlankPageLayout, createPageLayout, createHomePageLayout } from '../components/layout/createPageLayouts';
import { createStack }              from '../components/layout/createStack';
import { createInline }             from '../components/layout/createInline';
import { createGrid }               from '../components/layout/createGrid';
import { createSection }            from '../components/layout/createSection';

/* modules/common */
import { createActionLinkItem }   from '../components/modules/common/createActionLinkItem';
import { createAlertBanner }      from '../components/modules/common/createAlertBanner';
import { createBalanceToggle }    from '../components/modules/common/createBalanceToggle';
import { createBankSelectGridItem, createBankSelectGrid } from '../components/modules/common/createBankSelectGrid';
import { createBottomSheet }      from '../components/modules/common/createBottomSheet';
import { createCard }             from '../components/modules/common/createCard';
import { createCheckbox }         from '../components/modules/common/createCheckbox';
import { createCollapsibleSection}from '../components/modules/common/createCollapsibleSection';
import { createDatePicker }       from '../components/modules/common/createDatePicker';
import { createDivider }          from '../components/modules/common/createDivider';
import { createDividerWithLabel } from '../components/modules/common/createDividerWithLabel';
import { createDropdownMenuItem, createDropdownMenu } from '../components/modules/common/createDropdownMenu';
import { createEmptyState }       from '../components/modules/common/createEmptyState';
import { createErrorState }       from '../components/modules/common/createErrorState';
import { createInfoRow, createLabelValueRow } from '../components/modules/common/createInfoRow';
import { createModal }            from '../components/modules/common/createModal';
import { createNoticeItem }       from '../components/modules/common/createNoticeItem';
import { createPinConfirmSheet }  from '../components/modules/common/createPinConfirmSheet';
import { createRecentRecipientItem } from '../components/modules/common/createRecentRecipientItem';
import { createSectionHeader }    from '../components/modules/common/createSectionHeader';
import { createSelectableItem }   from '../components/modules/common/createSelectableItem';
import { createSelectableListItem }  from '../components/modules/common/createSelectableListItem';
import { createSidebarNavItem, createSidebarNav } from '../components/modules/common/createSidebarNav';
import { createStepIndicator }    from '../components/modules/common/createStepIndicator';
import { createSuccessHero }      from '../components/modules/common/createSuccessHero';
import { createTabNavItem, createTabNav } from '../components/modules/common/createTabNav';
import { createTransferLimitInfo } from '../components/modules/common/createTransferLimitInfo';

/* modules/banking */
import { createAccountSelectItem }from '../components/modules/banking/createAccountSelectItem';
import { createAmountInput }      from '../components/modules/banking/createAmountInput';
import { createNumberKeypad }          from '../components/modules/banking/createNumberKeypad';
import { createOtpInput }         from '../components/modules/banking/createOtpInput';
import { createPinDotIndicator }       from '../components/modules/banking/createPinDotIndicator';
import { createTransactionList }       from '../components/modules/banking/createTransactionList';
import { createTransactionSearchFilter } from '../components/modules/banking/createTransactionSearchFilter';
import { createTransferForm }          from '../components/modules/banking/createTransferForm';

/* biz/common */
import { createBannerCarousel }      from '../components/biz/common/createBannerCarousel';
import { createBrandBanner }         from '../components/biz/common/createBrandBanner';
import { createQuickMenuGridItem, createQuickMenuGrid } from '../components/biz/common/createQuickMenuGrid';
import { createUserProfile }         from '../components/biz/common/createUserProfile';

/* biz/banking */
import { createAccountSelectorCard } from '../components/biz/banking/createAccountSelectorCard';
import { createAccountSummaryCard }  from '../components/biz/banking/createAccountSummaryCard';

/* biz/card */
import { createAccountSelectCard }       from '../components/biz/card/createAccountSelectCard';
import { createBillingPeriodLabel }      from '../components/biz/card/createBillingPeriodLabel';
import { createCardBenefitSummaryBenefitItem, createCardBenefitSummary } from '../components/biz/card/createCardBenefitSummary';
import { createCardChipItem }            from '../components/biz/card/createCardChipItem';
import { createCardInfoPanelCardInfoRow, createCardInfoPanelCardInfoSection, createCardInfoPanel } from '../components/biz/card/createCardInfoPanel';
import { createCardLinkedBalance }       from '../components/biz/card/createCardLinkedBalance';
import { createCardManagementPanelItem, createCardManagementPanel } from '../components/biz/card/createCardManagementPanel';
import { createCardPaymentActions }      from '../components/biz/card/createCardPaymentActions';
import { createCardPaymentItem }         from '../components/biz/card/createCardPaymentItem';
import { createCardPaymentSummary }      from '../components/biz/card/createCardPaymentSummary';
import { createCardPerformanceBar }      from '../components/biz/card/createCardPerformanceBar';
import { createCardPillTab }             from '../components/biz/card/createCardPillTab';
import { createCardSummaryCard }         from '../components/biz/card/createCardSummaryCard';
import { createCardVisual, createCardImagePlaceholder } from '../components/biz/card/createCardVisual';
import { createLoanMenuBarItem, createLoanMenuBar } from '../components/biz/card/createLoanMenuBar';
import { createPaymentAccountCard }      from '../components/biz/card/createPaymentAccountCard';
import { createQuickShortcutCard }       from '../components/biz/card/createQuickShortcutCard';
import { createStatementHeroCard }       from '../components/biz/card/createStatementHeroCard';
import { createStatementTotalCard }      from '../components/biz/card/createStatementTotalCard';
import { createSummaryCardActionItem, createSummaryCard } from '../components/biz/card/createSummaryCard';
import { createUsageHistoryFilterSheet } from '../components/biz/card/createUsageHistoryFilterSheet';
import { createUsageTransactionItem }    from '../components/biz/card/createUsageTransactionItem';

/* biz/insurance */
import { createInsuranceSummaryCard }    from '../components/biz/insurance/createInsuranceSummaryCard';

/* ── 레이아웃 상수 ──────────────────────────────────────────── */
const COMPONENT_GAP   = 48;  // 같은 행 내 컴포넌트 간격
const SECTION_GAP     = 100; // 섹션 간 세로 간격
const SECTION_PADDING = 48;  // 섹션 내부 패딩 (상하좌우 동일)

/**
 * 피그마 Section 노드를 생성하고 nodes를 가로로 나열해 담는다.
 *
 * 치수는 section.appendChild() 이전에 측정한다.
 * Figma는 appendChild 시 자식 노드 좌표를 리셋하므로, 이동 후 섹션 기준 상대 좌표로 재지정한다.
 * @returns 다음 섹션의 시작 y 좌표
 */
function layoutSection(name: string, nodes: SceneNode[], startY: number): number {
  /* 1. appendChild 이전에 치수 측정 — 이동 후 크기가 달라질 수 있으므로 미리 기록 */
  const dims = nodes.map(n => ({ w: n.width, h: n.height }));

  let totalW = SECTION_PADDING;
  let maxH   = 0;
  dims.forEach(d => {
    totalW += d.w + COMPONENT_GAP;
    if (d.h > maxH) maxH = d.h;
  });
  totalW = totalW - COMPONENT_GAP + SECTION_PADDING;
  const totalH = SECTION_PADDING + maxH + SECTION_PADDING;

  /* 2. 섹션 생성·위치·크기 설정 */
  const section = figma.createSection();
  section.name  = name;
  figma.currentPage.appendChild(section);
  section.x = 0;
  section.y = startY;
  section.resizeWithoutConstraints(totalW, totalH);

  /* 3. 노드를 섹션으로 이동한 뒤 섹션 기준 상대 좌표로 재배치
   * appendChild 시 좌표가 리셋되므로, 이후에 좌표를 지정해야 한다.
   * ComponentSetNode 등 일부 타입은 Section에 직접 담지 못할 수 있으므로
   * 실패 시 캔버스 절대 좌표로 대체 배치한다 */
  let cursorX = SECTION_PADDING;
  nodes.forEach((node, i) => {
    try {
      section.appendChild(node);
      node.x = cursorX;
      node.y = SECTION_PADDING;
    } catch {
      /* section.appendChild 실패 → 섹션 밖 캔버스에 절대 좌표로 배치 */
      figma.currentPage.appendChild(node);
      node.x = cursorX;
      node.y = startY + SECTION_PADDING;
    }
    cursorX += dims[i].w + COMPONENT_GAP;
  });

  return startY + totalH + SECTION_GAP;
}

/** 현재 실행 중인 단계 이름 — 오류 발생 시 위치 특정용 */
let _step = '(init)';
/** 단계 변경 시 호출되는 콜백 — main.ts에서 진행 토스트 갱신에 사용 */
let _onProgress: ((step: string) => void) | undefined;

/** 단계 이름을 갱신하고 진행 콜백을 호출한다. */
function setStep(name: string): void {
  _step = name;
  _onProgress?.(name);
}

/** 단계 이름을 갱신하고 fn()을 실행한다. 에러 시 단계 이름이 메시지에 포함된다. */
async function s<T extends SceneNode>(name: string, fn: () => Promise<T>): Promise<T> {
  setStep(name);
  return fn();
}

export async function createComponents(
  onProgress?: (step: string) => void,
): Promise<string> {
  _onProgress = onProgress;

  /* 0. 페이지 로드 — dynamic-page documentAccess 모드에서는
   * figma.currentPage.appendChild 등 ChildrenMixin 메서드 사용 전에 반드시 호출해야 한다 */
  setStep('loadAsync');
  await figma.currentPage.loadAsync();

  /* 1. 폰트 사전 로드 */
  setStep('loadFontAsync');
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Regular' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Medium' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Bold' });

  /* 2. 컴포넌트 생성 — 섹션 순서: Core → Layout → Modules/Common → Modules/Banking → Biz/Common → Biz/Banking → Biz/Card → Biz/Insurance */

  /* ── Core (알파벳 순) ────────────────────────────────── */
  const coreNodes: SceneNode[] = [
    await s('createBadge',           createBadge),
    await s('createButton',          createButton),
    await s('createInput',           createInput),
    await s('createSelect',          createSelect),
    await s('createTypography',      createTypography),
  ];

  /* ── Layout ────────────────────────────────────────────── */
  /* HomePageLayout은 bottomNav 인스턴스를 내부에서 사용하므로
   * bottomNav를 먼저 생성한 뒤 createHomePageLayout에 전달해야 한다. */
  const appBrandHeader = await s('createAppBrandHeader', createAppBrandHeader);
  const bottomNav      = await s('createBottomNav',      createBottomNav);
  const stack          = await s('createStack',          createStack);
  const inline         = await s('createInline',         createInline);
  const grid           = await s('createGrid',           createGrid);
  grid.name = 'Grid (사용x)';
  const section        = await s('createSection',        createSection);
  const blankPageLayout = await s('createBlankPageLayout', createBlankPageLayout);
  const pageLayout      = await s('createPageLayout',      createPageLayout);
  setStep('createHomePageLayout');
  const homePageLayout  = await createHomePageLayout(bottomNav);
  const modalSlideOver  = await s('createModalSlideOver',  createModalSlideOver);
  modalSlideOver.name = 'ModalSlideOver (사용x)';

  const layoutNodes: SceneNode[] = [
    appBrandHeader, bottomNav, stack, inline, grid, section,
    blankPageLayout, pageLayout, homePageLayout, modalSlideOver,
  ];

  /* ── Modules/Common (알파벳 순) ──────────────────────── */
  /* BankSelectGrid/Item을 먼저 생성한 뒤 Grid에 전달한다 */
  const bankSelectGridItem = await s('createBankSelectGridItem', createBankSelectGridItem);
  setStep('createBankSelectGrid');
  const bankSelectGrid = await createBankSelectGrid(bankSelectGridItem);

  /* DropdownMenu/Item을 먼저 생성한 뒤 DropdownMenu 패널에 전달한다 */
  const dropdownMenuItem = await s('createDropdownMenuItem', createDropdownMenuItem);
  setStep('createDropdownMenu');
  const dropdownMenu = await createDropdownMenu(dropdownMenuItem);

  /* SidebarNav/Item을 먼저 생성한 뒤 SidebarNav에 전달한다 */
  const sidebarNavItem = await s('createSidebarNavItem', createSidebarNavItem);
  setStep('createSidebarNav');
  const sidebarNav = await createSidebarNav(sidebarNavItem);

  /* TabNav/Item을 먼저 생성한 뒤 TabNav에 전달한다 */
  const tabNavItem = await s('createTabNavItem', createTabNavItem);
  setStep('createTabNav');
  const tabNav = await createTabNav(tabNavItem);

  const moduleCommonNodes: SceneNode[] = [
    await s('createActionLinkItem',      createActionLinkItem),
    await s('createAlertBanner',         createAlertBanner),
    await s('createBalanceToggle',       createBalanceToggle),
    bankSelectGrid,
    bankSelectGridItem,
    await s('createBottomSheet',         createBottomSheet),
    await s('createCard',                createCard).then(n => { n.name = 'Card (사용x)'; return n; }),
    await s('createCheckbox',            createCheckbox),
    await s('createCollapsibleSection',  createCollapsibleSection),
    await s('createDatePicker',          createDatePicker),
    await s('createDivider',             createDivider),
    await s('createDividerWithLabel',    createDividerWithLabel),
    dropdownMenu,
    dropdownMenuItem,
    await s('createEmptyState',          createEmptyState),
    await s('createErrorState',          createErrorState),
    await s('createInfoRow',             createInfoRow),
    await s('createLabelValueRow',       createLabelValueRow),
    await s('createModal',               createModal),
    await s('createNoticeItem',          createNoticeItem),
    await s('createPinConfirmSheet',     createPinConfirmSheet),
    await s('createRecentRecipientItem', createRecentRecipientItem),
    await s('createSectionHeader',       createSectionHeader),
    await s('createSelectableItem',      createSelectableItem),
    await s('createSelectableListItem',  createSelectableListItem),
    sidebarNavItem,
    sidebarNav,
    await s('createStepIndicator',       createStepIndicator),
    await s('createSuccessHero',         createSuccessHero),
    tabNav,
    tabNavItem,
    await s('createTransferLimitInfo',   createTransferLimitInfo),
  ];

  /* ── Modules/Banking (알파벳 순) ─────────────────────── */
  const moduleBankingNodes: SceneNode[] = [
    await s('createAccountSelectItem',        createAccountSelectItem),
    await s('createAmountInput',              createAmountInput),
    await s('createNumberKeypad',             createNumberKeypad),
    await s('createOtpInput',                 createOtpInput),
    await s('createPinDotIndicator',          createPinDotIndicator),
    await s('createTransactionList',          createTransactionList),
    await s('createTransactionSearchFilter',  createTransactionSearchFilter),
    await s('createTransferForm',             createTransferForm),
  ];

  /* ── Biz/Common (알파벳 순) ──────────────────────────── */
  /* QuickMenuGrid/Item을 먼저 생성한 뒤 Grid에 전달한다 */
  const quickMenuGridItem = await s('createQuickMenuGridItem', createQuickMenuGridItem);
  setStep('createQuickMenuGrid');
  const quickMenuGrid = await createQuickMenuGrid(quickMenuGridItem);

  const bizCommonNodes: SceneNode[] = [
    await s('createBannerCarousel', createBannerCarousel),
    await s('createBrandBanner',    createBrandBanner),
    quickMenuGrid,
    quickMenuGridItem,
    await s('createUserProfile',    createUserProfile),
  ];

  /* ── Biz/Banking (알파벳 순) ─────────────────────────── */
  const bizBankingNodes: SceneNode[] = [
    await s('createAccountSelectorCard', createAccountSelectorCard),
    await s('createAccountSummaryCard',  createAccountSummaryCard),
  ];

  /* ── Biz/Card (알파벳 순) ────────────────────────────── */
  /* CardBenefitSummary/BenefitItem을 먼저 생성한 뒤 CardBenefitSummary 슬롯에 전달한다 */
  const cardBenefitSummaryBenefitItem = await s('createCardBenefitSummaryBenefitItem', createCardBenefitSummaryBenefitItem);
  setStep('createCardBenefitSummary');
  const cardBenefitSummary = await createCardBenefitSummary(cardBenefitSummaryBenefitItem);

  /* CardInfoPanel/CardInfoRow → CardInfoSection → CardInfoPanel 순으로 생성한다 */
  const cardInfoPanelCardInfoRow = await s('createCardInfoPanelCardInfoRow', createCardInfoPanelCardInfoRow);
  setStep('createCardInfoPanelCardInfoSection');
  const cardInfoPanelCardInfoSection = await createCardInfoPanelCardInfoSection(cardInfoPanelCardInfoRow);
  setStep('createCardInfoPanel');
  const cardInfoPanel = await createCardInfoPanel(cardInfoPanelCardInfoSection);

  /* CardManagementPanel/Item을 먼저 생성한 뒤 CardManagementPanel 슬롯에 전달한다 */
  const cardManagementPanelItem = await s('createCardManagementPanelItem', createCardManagementPanelItem);
  setStep('createCardManagementPanel');
  const cardManagementPanel = await createCardManagementPanel(cardManagementPanelItem);

  /* LoanMenuBar/Item을 먼저 생성한 뒤 LoanMenuBar 슬롯에 전달한다 */
  const loanMenuBarItem = await s('createLoanMenuBarItem', createLoanMenuBarItem);
  setStep('createLoanMenuBar');
  const loanMenuBar = await createLoanMenuBar(loanMenuBarItem);

  /* SummaryCard/ActionItem을 먼저 생성한 뒤 SummaryCard Actions 슬롯에 전달한다 */
  const summaryCardActionItem = await s('createSummaryCardActionItem', createSummaryCardActionItem);
  setStep('createSummaryCard');
  const summaryCard = await createSummaryCard(summaryCardActionItem);

  /* CardVisual/Image 플레이스홀더를 먼저 생성한 뒤 CardVisual에 전달한다
   * layoutSection에서 CardVisual 바로 옆에 나란히 배치된다 */
  const cardVisualImage = await s('createCardImagePlaceholder', createCardImagePlaceholder);
  setStep('createCardVisual');
  const cardVisual = await createCardVisual(cardVisualImage);

  const bizCardNodes: SceneNode[] = [
    await s('createAccountSelectCard',         createAccountSelectCard),
    await s('createBillingPeriodLabel',        createBillingPeriodLabel),
    cardBenefitSummary,
    cardBenefitSummaryBenefitItem,
    await s('createCardChipItem',              createCardChipItem),
    cardInfoPanel,
    cardInfoPanelCardInfoSection,
    cardInfoPanelCardInfoRow,
    await s('createCardLinkedBalance',         createCardLinkedBalance),
    cardManagementPanel,
    cardManagementPanelItem,
    await s('createCardPaymentActions',        createCardPaymentActions),
    await s('createCardPaymentItem',           createCardPaymentItem),
    await s('createCardPaymentSummary',        createCardPaymentSummary),
    await s('createCardPerformanceBar',        createCardPerformanceBar),
    await s('createCardPillTab',               createCardPillTab),
    await s('createCardSummaryCard',           createCardSummaryCard),
    cardVisual,
    cardVisualImage,                           /* CardVisual/Image 플레이스홀더 — CardVisual 옆 배치 */
    loanMenuBar,
    loanMenuBarItem,
    await s('createPaymentAccountCard',        createPaymentAccountCard),
    await s('createQuickShortcutCard',         createQuickShortcutCard),
    await s('createStatementHeroCard',         createStatementHeroCard),
    await s('createStatementTotalCard',        createStatementTotalCard),
    summaryCard,
    summaryCardActionItem,
    await s('createUsageHistoryFilterSheet',   createUsageHistoryFilterSheet),
    await s('createUsageTransactionItem',      createUsageTransactionItem),
  ];

  /* ── Biz/Insurance ───────────────────────────────────── */
  const bizInsuranceNodes: SceneNode[] = [
    await s('createInsuranceSummaryCard', createInsuranceSummaryCard),
  ];

  /* 3. 섹션별 배치 */
  let nextY = 0;
  setStep('layoutSection:Core');
  nextY = layoutSection('Core',            coreNodes,          nextY);
  setStep('layoutSection:Layout');
  nextY = layoutSection('Layout',          layoutNodes,        nextY);
  setStep('layoutSection:Modules/Common');
  nextY = layoutSection('Modules/Common',  moduleCommonNodes,  nextY);
  setStep('layoutSection:Modules/Banking');
  nextY = layoutSection('Modules/Banking', moduleBankingNodes, nextY);
  setStep('layoutSection:Biz/Common');
  nextY = layoutSection('Biz/Common',      bizCommonNodes,     nextY);
  setStep('layoutSection:Biz/Banking');
  nextY = layoutSection('Biz/Banking',     bizBankingNodes,    nextY);
  setStep('layoutSection:Biz/Card');
  nextY = layoutSection('Biz/Card',        bizCardNodes,       nextY);
  setStep('layoutSection:Biz/Insurance');
  nextY = layoutSection('Biz/Insurance',   bizInsuranceNodes,  nextY);

  /* 4. 뷰포트 맞춤 */
  figma.viewport.scrollAndZoomIntoView([
    ...coreNodes, ...layoutNodes, ...moduleCommonNodes, ...moduleBankingNodes,
    ...bizCommonNodes, ...bizBankingNodes, ...bizCardNodes, ...bizInsuranceNodes,
  ]);

  return '✅ React Component Library 생성 완료! (총 89개 컴포넌트)';
}

/** createComponents 실행 중 발생한 에러에 단계 정보를 포함해 반환한다. */
export function getLastStep(): string {
  return _step;
}
