/**
 * @file createStatementHeroCard.ts
 * @description Figma StatementHeroCard 컴포넌트 세트 생성.
 * 카드 대시보드 상단 브랜드 배경 히어로 카드.
 * State(Visible|Hidden) = 2 variants.
 * 컴포넌트 이름: "StatementHeroCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

async function createStatementHeroVariant(state: 'Visible' | 'Hidden'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm, 'MIN');
  setPadding(comp, SPACING['2xl'], SPACING['2xl']);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  /* 브랜드 배경 */
  await setFillWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  comp.effects = [{
    type: 'DROP_SHADOW',
    color: { r: BRAND.primary.r, g: BRAND.primary.g, b: BRAND.primary.b, a: 0.25 },
    offset: { x: 0, y: 8 }, radius: 24, spread: 0, visible: true, blendMode: 'NORMAL',
  }];

  /* 우상단 장식 원 (Figma 글래스 오버레이 재현) */
  const decoCircle = figma.createEllipse();
  decoCircle.resize(160, 160);
  decoCircle.fills = [{ type: 'SOLID', color: BRAND.fg, opacity: 0.1 }];
  decoCircle.x = CARD_WIDTH - 80;  /* 우측 반만 보이도록 */
  decoCircle.y = -40;
  comp.appendChild(decoCircle);

  /* 레이블 */
  await addTextWithVar(comp, '이번 달 명세서', FONT_SIZE.sm, COLOR_VAR.brandFg, { ...BRAND.fg, r: 1, g: 1, b: 1 }, false, SIZE_VAR.fontSizeSm, 'cardLabel');

  /* 금액 + 원 단위 */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', SPACING.xs);
  amountRow.counterAxisAlignItems = 'MAX';
  amountRow.primaryAxisSizingMode = 'AUTO';
  amountRow.counterAxisSizingMode = 'AUTO';
  clearFill(amountRow);

  /* amountRow를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(amountRow);
  const amountText = state === 'Visible' ? '1,250,000' : '금액 숨김 중';
  await addTextWithVar(amountRow, amountText, FONT_SIZE['4xl'], COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSize4xl, 'amountValue', comp);
  if (state === 'Visible') {
    await addTextWithVar(amountRow, '원', FONT_SIZE.xl, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeXl, 'amountUnit', comp);
  }

  /* 결제일 pill + 화살표 */
  const bottomRow = figma.createFrame();
  setAutoLayout(bottomRow, 'HORIZONTAL', 0);
  bottomRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  bottomRow.counterAxisAlignItems = 'CENTER';
  setPadding(bottomRow, SPACING.lg, 0, 0, 0);
  bottomRow.layoutAlign = 'STRETCH';
  bottomRow.primaryAxisSizingMode = 'FIXED';
  bottomRow.counterAxisSizingMode = 'AUTO';
  bottomRow.resize(CARD_WIDTH - SPACING['2xl'] * 2, 1);
  clearFill(bottomRow);

  /* bottomRow → comp, pill → bottomRow 순서로 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(bottomRow);

  /* 결제일 pill */
  const pill = figma.createFrame();
  setAutoLayout(pill, 'HORIZONTAL', 0);
  setPadding(pill, SPACING.xs, SPACING.md);
  await setFloatVar(pill, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  pill.fills = [{ type: 'SOLID', color: BRAND.fg, opacity: 0.2 }];
  bottomRow.appendChild(pill);
  await addTextWithVar(pill, '결제일: 12월 25일', FONT_SIZE.xs, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeXs, 'paymentDateLabel', comp);

  bottomRow.appendChild(createIcon('ChevronRight', 16, BRAND.fg));

  return comp;
}

export async function createStatementHeroCard(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createStatementHeroVariant('Visible'), await createStatementHeroVariant('Hidden')],
    'StatementHeroCard', 1,
  );
}
