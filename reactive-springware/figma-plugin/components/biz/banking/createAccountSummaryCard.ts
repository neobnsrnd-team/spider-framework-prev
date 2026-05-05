/**
 * @file createAccountSummaryCard.ts
 * @description Figma AccountSummaryCard 컴포넌트 세트 생성.
 * Type(6) × MoreButton(3) = 18 variants.
 *
 * Type:
 *   Deposit / Savings / Loan / ForeignDeposit / Retirement / Securities
 *
 * MoreButton:
 *   None     — 더보기 버튼 없음
 *   Chevron  — ChevronRight 아이콘
 *   Ellipsis — MoreHorizontal 아이콘
 *
 * TEXT properties:
 *   - accountName   — 계좌명 (type별 기본값)
 *   - accountNumber — 계좌번호
 *   - balanceLabel  — 금액 레이블 (type별 기본값)
 *   - balance       — 금액 (type별 기본값)
 *   - badgeText     — 배지 텍스트
 *
 * Actions (하단 버튼 슬롯):
 *   Deposit / ForeignDeposit: '거래내역'(outline) + '이체'(primary fill) 버튼 노출
 *   나머지 type: 미노출
 *
 * [레이아웃 — 실제 컴포넌트 기준]
 *   comp(VERTICAL, gap=standard, p-lg, FIXED 390×AUTO, radiusXl, surface bg, borderSubtle)
 *     topSection(VERTICAL, gap=xs, FILL)
 *       headingRow(HORIZONTAL, SPACE_BETWEEN, CENTER, FILL)
 *         leftInfo(HORIZONTAL, gap=sm, CENTER, FILL)
 *           accountName(TEXT sm bold, textHeading, FILL, truncate)
 *           badge(px-sm py-0.5, radiusFull, brandBg)
 *             badgeText(TEXT xs bold, brandText)
 *         moreBtn(24×24, optional) — Chevron/Ellipsis
 *       accountNumber(TEXT xs, textMuted, FILL)
 *     balanceRow(HORIZONTAL, SPACE_BETWEEN, CENTER, FILL)
 *       balanceLabel(TEXT sm, textSecondary)
 *       balance(TEXT xl bold, textHeading|danger)
 *     actionsRow(HORIZONTAL, gap=sm, FILL) — Deposit/ForeignDeposit만
 *       btn거래내역(FILL, h=40, radiusMd, outline=brandPrimary, brandText)
 *       btn이체(FILL, h=40, radiusMd, fill=brandPrimary, brandFg)
 *
 * TEXT property 바인딩 타이밍:
 *   accountName  — comp → topSection → headingRow → leftInfo → text: 수동 바인딩
 *   badgeText    — comp → topSection → headingRow → leftInfo → badge → text: 수동 바인딩
 *   accountNumber — comp → topSection → text: 수동 바인딩
 *   balanceLabel — comp → balanceRow → text: 수동 바인딩
 *   balance      — comp → balanceRow → text: 수동 바인딩
 *
 * 컴포넌트 이름: "AccountSummaryCard"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

type RGB = { r: number; g: number; b: number };
type AccountType    = 'Deposit' | 'Savings' | 'Loan' | 'ForeignDeposit' | 'Retirement' | 'Securities';
type MoreButtonType = 'None' | 'Chevron' | 'Ellipsis';

const CARD_WIDTH = 390;

type TypeConfig = {
  defaultAccountName:   string;
  defaultBalanceLabel:  string;
  defaultBalance:       string;
  balanceColorVar:      string;
  balanceColorFallback: RGB;
  showActions:          boolean; /* Deposit/ForeignDeposit만 하단 버튼 노출 */
};

const TYPE_CONFIG: Record<AccountType, TypeConfig> = {
  Deposit:        { defaultAccountName: '하나 급여통장',           defaultBalanceLabel: '잔액',     defaultBalance: '1,234,567원',   balanceColorVar: COLOR_VAR.textHeading, balanceColorFallback: COLOR.textHeading, showActions: true  },
  Savings:        { defaultAccountName: '청약저축',               defaultBalanceLabel: '납입금액', defaultBalance: '5,000,000원',   balanceColorVar: COLOR_VAR.textHeading, balanceColorFallback: COLOR.textHeading, showActions: false },
  Loan:           { defaultAccountName: '신용대출',               defaultBalanceLabel: '대출잔액', defaultBalance: '30,000,000원',  balanceColorVar: COLOR_VAR.danger,      balanceColorFallback: COLOR.danger,      showActions: false },
  ForeignDeposit: { defaultAccountName: '외화 다통화 예금',        defaultBalanceLabel: '잔액',     defaultBalance: '$1,000.00',    balanceColorVar: COLOR_VAR.textHeading, balanceColorFallback: COLOR.textHeading, showActions: true  },
  Retirement:     { defaultAccountName: '확정기여형(DC) 퇴직연금', defaultBalanceLabel: '적립금',   defaultBalance: '12,000,000원', balanceColorVar: COLOR_VAR.textHeading, balanceColorFallback: COLOR.textHeading, showActions: false },
  Securities:     { defaultAccountName: '하나 증권계좌',           defaultBalanceLabel: '평가금액', defaultBalance: '8,750,000원',  balanceColorVar: COLOR_VAR.textHeading, balanceColorFallback: COLOR.textHeading, showActions: false },
};

async function createAccountSummaryVariant(
  type: AccountType,
  moreButton: MoreButtonType,
): Promise<ComponentNode> {
  const cfg = TYPE_CONFIG[type];

  const comp = createComponent(`Type=${type}, MoreButton=${moreButton}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.standard);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign = 'INSIDE';

  /* ── topSection: headingRow(계좌명+배지+더보기) + 계좌번호 ── */
  const topSection = figma.createFrame();
  setAutoLayout(topSection, 'VERTICAL', SPACING.xs);
  topSection.primaryAxisSizingMode = 'AUTO';
  clearFill(topSection);
  comp.appendChild(topSection);
  topSection.layoutSizingHorizontal = 'FILL';

  /* headingRow: leftInfo / moreBtn */
  const headingRow = figma.createFrame();
  setAutoLayout(headingRow, 'HORIZONTAL', SPACING.sm);
  headingRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  headingRow.counterAxisAlignItems = 'CENTER';
  headingRow.primaryAxisSizingMode = 'AUTO';
  headingRow.counterAxisSizingMode = 'AUTO'; /* height 콘텐츠에 맞게 */
  clearFill(headingRow);
  topSection.appendChild(headingRow);
  headingRow.layoutSizingHorizontal = 'FILL';

  /* leftInfo: accountName + badge (왼쪽 정렬, 오버플로우 클리핑) */
  const leftInfo = figma.createFrame();
  setAutoLayout(leftInfo, 'HORIZONTAL', SPACING.sm);
  leftInfo.counterAxisAlignItems = 'CENTER';
  leftInfo.primaryAxisAlignItems = 'MIN'; /* 자식을 왼쪽부터 배치 */
  leftInfo.primaryAxisSizingMode = 'AUTO';
  leftInfo.counterAxisSizingMode = 'AUTO'; /* height 콘텐츠에 맞게 */
  leftInfo.clipsContent = true; /* accountName이 길어도 badge가 밖으로 나가지 않게 */
  clearFill(leftInfo);
  headingRow.appendChild(leftInfo);
  leftInfo.layoutSizingHorizontal = 'FILL'; /* moreBtn을 우측으로 밀어냄 */

  /* accountName — FILL 미사용: badge가 accountName 바로 옆에 붙도록 AUTO 너비 유지 */
  const accountNameNode = await addTextWithVar(
    leftInfo, cfg.defaultAccountName, FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );
  accountNameNode.textAlignHorizontal = 'LEFT';
  const accountNameKey = comp.addComponentProperty('accountName', 'TEXT', cfg.defaultAccountName);
  accountNameNode.componentPropertyReferences = { characters: accountNameKey };

  /* badge */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  setPadding(badge, 2, SPACING.sm); /* py-0.5 px-sm */
  badge.primaryAxisSizingMode = 'AUTO';
  badge.counterAxisSizingMode = 'AUTO';
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
  leftInfo.appendChild(badge);

  const badgeTextNode = await addTextWithVar(
    badge, '주거래', FONT_SIZE.xs,
    COLOR_VAR.brandText, BRAND.text,
    true, SIZE_VAR.fontSizeXs,
  );
  const badgeTextKey = comp.addComponentProperty('badgeText', 'TEXT', '주거래');
  badgeTextNode.componentPropertyReferences = { characters: badgeTextKey };

  /* moreBtn — Chevron / Ellipsis 시에만 표시 */
  if (moreButton !== 'None') {
    const moreBtnFrame = figma.createFrame();
    setAutoLayout(moreBtnFrame, 'HORIZONTAL', 0);
    moreBtnFrame.resize(24, 24);
    moreBtnFrame.primaryAxisSizingMode = 'FIXED';
    moreBtnFrame.counterAxisSizingMode = 'FIXED';
    moreBtnFrame.primaryAxisAlignItems = 'CENTER';
    moreBtnFrame.counterAxisAlignItems = 'CENTER';
    clearFill(moreBtnFrame);
    const iconName = moreButton === 'Chevron' ? 'ChevronRight' : 'MoreHorizontal';
    moreBtnFrame.appendChild(createIcon(iconName, 16, COLOR.textMuted));
    headingRow.appendChild(moreBtnFrame);
  }

  /* accountNumber — topSection 직접 자식: 수동 바인딩 */
  const accountNumberNode = await addTextWithVar(
    topSection, '123-456789-01234', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  accountNumberNode.layoutSizingHorizontal = 'FILL';
  accountNumberNode.textAlignHorizontal = 'LEFT';
  const accountNumberKey = comp.addComponentProperty('accountNumber', 'TEXT', '123-456789-01234');
  accountNumberNode.componentPropertyReferences = { characters: accountNumberKey };

  /* ── balanceRow: 레이블(좌) / 금액(우) ── */
  const balanceRow = figma.createFrame();
  setAutoLayout(balanceRow, 'HORIZONTAL', SPACING.xs);
  balanceRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  balanceRow.counterAxisAlignItems = 'CENTER';
  balanceRow.primaryAxisSizingMode = 'AUTO';
  balanceRow.counterAxisSizingMode = 'AUTO'; /* height 콘텐츠에 맞게 */
  clearFill(balanceRow);
  comp.appendChild(balanceRow);
  balanceRow.layoutSizingHorizontal = 'FILL';

  /* balanceLabel — TEXT sm, textSecondary (실제 컴포넌트: #475569) */
  const balanceLabelNode = await addTextWithVar(
    balanceRow, cfg.defaultBalanceLabel, FONT_SIZE.sm,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, SIZE_VAR.fontSizeSm,
  );
  balanceLabelNode.textAlignHorizontal = 'LEFT';
  const balanceLabelKey = comp.addComponentProperty('balanceLabel', 'TEXT', cfg.defaultBalanceLabel);
  balanceLabelNode.componentPropertyReferences = { characters: balanceLabelKey };

  /* balance — TEXT xl bold */
  const balanceNode = await addTextWithVar(
    balanceRow, cfg.defaultBalance, FONT_SIZE.xl,
    cfg.balanceColorVar, cfg.balanceColorFallback,
    true, SIZE_VAR.fontSizeXl,
  );
  balanceNode.textAlignHorizontal = 'RIGHT';
  const balanceKey = comp.addComponentProperty('balance', 'TEXT', cfg.defaultBalance);
  balanceNode.componentPropertyReferences = { characters: balanceKey };

  /* ── actionsRow — Deposit / ForeignDeposit만 노출 ── */
  if (cfg.showActions) {
    const actionsRow = figma.createFrame();
    setAutoLayout(actionsRow, 'HORIZONTAL', SPACING.sm);
    clearFill(actionsRow);
    comp.appendChild(actionsRow);
    actionsRow.layoutSizingHorizontal = 'FILL';

    /* btn1: 거래내역 — outline (border=brandPrimary, 투명 배경) */
    const btn1 = figma.createFrame();
    setAutoLayout(btn1, 'HORIZONTAL', 0);
    btn1.primaryAxisAlignItems = 'CENTER';
    btn1.counterAxisAlignItems = 'CENTER';
    btn1.resize(1, 40);                  /* height 먼저 — AUTO 설정 전에 resize */
    btn1.primaryAxisSizingMode = 'AUTO';
    btn1.counterAxisSizingMode = 'FIXED';
    clearFill(btn1);
    await setStrokeWithVar(btn1, COLOR_VAR.brandPrimary, BRAND.primary);
    btn1.strokeWeight = 1;
    btn1.strokeAlign = 'INSIDE';
    await setFloatVar(btn1, 'cornerRadius', SIZE_VAR.radiusMd, RADIUS.md);
    await addTextWithVar(btn1, '거래내역', FONT_SIZE.sm, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeSm);
    actionsRow.appendChild(btn1);
    btn1.layoutSizingHorizontal = 'FILL'; /* 두 버튼이 균등 너비로 채움 */

    /* btn2: 이체 — primary fill */
    const btn2 = figma.createFrame();
    setAutoLayout(btn2, 'HORIZONTAL', 0);
    btn2.primaryAxisAlignItems = 'CENTER';
    btn2.counterAxisAlignItems = 'CENTER';
    btn2.resize(1, 40);
    btn2.primaryAxisSizingMode = 'AUTO';
    btn2.counterAxisSizingMode = 'FIXED';
    await setFillWithVar(btn2, COLOR_VAR.brandPrimary, BRAND.primary);
    await setFloatVar(btn2, 'cornerRadius', SIZE_VAR.radiusMd, RADIUS.md);
    await addTextWithVar(btn2, '이체', FONT_SIZE.sm, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeSm);
    actionsRow.appendChild(btn2);
    btn2.layoutSizingHorizontal = 'FILL';
  }

  return comp;
}

export async function createAccountSummaryCard(): Promise<ComponentSetNode> {
  const types: AccountType[]          = ['Deposit', 'Savings', 'Loan', 'ForeignDeposit', 'Retirement', 'Securities'];
  const moreButtons: MoreButtonType[] = ['None', 'Chevron', 'Ellipsis'];

  const components: ComponentNode[] = [];
  for (const type of types) {
    for (const mb of moreButtons) {
      components.push(await createAccountSummaryVariant(type, mb));
    }
  }

  /* cols=3: MoreButton(None|Chevron|Ellipsis) 기준으로 열 정렬, Type별 행 구분 */
  return combineVariants(components, 'AccountSummaryCard', 3);
}
