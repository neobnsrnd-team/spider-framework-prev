/**
 * @file createAmountInput.ts
 * @description Figma AmountInput 컴포넌트 세트 생성.
 * State(Default|Error) × 2 variants.
 *
 * TEXT properties:
 *   - label            — 레이블 (기본값: '이체 금액')
 *   - transferLimitText — 이체 가능 금액 표시 (기본값: '10,000,000원')
 *   - maxAmount        — Error 전용, 에러 메시지 내 잔액 (기본값: '3,000,000원')
 *
 * [레이아웃]
 *   comp(VERTICAL, gap=sm, padding=0+standard, FIXED 390, AUTO height)
 *     label(TEXT, xs, textLabel, bold)
 *     field(HORIZONTAL, FILL, FIXED 56h, border+radius)
 *       amount(TEXT, xl, grow=1) — error: danger / default: textHeading
 *       "원"(TEXT, base, textSecondary)
 *     quickRow(HORIZONTAL, WRAP, xs gap) — 6개 빠른 금액 버튼
 *       btn * 6 (AUTO width, FIXED 32h, padding=xs+md, brandBg, brandText)
 *     transferLimitRow(HORIZONTAL, SPACE_BETWEEN, FILL)
 *       "이체 가능 금액"(TEXT, xs, textMuted)
 *       transferLimitText(TEXT, xs, textMuted)
 *     errorRow(HORIZONTAL, FILL) — Error only
 *       "잔액(" + [maxAmount] + ")을 초과할 수 없습니다" (xs, dangerText)
 *
 * TEXT property 바인딩 타이밍:
 *   label            — comp 직접 자식, 자동 바인딩
 *   transferLimitText — comp.appendChild(transferLimitRow) 이후 수동 바인딩 (2단계)
 *   maxAmount        — comp.appendChild(errorRow) 이후 수동 바인딩 (2단계)
 *
 * 컴포넌트 이름: "AmountInput"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const QUICK_LABELS = ['+1만', '+5만', '+10만', '+50만', '+100만', '전액'];

async function createAmountInputVariant(state: 'Default' | 'Error'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm);
  setPadding(comp, 0, SPACING.standard);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* ── 레이블 — comp 직접 자식, 자동 바인딩 ───────────────────── */
  const labelNode = await addTextWithVar(
    comp, '이체 금액', FONT_SIZE.xs,
    COLOR_VAR.textLabel, COLOR.textLabel,
    true, SIZE_VAR.fontSizeXs, 'label',
  );
  labelNode.layoutSizingHorizontal = 'FILL';

  /* ── 입력 필드 ───────────────────────────────────────────────── */
  const field = figma.createFrame();
  setAutoLayout(field, 'HORIZONTAL', SPACING.sm);
  setPadding(field, 0, SPACING.standard);
  field.resize(390 - SPACING.standard * 2, 56);
  field.primaryAxisSizingMode = 'FIXED';
  field.counterAxisSizingMode = 'FIXED';
  field.primaryAxisAlignItems = 'SPACE_BETWEEN';
  field.counterAxisAlignItems = 'CENTER';
  field.strokeWeight = 1;
  field.strokeAlign = 'INSIDE';
  await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

  if (state === 'Error') {
    await setFillWithVar(field, COLOR_VAR.dangerSurface, COLOR.dangerSurface);
    await setStrokeWithVar(field, COLOR_VAR.danger, COLOR.danger);
  } else {
    await setFillWithVar(field, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(field, COLOR_VAR.border, COLOR.border);
  }

  const amountNode = await addTextWithVar(
    field, '0', FONT_SIZE.xl,
    state === 'Error' ? COLOR_VAR.danger      : COLOR_VAR.textHeading,
    state === 'Error' ? COLOR.danger          : COLOR.textHeading,
    true, SIZE_VAR.fontSizeXl,
  );
  amountNode.layoutGrow = 1;

  await addTextWithVar(
    field, '원', FONT_SIZE.base,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, SIZE_VAR.fontSizeBase,
  );

  comp.appendChild(field);
  field.layoutSizingHorizontal = 'FILL'; /* append 이후 FILL 설정 */

  /* ── 빠른 금액 선택 버튼 (WRAP으로 텍스트 잘림 방지) ─────────── */
  const quickRow = figma.createFrame();
  setAutoLayout(quickRow, 'HORIZONTAL', SPACING.xs);
  (quickRow as any).layoutWrap          = 'WRAP';
  (quickRow as any).counterAxisSpacing  = SPACING.xs; /* 줄 바꿈 시 행 간격 */
  quickRow.counterAxisSizingMode = 'AUTO';
  clearFill(quickRow);
  comp.appendChild(quickRow);
  quickRow.layoutSizingHorizontal = 'FILL';

  for (const btnLabel of QUICK_LABELS) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0, 'CENTER');
    setPadding(btn, SPACING.xs, SPACING.md);
    btn.resize(1, 32);                  /* height 고정; width는 AUTO가 결정 */
    btn.primaryAxisSizingMode = 'AUTO'; /* resize 이후 설정해야 반영됨 */
    btn.counterAxisSizingMode = 'FIXED';
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusMd, RADIUS.md);
    await setFillWithVar(btn, COLOR_VAR.brandBg, BRAND.bg);

    const btnText = await addTextWithVar(
      btn, btnLabel, FONT_SIZE.xs,
      COLOR_VAR.brandText, BRAND.text,
      true, SIZE_VAR.fontSizeXs,
    );
    btnText.textAlignHorizontal = 'CENTER';

    quickRow.appendChild(btn);
  }

  /* ── 이체 가능 금액 행 ─────────────────────────────────────── */
  const transferLimitRow = figma.createFrame();
  setAutoLayout(transferLimitRow, 'HORIZONTAL', 0, 'CENTER');
  transferLimitRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  transferLimitRow.counterAxisSizingMode = 'AUTO';
  clearFill(transferLimitRow);
  comp.appendChild(transferLimitRow);
  transferLimitRow.layoutSizingHorizontal = 'FILL';

  await addTextWithVar(
    transferLimitRow, '이체 가능 금액', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );

  /* transferLimitText — 2단계 수동 바인딩 (comp → transferLimitRow → text) */
  const transferLimitValueNode = await addTextWithVar(
    transferLimitRow, '10,000,000원', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  const transferLimitKey = comp.addComponentProperty('transferLimitText', 'TEXT', '10,000,000원');
  transferLimitValueNode.componentPropertyReferences = { characters: transferLimitKey };

  /* ── 에러 메시지: "잔액({maxAmount}원)을 초과할 수 없습니다" — Error only ── */
  if (state === 'Error') {
    const errorRow = figma.createFrame();
    setAutoLayout(errorRow, 'HORIZONTAL', 0, 'CENTER');
    errorRow.primaryAxisSizingMode = 'AUTO';
    errorRow.counterAxisSizingMode = 'AUTO';
    clearFill(errorRow);
    comp.appendChild(errorRow);
    errorRow.layoutSizingHorizontal = 'FILL'; /* 왼쪽 정렬: 부모 너비만큼 채움 */

    /* "잔액(" — 고정 텍스트 */
    await addTextWithVar(
      errorRow, '잔액(', FONT_SIZE.xs,
      COLOR_VAR.dangerText, COLOR.dangerText,
      false, SIZE_VAR.fontSizeXs,
    );

    /* maxAmount — 2단계 수동 바인딩 (comp → errorRow → text) */
    const maxAmountNode = await addTextWithVar(
      errorRow, '3,000,000원', FONT_SIZE.xs,
      COLOR_VAR.dangerText, COLOR.dangerText,
      false, SIZE_VAR.fontSizeXs,
    );

    /* ")을 초과할 수 없습니다" — 고정 텍스트 */
    await addTextWithVar(
      errorRow, ')을 초과할 수 없습니다', FONT_SIZE.xs,
      COLOR_VAR.dangerText, COLOR.dangerText,
      false, SIZE_VAR.fontSizeXs,
    );

    const maxAmountKey = comp.addComponentProperty('maxAmount', 'TEXT', '3,000,000원');
    maxAmountNode.componentPropertyReferences = { characters: maxAmountKey };
  }

  return comp;
}

export async function createAmountInput(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([
      createAmountInputVariant('Default'),
      createAmountInputVariant('Error'),
    ]),
    'AmountInput', 2,
  );
}
