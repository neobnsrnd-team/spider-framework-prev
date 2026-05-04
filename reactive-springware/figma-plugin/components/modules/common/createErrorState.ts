/**
 * @file createErrorState.ts
 * @description Figma ErrorState 컴포넌트 세트 생성.
 * API 요청 실패·네트워크 오류 시 표시하는 에러 상태 컴포넌트.
 * State(WithRetry|WithoutRetry) = 2 variants.
 * 컴포넌트 이름: "ErrorState"
 */

import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

type ErrorStateVariant = 'WithRetry' | 'WithoutRetry';

async function createErrorStateVariant(variant: ErrorStateVariant): Promise<ComponentNode> {
  const comp = createComponent(`State=${variant}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.lg, 'CENTER');
  setPadding(comp, SPACING['2xl'], SPACING.xl);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* AlertCircle 아이콘 (48px, danger 색상) */
  const iconWrap = figma.createFrame();
  iconWrap.resize(48, 48);
  iconWrap.layoutMode = 'NONE';
  clearFill(iconWrap);
  iconWrap.appendChild(createIcon('AlertCircle', 48, COLOR.danger));
  comp.appendChild(iconWrap);

  /* 텍스트 그룹 */
  const textGroup = figma.createFrame();
  setAutoLayout(textGroup, 'VERTICAL', SPACING.sm, 'CENTER');
  textGroup.primaryAxisSizingMode = 'AUTO';
  textGroup.counterAxisSizingMode = 'AUTO';
  textGroup.primaryAxisAlignItems = 'CENTER';
  clearFill(textGroup);

  /* textGroup을 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(textGroup);

  /* 타이틀 */
  const title = await addTextWithVar(
    textGroup, '오류가 발생했습니다', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase, 'title', comp,
  );
  title.textAlignHorizontal = 'CENTER';

  /* 설명 */
  const desc = await addTextWithVar(
    textGroup, '데이터를 불러오지 못했습니다.\n잠시 후 다시 시도해 주세요.', FONT_SIZE.sm,
    COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm, 'description', comp,
  );
  desc.textAlignHorizontal = 'CENTER';

  /* 재시도 버튼 — WithRetry variant에만 표시 */
  if (variant === 'WithRetry') {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    setPadding(btn, SPACING.sm, SPACING.lg);
    btn.primaryAxisSizingMode = 'AUTO';
    btn.counterAxisSizingMode = 'AUTO';
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
    await setFillWithVar(btn, COLOR_VAR.surface, COLOR.surface);
    await setStrokeWithVar(btn, COLOR_VAR.border, COLOR.border);

    /* btn을 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
    comp.appendChild(btn);

    await addTextWithVar(
      btn, '다시 시도', FONT_SIZE.sm,
      COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm, 'retryLabel', comp,
    );
  }

  return comp;
}

export async function createErrorState(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createErrorStateVariant('WithRetry'),
    createErrorStateVariant('WithoutRetry'),
  ]);
  return combineVariants(components, 'ErrorState', 2);
}
