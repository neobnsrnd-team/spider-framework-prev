/**
 * @file createInsuranceSummaryCard.ts
 * @description Figma InsuranceSummaryCard 컴포넌트 세트 생성.
 * 보험 도메인 요약 카드.
 * Status(Active|Pending|Expired) = 3 variants.
 * - Active:  유효 보험 — 초록 상태 배지
 * - Pending: 대기 중   — 노랑(경고) 상태 배지
 * - Expired: 만료      — 회색 상태 배지
 * 컴포넌트 이름: "InsuranceSummaryCard"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const CARD_WIDTH = 390;

const STATUS_CONFIG = {
  Active: {
    label: '유효',
    badgeFillVar: COLOR_VAR.successSurface,
    badgeFill:    COLOR.successSurface,
    badgeTextVar: COLOR_VAR.successText,
    badgeText:    COLOR.successText,
    statusIcon:   'CheckCircle2' as const,
  },
  Pending: {
    label: '대기 중',
    badgeFillVar: COLOR_VAR.warningSurface,
    badgeFill:    COLOR.warningSurface,
    badgeTextVar: COLOR_VAR.warningText,
    badgeText:    COLOR.warningText,
    statusIcon:   'AlertTriangle' as const,
  },
  Expired: {
    label: '만료',
    badgeFillVar: COLOR_VAR.surfaceRaised,
    badgeFill:    COLOR.surfaceRaised,
    badgeTextVar: COLOR_VAR.textMuted,
    badgeText:    COLOR.textMuted,
    statusIcon:   'AlertCircle' as const,
  },
} as const;

async function createInsuranceVariant(status: 'Active' | 'Pending' | 'Expired'): Promise<ComponentNode> {
  const cfg = STATUS_CONFIG[status];

  const comp = createComponent(`Status=${status}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'MIN');
  setPadding(comp, SPACING.xl, SPACING.xl);
  comp.resize(CARD_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);

  /* 상단 행: 보험명 + 상태 배지 */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', SPACING.sm);
  topRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topRow.counterAxisAlignItems = 'CENTER';
  topRow.layoutAlign = 'STRETCH';
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';
  topRow.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  clearFill(topRow);

  const nameText = await addTextWithVar(topRow, '하나 건강보험', FONT_SIZE.lg, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeLg);
  nameText.layoutGrow = 1;

  /* 상태 배지 */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', SPACING.xs);
  setPadding(badge, SPACING.xs, SPACING.sm);
  badge.counterAxisAlignItems = 'CENTER';
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, cfg.badgeFillVar, cfg.badgeFill);
  badge.appendChild(createIcon(cfg.statusIcon, 12, cfg.badgeText));
  await addTextWithVar(badge, cfg.label, FONT_SIZE.xs, cfg.badgeTextVar, cfg.badgeText, false, SIZE_VAR.fontSizeXs);
  topRow.appendChild(badge);
  comp.appendChild(topRow);

  /* 증권번호 */
  await addTextWithVar(comp, '증권번호 · 2024-001234-56', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* 구분선 */
  const divider = figma.createFrame();
  divider.layoutAlign = 'STRETCH';
  divider.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  divider.fills = [{ type: 'SOLID', color: COLOR.border }];
  comp.appendChild(divider);

  /* 납입 정보 그리드 (2열) */
  const infoGrid = figma.createFrame();
  setAutoLayout(infoGrid, 'HORIZONTAL', 0);
  infoGrid.layoutAlign = 'STRETCH';
  infoGrid.primaryAxisSizingMode = 'FIXED';
  infoGrid.counterAxisSizingMode = 'AUTO';
  infoGrid.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  clearFill(infoGrid);

  for (const [label, value] of [['월 납입료', '25,000원'], ['다음 납입일', '2026.05.01']] as const) {
    const cell = figma.createFrame();
    setAutoLayout(cell, 'VERTICAL', SPACING.xs, 'MIN');
    cell.layoutGrow = 1;
    clearFill(cell);
    await addTextWithVar(cell, label, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    await addTextWithVar(cell, value, FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm);
    infoGrid.appendChild(cell);
  }
  comp.appendChild(infoGrid);

  /* 하단 액션 버튼 3개 */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.sm);
  actionRow.layoutAlign = 'STRETCH';
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO';
  actionRow.resize(CARD_WIDTH - SPACING.xl * 2, 1);
  clearFill(actionRow);

  for (const label of ['보험금 청구', '보장 확인', '계약 관리'] as const) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.layoutGrow = 1;
    btn.counterAxisSizingMode = 'AUTO';
    btn.primaryAxisSizingMode = 'FIXED';
    btn.resize(1, 44);
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(btn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXs);
    text.textAlignHorizontal = 'CENTER';
    text.layoutAlign = 'STRETCH';
    actionRow.appendChild(btn);
  }
  comp.appendChild(actionRow);

  return comp;
}

export async function createInsuranceSummaryCard(): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createInsuranceVariant('Active'),
      await createInsuranceVariant('Pending'),
      await createInsuranceVariant('Expired'),
    ],
    'InsuranceSummaryCard', 1,
  );
}
