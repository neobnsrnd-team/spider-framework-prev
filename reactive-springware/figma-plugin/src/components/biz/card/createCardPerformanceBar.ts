/**
 * @file createCardPerformanceBar.ts
 * @description Figma CardPerformanceBar 컴포넌트 세트 생성.
 * 카드 이용실적 진행 바 컴포넌트.
 * State(InProgress|Achieved) = 2 variants.
 * - InProgress: 50% 달성, "X원 더 이용하면 실적 달성" 안내
 * - Achieved:   100% 달성, "전월 실적 달성 완료" 표시
 * 컴포넌트 이름: "CardPerformanceBar"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addRect,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;
const BAR_HEIGHT = 8;

async function createPerformanceVariant(state: 'InProgress' | 'Achieved'): Promise<ComponentNode> {
  const isAchieved = state === 'Achieved';
  const percent = isAchieved ? 100 : 50;

  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm, 'MIN');
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);

  /* 상단: 카드명 + 상세 링크 */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', 0);
  topRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topRow.counterAxisAlignItems = 'CENTER';
  topRow.layoutAlign = 'STRETCH';
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';
  topRow.resize(CARD_WIDTH - SPACING.md * 2, 1);
  clearFill(topRow);
  await addTextWithVar(topRow, '하나 머니 체크카드', FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXs);

  const detailRow = figma.createFrame();
  setAutoLayout(detailRow, 'HORIZONTAL', SPACING.xs);
  detailRow.counterAxisAlignItems = 'CENTER';
  detailRow.primaryAxisSizingMode = 'AUTO';
  detailRow.counterAxisSizingMode = 'AUTO';
  clearFill(detailRow);
  await addTextWithVar(detailRow, '실적 상세', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  detailRow.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));
  topRow.appendChild(detailRow);
  comp.appendChild(topRow);

  /* 이용금액 / 목표금액 행 */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', 0);
  amountRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  amountRow.counterAxisAlignItems = 'MIN';
  amountRow.layoutAlign = 'STRETCH';
  amountRow.primaryAxisSizingMode = 'FIXED';
  amountRow.counterAxisSizingMode = 'AUTO';
  amountRow.resize(CARD_WIDTH - SPACING.md * 2, 1);
  clearFill(amountRow);
  await addTextWithVar(amountRow, isAchieved ? '300,000원' : '150,000원', FONT_SIZE.sm, isAchieved ? COLOR_VAR.brandText : COLOR_VAR.textHeading, isAchieved ? BRAND.text : COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  await addTextWithVar(amountRow, '목표 300,000원', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  comp.appendChild(amountRow);

  /* 진행 바 */
  const barBg = figma.createFrame();
  barBg.resize(CARD_WIDTH - SPACING.md * 2, BAR_HEIGHT);
  barBg.layoutMode = 'NONE';
  barBg.cornerRadius = RADIUS.full;
  await setFillWithVar(barBg, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);

  const fill = addRect(barBg, Math.round((CARD_WIDTH - SPACING.md * 2) * percent / 100), BAR_HEIGHT, BRAND.primary, RADIUS.full);
  fill.x = 0; fill.y = 0;
  comp.appendChild(barBg);

  /* 달성 안내 문구 */
  const statusText = isAchieved ? '✓ 전월 실적 달성 완료' : '150,000원 더 이용하면 실적 달성';
  await addTextWithVar(comp, statusText, FONT_SIZE.xs, isAchieved ? COLOR_VAR.brandText : COLOR_VAR.textMuted, isAchieved ? BRAND.text : COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  return comp;
}

export async function createCardPerformanceBar(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createPerformanceVariant('InProgress'), await createPerformanceVariant('Achieved')],
    'CardPerformanceBar', 1,
  );
}
