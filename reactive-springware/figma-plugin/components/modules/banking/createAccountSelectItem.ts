/**
 * @file createAccountSelectItem.ts
 * @description Figma AccountSelectItem 컴포넌트 세트 생성.
 * Selected(true|false) 2 variants.
 * 컴포넌트 이름: "AccountSelectItem"
 */
import { COLOR, BRAND, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, addText } from '../../../helpers';
import { createIcon } from '../../../icons';

async function createAccountSelectVariant(selected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Selected=${selected ? 'True' : 'False'}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(390, 72);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  setFill(comp, selected ? BRAND.bg : COLOR.surfaceSubtle);

  /* 아이콘 원형 */
  const iconCircle = figma.createEllipse();
  iconCircle.resize(40, 40);
  setFill(iconCircle, selected ? BRAND.primary : COLOR.surfaceRaised);
  comp.appendChild(iconCircle);

  /* 텍스트 영역 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', 2);
  textArea.layoutGrow = 1;
  textArea.fills = [];
  await addText(textArea, '하나 급여통장', FONT_SIZE.sm, COLOR.textHeading, true);
  await addText(textArea, '123-456789-01234', FONT_SIZE.xs, COLOR.textMuted);
  await addText(textArea, '1,234,567원', FONT_SIZE.xs, COLOR.textSecondary);
  comp.appendChild(textArea);

  /* 선택 체크 아이콘 */
  if (selected) {
    comp.appendChild(createIcon('Check', 20, BRAND.primary));
  }

  return comp;
}

export async function createAccountSelectItem(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createAccountSelectVariant(false), createAccountSelectVariant(true)]),
    'AccountSelectItem', 1,
  );
}
