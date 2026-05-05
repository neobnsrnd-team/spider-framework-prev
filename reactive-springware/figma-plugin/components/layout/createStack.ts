/**
 * @file createStack.ts
 * @description Figma Stack 레이아웃 컴포넌트 세트 생성.
 * 세로 방향 flex 레이아웃(flex flex-col)의 gap 변형을 시각적으로 표현한다.
 * Gap=sm|md|lg = 3 variants.
 *
 * Content 영역은 SlotNode로 구현되어, 인스턴스에서 자식 컴포넌트를 자유롭게 배치할 수 있다.
 * 슬롯 내부의 VERTICAL auto-layout이 gap 값을 적용한다.
 *
 * 컴포넌트 이름: "Stack"
 */
import { COLOR, SPACING, RADIUS } from '../../utils/tokens';
import { createComponent, combineVariants, clearFill, setFill } from '../../utils/helpers';

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
  /* layoutSection이 appendChild 이전에 height를 읽으므로 명시적으로 계산 */
  const totalH = ITEM_H * ITEM_COUNT + gap * (ITEM_COUNT - 1);

  const comp = createComponent(`Gap=${name}`);
  comp.layoutMode = 'VERTICAL';
  comp.itemSpacing = 0;
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.resize(STACK_W, totalH);
  clearFill(comp);

  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.layoutMode = 'VERTICAL';
  slot.itemSpacing = gap;          /* gap 값이 슬롯 레이아웃에 적용됨 */
  slot.primaryAxisAlignItems = 'MIN';
  slot.counterAxisAlignItems = 'MIN';
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;             /* comp 전체 높이를 채움 */
  clearFill(slot);

  /* 기본 콘텐츠 — 인스턴스에서 자식을 추가하면 대체된다 */
  for (let i = 0; i < ITEM_COUNT; i++) {
    const item = makeItem();
    slot.appendChild(item);
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
