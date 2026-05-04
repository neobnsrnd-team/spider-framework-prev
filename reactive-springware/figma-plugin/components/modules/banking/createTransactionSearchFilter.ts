/**
 * @file createTransactionSearchFilter.ts
 * @description Figma TransactionSearchFilter 컴포넌트 세트 생성.
 * 거래내역 조회 조건 필터 아코디언 컴포넌트.
 *
 * State(Collapsed|Expanded) = 2 variants.
 * - Collapsed: 헤더(기간 요약 + ChevronDown)만 표시
 * - Expanded:  헤더 + 퀵기간 탭 4개 + DatePicker 쌍 + Select 쌍 + 조회 버튼
 *
 * 컴포넌트 이름: "TransactionSearchFilter"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addRect,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const FILTER_WIDTH = 390;
const CONTENT_PX = SPACING.standard; /* px-standard */

/** 헤더 행 생성: 기간 요약 또는 "조회 조건 설정" + ChevronDown */
async function createFilterHeader(expanded: boolean): Promise<FrameNode> {
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0);
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(header, 21, CONTENT_PX);
  header.resize(FILTER_WIDTH, 1);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  clearFill(header);

  const labelText = expanded ? '조회 조건 설정' : '2026.03.28 ~ 2026.04.28';
  const labelBold = expanded;
  const labelColorVar = expanded ? COLOR_VAR.textHeading : COLOR_VAR.textMuted;
  const labelFallback = expanded ? COLOR.textHeading : COLOR.textMuted;

  const label = await addTextWithVar(header, labelText, FONT_SIZE.sm, labelColorVar, labelFallback, labelBold, SIZE_VAR.fontSizeSm);
  label.layoutGrow = 1;

  header.appendChild(createIcon('ChevronDown', 10, COLOR.textMuted));

  return header;
}

/** 퀵 기간 탭 컨테이너 생성 (4 탭: 1개월|3개월|6개월|12개월) */
async function createQuickPeriodTabs(): Promise<FrameNode> {
  const container = figma.createFrame();
  setAutoLayout(container, 'HORIZONTAL', 0);
  container.resize(FILTER_WIDTH - CONTENT_PX * 2, 40);
  container.primaryAxisSizingMode = 'FIXED';
  container.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(container, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(container, COLOR_VAR.border, COLOR.border);
  await setFloatVar(container, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  container.paddingTop    = 5;
  container.paddingBottom = 5;
  container.paddingLeft   = 5;
  container.paddingRight  = 5;
  container.primaryAxisAlignItems = 'SPACE_BETWEEN';

  const periods = ['1개월', '3개월', '6개월', '12개월'];
  for (let i = 0; i < periods.length; i++) {
    const tab = figma.createFrame();
    setAutoLayout(tab, 'HORIZONTAL', 0);
    tab.layoutGrow = 1;
    tab.counterAxisSizingMode = 'FIXED';
    tab.resize(1, 30); /* flex-1, 높이는 컨테이너 패딩 반영 */

    /* 첫 탭만 활성(선택) 표시 */
    const isActive = i === 0;
    if (isActive) {
      await setFillWithVar(tab, COLOR_VAR.brandBg, BRAND.bg);
      await setFloatVar(tab, 'cornerRadius', SIZE_VAR.radiusMd, RADIUS.md);
    } else {
      clearFill(tab);
      tab.cornerRadius = 0;
    }

    const text = await addTextWithVar(
      tab, periods[i], FONT_SIZE.xs,
      isActive ? COLOR_VAR.brandText : COLOR_VAR.textMuted,
      isActive ? BRAND.text         : COLOR.textMuted,
      false, SIZE_VAR.fontSizeXs,
    );
    text.layoutAlign = 'STRETCH';
    text.textAlignHorizontal = 'CENTER';

    container.appendChild(tab);
  }

  return container;
}

/** DatePicker 트리거 버튼 (단일 날짜 입력용) */
async function createDateTrigger(placeholder: string): Promise<FrameNode> {
  const trigger = figma.createFrame();
  setAutoLayout(trigger, 'HORIZONTAL', 0);
  trigger.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(trigger, 0, SPACING.standard);
  trigger.resize(1, 44); /* flex-1, h-11 */
  trigger.layoutGrow = 1;
  trigger.primaryAxisSizingMode = 'FIXED';
  trigger.counterAxisSizingMode = 'FIXED';
  await setFloatVar(trigger, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  await setFillWithVar(trigger, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(trigger, COLOR_VAR.border, COLOR.border);

  const text = await addTextWithVar(
    trigger, placeholder, FONT_SIZE.sm,
    COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, SIZE_VAR.fontSizeSm,
  );
  text.layoutGrow = 1;
  trigger.appendChild(createIcon('Calendar', 16, COLOR.textMuted));

  return trigger;
}

/** Select 드롭다운 (단일 항목용) */
async function createSelectBox(label: string): Promise<FrameNode> {
  const select = figma.createFrame();
  setAutoLayout(select, 'HORIZONTAL', 0);
  select.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(select, 0, SPACING.standard);
  select.resize(1, 44);
  select.layoutGrow = 1;
  select.primaryAxisSizingMode = 'FIXED';
  select.counterAxisSizingMode = 'FIXED';
  await setFloatVar(select, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  await setFillWithVar(select, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(select, COLOR_VAR.border, COLOR.border);

  const text = await addTextWithVar(
    select, label, FONT_SIZE.sm,
    COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm,
  );
  text.layoutGrow = 1;
  select.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));

  return select;
}

/** 조회 버튼 */
async function createSearchButton(): Promise<FrameNode> {
  const btn = figma.createFrame();
  setAutoLayout(btn, 'HORIZONTAL', 0);
  setPadding(btn, 10, SPACING.standard);
  btn.primaryAxisSizingMode = 'AUTO';
  btn.counterAxisSizingMode = 'FIXED';
  btn.resize(1, 44);
  await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(btn, COLOR_VAR.brandPrimary, BRAND.primary);

  await addTextWithVar(btn, '조회', FONT_SIZE.xs, COLOR_VAR.brandFg, BRAND.fg, false, SIZE_VAR.fontSizeXs);
  return btn;
}

async function createFilterVariant(state: 'Collapsed' | 'Expanded'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(FILTER_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
  comp.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  /* 상하 구분선만 표시 */
  comp.strokeTopWeight    = 1;
  comp.strokeBottomWeight = 1;
  comp.strokeLeftWeight   = 0;
  comp.strokeRightWeight  = 0;

  comp.appendChild(await createFilterHeader(state === 'Expanded'));

  if (state === 'Expanded') {
    /* 펼침 영역 */
    const body = figma.createFrame();
    setAutoLayout(body, 'VERTICAL', SPACING.md, 'MIN');
    setPadding(body, 0, CONTENT_PX, 21, CONTENT_PX);
    body.resize(FILTER_WIDTH, 1);
    body.primaryAxisSizingMode = 'AUTO';   /* VERTICAL: height가 콘텐츠에 맞게 늘어남 */
    body.counterAxisSizingMode = 'FIXED';  /* VERTICAL: width 고정 */
    clearFill(body);

    /* 퀵 기간 탭 */
    body.appendChild(await createQuickPeriodTabs());

    /* DatePicker 쌍 */
    const dateRow = figma.createFrame();
    setAutoLayout(dateRow, 'HORIZONTAL', SPACING.sm);
    dateRow.resize(FILTER_WIDTH - CONTENT_PX * 2, 44);
    dateRow.primaryAxisSizingMode = 'FIXED';
    dateRow.counterAxisSizingMode = 'FIXED';
    clearFill(dateRow);
    dateRow.appendChild(await createDateTrigger('시작일'));
    dateRow.appendChild(await createDateTrigger('종료일'));
    body.appendChild(dateRow);

    /* Select 쌍 + 조회 버튼 */
    const controlRow = figma.createFrame();
    setAutoLayout(controlRow, 'HORIZONTAL', SPACING.sm);
    controlRow.resize(FILTER_WIDTH - CONTENT_PX * 2, 44);
    controlRow.primaryAxisSizingMode = 'FIXED';
    controlRow.counterAxisSizingMode = 'FIXED';
    clearFill(controlRow);
    controlRow.appendChild(await createSelectBox('최근순'));
    controlRow.appendChild(await createSelectBox('전체'));
    controlRow.appendChild(await createSearchButton());
    body.appendChild(controlRow);

    comp.appendChild(body);
  }

  return comp;
}

export async function createTransactionSearchFilter(): Promise<ComponentSetNode> {
  const components = [
    await createFilterVariant('Collapsed'),
    await createFilterVariant('Expanded'),
  ];
  return combineVariants(components, 'TransactionSearchFilter', 1);
}
