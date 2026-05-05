/**
 * @file createCardInfoPanel.ts
 * @description Figma CardInfoPanel 컴포넌트 세트 생성.
 * 섹션 제목 + 레이블-값 행 목록을 렌더링하는 정보 패널.
 * 3개 컴포넌트로 구성: CardInfoRow → CardInfoSection → CardInfoPanel.
 *
 * ── CardInfoPanel/CardInfoRow ───────────────────────────────────
 * TEXT properties:
 *   - label — 레이블 (기본값: '레이블')
 *   - value — 값    (기본값: '값')
 *
 * ── CardInfoPanel/CardInfoSection ──────────────────────────────
 * TEXT properties:
 *   - title — 섹션 제목 (기본값: '섹션 제목')
 * SLOT:
 *   - Rows (CardInfoRow 인스턴스를 추가할 수 있는 슬롯)
 *
 * ── CardInfoPanel ──────────────────────────────────────────────
 * SLOT:
 *   - Sections (CardInfoSection 인스턴스를 추가할 수 있는 슬롯)
 *
 * [레이아웃]
 *   CardInfoRow (HORIZONTAL SPACE_BETWEEN, MIN, py-xs, FIXED 358)
 *     label (TEXT xs, textMuted, 바인딩)
 *     value (TEXT xs, textHeading, RIGHT, 바인딩)
 *
 *   CardInfoSection (VERTICAL gap=xs, MIN, FIXED 358)
 *     title (TEXT sm bold, textHeading, 바인딩)
 *     Rows SlotNode (VERTICAL, FIXED 358, CardInfoRow×2)
 *
 *   CardInfoPanel (VERTICAL gap=0, p-standard, FIXED 390)
 *     Sections SlotNode (VERTICAL itemSpacing=md, FIXED 358, CardInfoSection×2)
 *
 * 컴포넌트 이름: "CardInfoPanel/CardInfoRow", "CardInfoPanel/CardInfoSection", "CardInfoPanel"
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  addTextWithVar,
} from '../../../utils/helpers';

const PANEL_WIDTH    = 390;
const CONTENT_WIDTH  = PANEL_WIDTH - SPACING.standard * 2; // 358

/* ── CardInfoPanel/CardInfoRow ─────────────────────────────────── */

export async function createCardInfoPanelCardInfoRow(): Promise<ComponentNode> {
  const comp = createComponent('CardInfoPanel/CardInfoRow');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.primaryAxisAlignItems  = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems  = 'MIN'; /* items-start: 다행 텍스트 위쪽 정렬 */
  setPadding(comp, SPACING.xs, 0);    /* py-xs, 좌우 padding 없음 */
  comp.resize(CONTENT_WIDTH, 1);
  comp.primaryAxisSizingMode  = 'FIXED';
  comp.counterAxisSizingMode  = 'AUTO';
  clearFill(comp);

  /* label — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '레이블', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'label');

  /* value — comp 직접 자식: 자동 바인딩, 우측 텍스트 정렬 */
  const valueNode = await addTextWithVar(comp, '값', FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeXs, 'value');
  valueNode.textAlignHorizontal = 'RIGHT';

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── CardInfoPanel/CardInfoSection ─────────────────────────────── */

/**
 * @param row - createCardInfoPanelCardInfoRow()가 반환한 ComponentNode.
 *              Rows 슬롯의 기본 인스턴스 배치에 사용한다.
 */
export async function createCardInfoPanelCardInfoSection(row: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('CardInfoPanel/CardInfoSection');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(CONTENT_WIDTH, 1);
  comp.primaryAxisSizingMode  = 'AUTO';
  comp.counterAxisSizingMode  = 'FIXED';
  clearFill(comp);

  /* title — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '섹션 제목', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'title');

  /* Rows 슬롯 — CardInfoRow 인스턴스를 추가할 수 있는 영역 */
  const rowsSlot = comp.createSlot();
  rowsSlot.name = 'Rows';
  rowsSlot.layoutMode = 'VERTICAL';
  rowsSlot.itemSpacing = 0;
  rowsSlot.resize(CONTENT_WIDTH, 1); /* resize 먼저 호출 후 AUTO 설정 */
  rowsSlot.primaryAxisSizingMode  = 'AUTO';  /* height 콘텐츠에 맞게 */
  rowsSlot.counterAxisSizingMode  = 'FIXED'; /* width 고정 */
  clearFill(rowsSlot);

  /* 기본 CardInfoRow 인스턴스 2개 배치 */
  rowsSlot.appendChild(row.createInstance());
  rowsSlot.appendChild(row.createInstance());

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── CardInfoPanel ──────────────────────────────────────────────── */

/**
 * @param section - createCardInfoPanelCardInfoSection()이 반환한 ComponentNode.
 *                  Sections 슬롯의 기본 인스턴스 배치에 사용한다.
 */
export async function createCardInfoPanel(section: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('CardInfoPanel');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(PANEL_WIDTH, 1);
  comp.primaryAxisSizingMode  = 'AUTO';
  comp.counterAxisSizingMode  = 'FIXED';
  clearFill(comp);

  /* Sections 슬롯 — CardInfoSection 인스턴스를 추가할 수 있는 영역 */
  const sectionsSlot = comp.createSlot();
  sectionsSlot.name = 'Sections';
  sectionsSlot.layoutMode = 'VERTICAL';
  sectionsSlot.itemSpacing = SPACING.md; /* 섹션 간 gap=md */
  sectionsSlot.resize(CONTENT_WIDTH, 1); /* resize 먼저 호출 후 AUTO 설정 */
  sectionsSlot.primaryAxisSizingMode  = 'AUTO';  /* height 콘텐츠에 맞게 */
  sectionsSlot.counterAxisSizingMode  = 'FIXED'; /* width 고정 */
  clearFill(sectionsSlot);

  /* 기본 CardInfoSection 인스턴스 2개 배치 */
  sectionsSlot.appendChild(section.createInstance());
  sectionsSlot.appendChild(section.createInstance());

  figma.currentPage.appendChild(comp);
  return comp;
}
