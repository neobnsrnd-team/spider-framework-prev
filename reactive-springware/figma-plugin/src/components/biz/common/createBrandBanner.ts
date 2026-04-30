/**
 * @file createBrandBanner.ts
 * @description Figma BrandBanner 컴포넌트 생성.
 * 브랜드 컬러 배경 위에 광고 문구와 아이콘 원형이 배치된 단일 ComponentNode를 반환한다.
 */
import { BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, addText } from '../../../helpers';

export async function createBrandBanner(): Promise<ComponentNode> {
  const comp = createComponent('BrandBanner');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(328, 64);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, BRAND.primary);

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', 2);
  textArea.layoutGrow = 1;
  textArea.fills = [];
  await addText(textArea, '하나은행 광고 문구', FONT_SIZE.xs, BRAND.fg);
  await addText(textArea, '브랜드 배너 제목', FONT_SIZE.sm, BRAND.fg, true);
  comp.appendChild(textArea);

  /* 아이콘 원형 — opacity 0.2로 오버레이해 배경색과 자연스럽게 어우러지도록 */
  const iconCircle = figma.createEllipse();
  iconCircle.resize(40, 40);
  setFill(iconCircle, BRAND.fg, 0.2);
  comp.appendChild(iconCircle);

  figma.currentPage.appendChild(comp);
  return comp;
}
