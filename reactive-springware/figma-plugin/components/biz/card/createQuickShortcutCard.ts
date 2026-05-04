/**
 * @file createQuickShortcutCard.ts
 * @description Figma QuickShortcutCard 컴포넌트 생성.
 * 카드 도메인 빠른 바로가기 소형 카드.
 * 타이틀 + 서브텍스트 + 우측 아이콘.
 * 단일 variant.
 * 컴포넌트 이름: "QuickShortcutCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createQuickShortcutCard(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(185, 1);  /* 2열 그리드 기준 절반 너비 */
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.06 }, offset: { x: 0, y: 1 }, radius: 4, spread: 0, visible: true, blendMode: 'NORMAL' }];

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.layoutGrow = 1;
  clearFill(textArea);
  await addTextWithVar(textArea, '내 쿠폰', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  await addTextWithVar(textArea, '3장 사용가능', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs);
  comp.appendChild(textArea);

  comp.appendChild(createIcon('Ticket', 20, COLOR.textMuted));

  figma.currentPage.appendChild(comp);
  return comp;
}
