/**
 * @file createPinDotIndicator.ts
 * @description Figma PinDotIndicator 컴포넌트 세트 생성.
 * PIN 입력 진행 상태를 도트로 표시하는 컴포넌트.
 *
 * Length(4|5|6) × FilledCount(0~Length) = 5 + 6 + 7 = 18 variants.
 *   Length=4: FilledCount 0..4 → 5 variants
 *   Length=5: FilledCount 0..5 → 6 variants
 *   Length=6: FilledCount 0..6 → 7 variants
 *
 * [도트 스타일]
 *   채워진 도트: brandPrimary fill (브랜드 원색)
 *   빈 도트    : 투명 배경 + borderSubtle 2px stroke
 *
 * 컴포넌트 이름: "PinDotIndicator"
 */
import { BRAND, COLOR, SPACING, RADIUS, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setStrokeWithVar, setFloatVar,
} from '../../../utils/helpers';

const DOT_SIZE = 16; /* size-4 = 16px */
const DOT_GAP  = 20; /* gap-5  = 20px */

async function createDot(filled: boolean): Promise<FrameNode> {
  const dot = figma.createFrame();
  dot.resize(DOT_SIZE, DOT_SIZE);
  dot.layoutMode = 'NONE';
  await setFloatVar(dot, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

  if (filled) {
    await setFillWithVar(dot, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    clearFill(dot);
    await setStrokeWithVar(dot, COLOR_VAR.borderSubtle, COLOR.borderSubtle, 2);
  }

  return dot;
}

async function createPinDotVariant(
  length: 4 | 5 | 6,
  filledCount: number,
): Promise<ComponentNode> {
  const comp = createComponent(`Length=${length}, FilledCount=${filledCount}`);
  setAutoLayout(comp, 'HORIZONTAL', DOT_GAP);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  for (let i = 0; i < length; i++) {
    comp.appendChild(await createDot(i < filledCount));
  }

  return comp;
}

export async function createPinDotIndicator(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];

  for (const length of [4, 5, 6] as (4 | 5 | 6)[]) {
    for (let count = 0; count <= length; count++) {
      components.push(await createPinDotVariant(length, count));
    }
  }

  /* cols=7 (Length=6의 최대 variant 수): Length별로 행이 구분되도록 배치
   * Row 1(대부분 Length=4), Row 2(Length=5 중심), Row 3(Length=6) */
  return combineVariants(components, 'PinDotIndicator', 7);
}
