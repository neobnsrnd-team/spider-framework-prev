/**
 * @file createInput.ts
 * @description Figma Input 컴포넌트 세트 생성.
 * React InputProps의 모든 주요 prop을 Figma variant로 표현하기 위해
 * 아래 5개 ComponentSet을 생성한다.
 *
 * 1. Input           — Size(2) × State(Default|Error|Success|Disabled) = 8
 * 2. Input/WithLabel — Size(2) × State(Default|Error|Success) = 6  (상단 label)
 * 3. Input/WithHelper— Size(2) × State(Default|Error|Success) = 6  (하단 helperText)
 * 4. Input/WithIcon  — Size(2) × Icon(Left|Right|Both) = 6         (아이콘 슬롯)
 * 5. Input/Format    — Size(2) × Format(Account|Phone) = 4         (포맷 placeholder)
 * 6. Input/FullWidth — Size(2) × State(Default|Error|Success) = 6  (w-full)
 *
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없으면 tokens.ts의 RGB fallback 적용.
 */

import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, setStrokeWithVar, addTextWithVar, addRect, setFloatVar,
} from '../../helpers';

type InputSize  = 'Medium' | 'Large';
type InputState = 'Default' | 'Error' | 'Success' | 'Disabled';
type InputIcon  = 'Left' | 'Right' | 'Both';
type InputFormat = 'Account' | 'Phone';

/** size별 높이 / 가로패딩 / 폰트 크기 */
const SIZE_CONFIG: Record<InputSize, { height: number; px: number; pxVar: string; fontSize: number; fontSizeVar: string }> = {
  Medium: { height: 48, px: SPACING.md,       pxVar: SIZE_VAR.spacingMd,       fontSize: FONT_SIZE.sm,   fontSizeVar: SIZE_VAR.fontSizeSm   },
  Large:  { height: 56, px: SPACING.standard, pxVar: SIZE_VAR.spacingStandard, fontSize: FONT_SIZE.base, fontSizeVar: SIZE_VAR.fontSizeBase },
};

/** validationState별 배경·테두리 변수 및 fallback */
const STATE_STYLE: Record<InputState, {
  bgVar: string;      bgFallback: Parameters<typeof setFillWithVar>[2];
  borderVar: string;  borderFallback: Parameters<typeof setFillWithVar>[2];
}> = {
  Default:  {
    bgVar:     COLOR_VAR.surface,       bgFallback:     COLOR.surface,
    borderVar: COLOR_VAR.border,        borderFallback: COLOR.border,
  },
  Error: {
    bgVar:     COLOR_VAR.dangerSurface, bgFallback:     COLOR.dangerSurface,
    borderVar: COLOR_VAR.danger, borderFallback: COLOR.danger,
  },
  Success: {
    bgVar:     COLOR_VAR.successSurface,bgFallback:     COLOR.successSurface,
    borderVar: COLOR_VAR.successBorder, borderFallback: COLOR.successBorder,
  },
  Disabled: {
    /* opacity-50 + bg-surface-raised 로 처리 — 테두리는 기본 border */
    bgVar:     COLOR_VAR.surfaceRaised, bgFallback:     COLOR.surfaceRaised,
    borderVar: COLOR_VAR.border,        borderFallback: COLOR.border,
  },
};

/* ── 내부 레이아웃 헬퍼 ────────────────────────────────────────── */

/**
 * Input 필드 프레임 기본 레이아웃 세팅.
 * @param fullWidth true이면 너비를 320으로 고정 (w-full 표현)
 */
async function buildInputField(
  comp: ComponentNode,
  size: InputSize,
  state: InputState,
  fullWidth = false,
): Promise<void> {
  const { height, px, pxVar, fontSize, fontSizeVar } = SIZE_CONFIG[size];
  const { bgVar, bgFallback, borderVar, borderFallback } = STATE_STYLE[state];

  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  setPadding(comp, 0, px);
  await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
  await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
  await setFloatVar(comp, 'paddingRight',  pxVar, px);
  await setFloatVar(comp, 'paddingLeft',   pxVar, px);
  comp.resize(fullWidth ? 320 : 280, height);
  comp.primaryAxisSizingMode  = 'FIXED';
  comp.counterAxisSizingMode  = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

  await setFillWithVar(comp, bgVar, bgFallback);
  await setStrokeWithVar(comp, borderVar, borderFallback);

  /* placeholder 텍스트 */
  const placeholder = await addTextWithVar(comp, '입력해주세요', fontSize, COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, fontSizeVar);
  placeholder.layoutGrow = 1;
  placeholder.textAlignVertical = 'CENTER';

  /* Disabled: 투명도로 표현 (opacity 50%) */
  if (state === 'Disabled') {
    comp.opacity = 0.5;
  }
}

/** 아이콘 플레이스홀더 사각형 추가 */
function addIconPlaceholder(comp: ComponentNode, size: InputSize): void {
  const iconSize = size === 'Large' ? 20 : 16;
  addRect(comp, iconSize, iconSize, COLOR.textMuted, RADIUS.xs);
}

/* ── ComponentSet 생성 함수 ─────────────────────────────────────── */

/**
 * 1. Input — Size × State(Default|Error|Success|Disabled) = 8
 * disabled prop + validationState(error|success) 모두 표현
 */
export async function createInput(): Promise<ComponentSetNode> {
  const sizes:  InputSize[]  = ['Medium', 'Large'];
  const states: InputState[] = ['Default', 'Error', 'Success', 'Disabled'];

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const state of states) {
      const comp = createComponent(`Size=${size}, State=${state}`);
      await buildInputField(comp, size, state);
      components.push(comp);
    }
  }

  /* cols=4: 한 행에 State 4개, 행마다 Size 변경 */
  return combineVariants(components, 'Input', 4);
}

/**
 * 2. Input/WithLabel — Size × State(Default|Error|Success) = 6
 * label prop 표현: 상단에 label 텍스트 추가, 세로 Auto Layout으로 묶음
 */
export async function createInputWithLabel(): Promise<ComponentSetNode> {
  const sizes:  InputSize[]  = ['Medium', 'Large'];
  /* Disabled는 기본 Input에서 커버하므로 3가지만 */
  const states: ('Default' | 'Error' | 'Success')[] = ['Default', 'Error', 'Success'];

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const state of states) {
      const { height, fontSize, fontSizeVar } = SIZE_CONFIG[size];
      const comp = createComponent(`Size=${size}, State=${state}`);

      /* 세로 Auto Layout: label + input field */
      setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
      comp.primaryAxisSizingMode = 'AUTO';
      comp.counterAxisSizingMode = 'AUTO';
      comp.fills = [];
      comp.strokes = [];

      /* label 텍스트 */
      const label = await addTextWithVar(comp, '레이블', FONT_SIZE.xs, COLOR_VAR.textLabel, COLOR.textLabel, true, SIZE_VAR.fontSizeXs);

      /* 입력 필드 프레임 */
      const field = figma.createFrame();
      field.name = 'field';
      setAutoLayout(field, 'HORIZONTAL', SPACING.xs);
      await setFloatVar(field, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
      setPadding(field, 0, SIZE_CONFIG[size].px);
      await setFloatVar(field, 'paddingTop',    SIZE_VAR.spacing0,              SPACING['0']);
      await setFloatVar(field, 'paddingBottom', SIZE_VAR.spacing0,              SPACING['0']);
      await setFloatVar(field, 'paddingRight',  SIZE_CONFIG[size].pxVar, SIZE_CONFIG[size].px);
      await setFloatVar(field, 'paddingLeft',   SIZE_CONFIG[size].pxVar, SIZE_CONFIG[size].px);
      field.resize(280, height);
      field.primaryAxisSizingMode = 'FIXED';
      field.counterAxisSizingMode = 'FIXED';
      await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

      const { bgVar, bgFallback, borderVar, borderFallback } = STATE_STYLE[state];
      await setFillWithVar(field, bgVar, bgFallback);
      await setStrokeWithVar(field, borderVar, borderFallback);

      const placeholder = await addTextWithVar(field, '입력해주세요', fontSize, COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, fontSizeVar);
      placeholder.layoutGrow = 1;
      placeholder.textAlignVertical = 'CENTER';

      comp.appendChild(field);
      components.push(comp);
    }
  }

  return combineVariants(components, 'Input/WithLabel', 3);
}

/**
 * 3. Input/WithHelper — Size × State(Default|Error|Success) = 6
 * helperText prop 표현: 하단에 보조 텍스트 추가, 색상은 state에 따라 변화
 */
export async function createInputWithHelper(): Promise<ComponentSetNode> {
  const sizes:  InputSize[]  = ['Medium', 'Large'];
  const states: ('Default' | 'Error' | 'Success')[] = ['Default', 'Error', 'Success'];

  /** state별 helperText 색상 변수·fallback */
  const helperStyle: Record<'Default' | 'Error' | 'Success', {
    varName: string; fallback: Parameters<typeof setFillWithVar>[2];
  }> = {
    Default: { varName: COLOR_VAR.textMuted,   fallback: COLOR.textMuted   },
    Error:   { varName: COLOR_VAR.dangerText,  fallback: COLOR.dangerText  },
    Success: { varName: COLOR_VAR.successText, fallback: COLOR.successText },
  };

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const state of states) {
      const { height, fontSize, fontSizeVar } = SIZE_CONFIG[size];
      const comp = createComponent(`Size=${size}, State=${state}`);

      setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
      comp.primaryAxisSizingMode = 'AUTO';
      comp.counterAxisSizingMode = 'AUTO';
      comp.fills = [];
      comp.strokes = [];

      /* 입력 필드 프레임 */
      const field = figma.createFrame();
      field.name = 'field';
      setAutoLayout(field, 'HORIZONTAL', SPACING.xs);
      await setFloatVar(field, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
      setPadding(field, 0, SIZE_CONFIG[size].px);
      await setFloatVar(field, 'paddingTop',    SIZE_VAR.spacing0,              SPACING['0']);
      await setFloatVar(field, 'paddingBottom', SIZE_VAR.spacing0,              SPACING['0']);
      await setFloatVar(field, 'paddingRight',  SIZE_CONFIG[size].pxVar, SIZE_CONFIG[size].px);
      await setFloatVar(field, 'paddingLeft',   SIZE_CONFIG[size].pxVar, SIZE_CONFIG[size].px);
      field.resize(280, height);
      field.primaryAxisSizingMode = 'FIXED';
      field.counterAxisSizingMode = 'FIXED';
      await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

      const { bgVar, bgFallback, borderVar, borderFallback } = STATE_STYLE[state];
      await setFillWithVar(field, bgVar, bgFallback);
      await setStrokeWithVar(field, borderVar, borderFallback);

      const placeholder = await addTextWithVar(field, '입력해주세요', fontSize, COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, fontSizeVar);
      placeholder.layoutGrow = 1;
      placeholder.textAlignVertical = 'CENTER';

      comp.appendChild(field);

      /* helperText */
      const { varName, fallback } = helperStyle[state];
      await addTextWithVar(comp, '안내 문구입니다', FONT_SIZE.xs, varName, fallback, false, SIZE_VAR.fontSizeXs);

      components.push(comp);
    }
  }

  return combineVariants(components, 'Input/WithHelper', 3);
}

/**
 * 4. Input/WithIcon — Size × Icon(Left|Right|Both) = 6
 * leftIcon / rightElement prop 표현: 아이콘 플레이스홀더(rect) 포함
 */
export async function createInputWithIcon(): Promise<ComponentSetNode> {
  const sizes: InputSize[]  = ['Medium', 'Large'];
  const icons: InputIcon[]  = ['Left', 'Right', 'Both'];

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const icon of icons) {
      const { height, px, pxVar, fontSize, fontSizeVar } = SIZE_CONFIG[size];

      const comp = createComponent(`Size=${size}, Icon=${icon}`);
      setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
      await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
      setPadding(comp, 0, px);
      await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(comp, 'paddingRight',  pxVar, px);
      await setFloatVar(comp, 'paddingLeft',   pxVar, px);
      comp.resize(280, height);
      comp.primaryAxisSizingMode = 'FIXED';
      comp.counterAxisSizingMode = 'FIXED';
      await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

      await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
      await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);

      /* 좌측 아이콘 */
      if (icon === 'Left' || icon === 'Both') {
        addIconPlaceholder(comp, size);
      }

      /* placeholder */
      const placeholder = await addTextWithVar(comp, '입력해주세요', fontSize, COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, fontSizeVar);
      placeholder.layoutGrow = 1;
      placeholder.textAlignVertical = 'CENTER';

      /* 우측 아이콘 */
      if (icon === 'Right' || icon === 'Both') {
        addIconPlaceholder(comp, size);
      }

      components.push(comp);
    }
  }

  return combineVariants(components, 'Input/WithIcon', 3);
}

/**
 * 5. Input/Format — Size × Format(Account|Phone) = 4
 * formatPattern(계좌번호) / phoneFormat(휴대폰) prop 표현.
 * placeholder 텍스트로 포맷 패턴을 시각화한다.
 */
export async function createInputFormat(): Promise<ComponentSetNode> {
  const sizes:   InputSize[]   = ['Medium', 'Large'];
  const formats: InputFormat[] = ['Account', 'Phone'];

  /** 포맷별 placeholder 예시 텍스트 */
  const FORMAT_PLACEHOLDER: Record<InputFormat, string> = {
    Account: '000-000000-00000',  // 하나은행 계좌번호 패턴 ###-######-#####
    Phone:   '010-1234-5678',     // 휴대폰번호 패턴 010-XXXX-XXXX
  };

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const format of formats) {
      const { height, px, pxVar, fontSize, fontSizeVar } = SIZE_CONFIG[size];

      const comp = createComponent(`Size=${size}, Format=${format}`);
      setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
      await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
      setPadding(comp, 0, px);
      await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(comp, 'paddingRight',  pxVar, px);
      await setFloatVar(comp, 'paddingLeft',   pxVar, px);
      comp.resize(280, height);
      comp.primaryAxisSizingMode = 'FIXED';
      comp.counterAxisSizingMode = 'FIXED';
      await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

      await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
      await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);

      /* 포맷 패턴을 placeholder로 표시 */
      const placeholder = await addTextWithVar(comp, FORMAT_PLACEHOLDER[format], fontSize, COLOR_VAR.textPlaceholder, COLOR.textPlaceholder, false, fontSizeVar);
      placeholder.layoutGrow = 1;
      placeholder.textAlignVertical = 'CENTER';

      components.push(comp);
    }
  }

  return combineVariants(components, 'Input/Format', 2);
}

/**
 * 6. Input/FullWidth — Size × State(Default|Error|Success) = 6
 * fullWidth=true prop 표현: 고정 너비 320px
 */
export async function createInputFullWidth(): Promise<ComponentSetNode> {
  const sizes:  InputSize[]  = ['Medium', 'Large'];
  const states: ('Default' | 'Error' | 'Success')[] = ['Default', 'Error', 'Success'];

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const state of states) {
      const comp = createComponent(`Size=${size}, State=${state}`);
      await buildInputField(comp, size, state, true);
      components.push(comp);
    }
  }

  return combineVariants(components, 'Input/FullWidth', 3);
}
