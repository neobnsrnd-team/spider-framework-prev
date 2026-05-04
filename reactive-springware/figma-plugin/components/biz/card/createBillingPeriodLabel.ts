/**
 * @file createBillingPeriodLabel.ts
 * @description Figma BillingPeriodLabel 컴포넌트 생성.
 * 카드 이용내역 상단 청구 기간 레이블.
 * Clock 아이콘 + "이용기간 : 시작일 ~ 종료일" + 하단 border.
 * 단일 variant.
 * 컴포넌트 이름: "BillingPeriodLabel"
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setStrokeWithVar, addTextWithVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createBillingPeriodLabel(): Promise<ComponentNode> {
  const comp = createComponent('BillingPeriodLabel');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, 0, SPACING.standard, SPACING.sm, SPACING.standard);
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);
  /* 하단 border-b */
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeTopWeight = 0; comp.strokeLeftWeight = 0; comp.strokeRightWeight = 0;
  comp.strokeBottomWeight = 1;

  comp.appendChild(createIcon('Clock', 14, COLOR.textMuted));
  await addTextWithVar(comp, '이용기간 : 2026.03.01 ~ 2026.03.31', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'billingPeriodText');

  figma.currentPage.appendChild(comp);
  return comp;
}
