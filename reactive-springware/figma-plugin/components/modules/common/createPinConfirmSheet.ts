/**
 * @file createPinConfirmSheet.ts
 * @description Figma PinConfirmSheet 컴포넌트 세트 생성.
 * BottomSheet + PinDotIndicator + NumberKeypad 조합 컴포넌트.
 *
 * State(Idle|Filling|Error) = 3 variants.
 * - Idle:    PIN 미입력 (0/4 도트 빈 상태)
 * - Filling: PIN 입력 중 (2/4 도트 채워진 상태)
 * - Error:   PIN 오류 메시지 표시 + 도트 초기화
 *
 * 컴포넌트 이름: "PinConfirmSheet"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const SHEET_WIDTH  = 390;
const CELL_HEIGHT  = 56;  /* h-14 — 키패드 셀 높이 */
const DOT_SIZE     = 16;  /* size-4 */
const DOT_GAP      = 20;  /* gap-5 */
const PIN_LENGTH   = 4;

/** 하단 시트 핸들 바 (드래그 인디케이터) */
function createHandleBar(): FrameNode {
  const wrap = figma.createFrame();
  setAutoLayout(wrap, 'HORIZONTAL', 0);
  wrap.resize(SHEET_WIDTH, 28);
  wrap.primaryAxisSizingMode = 'FIXED';
  wrap.counterAxisSizingMode = 'FIXED';
  clearFill(wrap);

  const bar = figma.createFrame();
  bar.resize(40, 4);
  bar.layoutMode = 'NONE';
  bar.cornerRadius = RADIUS.full;
  bar.fills = [{ type: 'SOLID', color: COLOR.surfaceRaised }];
  wrap.appendChild(bar);

  return wrap;
}

/** 시트 헤더: 제목 + 닫기 버튼 */
async function createSheetHeader(): Promise<FrameNode> {
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0);
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(header, SPACING.standard, SPACING.standard);
  header.resize(SHEET_WIDTH, 1);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  clearFill(header);

  const title = await addTextWithVar(
    header, '비밀번호 입력', FONT_SIZE.lg,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeLg,
  );
  title.layoutGrow = 1;

  /* 닫기 아이콘 버튼 */
  const closeBtn = figma.createFrame();
  closeBtn.resize(32, 32);
  closeBtn.layoutMode = 'NONE';
  clearFill(closeBtn);
  closeBtn.appendChild(createIcon('X', 20, COLOR.textMuted));
  header.appendChild(closeBtn);

  return header;
}

/** PinDotIndicator (n개 채워짐) */
async function createPinDots(filledCount: number): Promise<FrameNode> {
  const row = figma.createFrame();
  setAutoLayout(row, 'HORIZONTAL', DOT_GAP);
  row.primaryAxisSizingMode = 'AUTO';
  row.counterAxisSizingMode = 'AUTO';
  clearFill(row);

  for (let i = 0; i < PIN_LENGTH; i++) {
    const dot = figma.createFrame();
    dot.resize(DOT_SIZE, DOT_SIZE);
    dot.layoutMode = 'NONE';
    await setFloatVar(dot, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

    if (i < filledCount) {
      await setFillWithVar(dot, COLOR_VAR.brandPrimary, BRAND.primary);
    } else {
      clearFill(dot);
      await setStrokeWithVar(dot, COLOR_VAR.borderSubtle, COLOR.borderSubtle, 2);
    }
    row.appendChild(dot);
  }

  return row;
}

/** 숫자 버튼 셀 */
async function createKeyCell(label: string, isText = true): Promise<FrameNode> {
  const cellWidth = Math.floor(SHEET_WIDTH / 3);
  const cell = figma.createFrame();
  setAutoLayout(cell, 'HORIZONTAL', 0);
  cell.resize(cellWidth, CELL_HEIGHT);
  cell.primaryAxisSizingMode = 'FIXED';
  cell.counterAxisSizingMode = 'FIXED';
  clearFill(cell);
  await setFloatVar(cell, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (isText) {
    await addTextWithVar(cell, label, FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl);
  } else {
    /* Delete 아이콘 */
    const icon = createIcon('Delete', 24, COLOR.textHeading);
    cell.appendChild(icon);
  }
  return cell;
}

/** 3×4 숫자 키패드 그리드 */
async function createKeypad(): Promise<FrameNode> {
  const cellWidth = Math.floor(SHEET_WIDTH / 3);
  const grid = figma.createFrame();
  grid.layoutMode = 'NONE';
  grid.resize(SHEET_WIDTH, CELL_HEIGHT * 4);
  clearFill(grid);

  const labels = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '재배열', '0', '⌫'];
  for (let i = 0; i < labels.length; i++) {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const isDelete = labels[i] === '⌫';
    /* '재배열'은 base 크기 텍스트 — 생성 후 fontSize 조정 */
    const cell = await createKeyCell(labels[i], !isDelete);

    if (isDelete) {
      /* Delete 셀 재생성 — 아이콘 전용 */
      clearFill(cell);
      cell.children.forEach(c => c.remove());
      const icon = createIcon('Delete', 24, COLOR.textHeading);
      cell.appendChild(icon);
    } else if (labels[i] === '재배열') {
      /* 재배열 텍스트는 base 크기 */
      const txt = cell.findOne(n => n.type === 'TEXT') as TextNode | null;
      if (txt) txt.fontSize = FONT_SIZE.base;
    }

    cell.x = col * cellWidth;
    cell.y = row * CELL_HEIGHT;
    grid.appendChild(cell);
  }

  return grid;
}

/** PIN 도트 + 안내 문구 + 선택적 오류 메시지 섹션 */
async function createPinSection(filledCount: number, showError: boolean): Promise<FrameNode> {
  const section = figma.createFrame();
  setAutoLayout(section, 'VERTICAL', SPACING.xl);
  setPadding(section, SPACING.xl, SPACING.standard);
  section.resize(SHEET_WIDTH, 1);
  section.primaryAxisSizingMode = 'FIXED';
  section.counterAxisSizingMode = 'AUTO';
  clearFill(section);

  await addTextWithVar(section, '비밀번호를 입력하세요', FONT_SIZE.base, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeBase);
  section.appendChild(await createPinDots(filledCount));

  if (showError) {
    await addTextWithVar(section, '비밀번호가 올바르지 않습니다', FONT_SIZE.xs, COLOR_VAR.dangerText, COLOR.dangerText, false, SIZE_VAR.fontSizeXs);
  }

  return section;
}

async function createPinConfirmSheetVariant(state: 'Idle' | 'Filling' | 'Error'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SHEET_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setFloatVar(comp, 'topLeftRadius',  SIZE_VAR.radiusXl, RADIUS.xl);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  comp.appendChild(createHandleBar());
  comp.appendChild(await createSheetHeader());

  const filledCount = state === 'Filling' ? 2 : 0;
  comp.appendChild(await createPinSection(filledCount, state === 'Error'));
  comp.appendChild(await createKeypad());

  return comp;
}

export async function createPinConfirmSheet(): Promise<ComponentSetNode> {
  const components = [
    await createPinConfirmSheetVariant('Idle'),
    await createPinConfirmSheetVariant('Filling'),
    await createPinConfirmSheetVariant('Error'),
  ];
  return combineVariants(components, 'PinConfirmSheet', 1);
}
