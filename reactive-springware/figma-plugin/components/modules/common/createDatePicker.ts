/**
 * @file createDatePicker.ts
 * @description Figma DatePicker 컴포넌트 세트 생성.
 * Mode(Single|Range) × Open(True|False) × Disabled(True|False) = 8 variants.
 *
 * TEXT properties:
 *   - label       — 입력 레이블 (기본값: '날짜 선택')
 *   - placeholder — 트리거 버튼 플레이스홀더
 *                   Single: '날짜를 선택하세요' / Range: '시작일 ~ 종료일'
 *
 * [레이아웃]
 *   Open=False: label + trigger(48px)
 *   Open=True:  label + trigger(48px) + 달력 패널(7열 날짜 그리드)
 *   Disabled=True: comp.opacity = 0.5
 *
 * 컴포넌트 이름: "DatePicker"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

type DatePickerMode = 'Single' | 'Range';

const TRIGGER_WIDTH  = 288;
const CALENDAR_WIDTH = 288; /* w-72 */
const CALENDAR_DAYS  = ['일', '월', '화', '수', '목', '금', '토'];
const CELL_SIZE      = 32;  /* size-8 */
const CELL_GAP       = 4;   /* gap-xs */

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
  const calHeader = figma.createFrame();
  setAutoLayout(calHeader, 'HORIZONTAL', 0);
  calHeader.primaryAxisAlignItems = 'SPACE_BETWEEN';
  calHeader.resize(CALENDAR_WIDTH - SPACING.md * 2, 32);
  calHeader.primaryAxisSizingMode = 'FIXED';
  calHeader.counterAxisSizingMode = 'FIXED';
  clearFill(calHeader);

  const prevBtn = figma.createFrame();
  prevBtn.resize(32, 32);
  prevBtn.layoutMode = 'NONE';
  clearFill(prevBtn);
  prevBtn.appendChild(createIcon('ChevronLeft', 16, COLOR.textMuted));
  calHeader.appendChild(prevBtn);

  const monthLabel = await addTextWithVar(
    calHeader, '2026년 4월', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm,
  );
  monthLabel.layoutGrow = 1;
  monthLabel.textAlignHorizontal = 'CENTER';

  const nextBtn = figma.createFrame();
  nextBtn.resize(32, 32);
  nextBtn.layoutMode = 'NONE';
  clearFill(nextBtn);
  nextBtn.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));
  calHeader.appendChild(nextBtn);
  panel.appendChild(calHeader);

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

  /* 날짜 그리드 (샘플: 4월 — 화요일 시작) */
  const SAMPLE_FIRST_DAY = 2; /* 화요일 */
  const SAMPLE_LAST_DATE = 30;
  const totalCells = SAMPLE_FIRST_DAY + SAMPLE_LAST_DATE;
  const gridRows   = Math.ceil(totalCells / 7);

  const gridFrame = figma.createFrame();
  gridFrame.layoutMode = 'NONE';
  gridFrame.resize(CALENDAR_WIDTH - SPACING.md * 2, gridRows * CELL_SIZE + (gridRows - 1) * CELL_GAP);
  clearFill(gridFrame);

  for (let i = 0; i < totalCells; i++) {
    const col = i % 7;
    const row = Math.floor(i / 7);
    const day = i - SAMPLE_FIRST_DAY + 1;
    let cellState: 'normal' | 'selected' | 'inRange' | 'empty' = 'empty';
    if (i >= SAMPLE_FIRST_DAY) {
      if (mode === 'Range') {
        cellState = (day === 5 || day === 15) ? 'selected'
                  : (day > 5 && day < 15)    ? 'inRange'
                  : 'normal';
      } else {
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

async function createDatePickerVariant(
  mode: DatePickerMode,
  open: boolean,
  disabled: boolean,
): Promise<ComponentNode> {
  const comp = createComponent(
    `Mode=${mode}, Open=${open ? 'True' : 'False'}, Disabled=${disabled ? 'True' : 'False'}`,
  );
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  if (disabled) comp.opacity = 0.5; /* opacity-50 */

  /* label TEXT property — comp 직접 자식(1단계), addTextWithVar 내부 자동 바인딩 */
  await addTextWithVar(
    comp, '날짜 선택', FONT_SIZE.xs,
    COLOR_VAR.textLabel, COLOR.textLabel,
    true, SIZE_VAR.fontSizeXs, 'label',
  );

  /* 트리거 버튼 (h-12 = 48px) */
  const trigger = figma.createFrame();
  trigger.name = 'Trigger';
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
    open ? COLOR_VAR.borderFocus : COLOR_VAR.border,
    open ? COLOR.borderFocus     : COLOR.border,
  );

  /* placeholder 텍스트 — trigger에 먼저 추가하고 comp.appendChild 후 바인딩 */
  const placeholderDefault = mode === 'Single' ? '날짜를 선택하세요' : '시작일 ~ 종료일';
  const placeholderText = await addTextWithVar(
    trigger, placeholderDefault, FONT_SIZE.sm,
    COLOR_VAR.textPlaceholder, COLOR.textPlaceholder,
    false, SIZE_VAR.fontSizeSm,
  );
  placeholderText.layoutGrow = 1;
  trigger.appendChild(createIcon('Calendar', 16, COLOR.textMuted));

  comp.appendChild(trigger);

  /* placeholder TEXT property 바인딩 — comp.appendChild(trigger) 완료 후 (2단계 ✓) */
  const placeholderKey = comp.addComponentProperty('placeholder', 'TEXT', placeholderDefault);
  placeholderText.componentPropertyReferences = { characters: placeholderKey };

  /* Open=True: 달력 패널 표시 */
  if (open) {
    comp.appendChild(await createCalendarPanel(mode));
  }

  return comp;
}

export async function createDatePicker(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (const mode of ['Single', 'Range'] as DatePickerMode[]) {
    for (const open of [false, true]) {
      for (const disabled of [false, true]) {
        components.push(await createDatePickerVariant(mode, open, disabled));
      }
    }
  }
  /* cols=4: 한 행 = Mode 동일 × Open(2) × Disabled(2) */
  return combineVariants(components, 'DatePicker', 4);
}
