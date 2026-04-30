/**
 * @file createStepIndicator.ts
 * @description Figma StepIndicator 컴포넌트 세트 생성.
 * 전체 4단계 기준 Current(1|2|3|4) = 4 variants.
 *
 * 활성 도트: w=16 h=8 pill(radiusFull) bg-brand
 * 비활성 도트: w=8 h=8 circle(radiusFull) bg-border
 *
 * 컴포넌트 이름: "StepIndicator"
 */

import { BRAND, COLOR, SPACING, RADIUS, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setFloatVar,
} from '../../../helpers';

const TOTAL = 4;

async function createDot(isActive: boolean): Promise<FrameNode> {
  const dot = figma.createFrame();
  dot.layoutMode = 'NONE';
  /* 활성: pill(16×8), 비활성: circle(8×8) */
  dot.resize(isActive ? 16 : 8, 8);
  await setFloatVar(dot, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

  if (isActive) {
    await setFillWithVar(dot, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(dot, COLOR_VAR.border, COLOR.border);
  }

  return dot;
}

async function createStepIndicatorVariant(current: number): Promise<ComponentNode> {
  const comp = createComponent(`Current=${current}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  for (let i = 1; i <= TOTAL; i++) {
    comp.appendChild(await createDot(i === current));
  }

  return comp;
}

export async function createStepIndicator(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (let current = 1; current <= TOTAL; current++) {
    components.push(await createStepIndicatorVariant(current));
  }
  /* cols=4: 4단계를 한 행에 나란히 */
  return combineVariants(components, 'StepIndicator', 4);
}
