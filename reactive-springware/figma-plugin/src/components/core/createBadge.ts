/**
 * @file createBadge.ts
 * @description Figma Badge 컴포넌트 세트 생성.
 * React Badge의 variant(primary|brand|success|danger|warning|neutral) × dot(true|false)를
 * Figma variant로 매핑한다.
 *
 * 컴포넌트 이름: "Badge"
 * Variant 형식: "Variant=Brand, Dot=False" / "Variant=Brand, Dot=True"
 *
 * dot은 독립 variant가 아닌 boolean prop으로, 기존 6개 variant 각각에 조합된다.
 * 총 ComponentSet 조합: 6(variant) × 2(dot) = 12개
 *
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없을 경우 tokens.ts의 RGB 값으로 fallback한다.
 */

import { BRAND, COLOR, FONT_SIZE, RADIUS, SPACING, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../helpers';

type BadgeVariant = 'Primary' | 'Brand' | 'Success' | 'Danger' | 'Warning' | 'Neutral';

/**
 * variant별 Figma 변수명 + fallback RGB 쌍.
 * setFillWithVar가 변수 바인딩을 시도하고, 없으면 fallback으로 렌더링한다.
 */
const VARIANT_CONFIG: Record<BadgeVariant, {
  bgVar: string;   bgFallback: Parameters<typeof setFillWithVar>[2];
  textVar: string; textFallback: Parameters<typeof setFillWithVar>[2];
}> = {
  Primary: {
    /* Badge Primary는 color/info/surface — color/primary/surface와 별개 경로 */
    bgVar:        COLOR_VAR.infoSurface,    bgFallback:   COLOR.primarySurface,
    textVar:      COLOR_VAR.primaryText,    textFallback: COLOR.primaryText,
  },
  Brand: {
    bgVar:        COLOR_VAR.brandBg,        bgFallback:   BRAND.bg,
    textVar:      COLOR_VAR.brandText,      textFallback: BRAND.text,
  },
  Success: {
    bgVar:        COLOR_VAR.successSurface, bgFallback:   COLOR.successSurface,
    textVar:      COLOR_VAR.successText,    textFallback: COLOR.successText,
  },
  Danger: {
    bgVar:        COLOR_VAR.dangerSurface,  bgFallback:   COLOR.dangerSurface,
    textVar:      COLOR_VAR.dangerText,     textFallback: COLOR.dangerText,
  },
  Warning: {
    bgVar:        COLOR_VAR.warningSurface, bgFallback:   COLOR.warningSurface,
    textVar:      COLOR_VAR.warningText,    textFallback: COLOR.warningText,
  },
  Neutral: {
    bgVar:        COLOR_VAR.borderSubtle,   bgFallback:   COLOR.surfaceRaised,
    textVar:      COLOR_VAR.textSecondary,  textFallback: COLOR.textSecondary,
  },
};

/**
 * Badge 단일 variant 컴포넌트 생성.
 * @param variant - 색상 variant
 * @param dot     - true이면 텍스트 없는 8×8 원형 인디케이터, false이면 pill + 텍스트
 */
async function createBadgeVariant(variant: BadgeVariant, dot: boolean): Promise<ComponentNode> {
  const { bgVar, bgFallback, textVar, textFallback } = VARIANT_CONFIG[variant];

  /* Figma variant 속성 이름: "Variant=Brand, Dot=True" 형식 */
  const comp = createComponent(`Variant=${variant}, Dot=${dot ? 'True' : 'False'}`);

  if (dot) {
    /* dot=true: 텍스트 없는 8×8 원형 인디케이터
     * React Badge의 size-2(8px) rounded-full에 대응 */
    comp.resize(8, 8);
    await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    comp.layoutMode = 'NONE';
    await setFillWithVar(comp, bgVar, bgFallback);
  } else {
    /* dot=false: 기존 pill + 텍스트 형태
     * py=2, px=8: React의 px-sm py-0.5에 대응 */
    setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
    await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
    setPadding(comp, 2, SPACING.sm);
    /* paddingTop/Bottom = 2px는 디자인 토큰에 없으므로 raw 값 유지 */
    await setFloatVar(comp, 'paddingRight', SIZE_VAR.spacingSm, SPACING.sm);
    await setFloatVar(comp, 'paddingLeft',  SIZE_VAR.spacingSm, SPACING.sm);
    comp.primaryAxisSizingMode = 'AUTO';
    comp.counterAxisSizingMode = 'AUTO';
    await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(comp, bgVar, bgFallback);

    const label = await addTextWithVar(comp, variant, FONT_SIZE.xs, textVar, textFallback, true, SIZE_VAR.fontSizeXs);
    label.textAlignHorizontal = 'CENTER';
  }

  return comp;
}

/** Badge ComponentSet 생성 후 반환 */
export async function createBadge(): Promise<ComponentSetNode> {
  const variants: BadgeVariant[] = ['Primary', 'Brand', 'Success', 'Danger', 'Warning', 'Neutral'];

  const components: ComponentNode[] = [];

  /* Dot=False 행 먼저, Dot=True 행 다음 순서로 생성
   * combineVariants cols=6: 한 행에 variant 6개 → Dot=False 행 / Dot=True 행 2행 구성 */
  for (const dot of [false, true]) {
    for (const variant of variants) {
      components.push(await createBadgeVariant(variant, dot));
    }
  }

  return combineVariants(components, 'Badge', 6);
}
