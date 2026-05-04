/**
 * @file createSummaryCard.ts
 * @description Figma SummaryCard 컴포넌트 세트 생성.
 * 카드 도메인 요약 카드 (총 자산 / 이번달 지출).
 * Variant(Asset|Spending) = 2 variants.
 * - Asset:    브랜드 금액 색상, 왼쪽 액센트 바 없음
 * - Spending: 기본 헤딩 색상, 왼쪽 4px 도메인 액센트 바
 * 컴포넌트 이름: "SummaryCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

const VARIANT_CONFIG = {
  Asset: {
    title: '총 자산',
    amount: '42,850,000원',
    amountColorVar: COLOR_VAR.brandText,
    amountColor: BRAND.text,
    actions: ['내 계좌', '금융진단'],
    accentBar: false,
  },
  Spending: {
    title: '이번달 지출',
    amount: '1,250,000원',
    amountColorVar: COLOR_VAR.textHeading,
    amountColor: COLOR.textHeading,
    actions: ['내역', '분석'],
    accentBar: true,
  },
} as const;

async function createSummaryVariant(variant: 'Asset' | 'Spending'): Promise<ComponentNode> {
  const cfg = VARIANT_CONFIG[variant];

  const comp = createComponent(`Variant=${variant}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.lg, 'MIN');
  setPadding(comp, SPACING.xl, SPACING.xl);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);

  /* Spending variant: 왼쪽 4px 도메인 액센트 바 */
  if (cfg.accentBar) {
    /* border-l 효과는 Figma에서 직접 표현이 어려워 strokes로 근사 */
    comp.strokeLeftWeight = 4;
    comp.strokeRightWeight = 0;
    comp.strokeTopWeight = 0;
    comp.strokeBottomWeight = 0;
    comp.strokes = [{ type: 'SOLID', color: BRAND.alt }]; /* card accent color */
  }

  /* 상단: 제목 + 금액 / 우측 아이콘 */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', SPACING.md);
  topRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topRow.counterAxisAlignItems = 'MIN';
  topRow.layoutAlign = 'STRETCH';
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';
  topRow.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  clearFill(topRow);

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.layoutGrow = 1;
  clearFill(textArea);

  /* topRow/textArea를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(topRow);
  topRow.appendChild(textArea);
  await addTextWithVar(textArea, cfg.title, FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl, 'cardTitle', comp);
  await addTextWithVar(textArea, cfg.amount, FONT_SIZE.lg, cfg.amountColorVar, cfg.amountColor, true, SIZE_VAR.fontSizeLg, 'cardAmount', comp);

  /* 아이콘 원형 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.resize(48, 48);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  iconCircle.appendChild(createIcon(variant === 'Asset' ? 'Building2' : 'CreditCard', 24, BRAND.primary));
  topRow.appendChild(iconCircle);

  /* 하단: 액션 버튼 */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.sm);
  actionRow.layoutAlign = 'STRETCH';
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO';
  actionRow.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  clearFill(actionRow);

  for (const label of cfg.actions) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.layoutGrow = 1;
    btn.counterAxisSizingMode = 'AUTO';
    btn.primaryAxisSizingMode = 'FIXED';
    btn.resize(1, 44);
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(btn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXs);
    text.textAlignHorizontal = 'CENTER';
    text.layoutAlign = 'STRETCH';
    actionRow.appendChild(btn);
  }
  comp.appendChild(actionRow);

  return comp;
}

export async function createSummaryCard(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createSummaryVariant('Asset'), await createSummaryVariant('Spending')],
    'SummaryCard', 1,
  );
}
