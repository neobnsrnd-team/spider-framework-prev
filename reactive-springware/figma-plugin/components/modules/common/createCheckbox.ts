/**
 * @file createCheckbox.ts
 * @description Figma Checkbox 컴포넌트 세트 생성.
 * React CheckboxProps의 shape(Square|Circle) × state(Unchecked|Checked|Disabled) = 6 variants.
 * 컴포넌트 이름: "Checkbox"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

type CheckboxShape = 'Square' | 'Circle';
type CheckboxState = 'Unchecked' | 'Checked' | 'Disabled';

async function createCheckboxVariant(shape: CheckboxShape, state: CheckboxState): Promise<ComponentNode> {
  const comp = createComponent(`Shape=${shape}, State=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 비활성: opacity-50 */
  if (state === 'Disabled') comp.opacity = 0.5;

  /* 체크박스 UI 프레임 (16×16) */
  const box = figma.createFrame();
  box.resize(16, 16);
  box.layoutMode = 'NONE';
  /* shape=Circle → radiusFull, shape=Square → radiusXs(4px) */
  if (shape === 'Circle') {
    await setFloatVar(box, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  } else {
    await setFloatVar(box, 'cornerRadius', SIZE_VAR.radiusXs, RADIUS.xs);
  }

  if (state === 'Checked') {
    /* 체크: 브랜드 배경 + 흰색 체크 아이콘 */
    await setFillWithVar(box, COLOR_VAR.brandText, BRAND.text);
    await setStrokeWithVar(box, COLOR_VAR.brandText, BRAND.text);
    const checkIcon = createIcon('Check', 10, { r: 1, g: 1, b: 1 });
    /* 체크 아이콘 중앙 배치 (16-10)/2 = 3 */
    checkIcon.x = 3;
    checkIcon.y = 3;
    box.appendChild(checkIcon);
  } else {
    /* Unchecked / Disabled: 흰 배경 + border */
    await setFillWithVar(box, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(box, COLOR_VAR.border, COLOR.border);
  }

  comp.appendChild(box);

  /* 우측 레이블 텍스트 */
  await addTextWithVar(
    comp, '레이블', FONT_SIZE.sm,
    COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm,
  );

  return comp;
}

export async function createCheckbox(): Promise<ComponentSetNode> {
  const shapes: CheckboxShape[] = ['Square', 'Circle'];
  const states: CheckboxState[] = ['Unchecked', 'Checked', 'Disabled'];
  const components: ComponentNode[] = [];
  for (const shape of shapes) {
    for (const state of states) {
      components.push(await createCheckboxVariant(shape, state));
    }
  }
  /* cols=3: State 3개가 한 행, 행마다 Shape 변경 */
  return combineVariants(components, 'Checkbox', 3);
}
