/**
 * @file createUsageTransactionItem.ts
 * @description Figma UsageTransactionItem 컴포넌트 세트 생성.
 * 카드 이용내역 단건 행 컴포넌트.
 * Type(Normal|Refund) = 2 variants.
 * - Normal: 일반 결제 — 금액 text-heading
 * - Refund: 취소/환불 — 금액 brand 색상 (음수)
 * 컴포넌트 이름: "UsageTransactionItem"
 */
import { BRAND, COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  addTextWithVar,
} from '../../../helpers';

const ITEM_WIDTH = 390;

async function createUsageTransactionVariant(type: 'Normal' | 'Refund'): Promise<ComponentNode> {
  const isRefund = type === 'Refund';

  const comp = createComponent(`Type=${type}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(ITEM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 좌측: 가맹점 + 날짜·유형·카드명 */
  const left = figma.createFrame();
  setAutoLayout(left, 'VERTICAL', 2, 'MIN');
  left.layoutGrow = 1;
  clearFill(left);

  await addTextWithVar(left, isRefund ? '스타벅스 (취소)' : '스타벅스', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  await addTextWithVar(left, '2026.04.15 · 신용 · 하나 머니 체크카드', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  comp.appendChild(left);

  /* 우측: 금액 */
  const amountText = isRefund ? '-4,500원' : '4,500원';
  await addTextWithVar(
    comp, amountText, FONT_SIZE.sm,
    isRefund ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isRefund ? BRAND.text         : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );

  return comp;
}

export async function createUsageTransactionItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createUsageTransactionVariant('Normal'), await createUsageTransactionVariant('Refund')],
    'UsageTransactionItem', 1,
  );
}
