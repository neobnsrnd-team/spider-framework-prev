/**
 * @file createCollapsibleSection.ts
 * @description Figma CollapsibleSection 컴포넌트 세트 생성.
 * 항상 열린(expanded) 상태만 표현한다. HeaderAlign(Left|Center) = 2 variants.
 *
 * TEXT properties:
 *   - header — 섹션 제목 (기본값: '섹션 제목')
 *
 * [레이아웃 구조]
 *   HeaderRow: [title(grow, textAlign=Left|Center)] [ChevronUp]
 *   Divider   (1px, border-subtle)
 *   Content   (Slot, FILL, FIXED 80px — 인스턴스에서 자유롭게 컴포넌트 배치 가능)
 *
 * [TEXT property 바인딩 타이밍]
 *   comp.appendChild(headerRow) 이후에 header TEXT 프로퍼티를 바인딩한다.
 *
 * 컴포넌트 이름: "CollapsibleSection"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, clearFill, setFill, addTextWithVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const COMP_W      = 390;
const HEADER_H    = 40;
const SLOT_H      = 80;
const DIVIDER_H   = 1;
const COMP_GAP    = SPACING.xs; /* header ↔ divider ↔ slot 간격 */
const COMP_H      = SPACING.md * 2 + HEADER_H + COMP_GAP + DIVIDER_H + COMP_GAP + SLOT_H; /* 160px */

async function createCollapsibleVariant(
  headerAlign: 'Left' | 'Center',
): Promise<ComponentNode> {
  const comp = createComponent(`HeaderAlign=${headerAlign}`);
  setAutoLayout(comp, 'VERTICAL', COMP_GAP);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(COMP_W, COMP_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.sm;
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* ── Header Row ──────────────────────────────────────────── */
  const headerRow = figma.createFrame();
  headerRow.name = 'HeaderRow';
  setAutoLayout(headerRow, 'HORIZONTAL', SPACING.sm);
  headerRow.resize(COMP_W - SPACING.md * 2, HEADER_H);
  headerRow.primaryAxisSizingMode = 'FIXED';
  headerRow.counterAxisSizingMode = 'FIXED';
  clearFill(headerRow);
  comp.appendChild(headerRow);
  headerRow.layoutSizingHorizontal = 'FILL';

  /* title TEXT property (comp → headerRow → title, 2단계 ✓) */
  const titleText = await addTextWithVar(
    headerRow, '섹션 제목', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );
  titleText.layoutGrow = 1;
  titleText.textAlignHorizontal = headerAlign === 'Center' ? 'CENTER' : 'LEFT';
  const headerKey = comp.addComponentProperty('header', 'TEXT', '섹션 제목');
  titleText.componentPropertyReferences = { characters: headerKey };

  /* ChevronUp — 항상 열린 상태이므로 고정 */
  headerRow.appendChild(createIcon('ChevronUp', 16, COLOR.textMuted));

  /* ── Divider ──────────────────────────────────────────────── */
  const divider = figma.createRectangle();
  divider.name = 'Divider';
  divider.resize(COMP_W - SPACING.md * 2, DIVIDER_H);
  setFill(divider, COLOR.borderSubtle);
  comp.appendChild(divider);
  divider.layoutSizingHorizontal = 'FILL';

  /* ── Content Slot ─────────────────────────────────────────── */
  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.resize(COMP_W - SPACING.md * 2, SLOT_H);
  clearFill(slot);
  slot.layoutSizingHorizontal = 'FILL';

  return comp;
}

export async function createCollapsibleSection(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createCollapsibleVariant('Left'),
    createCollapsibleVariant('Center'),
  ]);
  /* cols=2: HeaderAlign 2종 나란히 배치 */
  return combineVariants(components, 'CollapsibleSection', 2);
}
