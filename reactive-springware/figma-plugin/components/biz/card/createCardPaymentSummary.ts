/**
 * @file createCardPaymentSummary.ts
 * @description Figma CardPaymentSummary 컴포넌트 생성.
 * 카드 이용내역 화면 상단 청구 요약 카드.
 * 총 청구금액(브랜드 컬러) + 세부 3열(리볼빙·카드론·현금서비스) + 결제 날짜.
 * 단일 variant.
 *
 * TEXT properties:
 *   - dateYM      — 년월 선택 (기본값: '2026년 4월')
 *   - dateFull    — 출금 날짜 (기본값: '2026.04.25')  → '{dateFull} 출금예정 ({dateMD} 기준)' 형태
 *   - dateMD      — 기준 날짜 (기본값: '04.12')
 *   - totalAmount — 총 청구금액 숫자만 (기본값: '350,000')  + 고정 '원'
 *   - revolving   — 리볼빙 금액 숫자만 (기본값: '0')         + 고정 '원'
 *   - cardLoan    — 카드론 금액 숫자만 (기본값: '0')         + 고정 '원'
 *   - cashAdvance — 현금서비스 금액 숫자만 (기본값: '0')     + 고정 '원'
 *
 * 컴포넌트 이름: "CardPaymentSummary"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH = 390;

export async function createCardPaymentSummary(): Promise<ComponentNode> {
  const comp = createComponent('CardPaymentSummary');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(CARD_WIDTH, 1);           /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  comp.effects = [{
    type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 },
    offset: { x: 0, y: 2 }, radius: 8, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* ── 상단: 날짜 + 총 청구금액 (가운데 정렬) ── */
  const top = figma.createFrame();
  setAutoLayout(top, 'VERTICAL', SPACING.xs, 'MIN');
  top.primaryAxisAlignItems  = 'MIN';
  top.counterAxisAlignItems  = 'CENTER'; /* 자식 가운데 정렬 */
  setPadding(top, SPACING.md, SPACING.md, SPACING.lg, SPACING.md);
  top.resize(CARD_WIDTH, 1);            /* resize 먼저 */
  top.primaryAxisSizingMode  = 'AUTO';
  top.counterAxisSizingMode  = 'FIXED';
  clearFill(top);
  comp.appendChild(top);

  /* 년월 행 (ChevronDown) */
  const monthRow = figma.createFrame();
  setAutoLayout(monthRow, 'HORIZONTAL', SPACING.xs);
  monthRow.primaryAxisAlignItems = 'MIN';
  monthRow.counterAxisAlignItems = 'CENTER';
  monthRow.resize(1, 1);               /* resize 먼저 */
  monthRow.primaryAxisSizingMode = 'AUTO';
  monthRow.counterAxisSizingMode = 'AUTO';
  clearFill(monthRow);
  top.appendChild(monthRow);
  /* dateYM — monthRow → top → comp: 수동 바인딩 */
  await addTextWithVar(monthRow, '2026년 4월', FONT_SIZE.lg, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeLg, 'dateYM', comp);
  monthRow.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));

  /* 날짜 행: {dateFull} 출금예정 ({dateMD} 기준) — gap=0 HORIZONTAL */
  const dateRow = figma.createFrame();
  setAutoLayout(dateRow, 'HORIZONTAL', 0);
  dateRow.primaryAxisAlignItems = 'MIN';
  dateRow.counterAxisAlignItems = 'CENTER';
  dateRow.resize(1, 1);                /* resize 먼저 */
  dateRow.primaryAxisSizingMode = 'AUTO';
  dateRow.counterAxisSizingMode = 'AUTO';
  clearFill(dateRow);
  top.appendChild(dateRow);
  /* dateFull — dateRow → top → comp: 수동 바인딩 */
  await addTextWithVar(dateRow, '2026.04.25', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm, 'dateFull', comp);
  /* 고정 구분 텍스트 */
  await addTextWithVar(dateRow, ' 출금예정 (', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);
  /* dateMD — dateRow → top → comp: 수동 바인딩 */
  await addTextWithVar(dateRow, '04.12', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm, 'dateMD', comp);
  /* 고정 마무리 텍스트 */
  await addTextWithVar(dateRow, ' 기준)', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);

  /* 총 청구금액 — 숫자 + 고정 '원' */
  const totalRow = figma.createFrame();
  setAutoLayout(totalRow, 'HORIZONTAL', 0);
  totalRow.primaryAxisAlignItems = 'MIN';
  totalRow.counterAxisAlignItems = 'CENTER';
  totalRow.resize(1, 1);               /* resize 먼저 */
  totalRow.primaryAxisSizingMode = 'AUTO';
  totalRow.counterAxisSizingMode = 'AUTO';
  clearFill(totalRow);
  top.appendChild(totalRow);
  /* totalAmount (숫자만) — totalRow → top → comp: 수동 바인딩 */
  await addTextWithVar(totalRow, '350,000', FONT_SIZE['3xl'], COLOR_VAR.brandPrimary, BRAND.primary, true, SIZE_VAR.fontSize3xl, 'totalAmount', comp);
  /* 고정 '원' */
  await addTextWithVar(totalRow, '원', FONT_SIZE['3xl'], COLOR_VAR.brandPrimary, BRAND.primary, true, SIZE_VAR.fontSize3xl);

  /* ── 하단: 세부 3열 ── */
  const bottom = figma.createFrame();
  setAutoLayout(bottom, 'HORIZONTAL', 0);
  bottom.primaryAxisAlignItems = 'MIN';
  bottom.counterAxisAlignItems = 'MIN';
  setPadding(bottom, SPACING.lg, 0, SPACING.md, 0);
  bottom.resize(CARD_WIDTH, 1);         /* resize 먼저 */
  bottom.primaryAxisSizingMode = 'FIXED';
  bottom.counterAxisSizingMode = 'AUTO';
  clearFill(bottom);
  comp.appendChild(bottom);

  const colDefs: { label: string; propName: 'revolving' | 'cardLoan' | 'cashAdvance' }[] = [
    { label: '일부결제금액\n이월약정(리볼빙)', propName: 'revolving' },
    { label: '장기카드대출\n(카드론)',         propName: 'cardLoan' },
    { label: '단기카드대출\n(현금서비스)',     propName: 'cashAdvance' },
  ];

  for (let i = 0; i < colDefs.length; i++) {
    const col = figma.createFrame();
    setAutoLayout(col, 'VERTICAL', SPACING.xs);
    col.primaryAxisAlignItems = 'MIN';
    col.counterAxisAlignItems = 'CENTER'; /* 금액 행 가운데 정렬 */
    setPadding(col, 0, SPACING.xs);
    col.resize(1, 1);                    /* resize 먼저 */
    col.layoutGrow = 1;                  /* 3등분 균일 너비 */
    col.primaryAxisSizingMode = 'AUTO';
    clearFill(col);

    /* 중간 열 border-x */
    if (i === 1) {
      await setStrokeWithVar(col, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
      col.strokeTopWeight = 0; col.strokeBottomWeight = 0;
      col.strokeLeftWeight = 1; col.strokeRightWeight = 1;
    }

    bottom.appendChild(col);

    /* 레이블 — 고정 텍스트 */
    const labelNode = await addTextWithVar(col, colDefs[i].label, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    labelNode.textAlignHorizontal = 'CENTER';

    /* 금액 행: 숫자(bound) + 고정 '원' */
    const amountRow = figma.createFrame();
    setAutoLayout(amountRow, 'HORIZONTAL', 0);
    amountRow.primaryAxisAlignItems = 'MIN';
    amountRow.counterAxisAlignItems = 'CENTER';
    amountRow.resize(1, 1);              /* resize 먼저 */
    amountRow.primaryAxisSizingMode = 'AUTO';
    amountRow.counterAxisSizingMode = 'AUTO';
    clearFill(amountRow);
    col.appendChild(amountRow);

    /* 금액 숫자만 — amountRow → col → bottom → comp: 수동 바인딩 */
    const amountNode = await addTextWithVar(amountRow, '0', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, colDefs[i].propName, comp);
    amountNode.textAlignHorizontal = 'CENTER';
    /* 고정 '원' */
    await addTextWithVar(amountRow, '원', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
