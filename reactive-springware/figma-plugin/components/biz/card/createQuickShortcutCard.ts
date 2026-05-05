/**
 * @file createQuickShortcutCard.ts
 * @description Figma QuickShortcutCard 컴포넌트 생성.
 * 카드 도메인 빠른 바로가기 소형 카드.
 * 타이틀 + 서브텍스트 + 우측 아이콘. 단일 variant.
 *
 * TEXT properties:
 *   - title    — 카드 제목   (기본값: '내 쿠폰')
 *   - subtitle — 서브 텍스트 (기본값: '3장 사용가능')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 아이콘 (기본값: Ticket)
 *
 * 컴포넌트 이름: "QuickShortcutCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

export async function createQuickShortcutCard(): Promise<ComponentNode> {
  const comp = createComponent('QuickShortcutCard');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(185, 1);                  /* 2열 그리드 기준 절반 너비 */
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign  = 'INSIDE';
  comp.effects = [{
    type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.06 },
    offset: { x: 0, y: 1 }, radius: 4, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* 텍스트 영역 (좌측) */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.primaryAxisAlignItems = 'MIN';
  textArea.layoutGrow = 1;
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'AUTO';
  clearFill(textArea);
  /* textArea를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(textArea);
  await addTextWithVar(textArea, '내 쿠폰', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'title', comp);
  await addTextWithVar(textArea, '3장 사용가능', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs, 'subtitle', comp);

  /* icon — INSTANCE_SWAP, comp 직접 자식으로 등록 */
  addIconSlot(comp, 'Ticket', 20, COLOR.textMuted, 'icon');

  figma.currentPage.appendChild(comp);
  return comp;
}
