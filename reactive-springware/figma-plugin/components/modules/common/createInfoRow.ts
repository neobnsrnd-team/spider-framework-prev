/**
 * @file createInfoRow.ts
 * @description Figma InfoRow / LabelValueRow 컴포넌트 세트 생성.
 * 두 컴포넌트 모두 레이블-값 수평 배치 패턴이나 역할이 다르므로 각각 생성한다.
 * - InfoRow: 레이블(secondary) + 값(heading), 선택적 구분선
 * - LabelValueRow: 레이블(xs/muted) + 값(sm/bold)
 */
import { COLOR, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, clearFill, addText, setStroke } from '../../../helpers';

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

  const lbl = await addText(comp, '레이블', FONT_SIZE.sm, COLOR.textSecondary);
  lbl.layoutGrow = 0;
  const val = await addText(comp, '값', FONT_SIZE.sm, COLOR.textHeading, true);
  val.layoutGrow = 0;
  return comp;
}

export async function createInfoRow(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createInfoRowVariant(false), createInfoRowVariant(true)]),
    'InfoRow', 2,
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

  await addText(comp, '레이블', FONT_SIZE.xs, COLOR.textMuted);
  await addText(comp, '값', FONT_SIZE.sm, COLOR.textHeading, true);
  return comp;
}

export async function createLabelValueRow(): Promise<ComponentNode> {
  const comp = await createLabelValueRowNode();
  figma.currentPage.appendChild(comp);
  return comp;
}
