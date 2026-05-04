/**
 * @file createDatePicker.ts
 * @description Figma DatePicker 컴포넌트 세트 생성.
 * mode(single|range) × state(Closed|Open) = 4 variants.
 * Open 상태에서는 트리거 버튼 + 달력 패널(7열 날짜 그리드)을 렌더링한다.
 * 컴포넌트 이름: "DatePicker"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

type DatePickerMode  = 'Single' | 'Range';
type DatePickerState = 'Closed' | 'Open';

const TRIGGER_WIDTH   = 280;
const CALENDAR_WIDTH  = 288; /* w-72 */
const CALENDAR_DAYS   = ['일', '월', '화', '수', '목', '금', '토'];
const CELL_SIZE       = 32;  /* size-8 */
const CELL_GAP        = 4;   /* gap-xs */

/** 달력 날짜 셀 생성 */
async function createDayCell(
  day: number,
  state: 'normal' | 'selected' | 'inRange' | 'empty',
): Promise<FrameNode> {
  const cell = figma.createFrame();
  cell.resize(CELL_SIZE, CELL_SIZE);
  cell.layoutMode = 'NONE';
  await setFloatVar(cell, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

  if (state === 'selected') {
    await setFillWithVar(cell, COLOR_VAR.brandPrimary, BRAND.primary);
  } else if (state === 'inRange') {
    cell.fills = [{ type: 'SOLID', color: BRAND.primary, opacity: 0.05 }];
  } else {
    clearFill(cell);
  }

  if (state !== 'empty') {
    const text = figma.createText();
    text.fontName = { family: 'Noto Sans KR', style: state === 'selected' ? 'Bold' : 'Regular' };
    text.fontSize = FONT_SIZE.xs;
    text.characters = String(day);
    text.fills = [{
      type: 'SOLID',
      color: state === 'selected' ? { r: 1, g: 1, b: 1 }
           : state === 'inRange'  ? BRAND.text
           : COLOR.textBase,
    }];
    /* 날짜 숫자 중앙 배치 */
    text.resize(CELL_SIZE, CELL_SIZE);
    text.textAlignHorizontal = 'CENTER';
    text.textAlignVertical   = 'CENTER';
    cell.appendChild(text);
  }

  return cell;
}

/** 달력 패널 프레임 생성 */
async function createCalendarPanel(mode: DatePickerMode): Promise<FrameNode> {
  const panel = figma.createFrame();
  setAutoLayout(panel, 'VERTICAL', SPACING.md, 'MIN');
  setPadding(panel, SPACING.md, SPACING.md);
  panel.resize(CALENDAR_WIDTH, 1);
  panel.primaryAxisSizingMode = 'AUTO';
  panel.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(panel, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(panel, COLOR_VAR.border, COLOR.border);
  await setFloatVar(panel, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  /* 월 이동 헤더 */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0);
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  header.resize(CALENDAR_WIDTH - SPACING.md * 2, 32);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'FIXED';
  clearFill(header);

  const prevBtn = figma.createFrame();
  prevBtn.resize(32, 32);
  prevBtn.layoutMode = 'NONE';
  clearFill(prevBtn);
  prevBtn.appendChild(createIcon('ChevronLeft', 16, COLOR.textMuted));
  header.appendChild(prevBtn);

  const monthLabel = await addTextWithVar(
    header, '2026년 4월', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm,
  );
  monthLabel.layoutGrow = 1;
  monthLabel.textAlignHorizontal = 'CENTER';

  const nextBtn = figma.createFrame();
  nextBtn.resize(32, 32);
  nextBtn.layoutMode = 'NONE';
  clearFill(nextBtn);
  nextBtn.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));
  header.appendChild(nextBtn);

  panel.appendChild(header);

  /* 요일 헤더 (7열) */
  const dayHeaderRow = figma.createFrame();
  dayHeaderRow.layoutMode = 'HORIZONTAL';
  dayHeaderRow.itemSpacing = CELL_GAP;
  dayHeaderRow.resize(CALENDAR_WIDTH - SPACING.md * 2, 24);
  dayHeaderRow.primaryAxisSizingMode = 'FIXED';
  dayHeaderRow.counterAxisSizingMode = 'FIXED';
  clearFill(dayHeaderRow);

  for (const dayLabel of CALENDAR_DAYS) {
    const dayText = figma.createText();
    dayText.fontName = { family: 'Noto Sans KR', style: 'Regular' };
    dayText.fontSize = FONT_SIZE.xs;
    dayText.characters = dayLabel;
    dayText.fills = [{ type: 'SOLID', color: COLOR.textMuted }];
    dayText.resize(CELL_SIZE, 24);
    dayText.textAlignHorizontal = 'CENTER';
    dayHeaderRow.appendChild(dayText);
  }
  panel.appendChild(dayHeaderRow);

  /* 날짜 그리드 (샘플: 4월 기준 — 화요일 시작) */
  /* 4월 1일은 화요일(2) → 빈 셀 2개 + 1~30 */
  const SAMPLE_FIRST_DAY = 2; /* 화요일 */
  const SAMPLE_LAST_DATE = 30;

  const gridFrame = figma.createFrame();
  gridFrame.layoutMode = 'NONE';
  const totalCells = SAMPLE_FIRST_DAY + SAMPLE_LAST_DATE;
  const gridRows = Math.ceil(totalCells / 7);
  gridFrame.resize(CALENDAR_WIDTH - SPACING.md * 2, gridRows * CELL_SIZE + (gridRows - 1) * CELL_GAP);
  clearFill(gridFrame);

  for (let i = 0; i < totalCells; i++) {
    const col = i % 7;
    const row = Math.floor(i / 7);
    const day = i - SAMPLE_FIRST_DAY + 1;

    let cellState: 'normal' | 'selected' | 'inRange' | 'empty' = 'empty';
    if (i >= SAMPLE_FIRST_DAY) {
      /* Range 모드: 5~15일을 범위로 표시 */
      if (mode === 'Range') {
        if (day === 5 || day === 15) cellState = 'selected';
        else if (day > 5 && day < 15) cellState = 'inRange';
        else cellState = 'normal';
      } else {
        /* Single 모드: 10일 선택 */
        cellState = day === 10 ? 'selected' : 'normal';
      }
    }

    const cell = await createDayCell(day, cellState);
    cell.x = col * (CELL_SIZE + CELL_GAP);
    cell.y = row * (CELL_SIZE + CELL_GAP);
    gridFrame.appendChild(cell);
  }

  panel.appendChild(gridFrame);
  return panel;
}

async function createDatePickerVariant(mode: DatePickerMode, state: DatePickerState): Promise<ComponentNode> {
  const comp = createComponent(`Mode=${mode}, State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 레이블 */
  await addTextWithVar(
    comp, '날짜 선택', FONT_SIZE.xs,
    COLOR_VAR.textLabel, COLOR.textLabel, true, SIZE_VAR.fontSizeXs,
  );

  /* 트리거 버튼 (h-12 = 48px) */
  const trigger = figma.createFrame();
  setAutoLayout(trigger, 'HORIZONTAL', 0);
  trigger.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(trigger, 0, SPACING.standard);
  trigger.resize(TRIGGER_WIDTH, 48);
  trigger.primaryAxisSizingMode = 'FIXED';
  trigger.counterAxisSizingMode = 'FIXED';
  await setFloatVar(trigger, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  await setFillWithVar(trigger, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(
    trigger,
    state === 'Open' ? COLOR_VAR.brandText : COLOR_VAR.border,
    state === 'Open' ? BRAND.text          : COLOR.border,
  );

  const triggerText = mode === 'Single' ? '날짜를 선택하세요' : '시작일 ~ 종료일';
  await addTextWithVar(
    trigger, triggerText, FONT_SIZE.sm,
    COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, SIZE_VAR.fontSizeSm,
  );
  comp.appendChild(trigger);

  /* Open 상태: 달력 패널 표시 */
  if (state === 'Open') {
    comp.appendChild(await createCalendarPanel(mode));
  }

  return comp;
}

export async function createDatePicker(): Promise<ComponentSetNode> {
  const modes:  DatePickerMode[]  = ['Single', 'Range'];
  const states: DatePickerState[] = ['Closed', 'Open'];
  const components: ComponentNode[] = [];

  for (const mode of modes) {
    for (const state of states) {
      components.push(await createDatePickerVariant(mode, state));
    }
  }
  /* cols=2: Closed|Open 한 행, 행마다 Mode 변경 */
  return combineVariants(components, 'DatePicker', 2);
}
