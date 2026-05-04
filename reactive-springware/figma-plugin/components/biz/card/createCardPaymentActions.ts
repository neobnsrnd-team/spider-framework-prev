/**
 * @file createCardPaymentActions.ts
 * @description Figma CardPaymentActions 컴포넌트 생성.
 * 분할납부 / 즉시결제 / 리볼빙 3개의 outline 버튼 그룹.
 * 단일 variant.
 * 컴포넌트 이름: "CardPaymentActions"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, clearFill,
  setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

const GROUP_WIDTH = 390 - SPACING.standard * 2; /* 양쪽 패딩 제외 */

const ACTIONS = ['분할납부', '즉시결제', '리볼빙'];

export async function createCardPaymentActions(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.resize(GROUP_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  for (const label of ACTIONS) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.layoutGrow = 1;
    btn.counterAxisSizingMode = 'AUTO';
    btn.primaryAxisSizingMode = 'FIXED';
    btn.resize(1, 36);
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
    clearFill(btn);
    await setStrokeWithVar(btn, COLOR_VAR.border, COLOR.border);

    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeXs);
    text.textAlignHorizontal = 'CENTER';
    text.layoutAlign = 'STRETCH';
    comp.appendChild(btn);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
