/**
 * @file createBannerCarousel.ts
 * @description Figma BannerCarousel 컴포넌트 세트 생성.
 * Promo / Info / Warning 3가지 variant를 가진 ComponentSet을 반환한다.
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, addText } from '../../../helpers';

type BannerVariant = 'Promo' | 'Info' | 'Warning';

async function createBannerCarouselVariant(variant: BannerVariant): Promise<ComponentNode> {
  const BG_CONFIG: Record<BannerVariant, Parameters<typeof setFill>[1]> = {
    Promo:   BRAND.primary,
    Info:    COLOR.surfaceRaised,
    Warning: COLOR.warningSurface,
  };
  const TEXT_CONFIG: Record<BannerVariant, Parameters<typeof setFill>[1]> = {
    Promo:   BRAND.fg,
    Info:    COLOR.textHeading,
    Warning: COLOR.warningText,
  };

  const comp = createComponent(`Variant=${variant}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(328, 80);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, BG_CONFIG[variant]);

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', 4);
  textArea.layoutGrow = 1;
  textArea.fills = [];
  await addText(textArea, '배너 제목', FONT_SIZE.sm, TEXT_CONFIG[variant], true);
  await addText(textArea, '배너 설명 텍스트입니다.', FONT_SIZE.xs, TEXT_CONFIG[variant]);
  comp.appendChild(textArea);

  return comp;
}

export async function createBannerCarousel(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all(['Promo', 'Info', 'Warning'].map((v) => createBannerCarouselVariant(v as BannerVariant))),
    'BannerCarousel', 3,
  );
}
