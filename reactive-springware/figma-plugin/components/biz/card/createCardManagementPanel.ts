/**
 * @file createCardManagementPanel.ts
 * @description Figma CardManagementPanel 컴포넌트 세트 생성.
 * "카드 관리" 고정 헤더 + 관리 항목 목록 패널.
 * 2개 컴포넌트로 구성: CardManagementPanel/Item → CardManagementPanel.
 *
 * ── CardManagementPanel/Item ─────────────────────────────────
 * TEXT properties:
 *   - label   — 항목 레이블 (기본값: '카드 항목')
 *   - subText — 부가 설명  (기본값: '1234 **** **** 5678')
 *
 * ── CardManagementPanel ──────────────────────────────────────
 * SLOT:
 *   - Rows (CardManagementPanel/Item 인스턴스를 추가할 수 있는 슬롯)
 *
 * [레이아웃]
 *   CardManagementPanel/Item (HORIZONTAL SPACE_BETWEEN CENTER, p-md, FIXED 390)
 *     label (TEXT sm, textHeading, 바인딩)
 *     right (HORIZONTAL gap=xs CENTER)
 *       subText (TEXT xs, textMuted, 바인딩)
 *       ChevronRight (16px, textMuted)
 *
 *   CardManagementPanel (VERTICAL gap=xs, MIN, FIXED 390)
 *     header (HORIZONTAL, p-[md standard], FIXED 390)
 *       "카드 관리" (TEXT base bold, textHeading, 고정)
 *     Rows SlotNode (VERTICAL gap=xs, FIXED 390, Item×2)
 *
 * 컴포넌트 이름: "CardManagementPanel/Item", "CardManagementPanel"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const PANEL_WIDTH = 390;

/* ── CardManagementPanel/Item ───────────────────────────────── */

export async function createCardManagementPanelItem(): Promise<ComponentNode> {
  const comp = createComponent('CardManagementPanel/Item');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(PANEL_WIDTH, 1);           /* resize 먼저 호출 */
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';   /* height 텍스트 높이에 맞게 */
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign = 'INSIDE';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

  /* label — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '카드 항목', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeSm, 'label');

  /* right 영역: subText + ChevronRight */
  const right = figma.createFrame();
  setAutoLayout(right, 'HORIZONTAL', SPACING.xs);
  right.primaryAxisAlignItems = 'MIN';
  right.counterAxisAlignItems = 'CENTER';
  right.primaryAxisSizingMode = 'AUTO';
  right.counterAxisSizingMode = 'AUTO';
  clearFill(right);
  comp.appendChild(right);

  /* subText — right의 자식이라 수동 바인딩 */
  await addTextWithVar(right, '1234 **** **** 5678', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'subText', comp);
  right.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── CardManagementPanel ─────────────────────────────────────── */

/**
 * @param item - createCardManagementPanelItem()이 반환한 ComponentNode.
 *               Rows 슬롯의 기본 인스턴스 배치에 사용한다.
 */
export async function createCardManagementPanel(item: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('CardManagementPanel');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(PANEL_WIDTH, 1);           /* resize 먼저 호출 */
  comp.primaryAxisSizingMode = 'AUTO';   /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED';  /* width 고정 */
  clearFill(comp);

  /* "카드 관리" 헤더 — 고정 텍스트 (TEXT property 없음) */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0);
  header.primaryAxisAlignItems = 'MIN';
  header.counterAxisAlignItems = 'CENTER';
  setPadding(header, SPACING.md, SPACING.standard);
  header.resize(PANEL_WIDTH, 1);         /* resize 먼저 호출 — AUTO 전에 반드시 */
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO'; /* height 텍스트 높이에 맞게 */
  clearFill(header);
  comp.appendChild(header);
  await addTextWithVar(header, '카드 관리', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase);

  /* Rows 슬롯 — CardManagementPanel/Item 인스턴스를 추가할 수 있는 영역 */
  const rowsSlot = comp.createSlot();
  rowsSlot.name = 'Rows';
  rowsSlot.layoutMode = 'VERTICAL';
  rowsSlot.itemSpacing = SPACING.xs;
  rowsSlot.resize(PANEL_WIDTH, 1);       /* resize 먼저 호출 */
  rowsSlot.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  rowsSlot.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  clearFill(rowsSlot);

  /* 기본 Item 인스턴스 2개 배치 */
  rowsSlot.appendChild(item.createInstance());
  rowsSlot.appendChild(item.createInstance());

  figma.currentPage.appendChild(comp);
  return comp;
}
