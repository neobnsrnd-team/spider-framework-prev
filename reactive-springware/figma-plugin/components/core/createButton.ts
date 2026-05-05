/**
 * @file createButton.ts
 * @description Figma Button 컴포넌트 세트 생성.
 *
 * Variant(4) × Size(3) × Mode(8) = 96 variants 단일 ComponentSet으로 구성한다.
 *
 * Mode 8가지 (각 Variant×Size에 대해):
 *   1. Default          — Disabled=False, FullWidth=False, LeftIcon=False, RightIcon=False
 *   2. Disabled         — Disabled=True,  FullWidth=False, LeftIcon=False, RightIcon=False
 *   3. FullWidth        — Disabled=False, FullWidth=True,  LeftIcon=False, RightIcon=False
 *   4. LeftIcon         — Disabled=False, FullWidth=False, LeftIcon=True,  RightIcon=False
 *   5. RightIcon        — Disabled=False, FullWidth=False, LeftIcon=False, RightIcon=True
 *   6. LeftIcon+Both    — Disabled=False, FullWidth=False, LeftIcon=True,  RightIcon=True
 *   7. LeftIcon+Full    — Disabled=False, FullWidth=True,  LeftIcon=True,  RightIcon=False
 *   8. RightIcon+Full   — Disabled=False, FullWidth=True,  LeftIcon=False, RightIcon=True
 *
 * 제외 이유:
 *   - loading: 런타임 스피너 애니메이션으로 Figma 표현 불가
 *   - Justify: FullWidth와 LeftIcon/RightIcon을 분리 관리하므로 불필요
 *   - Disabled × Icon/FullWidth 조합: 시각적 차이 없어 중복 제외
 *
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없으면 tokens.ts의 RGB fallback 적용.
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, setStroke, clearStroke, addTextWithVar, setFloatVar,
  addIconSlot,
} from '../../utils/helpers';

type ButtonVariant = 'Primary' | 'Outline' | 'Ghost' | 'Danger';
type ButtonSize    = 'Small' | 'Medium' | 'Large';

/** size별 height / padding-x / font-size / border-radius / icon 크기 */
const SIZE_CONFIG: Record<ButtonSize, {
  height: number; px: number; pxVar: string; fontSize: number; fontSizeVar: string; radius: number; radiusVar: string; iconSize: number;
}> = {
  /* component-library: sm=rounded-lg(16px), md/lg=rounded-xl(24px) */
  Small:  { height: 32, px: SPACING.md,       pxVar: SIZE_VAR.spacingMd,       fontSize: FONT_SIZE.xs, fontSizeVar: SIZE_VAR.fontSizeXs, radius: RADIUS.lg, radiusVar: SIZE_VAR.radiusLg, iconSize: 12 },
  Medium: { height: 40, px: SPACING.standard,  pxVar: SIZE_VAR.spacingStandard, fontSize: FONT_SIZE.sm, fontSizeVar: SIZE_VAR.fontSizeSm, radius: RADIUS.xl, radiusVar: SIZE_VAR.radiusXl, iconSize: 14 },
  Large:  { height: 56, px: SPACING.xl,        pxVar: SIZE_VAR.spacingXl,       fontSize: FONT_SIZE.lg, fontSizeVar: SIZE_VAR.fontSizeLg, radius: RADIUS.xl, radiusVar: SIZE_VAR.radiusXl, iconSize: 16 },
};

/**
 * Variant × Disabled 조합의 배경 변수·fallback 반환.
 * Outline/Ghost는 투명 배경이므로 null 반환.
 */
function getBgStyle(variant: ButtonVariant, disabled: boolean) {
  if (disabled) {
    if (variant === 'Outline' || variant === 'Ghost') return null;
    return { varName: COLOR_VAR.textDisabled, fallback: COLOR.textDisabled };
  }
  switch (variant) {
    case 'Primary': return { varName: COLOR_VAR.brandPrimary, fallback: BRAND.primary };
    case 'Danger':  return { varName: COLOR_VAR.danger,       fallback: COLOR.dangerDark };
    default:        return null;
  }
}

/**
 * Variant × Disabled 조합의 텍스트 변수·fallback 반환.
 */
function getTextStyle(variant: ButtonVariant, disabled: boolean) {
  if (disabled) {
    /* Primary/Danger disabled: 배경이 회색이므로 흰색 텍스트 유지 */
    if (variant === 'Primary' || variant === 'Danger') {
      return { varName: COLOR_VAR.surface, fallback: COLOR.surface };
    }
    return { varName: COLOR_VAR.textDisabled, fallback: COLOR.textDisabled };
  }
  switch (variant) {
    case 'Primary': return { varName: COLOR_VAR.brandFg,   fallback: BRAND.fg   };
    case 'Danger':  return { varName: COLOR_VAR.surface,   fallback: COLOR.surface };
    default:        return { varName: COLOR_VAR.brandText, fallback: BRAND.text  };
  }
}

/**
 * 아이콘 슬롯 추가.
 * Icons/{name} 컴포넌트가 있으면 INSTANCE_SWAP, 없으면 rect fallback.
 * fallback 색상은 해당 variant의 기본(non-disabled) 텍스트 색을 따른다.
 */
function addIconPlaceholder(
  comp: ComponentNode,
  size: ButtonSize,
  variant: ButtonVariant,
  propertyName: 'leftIcon' | 'rightIcon',
) {
  const { iconSize } = SIZE_CONFIG[size];
  const iconColor = getTextStyle(variant, false).fallback;
  addIconSlot(comp, 'ChevronRight', iconSize, iconColor, propertyName);
}

/**
 * Button — Variant(4) × Size(3) × Mode(8) = 96 variants
 *
 * cols=8로 배치 시 한 행 = 하나의 (Variant, Size) 조합의 Mode 8가지.
 */
export async function createButton(): Promise<ComponentSetNode> {
  const variants: ButtonVariant[] = ['Primary', 'Outline', 'Ghost', 'Danger'];
  const sizes:    ButtonSize[]    = ['Small', 'Medium', 'Large'];

  /** Mode별 prop 조합. 순서가 Figma 캔버스 열 순서를 결정한다 */
  const modes: {
    disabled: 'True' | 'False';
    fullWidth: 'True' | 'False';
    leftIcon:  'True' | 'False';
    rightIcon: 'True' | 'False';
  }[] = [
    { disabled: 'False', fullWidth: 'False', leftIcon: 'False', rightIcon: 'False' }, // Default
    { disabled: 'True',  fullWidth: 'False', leftIcon: 'False', rightIcon: 'False' }, // Disabled
    { disabled: 'False', fullWidth: 'True',  leftIcon: 'False', rightIcon: 'False' }, // FullWidth
    { disabled: 'False', fullWidth: 'False', leftIcon: 'True',  rightIcon: 'False' }, // LeftIcon
    { disabled: 'False', fullWidth: 'False', leftIcon: 'False', rightIcon: 'True'  }, // RightIcon
    { disabled: 'False', fullWidth: 'False', leftIcon: 'True',  rightIcon: 'True'  }, // LeftIcon + RightIcon
    { disabled: 'False', fullWidth: 'True',  leftIcon: 'True',  rightIcon: 'False' }, // LeftIcon + FullWidth
    { disabled: 'False', fullWidth: 'True',  leftIcon: 'False', rightIcon: 'True'  }, // RightIcon + FullWidth
  ];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const size of sizes) {
      for (const mode of modes) {
        const isDisabled  = mode.disabled  === 'True';
        const isFullWidth = mode.fullWidth === 'True';
        const hasLeftIcon  = mode.leftIcon  === 'True';
        const hasRightIcon = mode.rightIcon === 'True';

        const comp = createComponent(
          `Variant=${variant}, Size=${size}, Disabled=${mode.disabled}, FullWidth=${mode.fullWidth}, LeftIcon=${mode.leftIcon}, RightIcon=${mode.rightIcon}`,
        );

        /* ── 레이아웃 ── */
        const { height, px, pxVar, radius, radiusVar } = SIZE_CONFIG[size];
        setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
        await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingSm, SPACING.sm);
        setPadding(comp, 0, px);
        await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
        await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
        await setFloatVar(comp, 'paddingRight',  pxVar, px);
        await setFloatVar(comp, 'paddingLeft',   pxVar, px);
        comp.resize(isFullWidth ? 320 : 120, height);
        comp.primaryAxisSizingMode = isFullWidth ? 'FIXED' : 'AUTO';
        comp.counterAxisSizingMode = 'FIXED';
        await setFloatVar(comp, 'cornerRadius', radiusVar, radius);


        /* ── 배경 ── */
        const bg = getBgStyle(variant, isDisabled);
        if (bg) {
          await setFillWithVar(comp, bg.varName, bg.fallback);
        } else {
          comp.fills = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
        }

        /* ── 테두리 (Outline variant) ── */
        if (variant === 'Outline') {
          if (isDisabled) {
            /* disabled Outline: 테두리를 border 색상 변수로 교체 */
            await setFillWithVar(
              comp as unknown as Parameters<typeof setFillWithVar>[0],
              COLOR_VAR.border,
              COLOR.border,
            );
            comp.strokes = comp.fills as unknown as Paint[];
            comp.fills   = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
            comp.strokeWeight = 1;
            comp.strokeAlign  = 'INSIDE';
          } else {
            setStroke(comp, BRAND.primary);
          }
        } else {
          clearStroke(comp);
        }

        /* ── 콘텐츠 (아이콘 + 텍스트) ── */
        if (hasLeftIcon) addIconPlaceholder(comp, size, variant, 'leftIcon');

        const text = getTextStyle(variant, isDisabled);
        const label = await addTextWithVar(
          comp, '버튼',
          SIZE_CONFIG[size].fontSize, text.varName, text.fallback,
          true, SIZE_CONFIG[size].fontSizeVar, 'label',
        );

        /* FullWidth + 단일 아이콘: layoutGrow=1로 텍스트를 확장해 아이콘을 반대쪽 끝으로 밀기
         * SPACE_BETWEEN은 Figma ComponentSet 클릭 시 per-variant 정렬이 초기화되므로 사용하지 않음 */
        if (isFullWidth && (hasLeftIcon !== hasRightIcon)) {
          label.layoutGrow = 1;
          label.textAlignHorizontal = hasLeftIcon ? 'RIGHT' : 'LEFT';
        } else {
          label.textAlignHorizontal = 'CENTER';
        }

        if (hasRightIcon) addIconPlaceholder(comp, size, variant, 'rightIcon');

        components.push(comp);
      }
    }
  }

  /* cols=8: 한 행 = 하나의 (Variant, Size) 조합 × Mode 8가지 */
  return combineVariants(components, 'Button', 8);
}
