/**
 * @file createStatementHeroCard.ts
 * @description Figma StatementHeroCard 컴포넌트 세트 생성.
 * 카드 대시보드 상단 브랜드 배경 히어로 카드.
 * hidden(False|True) = 2 variants.
 *
 * TEXT properties:
 *   - label   — 카드 레이블 (기본값: '이번 달 명세서')
 *   - amount  — 금액 숫자만  (기본값: hidden=False '1,250,000' / hidden=True '금액 숨김 중')  hidden=False 시 고정 '원' 추가
 *   - dueDate — 결제일       (기본값: '12월 25일')  pill 안에 고정 '결제일: ' + dueDate
 *
 * 컴포넌트 이름: "StatementHeroCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING['2xl'] * 2; /* 양쪽 padding-2xl 제외 */

async function createStatementHeroVariant(hidden: boolean): Promise<ComponentNode> {
  const comp = createComponent(`hidden=${hidden ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING['2xl'], SPACING['2xl']);
  comp.resize(CARD_WIDTH, 1);             /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';    /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED';   /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  comp.effects = [{
    type: 'DROP_SHADOW',
    color: { r: BRAND.primary.r, g: BRAND.primary.g, b: BRAND.primary.b, a: 0.25 },
    offset: { x: 0, y: 8 }, radius: 24, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* label — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '이번 달 명세서', FONT_SIZE.sm, COLOR_VAR.brandFg, BRAND.fg, false, SIZE_VAR.fontSizeSm, 'label');

  if (!hidden) {
    /* hidden=False: 숫자(bound) + 고정 '원' — gap=0 HORIZONTAL 행 */
    const amountRow = figma.createFrame();
    setAutoLayout(amountRow, 'HORIZONTAL', 0);
    amountRow.primaryAxisAlignItems = 'MIN';
    amountRow.counterAxisAlignItems = 'CENTER';
    amountRow.resize(1, 1);               /* resize 먼저 */
    amountRow.primaryAxisSizingMode = 'AUTO';
    amountRow.counterAxisSizingMode = 'AUTO';
    clearFill(amountRow);
    comp.appendChild(amountRow);
    /* amount (숫자만) — amountRow → comp: 수동 바인딩 */
    await addTextWithVar(amountRow, '1,250,000', FONT_SIZE['4xl'], COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSize4xl, 'amount', comp);
    /* 고정 '원' */
    await addTextWithVar(amountRow, '원', FONT_SIZE['4xl'], COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSize4xl);
  } else {
    /* hidden=True: 숨김 안내 단독 텍스트 — comp 직접 자식: 자동 바인딩 */
    await addTextWithVar(comp, '금액 숨김 중', FONT_SIZE['4xl'], COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSize4xl, 'amount');
  }

  /* ── 하단 행: 결제일 pill + 화살표 ── */
  const bottomRow = figma.createFrame();
  setAutoLayout(bottomRow, 'HORIZONTAL', 0);
  bottomRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  bottomRow.counterAxisAlignItems = 'CENTER';
  setPadding(bottomRow, SPACING.lg, 0, 0, 0);
  bottomRow.resize(CONTENT_WIDTH, 1);     /* resize 먼저 */
  bottomRow.primaryAxisSizingMode = 'FIXED';
  bottomRow.counterAxisSizingMode = 'AUTO'; /* height 콘텐츠에 맞게 */
  clearFill(bottomRow);
  /* bottomRow → comp 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(bottomRow);

  /* 결제일 pill — dueDate TEXT property */
  const pill = figma.createFrame();
  setAutoLayout(pill, 'HORIZONTAL', 0);
  pill.primaryAxisAlignItems = 'CENTER';
  pill.counterAxisAlignItems = 'CENTER';
  setPadding(pill, SPACING.xs, SPACING.md);
  pill.resize(1, 1);                      /* resize 먼저 */
  pill.primaryAxisSizingMode = 'AUTO';    /* width 텍스트+padding에 맞게 */
  pill.counterAxisSizingMode = 'AUTO';    /* height 텍스트+padding에 맞게 */
  await setFloatVar(pill, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  pill.fills = [{ type: 'SOLID', color: BRAND.fg, opacity: 0.2 }];
  bottomRow.appendChild(pill);
  /* 고정 '결제일: ' */
  await addTextWithVar(pill, '결제일: ', FONT_SIZE.xs, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeXs);
  /* dueDate — pill → bottomRow → comp: 수동 바인딩 */
  await addTextWithVar(pill, '12월 25일', FONT_SIZE.xs, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeXs, 'dueDate', comp);

  bottomRow.appendChild(createIcon('ChevronRight', 16, BRAND.fg));

  return comp;
}

export async function createStatementHeroCard(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createStatementHeroVariant(false), await createStatementHeroVariant(true)],
    'StatementHeroCard', 1,
  );
}
