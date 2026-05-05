/**
 * @file createTransferForm.ts
 * @description Figma TransferForm 컴포넌트 세트 생성.
 * 이체 폼 컴포넌트.
 *
 * Submitting(True|False) = 2 variants.
 * - Submitting=False: 이체하기 버튼 활성, 모든 필드 입력 가능 상태
 * - Submitting=True : 처리 중 버튼 표시, 모든 필드 입력 가능 상태
 *
 * [필드 구성]
 *   받는 계좌번호 — 텍스트 입력 (active border)
 *   이체 금액     — 숫자 입력 + "원" 단위 / 이체 가능 금액 helper text
 *   메모 (선택)   — 텍스트 입력 (active border)
 *
 * 컴포넌트 이름: "TransferForm"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const FORM_WIDTH = 360;
const INPUT_HEIGHT = 56; /* h-14 */

/** 필드 레이블 생성 */
async function createFieldLabel(parent: FrameNode | ComponentNode, labelText: string): Promise<void> {
  const label = await addTextWithVar(
    parent, labelText, FONT_SIZE.xs,
    COLOR_VAR.textLabel, COLOR.textLabel,
    true, SIZE_VAR.fontSizeXs,
  );
  label.layoutSizingHorizontal = 'FILL';
}

/** 텍스트 입력 필드 생성 (항상 active 상태로 표현) */
async function createInputField(
  placeholder: string,
  value: string,
  height = INPUT_HEIGHT,
): Promise<FrameNode> {
  const field = figma.createFrame();
  setAutoLayout(field, 'HORIZONTAL', 0);
  setPadding(field, 0, SPACING.standard);
  field.resize(FORM_WIDTH, height);
  field.primaryAxisSizingMode = 'FIXED';
  field.counterAxisSizingMode = 'FIXED';
  field.counterAxisAlignItems = 'CENTER';
  await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(field, COLOR_VAR.surface, COLOR.surface);
  /* disabled 없이 항상 active(focus) 테두리 표시 */
  await setStrokeWithVar(field, COLOR_VAR.borderFocus, COLOR.borderFocus);

  const displayText = value || placeholder;
  const isPlaceholder = !value;
  const textNode = await addTextWithVar(
    field, displayText, FONT_SIZE.base,
    isPlaceholder ? COLOR_VAR.textPlaceholder : COLOR_VAR.textHeading,
    isPlaceholder ? COLOR.textPlaceholder     : COLOR.textHeading,
    !isPlaceholder, /* 값이 있으면 bold */
    SIZE_VAR.fontSizeBase,
  );
  textNode.layoutGrow = 1;

  return field;
}

/** 금액 입력 필드 (우측 "원" 단위 포함, 항상 active 상태) */
async function createAmountField(
  value: string,
): Promise<FrameNode> {
  const wrapper = figma.createFrame();
  setAutoLayout(wrapper, 'HORIZONTAL', SPACING.sm);
  wrapper.primaryAxisAlignItems = 'SPACE_BETWEEN';
  wrapper.counterAxisAlignItems = 'CENTER';
  setPadding(wrapper, 0, SPACING.standard);
  wrapper.resize(FORM_WIDTH, INPUT_HEIGHT);
  wrapper.primaryAxisSizingMode = 'FIXED';
  wrapper.counterAxisSizingMode = 'FIXED';
  await setFloatVar(wrapper, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(wrapper, COLOR_VAR.surface, COLOR.surface);
  /* disabled 없이 항상 active(focus) 테두리 표시 */
  await setStrokeWithVar(wrapper, COLOR_VAR.borderFocus, COLOR.borderFocus);

  const isPlaceholder = !value;
  const textNode = await addTextWithVar(
    wrapper,
    isPlaceholder ? '금액을 입력하세요' : value,
    FONT_SIZE.lg,
    isPlaceholder ? COLOR_VAR.textPlaceholder : COLOR_VAR.textHeading,
    isPlaceholder ? COLOR.textPlaceholder     : COLOR.textHeading,
    !isPlaceholder,
    SIZE_VAR.fontSizeLg,
  );
  textNode.layoutGrow = 1;

  if (!isPlaceholder) {
    await addTextWithVar(wrapper, '원', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);
  }

  return wrapper;
}

/** helper 텍스트 (이체 가능 금액 등) */
async function createHelperText(text: string): Promise<FrameNode> {
  const wrap = figma.createFrame();
  setAutoLayout(wrap, 'HORIZONTAL', 0, 'MIN');
  wrap.resize(FORM_WIDTH, 1);
  wrap.primaryAxisSizingMode = 'FIXED';
  wrap.counterAxisSizingMode = 'AUTO';
  clearFill(wrap);
  await addTextWithVar(wrap, text, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  return wrap;
}

/** 이체 버튼 */
async function createSubmitButton(submitting: boolean): Promise<FrameNode> {
  const btn = figma.createFrame();
  setAutoLayout(btn, 'HORIZONTAL', 0);
  btn.primaryAxisAlignItems = 'CENTER';
  btn.counterAxisAlignItems = 'CENTER';
  btn.resize(FORM_WIDTH, INPUT_HEIGHT);
  btn.primaryAxisSizingMode = 'FIXED';
  btn.counterAxisSizingMode = 'FIXED';
  await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  if (submitting) {
    /* 비활성화: 브랜드 색 + opacity 40% */
    btn.fills = [{ type: 'SOLID', color: BRAND.primary, opacity: 0.4 }];
  } else {
    await setFillWithVar(btn, COLOR_VAR.brandPrimary, BRAND.primary);
  }

  const btnLabel = submitting ? '처리 중' : '이체하기';
  await addTextWithVar(btn, btnLabel, FONT_SIZE.lg, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeLg);
  return btn;
}

/** 필드 그룹 래퍼 (label + input 수직 스택) */
function createFieldGroup(): FrameNode {
  const group = figma.createFrame();
  setAutoLayout(group, 'VERTICAL', SPACING.xs, 'MIN');
  group.resize(FORM_WIDTH, 1);
  group.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  group.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  clearFill(group);
  return group;
}

async function createTransferFormVariant(submitting: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Submitting=${submitting ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.lg, 'MIN');
  comp.resize(FORM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  clearFill(comp);

  /* ── 받는 계좌번호 ── */
  const accountGroup = createFieldGroup();
  await createFieldLabel(accountGroup, '받는 계좌번호');
  accountGroup.appendChild(await createInputField('계좌번호를 입력하세요', '123-456789-01234'));
  comp.appendChild(accountGroup);

  /* ── 이체 금액 ── */
  const amountGroup = createFieldGroup();
  await createFieldLabel(amountGroup, '이체 금액');
  amountGroup.appendChild(await createAmountField('500,000'));
  amountGroup.appendChild(await createHelperText('이체 가능 금액: 4,730,067원'));
  comp.appendChild(amountGroup);

  /* ── 메모 (선택) ── */
  const memoGroup = createFieldGroup();
  await createFieldLabel(memoGroup, '메모 (선택)');
  memoGroup.appendChild(await createInputField('메모를 입력하세요', '생활비', 48));
  comp.appendChild(memoGroup);

  /* ── 이체 / 처리 중 버튼 ── */
  comp.appendChild(await createSubmitButton(submitting));

  return comp;
}

export async function createTransferForm(): Promise<ComponentSetNode> {
  const components = [
    await createTransferFormVariant(false),
    await createTransferFormVariant(true),
  ];
  return combineVariants(components, 'TransferForm', 2);
}
