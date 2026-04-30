/**
 * @file main.ts
 * @description Figma 플러그인 진입점.
 * React component-library의 모든 컴포넌트를 카테고리별 섹션으로 배치한다.
 *
 * 캔버스 레이아웃:
 * ● Core            — Button, Button/WithIcon, Button/IconOnly, Button/FullWidth,
 *                     Badge,
 *                     Input, Input/WithLabel, Input/WithHelper, Input/WithIcon, Input/Format, Input/FullWidth,
 *                     Typography, Select
 * ● Modules/Common  — SectionHeader, AlertBanner, EmptyState, InfoRow, LabelValueRow,
 *                     DividerWithLabel, SelectableItem, ActionLinkItem, NoticeItem,
 *                     CollapsibleSection, SuccessHero, Card, BalanceToggle, DropdownMenu,
 *                     Checkbox, Divider, StepIndicator, ErrorState, SelectableListItem,
 *                     RecentRecipientItem, SidebarNav, BankSelectGrid/Item, BankSelectGrid,
 *                     TransferLimitInfo, DatePicker, PinConfirmSheet
 * ● Modules/Banking — AccountSelectItem, AmountInput, OtpInput,
 *                     NumberKeypad, PinDotIndicator, TransactionList,
 *                     TransactionSearchFilter, TransferForm
 * ● Modules/Common  (cont.) — BottomSheet, Modal, TabNav
 * ● Layout          — PageHeader, HomeHeader, AppBrandHeader, BottomNav, ModalSlideOver,
 *                     PageLayouts(Blank|Page|Home),
 *                     Stack(Gap=sm|md|lg), Inline(Justify=Start|Between|End),
 *                     Grid(Cols=2|3|4), Section(HasTitle=True|False)
 * ● Biz/Banking     — AccountSummaryCard, AccountSelectorCard
 * ● Biz/Common      — QuickMenuGrid, BannerCarousel, UserProfile, BrandBanner
 * ● Biz/Card        — CardVisual, CardPillTab, CardSummaryCard, CardChipItem, CardInfoPanel,
 *                     CardPaymentItem, CardPaymentActions, CardPaymentSummary, CardBenefitSummary,
 *                     CardLinkedBalance, CardManagementPanel, CardPerformanceBar, BillingPeriodLabel,
 *                     AccountSelectCard, PaymentAccountCard, StatementHeroCard, StatementTotalCard,
 *                     SummaryCard, QuickShortcutCard, LoanMenuBar, UsageHistoryFilterSheet,
 *                     UsageTransactionItem
 * ● Biz/Insurance   — InsuranceSummaryCard
 */

import { COLOR, BRAND, FONT_SIZE, SPACING, FONT_FAMILY, FONT_VAR } from './tokens';
import { solid } from './helpers';
import { createVariables } from './createVariables';

/* core */
import {
  createButton,
  createButtonWithIcon,
  createButtonIconOnly,
  createButtonFullWidth,
}                                  from './components/core/createButton';
import { createBadge }            from './components/core/createBadge';
import {
  createInput,
  createInputWithLabel,
  createInputWithHelper,
  createInputWithIcon,
  createInputFormat,
  createInputFullWidth,
}                                  from './components/core/createInput';
import { createTypography }       from './components/core/createTypography';
import { createSelect }           from './components/core/createSelect';

/* modules */
import { createSectionHeader }    from './components/modules/common/createSectionHeader';
import { createAlertBanner }      from './components/modules/common/createAlertBanner';
import { createEmptyState }       from './components/modules/common/createEmptyState';
import { createInfoRow, createLabelValueRow } from './components/modules/common/createInfoRow';
import { createDividerWithLabel } from './components/modules/common/createDividerWithLabel';
import { createSelectableItem }   from './components/modules/common/createSelectableItem';
import { createAccountSelectItem }from './components/modules/banking/createAccountSelectItem';
import { createActionLinkItem }   from './components/modules/common/createActionLinkItem';
import { createNoticeItem }       from './components/modules/common/createNoticeItem';
import { createAmountInput }      from './components/modules/banking/createAmountInput';
import { createOtpInput }         from './components/modules/banking/createOtpInput';
import { createCollapsibleSection}from './components/modules/common/createCollapsibleSection';
import { createSuccessHero }      from './components/modules/common/createSuccessHero';
import { createCard }             from './components/modules/common/createCard';
import { createBalanceToggle }    from './components/modules/common/createBalanceToggle';
import { createDropdownMenu }     from './components/modules/common/createDropdownMenu';
import { createCheckbox }         from './components/modules/common/createCheckbox';
import { createDivider }          from './components/modules/common/createDivider';
import { createStepIndicator }    from './components/modules/common/createStepIndicator';
import { createErrorState }       from './components/modules/common/createErrorState';
import { createSelectableListItem }  from './components/modules/common/createSelectableListItem';
import { createRecentRecipientItem } from './components/modules/common/createRecentRecipientItem';
import { createSidebarNav }       from './components/modules/common/createSidebarNav';
import { createBankSelectGridItem, createBankSelectGrid } from './components/modules/common/createBankSelectGrid';
import { createTransferLimitInfo } from './components/modules/common/createTransferLimitInfo';
import { createDatePicker }       from './components/modules/common/createDatePicker';
import { createPinConfirmSheet }  from './components/modules/common/createPinConfirmSheet';

/* banking modules */
import { createNumberKeypad }          from './components/modules/banking/createNumberKeypad';
import { createPinDotIndicator }       from './components/modules/banking/createPinDotIndicator';
import { createTransactionList }       from './components/modules/banking/createTransactionList';
import { createTransactionSearchFilter } from './components/modules/banking/createTransactionSearchFilter';
import { createTransferForm }          from './components/modules/banking/createTransferForm';

/* modules/common (overlay) */
import { createModal }            from './components/modules/common/createModal';
import { createBottomSheet }      from './components/modules/common/createBottomSheet';

/* layout */
import { createBottomNav }          from './components/layout/createBottomNav';
import { createPageHeader, createHomeHeader } from './components/layout/createPageHeaders';
import { createAppBrandHeader }     from './components/layout/createAppBrandHeader';
import { createModalSlideOver }     from './components/layout/createModalSlideOver';
import { createPageLayouts }        from './components/layout/createPageLayouts';
import { createStack }              from './components/layout/createStack';
import { createInline }             from './components/layout/createInline';
import { createGrid }               from './components/layout/createGrid';
import { createSection }            from './components/layout/createSection';
import { createTabNav }             from './components/modules/common/createTabNav';

/* biz/banking */
import { createAccountSummaryCard }  from './components/biz/banking/createAccountSummaryCard';
import { createAccountSelectorCard } from './components/biz/banking/createAccountSelectorCard';

/* biz/common */
import { createQuickMenuGrid }       from './components/biz/common/createQuickMenuGrid';
import { createBannerCarousel }      from './components/biz/common/createBannerCarousel';
import { createUserProfile }         from './components/biz/common/createUserProfile';
import { createBrandBanner }         from './components/biz/common/createBrandBanner';

/* biz/card */
import { createCardVisual }              from './components/biz/card/createCardVisual';
import { createCardPillTab }             from './components/biz/card/createCardPillTab';
import { createCardSummaryCard }         from './components/biz/card/createCardSummaryCard';
import { createCardChipItem }            from './components/biz/card/createCardChipItem';
import { createCardInfoPanel }           from './components/biz/card/createCardInfoPanel';
import { createCardPaymentItem }         from './components/biz/card/createCardPaymentItem';
import { createCardPaymentActions }      from './components/biz/card/createCardPaymentActions';
import { createCardPaymentSummary }      from './components/biz/card/createCardPaymentSummary';
import { createCardBenefitSummary }      from './components/biz/card/createCardBenefitSummary';
import { createCardLinkedBalance }       from './components/biz/card/createCardLinkedBalance';
import { createCardManagementPanel }     from './components/biz/card/createCardManagementPanel';
import { createCardPerformanceBar }      from './components/biz/card/createCardPerformanceBar';
import { createBillingPeriodLabel }      from './components/biz/card/createBillingPeriodLabel';
import { createAccountSelectCard }       from './components/biz/card/createAccountSelectCard';
import { createPaymentAccountCard }      from './components/biz/card/createPaymentAccountCard';
import { createStatementHeroCard }       from './components/biz/card/createStatementHeroCard';
import { createStatementTotalCard }      from './components/biz/card/createStatementTotalCard';
import { createSummaryCard }             from './components/biz/card/createSummaryCard';
import { createQuickShortcutCard }       from './components/biz/card/createQuickShortcutCard';
import { createLoanMenuBar }             from './components/biz/card/createLoanMenuBar';
import { createUsageHistoryFilterSheet } from './components/biz/card/createUsageHistoryFilterSheet';
import { createUsageTransactionItem }    from './components/biz/card/createUsageTransactionItem';

/* biz/insurance */
import { createInsuranceSummaryCard }    from './components/biz/insurance/createInsuranceSummaryCard';

/* ── 레이아웃 상수 ──────────────────────────────────────────── */
const COMPONENT_GAP = 48;  // 같은 행 내 컴포넌트 간격
const SECTION_GAP   = 100; // 섹션 간 세로 간격
const LABEL_GAP     = 28;  // 섹션 레이블과 첫 컴포넌트 간격

/** 섹션 레이블 — "● 섹션명" 형태로 브랜드 점 + Bold 텍스트 */
function createSectionLabel(text: string, x: number, y: number): FrameNode {
  const frame = figma.createFrame();
  frame.name = `_section/${text}`;
  frame.layoutMode = 'HORIZONTAL';
  frame.itemSpacing = SPACING.sm;
  frame.counterAxisAlignItems = 'CENTER';
  frame.primaryAxisSizingMode = 'AUTO';
  frame.counterAxisSizingMode = 'AUTO';
  frame.fills = [];

  const dot = figma.createEllipse();
  dot.resize(10, 10);
  dot.fills = [solid(BRAND.primary)];
  frame.appendChild(dot);

  const label = figma.createText();
  label.fontName = { family: FONT_FAMILY.sans, style: 'Bold' };
  label.fontSize = FONT_SIZE.lg;
  label.characters = text;
  label.fills = [solid(COLOR.textHeading)];
  frame.appendChild(label);

  /* x, y는 페이지에 추가된 이후에 설정해야 실제 캔버스 위치에 반영된다.
   * appendChild 이전에 설정하면 페이지 삽입 시 (0, 0)으로 리셋된다. */
  figma.currentPage.appendChild(frame);
  frame.x = x;
  frame.y = y;
  return frame;
}

/**
 * 컴포넌트 노드 배열을 가로로 나열하고 최대 높이를 반환한다.
 * 컴포넌트 너비가 달라도 각 노드의 실제 width를 사용하므로 겹치지 않는다.
 */
function layoutRow(nodes: SceneNode[], startX: number, startY: number): number {
  let cursorX = startX;
  let maxHeight = 0;
  nodes.forEach((node) => {
    /* x/y 설정 전에 반드시 페이지에 append해야 좌표가 (0,0)으로 리셋되지 않는다.
     * 이미 페이지에 있더라도 appendChild → x/y 순서를 강제해 좌표를 안전하게 적용한다. */
    figma.currentPage.appendChild(node);
    node.x = cursorX;
    node.y = startY;
    cursorX += node.width + COMPONENT_GAP;
    if (node.height > maxHeight) maxHeight = node.height;
  });
  return maxHeight;
}

/** 섹션 레이블 + 컴포넌트 행을 배치하고 다음 섹션의 시작 y를 반환한다. */
function layoutSection(name: string, nodes: SceneNode[], startY: number): number {
  const label = createSectionLabel(name, 0, startY);
  const rowY = startY + label.height + LABEL_GAP;
  const rowHeight = layoutRow(nodes, 0, rowY);
  return rowY + rowHeight + SECTION_GAP;
}

/* ── 메인 ──────────────────────────────────────────────────── */
(async () => {
  /* ── 커맨드 분기 ──────────────────────────────────────────
   * manifest.json menu 항목에 따라 실행 흐름을 분기한다.
   * 'createVariables' : Primitives + Semantic 변수 일괄 등록
   * 'createComponents': 기존 컴포넌트 생성 로직
   * ────────────────────────────────────────────────────── */
  if (figma.command === 'createVariables') {
    const message = await createVariables();
    figma.closePlugin(message);
    return;
  }

  /* 1. 폰트 사전 로드 */
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Regular' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Medium' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Bold' });

  /* 2. 컴포넌트 생성 */
  const coreNodes: SceneNode[] = [
    await createButton(),
    await createButtonWithIcon(),
    await createButtonIconOnly(),
    await createButtonFullWidth(),
    await createBadge(),
    await createInput(),
    await createInputWithLabel(),
    await createInputWithHelper(),
    await createInputWithIcon(),
    await createInputFormat(),
    await createInputFullWidth(),
    await createTypography(),
    await createSelect(),
  ];

  const moduleCommonNodes: SceneNode[] = [
    await createSectionHeader(),
    await createAlertBanner(),
    await createEmptyState(),
    await createInfoRow(),
    await createLabelValueRow(),
    await createDividerWithLabel(),
    await createSelectableItem(),
    await createActionLinkItem(),
    await createNoticeItem(),
    await createCollapsibleSection(),
    await createSuccessHero(),
    await createCard(),
    await createBalanceToggle(),
    await createDropdownMenu(),
    await createCheckbox(),
    await createDivider(),
    await createStepIndicator(),
    await createErrorState(),
    await createSelectableListItem(),
    await createRecentRecipientItem(),
    await createSidebarNav(),
    await createBankSelectGridItem(),
    await createBankSelectGrid(),
    await createTransferLimitInfo(),
    await createDatePicker(),
    await createPinConfirmSheet(),
    await createBottomSheet(),
    await createModal(),
    await createTabNav(),
  ];

  const moduleBankingNodes: SceneNode[] = [
    await createAccountSelectItem(),
    await createAmountInput(),
    await createOtpInput(),
    await createNumberKeypad(),
    await createPinDotIndicator(),
    await createTransactionList(),
    await createTransactionSearchFilter(),
    await createTransferForm(),
  ];
  
  /* layout 컴포넌트 참조를 변수에 저장 — createPageLayouts에서 인스턴스 생성에 사용 */
  const pageHeader    = await createPageHeader();
  const homeHeader    = await createHomeHeader();
  const appBrandHeader = await createAppBrandHeader();
  const bottomNav     = await createBottomNav();
  const modalSlideOver = await createModalSlideOver();
  const pageLayouts   = await createPageLayouts(pageHeader, homeHeader, bottomNav);

  const layoutNodes: SceneNode[] = [
    pageHeader, homeHeader, appBrandHeader, bottomNav, modalSlideOver, pageLayouts,
    await createStack(),
    await createInline(),
    await createGrid(),
    await createSection(),
  ];

  const bizBankingNodes: SceneNode[] = [
    await createAccountSummaryCard(),
    await createAccountSelectorCard(),
  ];

  const bizCommonNodes: SceneNode[] = [
    await createBannerCarousel(),
    await createBrandBanner(),
    await createQuickMenuGrid(),
    await createUserProfile(),
  ];

  const bizCardNodes: SceneNode[] = [
    await createCardVisual(),
    await createCardPillTab(),
    await createCardSummaryCard(),
    await createCardChipItem(),
    await createCardInfoPanel(),
    await createCardPaymentItem(),
    await createCardPaymentActions(),
    await createCardPaymentSummary(),
    await createCardBenefitSummary(),
    await createCardLinkedBalance(),
    await createCardManagementPanel(),
    await createCardPerformanceBar(),
    await createBillingPeriodLabel(),
    await createAccountSelectCard(),
    await createPaymentAccountCard(),
    await createStatementHeroCard(),
    await createStatementTotalCard(),
    await createSummaryCard(),
    await createQuickShortcutCard(),
    await createLoanMenuBar(),
    await createUsageHistoryFilterSheet(),
    await createUsageTransactionItem(),
  ];

  const bizInsuranceNodes: SceneNode[] = [
    await createInsuranceSummaryCard(),
  ];

  /* 3. 섹션별 배치 */
  let nextY = 0;
  nextY = layoutSection('Core',            coreNodes,          nextY);
  nextY = layoutSection('Modules/Common',  moduleCommonNodes,  nextY);
  nextY = layoutSection('Modules/Banking', moduleBankingNodes, nextY);
  nextY = layoutSection('Layout',          layoutNodes,        nextY);
  nextY = layoutSection('Biz/Banking',     bizBankingNodes,    nextY);
  nextY = layoutSection('Biz/Common',      bizCommonNodes,     nextY);
  nextY = layoutSection('Biz/Card',        bizCardNodes,       nextY);
  nextY = layoutSection('Biz/Insurance',   bizInsuranceNodes,  nextY);

  /* 4. 뷰포트 맞춤 */
  figma.viewport.scrollAndZoomIntoView([
    ...coreNodes, ...moduleCommonNodes, ...moduleBankingNodes, ...layoutNodes,
    ...bizBankingNodes, ...bizCommonNodes, ...bizCardNodes, ...bizInsuranceNodes,
  ]);
  figma.closePlugin('✅ React Component Library 생성 완료! (총 89개 컴포넌트)');
})().catch((err) => {
  /* 어떤 createXxx()에서 에러가 났는지 플러그인 알림으로 표시 */
  figma.closePlugin(`❌ 오류: ${err instanceof Error ? err.message : String(err)}`);
});
