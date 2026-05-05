/**
 * @file createQuickMenuGrid.ts
 * @description Figma QuickMenuGrid 컴포넌트 세트 생성.
 *
 * 1. QuickMenuGrid/Item — variant 없는 단일 ComponentNode
 *    TEXT properties:
 *      - label — 메뉴 레이블 (기본값: '메뉴')
 *    INSTANCE_SWAP properties:
 *      - icon  — 메뉴 아이콘 (기본값: LayoutGrid, swap 가능)
 *
 * 2. QuickMenuGrid — Cols(2|3|4) = 3 variants
 *    슬롯(HORIZONTAL WRAP)에 QuickMenuGrid/Item 인스턴스를 기본 배치.
 *    열 수(cols)에 맞게 셀 너비를 자동 조정해 WRAP이 정확히 N열로 동작한다.
 *
 * [주의] createQuickMenuGridItem()을 먼저 실행한 뒤 반환값을 createQuickMenuGrid()에 전달해야 한다.
 *
 * @param item - createQuickMenuGridItem()이 반환한 ComponentNode
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

const ITEM_HEIGHT = 88;  /* py-md(12) + icon(48) + gap-xs(4) + label(12) + py-md(12) */
const ICON_BOX_SIZE = 48; /* size-12 */
const ITEM_GAP = SPACING.sm;

/** 총 그리드 너비 (2/3/4열 모두 동일하게 맞춤) */
const GRID_WIDTH = 376;

/** 열 수별 셀 너비: cols * cellWidth + (cols-1) * gap = GRID_WIDTH */
const CELL_WIDTH: Record<2 | 3 | 4, number> = {
  4: 88,  /* 4*88 + 3*8 = 376 */
  3: 120, /* 3*120 + 2*8 = 376 */
  2: 184, /* 2*184 + 1*8 = 376 */
};

/* ── QuickMenuGrid/Item ───────────────────────────────────── */

export async function createQuickMenuGridItem(): Promise<ComponentNode> {
  const comp = createComponent('QuickMenuGrid/Item');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs);
  setPadding(comp, SPACING.md, 0); /* py-md px-0 */
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(CELL_WIDTH[4], ITEM_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* 아이콘 컨테이너 — size-12(48px), rounded-full, brandPrimary5 bg */
  const iconBox = figma.createFrame();
  setAutoLayout(iconBox, 'HORIZONTAL', 0);
  iconBox.resize(ICON_BOX_SIZE, ICON_BOX_SIZE);
  iconBox.primaryAxisSizingMode = 'FIXED';
  iconBox.counterAxisSizingMode = 'FIXED';
  iconBox.primaryAxisAlignItems = 'CENTER';
  iconBox.counterAxisAlignItems = 'CENTER';
  await setFloatVar(iconBox, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  /* bg-brand-5: brandPrimary 5% 배경 (fallback: BRAND.bg ≈ brand/10) */
  await setFillWithVar(iconBox, COLOR_VAR.brandPrimary5, BRAND.bg);
  comp.appendChild(iconBox);

  /* INSTANCE_SWAP: icon (기본값 LayoutGrid, Figma 패널에서 교체 가능) */
  addIconSlot(comp, 'LayoutGrid', 24, BRAND.text, 'icon', iconBox);

  /* label — comp 직접 자식: 자동 바인딩 */
  const labelNode = await addTextWithVar(
    comp, '메뉴', FONT_SIZE.xs,
    COLOR_VAR.textBase, COLOR.textBase,
    true, SIZE_VAR.fontSizeXs, 'label',
  );
  labelNode.textAlignHorizontal = 'CENTER';

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── QuickMenuGrid ────────────────────────────────────────── */

async function createGridVariant(
  item: ComponentNode,
  cols: 2 | 3 | 4,
): Promise<ComponentNode> {
  const cellWidth = CELL_WIDTH[cols];
  const rows      = 2; /* 기본 2행 표시 */
  const gridHeight = rows * ITEM_HEIGHT + (rows - 1) * ITEM_GAP;

  const comp = createComponent(`Cols=${cols}`);
  /* NONE layout으로 슬롯을 절대 위치에 배치 */
  comp.layoutMode = 'NONE';
  comp.resize(GRID_WIDTH, gridHeight);
  clearFill(comp);

  /* HORIZONTAL WRAP 슬롯
   * 셀 너비가 WRAP 기준을 정확히 채우므로 cols열로 자동 줄바꿈된다 */
  const slot = comp.createSlot();
  slot.name = 'Items';
  slot.layoutMode = 'HORIZONTAL';
  slot.layoutWrap = 'WRAP';
  slot.itemSpacing = ITEM_GAP;
  slot.counterAxisSpacing = ITEM_GAP;
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.resize(GRID_WIDTH, gridHeight);
  slot.x = 0;
  slot.y = 0;
  clearFill(slot);

  /* 기본 Item 인스턴스 배치 (rows × cols개), 열 수에 맞게 셀 너비 조정 */
  for (let i = 0; i < rows * cols; i++) {
    const inst = item.createInstance();
    inst.resize(cellWidth, ITEM_HEIGHT);
    slot.appendChild(inst);
  }

  return comp;
}

/**
 * QuickMenuGrid ComponentSet을 생성한다.
 * QuickMenuGrid/Item이 먼저 캔버스에 존재해야 인스턴스 배치가 가능하므로
 * createQuickMenuGridItem() 반환값을 item으로 전달해야 한다.
 */
export async function createQuickMenuGrid(
  item: ComponentNode,
): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createGridVariant(item, 2),
    createGridVariant(item, 3),
    createGridVariant(item, 4),
  ]);
  return combineVariants(components, 'QuickMenuGrid', 3);
}
