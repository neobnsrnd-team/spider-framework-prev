/**
 * @file createCardInfoPanel.ts
 * @description Figma CardInfoPanel 컴포넌트 생성.
 * 섹션 제목 + 레이블-값 행 목록을 렌더링하는 정보 패널.
 * 2개 섹션, 구분선 포함. 단일 variant.
 * 컴포넌트 이름: "CardInfoPanel"
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, addRect,
} from '../../../helpers';

const PANEL_WIDTH = 390;

/** 레이블-값 단일 행 */
async function createInfoRow(parent: FrameNode | ComponentNode, label: string, value: string): Promise<void> {
  const row = figma.createFrame();
  setAutoLayout(row, 'HORIZONTAL', 0);
  row.primaryAxisAlignItems = 'SPACE_BETWEEN';
  row.counterAxisAlignItems = 'MIN'; /* items-start */
  setPadding(row, SPACING.xs, 0);
  row.resize(PANEL_WIDTH - SPACING.standard * 2, 1);
  row.primaryAxisSizingMode = 'FIXED';
  row.counterAxisSizingMode = 'AUTO';
  clearFill(row);

  await addTextWithVar(row, label, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  await addTextWithVar(row, value, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeXs);

  parent.appendChild(row);
}

/** 섹션 제목 + 행 목록 블록 */
async function createSection(
  parent: FrameNode | ComponentNode,
  title: string,
  rows: Array<{ label: string; value: string }>,
): Promise<void> {
  await addTextWithVar(parent, title, FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  for (const { label, value } of rows) {
    await createInfoRow(parent, label, value);
  }
}

export async function createCardInfoPanel(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(PANEL_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  await createSection(comp, '결제정보', [
    { label: '결제 계좌', value: '하나은행 123456****1234' },
    { label: '결제일',   value: '매월 25일' },
  ]);

  /* 구분선 — RectangleNode에 padding 속성이 없으므로 여백은 Auto Layout gap에 위임 */
  addRect(comp, PANEL_WIDTH - SPACING.standard * 2, 1, COLOR.borderSubtle);

  await createSection(comp, '카드 이용기간', [
    { label: '일시불/할부',    value: '2026.03.13 ~ 2026.04.12' },
    { label: '단기카드대출', value: '2026.02.26 ~ 2026.03.25' },
  ]);

  figma.currentPage.appendChild(comp);
  return comp;
}
