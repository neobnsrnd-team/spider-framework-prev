/**
 * @file createInfoRow.ts
 * @description Figma InfoRow / LabelValueRow 컴포넌트 세트 생성.
 * 두 컴포넌트 모두 레이블-값 수평 배치 패턴이나 역할이 다르므로 각각 생성한다.
 *
 * InfoRow TEXT properties:
 *   - label — 좌측 레이블 (기본값: '레이블')
 *   - value — 우측 값 (기본값: '값')
 *
 * LabelValueRow TEXT properties:
 *   - label — 좌측 레이블 (기본값: '레이블')
 *   - value — 우측 값 (기본값: '값')
 *
 * [레이아웃]
 *   InfoRow:       HORIZONTAL SPACE_BETWEEN, FIXED 390×44, ShowBorder(True|False)
 *   LabelValueRow: HORIZONTAL SPACE_BETWEEN, FIXED 390×36, 단일 컴포넌트
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, addTextWithVar, setStroke,
} from '../../../utils/helpers';

/* ── InfoRow ──────────────────────────────────────────────── */
async function createInfoRowVariant(showBorder: boolean): Promise<ComponentNode> {
  const comp = createComponent(`ShowBorder=${showBorder ? 'True' : 'False'}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.sm, SPACING.standard);
  comp.resize(390, 44);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  clearFill(comp);
  if (showBorder) setStroke(comp, COLOR.borderSubtle);

  /* label — comp 직접 자식, 자동 바인딩 */
  await addTextWithVar(
    comp, '레이블', FONT_SIZE.sm,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, SIZE_VAR.fontSizeSm, 'label',
  );

  /* value */
  await addTextWithVar(
    comp, '값', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'value',
  );

  return comp;
}

export async function createInfoRow(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createInfoRowVariant(false), createInfoRowVariant(true)]),
    'InfoRow',
    2,
  );
}

/* ── LabelValueRow ────────────────────────────────────────── */
async function createLabelValueRowNode(): Promise<ComponentNode> {
  const comp = createComponent('LabelValueRow');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.xs, SPACING.standard);
  comp.resize(390, 36);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  clearFill(comp);

  /* label — xs/muted */
  await addTextWithVar(
    comp, '레이블', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs, 'label',
  );

  /* value — sm/bold/heading */
  await addTextWithVar(
    comp, '값', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'value',
  );

  return comp;
}

export async function createLabelValueRow(): Promise<ComponentNode> {
  const comp = await createLabelValueRowNode();
  figma.currentPage.appendChild(comp);
  return comp;
}
