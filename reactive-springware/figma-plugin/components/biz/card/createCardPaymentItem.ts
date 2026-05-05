/**
 * @file createCardPaymentItem.ts
 * @description Figma CardPaymentItem 컴포넌트 세트 생성.
 * 카드 이용내역 목록의 개별 결제 항목 행.
 * state(default|refund) = 2 variants.
 * - default: 일반 결제 — 금액 textHeading 색상
 * - refund:  취소/환불 — 금액 brandText 색상 (음수)
 *
 * TEXT properties:
 *   - cardEnName — 카드 영문명 (기본값: 'HANA MONEY CHECK')
 *   - cardName   — 카드 한글명 (기본값: '하나 머니 체크카드')
 *   - amount     — 결제 금액  (기본값: state별 상이)
 *
 * INSTANCE_SWAP properties:
 *   - icon — 아이콘 (기본값: CreditCard)
 *
 * 컴포넌트 이름: "CardPaymentItem"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

const ITEM_WIDTH = 390;

async function createCardPaymentItemVariant(state: 'default' | 'refund'): Promise<ComponentNode> {
  const isRefund = state === 'refund';

  const comp = createComponent(`state=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(ITEM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 아이콘 원형 (48×48) — INSTANCE_SWAP 속성 등록 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.primaryAxisAlignItems = 'CENTER';
  iconCircle.counterAxisAlignItems = 'CENTER';
  iconCircle.resize(48, 48);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  /* iconCircle을 comp에 먼저 추가해야 INSTANCE_SWAP 바인딩 가능 */
  comp.appendChild(iconCircle);
  /* icon INSTANCE_SWAP — iconCircle 내부에 배치, comp에 속성 등록 */
  addIconSlot(comp, 'CreditCard', 20, BRAND.primary, 'icon', iconCircle);

  /* 카드 정보: 영문명 + 한글명 */
  const info = figma.createFrame();
  setAutoLayout(info, 'VERTICAL', 2, 'MIN');
  info.primaryAxisAlignItems = 'MIN';
  info.resize(1, 1);                   /* resize 먼저 */
  info.primaryAxisSizingMode = 'AUTO';
  info.counterAxisSizingMode = 'AUTO';
  info.layoutGrow = 1;                 /* 나머지 가로 공간 채움 */
  clearFill(info);
  /* info를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(info);
  await addTextWithVar(info, 'HANA MONEY CHECK', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'cardEnName', comp);
  await addTextWithVar(info, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName', comp);

  /* 우측: 금액 */
  const right = figma.createFrame();
  setAutoLayout(right, 'VERTICAL', 2, 'MAX');
  right.primaryAxisAlignItems = 'MIN';
  right.resize(1, 1);                  /* resize 먼저 */
  right.primaryAxisSizingMode = 'AUTO';
  right.counterAxisSizingMode = 'AUTO';
  clearFill(right);
  /* right를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(right);

  /* refund: 음수 금액 + brandText 색상 / default: 양수 금액 + textHeading 색상 */
  const amountText = isRefund ? '-15,000원' : '15,000원';
  const amountNode = await addTextWithVar(
    right, amountText, FONT_SIZE.sm,
    isRefund ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isRefund ? BRAND.text          : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'amount', comp,
  );
  amountNode.textAlignHorizontal = 'RIGHT';

  /* 상세보기 — 고정 텍스트, TEXT property 없음 */
  const detailNode = await addTextWithVar(right, '상세보기', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeXs);
  detailNode.textAlignHorizontal = 'RIGHT';

  return comp;
}

export async function createCardPaymentItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createCardPaymentItemVariant('default'), await createCardPaymentItemVariant('refund')],
    'CardPaymentItem', 2,
  );
}
