/**
 * @file createVariables.ts
 * @description Figma Variables 패널에 디자인 토큰 변수를 일괄 등록(upsert)한다.
 *
 * tokens.ts에 선언된 모든 경로 상수(COLOR_VAR / SIZE_VAR / FONT_VAR)를 기반으로
 * 아래 두 컬렉션을 생성하거나 값을 업데이트한다.
 *
 * ┌─ Primitives 컬렉션 ───────────────────────────────────────────────┐
 * │  spacing/*, radius/*, text/{size}/fontSize|lineHeight|letterSpacing │
 * │  font/sans, font/numeric, font/icon                                 │
 * │  변수 타입: FLOAT (숫자) / STRING (폰트 패밀리)                      │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * ┌─ Semantic 컬렉션 ────────────────────────────────────────────────┐
 * │  color/surface/*, color/border/*, color/text/*, color/brand/*,    │
 * │  color/danger/*, color/success/*, color/warning/*, color/primary/* │
 * │  color/info/*, color/brand/5|10|20|shadow                          │
 * │  변수 타입: COLOR (RGBA 0~1)                                        │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * 이미 동일한 이름의 컬렉션·변수가 존재하면 값만 업데이트(upsert)한다.
 * design-tokens/globals.css → tokens.ts → 이 파일 순서로 동기화하면
 * Tokens Studio 없이도 Figma Variables를 최신 상태로 유지할 수 있다.
 *
 * @example
 *  // manifest.json menu 커맨드로 실행
 *  figma.command === 'createVariables'  →  createVariables() 호출
 */

import {
  BRAND, COLOR,
  SPACING, RADIUS,
  FONT_SIZE, LINE_HEIGHT, LETTER_SPACING,
  FONT_FAMILY,
  COLOR_VAR, SIZE_VAR, FONT_VAR,
} from './tokens';

/* ── 타입 ────────────────────────────────────────────────────── */

type RGBA = { r: number; g: number; b: number; a: number };

/** RGB(0~1)에 alpha를 더해 Figma COLOR 변수 값 형식으로 변환 */
function rgba(rgb: { r: number; g: number; b: number }, a = 1): RGBA {
  return { r: rgb.r, g: rgb.g, b: rgb.b, a };
}

/* ── 변수 정의 목록 ──────────────────────────────────────────── */

/**
 * Semantic 컬렉션에 등록할 COLOR 변수 전체 목록.
 * path는 COLOR_VAR 상수와 1:1 대응한다.
 * brandShadow / brandBg(brand/10) / brandPrimary5 / brandPrimary20 은
 * 투명도를 포함한 RGBA로 등록한다.
 */
const COLOR_DEFINITIONS: Array<{ path: string; value: RGBA }> = [
  /* ── Surface ───────────────────────────────────────────── */
  { path: COLOR_VAR.surface,          value: rgba(COLOR.surface) },
  { path: COLOR_VAR.surfacePage,      value: rgba(COLOR.surfacePage) },
  { path: COLOR_VAR.surfaceSubtle,    value: rgba(COLOR.surfaceSubtle) },
  { path: COLOR_VAR.surfaceRaised,    value: rgba(COLOR.surfaceRaised) },

  /* ── Border ─────────────────────────────────────────────── */
  { path: COLOR_VAR.border,           value: rgba(COLOR.border) },
  { path: COLOR_VAR.borderSubtle,     value: rgba(COLOR.borderSubtle) },
  { path: COLOR_VAR.borderFocus,      value: rgba(COLOR.borderFocus) },

  /* ── Text ───────────────────────────────────────────────── */
  { path: COLOR_VAR.textHeading,      value: rgba(COLOR.textHeading) },
  { path: COLOR_VAR.textBase,         value: rgba(COLOR.textBase) },
  { path: COLOR_VAR.textLabel,        value: rgba(COLOR.textLabel) },
  { path: COLOR_VAR.textSecondary,    value: rgba(COLOR.textSecondary) },
  { path: COLOR_VAR.textMuted,        value: rgba(COLOR.textMuted) },
  { path: COLOR_VAR.textPlaceholder,  value: rgba(COLOR.textPlaceholder) },
  { path: COLOR_VAR.textDisabled,     value: rgba(COLOR.textDisabled) },

  /* ── Brand ──────────────────────────────────────────────── */
  { path: COLOR_VAR.brandPrimary,     value: rgba(BRAND.primary) },
  { path: COLOR_VAR.brandDark,        value: rgba(BRAND.dark) },
  { path: COLOR_VAR.brandDarker,      value: rgba(BRAND.darker) },
  { path: COLOR_VAR.brandAlt,         value: rgba(BRAND.alt) },
  /* shadow = brand/primary 25% opacity */
  { path: COLOR_VAR.brandShadow,      value: rgba(BRAND.primary, 0.25) },
  { path: COLOR_VAR.brandFg,          value: rgba(BRAND.fg) },
  { path: COLOR_VAR.brandText,        value: rgba(BRAND.text) },
  /* color/brand/10 = brand/primary 10% opacity */
  { path: COLOR_VAR.brandBg,          value: rgba(BRAND.primary, 0.10) },

  /* ── Danger ─────────────────────────────────────────────── */
  { path: COLOR_VAR.danger,           value: rgba(COLOR.danger) },
  { path: COLOR_VAR.dangerDark,       value: rgba(COLOR.dangerDark) },
  { path: COLOR_VAR.dangerDarker,     value: rgba(COLOR.dangerDarker) },
  { path: COLOR_VAR.dangerBadge,      value: rgba(COLOR.dangerBadge) },
  { path: COLOR_VAR.dangerSurface,    value: rgba(COLOR.dangerSurface) },
  { path: COLOR_VAR.dangerBorder,     value: rgba(COLOR.dangerBorder) },
  { path: COLOR_VAR.dangerText,       value: rgba(COLOR.dangerText) },

  /* ── Success ────────────────────────────────────────────── */
  { path: COLOR_VAR.success,          value: rgba(COLOR.success) },
  { path: COLOR_VAR.successSurface,   value: rgba(COLOR.successSurface) },
  { path: COLOR_VAR.successBorder,    value: rgba(COLOR.successBorder) },
  { path: COLOR_VAR.successText,      value: rgba(COLOR.successText) },

  /* ── Warning ────────────────────────────────────────────── */
  { path: COLOR_VAR.warning,          value: rgba(COLOR.warning) },
  { path: COLOR_VAR.warningSurface,   value: rgba(COLOR.warningSurface) },
  { path: COLOR_VAR.warningBorder,    value: rgba(COLOR.warningBorder) },
  { path: COLOR_VAR.warningText,      value: rgba(COLOR.warningText) },

  /* ── Primary ────────────────────────────────────────────── */
  { path: COLOR_VAR.primary,          value: rgba(COLOR.primary) },
  { path: COLOR_VAR.primaryDark,      value: rgba(COLOR.primaryDark) },
  { path: COLOR_VAR.primarySurface,   value: rgba(COLOR.primarySurface) },
  { path: COLOR_VAR.primaryBorder,    value: rgba(COLOR.primaryBorder) },
  { path: COLOR_VAR.primaryText,      value: rgba(COLOR.primaryText) },

  /* ── Info (Badge Primary 전용 별칭) ─────────────────────── */
  /* color/primary/surface와 동일한 hex지만 별개의 Figma 변수 경로 */
  { path: COLOR_VAR.infoSurface,      value: rgba(COLOR.primarySurface) },

  /* ── Brand 투명도 단계 ──────────────────────────────────── */
  { path: COLOR_VAR.brandPrimary5,    value: rgba(BRAND.primary, 0.05) },
  { path: COLOR_VAR.brandPrimary20,   value: rgba(BRAND.primary, 0.20) },
];

/**
 * Primitives 컬렉션에 등록할 FLOAT 변수 전체 목록.
 * path는 SIZE_VAR 상수와 1:1 대응한다.
 */
const FLOAT_DEFINITIONS: Array<{ path: string; value: number }> = [
  /* ── Spacing (px) ───────────────────────────────────────── */
  { path: SIZE_VAR.spacing0,          value: SPACING['0'] },
  { path: SIZE_VAR.spacingPx,         value: SPACING.px },
  { path: SIZE_VAR.spacingXs,         value: SPACING.xs },
  { path: SIZE_VAR.spacingSm,         value: SPACING.sm },
  { path: SIZE_VAR.spacingMd,         value: SPACING.md },
  { path: SIZE_VAR.spacingStandard,   value: SPACING.standard },
  { path: SIZE_VAR.spacingLg,         value: SPACING.lg },
  { path: SIZE_VAR.spacingXl,         value: SPACING.xl },
  { path: SIZE_VAR.spacing2xl,        value: SPACING['2xl'] },
  { path: SIZE_VAR.spacing3xl,        value: SPACING['3xl'] },
  { path: SIZE_VAR.spacing4xl,        value: SPACING['4xl'] },
  { path: SIZE_VAR.spacingNav,        value: SPACING.nav },

  /* ── Radius (px) ────────────────────────────────────────── */
  { path: SIZE_VAR.radiusNone,        value: RADIUS.none },
  { path: SIZE_VAR.radiusXs,          value: RADIUS.xs },
  { path: SIZE_VAR.radiusSm,          value: RADIUS.sm },
  { path: SIZE_VAR.radiusMd,          value: RADIUS.md },
  { path: SIZE_VAR.radiusLg,          value: RADIUS.lg },
  { path: SIZE_VAR.radiusXl,          value: RADIUS.xl },
  { path: SIZE_VAR.radiusFull,        value: RADIUS.full },

  /* ── Font Size (px) ─────────────────────────────────────── */
  { path: SIZE_VAR.fontSizeXs,        value: FONT_SIZE.xs },
  { path: SIZE_VAR.fontSizeSm,        value: FONT_SIZE.sm },
  { path: SIZE_VAR.fontSizeBase,      value: FONT_SIZE.base },
  { path: SIZE_VAR.fontSizeLg,        value: FONT_SIZE.lg },
  { path: SIZE_VAR.fontSizeXl,        value: FONT_SIZE.xl },
  { path: SIZE_VAR.fontSize2xl,       value: FONT_SIZE['2xl'] },
  { path: SIZE_VAR.fontSize3xl,       value: FONT_SIZE['3xl'] },
  { path: SIZE_VAR.fontSize4xl,       value: FONT_SIZE['4xl'] },

  /* ── Line Height (px) ───────────────────────────────────── */
  { path: SIZE_VAR.lineHeightXs,      value: LINE_HEIGHT.xs },
  { path: SIZE_VAR.lineHeightSm,      value: LINE_HEIGHT.sm },
  { path: SIZE_VAR.lineHeightBase,    value: LINE_HEIGHT.base },
  { path: SIZE_VAR.lineHeightLg,      value: LINE_HEIGHT.lg },
  { path: SIZE_VAR.lineHeightXl,      value: LINE_HEIGHT.xl },
  { path: SIZE_VAR.lineHeight2xl,     value: LINE_HEIGHT['2xl'] },
  { path: SIZE_VAR.lineHeight3xl,     value: LINE_HEIGHT['3xl'] },
  { path: SIZE_VAR.lineHeight4xl,     value: LINE_HEIGHT['4xl'] },

  /* ── Letter Spacing (em 단위 — Figma는 그대로 사용) ──────── */
  { path: SIZE_VAR.letterSpacingXs,   value: LETTER_SPACING.xs },
  { path: SIZE_VAR.letterSpacingSm,   value: LETTER_SPACING.sm },
  { path: SIZE_VAR.letterSpacingBase, value: LETTER_SPACING.base },
  { path: SIZE_VAR.letterSpacingLg,   value: LETTER_SPACING.lg },
  { path: SIZE_VAR.letterSpacingXl,   value: LETTER_SPACING.xl },
  { path: SIZE_VAR.letterSpacing2xl,  value: LETTER_SPACING['2xl'] },
  { path: SIZE_VAR.letterSpacing3xl,  value: LETTER_SPACING['3xl'] },
  { path: SIZE_VAR.letterSpacing4xl,  value: LETTER_SPACING['4xl'] },
];

/**
 * Primitives 컬렉션에 등록할 STRING 변수 전체 목록.
 * path는 FONT_VAR 상수와 1:1 대응한다.
 */
const STRING_DEFINITIONS: Array<{ path: string; value: string }> = [
  { path: FONT_VAR.sans,    value: FONT_FAMILY.sans },
  { path: FONT_VAR.numeric, value: FONT_FAMILY.numeric },
  { path: FONT_VAR.icon,    value: FONT_FAMILY.icon },
];

/* ── 유틸리티 ────────────────────────────────────────────────── */

/**
 * 이름으로 컬렉션을 찾거나 없으면 새로 생성한다(upsert).
 * @param name - Figma Variables 패널에 표시될 컬렉션 이름
 */
async function upsertCollection(
  name: string,
): Promise<VariableCollection> {
  const collections = await figma.variables.getLocalVariableCollectionsAsync();
  return collections.find(c => c.name === name)
    ?? figma.variables.createVariableCollection(name);
}

/**
 * 컬렉션 안에서 이름으로 변수를 찾거나 없으면 새로 생성하고
 * defaultMode 값을 설정한다(upsert).
 *
 * @param collection - 대상 컬렉션
 * @param name       - 변수 경로 (예: 'spacing/md', 'color/brand/text')
 * @param type       - 변수 타입 ('COLOR' | 'FLOAT' | 'STRING')
 * @param value      - 설정할 값
 */
async function upsertVariable(
  collection: VariableCollection,
  name: string,
  type: VariableResolvedDataType,
  value: RGBA | number | string,
): Promise<Variable> {
  /* 같은 컬렉션 안에서 이름이 일치하는 변수를 찾는다 */
  const allVars = await figma.variables.getLocalVariablesAsync(type);
  const existing = allVars.find(
    v => v.variableCollectionId === collection.id && v.name === name,
  );

  const variable = existing
    ?? figma.variables.createVariable(name, collection, type);

  variable.setValueForMode(collection.defaultModeId, value);
  return variable;
}

/* ── 공개 API ────────────────────────────────────────────────── */

/**
 * Primitives 컬렉션(FLOAT + STRING)과 Semantic 컬렉션(COLOR)을 일괄 등록한다.
 * 이미 존재하는 변수는 값만 업데이트하므로 기존 바인딩이 깨지지 않는다.
 *
 * @returns 등록 완료 메시지 (성공한 변수 수 포함)
 */
export async function createVariables(): Promise<string> {
  const errors: string[] = [];

  /* 1. Primitives 컬렉션 — 숫자·폰트 변수 등록 */
  const primitives = await upsertCollection('Primitives');

  for (const def of FLOAT_DEFINITIONS) {
    try {
      await upsertVariable(primitives, def.path, 'FLOAT', def.value);
    } catch (err) {
      errors.push(def.path);
      figma.notify(`❌ '${def.path}' 변수 생성 오류: ${err instanceof Error ? err.message : String(err)}`, { error: true });
    }
  }
  for (const def of STRING_DEFINITIONS) {
    try {
      await upsertVariable(primitives, def.path, 'STRING', def.value);
    } catch (err) {
      errors.push(def.path);
      figma.notify(`❌ '${def.path}' 변수 생성 오류: ${err instanceof Error ? err.message : String(err)}`, { error: true });
    }
  }

  const primitivesCount = FLOAT_DEFINITIONS.length + STRING_DEFINITIONS.length - errors.length;

  /* 2. Semantic 컬렉션 — 색상 변수 등록 */
  const semantic = await upsertCollection('Semantic');

  for (const def of COLOR_DEFINITIONS) {
    try {
      await upsertVariable(semantic, def.path, 'COLOR', def.value);
    } catch (err) {
      errors.push(def.path);
      figma.notify(`❌ '${def.path}' 변수 생성 오류: ${err instanceof Error ? err.message : String(err)}`, { error: true });
    }
  }

  const semanticCount = COLOR_DEFINITIONS.length - errors.filter(p =>
    COLOR_DEFINITIONS.some(d => d.path === p),
  ).length;

  if (errors.length > 0) {
    return `⚠️ 변수 등록 완료 (일부 실패) — Primitives: ${primitivesCount}개, Semantic: ${semanticCount}개 / 실패: ${errors.length}개`;
  }
  return `✅ 변수 등록 완료 — Primitives: ${primitivesCount}개, Semantic: ${semanticCount}개`;
}
