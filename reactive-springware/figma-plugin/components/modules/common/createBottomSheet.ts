/**
 * @file createBottomSheet.ts
 * @description Figma BottomSheet 컴포넌트 세트 생성.
 *
 * Snap × ButtonCount 2가지 속성 조합으로 6종 variant를 생성한다.
 * - Snap        : Auto | Half | Full
 * - ButtonCount : Two | One
 *
 * React 대응 컴포넌트: packages/component-library/modules/common/BottomSheet/index.tsx
 * - 드래그 핸들: w-10(40px) × h-1(4px), rounded-full, bg-border
 * - 헤더: 타이틀 중앙 정렬 + X 버튼 우측 배치 (showCloseButton Boolean 속성으로 제어)
 * - 상단 radius: rounded-t-2xl → Tailwind 기본값 1rem = 16px = RADIUS.lg
 * - footer: border-t border-border-subtle + 버튼 영역
 * - 모든 색상: Figma Variables 바인딩 (COLOR_VAR), fallback RGB 병행 제공
 *
 * 색상 → COLOR_VAR + setFillWithVar / addTextWithVar
 * 수치(spacing·radius·fontSize) → SIZE_VAR + setFloatVar
 *
 * 핵심 규칙: layoutSizingHorizontal='FILL' / layoutGrow=1 은
 * 반드시 parent.appendChild(child) 이후에 설정해야 한다.
 * 이전에 설정하면 Figma가 조용히 무시한다.
 *
 * @returns Figma ComponentSetNode ('BottomSheet')
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent,
  combineVariants,
  setAutoLayout,
  setPadding,
  setFillWithVar,
  addTextWithVar,
  setFloatVar,
  solid,
} from '../../../helpers';
import { createIcon } from '../../../icons';

type SnapMode    = 'Auto' | 'Half' | 'Full';
type ButtonCount = 'Two'  | 'One';

/**
 * snap 프리셋별 고정 높이 (px).
 * index.tsx: auto=max-h-[90dvh], half=max-h-[50dvh], full=max-h-[90dvh]
 * Figma는 dvh 미지원이므로 390×844 (iPhone 14) 기준 근사값을 사용한다.
 */
const SNAP_HEIGHT: Record<SnapMode, number> = { Auto: 300, Half: 420, Full: 680 };

/**
 * 하단 버튼 색상 설정.
 * index [0] = 취소(secondary), index [1] = 확인(primary).
 * ButtonCount=One 의 경우 index [1] 만 사용한다.
 */
const BUTTON_CONFIGS = [
  {
    label:        '취소',
    bgVar:        COLOR_VAR.surfaceRaised,
    bgFallback:   COLOR.surfaceRaised,
    textVar:      COLOR_VAR.textBase,
    textFallback: COLOR.textBase,
  },
  {
    label:        '확인',
    bgVar:        COLOR_VAR.brandPrimary,
    bgFallback:   BRAND.primary,
    textVar:      COLOR_VAR.brandFg,
    textFallback: BRAND.fg,
  },
] as const;

/**
 * 단일 BottomSheet variant ComponentNode를 생성한다.
 *
 * @param snap        - 시트 최대 높이 프리셋 ('Auto' | 'Half' | 'Full')
 * @param buttonCount - 하단 버튼 수 ('Two' | 'One')
 * @returns Figma ComponentNode (variant)
 */
async function createBottomSheetVariant(
  snap: SnapMode,
  buttonCount: ButtonCount,
): Promise<ComponentNode> {
  const comp = createComponent(`Snap=${snap}, ButtonCount=${buttonCount}`);

  /* VERTICAL Auto Layout.
   * counterAxisAlignItems='CENTER': 드래그 핸들(40px)이 390px 폭 내 가로 중앙 정렬 */
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'CENTER');
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingMd, SPACING.md);
  setPadding(comp, SPACING.md, SPACING.xl, SPACING.xl, SPACING.xl);
  await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(comp, 'paddingRight',  SIZE_VAR.spacingXl, SPACING.xl);
  await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacingXl, SPACING.xl);
  await setFloatVar(comp, 'paddingLeft',   SIZE_VAR.spacingXl, SPACING.xl);
  comp.resize(390, SNAP_HEIGHT[snap]);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  /* index.tsx: rounded-t-2xl → Tailwind 기본값 1rem = 16px = RADIUS.lg */
  await setFloatVar(comp, 'topLeftRadius',  SIZE_VAR.radiusLg, RADIUS.lg);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  comp.bottomLeftRadius  = 0;
  comp.bottomRightRadius = 0;
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* ── 드래그 핸들 ──────────────────────────────────────────────
   * index.tsx: <span className="w-10 h-1 rounded-full bg-border" />
   * comp의 counterAxisAlignItems='CENTER'로 가로 중앙 정렬 보장 */
  const handle = figma.createFrame();
  handle.name = 'drag-handle';
  handle.resize(40, 4);
  handle.cornerRadius = RADIUS.full;
  await setFillWithVar(handle, COLOR_VAR.border, COLOR.border);
  comp.appendChild(handle);

  /* ── 헤더: 타이틀 중앙 + X 버튼 우측 ──────────────────────────
   * 구현: [spacer(32px) | title(layoutGrow=1, CENTER text) | X버튼(32px)]
   * spacer와 X버튼 너비를 동일하게 맞춰 타이틀이 시각적으로 중앙 정렬되도록 함 */
  const header = figma.createFrame();
  header.name = 'header';
  setAutoLayout(header, 'HORIZONTAL', 0, 'CENTER');
  setPadding(header, SPACING.sm, 0, SPACING.md, 0);
  header.fills = [];

  const spacer = figma.createFrame();
  spacer.name = 'header-spacer';
  spacer.resize(32, 32);
  spacer.fills = [];
  header.appendChild(spacer);

  /* addTextWithVar는 내부에서 header에 append하므로 layoutGrow 설정 시점 ✓ */
  const titleNode = await addTextWithVar(
    header, '바텀시트 제목', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase,
  );
  titleNode.layoutGrow = 1;
  titleNode.textAlignHorizontal = 'CENTER';

  /* X 버튼: 32×32px, rounded-lg */
  const closeBtn = figma.createFrame();
  closeBtn.name = 'close-button';
  setAutoLayout(closeBtn, 'HORIZONTAL', 0, 'CENTER');
  closeBtn.resize(32, 32);
  closeBtn.primaryAxisSizingMode = 'FIXED';
  closeBtn.counterAxisSizingMode = 'FIXED';
  closeBtn.cornerRadius = RADIUS.sm;
  closeBtn.fills = [];
  closeBtn.appendChild(createIcon('X', 16, COLOR.textMuted));
  header.appendChild(closeBtn);

  /* appendChild 이후에 FILL 설정 */
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  /* ── 본문 placeholder ─────────────────────────────────────────
   * layoutGrow=1: 핸들·헤더·footer를 제외한 세로 공간을 모두 차지 */
  const body = figma.createFrame();
  body.name = 'body';
  body.fills = [];
  comp.appendChild(body);
  /* appendChild 이후에 FILL / layoutGrow 설정 */
  body.layoutSizingHorizontal = 'FILL';
  body.layoutGrow = 1;

  /* ── 하단 버튼 영역 ─────────────────────────────────────────── */
  const footer = figma.createFrame();
  footer.name = 'footer';
  setAutoLayout(footer, 'HORIZONTAL', SPACING.sm);
  await setFloatVar(footer, 'itemSpacing', SIZE_VAR.spacingSm, SPACING.sm);
  setPadding(footer, SPACING.md, 0, SPACING.xl, 0);
  await setFloatVar(footer, 'paddingTop',    SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(footer, 'paddingBottom', SIZE_VAR.spacingXl, SPACING.xl);
  footer.fills = [];
  /* border-t border-border-subtle 시뮬레이션: 상단에만 1px stroke 적용 */
  footer.strokes           = [solid(COLOR.borderSubtle)];
  footer.strokeTopWeight   = 1;
  footer.strokeBottomWeight = 0;
  footer.strokeLeftWeight  = 0;
  footer.strokeRightWeight = 0;
  footer.strokeAlign = 'INSIDE';

  const buttonsToRender = buttonCount === 'Two' ? BUTTON_CONFIGS : [BUTTON_CONFIGS[1]];
  for (const { label, bgVar, bgFallback, textVar, textFallback } of buttonsToRender) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0, 'CENTER');
    btn.resize(80, 48); // h-12(48px) — BottomSheet 버튼은 Modal보다 높이 여유 있음
    btn.counterAxisSizingMode = 'FIXED';
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    setPadding(btn, 0, SPACING.standard, 0, SPACING.standard);
    await setFloatVar(btn, 'paddingRight', SIZE_VAR.spacingStandard, SPACING.standard);
    await setFloatVar(btn, 'paddingLeft',  SIZE_VAR.spacingStandard, SPACING.standard);

    await setFillWithVar(btn, bgVar, bgFallback);
    await addTextWithVar(btn, label, FONT_SIZE.sm, textVar, textFallback, true, SIZE_VAR.fontSizeSm);

    /* appendChild 이후에 layoutGrow 설정 — footer 너비를 균등 분할 */
    footer.appendChild(btn);
    btn.layoutGrow = 1;
  }

  /* appendChild 이후에 FILL 설정 */
  comp.appendChild(footer);
  footer.layoutSizingHorizontal = 'FILL';

  /* ── Figma Boolean 속성: showCloseButton ──────────────────────
   * showCloseButton=true  ↔ hideCloseButton=false (기본: X 버튼 표시)
   * showCloseButton=false ↔ hideCloseButton=true  (X 버튼 숨김) */
  const showClosePropKey = comp.addComponentProperty('showCloseButton', 'BOOLEAN', true);
  closeBtn.componentPropertyReferences = { visible: showClosePropKey };

  return comp;
}

/**
 * BottomSheet ComponentSet을 생성하고 캔버스에 추가한다.
 *
 * 6종 variant (Snap × ButtonCount):
 * - Row 1 (Two): Auto / Half / Full
 * - Row 2 (One): Auto / Half / Full
 *
 * @returns Figma ComponentSetNode ('BottomSheet')
 */
export async function createBottomSheet(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];

  for (const buttonCount of ['Two', 'One'] as ButtonCount[]) {
    for (const snap of ['Auto', 'Half', 'Full'] as SnapMode[]) {
      variants.push(await createBottomSheetVariant(snap, buttonCount));
    }
  }

  /* cols=3: Snap 순으로 3열 배치 → 행은 ButtonCount 조합 */
  return combineVariants(variants, 'BottomSheet', 3);
}
