/**
 * @file createStack.ts
 * @description Figma Stack 레이아웃 컴포넌트 세트 생성.
 * 세로 방향 flex 레이아웃(flex flex-col)의 gap 변형을 시각적으로 표현한다.
 * Gap=sm|md|lg = 3 variants.
 *
 * 컴포넌트 이름: "Stack"
 */
import { COLOR, SPACING, RADIUS } from '../../tokens';
import { createComponent, combineVariants, setAutoLayout, setFill } from '../../helpers';

const STACK_W    = 390;
const ITEM_H     = 48;
const ITEM_COUNT = 3;

const GAP_VARIANTS = [
  { name: 'sm', gap: SPACING.sm },
  { name: 'md', gap: SPACING.md },
  { name: 'lg', gap: SPACING.lg },
] as const;

function makeItem(): FrameNode {
  const item = figma.createFrame();
  item.resize(STACK_W, ITEM_H);
  item.cornerRadius = RADIUS.sm;
  setFill(item, COLOR.surfaceRaised);
  item.strokes = [{ type: 'SOLID', color: COLOR.border }];
  item.strokeWeight = 1;
  item.strokeAlign = 'INSIDE';
  return item;
}

async function createStackVariant(name: string, gap: number): Promise<ComponentNode> {
  const totalH = ITEM_H * ITEM_COUNT + gap * (ITEM_COUNT - 1);
  const comp = createComponent(`Gap=${name}`);
  setAutoLayout(comp, 'VERTICAL', gap, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(STACK_W, totalH);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';

  for (let i = 0; i < ITEM_COUNT; i++) {
    const item = makeItem();
    comp.appendChild(item);
    /* VERTICAL 레이아웃에서 자식의 가로폭을 부모에 맞게 확장 — appendChild 이후에 설정해야 적용됨 */
    item.layoutSizingHorizontal = 'FILL';
  }

  return comp;
}

export async function createStack(): Promise<ComponentSetNode> {
  const components = await Promise.all(
    GAP_VARIANTS.map(({ name, gap }) => createStackVariant(name, gap)),
  );
  return combineVariants(components, 'Stack', 1);
}
