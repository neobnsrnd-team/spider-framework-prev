/**
 * @file createPaymentAccountCard.ts
 * @description Figma PaymentAccountCard 컴포넌트 생성.
 * 즉시결제 화면의 당행/타행 결제 계좌 안내 카드.
 * 단일 variant.
 *
 * TEXT properties:
 *   - title — 계좌 제목     (기본값: '하나은행 결제계좌')
 *   - hours — 운영 시간     (기본값: '365일 06:00~23:30')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 아이콘 (기본값: Building2)
 *
 * 컴포넌트 이름: "PaymentAccountCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

export async function createPaymentAccountCard(): Promise<ComponentNode> {
  const comp = createComponent('PaymentAccountCard');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  comp.strokeWeight = 1;
  comp.strokeAlign  = 'INSIDE';
  comp.effects = [{
    type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 },
    offset: { x: 0, y: 2 }, radius: 8, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* 텍스트 영역 (좌측) */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.primaryAxisAlignItems = 'MIN';
  textArea.layoutGrow = 1;
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'AUTO';
  clearFill(textArea);
  /* textArea를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(textArea);
  await addTextWithVar(textArea, '하나은행 결제계좌', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'title', comp);
  await addTextWithVar(textArea, '365일 06:00~23:30', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'hours', comp);

  /* 아이콘 원형 (우측) — INSTANCE_SWAP 속성 등록 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.primaryAxisAlignItems = 'CENTER';
  iconCircle.counterAxisAlignItems = 'CENTER';
  iconCircle.resize(40, 40);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  /* iconCircle을 comp에 먼저 추가해야 INSTANCE_SWAP 바인딩 가능 */
  comp.appendChild(iconCircle);
  /* icon INSTANCE_SWAP — iconCircle 내부에 배치, comp에 속성 등록 */
  addIconSlot(comp, 'Building2', 20, BRAND.primary, 'icon', iconCircle);

  figma.currentPage.appendChild(comp);
  return comp;
}
