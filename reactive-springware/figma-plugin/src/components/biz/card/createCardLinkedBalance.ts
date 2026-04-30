/**
 * @file createCardLinkedBalance.ts
 * @description Figma CardLinkedBalance 컴포넌트 세트 생성.
 * 연결계좌 잔액 표시 컴포넌트.
 * State(Visible|Hidden) = 2 variants.
 * 컴포넌트 이름: "CardLinkedBalance"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

async function createCardLinkedBalanceVariant(state: 'Visible' | 'Hidden'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  await addTextWithVar(comp, '사용가능 한도 금액', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* 금액 + 배지 버튼 */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', SPACING.xs);
  amountRow.counterAxisAlignItems = 'CENTER';
  amountRow.primaryAxisSizingMode = 'AUTO';
  amountRow.counterAxisSizingMode = 'AUTO';
  clearFill(amountRow);

  const amountText = state === 'Visible' ? '1,200,000원' : '잔액 숨김 중';
  await addTextWithVar(amountRow, amountText, FONT_SIZE['2xl'], COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSize2xl);

  /* 배지 버튼 (보기/숨기기) */
  const badgeBtn = figma.createFrame();
  setAutoLayout(badgeBtn, 'HORIZONTAL', 0);
  setPadding(badgeBtn, 2, SPACING.sm);
  await setFloatVar(badgeBtn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badgeBtn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setStrokeWithVar(badgeBtn, COLOR_VAR.border, COLOR.border);
  const badgeLabel = state === 'Visible' ? '숨기기' : '보기';
  await addTextWithVar(badgeBtn, badgeLabel, FONT_SIZE.xs, COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeXs);
  amountRow.appendChild(badgeBtn);

  comp.appendChild(amountRow);
  return comp;
}

export async function createCardLinkedBalance(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createCardLinkedBalanceVariant('Visible'), await createCardLinkedBalanceVariant('Hidden')],
    'CardLinkedBalance', 2,
  );
}
