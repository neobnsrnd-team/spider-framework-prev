/**
 * @file createBankSelectGrid.ts
 * @description Figma BankSelectGrid 컴포넌트 세트 생성.
 * 이체·계좌 등록 시 수취 은행을 선택하는 그리드 UI.
 *
 * 1. BankSelectGrid/Item — State(Default|Selected) = 2 variants
 *    TEXT properties:
 *      - name — 은행명 (기본값: '하나은행')
 *    INSTANCE_SWAP properties:
 *      - icon — 은행 아이콘 (기본: Landmark, Icons/* 컴포넌트로 swap 가능)
 *
 * 2. BankSelectGrid — Columns(3|4) = 2 variants
 *    슬롯(HORIZONTAL WRAP)에 BankSelectGrid/Item 인스턴스를 기본 배치.
 *    인스턴스에서 슬롯 항목 추가/제거 가능.
 *    열 수(cols)와 셀 너비가 맞게 계산되어 WRAP이 정확히 N열로 동작한다.
 *
 * [주의] createBankSelectGridItem()을 먼저 실행한 뒤 반환값을 createBankSelectGrid()에 전달해야 한다.
 *
 * @param itemSet - createBankSelectGridItem()이 반환한 ComponentSetNode
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

type ItemState = 'Default' | 'Selected';

/** 4열 기준 셀 너비. 4*90 + 3*8(gap) = 384px */
const CELL_WIDTH_4 = 90;
/** 3열 기준 셀 너비. 3*118 + 2*8(gap) = 370px */
const CELL_WIDTH_3 = 118;
const CELL_HEIGHT  = 72;

/* ── BankSelectGrid/Item ──────────────────────────────────── */

async function createItemVariant(state: ItemState): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs);
  setPadding(comp, SPACING.md, SPACING.xs);
  comp.primaryAxisAlignItems = 'CENTER';
  comp.resize(CELL_WIDTH_4, CELL_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (state === 'Selected') {
    await setFillWithVar(comp, COLOR_VAR.brandBg, BRAND.bg);
    await setStrokeWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  }

  /* 아이콘 래퍼 — comp.appendChild 후 addIconSlot 호출해야 sublayer 조건 충족
   * comp → iconBox(level 1) → icon-instance(level 2) */
  const iconBox = figma.createFrame();
  iconBox.name = 'IconWrap';
  setAutoLayout(iconBox, 'HORIZONTAL', 0);
  iconBox.resize(28, 28);
  iconBox.primaryAxisSizingMode = 'FIXED';
  iconBox.counterAxisSizingMode = 'FIXED';
  iconBox.cornerRadius = RADIUS.xs;
  clearFill(iconBox);
  comp.appendChild(iconBox);

  addIconSlot(
    comp, 'Landmark', 20,
    state === 'Selected' ? BRAND.primary : COLOR.textMuted,
    'icon', iconBox,
  );

  /* name TEXT property — comp 직접 자식(1단계) */
  await addTextWithVar(
    comp, '하나은행', FONT_SIZE.xs,
    state === 'Selected' ? COLOR_VAR.brandPrimary : COLOR_VAR.textSecondary,
    state === 'Selected' ? BRAND.primary          : COLOR.textSecondary,
    state === 'Selected', /* Selected: bold */
    SIZE_VAR.fontSizeXs, 'name',
  );

  return comp;
}

export async function createBankSelectGridItem(): Promise<ComponentSetNode> {
  const components = await Promise.all(
    (['Default', 'Selected'] as ItemState[]).map(createItemVariant),
  );
  return combineVariants(components, 'BankSelectGrid/Item', 2);
}

/* ── BankSelectGrid ───────────────────────────────────────── */

async function createGridVariant(
  itemSet: ComponentSetNode,
  cols: 3 | 4,
): Promise<ComponentNode> {
  const cellWidth  = cols === 3 ? CELL_WIDTH_3 : CELL_WIDTH_4;
  const gap        = SPACING.sm;
  const rows       = 2; /* 기본 2행 표시 */
  const gridWidth  = cols * cellWidth + (cols - 1) * gap;
  const gridHeight = rows * CELL_HEIGHT + (rows - 1) * gap;

  const comp = createComponent(`Columns=${cols}`);
  /* NONE layout으로 슬롯을 절대 위치에 배치 — AUTO height 미사용으로 layoutSection 크기 읽기 정확 */
  comp.layoutMode = 'NONE';
  comp.resize(gridWidth, gridHeight);
  clearFill(comp);

  /* HORIZONTAL WRAP 슬롯
   * itemSpacing        = 열 간격(수평 gap)
   * counterAxisSpacing = 행 간격(수직 gap)
   * 셀 너비가 WRAP 기준을 정확히 채우므로 cols열로 자동 줄바꿈된다 */
  const slot = comp.createSlot();
  slot.name = 'Items';
  slot.layoutMode = 'HORIZONTAL';
  slot.layoutWrap = 'WRAP';
  slot.itemSpacing = gap;
  slot.counterAxisSpacing = gap;
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.resize(gridWidth, gridHeight);
  slot.x = 0;
  slot.y = 0;
  clearFill(slot);

  /* 기본 Item 인스턴스 배치 (rows × cols개) */
  const defaultItem = itemSet.defaultVariant;
  for (let i = 0; i < rows * cols; i++) {
    const inst = defaultItem.createInstance();
    inst.resize(cellWidth, CELL_HEIGHT); /* 열 수에 맞게 셀 너비 조정 */
    slot.appendChild(inst);
  }

  return comp;
}

/**
 * BankSelectGrid ComponentSet을 생성한다.
 * BankSelectGrid/Item이 먼저 캔버스에 존재해야 인스턴스 배치가 가능하므로
 * createBankSelectGridItem() 반환값을 itemSet으로 전달해야 한다.
 */
export async function createBankSelectGrid(
  itemSet: ComponentSetNode,
): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createGridVariant(itemSet, 3),
    createGridVariant(itemSet, 4),
  ]);
  return combineVariants(components, 'BankSelectGrid', 2);
}
