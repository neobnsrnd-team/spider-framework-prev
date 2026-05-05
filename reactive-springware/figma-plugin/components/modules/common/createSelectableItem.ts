/**
 * @file createSelectableItem.ts
 * @description Figma SelectableItem 컴포넌트 세트 생성.
 * Selected(True|False) = 2 variants.
 *
 * TEXT properties:
 *   - label — 항목 레이블 (기본값: '항목')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 아이콘 (기본값: Home)
 *
 * 컴포넌트 이름: "SelectableItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setFillWithVar, setStroke, clearFill, clearStroke,
  addTextWithVar, addIconSlot,
} from '../../../utils/helpers';

async function createSelectableVariant(selected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Selected=${selected ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(100, 88);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  comp.cornerRadius = RADIUS.md;

  if (selected) {
    await setFillWithVar(comp, COLOR_VAR.brandBg, BRAND.bg);
    setStroke(comp, BRAND.primary, 1.5);
  } else {
    await setFillWithVar(comp, COLOR_VAR.surfaceSubtle, COLOR.surfaceSubtle);
    clearStroke(comp);
  }

  /* 아이콘 원형 배경 — comp.appendChild 후 addIconSlot 호출 */
  const iconWrap = figma.createFrame();
  iconWrap.name = 'IconWrap';
  setAutoLayout(iconWrap, 'HORIZONTAL', 0, 'CENTER');
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.cornerRadius = RADIUS.full;
  setFill(iconWrap, selected ? BRAND.primary : COLOR.surfaceRaised);
  comp.appendChild(iconWrap);

  /* icon INSTANCE_SWAP */
  const iconColor = selected ? COLOR.surface : COLOR.textMuted;
  addIconSlot(comp, 'Home', 24, iconColor, 'icon', iconWrap);

  /* label TEXT property — comp 직접 자식, 자동 바인딩 */
  await addTextWithVar(
    comp, '항목', FONT_SIZE.xs,
    selected ? COLOR_VAR.brandText : COLOR_VAR.textBase,
    selected ? BRAND.text         : COLOR.textBase,
    selected, SIZE_VAR.fontSizeXs, 'label',
  );

  return comp;
}

export async function createSelectableItem(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createSelectableVariant(false), createSelectableVariant(true)]),
    'SelectableItem',
    2,
  );
}
