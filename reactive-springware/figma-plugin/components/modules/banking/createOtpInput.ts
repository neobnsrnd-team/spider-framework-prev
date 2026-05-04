/**
 * @file createOtpInput.ts
 * @description Figma OtpInput 컴포넌트 세트 생성.
 * Length(4|6) × State(Default|Error) = 4 variants.
 * 컴포넌트 이름: "OtpInput"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, clearFill, setFill, setStroke, addText } from '../../../helpers';

async function createOtpVariant(length: 4 | 6, state: 'Default' | 'Error'): Promise<ComponentNode> {
  const cellSize = 44;
  const totalWidth = length * cellSize + (length - 1) * SPACING.sm;

  const comp = createComponent(`Length=${length}, State=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.resize(totalWidth, cellSize);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  for (let i = 0; i < length; i++) {
    const cell = figma.createFrame();
    cell.resize(cellSize, cellSize);
    cell.cornerRadius = RADIUS.sm;
    cell.primaryAxisSizingMode = 'FIXED';
    cell.counterAxisSizingMode = 'FIXED';
    setAutoLayout(cell, 'HORIZONTAL', 0);
    cell.primaryAxisAlignItems = 'CENTER';
    cell.counterAxisAlignItems = 'CENTER';

    const isActive = i === 0; // 첫 번째 셀을 활성 상태로 표현
    if (state === 'Error') {
      setFill(cell, COLOR.dangerSurface);
      setStroke(cell, COLOR.danger);
    } else if (isActive) {
      setFill(cell, COLOR.surface);
      setStroke(cell, BRAND.primary);
    } else {
      setFill(cell, COLOR.surfaceSubtle);
      setStroke(cell, COLOR.border);
    }

    await addText(cell, i === 0 ? '●' : '', FONT_SIZE.base, state === 'Error' ? COLOR.danger : COLOR.textHeading, true);
    comp.appendChild(cell);
  }

  return comp;
}

export async function createOtpInput(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createOtpVariant(4, 'Default'),
    createOtpVariant(4, 'Error'),
    createOtpVariant(6, 'Default'),
    createOtpVariant(6, 'Error'),
  ]);
  return combineVariants(components, 'OtpInput', 2);
}
