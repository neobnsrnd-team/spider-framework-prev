/**
 * @file createTransactionList.ts
 * @description Figma TransactionList 컴포넌트 세트 생성.
 * 날짜별 그룹 헤더 + 거래 항목 목록 컴포넌트.
 *
 * State(Default|Loading|Empty) = 3 variants.
 * - Default: 날짜 헤더 + 입금/출금/이체 샘플 항목 3건
 * - Loading: 스켈레톤 플레이스홀더 3행
 * - Empty: 빈 상태 안내 텍스트
 *
 * 거래 유형별 금액 색상:
 *   입금(deposit)   → successText
 *   출금(withdrawal)→ dangerText
 *   이체(transfer)  → textHeading
 *
 * 컴포넌트 이름: "TransactionList"
 */

import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, addRect,
} from '../../../utils/helpers';

const LIST_WIDTH = 390;

type TransactionType = 'deposit' | 'withdrawal' | 'transfer';

/** 거래 유형별 금액 색상 */
function amountColorVar(type: TransactionType): string {
  if (type === 'deposit')    return COLOR_VAR.successText;
  if (type === 'withdrawal') return COLOR_VAR.dangerText;
  return COLOR_VAR.textHeading;
}

function amountColorFallback(type: TransactionType) {
  if (type === 'deposit')    return COLOR.successText;
  if (type === 'withdrawal') return COLOR.dangerText;
  return COLOR.textHeading;
}

/** 단일 거래 항목 행 생성 */
async function createTransactionItem(
  title: string,
  time: string,
  amount: string,
  balance: string,
  type: TransactionType,
): Promise<FrameNode> {
  const row = figma.createFrame();
  setAutoLayout(row, 'HORIZONTAL', SPACING.standard);
  setPadding(row, SPACING.md, SPACING.standard);
  row.resize(LIST_WIDTH, 1);
  row.primaryAxisSizingMode = 'FIXED';
  row.counterAxisSizingMode = 'AUTO';
  row.counterAxisAlignItems = 'CENTER';
  clearFill(row);

  /* 좌측: 거래명 + 시간 */
  const left = figma.createFrame();
  setAutoLayout(left, 'VERTICAL', SPACING.xs, 'MIN');
  left.layoutGrow = 1;
  clearFill(left);

  await addTextWithVar(left, title, FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  await addTextWithVar(left, time, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  row.appendChild(left);

  /* 우측: 금액 + 잔액 */
  const right = figma.createFrame();
  setAutoLayout(right, 'VERTICAL', SPACING.xs, 'MIN');
  right.counterAxisAlignItems = 'MAX';
  clearFill(right);

  await addTextWithVar(right, amount, FONT_SIZE.sm, amountColorVar(type), amountColorFallback(type), true, SIZE_VAR.fontSizeSm);
  await addTextWithVar(right, `잔액 ${balance}`, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  row.appendChild(right);

  return row;
}

/** 날짜 그룹 헤더 생성 */
async function createDateHeader(date: string): Promise<FrameNode> {
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0, 'MIN');
  setPadding(header, SPACING.xs, SPACING.standard);
  header.resize(LIST_WIDTH, 1);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  await setFillWithVar(header, COLOR_VAR.surface, COLOR.surface);

  await addTextWithVar(header, date, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  return header;
}

/** 구분선 생성 */
function createDividerLine(): RectangleNode {
  const line = figma.createRectangle();
  line.resize(LIST_WIDTH - SPACING.standard * 2, 1);
  line.fills = [{ type: 'SOLID', color: COLOR.borderSubtle }];
  return line;
}

/** Default variant: 날짜 헤더 + 3건 샘플 */
async function createDefaultVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=Default');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(LIST_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  clearFill(comp);

  comp.appendChild(await createDateHeader('4월 28일'));

  const items: Array<{ title: string; time: string; amount: string; balance: string; type: TransactionType }> = [
    { title: '급여',       time: '09:00:00', amount: '+3,500,000원', balance: '5,234,567원', type: 'deposit' },
    { title: '카페 결제',  time: '11:30:15', amount: '-4,500원',     balance: '5,230,067원', type: 'withdrawal' },
    { title: '가족 이체',  time: '18:22:33', amount: '-500,000원',   balance: '4,730,067원', type: 'transfer' },
  ];

  for (const item of items) {
    comp.appendChild(await createTransactionItem(item.title, item.time, item.amount, item.balance, item.type));
    comp.appendChild(createDividerLine());
  }

  return comp;
}

/** Loading variant: 스켈레톤 3행 */
async function createLoadingVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=Loading');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(LIST_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  clearFill(comp);

  for (let i = 0; i < 3; i++) {
    const row = figma.createFrame();
    setAutoLayout(row, 'HORIZONTAL', SPACING.standard);
    setPadding(row, SPACING.md, SPACING.standard);
    row.resize(LIST_WIDTH, 60);
    row.primaryAxisSizingMode = 'FIXED';
    row.counterAxisSizingMode = 'FIXED';
    row.counterAxisAlignItems = 'CENTER';
    clearFill(row);

    /* 좌측 스켈레톤 블록 */
    const leftSkel = figma.createFrame();
    setAutoLayout(leftSkel, 'VERTICAL', SPACING.xs, 'MIN');
    leftSkel.layoutGrow = 1;
    clearFill(leftSkel);
    addRect(leftSkel, 120, 14, COLOR.surfaceRaised, 4);
    addRect(leftSkel, 60, 10, COLOR.surfaceRaised, 4);
    row.appendChild(leftSkel);

    /* 우측 스켈레톤 블록 */
    addRect(row, 80, 14, COLOR.surfaceRaised, 4);

    comp.appendChild(row);
  }

  return comp;
}

/** Empty variant: 빈 상태 */
async function createEmptyVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=Empty');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, SPACING['2xl'], SPACING.standard);
  comp.resize(LIST_WIDTH, 120);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  await addTextWithVar(comp, '거래 내역이 없어요', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm, 'emptyMessage');
  return comp;
}

export async function createTransactionList(): Promise<ComponentSetNode> {
  const components = [
    await createDefaultVariant(),
    await createLoadingVariant(),
    await createEmptyVariant(),
  ];
  return combineVariants(components, 'TransactionList', 1);
}
