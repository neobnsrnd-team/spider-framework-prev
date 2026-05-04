/**
 * @file createInline.ts
 * @description Figma Inline 레이아웃 컴포넌트 세트 생성.
 * 가로 방향 flex 레이아웃(flex)의 justify 변형을 시각적으로 표현한다.
 * Justify=Start|Between|End = 3 variants.
 *
 * 컴포넌트 이름: "Inline"
 */
import { COLOR, SPACING, RADIUS } from '../../tokens';
import { createComponent, combineVariants, setAutoLayout, setFill } from '../../helpers';

const INLINE_W = 390;
const INLINE_H = 56;
const ITEM_W   = 100;
const ITEM_H   = 40;

type JustifyDef = {
  name: string;
  primary: 'MIN' | 'CENTER' | 'MAX' | 'SPACE_BETWEEN';
};

const JUSTIFY_VARIANTS: JustifyDef[] = [
  { name: 'Start',   primary: 'MIN'           },
  { name: 'Between', primary: 'SPACE_BETWEEN' },
  { name: 'End',     primary: 'MAX'           },
];

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

async function createInlineVariant({ name, primary }: JustifyDef): Promise<ComponentNode> {
  const comp = createComponent(`Justify=${name}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.primaryAxisAlignItems = primary;
  comp.resize(INLINE_W, INLINE_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';

  /* 3개 아이템: Justify=Between에서 양 끝 분리가 명확하게 보이도록 */
  for (let i = 0; i < 3; i++) {
    comp.appendChild(makeItem());
  }

  return comp;
}

export async function createInline(): Promise<ComponentSetNode> {
  const components = await Promise.all(JUSTIFY_VARIANTS.map(createInlineVariant));
  return combineVariants(components, 'Inline', 1);
}
