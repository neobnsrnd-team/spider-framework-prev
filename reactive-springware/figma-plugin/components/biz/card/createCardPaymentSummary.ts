/**
 * @file createCardPaymentSummary.ts
 * @description Figma CardPaymentSummary 컴포넌트 생성.
 * 카드 이용내역 화면 상단 청구 요약 카드.
 * 총 청구금액(브랜드 컬러) + 세부 3열(리볼빙·카드론·현금서비스) + 결제 계좌/날짜.
 * 단일 variant.
 * 컴포넌트 이름: "CardPaymentSummary"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

export async function createCardPaymentSummary(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  comp.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 }, offset: { x: 0, y: 2 }, radius: 8, spread: 0, visible: true, blendMode: 'NORMAL' }];

  /* 상단: 총 청구금액 */
  const top = figma.createFrame();
  setAutoLayout(top, 'VERTICAL', SPACING.xs);
  setPadding(top, SPACING.md, SPACING.md, SPACING.lg, SPACING.md);
  top.layoutAlign = 'STRETCH';
  top.primaryAxisSizingMode = 'FIXED';
  top.counterAxisSizingMode = 'AUTO';
  top.resize(CARD_WIDTH, 1);
  clearFill(top);

  /* 년월 행 (ChevronDown) */
  const monthRow = figma.createFrame();
  setAutoLayout(monthRow, 'HORIZONTAL', SPACING.xs);
  monthRow.counterAxisAlignItems = 'CENTER';
  monthRow.primaryAxisSizingMode = 'AUTO';
  monthRow.counterAxisSizingMode = 'AUTO';
  clearFill(monthRow);
  await addTextWithVar(monthRow, '2026년 4월', FONT_SIZE.lg, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeLg);
  monthRow.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));
  top.appendChild(monthRow);

  await addTextWithVar(top, '2026.04.25 출금예정 (04.12기준)', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);
  await addTextWithVar(top, '350,000원', FONT_SIZE['3xl'], COLOR_VAR.brandPrimary, BRAND.primary, true, SIZE_VAR.fontSize3xl);
  comp.appendChild(top);

  /* 하단: 세부 3열 */
  const bottom = figma.createFrame();
  setAutoLayout(bottom, 'HORIZONTAL', 0);
  bottom.counterAxisAlignItems = 'MIN';
  setPadding(bottom, SPACING.lg, 0, SPACING.md, 0);
  bottom.layoutAlign = 'STRETCH';
  bottom.primaryAxisSizingMode = 'FIXED';
  bottom.counterAxisSizingMode = 'AUTO';
  bottom.resize(CARD_WIDTH, 1);
  clearFill(bottom);

  const colDefs = [
    { label: '일부결제금액\n이월약정(리볼빙)', amount: '0원' },
    { label: '장기카드대출\n(카드론)',         amount: '0원' },
    { label: '단기카드대출\n(현금서비스)',     amount: '0원' },
  ];

  for (let i = 0; i < colDefs.length; i++) {
    const col = figma.createFrame();
    setAutoLayout(col, 'VERTICAL', SPACING.xs);
    setPadding(col, 0, SPACING.xs);
    col.counterAxisAlignItems = 'CENTER';
    col.layoutGrow = 1;
    col.primaryAxisSizingMode = 'FIXED';
    col.counterAxisSizingMode = 'AUTO';
    col.resize(1, 1);
    clearFill(col);

    /* 중간 열 border-x */
    if (i === 1) {
      await setStrokeWithVar(col, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
      col.strokeTopWeight = 0; col.strokeBottomWeight = 0;
      col.strokeLeftWeight = 1; col.strokeRightWeight = 1;
    }

    const labelText = await addTextWithVar(col, colDefs[i].label, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    labelText.textAlignHorizontal = 'CENTER';
    bottom.appendChild(col);
  }

  comp.appendChild(bottom);

  figma.currentPage.appendChild(comp);
  return comp;
}
