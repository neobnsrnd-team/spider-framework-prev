/**
 * @file createCheckbox.ts
 * @description Figma Checkbox 컴포넌트 세트 생성.
 * Shape(Square|Circle) × Checked(True|False) × Disabled(True|False) = 8 variants.
 *
 * TEXT properties:
 *   - label — 체크박스 우측 레이블 텍스트
 *
 * 시각 규칙:
 *   Checked=True  → 브랜드 배경 + 흰색 체크 아이콘
 *   Checked=False → 흰 배경 + border (빈 박스)
 *   Disabled=True → comp.opacity = 0.5 (Checked 여부와 무관)
 *
 * 컴포넌트 이름: "Checkbox"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

type CheckboxShape = 'Square' | 'Circle';

async function createCheckboxVariant(
  shape: CheckboxShape,
  checked: boolean,
  disabled: boolean,
): Promise<ComponentNode> {
  const comp = createComponent(
    `Shape=${shape}, Checked=${checked ? 'True' : 'False'}, Disabled=${disabled ? 'True' : 'False'}`,
  );
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  if (disabled) comp.opacity = 0.5; /* opacity-50 */

  /* 체크박스 UI 프레임 (16×16) */
  const box = figma.createFrame();
  box.resize(16, 16);
  box.layoutMode = 'NONE';
  /* Square → radiusXs(4px), Circle → radiusFull */
  if (shape === 'Circle') {
    await setFloatVar(box, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  } else {
    await setFloatVar(box, 'cornerRadius', SIZE_VAR.radiusXs, RADIUS.xs);
  }

  if (checked) {
    await setFillWithVar(box, COLOR_VAR.brandText, BRAND.text);
    await setStrokeWithVar(box, COLOR_VAR.brandText, BRAND.text);
    const checkIcon = createIcon('Check', 10, { r: 1, g: 1, b: 1 });
    checkIcon.x = 3; /* (16 - 10) / 2 = 3, 체크 아이콘 중앙 정렬 */
    checkIcon.y = 3;
    box.appendChild(checkIcon);
  } else {
    await setFillWithVar(box, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(box, COLOR_VAR.border, COLOR.border);
  }

  comp.appendChild(box);

  await addTextWithVar(
    comp, '레이블', FONT_SIZE.sm,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, SIZE_VAR.fontSizeSm, 'label',
  );

  return comp;
}

export async function createCheckbox(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (const shape of ['Square', 'Circle'] as CheckboxShape[]) {
    for (const checked of [false, true]) {
      for (const disabled of [false, true]) {
        components.push(await createCheckboxVariant(shape, checked, disabled));
      }
    }
  }
  /* cols=4: 한 행 = Shape 동일 × Checked(2) × Disabled(2) */
  return combineVariants(components, 'Checkbox', 4);
}
