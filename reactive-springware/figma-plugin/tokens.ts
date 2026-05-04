/**
 * @file tokens.ts
 * @description design-tokens/globals.css에 정의된 CSS 변수를 이 플러그인에서 사용하는 두 가지 형태로 관리한다.
 *
 * ┌─ 값 상수 (Figma Plugin API용 런타임 값) ──────────────────────────────────┐
 * │  BRAND, COLOR   — CSS --color-* / --brand-* 를 RGB(0~1)로 변환한 값      │
 * │  SPACING        — CSS --spacing-* 를 px 숫자로 저장한 값                  │
 * │  RADIUS         — CSS --radius-* 를 px 숫자로 저장한 값                   │
 * │  FONT_SIZE      — CSS --text-* 를 px 숫자로 저장한 값                     │
 * │  LINE_HEIGHT    — CSS --text-*--line-height 를 px 숫자로 저장한 값        │
 * │  LETTER_SPACING — CSS --text-*--letter-spacing 를 숫자로 저장한 값        │
 * │  FONT_FAMILY    — CSS --font-* 의 폰트 패밀리 문자열 fallback 값           │
 * └────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─ 변수 경로 상수 (Figma Variables 바인딩용 경로) ────────────────────────────┐
 * │  COLOR_VAR — CSS --color-* / --brand-* 에 대응하는 Figma Color 변수 경로   │
 * │  SIZE_VAR  — CSS --spacing-* / --radius-* / --text-*에 대응하는          │
 * │              Figma Number 변수 경로                                      │
 * │  FONT_VAR  — CSS --font-* 에 대응하는 Figma STRING 변수 경로               │
 * │                                                                         │
 * │  경로 명명 규칙: CSS 변수의 -- 접두사 제거 후 - -> / 로 치환                   │
 * │  예) --spacing-md -> spacing/md  /  --text-sm -> text/sm/fontSize        │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * 값 상수는 Figma 변수를 찾지 못할 때의 fallback으로도 사용된다.
 * Figma API는 색상을 0~1 범위 RGB로 받으므로 hex 값을 255로 나눠 변환한다.
 */

export type RGB = { r: number; g: number; b: number };

/** hex -> 0~1 범위 RGB 변환 헬퍼 */
function hex(h: string): RGB {
  const v = parseInt(h.replace('#', ''), 16);
  return { r: ((v >> 16) & 255) / 255, g: ((v >> 8) & 255) / 255, b: (v & 255) / 255 };
}

/** --brand-* 토큰 (하나은행 기본값) */
export const BRAND = {
  primary: hex('#008485'),
  dark:    hex('#006e6f'),
  darker:  hex('#005859'),
  alt:     hex('#14b8a6'), // color/brand/alt — 보조 브랜드 컬러
  fg:      hex('#ffffff'),
  text:    hex('#008485'),
  bg:      hex('#f5f8f8'),
  /* shadow: #008485 at 25% opacity — RGB는 primary와 동일, opacity는 사용 시 직접 지정 */
};

/** --color-* 토큰 */
export const COLOR = {
  surface:        hex('#ffffff'),
  surfacePage:    hex('#f5f8f8'), // color/surface/page — 페이지 루트 배경 (domain/page/bg)
  surfaceSubtle:  hex('#f8fafc'),
  surfaceRaised:  hex('#f1f5f9'),
  border:         hex('#e2e8f0'),
  borderSubtle:   hex('#f1f5f9'),
  borderFocus:    hex('#008485'), // color/border/focus — 포커스 링 (= brand/primary)

  textHeading:    hex('#0f172a'),
  textBase:       hex('#1e293b'),
  textLabel:      hex('#334155'),
  textSecondary:  hex('#475569'),
  textMuted:      hex('#64748b'),
  textPlaceholder:hex('#94a3b8'),
  textDisabled:   hex('#cbd5e1'),

  danger:         hex('#e11d48'),
  dangerDark:     hex('#c01742'),
  dangerDarker:   hex('#a8103a'),
  dangerBadge:    hex('#ef4444'),
  dangerSurface:  hex('#fff1f2'),
  dangerBorder:   hex('#fda4af'),
  dangerText:     hex('#be123c'),

  success:        hex('#16a34a'),
  successSurface: hex('#f0fdf4'),
  successBorder:  hex('#86efac'),
  successText:    hex('#15803d'),

  warning:        hex('#d97706'),
  warningSurface: hex('#fffbeb'),
  warningBorder:  hex('#fcd34d'),
  warningText:    hex('#b45309'),

  primary:        hex('#2563eb'),
  primaryDark:    hex('#1d4ed8'),
  primarySurface: hex('#eff6ff'),
  primaryBorder:  hex('#bfdbfe'),
  primaryText:    hex('#1e40af'),
};

/** --spacing-* 토큰 (px) */
export const SPACING = {
  '0':      0,  // spacing/0
  px:       1,  // spacing/px
  xs:       4,
  sm:       8,
  md:       12,
  standard: 16,
  lg:       20,
  xl:       24,
  '2xl':    32,
  '3xl':    40,
  '4xl':    48,
  nav:      80,
} as const;

/** --radius-* 토큰 (px) */
export const RADIUS = {
  none: 0,   // radius/none
  xs:   4,
  sm:   8,
  md:   12,
  lg:   16,
  xl:   24,
  full: 999, // Figma에서 9999는 overflow 문제가 생길 수 있어 999로 제한
} as const;

/** --text-fontSize-* 토큰 (px) */
export const FONT_SIZE = {
  xs:   12,
  sm:   14,
  base: 16,
  lg:   18,
  xl:   20,
  '2xl':24,
  '3xl':30,
  '4xl':36,
} as const;

/** --text-lineHeight-* 토큰 (px) */
export const LINE_HEIGHT = {
  xs:   16,
  sm:   20,
  base: 24,
  lg:   28,
  xl:   28,
  '2xl':32,
  '3xl':40,
  '4xl':44,
} as const;

/** --text-letterSpacing-* 토큰 (em 단위 — Figma는 px 환산 없이 그대로 사용) */
export const LETTER_SPACING = {
  xs:   0,
  sm:   0,
  base: 0,
  lg:   -0.45,
  xl:   -0.5,
  '2xl':-0.3,
  '3xl':-0.75,
  '4xl':-0.9,
} as const;

/**
 * 폰트 패밀리 fallback 값.
 * applyText()의 fontName.family에 사용하며, Figma 변수 바인딩 실패 시 이 값이 유지된다.
 */
export const FONT_FAMILY = {
  sans:    'Noto Sans KR',           // 본문·레이블 기본 폰트
  numeric: 'Manrope',                // 금액·숫자 전용 폰트
  icon:    'Material Symbols Outlined', // 아이콘 폰트
} as const;

/**
 * Figma STRING 변수 전체 경로 상수 (Primitives 컬렉션 > font 그룹).
 * design-tokens/globals.css의 --font-* CSS 변수를 Figma Variables 경로로 매핑한다.
 * 각 항목의 실제 폰트 패밀리 문자열은 FONT_FAMILY에서 확인한다.
 */
export const FONT_VAR = {
  sans:    'font/sans',
  numeric: 'font/numeric',
  icon:    'font/icon',
} as const;

/** 컴포넌트 캔버스 배치 간격 */
export const CANVAS_GAP = 48;

/**
 * Figma Number 변수 전체 경로 상수 (Primitives 컬렉션).
 * design-tokens/globals.css의 --spacing-* / --radius-* / --text-* CSS 변수를
 * Figma Variables 경로로 매핑한다.
 *
 * 각 항목의 실제 값은 같은 파일의 아래 상수에서 확인한다 (주석 중복을 방지하기 위해 생략):
 *   spacing/{size}              -> SPACING
 *   radius/{size}               -> RADIUS
 *   text/{size}/fontSize        -> FONT_SIZE
 *   text/{size}/lineHeight      -> LINE_HEIGHT
 *   text/{size}/letterSpacing   -> LETTER_SPACING
 */
export const SIZE_VAR = {
  /* ── Spacing ───────────────────────────────────── */
  spacing0:        'spacing/0',
  spacingPx:       'spacing/px',
  spacingXs:       'spacing/xs',
  spacingSm:       'spacing/sm',
  spacingMd:       'spacing/md',
  spacingStandard: 'spacing/standard',
  spacingLg:       'spacing/lg',
  spacingXl:       'spacing/xl',
  spacing2xl:      'spacing/2xl',
  spacing3xl:      'spacing/3xl',
  spacing4xl:      'spacing/4xl',
  spacingNav:      'spacing/nav',

  /* ── Radius ────────────────────────────────────── */
  radiusNone: 'radius/none',
  radiusXs:   'radius/xs',
  radiusSm:   'radius/sm',
  radiusMd:   'radius/md',
  radiusLg:   'radius/lg',
  radiusXl:   'radius/xl',
  radiusFull: 'radius/full',

  /* ── Text / fontSize ───────────────────────────── */
  fontSizeXs:   'text/xs/fontSize',
  fontSizeSm:   'text/sm/fontSize',
  fontSizeBase: 'text/base/fontSize',
  fontSizeLg:   'text/lg/fontSize',
  fontSizeXl:   'text/xl/fontSize',
  fontSize2xl:  'text/2xl/fontSize',
  fontSize3xl:  'text/3xl/fontSize',
  fontSize4xl:  'text/4xl/fontSize',

  /* ── Text / lineHeight ─────────────────────────── */
  lineHeightXs:   'text/xs/lineHeight',
  lineHeightSm:   'text/sm/lineHeight',
  lineHeightBase: 'text/base/lineHeight',
  lineHeightLg:   'text/lg/lineHeight',
  lineHeightXl:   'text/xl/lineHeight',
  lineHeight2xl:  'text/2xl/lineHeight',
  lineHeight3xl:  'text/3xl/lineHeight',
  lineHeight4xl:  'text/4xl/lineHeight',

  /* ── Text / letterSpacing ──────────────────────── */
  letterSpacingXs:   'text/xs/letterSpacing',
  letterSpacingSm:   'text/sm/letterSpacing',
  letterSpacingBase: 'text/base/letterSpacing',
  letterSpacingLg:   'text/lg/letterSpacing',
  letterSpacingXl:   'text/xl/letterSpacing',
  letterSpacing2xl:  'text/2xl/letterSpacing',
  letterSpacing3xl:  'text/3xl/letterSpacing',
  letterSpacing4xl:  'text/4xl/letterSpacing',
} as const;

/**
 * Figma Color 변수 전체 경로 상수 (Semantic 컬렉션).
 * setFillWithVar()에 전달하는 variableName 값으로 사용한다.
 * 컬렉션명은 경로에 포함되지 않으며, color/{그룹}/{이름} 형식이다.
 */
export const COLOR_VAR = {
  /* ── Surface ───────────────────────────────── */
  surface:         'color/surface',         // #FFFFFF
  surfacePage:     'color/surface/page',    // #F5F8F8 — 페이지 루트 배경 (domain/page/bg)
  surfaceSubtle:   'color/surface/subtle',  // #F8FAFC
  surfaceRaised:   'color/surface/raised',  // #F1F5F9

  /* ── Border ────────────────────────────────── */
  border:          'color/border',          // #E2E8F0
  borderSubtle:    'color/border/subtle',   // #F1F5F9
  borderFocus:     'color/border/focus',    // #008485 — 포커스 링 (= brand/primary)

  /* ── Text ──────────────────────────────────── */
  textHeading:     'color/text/heading',    // #0F172A
  textBase:        'color/text/base',       // #1E293B
  textLabel:       'color/text/label',      // #334155
  textSecondary:   'color/text/secondary',  // #475569
  textMuted:       'color/text/muted',      // #64748B
  textPlaceholder: 'color/text/placeholder',// #94A3B8
  textDisabled:    'color/text/disabled',   // #CBD5E1

  /* ── Brand ─────────────────────────────────── */
  brandPrimary:    'color/brand',           // #008485
  brandDark:       'color/brand/dark',      // #006E6F
  brandDarker:     'color/brand/darker',    // #005859
  brandAlt:        'color/brand/alt',       // #14B8A6
  brandShadow:     'color/brand/shadow',    // #008485 25%
  brandFg:         'color/brand/fg',        // #FFFFFF
  brandText:       'color/brand/text',      // #008485
  brandBg:         'color/brand/10',        // #008485 10%

  /* ── Danger ─────────────────────────────────── */
  danger:          'color/danger',          // #E11D48
  dangerDark:      'color/danger/dark',     // #C01742
  dangerDarker:    'color/danger/darker',   // #A8103A
  dangerBadge:     'color/danger/badge',    // #EF4444
  dangerSurface:   'color/danger/surface',  // #FFF1F2
  dangerBorder:    'color/danger/border',   // #FDA4AF
  dangerText:      'color/danger/text',     // #BE123C

  /* ── Success ────────────────────────────────── */
  success:         'color/success',         // #16A34A
  successSurface:  'color/success/surface', // #F0FDF4
  successBorder:   'color/success/border',  // #86EFAC
  successText:     'color/success/text',    // #15803D

  /* ── Warning ────────────────────────────────── */
  warning:         'color/warning',         // #D97706
  warningSurface:  'color/warning/surface', // #FFFBEB
  warningBorder:   'color/warning/border',  // #FCD34D
  warningText:     'color/warning/text',    // #B45309

  /* ── Primary ───────────────────────────────── */
  primary:         'color/primary',         // #2563EB
  primaryDark:     'color/primary/dark',    // #1D4ED8
  primarySurface:  'color/primary/surface', // #EFF6FF
  primaryBorder:   'color/primary/border',  // #BFDBFE
  primaryText:     'color/primary/text',    // #1E40AF

  /* ── Info (color/info — Badge 전용 별칭) ───── */
  /* color/primary와 경로가 다른 별개 변수. semantic.json color.info 참조 */
  infoSurface:     'color/info/surface',    // #EFF6FF — Badge Primary 배경

  /* ── Brand 투명도 단계 ──────────────────────── */
  /* fallback은 opacity 미지원으로 BRAND.primary(불투명) 사용 */
  brandPrimary5:   'color/brand/5',         // brand/primary 5%
  brandPrimary20:  'color/brand/20',        // brand/primary 20%
} as const;
