/**
 * @file createCardPaymentActions.ts
 * @description Figma CardPaymentActions 컴포넌트 생성.
 * 분할납부 / 즉시결제 / 일부결제금액이월약정(리볼빙) 3개의 outline 버튼 그룹.
 * 고정 텍스트 — TEXT property 없음. 단일 variant.
 * 컴포넌트 이름: "CardPaymentActions"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, clearFill,
  setStroke, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const GROUP_WIDTH = 390 - SPACING.standard * 2; /* 양쪽 패딩 제외 */

/* 고정 레이블 — TEXT property 없이 각 버튼에 직접 적용 */
const ACTIONS = ['분할납부', '즉시결제', '일부결제금액이월약정(리볼빙)'];

export async function createCardPaymentActions(): Promise<ComponentNode> {
  const comp = createComponent('CardPaymentActions');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.resize(GROUP_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  for (const label of ACTIONS) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    /* primaryAxis(가로) = CENTER, counterAxis(세로) = CENTER: 텍스트 상하좌우 중앙 정렬 */
    btn.primaryAxisAlignItems  = 'CENTER';
    btn.counterAxisAlignItems  = 'CENTER';
    btn.layoutGrow             = 1;       /* 3등분 균일 너비 */
    btn.resize(1, 44);                    /* resize 먼저 — StatementTotalCard 버튼과 동일 고정 높이 */
    btn.primaryAxisSizingMode  = 'FIXED'; /* 너비는 부모 layoutGrow로 결정 */
    btn.counterAxisSizingMode  = 'FIXED'; /* 높이 44 고정 — 모든 버튼 일관된 높이 */
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
    clearFill(btn);
    /* Button Outline 스타일: brandPrimary stroke */
    setStroke(btn, BRAND.primary);

    comp.appendChild(btn);

    /* 고정 텍스트 — TEXT property 없음 */
    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeXs);
    /* 버튼 너비 전체를 채워야 textAlignHorizontal='CENTER'가 제대로 작동 */
    text.layoutGrow           = 1;
    text.textAlignHorizontal  = 'CENTER';
    text.textAlignVertical    = 'CENTER'; /* FIXED 높이 내 세로 중앙 */
    text.textAutoResize       = 'HEIGHT'; /* 텍스트가 길면 줄바꿈 */
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
