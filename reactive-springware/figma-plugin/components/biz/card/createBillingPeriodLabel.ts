/**
 * @file createBillingPeriodLabel.ts
 * @description Figma BillingPeriodLabel 컴포넌트 생성.
 * 카드 이용내역 상단 청구 기간 레이블.
 * Clock 아이콘 + "이용기간 : startDate ~ endDate" + 하단 border.
 * 단일 variant.
 *
 * TEXT properties:
 *   - startDate — 이용기간 시작일 (기본값: '2026.03.01')
 *   - endDate   — 이용기간 종료일 (기본값: '2026.03.31')
 *
 * [레이아웃]
 *   comp(HORIZONTAL, gap=sm, p-[0 standard sm standard], FIXED 390, AUTO height, border-b)
 *     Clock(14px, textMuted)
 *     textRow(HORIZONTAL, gap=0, CENTER)
 *       "이용기간 : "(TEXT xs, textMuted, 정적)
 *       startDate(TEXT xs, textMuted, 바인딩)
 *       " ~ "(TEXT xs, textMuted, 정적)
 *       endDate(TEXT xs, textMuted, 바인딩)
 *
 * TEXT property 바인딩 타이밍:
 *   startDate / endDate — comp → textRow → text: 수동 바인딩
 *
 * 컴포넌트 이름: "BillingPeriodLabel"
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setStrokeWithVar, addTextWithVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createBillingPeriodLabel(): Promise<ComponentNode> {
  const comp = createComponent('BillingPeriodLabel');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.primaryAxisAlignItems = 'MIN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, 0, SPACING.standard, SPACING.sm, SPACING.standard);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);
  /* 하단 border-b */
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeTopWeight = 0; comp.strokeLeftWeight = 0; comp.strokeRightWeight = 0;
  comp.strokeBottomWeight = 1;

  comp.appendChild(createIcon('Clock', 14, COLOR.textMuted));

  /* 기간 텍스트 컨테이너 — gap=0으로 텍스트 조각을 인접하게 배치 */
  const textRow = figma.createFrame();
  setAutoLayout(textRow, 'HORIZONTAL', 0);
  textRow.primaryAxisAlignItems = 'MIN';
  textRow.counterAxisAlignItems = 'CENTER';
  textRow.primaryAxisSizingMode = 'AUTO';
  textRow.counterAxisSizingMode = 'AUTO';
  clearFill(textRow);
  comp.appendChild(textRow);

  /* 정적 레이블 — TEXT property 불필요 */
  await addTextWithVar(textRow, '이용기간 : ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* startDate — comp → textRow → text: 수동 바인딩 */
  const startDateNode = await addTextWithVar(textRow, '2026.03.01', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  const startDateKey = comp.addComponentProperty('startDate', 'TEXT', '2026.03.01');
  startDateNode.componentPropertyReferences = { characters: startDateKey };

  /* 정적 구분자 */
  await addTextWithVar(textRow, ' ~ ', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);

  /* endDate — comp → textRow → text: 수동 바인딩 */
  const endDateNode = await addTextWithVar(textRow, '2026.03.31', FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  const endDateKey = comp.addComponentProperty('endDate', 'TEXT', '2026.03.31');
  endDateNode.componentPropertyReferences = { characters: endDateKey };

  figma.currentPage.appendChild(comp);
  return comp;
}
