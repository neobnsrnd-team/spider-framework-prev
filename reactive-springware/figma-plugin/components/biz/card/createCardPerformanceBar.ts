/**
 * @file createCardPerformanceBar.ts
 * @description Figma CardPerformanceBar 컴포넌트 세트 생성.
 * 카드 이용실적 진행 바 컴포넌트.
 * State(InProgress|Achieved) = 2 variants.
 * - InProgress: 50% 달성 → 하단 "{targetAmount}-{currentAmount}원을 더 이용하면 실적 달성 ({benefitDescription})"
 * - Achieved:   100% 달성 → 하단 "{benefitDescription}" 단독 표시
 *
 * TEXT properties:
 *   - cardName           — 카드명          (기본값: '하나 머니 체크카드')
 *   - currentAmount      — 현재 이용금액 숫자만 (기본값: InProgress '150,000' / Achieved '300,000')  + 고정 '원'
 *   - targetAmount       — 목표 금액 숫자만    (기본값: '300,000')  + 고정 '원'
 *   - benefitDescription — 혜택 설명         (기본값: InProgress '항공 마일리지 5,000원 적립' / Achieved '전월 실적 달성 완료')
 *
 * 컴포넌트 이름: "CardPerformanceBar"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addRect,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING.md * 2;
const BAR_HEIGHT    = 8;

async function createPerformanceVariant(state: 'InProgress' | 'Achieved'): Promise<ComponentNode> {
  const isAchieved = state === 'Achieved';
  const percent    = isAchieved ? 100 : 50;

  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(CARD_WIDTH, 1);           /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign  = 'INSIDE';

  /* ── 상단: 카드명 + 실적 상세 링크 ── */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', 0);
  topRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topRow.counterAxisAlignItems = 'CENTER';
  topRow.resize(CONTENT_WIDTH, 1);      /* resize 먼저 */
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';
  clearFill(topRow);
  comp.appendChild(topRow);

  /* cardName — topRow → comp: 수동 바인딩 */
  await addTextWithVar(topRow, '하나 머니 체크카드', FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXs, 'cardName', comp);

  /* 실적 상세 링크 — 고정 텍스트 */
  const detailRow = figma.createFrame();
  setAutoLayout(detailRow, 'HORIZONTAL', SPACING.xs);
  detailRow.primaryAxisAlignItems = 'MIN';
  detailRow.counterAxisAlignItems = 'CENTER';
  detailRow.resize(1, 1);               /* resize 먼저 */
  detailRow.primaryAxisSizingMode = 'AUTO';
  detailRow.counterAxisSizingMode = 'AUTO';
  clearFill(detailRow);
  topRow.appendChild(detailRow);
  await addTextWithVar(detailRow, '실적 상세', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  detailRow.appendChild(createIcon('ChevronRight', 14, COLOR.textMuted));

  /* ── 이용금액 / 목표금액 행 ── */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', 0);
  amountRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  amountRow.counterAxisAlignItems = 'CENTER';
  amountRow.resize(CONTENT_WIDTH, 1);   /* resize 먼저 */
  amountRow.primaryAxisSizingMode = 'FIXED';
  amountRow.counterAxisSizingMode = 'AUTO';
  clearFill(amountRow);
  comp.appendChild(amountRow);

  /* 현재 이용금액 — 숫자(bound) + 고정 '원' */
  const currentFrame = figma.createFrame();
  setAutoLayout(currentFrame, 'HORIZONTAL', 0);
  currentFrame.primaryAxisAlignItems = 'MIN';
  currentFrame.counterAxisAlignItems = 'CENTER';
  currentFrame.resize(1, 1);            /* resize 먼저 */
  currentFrame.primaryAxisSizingMode = 'AUTO';
  currentFrame.counterAxisSizingMode = 'AUTO';
  clearFill(currentFrame);
  amountRow.appendChild(currentFrame);

  const currentAmountDefault = isAchieved ? '300,000' : '150,000';
  /* currentAmount — currentFrame → amountRow → comp: 수동 바인딩 */
  await addTextWithVar(
    currentFrame, currentAmountDefault, FONT_SIZE.sm,
    isAchieved ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isAchieved ? BRAND.text          : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'currentAmount', comp,
  );
  /* 고정 '원' */
  await addTextWithVar(
    currentFrame, '원', FONT_SIZE.sm,
    isAchieved ? COLOR_VAR.brandText : COLOR_VAR.textHeading,
    isAchieved ? BRAND.text          : COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );

  /* 목표금액 — '목표 ' + 숫자(bound) + 고정 '원' */
  const targetFrame = figma.createFrame();
  setAutoLayout(targetFrame, 'HORIZONTAL', 0);
  targetFrame.primaryAxisAlignItems = 'MIN';
  targetFrame.counterAxisAlignItems = 'CENTER';
  targetFrame.resize(1, 1);             /* resize 먼저 */
  targetFrame.primaryAxisSizingMode = 'AUTO';
  targetFrame.counterAxisSizingMode = 'AUTO';
  clearFill(targetFrame);
  amountRow.appendChild(targetFrame);

  /* 고정 '목표 ' */
  await addTextWithVar(targetFrame, '목표 ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  /* targetAmount — targetFrame → amountRow → comp: 수동 바인딩 */
  await addTextWithVar(targetFrame, '300,000', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'targetAmount', comp);
  /* 고정 '원' */
  await addTextWithVar(targetFrame, '원', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* ── 진행 바 ── */
  const barBg = figma.createFrame();
  barBg.resize(CONTENT_WIDTH, BAR_HEIGHT);
  barBg.layoutMode   = 'NONE';
  barBg.cornerRadius = RADIUS.full;
  await setFillWithVar(barBg, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  const fill = addRect(barBg, Math.round(CONTENT_WIDTH * percent / 100), BAR_HEIGHT, BRAND.primary, RADIUS.full);
  fill.x = 0; fill.y = 0;
  comp.appendChild(barBg);

  /* ── 하단 메시지 ── */
  if (isAchieved) {
    /* Achieved: benefitDescription 단독 — comp 직접 자식: 자동 바인딩 */
    await addTextWithVar(comp, '전월 실적 달성 완료', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeXs, 'benefitDescription');

  } else {
    /* InProgress: {targetAmount}-{currentAmount}원을 더 이용하면 실적 달성 ({benefitDescription}) */
    const statusRow = figma.createFrame();
    setAutoLayout(statusRow, 'HORIZONTAL', 0);
    statusRow.primaryAxisAlignItems = 'MIN';
    statusRow.counterAxisAlignItems = 'CENTER';
    statusRow.resize(1, 1);             /* resize 먼저 */
    statusRow.primaryAxisSizingMode = 'AUTO';
    statusRow.counterAxisSizingMode = 'AUTO';
    clearFill(statusRow);
    comp.appendChild(statusRow);

    /* targetAmount 재사용 — 등록된 property key 조회 후 바인딩 */
    const defs = comp.componentPropertyDefinitions;
    const targetKey  = Object.keys(defs).find(k => k.startsWith('targetAmount'))  ?? '';
    const currentKey = Object.keys(defs).find(k => k.startsWith('currentAmount')) ?? '';

    const statusTargetNode = await addTextWithVar(statusRow, '300,000', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    if (targetKey)  statusTargetNode.componentPropertyReferences  = { characters: targetKey };

    await addTextWithVar(statusRow, '-', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

    const statusCurrentNode = await addTextWithVar(statusRow, currentAmountDefault, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    if (currentKey) statusCurrentNode.componentPropertyReferences = { characters: currentKey };

    await addTextWithVar(statusRow, '원을 더 이용하면 실적 달성 (', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

    /* benefitDescription — statusRow → comp: 수동 바인딩 */
    await addTextWithVar(statusRow, '항공 마일리지 5,000원 적립', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'benefitDescription', comp);

    await addTextWithVar(statusRow, ')', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  }

  return comp;
}

export async function createCardPerformanceBar(): Promise<ComponentSetNode> {
  return combineVariants(
    [await createPerformanceVariant('InProgress'), await createPerformanceVariant('Achieved')],
    'CardPerformanceBar', 1,
  );
}
