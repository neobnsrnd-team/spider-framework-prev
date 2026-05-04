/**
 * @file createSelectableListItem.ts
 * @description Figma SelectableListItem 컴포넌트 세트 생성.
 * BottomSheet 내 단일 선택 행 컴포넌트.
 * State(Default|Selected) = 2 variants.
 * 컴포넌트 이름: "SelectableListItem"
 */

import { BRAND, COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar,
} from '../../../helpers';

type SelectableListItemState = 'Default' | 'Selected';

async function createSelectableListItemVariant(state: SelectableListItemState): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, SPACING.lg, SPACING.md);
  comp.resize(390, 56);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* 하단 구분선 (border-border-subtle) */
  const divider = figma.createRectangle();
  divider.resize(390, 1);
  divider.fills = [{ type: 'SOLID', color: COLOR.borderSubtle }];
  /* 절대 위치로 하단에 배치 */

  const label = await addTextWithVar(
    comp,
    state === 'Selected' ? '선택된 항목' : '항목 레이블',
    FONT_SIZE.base,
    state === 'Selected' ? COLOR_VAR.brandPrimary : COLOR_VAR.textHeading,
    state === 'Selected' ? BRAND.primary         : COLOR.textHeading,
    state === 'Selected', /* Selected: font-bold */
    SIZE_VAR.fontSizeBase, 'label',
  );
  label.layoutGrow = 1;

  return comp;
}

export async function createSelectableListItem(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createSelectableListItemVariant('Default'),
    createSelectableListItemVariant('Selected'),
  ]);
  return combineVariants(components, 'SelectableListItem', 2);
}
