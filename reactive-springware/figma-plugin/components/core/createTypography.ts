/**
 * @file createTypography.ts
 * @description Figma Typography 컴포넌트 세트 생성.
 * React Typography의 variant / weight / color prop을 Figma variant로 매핑한다.
 *
 * Variant(6) × Color(8) × Weight(3) = 144 variants
 *   cols=3: 한 행 = (Variant, Color) 조합의 Normal | Medium | Bold
 *   총 48행 구성
 *
 * variant-mapping.json: Color → color (global), 값은 lowercase 전략으로 자동 변환
 *   예) Color=Brand → color="brand", Color=Secondary → color="secondary"
 */

import { COLOR, BRAND, FONT_FAMILY, FONT_SIZE, LINE_HEIGHT, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import { createComponent, combineVariants, clearFill, addTextWithVar, setLineHeightVar } from '../../utils/helpers';

type TypographyVariant = 'Heading' | 'Subheading' | 'BodyLg' | 'Body' | 'BodySm' | 'Caption';
type TypographyWeight  = 'Normal' | 'Medium' | 'Bold';
type TypographyColor   = 'Heading' | 'Base' | 'Label' | 'Secondary' | 'Muted' | 'Brand' | 'Danger' | 'Success';

/** variant별 fontSize / lineHeight 토큰 */
const VARIANT_CONFIG: Record<TypographyVariant, {
  fontSize: number; fontSizeVar: string;
  lineHeight: number; lineHeightVar: string;
}> = {
  Heading:    { fontSize: FONT_SIZE['2xl'], fontSizeVar: SIZE_VAR.fontSize2xl,  lineHeight: LINE_HEIGHT['2xl'], lineHeightVar: SIZE_VAR.lineHeight2xl  },
  Subheading: { fontSize: FONT_SIZE.xl,    fontSizeVar: SIZE_VAR.fontSizeXl,   lineHeight: LINE_HEIGHT.xl,    lineHeightVar: SIZE_VAR.lineHeightXl   },
  BodyLg:     { fontSize: FONT_SIZE.lg,    fontSizeVar: SIZE_VAR.fontSizeLg,   lineHeight: LINE_HEIGHT.lg,    lineHeightVar: SIZE_VAR.lineHeightLg   },
  Body:       { fontSize: FONT_SIZE.base,  fontSizeVar: SIZE_VAR.fontSizeBase, lineHeight: LINE_HEIGHT.base,  lineHeightVar: SIZE_VAR.lineHeightBase },
  BodySm:     { fontSize: FONT_SIZE.sm,    fontSizeVar: SIZE_VAR.fontSizeSm,   lineHeight: LINE_HEIGHT.sm,    lineHeightVar: SIZE_VAR.lineHeightSm   },
  Caption:    { fontSize: FONT_SIZE.xs,    fontSizeVar: SIZE_VAR.fontSizeXs,   lineHeight: LINE_HEIGHT.xs,    lineHeightVar: SIZE_VAR.lineHeightXs   },
};

/** color별 텍스트 색상 변수 및 fallback */
const COLOR_CONFIG: Record<TypographyColor, {
  varName: string; fallback: Parameters<typeof addTextWithVar>[4];
}> = {
  Heading:   { varName: COLOR_VAR.textHeading,   fallback: COLOR.textHeading   },
  Base:      { varName: COLOR_VAR.textBase,       fallback: COLOR.textBase      },
  Label:     { varName: COLOR_VAR.textLabel,      fallback: COLOR.textLabel     },
  Secondary: { varName: COLOR_VAR.textSecondary,  fallback: COLOR.textSecondary },
  Muted:     { varName: COLOR_VAR.textMuted,      fallback: COLOR.textMuted     },
  Brand:     { varName: COLOR_VAR.brandText,      fallback: BRAND.text          },
  Danger:    { varName: COLOR_VAR.dangerText,     fallback: COLOR.dangerText    },
  Success:   { varName: COLOR_VAR.successText,    fallback: COLOR.successText   },
};

/** weight → Noto Sans KR font style 매핑 */
const WEIGHT_FONT_STYLE: Record<TypographyWeight, string> = {
  Normal: 'Regular',
  Medium: 'Medium',
  Bold:   'Bold',
};

async function createTypographyVariant(
  variant: TypographyVariant,
  weight: TypographyWeight,
  color: TypographyColor,
): Promise<ComponentNode> {
  const { fontSize, fontSizeVar, lineHeight, lineHeightVar } = VARIANT_CONFIG[variant];
  const { varName, fallback } = COLOR_CONFIG[color];

  const comp = createComponent(`Variant=${variant}, Weight=${weight}, Color=${color}`);
  comp.layoutMode = 'HORIZONTAL';
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  const text = await addTextWithVar(
    comp, `${variant} 텍스트 예시`, fontSize,
    varName, fallback, weight === 'Bold', fontSizeVar, 'text',
  );

  /* Medium weight: addTextWithVar는 Bold/Regular만 지원하므로 fontName 직접 교체 */
  if (weight === 'Medium') {
    text.fontName = { family: FONT_FAMILY.sans, style: WEIGHT_FONT_STYLE.Medium };
  }

  await setLineHeightVar(text, lineHeightVar, lineHeight);
  return comp;
}

export async function createTypography(): Promise<ComponentSetNode> {
  const variants: TypographyVariant[] = ['Heading', 'Subheading', 'BodyLg', 'Body', 'BodySm', 'Caption'];
  const colors:   TypographyColor[]   = ['Heading', 'Base', 'Label', 'Secondary', 'Muted', 'Brand', 'Danger', 'Success'];
  const weights:  TypographyWeight[]  = ['Normal', 'Medium', 'Bold'];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const color of colors) {
      for (const weight of weights) {
        components.push(await createTypographyVariant(variant, weight, color));
      }
    }
  }

  /* cols=3: Normal | Medium | Bold 한 행, Variant × Color 조합이 행을 구성
   * 6 variants × 8 colors = 48행 × 3 weights = 144 */
  return combineVariants(components, 'Typography', 3);
}
