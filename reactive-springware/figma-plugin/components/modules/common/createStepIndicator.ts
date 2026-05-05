/**
 * @file createStepIndicator.ts
 * @description Figma StepIndicator 컴포넌트 세트 생성.
 * Current(1|2|3|4) = 4 variants.
 *
 * TEXT properties:
 *   - total — 총 단계 수 텍스트 (기본값: '4')
 *             ※ total TEXT property는 표시 레이블만 제어한다.
 *               도트 개수는 TOTAL 상수(4)로 컴파일 타임에 결정되므로,
 *               총 단계 수 변경 시 TOTAL 상수도 함께 수정해야 한다.
 *
 * [레이아웃]
 *   comp(VERTICAL, CENTER)
 *     DotsRow(HORIZONTAL): 활성 pill(16×8) + 비활성 circle(8×8)
 *     StepCounter(HORIZONTAL): "{current} / " + [total TEXT property]
 *
 * TEXT property 바인딩 타이밍:
 *   total — comp.appendChild(stepCounter) 이후 수동 바인딩
 *   (2단계: comp → stepCounter → totalText)
 *
 * 컴포넌트 이름: "StepIndicator"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

const TOTAL = 4; /* 총 단계 수 — 변경 시 이 상수만 수정 */

async function createDot(isActive: boolean): Promise<FrameNode> {
  const dot = figma.createFrame();
  dot.layoutMode = 'NONE';
  /* 활성: pill(16×8), 비활성: circle(8×8) */
  dot.resize(isActive ? 16 : 8, 8);
  await setFloatVar(dot, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(dot, isActive ? COLOR_VAR.brandPrimary : COLOR_VAR.border, isActive ? BRAND.primary : COLOR.border);
  return dot;
}

async function createStepIndicatorVariant(current: number): Promise<ComponentNode> {
  const comp = createComponent(`Current=${current}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'CENTER');
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* ── 도트 행 ─────────────────────────────────────────────── */
  const dotsRow = figma.createFrame();
  dotsRow.name = 'DotsRow';
  setAutoLayout(dotsRow, 'HORIZONTAL', SPACING.xs);
  dotsRow.primaryAxisSizingMode = 'AUTO';
  dotsRow.counterAxisSizingMode = 'AUTO';
  clearFill(dotsRow);
  comp.appendChild(dotsRow);

  for (let i = 1; i <= TOTAL; i++) {
    dotsRow.appendChild(await createDot(i === current));
  }

  /* ── 단계 카운터: "{current} / {total}" ──────────────────── */
  const stepCounter = figma.createFrame();
  stepCounter.name = 'StepCounter';
  setAutoLayout(stepCounter, 'HORIZONTAL', 0);
  stepCounter.primaryAxisSizingMode = 'AUTO';
  stepCounter.counterAxisSizingMode = 'AUTO';
  clearFill(stepCounter);

  /* comp.appendChild(stepCounter) 이후 total TEXT property 바인딩 (2단계 ✓) */
  comp.appendChild(stepCounter);

  /* 현재 단계 접두 텍스트 — variant마다 고정값, TEXT property 없음 */
  await addTextWithVar(
    stepCounter, `${current} / `, FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );

  /* total — TEXT property, default = TOTAL 상수값 */
  const totalText = await addTextWithVar(
    stepCounter, `${TOTAL}`, FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  const totalKey = comp.addComponentProperty('total', 'TEXT', `${TOTAL}`);
  totalText.componentPropertyReferences = { characters: totalKey };

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
