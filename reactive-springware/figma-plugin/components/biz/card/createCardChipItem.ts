/**
 * @file createCardChipItem.ts
 * @description Figma CardChipItem 컴포넌트 생성.
 * CreditCard 아이콘 원형 배경 + 카드명 + 마스킹 카드번호.
 * 단일 variant.
 *
 * TEXT properties:
 *   - name         — 카드명 (기본값: '하나 머니 체크카드')
 *   - maskedNumber — 마스킹된 카드번호 (기본값: '1234-****-****-5678')
 *
 * 컴포넌트 이름: "CardChipItem"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createCardChipItem(): Promise<ComponentNode> {
  const comp = createComponent('CardChipItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.sm, SPACING.sm);
  comp.counterAxisAlignItems = 'CENTER';
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* CreditCard 아이콘 원형 (32×32) */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.resize(32, 32);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  iconCircle.appendChild(createIcon('CreditCard', 16, BRAND.primary));
  comp.appendChild(iconCircle);

  /* 카드명 + 마스킹 카드번호 */
  const info = figma.createFrame();
  setAutoLayout(info, 'VERTICAL', SPACING.xs, 'MIN');
  info.primaryAxisSizingMode = 'AUTO';
  info.counterAxisSizingMode = 'AUTO';
  clearFill(info);

  /* info를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(info);
  await addTextWithVar(info, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'name', comp);
  await addTextWithVar(info, '1234-****-****-5678', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'maskedNumber', comp);

  figma.currentPage.appendChild(comp);
  return comp;
}
