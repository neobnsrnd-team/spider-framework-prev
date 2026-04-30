/**
 * @file createAmountInput.ts
 * @description Figma AmountInput 컴포넌트 세트 생성.
 * State(Default|Error) × 2 variants.
 * 컴포넌트 이름: "AmountInput"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, setStroke, clearFill, addText } from '../../../helpers';

async function createAmountInputVariant(state: 'Default' | 'Error'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm);
  setPadding(comp, 0, SPACING.standard);
  comp.resize(390, 140);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* 레이블 */
  await addText(comp, '이체 금액', FONT_SIZE.xs, COLOR.textLabel, true);

  /* 입력 필드 */
  const field = figma.createFrame();
  setAutoLayout(field, 'HORIZONTAL', SPACING.sm);
  setPadding(field, 0, SPACING.md);
  field.layoutAlign = 'STRETCH';
  field.resize(358, 56);
  field.primaryAxisSizingMode = 'FIXED';
  field.counterAxisSizingMode = 'FIXED';
  field.primaryAxisAlignItems = 'SPACE_BETWEEN';
  field.counterAxisAlignItems = 'CENTER';
  field.cornerRadius = RADIUS.sm;

  if (state === 'Error') {
    setFill(field, COLOR.dangerSurface);
    setStroke(field, COLOR.danger);
  } else {
    setFill(field, COLOR.surface);
    setStroke(field, COLOR.border);
  }

  const amount = await addText(field, '0', FONT_SIZE.xl, state === 'Error' ? COLOR.danger : COLOR.textHeading, true);
  amount.layoutGrow = 1;
  await addText(field, '원', FONT_SIZE.base, COLOR.textSecondary);
  comp.appendChild(field);

  /* 빠른 금액 버튼 행 */
  const quickRow = figma.createFrame();
  setAutoLayout(quickRow, 'HORIZONTAL', SPACING.xs);
  quickRow.layoutAlign = 'STRETCH';
  quickRow.fills = [];
  for (const label of ['+ 1만', '+ 10만', '+ 100만', '전액']) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.layoutGrow = 1;
    btn.resize(80, 32);
    btn.cornerRadius = RADIUS.sm;
    setFill(btn, COLOR.surfaceRaised);
    await addText(btn, label, FONT_SIZE.xs, COLOR.textSecondary);
    quickRow.appendChild(btn);
  }
  comp.appendChild(quickRow);

  if (state === 'Error') {
    await addText(comp, '이체 한도를 초과했습니다.', FONT_SIZE.xs, COLOR.dangerText);
  }

  return comp;
}

export async function createAmountInput(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createAmountInputVariant('Default'), createAmountInputVariant('Error')]),
    'AmountInput', 2,
  );
}
