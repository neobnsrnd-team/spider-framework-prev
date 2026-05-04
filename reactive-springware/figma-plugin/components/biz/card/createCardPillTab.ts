/**
 * @file createCardPillTab.ts
 * @description Figma CardPillTab 컴포넌트 세트 생성.
 * 카드 목록 가로 스크롤 pill 형태 탭 버튼.
 * State(Default|Selected) = 2 variants.
 * 컴포넌트 이름: "CardPillTab"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

async function createCardPillTabVariant(selected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`State=${selected ? 'Selected' : 'Default'}`);
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, SPACING.xs, SPACING.md);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

  if (selected) {
    await setFillWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
    await addTextWithVar(comp, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeSm, 'tabLabel');
  } else {
    await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
    await addTextWithVar(comp, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm, 'tabLabel');
  }

  return comp;
}

export async function createCardPillTab(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createCardPillTabVariant(false), await createCardPillTabVariant(true)],
    'CardPillTab', 2,
  );
}
