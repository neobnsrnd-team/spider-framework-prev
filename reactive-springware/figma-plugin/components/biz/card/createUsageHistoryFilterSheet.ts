/**
 * @file createUsageHistoryFilterSheet.ts
 * @description Figma UsageHistoryFilterSheet 컴포넌트 세트 생성.
 * 카드 이용내역 검색 필터 BottomSheet.
 * State(Collapsed|Expanded) = 2 variants.
 * - Collapsed: 헤더(현재 필터 요약) + 닫힌 상태
 * - Expanded:  핸들바 + 제목 + 5개 필터 그룹 (칩 선택) + 조회 버튼
 * 컴포넌트 이름: "UsageHistoryFilterSheet"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const SHEET_WIDTH = 390;

/** 필터 섹션 레이블 + 칩 그룹 */
async function createFilterSection(
  parent: FrameNode | ComponentNode,
  sectionLabel: string,
  chips: string[],
  activeIdx = 0,
): Promise<void> {
  const section = figma.createFrame();
  setAutoLayout(section, 'VERTICAL', SPACING.xs, 'MIN');
  section.layoutAlign = 'STRETCH';
  section.primaryAxisSizingMode = 'FIXED';
  section.counterAxisSizingMode = 'AUTO';
  section.resize(SHEET_WIDTH - SPACING.standard * 2, 1);
  clearFill(section);

  await addTextWithVar(section, sectionLabel, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  const chipRow = figma.createFrame();
  setAutoLayout(chipRow, 'HORIZONTAL', SPACING.xs);
  chipRow.primaryAxisSizingMode = 'AUTO';
  chipRow.counterAxisSizingMode = 'AUTO';
  clearFill(chipRow);

  for (let i = 0; i < chips.length; i++) {
    const chip = figma.createFrame();
    setAutoLayout(chip, 'HORIZONTAL', 0);
    setPadding(chip, SPACING.xs, SPACING.md);
    await setFloatVar(chip, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

    const isActive = i === activeIdx;
    if (isActive) {
      await setFillWithVar(chip, COLOR_VAR.brandPrimary, BRAND.primary);
      await setStrokeWithVar(chip, COLOR_VAR.brandPrimary, BRAND.primary);
      await addTextWithVar(chip, chips[i], FONT_SIZE.xs, COLOR_VAR.brandFg, BRAND.fg, false, SIZE_VAR.fontSizeXs);
    } else {
      await setFillWithVar(chip, COLOR_VAR.surface, COLOR.surface);
      await setStrokeWithVar(chip, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
      await addTextWithVar(chip, chips[i], FONT_SIZE.xs, COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeXs);
    }
    chipRow.appendChild(chip);
  }
  section.appendChild(chipRow);
  parent.appendChild(section);
}

async function createFilterVariant(state: 'Collapsed' | 'Expanded'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SHEET_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setFloatVar(comp, 'topLeftRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (state === 'Collapsed') {
    /* 닫힌 상태: 현재 필터 요약 헤더 */
    const header = figma.createFrame();
    setAutoLayout(header, 'HORIZONTAL', 0);
    header.primaryAxisAlignItems = 'SPACE_BETWEEN';
    header.counterAxisAlignItems = 'CENTER';
    setPadding(header, SPACING.standard, SPACING.standard);
    header.layoutAlign = 'STRETCH';
    header.primaryAxisSizingMode = 'FIXED';
    header.counterAxisSizingMode = 'AUTO';
    header.resize(SHEET_WIDTH, 1);
    clearFill(header);
    await addTextWithVar(header, '승인 · 전체 · 이번달', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeSm);
    header.appendChild(createIcon('SlidersHorizontal', 20, COLOR.textMuted));
    comp.appendChild(header);
  } else {
    /* 핸들바 */
    const handle = figma.createFrame();
    setAutoLayout(handle, 'HORIZONTAL', 0);
    handle.resize(SHEET_WIDTH, 28);
    clearFill(handle);
    const bar = figma.createFrame();
    bar.resize(40, 4);
    bar.cornerRadius = RADIUS.full;
    bar.fills = [{ type: 'SOLID', color: COLOR.surfaceRaised }];
    handle.appendChild(bar);
    comp.appendChild(handle);

    /* 제목 + 닫기 */
    const titleRow = figma.createFrame();
    setAutoLayout(titleRow, 'HORIZONTAL', 0);
    titleRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
    titleRow.counterAxisAlignItems = 'CENTER';
    setPadding(titleRow, SPACING.standard, SPACING.standard);
    titleRow.layoutAlign = 'STRETCH';
    titleRow.primaryAxisSizingMode = 'FIXED';
    titleRow.counterAxisSizingMode = 'AUTO';
    titleRow.resize(SHEET_WIDTH, 1);
    clearFill(titleRow);
    await addTextWithVar(titleRow, '검색 조건', FONT_SIZE.lg, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeLg);
    titleRow.appendChild(createIcon('X', 20, COLOR.textMuted));
    comp.appendChild(titleRow);

    /* 필터 바디 */
    const body = figma.createFrame();
    setAutoLayout(body, 'VERTICAL', SPACING.lg, 'MIN');
    setPadding(body, 0, SPACING.standard, SPACING['2xl'], SPACING.standard);
    body.layoutAlign = 'STRETCH';
    body.primaryAxisSizingMode = 'FIXED';
    body.counterAxisSizingMode = 'AUTO';
    body.resize(SHEET_WIDTH, 1);
    clearFill(body);

    await createFilterSection(body, '승인구분', ['승인', '취소', '전체'], 0);
    await createFilterSection(body, '카드구분', ['신용', '체크', '전체'], 2);
    await createFilterSection(body, '이용구분', ['국내', '해외', '전체'], 2);
    await createFilterSection(body, '조회기간', ['이번달', '1개월', '3개월', '직접입력'], 0);

    /* 조회 버튼 */
    const submitBtn = figma.createFrame();
    setAutoLayout(submitBtn, 'HORIZONTAL', 0);
    submitBtn.resize(SHEET_WIDTH - SPACING.standard * 2, 52);
    submitBtn.primaryAxisSizingMode = 'FIXED';
    submitBtn.counterAxisSizingMode = 'FIXED';
    await setFloatVar(submitBtn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
    await setFillWithVar(submitBtn, COLOR_VAR.brandPrimary, BRAND.primary);
    const submitText = await addTextWithVar(submitBtn, '조회', FONT_SIZE.base, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeBase);
    submitText.layoutAlign = 'STRETCH';
    submitText.textAlignHorizontal = 'CENTER';
    body.appendChild(submitBtn);

    comp.appendChild(body);
  }

  return comp;
}

export async function createUsageHistoryFilterSheet(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createFilterVariant('Collapsed'), await createFilterVariant('Expanded')],
    'UsageHistoryFilterSheet', 1,
  );
}
