/**
 * @file createModalSlideOver.ts
 * @description Figma ModalSlideOver 컴포넌트 세트 생성.
 * Route 기반 슬라이드 오버 모달 레이아웃.
 * Direction(Right|Bottom) = 2 variants.
 * - Right:  우측에서 슬라이드 — 모바일 390px 전체 너비 패널 (흰 배경 + 좌측 그림자)
 * - Bottom: 하단에서 슬라이드 — black/40 백드롭 + rounded-t-2xl 패널 (90dvh 근사)
 * 컴포넌트 이름: "ModalSlideOver"
 */
import { COLOR, RADIUS, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, setFloatVar,
} from '../../helpers';

const SCREEN_WIDTH  = 390;
const SCREEN_HEIGHT = 844; /* iPhone 14 기준 전체 높이 */

async function createModalSlideOverVariant(direction: 'Right' | 'Bottom'): Promise<ComponentNode> {
  const comp = createComponent(`Direction=${direction}`);
  comp.resize(SCREEN_WIDTH, SCREEN_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';

  if (direction === 'Right') {
    /* 모바일 390px에서 전체 너비 패널: 백드롭이 패널 뒤에 가려지므로 패널 자체만 표현 */
    setAutoLayout(comp, 'HORIZONTAL', 0);
    await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
    /* 좌측 그림자: slide-in-right 효과 표현 */
    comp.effects = [{
      type: 'DROP_SHADOW',
      color: { r: 0, g: 0, b: 0, a: 0.25 },
      offset: { x: -4, y: 0 },
      radius: 24, spread: 0,
      visible: true, blendMode: 'NORMAL',
    }];
  } else {
    /* 하단 슬라이드: 전체 화면 백드롭(black/40) + 하단 패널(90dvh, rounded-t-2xl) */
    setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
    comp.primaryAxisAlignItems = 'MAX'; /* 패널을 하단으로 밀기 */
    /* black/40 백드롭 */
    comp.fills = [{ type: 'SOLID', color: { r: 0, g: 0, b: 0 }, opacity: 0.4 }];

    const panel = figma.createFrame();
    setAutoLayout(panel, 'VERTICAL', 0, 'MIN');
    /* max-h-[90dvh] ≈ 844 × 0.9 ≈ 760px */
    panel.resize(SCREEN_WIDTH, Math.round(SCREEN_HEIGHT * 0.9));
    panel.primaryAxisSizingMode = 'FIXED';
    panel.counterAxisSizingMode = 'FIXED';
    await setFillWithVar(panel, COLOR_VAR.surface, COLOR.surface);
    /* rounded-t-2xl → Tailwind 1.5rem = 24px = RADIUS.xl */
    await setFloatVar(panel, 'topLeftRadius',  SIZE_VAR.radiusXl, RADIUS.xl);
    await setFloatVar(panel, 'topRightRadius', SIZE_VAR.radiusXl, RADIUS.xl);
    panel.bottomLeftRadius  = 0;
    panel.bottomRightRadius = 0;
    /* 위쪽 그림자: slide-in-bottom 효과 표현 */
    panel.effects = [{
      type: 'DROP_SHADOW',
      color: { r: 0, g: 0, b: 0, a: 0.25 },
      offset: { x: 0, y: -4 },
      radius: 24, spread: 0,
      visible: true, blendMode: 'NORMAL',
    }];
    comp.appendChild(panel);
  }

  return comp;
}

export async function createModalSlideOver(): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createModalSlideOverVariant('Right'),
      await createModalSlideOverVariant('Bottom'),
    ],
    'ModalSlideOver', 1,
  );
}
