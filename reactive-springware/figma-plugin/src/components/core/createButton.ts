/**
 * @file createButton.ts
 * @description Figma Button 컴포넌트 세트 생성.
 * 아래 4개 ComponentSet을 생성해 Button의 모든 prop 조합을 Figma에 표현한다.
 *
 * 1. Button           — Variant(4) × Size(3) × State(Default|Loading|Disabled) = 36
 * 2. Button/WithIcon  — Variant(4) × Size(3) × Icon(Left|Right) = 24
 * 3. Button/IconOnly  — Variant(4) × Size(3) = 12  (정방형 아이콘 전용)
 * 4. Button/FullWidth — Variant(4) × Justify(Center|Between) = 8  (Medium 고정)
 *
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없으면 tokens.ts의 RGB fallback 적용.
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setFillWithVar, setStroke, clearStroke, addTextWithVar, addRect, setFloatVar,
} from '../../helpers';

type ButtonVariant = 'Primary' | 'Outline' | 'Ghost' | 'Danger';
type ButtonSize    = 'Small' | 'Medium' | 'Large';
type ButtonState   = 'Default' | 'Loading' | 'Disabled';
type ButtonIcon    = 'Left' | 'Right';
type ButtonJustify = 'Center' | 'Between';

/** size별 height / padding-x / font-size / border-radius / icon 크기 */
const SIZE_CONFIG: Record<ButtonSize, {
  height: number; px: number; pxVar: string; fontSize: number; fontSizeVar: string; radius: number; radiusVar: string; iconSize: number;
}> = {
  /* component-library: sm=rounded-lg(16px), md/lg=rounded-xl(24px) */
  Small:  { height: 32, px: SPACING.md,      pxVar: SIZE_VAR.spacingMd,       fontSize: FONT_SIZE.xs, fontSizeVar: SIZE_VAR.fontSizeXs, radius: RADIUS.lg, radiusVar: SIZE_VAR.radiusLg, iconSize: 12 },
  Medium: { height: 40, px: SPACING.standard, pxVar: SIZE_VAR.spacingStandard, fontSize: FONT_SIZE.sm, fontSizeVar: SIZE_VAR.fontSizeSm, radius: RADIUS.xl, radiusVar: SIZE_VAR.radiusXl, iconSize: 14 },
  Large:  { height: 56, px: SPACING.xl,       pxVar: SIZE_VAR.spacingXl,       fontSize: FONT_SIZE.lg, fontSizeVar: SIZE_VAR.fontSizeLg, radius: RADIUS.xl, radiusVar: SIZE_VAR.radiusXl, iconSize: 16 },
};

/* ── 색상 헬퍼 ─────────────────────────────────────────────── */

/** variant × state 조합의 배경 변수·fallback 반환 */
function getBgStyle(variant: ButtonVariant, state: ButtonState) {
  if (state === 'Disabled') {
    /* Outline / Ghost disabled: 투명 배경 유지 */
    if (variant === 'Outline' || variant === 'Ghost') return null;
    return { varName: COLOR_VAR.textDisabled, fallback: COLOR.textDisabled };
  }
  switch (variant) {
    case 'Primary': return { varName: COLOR_VAR.brandPrimary, fallback: BRAND.primary };
    case 'Danger':  return { varName: COLOR_VAR.danger, fallback: COLOR.dangerDark };
    /* Outline / Ghost: 투명 배경 */
    default:        return null;
  }
}

/** variant × state 조합의 텍스트 변수·fallback 반환 */
function getTextStyle(variant: ButtonVariant, state: ButtonState) {
  if (state === 'Disabled') {
    /* Primary / Danger disabled: surface(흰색) 텍스트 */
    if (variant === 'Primary' || variant === 'Danger') {
      return { varName: COLOR_VAR.surface, fallback: COLOR.surface };
    }
    return { varName: COLOR_VAR.textDisabled, fallback: COLOR.textDisabled };
  }
  switch (variant) {
    case 'Primary': return { varName: COLOR_VAR.brandFg,   fallback: BRAND.fg   };
    case 'Danger':  return { varName: COLOR_VAR.surface,   fallback: COLOR.surface };
    default:        return { varName: COLOR_VAR.brandText, fallback: BRAND.text  };
  }
}

/* ── 내부 레이아웃 적용 ────────────────────────────────────── */

/** 버튼 프레임 기본 레이아웃 세팅 */
async function applyBaseLayout(
  comp: ComponentNode,
  size: ButtonSize,
  fullWidth = false,
) {
  const { height, px, pxVar, radius, radiusVar } = SIZE_CONFIG[size];
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingSm, SPACING.sm);
  setPadding(comp, 0, px);
  await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0, SPACING['0']);
  await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0, SPACING['0']);
  await setFloatVar(comp, 'paddingRight',  pxVar, px);
  await setFloatVar(comp, 'paddingLeft',   pxVar, px);
  comp.resize(fullWidth ? 320 : 120, height);
  comp.primaryAxisSizingMode  = fullWidth ? 'FIXED' : 'AUTO';
  comp.counterAxisSizingMode  = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', radiusVar, radius);
}

/**
 * 스피너 엘립스를 버튼 중앙에 추가한다 (Loading 상태).
 * Frame overlay를 사용하지 않고 스피너 엘립스만 직접 추가한다.
 * 텍스트는 호출 전에 이미 스킵되므로 스피너만 남아 중앙 정렬된다.
 */
function addSpinnerOverlay(comp: ComponentNode, size: ButtonSize, spinnerColor: { r: number; g: number; b: number }) {
  const { iconSize } = SIZE_CONFIG[size];

  /* 텍스트가 없으므로 FIXED + CENTER로 스피너를 중앙 정렬 */
  comp.primaryAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';

  const spinner = figma.createEllipse();
  spinner.resize(iconSize, iconSize);
  spinner.fills = [];
  spinner.strokes = [{ type: 'SOLID', color: spinnerColor, opacity: 0.9 }];
  spinner.strokeWeight = 2;
  /* 3/4 원호: 스피너 아이콘 표현 */
  spinner.arcData = { startingAngle: 0, endingAngle: Math.PI * 1.5, innerRadius: 0 };
  comp.appendChild(spinner);
}

/** 아이콘 플레이스홀더 사각형 추가 */
function addIconPlaceholder(comp: ComponentNode, size: ButtonSize) {
  const { iconSize } = SIZE_CONFIG[size];
  addRect(comp, iconSize, iconSize, BRAND.fg, RADIUS.xs);
}

/* ── ComponentSet 생성 함수 ──────────────────────────────────── */

/**
 * 1. Button — Variant × Size × State = 36
 * State: Default(기본) | Loading(스피너 오버레이) | Disabled(회색 처리)
 */
export async function createButton(): Promise<ComponentSetNode> {
  const variants: ButtonVariant[] = ['Primary', 'Outline', 'Ghost', 'Danger'];
  const sizes:    ButtonSize[]    = ['Small', 'Medium', 'Large'];
  const states:   ButtonState[]   = ['Default', 'Loading', 'Disabled'];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const size of sizes) {
      for (const state of states) {
        const comp = createComponent(
          `Variant=${variant}, Size=${size}, State=${state}`,
        );
        await applyBaseLayout(comp, size);

        const bg   = getBgStyle(variant, state);
        const text = getTextStyle(variant, state);

        /* 배경색 */
        if (bg) {
          await setFillWithVar(comp, bg.varName, bg.fallback);
        } else {
          comp.fills = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
        }

        /* 테두리 — Outline variant (Disabled 시 border 변수 사용) */
        if (variant === 'Outline') {
          if (state === 'Disabled') {
            await setFillWithVar(
              /* setStroke는 RGB만 받으므로 직접 strokes 세팅 */
              comp as unknown as Parameters<typeof setFillWithVar>[0],
              COLOR_VAR.border,
              COLOR.border,
            );
            comp.strokes = comp.fills as unknown as Paint[];
            comp.fills   = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
            comp.strokeWeight = 1;
            comp.strokeAlign  = 'INSIDE';
          } else {
            setStroke(comp, BRAND.primary);
          }
        } else {
          clearStroke(comp);
        }

        /* 텍스트 레이블 — Loading 상태에서는 텍스트 없이 스피너만 표시 */
        if (text && state !== 'Loading') {
          const label = await addTextWithVar(comp, '버튼', SIZE_CONFIG[size].fontSize, text.varName, text.fallback, true, SIZE_CONFIG[size].fontSizeVar);
          label.textAlignHorizontal = 'CENTER';
        }

        /* Loading 스피너 — 스피너 색상은 텍스트 색과 동일 */
        if (state === 'Loading') {
          const spinnerColor = text?.fallback ?? { r: 1, g: 1, b: 1 };
          addSpinnerOverlay(comp, size, spinnerColor);
        }

        components.push(comp);
      }
    }
  }

  /* cols=3: 한 행에 State 3개, 행마다 Size 변경, Variant 그룹으로 묶임 */
  return combineVariants(components, 'Button', 3);
}

/**
 * 2. Button/WithIcon — Variant × Size × Icon(Left|Right) = 24
 * leftIcon / rightIcon prop 표현: 아이콘 플레이스홀더(rect) 포함
 */
export async function createButtonWithIcon(): Promise<ComponentSetNode> {
  const variants: ButtonVariant[] = ['Primary', 'Outline', 'Ghost', 'Danger'];
  const sizes:    ButtonSize[]    = ['Small', 'Medium', 'Large'];
  const icons:    ButtonIcon[]    = ['Left', 'Right'];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const size of sizes) {
      for (const icon of icons) {
        const comp = createComponent(
          `Variant=${variant}, Size=${size}, Icon=${icon}`,
        );
        await applyBaseLayout(comp, size);

        const bg   = getBgStyle(variant, 'Default');
        const text = getTextStyle(variant, 'Default');

        if (bg) {
          await setFillWithVar(comp, bg.varName, bg.fallback);
        } else {
          comp.fills = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
        }

        if (variant === 'Outline') {
          setStroke(comp, BRAND.primary);
        } else {
          clearStroke(comp);
        }

        if (icon === 'Left') addIconPlaceholder(comp, size);

        if (text) {
          const label = await addTextWithVar(comp, '버튼', SIZE_CONFIG[size].fontSize, text.varName, text.fallback, true, SIZE_CONFIG[size].fontSizeVar);
          label.textAlignHorizontal = 'CENTER';
        }

        if (icon === 'Right') addIconPlaceholder(comp, size);

        components.push(comp);
      }
    }
  }

  return combineVariants(components, 'Button/WithIcon', 3);
}

/**
 * 3. Button/IconOnly — Variant × Size = 12
 * iconOnly=true prop 표현: 정방형 버튼, 텍스트 없이 아이콘 플레이스홀더만
 */
export async function createButtonIconOnly(): Promise<ComponentSetNode> {
  const variants: ButtonVariant[] = ['Primary', 'Outline', 'Ghost', 'Danger'];
  const sizes:    ButtonSize[]    = ['Small', 'Medium', 'Large'];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const size of sizes) {
      const { height, iconSize, radius, radiusVar } = SIZE_CONFIG[size];

      const comp = createComponent(`Variant=${variant}, Size=${size}`);
      /* 정방형: width = height */
      comp.resize(height, height);
      await setFloatVar(comp, 'cornerRadius', radiusVar, radius);
      setAutoLayout(comp, 'HORIZONTAL', 0);
      comp.primaryAxisAlignItems  = 'CENTER';
      comp.counterAxisAlignItems  = 'CENTER';
      comp.primaryAxisSizingMode  = 'FIXED';
      comp.counterAxisSizingMode  = 'FIXED';

      const bg   = getBgStyle(variant, 'Default');
      const text = getTextStyle(variant, 'Default');

      if (bg) {
        await setFillWithVar(comp, bg.varName, bg.fallback);
      } else {
        comp.fills = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
      }

      if (variant === 'Outline') {
        setStroke(comp, BRAND.primary);
      } else {
        clearStroke(comp);
      }

      /* 아이콘 플레이스홀더 */
      if (text) {
        addRect(comp, iconSize, iconSize, text.fallback, RADIUS.xs);
      }

      components.push(comp);
    }
  }

  return combineVariants(components, 'Button/IconOnly', 3);
}

/**
 * 4. Button/FullWidth — Variant × Justify(Center|Between) = 8
 * fullWidth=true + justify prop 표현: 고정 너비(320) Medium 버튼
 * - Center: 텍스트 가운데 정렬
 * - Between: 텍스트 왼쪽 + 아이콘 오른쪽 분리 (justify-between)
 */
export async function createButtonFullWidth(): Promise<ComponentSetNode> {
  const variants:  ButtonVariant[] = ['Primary', 'Outline', 'Ghost', 'Danger'];
  const justifies: ButtonJustify[] = ['Center', 'Between'];

  const components: ComponentNode[] = [];

  for (const variant of variants) {
    for (const justify of justifies) {
      const comp = createComponent(`Variant=${variant}, Justify=${justify}`);
      await applyBaseLayout(comp, 'Medium', true);

      /* justify=Between: primaryAxisAlignItems를 SPACE_BETWEEN으로 설정 */
      if (justify === 'Between') {
        comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
      }

      const bg   = getBgStyle(variant, 'Default');
      const text = getTextStyle(variant, 'Default');

      if (bg) {
        await setFillWithVar(comp, bg.varName, bg.fallback);
      } else {
        comp.fills = [{ type: 'SOLID', color: COLOR.surface, opacity: 0 }];
      }

      if (variant === 'Outline') {
        setStroke(comp, BRAND.primary);
      } else {
        clearStroke(comp);
      }

      if (justify === 'Between') {
        addIconPlaceholder(comp, 'Medium');
      }

      if (text) {
        const label = await addTextWithVar(comp, '버튼', FONT_SIZE.sm, text.varName, text.fallback, true, SIZE_VAR.fontSizeSm);
        label.textAlignHorizontal = justify === 'Center' ? 'CENTER' : 'LEFT';
      }

      if (justify === 'Between') {
        addIconPlaceholder(comp, 'Medium');
      }

      components.push(comp);
    }
  }

  return combineVariants(components, 'Button/FullWidth', 2);
}
