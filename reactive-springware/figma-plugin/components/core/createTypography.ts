/**
 * @file createTypography.ts
 * @description Figma Typography 컴포넌트 세트 생성.
 * React Text의 variant(heading|subheading|body-lg|body|body-sm|caption)를 Figma variant로 매핑한다.
 * 컴포넌트 이름: "Typography" / Variant 형식: "Variant=Heading, Weight=Bold"
 *
 * Weight: Normal(400) | Medium(500) | Bold(700) — component-library의 font-normal/medium/bold에 대응
 */
import { COLOR, FONT_FAMILY, FONT_SIZE, LINE_HEIGHT, COLOR_VAR, SIZE_VAR } from '../../tokens';
import { createComponent, combineVariants, clearFill, addTextWithVar, setLineHeightVar } from '../../helpers';

type TypographyVariant = 'Heading' | 'Subheading' | 'BodyLg' | 'Body' | 'BodySm' | 'Caption';
type TypographyWeight  = 'Normal' | 'Medium' | 'Bold';

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

/** weight → Noto Sans KR font style 문자열 매핑 */
const WEIGHT_FONT_STYLE: Record<TypographyWeight, string> = {
  Normal: 'Regular',
  Medium: 'Medium',
  Bold:   'Bold',
};

async function createTypographyVariant(variant: TypographyVariant, weight: TypographyWeight): Promise<ComponentNode> {
  const { fontSize, fontSizeVar, lineHeight, lineHeightVar } = VARIANT_CONFIG[variant];
  const comp = createComponent(`Variant=${variant}, Weight=${weight}`);
  comp.layoutMode = 'HORIZONTAL';
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* Bold=true로 먼저 생성 후 Medium은 fontName을 직접 교체 */
  const text = await addTextWithVar(
    comp, `${variant} 텍스트 예시`, fontSize,
    COLOR_VAR.textHeading, COLOR.textHeading, weight === 'Bold', fontSizeVar,
  );

  /* Medium weight: addTextWithVar는 Bold/Regular만 지원하므로 fontName 재설정 */
  if (weight === 'Medium') {
    text.fontName = { family: FONT_FAMILY.sans, style: WEIGHT_FONT_STYLE.Medium };
  }

  await setLineHeightVar(text, lineHeightVar, lineHeight);
  return comp;
}

export async function createTypography(): Promise<ComponentSetNode> {
  const variants: TypographyVariant[] = ['Heading', 'Subheading', 'BodyLg', 'Body', 'BodySm', 'Caption'];
  const weights: TypographyWeight[]   = ['Normal', 'Medium', 'Bold'];
  const components: ComponentNode[] = [];
  for (const variant of variants) {
    for (const weight of weights) {
      components.push(await createTypographyVariant(variant, weight));
    }
  }
  /* cols=3: Normal | Medium | Bold를 한 행에 나란히 (6 variants × 3 weights = 18개) */
  return combineVariants(components, 'Typography', 3);
}
