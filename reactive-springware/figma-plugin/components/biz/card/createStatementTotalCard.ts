/**
 * @file createStatementTotalCard.ts
 * @description Figma StatementTotalCard 컴포넌트 생성.
 * 총 결제금액 + 배지 + 이용내역 화살표 + 3개 액션 버튼.
 * 단일 variant.
 *
 * TEXT properties:
 *   - badge  — 상태 배지 (기본값: '예정')
 *   - amount — 총 결제금액 (기본값: '350,000원')
 *
 * 컴포넌트 이름: "StatementTotalCard"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStroke, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING.md * 2; /* 양쪽 padding-md 제외 */

export async function createStatementTotalCard(): Promise<ComponentNode> {
  const comp = createComponent('StatementTotalCard');
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(CARD_WIDTH, 1);             /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';    /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED';   /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  comp.effects = [{
    type: 'DROP_SHADOW', color: { r: 0, g: 0, b: 0, a: 0.08 },
    offset: { x: 0, y: 2 }, radius: 8, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* ── 레이블 + 배지 ── */
  const labelRow = figma.createFrame();
  setAutoLayout(labelRow, 'HORIZONTAL', SPACING.xs);
  labelRow.primaryAxisAlignItems = 'MIN';
  labelRow.counterAxisAlignItems = 'CENTER';
  labelRow.resize(1, 1);                  /* resize 먼저 */
  labelRow.primaryAxisSizingMode = 'AUTO';
  labelRow.counterAxisSizingMode = 'AUTO';
  clearFill(labelRow);
  comp.appendChild(labelRow);

  /* "총 결제금액" 고정 텍스트 — TEXT property 없음 */
  await addTextWithVar(labelRow, '총 결제금액', FONT_SIZE.sm, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm);

  /* 배지 */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  badge.primaryAxisAlignItems = 'CENTER';
  badge.counterAxisAlignItems = 'CENTER';
  setPadding(badge, 2, SPACING.sm);       /* 상하 padding을 작게 — 높이 줄임 */
  badge.resize(1, 1);                     /* resize 먼저 */
  badge.primaryAxisSizingMode = 'AUTO';   /* width 텍스트+padding에 맞게 */
  badge.counterAxisSizingMode = 'AUTO';   /* height 텍스트+padding에 맞게 */
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
  labelRow.appendChild(badge);
  /* badge — badge → labelRow → comp: 수동 바인딩 */
  await addTextWithVar(badge, '예정', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs, 'badge', comp);

  /* ── 금액 + 화살표 ── */
  const amountRow = figma.createFrame();
  setAutoLayout(amountRow, 'HORIZONTAL', SPACING.xs);
  amountRow.primaryAxisAlignItems = 'MIN';
  amountRow.counterAxisAlignItems = 'CENTER';
  amountRow.resize(1, 1);                 /* resize 먼저 */
  amountRow.primaryAxisSizingMode = 'AUTO';
  amountRow.counterAxisSizingMode = 'AUTO';
  clearFill(amountRow);
  comp.appendChild(amountRow);

  /* 숫자(bound) + 고정 '원' — gap=0 HORIZONTAL 서브 프레임 */
  const amountNumFrame = figma.createFrame();
  setAutoLayout(amountNumFrame, 'HORIZONTAL', 0);
  amountNumFrame.primaryAxisAlignItems = 'MIN';
  amountNumFrame.counterAxisAlignItems = 'CENTER';
  amountNumFrame.resize(1, 1);            /* resize 먼저 */
  amountNumFrame.primaryAxisSizingMode = 'AUTO';
  amountNumFrame.counterAxisSizingMode = 'AUTO';
  clearFill(amountNumFrame);
  amountRow.appendChild(amountNumFrame);
  /* amount (숫자만) — amountNumFrame → amountRow → comp: 수동 바인딩 */
  await addTextWithVar(amountNumFrame, '350,000', FONT_SIZE['3xl'], COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSize3xl, 'amount', comp);
  /* 고정 '원' */
  await addTextWithVar(amountNumFrame, '원', FONT_SIZE['3xl'], COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSize3xl);

  amountRow.appendChild(createIcon('ChevronRight', 24, COLOR.textMuted));

  /* ── 3개 액션 버튼 (분할납부 / 즉시결제 / 일부결제금액이월약정(리볼빙)) ── */
  const actionRow = figma.createFrame();
  setAutoLayout(actionRow, 'HORIZONTAL', SPACING.xs);
  actionRow.primaryAxisAlignItems = 'MIN';
  actionRow.counterAxisAlignItems = 'CENTER';
  actionRow.resize(CONTENT_WIDTH, 1);     /* resize 먼저 */
  actionRow.primaryAxisSizingMode = 'FIXED';
  actionRow.counterAxisSizingMode = 'AUTO'; /* height 버튼에 맞게 */
  clearFill(actionRow);
  comp.appendChild(actionRow);

  /* 고정 텍스트 액션 버튼 — TEXT property 없음 */
  for (const label of ['분할납부', '즉시결제', '일부결제금액이월약정(리볼빙)']) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.layoutGrow = 1;                   /* 3등분 균일 너비 */
    btn.resize(1, 44);                    /* resize 먼저 — 모든 버튼 동일 height 44 고정 */
    btn.primaryAxisSizingMode = 'FIXED';  /* width은 부모 layoutGrow */
    btn.counterAxisSizingMode = 'FIXED';  /* height 44 고정 — 일관된 높이 유지 */
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
    clearFill(btn);
    /* Outline 버튼 스타일: brandPrimary stroke */
    setStroke(btn, BRAND.primary);
    actionRow.appendChild(btn);

    const text = await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeXs);
    text.layoutGrow          = 1;          /* 버튼 너비 채워서 가운데 정렬 효과 */
    text.textAlignHorizontal = 'CENTER';
    text.textAlignVertical   = 'CENTER';   /* 버튼 FIXED 높이 내 세로 중앙 */
    text.textAutoResize      = 'HEIGHT';   /* 긴 텍스트 줄바꿈 */
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
