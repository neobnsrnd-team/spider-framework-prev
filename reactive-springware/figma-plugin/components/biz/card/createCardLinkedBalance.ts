/**
 * @file createCardLinkedBalance.ts
 * @description Figma CardLinkedBalance 컴포넌트 세트 생성.
 * 연결계좌 잔액 표시 컴포넌트.
 * hidden(False|True) = 2 variants.
 *
 * TEXT properties:
 *   - balance — 잔액 금액 (기본값: hidden=False '1,200,000원' / hidden=True '잔액 숨김 중')
 *
 * 컴포넌트 이름: "CardLinkedBalance"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

async function createCardLinkedBalanceVariant(hidden: boolean): Promise<ComponentNode> {
  const comp = createComponent(`hidden=${hidden ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.resize(1, 1);                   /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 고정 레이블 */
  await addTextWithVar(comp, '사용가능 한도 금액', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* 금액 + 배지 버튼 — HORIZONTAL 행 */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', SPACING.xs);
  amountRow.primaryAxisAlignItems  = 'MIN';
  amountRow.counterAxisAlignItems  = 'CENTER';
  amountRow.resize(1, 1);            /* resize 먼저 */
  amountRow.primaryAxisSizingMode  = 'AUTO';
  amountRow.counterAxisSizingMode  = 'AUTO'; /* height 텍스트 높이에 맞게 */
  clearFill(amountRow);

  /* amountRow를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(amountRow);

  const amountText = !hidden ? '1,200,000원' : '잔액 숨김 중';
  await addTextWithVar(amountRow, amountText, FONT_SIZE['2xl'], COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSize2xl, 'balance', comp);

  /* 배지 버튼 (보기/숨기기) — 고정 텍스트 */
  const badgeBtn = figma.createFrame();
  setAutoLayout(badgeBtn, 'HORIZONTAL', 0);
  badgeBtn.resize(1, 1);             /* resize 먼저 */
  badgeBtn.primaryAxisSizingMode  = 'AUTO'; /* width 텍스트+padding에 맞게 */
  badgeBtn.counterAxisSizingMode  = 'AUTO'; /* height 텍스트+padding에 맞게 */
  setPadding(badgeBtn, 2, SPACING.sm);
  await setFloatVar(badgeBtn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badgeBtn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setStrokeWithVar(badgeBtn, COLOR_VAR.border, COLOR.border);
  amountRow.appendChild(badgeBtn);

  /* 고정 텍스트 — TEXT property 없이 variant마다 다른 레이블 */
  const badgeLabel = !hidden ? '숨기기' : '보기';
  await addTextWithVar(badgeBtn, badgeLabel, FONT_SIZE.xs, COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeXs);

  return comp;
}

export async function createCardLinkedBalance(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createCardLinkedBalanceVariant(false), await createCardLinkedBalanceVariant(true)],
    'CardLinkedBalance', 2,
  );
}
