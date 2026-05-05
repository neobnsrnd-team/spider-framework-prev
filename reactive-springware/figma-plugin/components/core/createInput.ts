/**
 * @file createInput.ts
 * @description Figma Input 컴포넌트 세트 생성.
 *
 * Size(2) × Mode(6) = 12 variants 단일 ComponentSet으로 구성한다.
 *
 * Mode 6가지 (각 Size에 대해):
 *   1. Default   — ValidationState=Default, Disabled=False, LeftIcon=False, FullWidth=False
 *   2. Error     — ValidationState=Error,   Disabled=False, LeftIcon=False, FullWidth=False
 *   3. Success   — ValidationState=Success, Disabled=False, LeftIcon=False, FullWidth=False
 *   4. Disabled  — ValidationState=Default, Disabled=True,  LeftIcon=False, FullWidth=False
 *   5. LeftIcon  — ValidationState=Default, Disabled=False, LeftIcon=True,  FullWidth=False
 *   6. FullWidth — ValidationState=Default, Disabled=False, LeftIcon=False, FullWidth=True
 *
 * TEXT properties (인스턴스에서 직접 편집 가능):
 *   - label       — 입력 필드 상단 레이블
 *   - placeholder — 입력 필드 내부 힌트 텍스트
 *   - helperText  — 입력 필드 하단 안내 문구 (색상은 ValidationState 반영)
 *
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없으면 tokens.ts의 RGB fallback 적용.
 */

import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../utils/helpers';

type InputSize            = 'Medium' | 'Large';
type InputValidationState = 'Default' | 'Error' | 'Success';

/** size별 높이 / 가로패딩 / 폰트 크기 */
const SIZE_CONFIG: Record<InputSize, {
  height: number; px: number; pxVar: string; fontSize: number; fontSizeVar: string;
}> = {
  Medium: { height: 48, px: SPACING.md,       pxVar: SIZE_VAR.spacingMd,       fontSize: FONT_SIZE.sm,   fontSizeVar: SIZE_VAR.fontSizeSm   },
  Large:  { height: 56, px: SPACING.standard, pxVar: SIZE_VAR.spacingStandard, fontSize: FONT_SIZE.base, fontSizeVar: SIZE_VAR.fontSizeBase },
};

/** ValidationState별 배경·테두리 변수 및 fallback */
const STATE_STYLE: Record<InputValidationState, {
  bgVar: string;     bgFallback:     Parameters<typeof setFillWithVar>[2];
  borderVar: string; borderFallback: Parameters<typeof setFillWithVar>[2];
}> = {
  Default: {
    bgVar:     COLOR_VAR.surface,        bgFallback:     COLOR.surface,
    borderVar: COLOR_VAR.border,         borderFallback: COLOR.border,
  },
  Error: {
    bgVar:     COLOR_VAR.dangerSurface,  bgFallback:     COLOR.dangerSurface,
    borderVar: COLOR_VAR.danger,         borderFallback: COLOR.danger,
  },
  Success: {
    bgVar:     COLOR_VAR.successSurface, bgFallback:     COLOR.successSurface,
    borderVar: COLOR_VAR.successBorder,  borderFallback: COLOR.successBorder,
  },
};

/** ValidationState별 helperText 색상 */
const HELPER_STYLE: Record<InputValidationState, {
  varName: string; fallback: Parameters<typeof setFillWithVar>[2];
}> = {
  Default: { varName: COLOR_VAR.textMuted,   fallback: COLOR.textMuted   },
  Error:   { varName: COLOR_VAR.dangerText,  fallback: COLOR.dangerText  },
  Success: { varName: COLOR_VAR.successText, fallback: COLOR.successText },
};

/**
 * Input — Size(2) × Mode(6) = 12 variants
 *
 * cols=6으로 배치 시 한 행 = 하나의 Size에 대한 Mode 6가지.
 */
export async function createInput(): Promise<ComponentSetNode> {
  const sizes: InputSize[] = ['Medium', 'Large'];

  /** Mode별 prop 조합. 순서가 Figma 캔버스 열 순서를 결정한다 */
  const modes: {
    validationState: InputValidationState;
    disabled:  'True' | 'False';
    leftIcon:  'True' | 'False';
    fullWidth: 'True' | 'False';
  }[] = [
    { validationState: 'Default', disabled: 'False', leftIcon: 'False', fullWidth: 'False' }, // Default
    { validationState: 'Error',   disabled: 'False', leftIcon: 'False', fullWidth: 'False' }, // Error
    { validationState: 'Success', disabled: 'False', leftIcon: 'False', fullWidth: 'False' }, // Success
    { validationState: 'Default', disabled: 'True',  leftIcon: 'False', fullWidth: 'False' }, // Disabled
    { validationState: 'Default', disabled: 'False', leftIcon: 'True',  fullWidth: 'False' }, // LeftIcon
    { validationState: 'Default', disabled: 'False', leftIcon: 'False', fullWidth: 'True'  }, // FullWidth
  ];

  const components: ComponentNode[] = [];

  for (const size of sizes) {
    for (const mode of modes) {
      const isDisabled  = mode.disabled  === 'True';
      const hasLeftIcon = mode.leftIcon  === 'True';
      const isFullWidth = mode.fullWidth === 'True';
      const vs          = mode.validationState;

      const comp = createComponent(
        `Size=${size}, ValidationState=${vs}, Disabled=${mode.disabled}, LeftIcon=${mode.leftIcon}, FullWidth=${mode.fullWidth}`,
      );

      /* ── 외부 래퍼: VERTICAL (label → field → helperText) ── */
      setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
      comp.primaryAxisSizingMode = 'AUTO';
      comp.counterAxisSizingMode = 'AUTO';
      comp.fills   = [];
      comp.strokes = [];

      /* label: 상단 레이블 — TEXT property로 인스턴스에서 편집 가능 */
      await addTextWithVar(
        comp, '레이블', FONT_SIZE.xs,
        COLOR_VAR.textLabel, COLOR.textLabel,
        true, SIZE_VAR.fontSizeXs, 'label',
      );

      /* ── 입력 필드 프레임: HORIZONTAL ([leftIcon?] → placeholder) ── */
      const { height, px, pxVar, fontSize, fontSizeVar } = SIZE_CONFIG[size];
      const { bgVar, bgFallback, borderVar, borderFallback } = STATE_STYLE[vs];

      const field = figma.createFrame();
      field.name = 'field';
      setAutoLayout(field, 'HORIZONTAL', SPACING.xs);
      await setFloatVar(field, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
      setPadding(field, 0, px);
      await setFloatVar(field, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(field, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
      await setFloatVar(field, 'paddingRight',  pxVar, px);
      await setFloatVar(field, 'paddingLeft',   pxVar, px);
      field.resize(isFullWidth ? 320 : 280, height);
      field.primaryAxisSizingMode = 'FIXED';
      field.counterAxisSizingMode = 'FIXED';
      await setFloatVar(field, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
      await setFillWithVar(field, bgVar, bgFallback);
      await setStrokeWithVar(field, borderVar, borderFallback);

      /* field를 comp에 추가 — TEXT property reference 바인딩 전 삽입 필요 */
      comp.appendChild(field);

      /* 좌측 아이콘 슬롯
       * INSTANCE_SWAP property는 comp에 등록, 실제 노드는 field 내부에 배치 */
      if (hasLeftIcon) {
        const iconSize = size === 'Large' ? 20 : 16;
        addIconSlot(comp, 'Search', iconSize, COLOR.textMuted, 'leftIcon', field);
      }

      /* placeholder: TEXT property를 comp에 바인딩, 노드는 field에 삽입 */
      const placeholder = await addTextWithVar(
        field, '입력해주세요', fontSize,
        COLOR_VAR.textPlaceholder, COLOR.textPlaceholder,
        false, fontSizeVar, 'placeholder', comp,
      );
      placeholder.layoutGrow = 1;
      placeholder.textAlignVertical = 'CENTER';

      /* Disabled: opacity 50%로 전체 비활성화 표현 */
      if (isDisabled) comp.opacity = 0.5;

      /* helperText: 하단 안내 문구 — 색상은 ValidationState 반영 */
      const { varName: helperVar, fallback: helperFallback } = HELPER_STYLE[vs];
      await addTextWithVar(
        comp, '안내 문구입니다', FONT_SIZE.xs,
        helperVar, helperFallback,
        false, SIZE_VAR.fontSizeXs, 'helperText',
      );

      components.push(comp);
    }
  }

  /* cols=6: 한 행 = 하나의 Size에 대한 Mode 6가지 */
  return combineVariants(components, 'Input', 6);
}
