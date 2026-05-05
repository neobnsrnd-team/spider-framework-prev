/**
 * @file createActionLinkItem.ts
 * @description Figma ActionLinkItem 컴포넌트 세트 생성.
 * size(md|sm) × showBorder(true|false) = 4 variants.
 *
 * TEXT properties:
 *   - label — 메뉴 항목 텍스트 (기본값: '메뉴 항목')
 *
 * INSTANCE_SWAP properties:
 *   - icon  — 좌측 아이콘 (기본: Settings, Icons/* 컴포넌트로 swap 가능)
 *
 * 컴포넌트 이름: "ActionLinkItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFill, addTextWithVar, setStroke, addIconSlot,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

type ActionSize = 'Medium' | 'Small';

const SIZE_CONFIG: Record<ActionSize, { py: number; fontSize: number; fontSizeVar: string }> = {
  Medium: { py: SPACING.md, fontSize: FONT_SIZE.sm, fontSizeVar: SIZE_VAR.fontSizeSm },
  Small:  { py: SPACING.sm, fontSize: FONT_SIZE.xs, fontSizeVar: SIZE_VAR.fontSizeXs },
};

async function createActionLinkVariant(size: ActionSize, showBorder: boolean): Promise<ComponentNode> {
  const { py, fontSize, fontSizeVar } = SIZE_CONFIG[size];
  const comp = createComponent(`Size=${size}, ShowBorder=${showBorder ? 'True' : 'False'}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, py, SPACING.standard);
  comp.resize(390, size === 'Medium' ? 60 : 48);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  if (showBorder) {
    setFill(comp, COLOR.surface);
    setStroke(comp, COLOR.borderSubtle);
    comp.cornerRadius = RADIUS.md;
  }

  /* 아이콘 래퍼 — comp에 먼저 append해야 addIconSlot이 sublayer 조건을 충족한다 */
  const iconWrap = figma.createFrame();
  iconWrap.name = 'IconWrap';
  setAutoLayout(iconWrap, 'HORIZONTAL', 0);
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.primaryAxisAlignItems = 'CENTER';
  iconWrap.counterAxisAlignItems = 'CENTER';
  iconWrap.cornerRadius = RADIUS.sm;
  setFill(iconWrap, BRAND.bg);
  comp.appendChild(iconWrap);

  /* INSTANCE_SWAP icon: iconWrap이 comp의 sublayer가 된 뒤 호출 */
  addIconSlot(comp, 'Settings', 20, BRAND.primary, 'icon', iconWrap);

  /* label TEXT property */
  const label = await addTextWithVar(
    comp, '메뉴 항목', fontSize,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, fontSizeVar, 'label',
  );
  label.layoutGrow = 1;

  comp.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));

  return comp;
}

export async function createActionLinkItem(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (const size of ['Medium', 'Small'] as ActionSize[]) {
    for (const border of [false, true]) {
      components.push(await createActionLinkVariant(size, border));
    }
  }
  return combineVariants(components, 'ActionLinkItem', 2);
}
