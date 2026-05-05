/**
 * @file createInline.ts
 * @description Figma Inline 레이아웃 컴포넌트 세트 생성.
 * 가로 방향 flex 레이아웃(flex)의 justify × gap 변형을 시각적으로 표현한다.
 * Justify(Start|Between|End) × Gap(sm|md|lg) = 9 variants.
 *
 * Content 영역은 SlotNode로 구현되어, 인스턴스에서 자식 컴포넌트를 자유롭게 배치할 수 있다.
 * 슬롯 내부의 HORIZONTAL auto-layout이 justify·gap 값을 적용한다.
 *
 * 컴포넌트 이름: "Inline"
 */
import { COLOR, SPACING, RADIUS } from '../../utils/tokens';
import { createComponent, combineVariants, clearFill, setFill } from '../../utils/helpers';

const INLINE_W = 390;
const ITEM_W   = 100;
const ITEM_H   = 40;

type JustifyValue = 'MIN' | 'SPACE_BETWEEN' | 'MAX';

const JUSTIFY_VARIANTS: { name: string; primary: JustifyValue }[] = [
  { name: 'Start',   primary: 'MIN'           },
  { name: 'Between', primary: 'SPACE_BETWEEN' },
  { name: 'End',     primary: 'MAX'           },
];

const GAP_VARIANTS = [
  { name: 'sm', gap: SPACING.sm },
  { name: 'md', gap: SPACING.md },
  { name: 'lg', gap: SPACING.lg },
] as const;

function makeItem(): FrameNode {
  const item = figma.createFrame();
  item.resize(ITEM_W, ITEM_H);
  item.cornerRadius = RADIUS.sm;
  setFill(item, COLOR.surfaceRaised);
  item.strokes = [{ type: 'SOLID', color: COLOR.border }];
  item.strokeWeight = 1;
  item.strokeAlign = 'INSIDE';
  return item;
}

async function createInlineVariant(
  justify: { name: string; primary: JustifyValue },
  gap: { name: string; gap: number },
): Promise<ComponentNode> {
  const comp = createComponent(`Justify=${justify.name}, Gap=${gap.name}`);
  comp.layoutMode = 'VERTICAL';
  comp.itemSpacing = 0;
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.resize(INLINE_W, ITEM_H);
  clearFill(comp);

  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.layoutMode = 'HORIZONTAL';
  slot.itemSpacing = gap.gap;
  slot.primaryAxisAlignItems = justify.primary; /* justify 값이 슬롯 레이아웃에 적용됨 */
  slot.counterAxisAlignItems = 'CENTER';
  /* SPACE_BETWEEN이 동작하려면 FIXED primary axis가 필요 */
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;
  clearFill(slot);

  /* 기본 콘텐츠 — 인스턴스에서 자식을 추가하면 대체된다 */
  for (let i = 0; i < 3; i++) {
    slot.appendChild(makeItem());
  }

  return comp;
}

export async function createInline(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];

  for (const justify of JUSTIFY_VARIANTS) {
    for (const gap of GAP_VARIANTS) {
      components.push(await createInlineVariant(justify, gap));
    }
  }

  /* cols=3: 한 행 = 하나의 Justify 조합의 Gap sm|md|lg */
  return combineVariants(components, 'Inline', 3);
}
