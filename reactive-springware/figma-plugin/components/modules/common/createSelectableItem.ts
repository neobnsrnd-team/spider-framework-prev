/**
 * @file createSelectableItem.ts
 * @description Figma SelectableItem 컴포넌트 세트 생성.
 * Selected(true|false) 2 variants.
 * 컴포넌트 이름: "SelectableItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setStroke, clearFill, clearStroke, addText, addIconSlot,
} from '../../../helpers';

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
    setFill(comp, BRAND.bg);
    setStroke(comp, BRAND.primary, 1.5);
  } else {
    setFill(comp, COLOR.surfaceSubtle);
    clearStroke(comp);
  }

  /* 아이콘 원형 배경 Frame — 이전의 createEllipse() 대체 */
  const iconWrap = figma.createFrame();
  setAutoLayout(iconWrap, 'HORIZONTAL', 0);
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.primaryAxisAlignItems = 'CENTER';
  iconWrap.counterAxisAlignItems = 'CENTER';
  /* RADIUS.full(9999)로 완전 원형 표현 */
  iconWrap.cornerRadius = RADIUS.full;
  setFill(iconWrap, selected ? BRAND.primary : COLOR.surfaceRaised);
  comp.appendChild(iconWrap);

  /* 아이콘 색: selected면 흰색(surface), 아니면 뮤트 텍스트 */
  const iconColor = selected ? COLOR.surface : COLOR.textMuted;
  addIconSlot(comp, 'Home', 24, iconColor, 'icon', iconWrap);

  await addText(comp, '항목', FONT_SIZE.xs, selected ? BRAND.text : COLOR.textBase, selected);
  return comp;
}

export async function createSelectableItem(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createSelectableVariant(false), createSelectableVariant(true)]),
    'SelectableItem', 2,
  );
}
