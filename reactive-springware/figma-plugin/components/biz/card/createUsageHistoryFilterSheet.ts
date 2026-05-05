/**
 * @file createUsageHistoryFilterSheet.ts
 * @description Figma UsageHistoryFilterSheet 컴포넌트 생성.
 * 카드 이용내역 검색 필터 BottomSheet — 열린 상태 단일 variant.
 *
 * [레이아웃]
 *   comp (VERTICAL, gap=0, FIXED 390, AUTO height, 상단 radiusXl, surface)
 *     handle   (HORIZONTAL CENTER, FIXED 390×28 — 핸들바)
 *     titleRow (HORIZONTAL, FIXED 390 — 좌측 스페이서 + '검색' 중앙 + X 우측)
 *     body     (VERTICAL gap=lg, FIXED 390, AUTO height, px-standard pb-2xl)
 *       승인구분 FilterSection
 *       카드구분 FilterSection
 *       이용구분 FilterSection
 *       조회기간 FilterSection
 *       btnRow  (HORIZONTAL gap=sm, CONTENT_WIDTH)
 *         '초기화' (outline, layoutGrow=1, h=52)
 *         '조회'   (brandPrimary, layoutGrow=1, h=52)
 *
 * 컴포넌트 이름: "UsageHistoryFilterSheet"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const SHEET_WIDTH   = 390;
const CONTENT_WIDTH = SHEET_WIDTH - SPACING.standard * 2;

/** 필터 섹션 레이블 + 칩 그룹 */
async function createFilterSection(
  parent: FrameNode | ComponentNode,
  sectionLabel: string,
  chips: string[],
  activeIdx = 0,
): Promise<void> {
  const section = figma.createFrame();
  setAutoLayout(section, 'VERTICAL', SPACING.xs, 'MIN');
  section.primaryAxisAlignItems = 'MIN';
  section.resize(CONTENT_WIDTH, 1);     /* resize 먼저 */
  section.primaryAxisSizingMode = 'AUTO';
  section.counterAxisSizingMode = 'FIXED';
  clearFill(section);
  parent.appendChild(section);

  await addTextWithVar(section, sectionLabel, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  const chipRow = figma.createFrame();
  setAutoLayout(chipRow, 'HORIZONTAL', SPACING.xs);
  chipRow.primaryAxisAlignItems = 'MIN';
  chipRow.counterAxisAlignItems = 'CENTER';
  chipRow.layoutWrap = 'WRAP';
  chipRow.counterAxisSpacing = SPACING.xs;
  chipRow.resize(CONTENT_WIDTH, 1);     /* resize 먼저 */
  chipRow.primaryAxisSizingMode = 'FIXED';
  chipRow.counterAxisSizingMode = 'AUTO';
  clearFill(chipRow);
  section.appendChild(chipRow);

  for (let i = 0; i < chips.length; i++) {
    const chip = figma.createFrame();
    setAutoLayout(chip, 'HORIZONTAL', 0);
    chip.primaryAxisAlignItems = 'CENTER';
    chip.counterAxisAlignItems = 'CENTER';
    setPadding(chip, SPACING.xs, SPACING.md);
    chip.resize(1, 1);                  /* resize 먼저 */
    chip.primaryAxisSizingMode = 'AUTO';
    chip.counterAxisSizingMode = 'AUTO';
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
}

export async function createUsageHistoryFilterSheet(): Promise<ComponentNode> {
  const comp = createComponent('UsageHistoryFilterSheet');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(SHEET_WIDTH, 1);          /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setFloatVar(comp, 'topLeftRadius',  SIZE_VAR.radiusXl, RADIUS.xl);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  /* ── 핸들바 ── */
  const handle = figma.createFrame();
  setAutoLayout(handle, 'HORIZONTAL', 0);
  handle.primaryAxisAlignItems = 'CENTER';
  handle.counterAxisAlignItems = 'CENTER';
  handle.resize(SHEET_WIDTH, 28);
  handle.primaryAxisSizingMode = 'FIXED';
  handle.counterAxisSizingMode = 'FIXED';
  clearFill(handle);
  const bar = figma.createFrame();
  bar.resize(40, 4);
  bar.cornerRadius = RADIUS.full;
  bar.fills = [{ type: 'SOLID', color: COLOR.surfaceRaised }];
  handle.appendChild(bar);
  comp.appendChild(handle);

  /* ── 제목 행: 좌측 스페이서 + '검색' 중앙정렬 + X 아이콘 ── */
  const titleRow = figma.createFrame();
  setAutoLayout(titleRow, 'HORIZONTAL', 0);
  titleRow.primaryAxisAlignItems = 'MIN';
  titleRow.counterAxisAlignItems = 'CENTER';
  setPadding(titleRow, SPACING.standard, SPACING.standard);
  titleRow.resize(SHEET_WIDTH, 1);      /* resize 먼저 */
  titleRow.primaryAxisSizingMode = 'FIXED';
  titleRow.counterAxisSizingMode = 'AUTO';
  clearFill(titleRow);
  comp.appendChild(titleRow);

  /* 좌측 스페이서 — X 아이콘(20px)과 폭을 맞춰 제목이 시각적 중앙에 오도록 */
  const spacer = figma.createFrame();
  spacer.resize(20, 20);
  spacer.primaryAxisSizingMode = 'FIXED';
  spacer.counterAxisSizingMode = 'FIXED';
  clearFill(spacer);
  titleRow.appendChild(spacer);

  /* '검색' — layoutGrow=1 + textAlignHorizontal='CENTER' 로 중앙 배치 */
  const titleNode = await addTextWithVar(titleRow, '검색', FONT_SIZE.lg, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeLg);
  titleNode.layoutGrow = 1;
  titleNode.textAlignHorizontal = 'CENTER';

  /* X 닫기 아이콘 */
  titleRow.appendChild(createIcon('X', 20, COLOR.textMuted));

  /* ── 필터 바디 ── */
  const body = figma.createFrame();
  setAutoLayout(body, 'VERTICAL', SPACING.lg, 'MIN');
  body.primaryAxisAlignItems = 'MIN';
  setPadding(body, 0, SPACING.standard, SPACING['2xl'], SPACING.standard);
  body.resize(SHEET_WIDTH, 1);          /* resize 먼저 */
  body.primaryAxisSizingMode = 'AUTO';
  body.counterAxisSizingMode = 'FIXED';
  clearFill(body);
  comp.appendChild(body);

  await createFilterSection(body, '승인구분', ['승인', '취소', '전체'], 0);
  await createFilterSection(body, '카드구분', ['신용', '체크', '전체'], 2);
  await createFilterSection(body, '이용구분', ['국내', '해외', '전체'], 2);
  await createFilterSection(body, '조회기간', ['이번달', '1개월', '3개월', '직접입력'], 0);

  /* ── 하단 버튼 행: 초기화 + 조회 ── */
  const btnRow = figma.createFrame();
  setAutoLayout(btnRow, 'HORIZONTAL', SPACING.sm);
  btnRow.primaryAxisAlignItems = 'MIN';
  btnRow.counterAxisAlignItems = 'CENTER';
  btnRow.resize(CONTENT_WIDTH, 1);      /* resize 먼저 */
  btnRow.primaryAxisSizingMode = 'FIXED';
  btnRow.counterAxisSizingMode = 'AUTO';
  clearFill(btnRow);
  body.appendChild(btnRow);

  /* '초기화' — outline 스타일, 콘텐츠+패딩 크기 (조회 버튼보다 좁게) */
  const resetBtn = figma.createFrame();
  setAutoLayout(resetBtn, 'HORIZONTAL', 0);
  resetBtn.primaryAxisAlignItems = 'CENTER';
  resetBtn.counterAxisAlignItems = 'CENTER';
  setPadding(resetBtn, 0, SPACING.xl);   /* 수평 padding으로 너비 결정 */
  resetBtn.resize(1, 52);               /* resize 먼저 */
  resetBtn.primaryAxisSizingMode = 'AUTO';  /* 콘텐츠+padding에 맞게 */
  resetBtn.counterAxisSizingMode = 'FIXED'; /* height 52 고정 */
  await setFloatVar(resetBtn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  clearFill(resetBtn);
  await setStrokeWithVar(resetBtn, COLOR_VAR.border, COLOR.border);
  resetBtn.strokeWeight = 1;
  resetBtn.strokeAlign  = 'INSIDE';
  btnRow.appendChild(resetBtn);
  const resetText = await addTextWithVar(resetBtn, '초기화', FONT_SIZE.base, COLOR_VAR.textLabel, COLOR.textLabel, false, SIZE_VAR.fontSizeBase);
  resetText.textAlignHorizontal = 'CENTER';

  /* '조회' — brandPrimary 채움 스타일, 남은 공간 전체 차지 */
  const submitBtn = figma.createFrame();
  setAutoLayout(submitBtn, 'HORIZONTAL', 0);
  submitBtn.primaryAxisAlignItems = 'CENTER';
  submitBtn.counterAxisAlignItems = 'CENTER';
  submitBtn.layoutGrow = 1;             /* 초기화 버튼이 빠진 나머지 너비 차지 */
  submitBtn.resize(1, 52);              /* resize 먼저 */
  submitBtn.primaryAxisSizingMode = 'FIXED';
  submitBtn.counterAxisSizingMode = 'FIXED';
  await setFloatVar(submitBtn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(submitBtn, COLOR_VAR.brandPrimary, BRAND.primary);
  btnRow.appendChild(submitBtn);
  const submitText = await addTextWithVar(submitBtn, '조회', FONT_SIZE.base, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeBase);
  submitText.textAlignHorizontal = 'CENTER';

  figma.currentPage.appendChild(comp);
  return comp;
}
