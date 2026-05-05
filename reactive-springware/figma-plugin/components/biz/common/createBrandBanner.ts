/**
 * @file createBrandBanner.ts
 * @description Figma BrandBanner 컴포넌트 생성.
 * 브랜드 컬러 배경 배너. 좌측 subtitle/title 텍스트, 우측 교체 가능한 아이콘.
 *
 * TEXT properties:
 *   - subtitle — 소제목 (기본값: '개인 맞춤 혜택')
 *   - title    — 주요 타이틀 (기본값: '나만을 위한 특별한 혜택')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 우측 원형 컨테이너 아이콘 (기본값: Star, swap 가능)
 *
 * [레이아웃 — 실제 컴포넌트 기준]
 *   comp(HORIZONTAL, gap=md, padding=md, FIXED 390×64, radiusXl, brandPrimary bg)
 *     textArea(VERTICAL, gap=xs, grow=1)
 *       subtitle(TEXT xs, brandFg, opacity=0.8)
 *       title(TEXT sm, bold, brandFg)
 *     iconBox(40×40, radiusFull, white/20% bg)
 *       icon(INSTANCE_SWAP, 20px, brandFg)
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textArea) 이후 수동 바인딩 (2단계: comp → textArea → text)
 *
 * 컴포넌트 이름: "BrandBanner"
 */
import { BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

export async function createBrandBanner(): Promise<ComponentNode> {
  const comp = createComponent('BrandBanner');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(390, 64);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);

  /* 좌측 텍스트 영역 — comp.appendChild 후 하위 바인딩 가능 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs);
  clearFill(textArea);
  comp.appendChild(textArea);
  textArea.layoutGrow = 1; /* 아이콘을 우측으로 밀어냄 */

  /* subtitle — 2단계 수동 바인딩, 실제 컴포넌트의 opacity-80 재현 */
  const subtitleNode = await addTextWithVar(
    textArea, '개인 맞춤 혜택', FONT_SIZE.xs,
    COLOR_VAR.brandFg, BRAND.fg, false, SIZE_VAR.fontSizeXs,
  );
  subtitleNode.opacity = 0.8;
  subtitleNode.textAlignHorizontal = 'LEFT';
  subtitleNode.layoutSizingHorizontal = 'FILL';
  const subtitleKey = comp.addComponentProperty('subtitle', 'TEXT', '개인 맞춤 혜택');
  subtitleNode.componentPropertyReferences = { characters: subtitleKey };

  /* title — 2단계 수동 바인딩 */
  const titleNode = await addTextWithVar(
    textArea, '나만을 위한 특별한 혜택', FONT_SIZE.sm,
    COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeSm,
  );
  titleNode.textAlignHorizontal = 'LEFT';
  titleNode.layoutSizingHorizontal = 'FILL';
  const titleKey = comp.addComponentProperty('title', 'TEXT', '나만을 위한 특별한 혜택');
  titleNode.componentPropertyReferences = { characters: titleKey };

  /* 우측 아이콘 컨테이너 — 흰색 20% 반투명 원형 (Figma 원본 재현) */
  const iconBox = figma.createFrame();
  setAutoLayout(iconBox, 'HORIZONTAL', 0);
  iconBox.resize(40, 40);
  iconBox.primaryAxisSizingMode = 'FIXED';
  iconBox.counterAxisSizingMode = 'FIXED';
  iconBox.primaryAxisAlignItems = 'CENTER';
  iconBox.counterAxisAlignItems = 'CENTER';
  await setFloatVar(iconBox, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  /* brandFg(#fff) opacity 0.2 로 bg-white/20 재현 */
  iconBox.fills = [{ type: 'SOLID', color: BRAND.fg, opacity: 0.2 }];
  comp.appendChild(iconBox);

  /* INSTANCE_SWAP: icon (기본값 Star, Figma 패널에서 교체 가능) */
  addIconSlot(comp, 'Star', 20, BRAND.fg, 'icon', iconBox);

  figma.currentPage.appendChild(comp);
  return comp;
}
