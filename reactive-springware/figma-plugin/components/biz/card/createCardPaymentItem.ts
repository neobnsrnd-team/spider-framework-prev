/**
 * @file createCardPaymentItem.ts
 * @description Figma CardPaymentItem 컴포넌트 세트 생성.
 * 카드 이용내역 목록의 개별 결제 항목 행.
 * Type(Normal|Refund) = 2 variants.
 * - Normal: 일반 결제 — 금액 text-heading
 * - Refund: 취소/환불 — 금액 brand 색상
 * 컴포넌트 이름: "CardPaymentItem"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const ITEM_WIDTH = 390;

async function createCardPaymentVariant(type: 'Normal' | 'Refund'): Promise<ComponentNode> {
  const isRefund = type === 'Refund';

  const comp = createComponent(`Type=${type}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(ITEM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 아이콘 원형 (48×48) */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.resize(48, 48);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  iconCircle.appendChild(createIcon('CreditCard', 20, BRAND.primary));
  comp.appendChild(iconCircle);

  /* 카드 정보 (영문 + 한글명) */
  const info = figma.createFrame();
  setAutoLayout(info, 'VERTICAL', 2, 'MIN');
  info.layoutGrow = 1;
  clearFill(info);

  /* info를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(info);
  await addTextWithVar(info, 'HANA MONEY CHECK', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'cardNetworkLabel', comp);
  await addTextWithVar(info, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName', comp);

  /* 금액 + 상세보기 */
  const right = figma.createFrame();
  setAutoLayout(right, 'VERTICAL', 2, 'MAX');
  clearFill(right);

  /* right를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(right);
  const amountText = isRefund ? '-15,000원' : '15,000원';
  await addTextWithVar(
    right, amountText, FONT_SIZE.sm,
    isRefund ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isRefund ? BRAND.text         : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'transactionAmount', comp,
  );
  await addTextWithVar(right, '상세보기', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeXs, 'detailLabel', comp);

  return comp;
}

export async function createCardPaymentItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createCardPaymentVariant('Normal'), await createCardPaymentVariant('Refund')],
    'CardPaymentItem', 1,
  );
}
