/**
 * @file createCardSummaryCard.ts
 * @description Figma CardSummaryCard 컴포넌트 세트 생성.
 * Type(Credit|Check|Prepaid) = 3 variants.
 * - Credit: 이번달 사용금액 + 한도 서브텍스트
 * - Check: 연결계좌 잔액
 * - Prepaid: 충전 잔액
 * 컴포넌트 이름: "CardSummaryCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

const CARD_WIDTH = 390;

type CardType = 'Credit' | 'Check' | 'Prepaid';

const AMOUNT_LABEL: Record<CardType, string> = {
  Credit:  '이번달 사용금액',
  Check:   '연결계좌 잔액',
  Prepaid: '충전 잔액',
};

const AMOUNT_VALUE: Record<CardType, string> = {
  Credit:  '480,000원',
  Check:   '1,234,567원',
  Prepaid: '50,000원',
};

async function createCardSummaryVariant(type: CardType): Promise<ComponentNode> {
  const comp = createComponent(`Type=${type}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);

  /* 상단: 카드명 + 배지 */
  const headerRow = figma.createFrame();
  setAutoLayout(headerRow, 'HORIZONTAL', SPACING.sm);
  headerRow.counterAxisAlignItems = 'CENTER';
  headerRow.layoutAlign = 'STRETCH';
  headerRow.primaryAxisSizingMode = 'FIXED';
  headerRow.counterAxisSizingMode = 'AUTO';
  headerRow.resize(CARD_WIDTH - SPACING.lg * 2, 1);
  clearFill(headerRow);

  /* headerRow를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(headerRow);

  const cardNameText = await addTextWithVar(headerRow, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName', comp);
  cardNameText.layoutGrow = 1;

  /* 배지 */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  setPadding(badge, 2, SPACING.sm);
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
  /* badge를 headerRow에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  headerRow.appendChild(badge);
  await addTextWithVar(badge, '포인트 적립', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs, 'badgeLabel', comp);

  /* 마스킹 카드번호 */
  await addTextWithVar(comp, '1234 **** **** 5678', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'maskedCardNumber');

  /* 금액 영역 */
  const amountSection = figma.createFrame();
  setAutoLayout(amountSection, 'VERTICAL', SPACING.xs, 'MIN');
  amountSection.layoutAlign = 'STRETCH';
  amountSection.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  amountSection.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  amountSection.resize(CARD_WIDTH - SPACING.lg * 2, 1);
  clearFill(amountSection);
  setPadding(amountSection, SPACING.md, 0);

  /* amountSection을 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(amountSection);

  await addTextWithVar(amountSection, AMOUNT_LABEL[type], FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'amountLabel', comp);
  await addTextWithVar(amountSection, AMOUNT_VALUE[type], FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl, 'amountValue', comp);

  /* Credit: 한도 서브텍스트 */
  if (type === 'Credit') {
    await addTextWithVar(amountSection, '한도 600,000원', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'creditLimit', comp);
  }

  /* 액션 버튼 영역 (텍스트로 대표) */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.xs);
  actionRow.layoutAlign = 'STRETCH';
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO';
  actionRow.resize(CARD_WIDTH - SPACING.lg * 2, 1);
  clearFill(actionRow);

  /* 결제내역 아웃라인 버튼 */
  const btnFrame = figma.createFrame();
  setAutoLayout(btnFrame, 'HORIZONTAL', 0);
  setPadding(btnFrame, SPACING.xs, SPACING.md);
  await setFloatVar(btnFrame, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  clearFill(btnFrame);
  await setStrokeWithVar(btnFrame, COLOR_VAR.border, COLOR.border);
  await addTextWithVar(btnFrame, '결제내역', FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeXs);
  actionRow.appendChild(btnFrame);
  comp.appendChild(actionRow);

  return comp;
}

export async function createCardSummaryCard(): Promise<ComponentSetNode> {
  const types: CardType[] = ['Credit', 'Check', 'Prepaid'];
  const components = await Promise.all(types.map(createCardSummaryVariant));
  return combineVariants(components, 'CardSummaryCard', 1);
}
