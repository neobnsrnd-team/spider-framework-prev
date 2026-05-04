/**
 * @file createBankSelectGrid.ts
 * @description Figma BankSelectGrid 컴포넌트 세트 생성.
 * 이체·계좌 등록 시 수취 은행을 선택하는 그리드 UI.
 *
 * 1. BankSelectGrid/Item — State(Default|Selected) = 2  (단일 셀)
 * 2. BankSelectGrid      — Layout(3col|4col) = 2        (샘플 전체 그리드)
 *
 * 컴포넌트 이름: "BankSelectGrid/Item", "BankSelectGrid"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

type ItemState = 'Default' | 'Selected';
type GridLayout = '3col' | '4col';

/** 단일 은행 셀 너비: 4열 기준 (390 - 3×gap8) / 4 ≈ 90 */
const CELL_WIDTH_4 = 90;
const CELL_WIDTH_3 = 118;
const CELL_HEIGHT = 72; /* py-md(12) × 2 + 아이콘 28 + gap xs(4) + caption 16 ≈ 72 */

/** 단일 셀 프레임 생성 */
async function createBankCell(
  parentOrComp: FrameNode | ComponentNode,
  state: ItemState,
  width: number,
): Promise<void> {
  setAutoLayout(parentOrComp, 'VERTICAL', SPACING.xs);
  setPadding(parentOrComp, SPACING.md, SPACING.xs);
  parentOrComp.primaryAxisAlignItems = 'CENTER';
  parentOrComp.resize(width, CELL_HEIGHT);
  parentOrComp.primaryAxisSizingMode = 'FIXED';
  parentOrComp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(parentOrComp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (state === 'Selected') {
    await setFillWithVar(parentOrComp, COLOR_VAR.brandBg, BRAND.bg);
    await setStrokeWithVar(parentOrComp, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(parentOrComp, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(parentOrComp, COLOR_VAR.border, COLOR.border);
  }

  /* 아이콘 플레이스홀더 (28×28) */
  const iconBox = figma.createFrame();
  iconBox.resize(28, 28);
  iconBox.layoutMode = 'NONE';
  iconBox.cornerRadius = RADIUS.xs;
  iconBox.fills = [{ type: 'SOLID', color: COLOR.surfaceRaised }];
  parentOrComp.appendChild(iconBox);

  /* 은행명 (caption) — parentOrComp가 ComponentNode일 때만 TEXT property 등록됨 */
  await addTextWithVar(
    parentOrComp, '하나은행', FONT_SIZE.xs,
    state === 'Selected' ? COLOR_VAR.brandPrimary : COLOR_VAR.textSecondary,
    state === 'Selected' ? BRAND.primary          : COLOR.textSecondary,
    state === 'Selected', /* Selected: bold */
    SIZE_VAR.fontSizeXs, 'bankName',
  );
}

/** 1. BankSelectGrid/Item — 단일 셀 ComponentSet */
export async function createBankSelectGridItem(): Promise<ComponentSetNode> {
  const states: ItemState[] = ['Default', 'Selected'];
  const components: ComponentNode[] = [];

  for (const state of states) {
    const comp = createComponent(`State=${state}`);
    await createBankCell(comp, state, CELL_WIDTH_4);
    components.push(comp);
  }

  return combineVariants(components, 'BankSelectGrid/Item', 2);
}

/** 샘플 은행 목록 */
const SAMPLE_BANKS = ['하나', 'KB', '신한', '우리', 'IBK', 'NH', '카카오', '토스'];

/** 단일 셀 FrameNode 생성 (그리드 내부용) */
async function makeCellFrame(label: string, isSelected: boolean, width: number): Promise<FrameNode> {
  const cell = figma.createFrame();
  await createBankCell(cell, isSelected ? 'Selected' : 'Default', width);
  /* 은행명 덮어쓰기 */
  const texts = cell.findAll(n => n.type === 'TEXT') as TextNode[];
  if (texts.length > 0) texts[texts.length - 1].characters = label;
  return cell;
}

/** 2. BankSelectGrid — 전체 그리드 ComponentSet */
export async function createBankSelectGrid(): Promise<ComponentSetNode> {
  const layouts: GridLayout[] = ['3col', '4col'];
  const components: ComponentNode[] = [];

  for (const layout of layouts) {
    const cols = layout === '3col' ? 3 : 4;
    const cellWidth = layout === '3col' ? CELL_WIDTH_3 : CELL_WIDTH_4;
    const gap = SPACING.sm;
    const gridWidth = cols * cellWidth + (cols - 1) * gap;
    const rows = Math.ceil(SAMPLE_BANKS.length / cols);
    const gridHeight = rows * CELL_HEIGHT + (rows - 1) * gap;

    const comp = createComponent(`Layout=${layout}`);
    comp.resize(gridWidth, gridHeight);
    comp.layoutMode = 'NONE';
    clearFill(comp);

    /* 셀을 그리드 위치에 배치 */
    for (let i = 0; i < SAMPLE_BANKS.length; i++) {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const cell = await makeCellFrame(SAMPLE_BANKS[i], i === 0, cellWidth);
      cell.x = col * (cellWidth + gap);
      cell.y = row * (CELL_HEIGHT + gap);
      comp.appendChild(cell);
    }

    components.push(comp);
  }

  return combineVariants(components, 'BankSelectGrid', 2);
}
