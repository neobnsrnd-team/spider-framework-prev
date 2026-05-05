/**
 * @file createInsuranceSummaryCard.ts
 * @description Figma InsuranceSummaryCard 컴포넌트 세트 생성.
 * 보험 도메인 요약 카드.
 * type(Health|Car|Life) × status(Active|Pending|Expired) = 9 variants.
 *
 * ── VARIANT properties ──────────────────────────────────────────
 *   - type   — 보험 종류 (Health | Car | Life)
 *              Health → Heart 아이콘, Car → Car 아이콘, Life → Shield 아이콘
 *   - status — 계약 상태 (Active | Pending | Expired)
 *              Active:  유효 — 초록 배지, 다음 납입일 노출
 *              Pending: 대기 중 — 노랑 배지, 다음 납입일 노출
 *              Expired: 만료 — 빨간 배지, 다음 납입일 미노출
 *
 * ── TEXT properties ─────────────────────────────────────────────
 *   - insuranceName   — 보험명
 *   - contractNumber  — 계약번호 (prefix '계약번호 · '는 고정)
 *   - premium         — 납입료 숫자 (표시: {premium}원)
 *   - nextPaymentDate — 다음 납입일 (표시: 다음 납입일 {nextPaymentDate}, Expired 미노출)
 *
 * 컴포넌트 이름: "InsuranceSummaryCard"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING.xl * 2;

const TYPE_CONFIG = {
  Health: { icon: 'Heart'   as const, defaultName: '하나 건강보험' },
  Car:    { icon: 'Car'     as const, defaultName: '하나 자동차보험' },
  Life:   { icon: 'Shield'  as const, defaultName: '하나 생명보험' },
} as const;

const STATUS_CONFIG = {
  Active: {
    label:        '정상',
    badgeFillVar: COLOR_VAR.successSurface,
    badgeFill:    COLOR.successSurface,
    badgeTextVar: COLOR_VAR.successText,
    badgeText:    COLOR.successText,
    statusIcon:   'CheckCircle2' as const,
    showNextPay:  true,
  },
  Pending: {
    label:        '유예',
    badgeFillVar: COLOR_VAR.warningSurface,
    badgeFill:    COLOR.warningSurface,
    badgeTextVar: COLOR_VAR.warningText,
    badgeText:    COLOR.warningText,
    statusIcon:   'AlertTriangle' as const,
    showNextPay:  true,
  },
  Expired: {
    label:        '만기',
    badgeFillVar: COLOR_VAR.dangerSurface,
    badgeFill:    COLOR.dangerSurface,
    badgeTextVar: COLOR_VAR.dangerText,
    badgeText:    COLOR.dangerText,
    statusIcon:   'AlertCircle' as const,
    showNextPay:  false,              /* 만기 시 다음 납입일 미노출 */
  },
} as const;

async function createInsuranceVariant(
  type:   'Health' | 'Car' | 'Life',
  status: 'Active' | 'Pending' | 'Expired',
): Promise<ComponentNode> {
  const typeCfg   = TYPE_CONFIG[type];
  const statusCfg = STATUS_CONFIG[status];

  const comp = createComponent(`type=${type}, status=${status}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'MIN');
  setPadding(comp, SPACING.xl, SPACING.xl);
  comp.resize(CARD_WIDTH, 1);           /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED'; /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign  = 'INSIDE';

  /* ── 상단 행: 타입 아이콘 + 보험명 + 상태 배지 ── */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', SPACING.sm);
  topRow.primaryAxisAlignItems = 'MIN';  /* 왼쪽 정렬 — 아이콘·이름·배지 순서대로 붙여서 배치 */
  topRow.counterAxisAlignItems = 'CENTER';
  topRow.resize(CONTENT_WIDTH, 1);       /* resize 먼저 */
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';
  clearFill(topRow);
  comp.appendChild(topRow);

  /* 보험 종류 아이콘 (type별 고정) */
  topRow.appendChild(createIcon(typeCfg.icon, 20, COLOR.textMuted));

  /* 보험명 — TEXT: insuranceName (topRow → comp: 수동 바인딩) */
  await addTextWithVar(
    topRow, typeCfg.defaultName, FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase, 'insuranceName', comp,
  );
  /* layoutGrow 제거 — 배지가 이름 바로 오른쪽에 붙도록 */

  /* 상태 배지 — 고정 텍스트 (statusLabel TEXT property 없음, status variant로 구분) */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', SPACING.xs);
  setPadding(badge, SPACING.xs, SPACING.sm);
  badge.primaryAxisAlignItems = 'CENTER';
  badge.counterAxisAlignItems = 'CENTER';
  badge.resize(1, 1);                   /* resize 먼저 */
  badge.primaryAxisSizingMode = 'AUTO';
  badge.counterAxisSizingMode = 'AUTO';
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, statusCfg.badgeFillVar, statusCfg.badgeFill);
  topRow.appendChild(badge);
  badge.appendChild(createIcon(statusCfg.statusIcon, 12, statusCfg.badgeText));
  await addTextWithVar(
    badge, statusCfg.label, FONT_SIZE.xs,
    statusCfg.badgeTextVar, statusCfg.badgeText,
    false, SIZE_VAR.fontSizeXs,
    /* propName 없음 — 고정 텍스트 */
  );

  /* ── 계약번호 — '계약번호 · ' 고정 prefix + TEXT: contractNumber ── */
  const contractRow = figma.createFrame();
  setAutoLayout(contractRow, 'HORIZONTAL', 0);
  contractRow.primaryAxisAlignItems = 'CENTER';
  contractRow.counterAxisAlignItems = 'CENTER';
  contractRow.resize(1, 1);             /* resize 먼저 */
  contractRow.primaryAxisSizingMode = 'AUTO';
  contractRow.counterAxisSizingMode = 'AUTO';
  clearFill(contractRow);
  comp.appendChild(contractRow);
  await addTextWithVar(contractRow, '계약번호 · ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  /* contractNumber — contractRow → comp: 수동 바인딩 */
  await addTextWithVar(
    contractRow, '2024-001234-56', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs, 'contractNumber', comp,
  );

  /* ── 구분선 ── */
  const divider = figma.createFrame();
  divider.resize(CONTENT_WIDTH, 1);
  divider.primaryAxisSizingMode = 'FIXED';
  divider.counterAxisSizingMode = 'FIXED';
  divider.fills = [{ type: 'SOLID', color: COLOR.border }];
  comp.appendChild(divider);

  /* ── 납입 정보 ── */
  const infoSection = figma.createFrame();
  setAutoLayout(infoSection, 'VERTICAL', SPACING.xs, 'MIN');
  infoSection.primaryAxisAlignItems = 'MIN';
  infoSection.resize(CONTENT_WIDTH, 1); /* resize 먼저 */
  infoSection.primaryAxisSizingMode = 'AUTO';
  infoSection.counterAxisSizingMode = 'FIXED';
  clearFill(infoSection);
  comp.appendChild(infoSection);

  /* {premium}원 — HORIZONTAL gap=0: 숫자 + 고정 '원' */
  const premiumRow = figma.createFrame();
  setAutoLayout(premiumRow, 'HORIZONTAL', 0);
  premiumRow.counterAxisAlignItems = 'BASELINE'; /* 폰트 기준선 맞춤 */
  premiumRow.resize(1, 1);              /* resize 먼저 */
  premiumRow.primaryAxisSizingMode = 'AUTO';
  premiumRow.counterAxisSizingMode = 'AUTO';
  clearFill(premiumRow);
  infoSection.appendChild(premiumRow);

  /* premium 숫자 — TEXT: premium (premiumRow → infoSection → comp: 수동 바인딩) */
  await addTextWithVar(
    premiumRow, '25,000', FONT_SIZE.lg,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeLg, 'premium', comp,
  );
  /* '원' 고정 suffix */
  await addTextWithVar(premiumRow, '원', FONT_SIZE.lg, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeLg);

  /* 다음 납입일 {nextPaymentDate} — Expired 미노출 */
  if (statusCfg.showNextPay) {
    const nextPayRow = figma.createFrame();
    setAutoLayout(nextPayRow, 'HORIZONTAL', 0);
    nextPayRow.counterAxisAlignItems = 'CENTER';
    nextPayRow.resize(1, 1);            /* resize 먼저 */
    nextPayRow.primaryAxisSizingMode = 'AUTO';
    nextPayRow.counterAxisSizingMode = 'AUTO';
    clearFill(nextPayRow);
    infoSection.appendChild(nextPayRow);

    /* '다음 납입일 ' 고정 prefix */
    await addTextWithVar(nextPayRow, '다음 납입일 ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
    /* nextPaymentDate — TEXT: nextPaymentDate (nextPayRow → comp: 수동 바인딩) */
    await addTextWithVar(
      nextPayRow, '2026.05.01', FONT_SIZE.xs,
      COLOR_VAR.textMuted, COLOR.textMuted,
      false, SIZE_VAR.fontSizeXs, 'nextPaymentDate', comp,
    );
  }

  /* ── 하단 액션 버튼 3개 ── */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.sm);
  actionRow.primaryAxisAlignItems = 'MIN';
  actionRow.counterAxisAlignItems = 'CENTER';
  actionRow.resize(CONTENT_WIDTH, 1);   /* resize 먼저 */
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO';
  clearFill(actionRow);
  comp.appendChild(actionRow);

  for (const label of ['보험금 청구', '보장 확인', '계약 관리'] as const) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.layoutGrow = 1;
    btn.resize(1, 44);                  /* resize 먼저 — height 44 고정 */
    btn.primaryAxisSizingMode = 'FIXED';
    btn.counterAxisSizingMode = 'FIXED';
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(btn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
    actionRow.appendChild(btn);
    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXs);
    text.textAlignHorizontal = 'CENTER';
    text.layoutGrow = 1;
  }

  return comp;
}

export async function createInsuranceSummaryCard(): Promise<ComponentSetNode> {
  const types:    Array<'Health' | 'Car' | 'Life'>         = ['Health', 'Car', 'Life'];
  const statuses: Array<'Active' | 'Pending' | 'Expired'>  = ['Active', 'Pending', 'Expired'];
  const variants: ComponentNode[] = [];

  for (const type of types) {
    for (const status of statuses) {
      variants.push(await createInsuranceVariant(type, status));
    }
  }

  /* 3열(status 3개) × 3행(type 3개) 레이아웃 */
  return combineVariants(variants, 'InsuranceSummaryCard', 3);
}
