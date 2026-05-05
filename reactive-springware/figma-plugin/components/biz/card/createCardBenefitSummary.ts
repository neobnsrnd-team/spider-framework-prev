/**
 * @file createCardBenefitSummary.ts
 * @description Figma CardBenefitSummary 컴포넌트 생성.
 * 보유 포인트 + 이번달 혜택을 표시하는 카드.
 * 단일 variant.
 *
 * ── CardBenefitSummary/BenefitItem ───────────────────────────
 * TEXT properties:
 *   - label  — 혜택 레이블 (기본값: '할인')
 *   - amount — 혜택 금액   (기본값: '3,000원')
 *
 * ── CardBenefitSummary ───────────────────────────────────────
 * TEXT properties:
 *   - points       — 보유 포인트 (기본값: '12,345P')
 *   - totalBenefit — 이번달 혜택 합계 (기본값: '5,000원')
 *
 * [레이아웃]
 *   comp(VERTICAL, gap=0, FIXED 390×AUTO, radiusLg, surface, borderSubtle, shadow)
 *     topSection(HORIZONTAL, SPACE_BETWEEN, CENTER, px-md py-md, FIXED 390)
 *       "보유 포인트"(TEXT xs, textMuted)
 *       pointsRight(HORIZONTAL, gap=xs)
 *         points(TEXT sm bold, brandText) ← 수동 바인딩
 *         ChevronRight(14px, textMuted)
 *     divider(1px, 390, borderSubtle)
 *     benefitHeader(HORIZONTAL, SPACE_BETWEEN, CENTER, px-md pt-md pb-0, FIXED 390)
 *       "이번달 혜택"(TEXT xs, textMuted)
 *       benefitRight(HORIZONTAL, gap=xs)
 *         totalBenefit(TEXT sm bold, textHeading) ← 수동 바인딩
 *         ChevronRight(14px, textMuted)
 *     benefitsSlot(SlotNode, HORIZONTAL WRAP, gap=sm, px-md pt-xs pb-md)
 *       BenefitItem × 2 (기본 인스턴스)
 *
 * TEXT property 바인딩 타이밍:
 *   points       — comp → topSection → pointsRight → text: 수동 바인딩
 *   totalBenefit — comp → benefitHeader → benefitRight → text: 수동 바인딩
 *
 * 컴포넌트 이름: "CardBenefitSummary", "CardBenefitSummary/BenefitItem"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH = 390;

/* ── CardBenefitSummary/BenefitItem ─────────────────────────── */

export async function createCardBenefitSummaryBenefitItem(): Promise<ComponentNode> {
  const comp = createComponent('CardBenefitSummary/BenefitItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.primaryAxisAlignItems = 'MIN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(1, 1);                   /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* label / amount — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '할인', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'label');
  await addTextWithVar(comp, '3,000원', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'amount');

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── CardBenefitSummary ─────────────────────────────────────── */

/**
 * @param benefitItem - createCardBenefitSummaryBenefitItem()이 반환한 ComponentNode.
 *                      benefits 슬롯의 기본 인스턴스 배치에 사용한다.
 */
export async function createCardBenefitSummary(benefitItem: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('CardBenefitSummary');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign = 'INSIDE';
  comp.effects = [{
    type: 'DROP_SHADOW',
    color: { r: 0, g: 0, b: 0, a: 0.08 },
    offset: { x: 0, y: 2 },
    radius: 8, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* ── 상단: 보유 포인트 ── */
  const topSection = figma.createFrame();
  setAutoLayout(topSection, 'HORIZONTAL', 0);
  topSection.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topSection.counterAxisAlignItems = 'CENTER';
  setPadding(topSection, SPACING.md, SPACING.md);
  topSection.resize(CARD_WIDTH, 1);    /* resize 먼저 */
  topSection.primaryAxisSizingMode = 'FIXED';
  topSection.counterAxisSizingMode = 'AUTO';
  clearFill(topSection);
  comp.appendChild(topSection);

  await addTextWithVar(topSection, '보유 포인트', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  const pointsRight = figma.createFrame();
  setAutoLayout(pointsRight, 'HORIZONTAL', SPACING.xs);
  pointsRight.primaryAxisAlignItems = 'MIN';
  pointsRight.counterAxisAlignItems = 'CENTER';
  pointsRight.resize(1, 1);            /* resize 먼저 */
  pointsRight.primaryAxisSizingMode = 'AUTO';
  pointsRight.counterAxisSizingMode = 'AUTO';
  clearFill(pointsRight);
  topSection.appendChild(pointsRight);

  const pointsValueNode = await addTextWithVar(pointsRight, '12,345P', FONT_SIZE.sm, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeSm);
  const pointsKey = comp.addComponentProperty('points', 'TEXT', '12,345P');
  pointsValueNode.componentPropertyReferences = { characters: pointsKey };
  pointsRight.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));

  /* ── 구분선 ── */
  const divider = figma.createRectangle();
  divider.resize(CARD_WIDTH, 1);
  divider.fills = [{ type: 'SOLID', color: COLOR.borderSubtle }];
  comp.appendChild(divider);

  /* ── 하단 헤더: 이번달 혜택 ── */
  const benefitHeader = figma.createFrame();
  setAutoLayout(benefitHeader, 'HORIZONTAL', 0);
  benefitHeader.primaryAxisAlignItems = 'SPACE_BETWEEN';
  benefitHeader.counterAxisAlignItems = 'CENTER';
  /* pt-md px-md pb-0: 하단 padding은 slot의 pt-xs가 gap 역할 */
  setPadding(benefitHeader, SPACING.md, SPACING.md, 0, SPACING.md);
  benefitHeader.resize(CARD_WIDTH, 1); /* resize 먼저 */
  benefitHeader.primaryAxisSizingMode = 'FIXED';
  benefitHeader.counterAxisSizingMode = 'AUTO';
  clearFill(benefitHeader);
  comp.appendChild(benefitHeader);

  await addTextWithVar(benefitHeader, '이번달 혜택', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  const benefitRight = figma.createFrame();
  setAutoLayout(benefitRight, 'HORIZONTAL', SPACING.xs);
  benefitRight.primaryAxisAlignItems = 'MIN';
  benefitRight.counterAxisAlignItems = 'CENTER';
  benefitRight.resize(1, 1);           /* resize 먼저 */
  benefitRight.primaryAxisSizingMode = 'AUTO';
  benefitRight.counterAxisSizingMode = 'AUTO';
  clearFill(benefitRight);
  benefitHeader.appendChild(benefitRight);

  const totalValueNode = await addTextWithVar(benefitRight, '5,000원', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
  const totalKey = comp.addComponentProperty('totalBenefit', 'TEXT', '5,000원');
  totalValueNode.componentPropertyReferences = { characters: totalKey };
  benefitRight.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));

  /* ── 혜택 항목 슬롯 — BenefitItem 인스턴스를 추가할 수 있는 SlotNode ── */
  /* createSlot()은 자동으로 comp의 마지막 자식으로 추가된다 */
  const slot = comp.createSlot();
  slot.name = 'Benefits';
  slot.layoutMode = 'HORIZONTAL';
  slot.layoutWrap = 'WRAP';
  slot.itemSpacing = SPACING.sm;
  slot.counterAxisSpacing = SPACING.sm;
  slot.paddingTop = SPACING.xs;    /* benefitHeader와의 간격 */
  slot.paddingBottom = SPACING.md;
  slot.paddingLeft = SPACING.md;
  slot.paddingRight = SPACING.md;
  slot.resize(CARD_WIDTH, 1);
  slot.primaryAxisSizingMode = 'FIXED'; /* width = CARD_WIDTH 고정 */
  slot.counterAxisSizingMode = 'AUTO';  /* height는 콘텐츠에 맞게 */
  clearFill(slot);

  /* 기본 BenefitItem 인스턴스 2개 배치 */
  slot.appendChild(benefitItem.createInstance());
  slot.appendChild(benefitItem.createInstance());

  figma.currentPage.appendChild(comp);
  return comp;
}
