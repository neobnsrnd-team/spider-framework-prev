/**
 * @file createGrid.ts
 * @description Figma Grid 레이아웃 컴포넌트 세트 생성.
 * CSS 그리드(grid grid-cols-N)의 열 수 변형을 시각적으로 표현한다.
 * Cols=2|3|4 = 3 variants.
 *
 * Figma는 CSS grid를 지원하지 않으므로 각 셀의 x/y를 직접 계산해 배치한다.
 * 컴포넌트 이름: "Grid"
 */
import { COLOR, SPACING, RADIUS } from '../../utils/tokens';
import { createComponent, combineVariants, setFill, clearFill } from '../../utils/helpers';

const GRID_W    = 390;
const ITEM_H    = 64;
const ROW_COUNT = 2;
const GAP       = SPACING.sm; /* gap-sm(8px) — Grid 기본 gap */

function makeItem(w: number): FrameNode {
  const item = figma.createFrame();
  item.resize(w, ITEM_H);
  item.cornerRadius = RADIUS.sm;
  setFill(item, COLOR.surfaceRaised);
  item.strokes = [{ type: 'SOLID', color: COLOR.border }];
  item.strokeWeight = 1;
  item.strokeAlign = 'INSIDE';
  return item;
}

async function createGridVariant(cols: 2 | 3 | 4): Promise<ComponentNode> {
  const itemW  = Math.floor((GRID_W - GAP * (cols - 1)) / cols);
  const totalH = ITEM_H * ROW_COUNT + GAP * (ROW_COUNT - 1);

  const comp = createComponent(`Cols=${cols}`);
  comp.resize(GRID_W, totalH);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  for (let i = 0; i < cols * ROW_COUNT; i++) {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const item = makeItem(itemW);
    comp.appendChild(item);
    item.x = col * (itemW + GAP);
    item.y = row * (ITEM_H + GAP);
  }

  return comp;
}

export async function createGrid(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createGridVariant(2),
    createGridVariant(3),
    createGridVariant(4),
  ]);
  return combineVariants(components, 'Grid', 1);
}
