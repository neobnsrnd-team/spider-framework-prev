/**
 * @file createTransferForm.ts
 * @description Figma TransferForm 컴포넌트 세트 생성.
 * 이체 폼 컴포넌트 (순차 활성화 방식).
 *
 * State(Step1|Step2|Step3) = 3 variants.
 * - Step1: 받는 계좌번호 입력 활성 / 금액·메모·버튼 비활성(opacity 40%)
 * - Step2: 계좌 완료 + 금액 입력 활성 / 메모·버튼 활성
 * - Step3: 모든 필드 완료 + 이체 버튼 활성
 *
 * 컴포넌트 이름: "TransferForm"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

const FORM_WIDTH = 360;
const INPUT_HEIGHT = 56; /* h-14 */

type TransferStep = 'Step1' | 'Step2' | 'Step3';

/** 필드 레이블 생성 */
async function createFieldLabel(parent: FrameNode | ComponentNode, labelText: string): Promise<void> {
  await addTextWithVar(parent, labelText, FONT_SIZE.xs, COLOR_VAR.textLabel, COLOR.textLabel, true, SIZE_VAR.fontSizeXs);
}

/** 텍스트 입력 필드 생성 */
async function createInputField(
  placeholder: string,
  value: string,
  active: boolean,
  height = INPUT_HEIGHT,
): Promise<FrameNode> {
  const field = figma.createFrame();
  setAutoLayout(field, 'HORIZONTAL', 0);
  setPadding(field, 0, SPACING.standard);
  field.resize(FORM_WIDTH, height);
  field.primaryAxisSizingMode = 'FIXED';
  field.counterAxisSizingMode = 'FIXED';
  await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(field, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(field, active ? COLOR_VAR.borderFocus : COLOR_VAR.border, active ? COLOR.borderFocus : COLOR.border);

  const displayText = value || placeholder;
  const isPlaceholder = !value;
  await addTextWithVar(
    field, displayText, FONT_SIZE.base,
    isPlaceholder ? COLOR_VAR.textPlaceholder : COLOR_VAR.textHeading,
    isPlaceholder ? COLOR.textPlaceholder     : COLOR.textHeading,
    !isPlaceholder, /* value가 있으면 bold */
    SIZE_VAR.fontSizeBase,
  );

  return field;
}

/** 금액 입력 필드 (우측 "원" 단위 포함) */
async function createAmountField(
  placeholder: string,
  value: string,
  active: boolean,
): Promise<FrameNode> {
  const wrapper = figma.createFrame();
  setAutoLayout(wrapper, 'HORIZONTAL', 0);
  wrapper.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(wrapper, 0, SPACING.standard);
  wrapper.resize(FORM_WIDTH, INPUT_HEIGHT);
  wrapper.primaryAxisSizingMode = 'FIXED';
  wrapper.counterAxisSizingMode = 'FIXED';
  await setFloatVar(wrapper, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(wrapper, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(wrapper, active ? COLOR_VAR.borderFocus : COLOR_VAR.border, active ? COLOR.borderFocus : COLOR.border);

  const displayText = value || placeholder;
  const isPlaceholder = !value;
  const textNode = await addTextWithVar(
    wrapper, displayText, FONT_SIZE.lg,
    isPlaceholder ? COLOR_VAR.textPlaceholder : COLOR_VAR.textHeading,
    isPlaceholder ? COLOR.textPlaceholder     : COLOR.textHeading,
    !isPlaceholder,
    SIZE_VAR.fontSizeLg,
  );
  textNode.layoutGrow = 1;

  if (!isPlaceholder) {
    /* "원" 단위 표시 */
    await addTextWithVar(wrapper, '원', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);
  }

  return wrapper;
}

/** 잔액 도움말 텍스트 */
async function createHelperText(text: string, isError = false): Promise<FrameNode> {
  const wrap = figma.createFrame();
  setAutoLayout(wrap, 'HORIZONTAL', 0, 'MIN');
  wrap.resize(FORM_WIDTH, 1);
  wrap.primaryAxisSizingMode = 'FIXED';
  wrap.counterAxisSizingMode = 'AUTO';
  clearFill(wrap);
  await addTextWithVar(
    wrap, text, FONT_SIZE.xs,
    isError ? COLOR_VAR.dangerText : COLOR_VAR.textMuted,
    isError ? COLOR.dangerText     : COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  return wrap;
}

/** 이체 버튼 */
async function createSubmitButton(enabled: boolean): Promise<FrameNode> {
  const btn = figma.createFrame();
  setAutoLayout(btn, 'HORIZONTAL', 0);
  btn.resize(FORM_WIDTH, INPUT_HEIGHT);
  btn.primaryAxisSizingMode = 'FIXED';
  btn.counterAxisSizingMode = 'FIXED';
  await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  if (enabled) {
    await setFillWithVar(btn, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    /* 비활성: 브랜드 색 + opacity 40% */
    btn.fills = [{ type: 'SOLID', color: BRAND.primary, opacity: 0.4 }];
  }

  await addTextWithVar(btn, '이체하기', FONT_SIZE.lg, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeLg);
  return btn;
}

/** 필드 그룹 래퍼 (label + input 수직 스택) */
function createFieldGroup(opacity?: number): FrameNode {
  const group = figma.createFrame();
  setAutoLayout(group, 'VERTICAL', SPACING.xs, 'MIN');
  group.resize(FORM_WIDTH, 1);
  group.primaryAxisSizingMode = 'FIXED';
  group.counterAxisSizingMode = 'AUTO';
  clearFill(group);
  if (opacity !== undefined) group.opacity = opacity;
  return group;
}

async function createTransferFormVariant(step: TransferStep): Promise<ComponentNode> {
  const comp = createComponent(`State=${step}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.lg, 'MIN');
  comp.resize(FORM_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* ── 1단계: 받는 계좌번호 ── */
  const accountGroup = createFieldGroup();
  await createFieldLabel(accountGroup, '받는 계좌번호');
  const accountDone = step !== 'Step1';
  accountGroup.appendChild(await createInputField(
    '계좌번호를 입력하세요',
    accountDone ? '123-456789-01234' : '',
    step === 'Step1',
  ));
  comp.appendChild(accountGroup);

  /* ── 2단계: 이체 금액 (계좌 완료 후 활성화) ── */
  const amountOpacity = step === 'Step1' ? 0.4 : 1;
  const amountGroup = createFieldGroup(amountOpacity);
  await createFieldLabel(amountGroup, '이체 금액');
  const amountDone = step === 'Step3';
  amountGroup.appendChild(await createAmountField(
    '금액을 입력하세요',
    amountDone ? '500,000' : '',
    step === 'Step2',
  ));
  amountGroup.appendChild(await createHelperText('이체 가능 금액: 4,730,067원'));
  comp.appendChild(amountGroup);

  /* ── 3단계: 메모 (금액 완료 후 활성화) ── */
  const memoOpacity = step !== 'Step3' ? 0.4 : 1;
  const memoGroup = createFieldGroup(memoOpacity);
  await createFieldLabel(memoGroup, '메모 (선택)');
  memoGroup.appendChild(await createInputField(
    '메모를 입력하세요',
    step === 'Step3' ? '생활비' : '',
    step === 'Step3',
    48, /* h-12 */
  ));
  comp.appendChild(memoGroup);

  /* ── 이체 버튼 ── */
  comp.appendChild(await createSubmitButton(step === 'Step3'));

  return comp;
}

export async function createTransferForm(): Promise<ComponentSetNode> {
  const components = [
    await createTransferFormVariant('Step1'),
    await createTransferFormVariant('Step2'),
    await createTransferFormVariant('Step3'),
  ];
  return combineVariants(components, 'TransferForm', 1);
}
