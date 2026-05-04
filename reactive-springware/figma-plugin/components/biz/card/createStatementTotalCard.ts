/**
 * @file createStatementTotalCard.ts
 * @description Figma StatementTotalCard 컴포넌트 생성.
 * 총 결제금액 + "예정" 배지 + 이용내역 화살표 + 3개 액션 버튼.
 * 단일 variant.
 * 컴포넌트 이름: "StatementTotalCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

export async function createStatementTotalCard(): Promise<ComponentNode> {
  const comp = createComponent('StatementTotalCard');
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'MIN');
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  comp.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 }, offset: { x: 0, y: 2 }, radius: 8, spread: 0, visible: true, blendMode: 'NORMAL' }];

  /* 레이블 + "예정" 배지 */
  const labelRow = figma.createFrame();
  setAutoLayout(labelRow, 'HORIZONTAL', SPACING.xs);
  labelRow.counterAxisAlignItems = 'CENTER';
  labelRow.primaryAxisSizingMode = 'AUTO';
  labelRow.counterAxisSizingMode = 'AUTO';
  clearFill(labelRow);
  /* labelRow → comp, badge → labelRow 순서로 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(labelRow);
  await addTextWithVar(labelRow, '총 결제금액', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm, 'sectionLabel', comp);
  /* 배지 */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  setPadding(badge, 2, SPACING.sm);
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
  labelRow.appendChild(badge);
  await addTextWithVar(badge, '예정', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs, 'statusBadge', comp);

  /* 금액 + 화살표 */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', 0);
  amountRow.counterAxisAlignItems = 'CENTER';
  amountRow.primaryAxisSizingMode = 'AUTO';
  amountRow.counterAxisSizingMode = 'AUTO';
  clearFill(amountRow);
  /* amountRow를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(amountRow);
  await addTextWithVar(amountRow, '350,000원', FONT_SIZE['3xl'], COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSize3xl, 'totalAmount', comp);
  amountRow.appendChild(createIcon('ChevronRight', 24, COLOR.textMuted));

  /* 3개 액션 버튼 */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.xs);
  actionRow.layoutAlign = 'STRETCH';
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO';
  actionRow.resize(CARD_WIDTH - SPACING.md * 2, 1);
  clearFill(actionRow);

  for (const label of ['분할납부', '즉시결제', '리볼빙']) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.layoutGrow = 1;
    btn.counterAxisSizingMode = 'AUTO';
    btn.primaryAxisSizingMode = 'FIXED';
    btn.resize(1, 36);
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
    clearFill(btn);
    await setStrokeWithVar(btn, COLOR_VAR.border, COLOR.border);
    /* btn을 actionRow에 먼저 추가해야 TEXT property reference 바인딩 가능 */
    actionRow.appendChild(btn);
    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeXs);
    text.textAlignHorizontal = 'CENTER';
    text.layoutAlign = 'STRETCH';
  }
  comp.appendChild(actionRow);

  figma.currentPage.appendChild(comp);
  return comp;
}
