/**
 * @file createCardVisual.ts
 * @description Figma CardVisual 컴포넌트 세트 생성.
 * 카드 이미지 + 브랜드 + 카드명 표시 컴포넌트.
 * Mode(Full|Compact) = 2 variants.
 * - Full: 카드 이미지(16:10) + 브랜드 + 카드명 세로 배치
 * - Compact: 브랜드 + 카드명 한 줄 (스티키 헤더용)
 * 컴포넌트 이름: "CardVisual"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

const CARD_W = 260;  /* max-w-[260px] */
const CARD_H = 163;  /* 16:10 비율 → 260 × 10/16 ≈ 163 */

/** Full variant: 카드 이미지 + 브랜드 + 카드명 */
async function createFullVariant(): Promise<ComponentNode> {
  const comp = createComponent('Mode=Full');
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 카드 이미지 플레이스홀더 (16:10 비율) */
  const imgBox = figma.createFrame();
  imgBox.resize(CARD_W, CARD_H);
  imgBox.layoutMode = 'NONE';
  await setFloatVar(imgBox, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(imgBox, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  /* 그림자 재현 — Figma effect로 추가 */
  imgBox.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.15 }, offset: { x: 0, y: 4 }, radius: 12, spread: 0, visible: true, blendMode: 'NORMAL' }];
  comp.appendChild(imgBox);

  /* 브랜드 + 카드명 */
  const info = figma.createFrame();
  setAutoLayout(info, 'VERTICAL', SPACING.xs);
  info.counterAxisAlignItems = 'CENTER';
  info.primaryAxisSizingMode = 'AUTO';
  info.counterAxisSizingMode = 'AUTO';
  clearFill(info);

  /* info를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(info);
  await addTextWithVar(info, 'VISA', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, true, SIZE_VAR.fontSizeXs, 'brandLabel', comp);
  await addTextWithVar(info, '하나 머니 체크카드', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase, 'cardName', comp);

  return comp;
}

/** Compact variant: 브랜드 + 카드명 한 줄 */
async function createCompactVariant(): Promise<ComponentNode> {
  const comp = createComponent('Mode=Compact');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.counterAxisAlignItems = 'CENTER';
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  await addTextWithVar(comp, 'VISA', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, true, SIZE_VAR.fontSizeXs, 'brandLabel');
  await addTextWithVar(comp, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName');

  return comp;
}

export async function createCardVisual(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createFullVariant(), await createCompactVariant()],
    'CardVisual', 2,
  );
}
