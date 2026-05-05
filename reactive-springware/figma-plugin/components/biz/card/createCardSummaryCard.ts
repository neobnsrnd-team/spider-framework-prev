/**
 * @file createCardSummaryCard.ts
 * @description Figma CardSummaryCard 컴포넌트 세트 생성.
 * Type(Credit|Check|Prepaid) = 3 variants.
 * - Credit:  이번달 사용금액 + 한도(limitAmount) 표시
 * - Check:   연결계좌 잔액
 * - Prepaid: 충전 잔액
 *
 * TEXT properties:
 *   - cardName    — 카드명          (기본값: '하나 머니 체크카드')
 *   - badgeText   — 배지 텍스트     (기본값: '포인트 적립')
 *   - cardNumber  — 마스킹 카드번호 (기본값: '1234 **** **** 5678')
 *   - amount      — 금액            (기본값: type별 상이)
 *   - limitAmount — 한도 금액       (기본값: '한도 600,000원', Credit 전용)
 *
 * 컴포넌트 이름: "CardSummaryCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING.lg * 2; /* 양쪽 padding-lg 제외 */

type CardType = 'Credit' | 'Check' | 'Prepaid';

/* 금액 레이블 — 고정 텍스트 (TEXT property 없음) */
const AMOUNT_LABEL: Record<CardType, string> = {
  Credit:  '이번달 사용금액',
  Check:   '연결계좌 잔액',
  Prepaid: '충전 잔액',
};

const AMOUNT_VALUE: Record<CardType, string> = {
  Credit:  '480,000원',
  Check:   '1,234,567원',
  Prepaid: '50,000원',
};

async function createCardSummaryVariant(type: CardType): Promise<ComponentNode> {
  const comp = createComponent(`Type=${type}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(CARD_WIDTH, 1);           /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign  = 'INSIDE';

  /* ── 상단: 카드명 + 배지 (좌측 정렬, badge를 cardName 바로 오른쪽에 붙임) ── */
  const headerRow = figma.createFrame();
  setAutoLayout(headerRow, 'HORIZONTAL', SPACING.sm);
  headerRow.primaryAxisAlignItems = 'MIN'; /* 좌측 정렬 */
  headerRow.counterAxisAlignItems = 'CENTER';
  headerRow.resize(CONTENT_WIDTH, 1);    /* resize 먼저 */
  headerRow.primaryAxisSizingMode = 'FIXED';
  headerRow.counterAxisSizingMode = 'AUTO'; /* height 텍스트 높이에 맞게 */
  clearFill(headerRow);
  comp.appendChild(headerRow);

  /* cardName — headerRow → comp: 수동 바인딩 */
  await addTextWithVar(headerRow, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName', comp);

  /* 배지 */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  setPadding(badge, 2, SPACING.sm);
  badge.resize(1, 1);                    /* resize 먼저 */
  badge.primaryAxisSizingMode = 'AUTO';  /* width 텍스트+padding에 맞게 */
  badge.counterAxisSizingMode = 'AUTO';  /* height 텍스트+padding에 맞게 */
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
  headerRow.appendChild(badge);
  /* badgeText — badge → headerRow → comp: 수동 바인딩 */
  await addTextWithVar(badge, '포인트 적립', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs, 'badgeText', comp);

  /* 마스킹 카드번호 — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '1234 **** **** 5678', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'cardNumber');

  /* ── 금액 영역 ── */
  const amountSection = figma.createFrame();
  setAutoLayout(amountSection, 'VERTICAL', SPACING.xs, 'MIN');
  amountSection.primaryAxisAlignItems = 'MIN';
  setPadding(amountSection, SPACING.md, 0);
  amountSection.resize(CONTENT_WIDTH, 1); /* resize 먼저 */
  amountSection.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  amountSection.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  clearFill(amountSection);
  comp.appendChild(amountSection);

  /* 금액 레이블 — 고정 텍스트, TEXT property 없음 */
  await addTextWithVar(amountSection, AMOUNT_LABEL[type], FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  /* amount — amountSection → comp: 수동 바인딩 */
  await addTextWithVar(amountSection, AMOUNT_VALUE[type], FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl, 'amount', comp);

  /* limitAmount — Credit 전용, amountSection → comp: 수동 바인딩 */
  if (type === 'Credit') {
    await addTextWithVar(amountSection, '한도 600,000원', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'limitAmount', comp);
  }

  return comp;
}

export async function createCardSummaryCard(): Promise<ComponentSetNode> {
  const types: CardType[] = ['Credit', 'Check', 'Prepaid'];
  const components = await Promise.all(types.map(createCardSummaryVariant));
  return combineVariants(components, 'CardSummaryCard', 1);
}
