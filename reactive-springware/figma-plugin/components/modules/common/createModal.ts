/**
 * @file createModal.ts
 * @description Figma Modal 컴포넌트 세트 생성.
 *
 * Size × TitleAlign × ButtonCount 3가지 속성의 조합으로 12종 variant를 생성한다.
 * - Size        : Small | Medium | Large
 * - TitleAlign  : Left | Center
 * - ButtonCount : Two | One
 *
 * React 대응 컴포넌트: packages/component-library/modules/common/Modal/index.tsx
 * BottomSheet는 createBottomSheet.ts 로 분리되었다 (이슈 #53).
 *
 * 색상 → COLOR_VAR + setFillWithVar / addTextWithVar
 * 수치(spacing·radius·fontSize) → SIZE_VAR + setFloatVar
 *
 * @returns Figma ComponentSetNode ('Modal')
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
} from '../../../helpers';
import { createIcon } from '../../../icons';

type ModalSize    = 'Small' | 'Medium' | 'Large';
type TitleAlign   = 'Left'  | 'Center';
type ButtonCount  = 'Two'   | 'One';

/** Modal 크기별 고정 너비·높이 (px) */
const MODAL_SIZE_CONFIG: Record<ModalSize, { w: number; h: number }> = {
  Small:  { w: 320, h: 200 },
  Medium: { w: 360, h: 280 },
  Large:  { w: 390, h: 360 },
};

/**
 * 하단 버튼 색상 설정.
 * index [0] = 취소(secondary), index [1] = 확인(primary).
 * ButtonCount=One 의 경우 index [1] 만 사용한다.
 * 두 버튼 모두 footer 너비를 균등 분할 (layoutGrow=1, fullWidth 대응).
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
 * 단일 Modal variant ComponentNode를 생성한다.
 *
 * @param size        - 모달 크기 ('Small' | 'Medium' | 'Large')
 * @param titleAlign  - 타이틀 정렬 ('Left' | 'Center')
 * @param buttonCount - 하단 버튼 수 ('Two' | 'One')
 * @returns Figma ComponentNode (variant)
 */
async function createModalVariant(
  size: ModalSize,
  titleAlign: TitleAlign,
  buttonCount: ButtonCount,
): Promise<ComponentNode> {
  const { w, h } = MODAL_SIZE_CONFIG[size];
  const comp = createComponent(`Size=${size}, TitleAlign=${titleAlign}, ButtonCount=${buttonCount}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingMd, SPACING.md);
  setPadding(comp, SPACING.xl, SPACING.xl);
  await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacingXl, SPACING.xl);
  await setFloatVar(comp, 'paddingRight',  SIZE_VAR.spacingXl, SPACING.xl);
  await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacingXl, SPACING.xl);
  await setFloatVar(comp, 'paddingLeft',   SIZE_VAR.spacingXl, SPACING.xl);
  comp.resize(w, h);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* ── 헤더: TitleAlign 분기 ────────────────────────────────────── */
  /*
   * 핵심 규칙: layoutSizingHorizontal='FILL' / layoutGrow=1 은
   * 반드시 parent.appendChild(child) 이후에 설정해야 한다.
   */
  if (titleAlign === 'Left') {
    /* Left: 타이틀 좌측 + X 버튼 우측 (SPACE_BETWEEN) */
    const modalHeader = figma.createFrame();
    setAutoLayout(modalHeader, 'HORIZONTAL', 0);
    modalHeader.primaryAxisAlignItems = 'SPACE_BETWEEN';
    modalHeader.counterAxisAlignItems = 'CENTER';
    modalHeader.fills = [];

    /* addTextWithVar 7번째 인자: fontSize Figma Variable 경로 */
    const title = await addTextWithVar(
      modalHeader, '모달 제목', FONT_SIZE.base,
      COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase,
    );
    title.layoutGrow = 1;
    /* X 아이콘: createIcon은 변수 바인딩 미지원 — fallback RGB 사용 */
    modalHeader.appendChild(createIcon('X', 16, COLOR.textMuted));

    comp.appendChild(modalHeader);
    modalHeader.layoutSizingHorizontal = 'FILL';
  } else {
    /* Center: 타이틀 중앙 정렬 + X 버튼 우측 고정
     * 구현: [spacer(32px) | title(layoutGrow=1, CENTER text) | X버튼(32px)]
     * spacer와 X버튼 너비를 동일하게 맞춰 타이틀이 시각적으로 중앙 정렬되도록 함 */
    const modalHeader = figma.createFrame();
    setAutoLayout(modalHeader, 'HORIZONTAL', 0, 'CENTER');
    modalHeader.fills = [];

    const spacer = figma.createFrame();
    spacer.name = 'header-spacer';
    spacer.resize(32, 32);
    spacer.fills = [];
    modalHeader.appendChild(spacer);

    const title = await addTextWithVar(
      modalHeader, '모달 제목', FONT_SIZE.base,
      COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase,
    );
    title.layoutGrow = 1;
    title.textAlignHorizontal = 'CENTER';

    /* X 버튼: 32×32px */
    const closeBtn = figma.createFrame();
    closeBtn.name = 'close-button';
    setAutoLayout(closeBtn, 'HORIZONTAL', 0, 'CENTER');
    closeBtn.resize(32, 32);
    closeBtn.primaryAxisSizingMode = 'FIXED';
    closeBtn.counterAxisSizingMode = 'FIXED';
    await setFloatVar(closeBtn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
    closeBtn.fills = [];
    closeBtn.appendChild(createIcon('X', 16, COLOR.textMuted));
    modalHeader.appendChild(closeBtn);

    comp.appendChild(modalHeader);
    modalHeader.layoutSizingHorizontal = 'FILL';
  }

  /* ── 본문 텍스트 ────────────────────────────────────────────── */
  /* addTextWithVar(comp, ...) 가 내부에서 comp에 append하므로 이후 설정 ✓ */
  const content = await addTextWithVar(
    comp,
    '모달 내용 영역입니다.\n확인이 필요한 정보를 표시합니다.',
    FONT_SIZE.sm,
    COLOR_VAR.textBase,
    COLOR.textBase,
    false,
    SIZE_VAR.fontSizeSm,
  );
  content.layoutSizingHorizontal = 'FILL';
  content.layoutGrow = 1;

  /* ── 하단 버튼 영역 ─────────────────────────────────────────── */
  const footer = figma.createFrame();
  setAutoLayout(footer, 'HORIZONTAL', SPACING.sm);
  await setFloatVar(footer, 'itemSpacing', SIZE_VAR.spacingSm, SPACING.sm);
  footer.fills = [];

  const buttonsToRender = buttonCount === 'Two' ? BUTTON_CONFIGS : [BUTTON_CONFIGS[1]];
  for (const { label, bgVar, bgFallback, textVar, textFallback } of buttonsToRender) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0, 'CENTER');
    btn.resize(80, 40); // resize()는 sizing mode를 FIXED로 리셋 — 이후 layoutGrow로 덮어씀
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

  return comp;
}

/**
 * Modal ComponentSet을 생성하고 캔버스에 추가한다.
 *
 * 12종 variant (Size × TitleAlign × ButtonCount):
 * - Row 1: Left+Two  Small/Medium/Large
 * - Row 2: Left+One  Small/Medium/Large
 * - Row 3: Center+Two  Small/Medium/Large
 * - Row 4: Center+One  Small/Medium/Large
 *
 * @returns Figma ComponentSetNode ('Modal')
 */
export async function createModal(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];

  for (const titleAlign of ['Left', 'Center'] as TitleAlign[]) {
    for (const buttonCount of ['Two', 'One'] as ButtonCount[]) {
      for (const size of ['Small', 'Medium', 'Large'] as ModalSize[]) {
        variants.push(await createModalVariant(size, titleAlign, buttonCount));
      }
    }
  }

  /* cols=3: 크기 순으로 3열 배치 → 행은 TitleAlign+ButtonCount 조합 */
  return combineVariants(variants, 'Modal', 3);
}
