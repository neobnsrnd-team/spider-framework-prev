/**
 * @file createAccountSelectCard.ts
 * @description Figma AccountSelectCard 컴포넌트 세트 생성.
 * 출금계좌 선택 카드 컴포넌트.
 * State(Default|Selected) = 2 variants.
 * 컴포넌트 이름: "AccountSelectCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

async function createAccountSelectCardVariant(selected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`State=${selected ? 'Selected' : 'Default'}`);
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (selected) {
    await setFillWithVar(comp, COLOR_VAR.brandBg, BRAND.bg);
    await setStrokeWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  }

  const left = figma.createFrame();
  setAutoLayout(left, 'HORIZONTAL', SPACING.sm);
  left.counterAxisAlignItems = 'CENTER';
  left.primaryAxisSizingMode = 'AUTO';
  left.counterAxisSizingMode = 'AUTO';
  clearFill(left);

  /* 은행 아이콘 원형 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.resize(32, 32);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  iconCircle.appendChild(createIcon('Building2', 16, COLOR.textMuted));
  left.appendChild(iconCircle);

  const info = figma.createFrame();
  setAutoLayout(info, 'VERTICAL', SPACING.xs, 'MIN');
  info.primaryAxisSizingMode = 'AUTO';
  info.counterAxisSizingMode = 'AUTO';
  clearFill(info);
  /* info를 left에, left를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  left.appendChild(info);
  comp.appendChild(left);
  await addTextWithVar(info, '하나은행', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'bankName', comp);
  await addTextWithVar(info, '123-****-5678', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'accountNumber', comp);

  if (selected) {
    comp.appendChild(createIcon('CheckCircle', 20, BRAND.primary));
  }

  return comp;
}

export async function createAccountSelectCard(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createAccountSelectCardVariant(false), await createAccountSelectCardVariant(true)],
    'AccountSelectCard', 1,
  );
}
