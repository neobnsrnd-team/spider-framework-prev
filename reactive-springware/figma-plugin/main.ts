/// <reference types="@figma/plugin-typings" />
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

import { FONT_FAMILY } from './tokens';
import { createVariables } from './createVariables';
import { createIcons } from './createIcons';

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

/* ── 메인 ──────────────────────────────────────────────────── */

/** 현재 실행 중인 단계 이름 — 오류 발생 시 위치 특정용 */
let _step = '(init)';

/** 단계 이름을 갱신하고 fn()을 실행한다. 에러 시 단계 이름이 메시지에 포함된다. */
async function s<T extends SceneNode>(name: string, fn: () => Promise<T>): Promise<T> {
  _step = name;
  return fn();
}

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

  if (figma.command === 'createIcons') {
    const message = await createIcons();
    figma.closePlugin(message);
    return;
  }

  /* 0. 페이지 로드 — dynamic-page documentAccess 모드에서는
   * figma.currentPage.appendChild 등 ChildrenMixin 메서드 사용 전에 반드시 호출해야 한다 */
  _step = 'loadAsync';
  await figma.currentPage.loadAsync();

  /* 1. 폰트 사전 로드 */
  _step = 'loadFontAsync';
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Regular' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Medium' });
  await figma.loadFontAsync({ family: FONT_FAMILY.sans, style: 'Bold' });

  /* 2. 컴포넌트 생성 */
  const coreNodes: SceneNode[] = [
    await s('createButton',         createButton),
    await s('createButtonWithIcon', createButtonWithIcon),
    await s('createButtonIconOnly', createButtonIconOnly),
    await s('createButtonFullWidth',createButtonFullWidth),
    await s('createBadge',          createBadge),
    await s('createInput',          createInput),
    await s('createInputWithLabel', createInputWithLabel),
    await s('createInputWithHelper',createInputWithHelper),
    await s('createInputWithIcon',  createInputWithIcon),
    await s('createInputFormat',    createInputFormat),
    await s('createInputFullWidth', createInputFullWidth),
    await s('createTypography',     createTypography),
    await s('createSelect',         createSelect),
  ];

  const moduleCommonNodes: SceneNode[] = [
    await s('createSectionHeader',     createSectionHeader),
    await s('createAlertBanner',       createAlertBanner),
    await s('createEmptyState',        createEmptyState),
    await s('createInfoRow',           createInfoRow),
    await s('createLabelValueRow',     createLabelValueRow),
    await s('createDividerWithLabel',  createDividerWithLabel),
    await s('createSelectableItem',    createSelectableItem),
    await s('createActionLinkItem',    createActionLinkItem),
    await s('createNoticeItem',        createNoticeItem),
    await s('createCollapsibleSection',createCollapsibleSection),
    await s('createSuccessHero',       createSuccessHero),
    await s('createCard',              createCard),
    await s('createBalanceToggle',     createBalanceToggle),
    await s('createDropdownMenu',      createDropdownMenu),
    await s('createCheckbox',          createCheckbox),
    await s('createDivider',           createDivider),
    await s('createStepIndicator',     createStepIndicator),
    await s('createErrorState',        createErrorState),
    await s('createSelectableListItem',createSelectableListItem),
    await s('createRecentRecipientItem',createRecentRecipientItem),
    await s('createSidebarNav',        createSidebarNav),
    await s('createBankSelectGridItem',createBankSelectGridItem),
    await s('createBankSelectGrid',    createBankSelectGrid),
    await s('createTransferLimitInfo', createTransferLimitInfo),
    await s('createDatePicker',        createDatePicker),
    await s('createPinConfirmSheet',   createPinConfirmSheet),
    await s('createBottomSheet',       createBottomSheet),
    await s('createModal',             createModal),
    await s('createTabNav',            createTabNav),
  ];

  const moduleBankingNodes: SceneNode[] = [
    await s('createAccountSelectItem',      createAccountSelectItem),
    await s('createAmountInput',            createAmountInput),
    await s('createOtpInput',               createOtpInput),
    await s('createNumberKeypad',           createNumberKeypad),
    await s('createPinDotIndicator',        createPinDotIndicator),
    await s('createTransactionList',        createTransactionList),
    await s('createTransactionSearchFilter',createTransactionSearchFilter),
    await s('createTransferForm',           createTransferForm),
  ];

  /* layout 컴포넌트 참조를 변수에 저장 — createPageLayouts에서 인스턴스 생성에 사용 */
  const pageHeader     = await s('createPageHeader',    createPageHeader);
  const homeHeader     = await s('createHomeHeader',    createHomeHeader);
  const appBrandHeader = await s('createAppBrandHeader',createAppBrandHeader);
  const bottomNav      = await s('createBottomNav',     createBottomNav);
  const modalSlideOver = await s('createModalSlideOver',createModalSlideOver);
  _step = 'createPageLayouts';
  const pageLayouts    = await createPageLayouts(pageHeader, homeHeader, bottomNav);

  const layoutNodes: SceneNode[] = [
    pageHeader, homeHeader, appBrandHeader, bottomNav, modalSlideOver, pageLayouts,
    await s('createStack',   createStack),
    await s('createInline',  createInline),
    await s('createGrid',    createGrid),
    await s('createSection', createSection),
  ];

  const bizBankingNodes: SceneNode[] = [
    await s('createAccountSummaryCard',  createAccountSummaryCard),
    await s('createAccountSelectorCard', createAccountSelectorCard),
  ];

  const bizCommonNodes: SceneNode[] = [
    await s('createBannerCarousel', createBannerCarousel),
    await s('createBrandBanner',    createBrandBanner),
    await s('createQuickMenuGrid',  createQuickMenuGrid),
    await s('createUserProfile',    createUserProfile),
  ];

  const bizCardNodes: SceneNode[] = [
    await s('createCardVisual',           createCardVisual),
    await s('createCardPillTab',          createCardPillTab),
    await s('createCardSummaryCard',      createCardSummaryCard),
    await s('createCardChipItem',         createCardChipItem),
    await s('createCardInfoPanel',        createCardInfoPanel),
    await s('createCardPaymentItem',      createCardPaymentItem),
    await s('createCardPaymentActions',   createCardPaymentActions),
    await s('createCardPaymentSummary',   createCardPaymentSummary),
    await s('createCardBenefitSummary',   createCardBenefitSummary),
    await s('createCardLinkedBalance',    createCardLinkedBalance),
    await s('createCardManagementPanel',  createCardManagementPanel),
    await s('createCardPerformanceBar',   createCardPerformanceBar),
    await s('createBillingPeriodLabel',   createBillingPeriodLabel),
    await s('createAccountSelectCard',    createAccountSelectCard),
    await s('createPaymentAccountCard',   createPaymentAccountCard),
    await s('createStatementHeroCard',    createStatementHeroCard),
    await s('createStatementTotalCard',   createStatementTotalCard),
    await s('createSummaryCard',          createSummaryCard),
    await s('createQuickShortcutCard',    createQuickShortcutCard),
    await s('createLoanMenuBar',          createLoanMenuBar),
    await s('createUsageHistoryFilterSheet',createUsageHistoryFilterSheet),
    await s('createUsageTransactionItem', createUsageTransactionItem),
  ];

  const bizInsuranceNodes: SceneNode[] = [
    await s('createInsuranceSummaryCard', createInsuranceSummaryCard),
  ];

  /* 3. 섹션별 배치 */
  let nextY = 0;
  _step = 'layoutSection:Core';
  nextY = layoutSection('Core',            coreNodes,          nextY);
  _step = 'layoutSection:Modules/Common';
  nextY = layoutSection('Modules/Common',  moduleCommonNodes,  nextY);
  _step = 'layoutSection:Modules/Banking';
  nextY = layoutSection('Modules/Banking', moduleBankingNodes, nextY);
  _step = 'layoutSection:Layout';
  nextY = layoutSection('Layout',          layoutNodes,        nextY);
  _step = 'layoutSection:Biz/Banking';
  nextY = layoutSection('Biz/Banking',     bizBankingNodes,    nextY);
  _step = 'layoutSection:Biz/Common';
  nextY = layoutSection('Biz/Common',      bizCommonNodes,     nextY);
  _step = 'layoutSection:Biz/Card';
  nextY = layoutSection('Biz/Card',        bizCardNodes,       nextY);
  _step = 'layoutSection:Biz/Insurance';
  nextY = layoutSection('Biz/Insurance',   bizInsuranceNodes,  nextY);

  /* 4. 뷰포트 맞춤 */
  figma.viewport.scrollAndZoomIntoView([
    ...coreNodes, ...moduleCommonNodes, ...moduleBankingNodes, ...layoutNodes,
    ...bizBankingNodes, ...bizCommonNodes, ...bizCardNodes, ...bizInsuranceNodes,
  ]);
  figma.closePlugin('✅ React Component Library 생성 완료! (총 89개 컴포넌트)');
})().catch((err) => {
  /* _step 이 마지막으로 갱신된 함수명을 포함해 에러 위치를 표시한다 */
  figma.closePlugin(`❌ 오류 in [${_step}]: ${err instanceof Error ? err.message : String(err)}`);
});
