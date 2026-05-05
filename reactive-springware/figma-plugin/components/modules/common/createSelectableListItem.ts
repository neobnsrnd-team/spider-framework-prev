/**
 * @file createSelectableListItem.ts
 * @description Figma SelectableListItem 컴포넌트 세트 생성.
 * BottomSheet 내 단일 선택 행 컴포넌트.
 * IsSelected(True|False) = 2 variants.
 *
 * TEXT properties:
 *   - label — 항목 레이블 (기본값: '항목 레이블')
 *
 * 컴포넌트 이름: "SelectableListItem"
 */
import { BRAND, COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  addTextWithVar,
} from '../../../utils/helpers';

async function createSelectableListItemVariant(isSelected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`IsSelected=${isSelected ? 'True' : 'False'}`);
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, SPACING.lg, SPACING.md);
  comp.resize(390, 56);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* label TEXT property — comp 직접 자식, 자동 바인딩 */
  const label = await addTextWithVar(
    comp, '항목 레이블', FONT_SIZE.base,
    isSelected ? COLOR_VAR.brandPrimary : COLOR_VAR.textHeading,
    isSelected ? BRAND.primary         : COLOR.textHeading,
    isSelected, /* IsSelected=True: font-bold */
    SIZE_VAR.fontSizeBase, 'label',
  );
  label.layoutGrow = 1;

  return comp;
}

export async function createSelectableListItem(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createSelectableListItemVariant(false),
    createSelectableListItemVariant(true),
  ]);
  return combineVariants(components, 'SelectableListItem', 2);
}
