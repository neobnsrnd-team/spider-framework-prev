/**
 * @file createPinDotIndicator.ts
 * @description Figma PinDotIndicator 컴포넌트 세트 생성.
 * PIN 입력 진행 상태를 도트로 표시하는 컴포넌트.
 * FilledCount(0|1|2|3|4) = 5 variants.
 * - 채워진 도트: bg-brand (브랜드 원색)
 * - 빈 도트: border 2px border-subtle
 * 컴포넌트 이름: "PinDotIndicator"
 */

import { BRAND, COLOR, SPACING, RADIUS, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setStrokeWithVar, setFloatVar,
} from '../../../helpers';

const DOT_SIZE  = 16; /* size-4 = 16px */
const DOT_GAP   = 20; /* gap-5 = 20px */
const PIN_LENGTH = 4;

/** 단일 도트 프레임 생성 */
async function createDot(filled: boolean): Promise<FrameNode> {
  const dot = figma.createFrame();
  dot.resize(DOT_SIZE, DOT_SIZE);
  dot.layoutMode = 'NONE';
  /* 완전한 원형: full radius */
  await setFloatVar(dot, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

  if (filled) {
    /* 채워진 자리: 브랜드 색 */
    await setFillWithVar(dot, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    /* 미입력 자리: 투명 배경 + 테두리 */
    clearFill(dot);
    await setStrokeWithVar(dot, COLOR_VAR.borderSubtle, COLOR.borderSubtle, 2);
  }

  return dot;
}

async function createPinDotVariant(filledCount: number): Promise<ComponentNode> {
  const comp = createComponent(`FilledCount=${filledCount}`);
  setAutoLayout(comp, 'HORIZONTAL', DOT_GAP);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  for (let i = 0; i < PIN_LENGTH; i++) {
    comp.appendChild(await createDot(i < filledCount));
  }

  return comp;
}

export async function createPinDotIndicator(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (let count = 0; count <= PIN_LENGTH; count++) {
    components.push(await createPinDotVariant(count));
  }
  /* 5 variants: 한 행에 모두 배치 */
  return combineVariants(components, 'PinDotIndicator', 5);
}
