/**
 * @file createSidebarNav.ts
 * @description Figma SidebarNav 컴포넌트 세트 생성.
 *
 * SidebarNav/Item — State(default|selected) = 2 variants.
 * TEXT properties:
 *   - label — 항목 레이블 (기본값: '항목')
 *
 * [SidebarNav/Item 레이아웃]
 *   comp(HORIZONTAL, gap=standard, FIXED 117×56)
 *     Indicator(4px×56, BRAND.primary | transparent)
 *     label(TEXT, grow=1)
 *
 * SidebarNav — 단일 컴포넌트.
 * [SidebarNav 레이아웃]
 *   comp(VERTICAL, FIXED 117, AUTO, surfaceRaised)
 *     Items(Slot, VERTICAL, FILL) — SidebarNav/Item 인스턴스 추가·교체 가능
 *       [default × 3] + [selected × 1]  ← 시각 프리뷰용
 *
 * 컴포넌트 이름: "SidebarNav/Item", "SidebarNav"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const NAV_W   = 117;  /* 사이드바 너비 (w-[117px]) */
const ITEM_H  = 56;   /* 항목 높이 h-14 */

type ItemState = 'default' | 'selected';

async function createSidebarNavItemVariant(state: ItemState): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.standard, 'CENTER');
  comp.resize(NAV_W, ITEM_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';

  if (state === 'selected') {
    await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  } else {
    await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  }

  /* 좌측 인디케이터 바 (4px × ITEM_H) — selected: brand 색상, default: 투명 */
  const bar = figma.createFrame();
  bar.name = 'Indicator';
  bar.resize(4, ITEM_H);
  bar.primaryAxisSizingMode = 'FIXED';
  bar.counterAxisSizingMode = 'FIXED';
  await setFloatVar(bar, 'cornerRadius', SIZE_VAR.radiusXs, RADIUS.xs);
  if (state === 'selected') {
    await setFillWithVar(bar, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    clearFill(bar);
  }
  comp.appendChild(bar);

  /* label TEXT property — comp 직접 자식, 자동 바인딩 */
  const label = await addTextWithVar(
    comp, '항목', FONT_SIZE.sm,
    state === 'selected' ? COLOR_VAR.brandText      : COLOR_VAR.textSecondary,
    state === 'selected' ? BRAND.text               : COLOR.textSecondary,
    state === 'selected', /* selected: font-semibold */
    SIZE_VAR.fontSizeSm, 'label',
  );
  label.layoutGrow = 1;

  return comp;
}

export async function createSidebarNavItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createSidebarNavItemVariant('default'),
      await createSidebarNavItemVariant('selected'),
    ],
    'SidebarNav/Item',
    2,
  );
}

export async function createSidebarNav(
  itemSet: ComponentSetNode,
): Promise<ComponentNode> {
  const comp = createComponent('SidebarNav');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(NAV_W, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  /* 우측 구분선을 암시하는 배경 */
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);

  /* Items 슬롯 — SidebarNav/Item 인스턴스 추가·교체 가능 */
  const slot = comp.createSlot();
  slot.name = 'Items';
  slot.layoutSizingHorizontal = 'FILL';
  slot.primaryAxisSizingMode = 'AUTO';
  (slot as any).layoutMode    = 'VERTICAL';
  (slot as any).itemSpacing   = 0;

  /* 시각 프리뷰: default 3개 + selected 1개 */
  const defaultVariant  = itemSet.children.find(c => c.name.includes('State=default'))  as ComponentNode | undefined;
  const selectedVariant = itemSet.children.find(c => c.name.includes('State=selected')) as ComponentNode | undefined;

  const previewLabels = ['뱅킹', '카드', '보험', '전체'];
  for (let i = 0; i < previewLabels.length; i++) {
    const source = i === 1 ? selectedVariant : defaultVariant;
    if (!source) continue;
    const inst = source.createInstance();
    slot.appendChild(inst);
    inst.layoutSizingHorizontal = 'FILL';
  }

  return comp;
}
