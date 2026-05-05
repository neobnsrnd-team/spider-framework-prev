/**
 * @file createDropdownMenu.ts
 * @description Figma DropdownMenu 컴포넌트 세트 생성.
 *
 * DropdownMenu/Item — Variant(default|danger) = 2 variants.
 * TEXT properties:
 *   - label — 메뉴 항목 레이블 (default: '메뉴 항목' / danger: '로그아웃')
 * INSTANCE_SWAP properties:
 *   - icon — 좌측 아이콘 (default: User / danger: LogOut)
 *
 * DropdownMenu — 열린 패널 상태만 표시, 단일 variant.
 * INSTANCE_SWAP properties:
 *   - triggerIcon — 상단 트리거 아이콘 (기본값: Settings)
 *
 * [레이아웃 - DropdownMenu/Item]
 *   comp(HORIZONTAL, gap=sm, padding=sm+md, FIXED 200×40)
 *     icon(16×16, INSTANCE_SWAP) ← iconBox 없이 comp 직접 자식으로 추가
 *     label(TEXT, grow=1)
 *
 * [레이아웃 - DropdownMenu]
 *   comp(VERTICAL, gap=0, FIXED 200, border+radius+surface)
 *     TriggerArea(HORIZONTAL, padding=xs+sm, right-align) — 트리거 아이콘 영역
 *       triggerIcon(20×20, INSTANCE_SWAP)
 *     Slot 'Items'(VERTICAL, FILL) — DropdownMenu/Item 인스턴스 추가·교체 가능
 *       [default 인스턴스] [구분선 1px] [danger 인스턴스]  ← 시각 프리뷰용
 *
 * React 대응: packages/component-library/modules/common/DropdownMenu
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFillWithVar, setStrokeWithVar, addTextWithVar, setFill,
  addIconSlot,
} from '../../../utils/helpers';

const ITEM_W    = 200; /* 드롭다운 패널 너비 (w-50 ≈ 200px) */
const ITEM_H    = 40;  /* 항목 행 높이 (h-10 = 40px) */
const ICON_SIZE = 16;  /* DropdownMenu/Item 아이콘 크기 */

type ItemVariant = 'default' | 'danger';

async function createDropdownMenuItemVariant(
  variant: ItemVariant,
): Promise<ComponentNode> {
  const comp = createComponent(`Variant=${variant}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.sm, SPACING.md);
  comp.resize(ITEM_W, ITEM_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* INSTANCE_SWAP: icon — iconBox 없이 comp 직접 자식으로 추가
   * iconBox(layoutMode=NONE)에 넣으면 auto-layout 흐름 밖에서 크기 제어가 불안정하여 제거 */
  const defaultIcon = variant === 'danger' ? 'LogOut' : 'User';
  const iconColor   = variant === 'danger' ? COLOR.danger : COLOR.textBase;
  addIconSlot(comp, defaultIcon, ICON_SIZE, iconColor, 'icon');

  /* TEXT property: label — comp 직접 자식 TextNode 자동 바인딩 */
  const textColorVar      = variant === 'danger' ? COLOR_VAR.danger : COLOR_VAR.textBase;
  const textColorFallback = variant === 'danger' ? COLOR.danger     : COLOR.textBase;
  const labelDefault      = variant === 'danger' ? '로그아웃'       : '메뉴 항목';
  const labelText = await addTextWithVar(
    comp, labelDefault, FONT_SIZE.sm,
    textColorVar, textColorFallback,
    false, SIZE_VAR.fontSizeSm, 'label',
  );
  labelText.layoutGrow = 1;

  return comp;
}

export async function createDropdownMenuItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createDropdownMenuItemVariant('default'),
      await createDropdownMenuItemVariant('danger'),
    ],
    'DropdownMenu/Item',
    2,
  );
}

async function createDropdownMenuVariant(
  itemSet: ComponentSetNode,
): Promise<ComponentNode> {
  const comp = createComponent('Style=Default');
  setAutoLayout(comp, 'VERTICAL', 0);
  comp.resize(ITEM_W, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.md;
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  comp.strokeWeight = 1;
  comp.strokeAlign = 'INSIDE';

  /* 트리거 영역 — Settings 아이콘이 우측에 표시되는 버튼 역할 */
  const triggerArea = figma.createFrame();
  triggerArea.name = 'TriggerArea';
  setAutoLayout(triggerArea, 'HORIZONTAL', 0, 'CENTER');
  triggerArea.primaryAxisAlignItems = 'MAX'; /* 아이콘을 우측 정렬 */
  setPadding(triggerArea, SPACING.xs, SPACING.sm);
  triggerArea.primaryAxisSizingMode = 'FIXED';
  triggerArea.counterAxisSizingMode = 'AUTO';
  clearFill(triggerArea);
  comp.appendChild(triggerArea);
  triggerArea.layoutSizingHorizontal = 'FILL'; /* append 이후 FILL 설정 */

  /* INSTANCE_SWAP: triggerIcon — TriggerArea 안에 배치, comp에 프로퍼티 등록 */
  addIconSlot(comp, 'Settings', 20, COLOR.textBase, 'triggerIcon', triggerArea);

  /* 구분선 (TriggerArea / Items 사이) */
  const triggerDivider = figma.createRectangle();
  triggerDivider.name = 'TriggerDivider';
  triggerDivider.resize(ITEM_W, 1);
  setFill(triggerDivider, COLOR.border);
  comp.appendChild(triggerDivider);

  /* Items 슬롯 — DropdownMenu/Item 인스턴스를 추가·교체 가능 */
  const slot = comp.createSlot();
  slot.name = 'Items';
  slot.layoutSizingHorizontal = 'FILL';
  slot.primaryAxisSizingMode = 'AUTO';
  (slot as any).layoutMode = 'VERTICAL';

  /* 시각 프리뷰용 인스턴스 사전 배치 */
  const defaultVariant = itemSet.children.find(
    c => c.name.includes('Variant=default'),
  ) as ComponentNode | undefined;
  const dangerVariant = itemSet.children.find(
    c => c.name.includes('Variant=danger'),
  ) as ComponentNode | undefined;

  if (defaultVariant) {
    slot.appendChild(defaultVariant.createInstance());
  }

  /* 구분선 (1px) */
  const divider = figma.createRectangle();
  divider.name = 'Divider';
  divider.resize(ITEM_W, 1);
  setFill(divider, COLOR.border);
  slot.appendChild(divider);

  if (dangerVariant) {
    slot.appendChild(dangerVariant.createInstance());
  }

  return comp;
}

export async function createDropdownMenu(
  itemSet: ComponentSetNode,
): Promise<ComponentSetNode> {
  return combineVariants(
    [await createDropdownMenuVariant(itemSet)],
    'DropdownMenu',
    1,
  );
}
