/**
 * @file createActionLinkItem.ts
 * @description Figma ActionLinkItem 컴포넌트 세트 생성.
 * size(md|sm) × showBorder(true|false) = 4 variants.
 * 컴포넌트 이름: "ActionLinkItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, clearFill, setFill, addText, setStroke } from '../../../helpers';
import { createIcon } from '../../../icons';

type ActionSize = 'Medium' | 'Small';

const SIZE_CONFIG: Record<ActionSize, { py: number; fontSize: number }> = {
  Medium: { py: SPACING.md,  fontSize: FONT_SIZE.sm },
  Small:  { py: SPACING.sm,  fontSize: FONT_SIZE.xs },
};

async function createActionLinkVariant(size: ActionSize, showBorder: boolean): Promise<ComponentNode> {
  const { py, fontSize } = SIZE_CONFIG[size];
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

  /* 아이콘 */
  const iconWrap = figma.createFrame();
  setAutoLayout(iconWrap, 'HORIZONTAL', 0);
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.primaryAxisAlignItems = 'CENTER';
  iconWrap.counterAxisAlignItems = 'CENTER';
  iconWrap.cornerRadius = RADIUS.sm;
  setFill(iconWrap, BRAND.bg);
  iconWrap.appendChild(createIcon('Settings', 20, BRAND.primary));
  comp.appendChild(iconWrap);

  const label = await addText(comp, '메뉴 항목', fontSize, COLOR.textSecondary);
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
