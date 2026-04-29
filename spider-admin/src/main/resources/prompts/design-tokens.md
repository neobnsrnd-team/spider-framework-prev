# Design Tokens

> 이 파일은 `scripts/extract-design-tokens.ts`로 자동 생성됩니다. 직접 수정하지 마세요.
> `globals.css` 수정 후 `npx tsx scripts/extract-design-tokens.ts`를 실행하면 갱신됩니다.

## 사용 규칙

- 색상·간격·타이포는 반드시 CSS 변수(`var(--*)`) 또는 Tailwind 토큰 클래스를 사용할 것
- `#FFFFFF`, `16px` 같은 하드코딩 금지
- 브랜드 컬러(`--color-brand-*`)는 `[data-brand]` 속성으로 주입되므로 prop으로 노출하지 말 것

## Common Theme Tokens

Tailwind `@theme` 블록에 선언된 공통 고정 토큰. 도메인·브랜드 무관하게 항상 사용 가능.

### color — Brand (브랜드 연동 가변값)

| CSS 변수 | 값 |
|---------|-----|
| `--color-brand` | `var(--brand-primary)` |
| `--color-brand-alt` | `var(--brand-alt)` |
| `--color-brand-fg` | `var(--brand-fg)` |
| `--color-brand-text` | `var(--brand-text)` |
| `--color-brand-dark` | `var(--brand-dark)` |
| `--color-brand-darker` | `var(--brand-darker)` |
| `--color-brand-5` | `var(--brand-5)` |
| `--color-brand-10` | `var(--brand-10)` |
| `--color-brand-20` | `var(--brand-20)` |
| `--color-brand-shadow` | `var(--brand-shadow)` |

### color — Primary (시스템 고정 파란색)

| CSS 변수 | 값 |
|---------|-----|
| `--color-primary` | `#2563eb` |
| `--color-primary-surface` | `#eff6ff` |
| `--color-primary-border` | `#bfdbfe` |
| `--color-primary-text` | `#1e40af` |
| `--color-primary-dark` | `#1d4ed8` |

### color — Surface

| CSS 변수 | 값 |
|---------|-----|
| `--color-surface` | `#ffffff` |
| `--color-surface-subtle` | `#f8fafc` |
| `--color-surface-page` | `var(--brand-bg, var(--domain-page-bg, #f5f8f8))` |
| `--color-surface-raised` | `#f1f5f9` |

### color — Border

| CSS 변수 | 값 |
|---------|-----|
| `--color-border` | `#e2e8f0` |
| `--color-border-subtle` | `#f1f5f9` |
| `--color-border-focus` | `var(--brand-primary)` |

### color — Text

| CSS 변수 | 값 |
|---------|-----|
| `--color-text-heading` | `#0f172a` |
| `--color-text-base` | `#1e293b` |
| `--color-text-label` | `#334155` |
| `--color-text-secondary` | `#475569` |
| `--color-text-muted` | `#64748b` |
| `--color-text-placeholder` | `#94a3b8` |
| `--color-text-disabled` | `#cbd5e1` |

### color — Danger

| CSS 변수 | 값 |
|---------|-----|
| `--color-danger` | `#e11d48` |
| `--color-danger-dark` | `#c01742` |
| `--color-danger-darker` | `#a8103a` |
| `--color-danger-surface` | `#fff1f2` |
| `--color-danger-border` | `#fda4af` |
| `--color-danger-text` | `#be123c` |
| `--color-danger-badge` | `#ef4444` |

### color — Success

| CSS 변수 | 값 |
|---------|-----|
| `--color-success` | `#16a34a` |
| `--color-success-surface` | `#f0fdf4` |
| `--color-success-border` | `#86efac` |
| `--color-success-text` | `#15803d` |

### color — Warning

| CSS 변수 | 값 |
|---------|-----|
| `--color-warning` | `#d97706` |
| `--color-warning-surface` | `#fffbeb` |
| `--color-warning-border` | `#fcd34d` |
| `--color-warning-text` | `#b45309` |

### color — Info

| CSS 변수 | 값 |
|---------|-----|
| `--color-info-surface` | `#eff6ff` |

### Typography — Fonts

| CSS 변수 | 값 |
|---------|-----|
| `--font-sans` | `'Noto Sans KR', system-ui, sans-serif` |
| `--font-numeric` | `'Manrope', 'Noto Sans KR', system-ui, sans-serif` |
| `--font-icon` | `'Material Symbols Outlined'` |

### Typography — Text Sizes

| CSS 변수 | 값 |
|---------|-----|
| `--text-xs` | `12px` |
| `--text-xs--line-height` | `16px` |
| `--text-xs--letter-spacing` | `0px` |
| `--text-sm` | `14px` |
| `--text-sm--line-height` | `20px` |
| `--text-sm--letter-spacing` | `0px` |
| `--text-base` | `16px` |
| `--text-base--line-height` | `24px` |
| `--text-base--letter-spacing` | `0px` |
| `--text-lg` | `18px` |
| `--text-lg--line-height` | `28px` |
| `--text-lg--letter-spacing` | `-0.45px` |
| `--text-xl` | `20px` |
| `--text-xl--line-height` | `28px` |
| `--text-xl--letter-spacing` | `-0.5px` |
| `--text-2xl` | `24px` |
| `--text-2xl--line-height` | `32px` |
| `--text-2xl--letter-spacing` | `-0.3px` |
| `--text-3xl` | `30px` |
| `--text-3xl--line-height` | `40px` |
| `--text-3xl--letter-spacing` | `-0.75px` |
| `--text-4xl` | `36px` |
| `--text-4xl--line-height` | `44px` |
| `--text-4xl--letter-spacing` | `-0.9px` |

### Spacing

| CSS 변수 | 값 |
|---------|-----|
| `--spacing-px` | `1px` |
| `--spacing-0` | `0px` |
| `--spacing-xs` | `4px` |
| `--spacing-sm` | `8px` |
| `--spacing-md` | `12px` |
| `--spacing-standard` | `16px` |
| `--spacing-lg` | `20px` |
| `--spacing-xl` | `24px` |
| `--spacing-2xl` | `32px` |
| `--spacing-3xl` | `40px` |
| `--spacing-4xl` | `48px` |
| `--spacing-nav` | `80px` |

### Border Radius

| CSS 변수 | 값 |
|---------|-----|
| `--radius-none` | `0px` |
| `--radius-xs` | `4px` |
| `--radius-sm` | `8px` |
| `--radius-md` | `12px` |
| `--radius-lg` | `16px` |
| `--radius-xl` | `24px` |
| `--radius-full` | `9999px` |

### Box Shadow

| CSS 변수 | 값 |
|---------|-----|
| `--shadow-xs` | `0px 1px 2px 0px rgba(0, 0, 0, 0.05)` |
| `--shadow-sm` | `0px 1px 3px 0px rgba(0, 0, 0, 0.1), 0px 1px 2px -1px rgba(0, 0, 0, 0.1)` |
| `--shadow-md` | `0px 4px 6px -1px rgba(0, 0, 0, 0.1), 0px 2px 4px -2px rgba(0, 0, 0, 0.1)` |
| `--shadow-lg` | `0px 10px 15px -3px rgba(0, 0, 0, 0.1), 0px 4px 6px -4px rgba(0, 0, 0, 0.1)` |
| `--shadow-xl` | `0px 20px 25px -5px rgba(0, 0, 0, 0.1), 0px 8px 10px -6px rgba(0, 0, 0, 0.1)` |
| `--shadow-2xl` | `0px 25px 50px -12px rgba(0, 0, 0, 0.25)` |

### Transitions

| CSS 변수 | 값 |
|---------|-----|
| `--transition-fast` | `150ms ease-in-out` |
| `--transition-base` | `200ms ease-in-out` |
| `--transition-slow` | `300ms ease-in-out` |
| `--transition-spring` | `400ms cubic-bezier(0.34, 1.56, 0.64, 1)` |

### Breakpoints

| CSS 변수 | 값 |
|---------|-----|
| `--breakpoint-sm` | `640px` |
| `--breakpoint-md` | `768px` |
| `--breakpoint-lg` | `1024px` |
| `--breakpoint-xl` | `1280px` |
| `--breakpoint-2xl` | `1536px` |
| `--breakpoint-mobile` | `390px` |
| `--breakpoint-tablet` | `768px` |
| `--breakpoint-desktop` | `1280px` |

### Component Sizing

| CSS 변수 | 값 |
|---------|-----|
| `--nav-top-height` | `56px` |
| `--nav-bottom-height` | `60px` |

### Z-Index

| CSS 변수 | 값 |
|---------|-----|
| `--z-base` | `0` |
| `--z-raised` | `1` |
| `--z-overlay` | `10` |
| `--z-sticky` | `20` |
| `--z-modal` | `50` |
| `--z-toast` | `100` |

## Brand Tokens

`data-brand` 속성으로 주입되는 브랜드별 가변 토큰.
컴포넌트 props에 색상 값을 직접 전달하지 말고 `--brand-*` 변수를 통해 참조할 것.

### 하나은행 (`[data-brand='hana']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#008485` |
| `--brand-alt` | `#14b8a6` |
| `--brand-fg` | `#ffffff` |
| `--brand-text` | `#008485` |
| `--brand-dark` | `#006e6f` |
| `--brand-darker` | `#005859` |
| `--brand-5` | `rgba(0, 132, 133, 0.05)` |
| `--brand-10` | `rgba(0, 132, 133, 0.1)` |
| `--brand-20` | `rgba(0, 132, 133, 0.2)` |
| `--brand-shadow` | `rgba(0, 132, 133, 0.25)` |
| `--brand-bg` | `#f5f8f8` |
| `--brand-name` | `'하나은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### IBK기업은행 (`[data-brand='ibk']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#0068b7` |
| `--brand-alt` | `#3b9ad9` |
| `--brand-fg` | `#ffffff` |
| `--brand-text` | `#0057a0` |
| `--brand-dark` | `#0057a0` |
| `--brand-darker` | `#004580` |
| `--brand-5` | `rgba(0, 104, 183, 0.05)` |
| `--brand-10` | `rgba(0, 104, 183, 0.1)` |
| `--brand-20` | `rgba(0, 104, 183, 0.2)` |
| `--brand-shadow` | `rgba(0, 104, 183, 0.25)` |
| `--brand-bg` | `#f4f7fb` |
| `--brand-name` | `'IBK기업은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### KB국민은행 (`[data-brand='kb']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#ffbc00` |
| `--brand-alt` | `#ffd740` |
| `--brand-fg` | `#1a1200` |
| `--brand-text` | `#7a4400` |
| `--brand-dark` | `#e6aa00` |
| `--brand-darker` | `#cc9600` |
| `--brand-5` | `rgba(255, 188, 0, 0.05)` |
| `--brand-10` | `rgba(255, 188, 0, 0.1)` |
| `--brand-20` | `rgba(255, 188, 0, 0.2)` |
| `--brand-shadow` | `rgba(255, 188, 0, 0.25)` |
| `--brand-bg` | `#fffdf0` |
| `--brand-name` | `'KB국민은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### NH농협은행 (`[data-brand='nh']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#00a859` |
| `--brand-alt` | `#eef300` |
| `--brand-fg` | `#ffffff` |
| `--brand-text` | `#007a40` |
| `--brand-dark` | `#008a49` |
| `--brand-darker` | `#006b38` |
| `--brand-5` | `rgba(0, 168, 89, 0.05)` |
| `--brand-10` | `rgba(0, 168, 89, 0.1)` |
| `--brand-20` | `rgba(0, 168, 89, 0.2)` |
| `--brand-shadow` | `rgba(0, 168, 89, 0.25)` |
| `--brand-bg` | `#f4fbf7` |
| `--brand-name` | `'NH농협은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### 신한은행 (`[data-brand='shinhan']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#0046ff` |
| `--brand-alt` | `#4d7fff` |
| `--brand-fg` | `#ffffff` |
| `--brand-text` | `#0033cc` |
| `--brand-dark` | `#0038cc` |
| `--brand-darker` | `#002b99` |
| `--brand-5` | `rgba(0, 70, 255, 0.05)` |
| `--brand-10` | `rgba(0, 70, 255, 0.1)` |
| `--brand-20` | `rgba(0, 70, 255, 0.2)` |
| `--brand-shadow` | `rgba(0, 70, 255, 0.25)` |
| `--brand-bg` | `#f4f6ff` |
| `--brand-name` | `'신한은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### 우리은행 (`[data-brand='woori']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#0067ac` |
| `--brand-alt` | `#3395d4` |
| `--brand-fg` | `#ffffff` |
| `--brand-text` | `#005490` |
| `--brand-dark` | `#005490` |
| `--brand-darker` | `#00416e` |
| `--brand-5` | `rgba(0, 103, 172, 0.05)` |
| `--brand-10` | `rgba(0, 103, 172, 0.1)` |
| `--brand-20` | `rgba(0, 103, 172, 0.2)` |
| `--brand-shadow` | `rgba(0, 103, 172, 0.25)` |
| `--brand-bg` | `#f4f8fc` |
| `--brand-name` | `'우리은행'` |
| `--shadow-primary` | `0px 8px 20px -4px var(--brand-shadow)` |

### 지로 기본 (`[data-brand='giro']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#5b21b6` |
| `--brand-alt` | `#8b5cf6` |
| `--brand-bg` | `#f9f7ff` |
| `--brand-name` | `'지로 기본'` |

### 보험 기본 (`[data-brand='insurance']`)

| CSS 변수 | 값 |
|---------|-----|
| `--brand-primary` | `#e03a1e` |
| `--brand-alt` | `#f97f6a` |
| `--brand-bg` | `#fff8f7` |
| `--brand-name` | `'보험 기본'` |

## Domain Tokens

`data-domain` 속성으로 주입되는 도메인별 토큰.

### banking (`[data-domain='banking']`)

| CSS 변수 | 값 |
|---------|-----|
| `--domain-page-bg` | `#f5f8f8` |

### card (`[data-domain='card']`)

| CSS 변수 | 값 |
|---------|-----|
| `--domain-page-bg` | `#f4f7ff` |
| `--domain-card-accent` | `var(--brand-alt)` |
| `--domain-card-accent-text` | `var(--brand-text)` |

### giro (`[data-domain='giro']`)

| CSS 변수 | 값 |
|---------|-----|
| `--domain-page-bg` | `#f9f7ff` |

### insurance (`[data-domain='insurance']`)

| CSS 변수 | 값 |
|---------|-----|
| `--domain-page-bg` | `#fff8f7` |

## Brand × Domain 오버라이드

브랜드와 도메인 조합에서만 적용되는 토큰 오버라이드.

### 하나은행 × card (`[data-brand='hana'][data-domain='card']`)

| CSS 변수 | 값 |
|---------|-----|
| `--domain-card-accent` | `#caee5d` |
| `--domain-card-accent-text` | `#546b00` |
