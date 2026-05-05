/**
 * @file createPinConfirmSheet.ts
 * @description Figma PinConfirmSheet 컴포넌트 세트 생성.
 * PinLength(4|6) × State(default|error) = 4 variants.
 *
 * TEXT properties:
 *   - title        — 시트 제목 (기본값: '비밀번호 입력')
 *   - subtitle     — PIN 안내 문구 (기본값: '비밀번호를 입력하세요')
 *   - errorMessage — 오류 메시지, State=error 전용 (기본값: '비밀번호가 올바르지 않습니다')
 *
 * [레이아웃]
 *   comp(VERTICAL, FIXED 390, AUTO, surface, top-radius)
 *     HandleBar
 *     Header(HORIZONTAL): [Spacer(32×32)] [title(grow=1, CENTER)] [CloseButton(32×32)]
 *     PinSection(VERTICAL): [subtitle] [PinDots] [errorMessage — error only]
 *     Keypad(3×4 그리드)
 *
 * TEXT property 바인딩 타이밍:
 *   title       — comp.appendChild(header) 이후 (2단계: comp → header → title)
 *   subtitle    — comp.appendChild(pinSection) 이후 (2단계: comp → pinSection → subtitle)
 *   errorMessage— 동일 (2단계: comp → pinSection → errorMessage)
 *
 * 컴포넌트 이름: "PinConfirmSheet"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const SHEET_W   = 390;
const CELL_H    = 56;   /* 키패드 셀 높이 h-14 */
const DOT_SIZE  = 16;   /* PIN 도트 크기 size-4 */
const DOT_GAP   = 20;   /* 도트 간격 gap-5 */

type PinLength = 4 | 6;
type PinState  = 'default' | 'error';

/** 드래그 핸들 바 */
function createHandleBar(): FrameNode {
  const wrap = figma.createFrame();
  setAutoLayout(wrap, 'HORIZONTAL', 0);
  wrap.resize(SHEET_W, 28);
  wrap.primaryAxisSizingMode = 'FIXED';
  wrap.counterAxisSizingMode = 'FIXED';
  clearFill(wrap);

  const bar = figma.createFrame();
  bar.name = 'Bar';
  bar.resize(40, 4);
  bar.layoutMode = 'NONE';
  bar.cornerRadius = RADIUS.full;
  bar.fills = [{ type: 'SOLID', color: COLOR.border }];
  wrap.appendChild(bar);

  return wrap;
}

/** PIN 도트 n개 행 (filledCount개 채워짐) */
async function createPinDots(pinLength: PinLength, filledCount: number): Promise<FrameNode> {
  const row = figma.createFrame();
  row.name = 'PinDots';
  setAutoLayout(row, 'HORIZONTAL', DOT_GAP);
  row.primaryAxisSizingMode = 'AUTO';
  row.counterAxisSizingMode = 'AUTO';
  clearFill(row);

  for (let i = 0; i < pinLength; i++) {
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

/** 숫자 키패드 셀 */
async function createKeyCell(label: string, isDelete = false): Promise<FrameNode> {
  const cellW = Math.floor(SHEET_W / 3);
  const cell = figma.createFrame();
  cell.name = `Key_${label}`;
  setAutoLayout(cell, 'HORIZONTAL', 0, 'CENTER');
  cell.resize(cellW, CELL_H);
  cell.primaryAxisSizingMode = 'FIXED';
  cell.counterAxisSizingMode = 'FIXED';
  clearFill(cell);
  await setFloatVar(cell, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (isDelete) {
    cell.appendChild(createIcon('Delete', 24, COLOR.textHeading));
  } else {
    const fontSize = label === '재배열' ? FONT_SIZE.base : FONT_SIZE.xl;
    await addTextWithVar(
      cell, label, fontSize,
      COLOR_VAR.textHeading, COLOR.textHeading,
      true, label === '재배열' ? SIZE_VAR.fontSizeBase : SIZE_VAR.fontSizeXl,
    );
  }
  return cell;
}

/** 3×4 숫자 키패드 그리드 */
async function createKeypad(): Promise<FrameNode> {
  const cellW = Math.floor(SHEET_W / 3);
  const grid  = figma.createFrame();
  grid.name   = 'Keypad';
  grid.layoutMode = 'NONE';
  grid.resize(SHEET_W, CELL_H * 4);
  clearFill(grid);

  const labels = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '재배열', '0', '⌫'];
  for (let i = 0; i < labels.length; i++) {
    const cell = await createKeyCell(labels[i], labels[i] === '⌫');
    cell.x = (i % 3) * cellW;
    cell.y = Math.floor(i / 3) * CELL_H;
    grid.appendChild(cell);
  }

  return grid;
}

/**
 * PinConfirmSheet 단일 variant 생성.
 *
 * @returns { comp, headerTitleText, subtitleText, errorText? }
 *   TEXT 노드를 반환하여 호출부에서 comp에 append 후 property 바인딩에 사용
 */
async function createPinConfirmSheetVariant(
  pinLength: PinLength,
  state: PinState,
): Promise<ComponentNode> {
  const comp = createComponent(`PinLength=${pinLength}, State=${state}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SHEET_W, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setFloatVar(comp, 'topLeftRadius',  SIZE_VAR.radiusXl, RADIUS.xl);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  comp.bottomLeftRadius  = 0;
  comp.bottomRightRadius = 0;

  /* ── Handle Bar ────────────────────────────────────────────── */
  comp.appendChild(createHandleBar());

  /* ── Header ────────────────────────────────────────────────── */
  const header = figma.createFrame();
  header.name = 'Header';
  setAutoLayout(header, 'HORIZONTAL', 0, 'CENTER');
  setPadding(header, SPACING.standard, SPACING.standard);
  header.resize(SHEET_W, 1);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  clearFill(header);

  /* CloseButton(32×32)과 동일한 Spacer를 왼쪽에 추가 → title이 정확히 가운데 위치 */
  const spacer = figma.createFrame();
  spacer.name = 'Spacer';
  spacer.resize(32, 32);
  spacer.layoutMode = 'NONE';
  clearFill(spacer);
  header.appendChild(spacer);

  const titleText = await addTextWithVar(
    header, '비밀번호 입력', FONT_SIZE.lg,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeLg,
  );
  titleText.layoutGrow = 1;
  titleText.textAlignHorizontal = 'CENTER';

  const closeBtn = figma.createFrame();
  closeBtn.name = 'CloseButton';
  closeBtn.resize(32, 32);
  closeBtn.layoutMode = 'NONE';
  clearFill(closeBtn);
  closeBtn.appendChild(createIcon('X', 20, COLOR.textMuted));
  header.appendChild(closeBtn);

  /* comp.appendChild(header) 이후 title TEXT property 바인딩 (2단계 ✓) */
  comp.appendChild(header);
  const titleKey = comp.addComponentProperty('title', 'TEXT', '비밀번호 입력');
  titleText.componentPropertyReferences = { characters: titleKey };

  /* ── Pin Section ───────────────────────────────────────────── */
  const pinSection = figma.createFrame();
  pinSection.name = 'PinSection';
  setAutoLayout(pinSection, 'VERTICAL', SPACING.xl, 'CENTER');
  setPadding(pinSection, SPACING.xl, SPACING.standard);
  pinSection.resize(SHEET_W, 1);
  pinSection.primaryAxisSizingMode = 'AUTO';
  pinSection.counterAxisSizingMode = 'FIXED';
  clearFill(pinSection);

  const subtitleText = await addTextWithVar(
    pinSection, '비밀번호를 입력하세요', FONT_SIZE.base,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeBase,
  );

  /* 도트: default=절반 채움(시각 프리뷰), error=모두 비움(초기화 상태) */
  const filledCount = state === 'default' ? Math.floor(pinLength / 2) : 0;
  pinSection.appendChild(await createPinDots(pinLength, filledCount));

  let errorText: TextNode | undefined;
  if (state === 'error') {
    errorText = await addTextWithVar(
      pinSection, '비밀번호가 올바르지 않습니다', FONT_SIZE.xs,
      COLOR_VAR.dangerText, COLOR.dangerText,
      false, SIZE_VAR.fontSizeXs,
    );
  }

  /* comp.appendChild(pinSection) 이후 subtitle / errorMessage 바인딩 (2단계 ✓) */
  comp.appendChild(pinSection);

  const subtitleKey = comp.addComponentProperty('subtitle', 'TEXT', '비밀번호를 입력하세요');
  subtitleText.componentPropertyReferences = { characters: subtitleKey };

  if (errorText) {
    const errorKey = comp.addComponentProperty('errorMessage', 'TEXT', '비밀번호가 올바르지 않습니다');
    errorText.componentPropertyReferences = { characters: errorKey };
  }

  /* ── Keypad ─────────────────────────────────────────────────── */
  comp.appendChild(await createKeypad());

  return comp;
}

/**
 * PinConfirmSheet ComponentSet 생성.
 * PinLength(4|6) × State(default|error) = 4 variants, cols=2.
 * Row 1: PinLength=4 — default / error
 * Row 2: PinLength=6 — default / error
 */
export async function createPinConfirmSheet(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];
  for (const pinLength of [4, 6] as PinLength[]) {
    for (const state of ['default', 'error'] as PinState[]) {
      variants.push(await createPinConfirmSheetVariant(pinLength, state));
    }
  }
  return combineVariants(variants, 'PinConfirmSheet', 2);
}
