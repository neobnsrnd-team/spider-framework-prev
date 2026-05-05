/**
 * @file createUsageTransactionItem.ts
 * @description Figma UsageTransactionItem 컴포넌트 세트 생성.
 * 카드 이용내역 단건 행 컴포넌트.
 * state(normal|refund) = 2 variants.
 * - normal: 일반 결제 — 금액 text-heading
 * - refund: 취소/환불 — 금액 brand 색상 (음수)
 *
 * TEXT properties:
 *   - merchant — 가맹점명     (기본값: state별 상이)
 *   - date     — 거래일자     (기본값: '2026.04.15')
 *   - type     — 거래구분     (기본값: '신용')
 *   - cardName — 이용 카드명  (기본값: '하나 머니 체크카드')
 *   - amount   — 결제 금액    (기본값: state별 상이)
 *
 * 컴포넌트 이름: "UsageTransactionItem"
 */
import { BRAND, COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  addTextWithVar,
} from '../../../utils/helpers';

const ITEM_WIDTH = 390;

async function createUsageTransactionVariant(state: 'normal' | 'refund'): Promise<ComponentNode> {
  const isRefund = state === 'refund';

  const comp = createComponent(`state=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(ITEM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 좌측: 가맹점 + 메타 (date · type · cardName) */
  const left = figma.createFrame();
  setAutoLayout(left, 'VERTICAL', 2, 'MIN');
  left.primaryAxisAlignItems = 'MIN';
  left.layoutGrow = 1;
  left.primaryAxisSizingMode = 'AUTO';
  left.counterAxisSizingMode = 'AUTO';
  clearFill(left);
  /* left를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(left);

  /* merchant — left → comp: 수동 바인딩 */
  await addTextWithVar(left, isRefund ? '스타벅스 (취소)' : '스타벅스', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'merchant', comp);

  /* 메타 행 — gap=0으로 'date · type · cardName' 인접 배치 */
  const metaRow = figma.createFrame();
  setAutoLayout(metaRow, 'HORIZONTAL', 0);
  metaRow.primaryAxisAlignItems = 'MIN';
  metaRow.counterAxisAlignItems = 'CENTER';
  metaRow.resize(1, 1);                /* resize 먼저 */
  metaRow.primaryAxisSizingMode = 'AUTO';
  metaRow.counterAxisSizingMode = 'AUTO';
  clearFill(metaRow);
  left.appendChild(metaRow);

  /* date — metaRow → left → comp: 수동 바인딩 */
  await addTextWithVar(metaRow, '2026.04.15', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'date', comp);
  /* 정적 구분자 */
  await addTextWithVar(metaRow, ' · ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  /* type */
  await addTextWithVar(metaRow, '신용', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'type', comp);
  /* 정적 구분자 */
  await addTextWithVar(metaRow, ' · ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  /* cardName */
  await addTextWithVar(metaRow, '하나 머니 체크카드', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'cardName', comp);

  /* 우측: 금액 — comp 직접 자식: 자동 바인딩 */
  const amountText = isRefund ? '-4,500원' : '4,500원';
  await addTextWithVar(
    comp, amountText, FONT_SIZE.sm,
    isRefund ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isRefund ? BRAND.text         : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'amount',
  );

  return comp;
}

export async function createUsageTransactionItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createUsageTransactionVariant('normal'), await createUsageTransactionVariant('refund')],
    'UsageTransactionItem', 1,
  );
}
