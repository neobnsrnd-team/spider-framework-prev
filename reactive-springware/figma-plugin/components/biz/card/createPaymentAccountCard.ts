/**
 * @file createPaymentAccountCard.ts
 * @description Figma PaymentAccountCard 컴포넌트 생성.
 * 즉시결제 화면의 당행/타행 결제 계좌 안내 카드.
 * 단일 variant.
 * 컴포넌트 이름: "PaymentAccountCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createPaymentAccountCard(): Promise<ComponentNode> {
  const comp = createComponent('PaymentAccountCard');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  comp.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 }, offset: { x: 0, y: 2 }, radius: 8, spread: 0, visible: true, blendMode: 'NORMAL' }];

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.layoutGrow = 1;
  clearFill(textArea);
  /* textArea를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(textArea);
  await addTextWithVar(textArea, '하나은행 결제계좌', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'accountLabel', comp);
  await addTextWithVar(textArea, '365일 06:00~23:30', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'operatingHours', comp);

  /* 아이콘 원형 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.resize(40, 40);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  iconCircle.appendChild(createIcon('Building2', 20, BRAND.primary));
  comp.appendChild(iconCircle);

  figma.currentPage.appendChild(comp);
  return comp;
}
