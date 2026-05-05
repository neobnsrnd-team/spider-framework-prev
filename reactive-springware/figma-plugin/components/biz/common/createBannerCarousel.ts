/**
 * @file createBannerCarousel.ts
 * @description Figma BannerCarousel 컴포넌트 세트 생성.
 * Promo / Info / Warning 3가지 variant를 가진 ComponentSet을 반환한다.
 *
 * TEXT properties:
 *   - title       — 배너 제목 (기본값: '배너 제목')
 *   - description — 배너 설명 (기본값: '배너 설명 텍스트입니다.')
 *
 * [색상 구성 — 실제 컴포넌트 기준]
 *   Promo:   brandPrimary 배경 / brandFg 텍스트 (테두리 없음)
 *   Info:    surfaceRaised 배경 / textHeading 텍스트 / border 테두리
 *   Warning: warningSurface 배경 / warningText 텍스트 / warningBorder 테두리
 *
 * [레이아웃]
 *   comp(HORIZONTAL, gap=standard, padding=lg, FIXED 390×80, radiusXl)
 *     textArea(VERTICAL, gap=xs, grow=1)
 *       title(TEXT sm, bold)
 *       description(TEXT xs, opacity=0.8)
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textArea) 이후 수동 바인딩 (2단계: comp → textArea → text)
 *
 * 컴포넌트 이름: "BannerCarousel"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

type BannerVariant = 'Promo' | 'Info' | 'Warning';

type RGB = { r: number; g: number; b: number };

type VariantConfig = {
  bgVar:      string; bgFallback:     RGB;
  textVar:    string; textFallback:   RGB;
  borderVar?: string; borderFallback?: RGB;
};

const VARIANT_CONFIG: Record<BannerVariant, VariantConfig> = {
  Promo: {
    bgVar:   COLOR_VAR.brandPrimary, bgFallback:   BRAND.primary,
    textVar: COLOR_VAR.brandFg,      textFallback: BRAND.fg,
  },
  Info: {
    bgVar:      COLOR_VAR.surfaceRaised, bgFallback:     COLOR.surfaceRaised,
    textVar:    COLOR_VAR.textHeading,   textFallback:   COLOR.textHeading,
    borderVar:  COLOR_VAR.border,        borderFallback: COLOR.border,
  },
  Warning: {
    bgVar:      COLOR_VAR.warningSurface,  bgFallback:     COLOR.warningSurface,
    textVar:    COLOR_VAR.warningText,     textFallback:   COLOR.warningText,
    borderVar:  COLOR_VAR.warningBorder,   borderFallback: COLOR.warningBorder,
  },
};

async function createBannerCarouselVariant(variant: BannerVariant): Promise<ComponentNode> {
  const cfg = VARIANT_CONFIG[variant];

  const comp = createComponent(`Variant=${variant}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.standard);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(390, 80);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, cfg.bgVar, cfg.bgFallback);

  if (cfg.borderVar && cfg.borderFallback) {
    await setStrokeWithVar(comp, cfg.borderVar, cfg.borderFallback);
    comp.strokeWeight = 1;
    comp.strokeAlign = 'INSIDE';
  }

  /* 텍스트 영역 — comp.appendChild 후 하위 바인딩 가능 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs);
  clearFill(textArea);
  comp.appendChild(textArea);
  textArea.layoutGrow = 1;

  /* title — 2단계 수동 바인딩 (comp → textArea → text) */
  const titleNode = await addTextWithVar(
    textArea, '배너 제목', FONT_SIZE.sm,
    cfg.textVar, cfg.textFallback, true, SIZE_VAR.fontSizeSm,
  );
  titleNode.textAlignHorizontal = 'LEFT';
  titleNode.layoutSizingHorizontal = 'FILL';
  const titleKey = comp.addComponentProperty('title', 'TEXT', '배너 제목');
  titleNode.componentPropertyReferences = { characters: titleKey };

  /* description — 동일 2단계 바인딩, 실제 컴포넌트의 opacity-80 재현 */
  const descNode = await addTextWithVar(
    textArea, '배너 설명 텍스트입니다.', FONT_SIZE.xs,
    cfg.textVar, cfg.textFallback, false, SIZE_VAR.fontSizeXs,
  );
  descNode.opacity = 0.8;
  descNode.textAlignHorizontal = 'LEFT';
  descNode.layoutSizingHorizontal = 'FILL';
  const descKey = comp.addComponentProperty('description', 'TEXT', '배너 설명 텍스트입니다.');
  descNode.componentPropertyReferences = { characters: descKey };

  return comp;
}

export async function createBannerCarousel(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all(['Promo', 'Info', 'Warning'].map((v) => createBannerCarouselVariant(v as BannerVariant))),
    'BannerCarousel', 3,
  );
}
