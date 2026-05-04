/**
 * @file createCardBenefitSummary.ts
 * @description Figma CardBenefitSummary 컴포넌트 생성.
 * 보유 포인트 + 이번달 혜택을 표시하는 카드.
 * 단일 variant.
 * 컴포넌트 이름: "CardBenefitSummary"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

export async function createCardBenefitSummary(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.effects = [{ type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 }, offset: { x: 0, y: 2 }, radius: 8, spread: 0, visible: true, blendMode: 'NORMAL' }];

  /** 구분선으로 나뉜 섹션 행 생성 */
  async function addSection(label: string, valueText: string, subText?: string): Promise<void> {
    const section = figma.createFrame();
    setAutoLayout(section, 'HORIZONTAL', 0);
    section.primaryAxisAlignItems = 'SPACE_BETWEEN';
    section.counterAxisAlignItems = 'CENTER';
    setPadding(section, SPACING.md, SPACING.md);
    section.layoutAlign = 'STRETCH';
    section.primaryAxisSizingMode = 'FIXED';
    section.counterAxisSizingMode = 'AUTO';
    section.resize(CARD_WIDTH, 1);
    clearFill(section);

    const left = figma.createFrame();
    setAutoLayout(left, 'VERTICAL', SPACING.xs, 'MIN');
    left.primaryAxisSizingMode = 'AUTO';
    left.counterAxisSizingMode = 'AUTO';
    clearFill(left);
    await addTextWithVar(left, label, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    if (subText) {
      await addTextWithVar(left, subText, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    }
    section.appendChild(left);

    const right = figma.createFrame();
    setAutoLayout(right, 'HORIZONTAL', SPACING.xs);
    right.counterAxisAlignItems = 'CENTER';
    right.primaryAxisSizingMode = 'AUTO';
    right.counterAxisSizingMode = 'AUTO';
    clearFill(right);
    await addTextWithVar(right, valueText, FONT_SIZE.sm, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeSm);
    right.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));
    section.appendChild(right);

    comp.appendChild(section);

    /* 구분선 */
    const divider = figma.createRectangle();
    divider.resize(CARD_WIDTH, 1);
    divider.fills = [{ type: 'SOLID', color: COLOR.borderSubtle }];
    comp.appendChild(divider);
  }

  await addSection('보유 포인트', '12,345P');
  await addSection('이번달 혜택', '5,000원', '할인 3,000원 캐시백 2,000원');

  figma.currentPage.appendChild(comp);
  return comp;
}
