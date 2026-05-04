/**
 * @file extract-design-tokens.ts
 * @description design-tokens/globals.css를 파싱하여 design-tokens.md를 생성한다.
 *
 *   globals.css의 @theme 블록(공통 토큰), [data-brand='X'] 블록(브랜드별),
 *   [data-domain='X'] 블록(도메인별)에서 CSS 변수 선언을 추출하여
 *   마크다운 테이블로 변환한다.
 *
 *   JSON 파싱 방식 대비 장점:
 *   - globals.css가 실제 배포 소스이므로 항상 최신 상태를 반영
 *   - 모든 브랜드·도메인 토큰 자동 수집 (JSON 방식은 hana만 수동 지정)
 *   - 재귀 트리 탐색·필터 조건 없이 라인별 파싱으로 단순하게 처리
 *
 * @example
 *   npx tsx scripts/extract-design-tokens.ts
 */

import { readFileSync, mkdirSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const CSS_FILE = join(ROOT, 'design-tokens', 'globals.css');
const OUTPUT_DIR = join(ROOT, 'generated');
const OUTPUT_FILE = join(OUTPUT_DIR, 'design-tokens.md');
// reactPlatform 프로젝트의 Claude 시스템 프롬프트 디렉토리에도 동기화
const REACT_PLATFORM_OUTPUT_FILE = join(
  ROOT,
  '..',
  'reactPlatform',
  'src',
  'main',
  'resources',
  'prompts',
  'design-tokens.md',
);

type TokenEntry = { variable: string; value: string };

/** 브랜드 키 → 한글 이름 */
const BRAND_LABELS: Record<string, string> = {
  hana: '하나은행',
  ibk: 'IBK기업은행',
  kb: 'KB국민은행',
  nh: 'NH농협은행',
  shinhan: '신한은행',
  woori: '우리은행',
  giro: '지로 기본',
  insurance: '보험 기본',
};

/**
 * @theme 블록 내 변수 prefix → 섹션 레이블.
 * 배열 순서가 곧 마크다운 출력 순서다.
 */
const THEME_GROUPS: Array<{ prefix: string; label: string }> = [
  { prefix: '--color-brand',   label: 'color — Brand (브랜드 연동 가변값)' },
  { prefix: '--color-primary', label: 'color — Primary (시스템 고정 파란색)' },
  { prefix: '--color-surface', label: 'color — Surface' },
  { prefix: '--color-border',  label: 'color — Border' },
  { prefix: '--color-text',    label: 'color — Text' },
  { prefix: '--color-danger',  label: 'color — Danger' },
  { prefix: '--color-success', label: 'color — Success' },
  { prefix: '--color-warning', label: 'color — Warning' },
  { prefix: '--color-info',    label: 'color — Info' },
  { prefix: '--font-',         label: 'Typography — Fonts' },
  { prefix: '--text-',         label: 'Typography — Text Sizes' },
  { prefix: '--spacing-',      label: 'Spacing' },
  { prefix: '--radius-',       label: 'Border Radius' },
  { prefix: '--shadow-',       label: 'Box Shadow' },
  { prefix: '--transition-',   label: 'Transitions' },
  { prefix: '--breakpoint-',   label: 'Breakpoints' },
  { prefix: '--nav-',          label: 'Component Sizing' },
  { prefix: '--z-',            label: 'Z-Index' },
];

/**
 * CSS 변수 선언 라인에서 변수명과 값을 추출한다.
 * @example "  --color-brand: var(--brand-primary);" → { variable: "--color-brand", value: "var(--brand-primary)" }
 */
function extractVar(line: string): TokenEntry | null {
  const match = line.match(/^\s*(--[\w-]+)\s*:\s*(.+?)\s*;/);
  if (!match) return null;
  return { variable: match[1], value: match[2] };
}

/**
 * 토큰 배열을 마크다운 테이블 행 배열로 변환한다.
 * 토큰이 없으면 빈 배열을 반환한다.
 */
function toTable(tokens: TokenEntry[]): string[] {
  if (tokens.length === 0) return [];
  return [
    '| CSS 변수 | 값 |',
    '|---------|-----|',
    ...tokens.map(({ variable, value }) => `| \`${variable}\` | \`${value}\` |`),
    '',
  ];
}

function main() {
  const rawCss = readFileSync(CSS_FILE, 'utf-8');

  // CSS 블록 주석을 제거한다.
  // 미구현 브랜드 선택자를 주석으로 남겨둔 경우 (예: /* [data-brand='kb'] { ... } */)
  // 해당 패턴이 블록 진입 감지에 오탐되는 것을 방지한다.
  const css = rawCss.replace(/\/\*[\s\S]*?\*\//g, '');
  const lines = css.split('\n');

  // ── 수집 버킷 ────────────────────────────────────────────────────
  const themeTokens: TokenEntry[] = [];
  const brandTokens = new Map<string, TokenEntry[]>();
  const domainTokens = new Map<string, TokenEntry[]>();
  const brandDomainTokens = new Map<string, TokenEntry[]>(); // key: "brand+domain"

  // ── 파싱 상태 ────────────────────────────────────────────────────
  type BlockKind = 'theme' | 'brand' | 'domain' | 'brand-domain' | null;
  let blockKind: BlockKind = null;
  let blockBrand = '';
  let blockDomain = '';
  // 블록 내부 중첩 깊이. 0 = 블록 직속 자식, -1 = 블록 탈출
  let braceDepth = 0;

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    // ── 블록 외부: 새 블록 진입 감지 ─────────────────────────────
    if (blockKind === null) {
      if (/@theme\s*\{/.test(trimmed)) {
        blockKind = 'theme';
        braceDepth = 0;
        continue;
      }

      // [data-brand='X'][data-domain='Y'] { — 복합 선택자는 단순 brand보다 먼저 검사
      // 작은따옴표·큰따옴표 모두 허용 (CSS 작성 스타일에 따라 혼용될 수 있음)
      const bdMatch = trimmed.match(/^\[data-brand=['"](\w+)['"]\]\[data-domain=['"](\w+)['"]\]\s*\{/);
      if (bdMatch) {
        blockKind = 'brand-domain';
        blockBrand = bdMatch[1];
        blockDomain = bdMatch[2];
        braceDepth = 0;
        const key = `${blockBrand}+${blockDomain}`;
        if (!brandDomainTokens.has(key)) brandDomainTokens.set(key, []);
        continue;
      }

      // 작은따옴표·큰따옴표 모두 허용
      const bMatch = trimmed.match(/^\[data-brand=['"](\w+)['"]\]\s*\{/);
      if (bMatch) {
        blockKind = 'brand';
        blockBrand = bMatch[1];
        braceDepth = 0;
        if (!brandTokens.has(blockBrand)) brandTokens.set(blockBrand, []);
        continue;
      }

      // 작은따옴표·큰따옴표 모두 허용
      const dMatch = trimmed.match(/^\[data-domain=['"](\w+)['"]\]\s*\{/);
      if (dMatch) {
        blockKind = 'domain';
        blockDomain = dMatch[1];
        braceDepth = 0;
        if (!domainTokens.has(blockDomain)) domainTokens.set(blockDomain, []);
        continue;
      }

      // :root, @import, @layer base, @utility, @source 등 → 무시
      continue;
    }

    // ── 블록 내부: 중첩 깊이 추적 ────────────────────────────────
    // 한 줄에 { 와 } 가 모두 있을 수 있으므로 각각 세어 net 변화량을 적용한다.
    const opens  = (trimmed.match(/\{/g) || []).length;
    const closes = (trimmed.match(/\}/g) || []).length;
    braceDepth += opens - closes;

    if (braceDepth < 0) {
      // 최외곽 } 를 만남 → 블록 종료
      blockKind = null;
      blockBrand = '';
      blockDomain = '';
      braceDepth = 0;
      continue;
    }

    // 중첩 블록 내부(depth > 0)에는 CSS 변수 선언이 없으므로 스킵
    if (braceDepth !== 0) continue;

    // ── CSS 변수 추출 ─────────────────────────────────────────────
    const entry = extractVar(line);
    if (!entry) continue;

    switch (blockKind) {
      case 'theme':
        themeTokens.push(entry);
        break;
      case 'brand':
        brandTokens.get(blockBrand)!.push(entry);
        break;
      case 'domain':
        domainTokens.get(blockDomain)!.push(entry);
        break;
      case 'brand-domain': {
        const key = `${blockBrand}+${blockDomain}`;
        brandDomainTokens.get(key)!.push(entry);
        break;
      }
    }
  }

  // ── 마크다운 생성 ─────────────────────────────────────────────────
  const out: string[] = [
    '# Design Tokens',
    '',
    '> 이 파일은 `scripts/extract-design-tokens.ts`로 자동 생성됩니다. 직접 수정하지 마세요.',
    '> `globals.css` 수정 후 `npx tsx scripts/extract-design-tokens.ts`를 실행하면 갱신됩니다.',
    '',
    '## 사용 규칙',
    '',
    '- 색상·간격·타이포는 반드시 CSS 변수(`var(--*)`) 또는 Tailwind 토큰 클래스를 사용할 것',
    '- `#FFFFFF`, `16px` 같은 하드코딩 금지',
    '- 브랜드 컬러(`--color-brand-*`)는 `[data-brand]` 속성으로 주입되므로 prop으로 노출하지 말 것',
    '',
  ];

  // ── @theme 공통 토큰 ──────────────────────────────────────────────
  out.push('## Common Theme Tokens');
  out.push('');
  out.push('Tailwind `@theme` 블록에 선언된 공통 고정 토큰. 도메인·브랜드 무관하게 항상 사용 가능.');
  out.push('');

  // prefix 기반으로 그룹핑. 매핑되지 않은 토큰은 "기타"로 처리
  const coveredVars = new Set<string>();
  for (const { prefix, label } of THEME_GROUPS) {
    const group = themeTokens.filter(({ variable }) => variable.startsWith(prefix));
    if (group.length === 0) continue;
    group.forEach(({ variable }) => coveredVars.add(variable));
    out.push(`### ${label}`);
    out.push('');
    out.push(...toTable(group));
  }

  const remainder = themeTokens.filter(({ variable }) => !coveredVars.has(variable));
  if (remainder.length > 0) {
    out.push('### 기타');
    out.push('');
    out.push(...toTable(remainder));
  }

  // ── 브랜드 토큰 ────────────────────────────────────────────────────
  out.push('## Brand Tokens');
  out.push('');
  out.push('`data-brand` 속성으로 주입되는 브랜드별 가변 토큰.');
  out.push('컴포넌트 props에 색상 값을 직접 전달하지 말고 `--brand-*` 변수를 통해 참조할 것.');
  out.push('');

  for (const [brand, tokens] of brandTokens) {
    const label = BRAND_LABELS[brand] ?? brand;
    out.push(`### ${label} (\`[data-brand='${brand}']\`)`);
    out.push('');
    out.push(...toTable(tokens));
  }

  // ── 도메인 토큰 ────────────────────────────────────────────────────
  if (domainTokens.size > 0) {
    out.push('## Domain Tokens');
    out.push('');
    out.push('`data-domain` 속성으로 주입되는 도메인별 토큰.');
    out.push('');

    for (const [domain, tokens] of domainTokens) {
      out.push(`### ${domain} (\`[data-domain='${domain}']\`)`);
      out.push('');
      out.push(...toTable(tokens));
    }
  }

  // ── 브랜드 × 도메인 오버라이드 ────────────────────────────────────
  if (brandDomainTokens.size > 0) {
    out.push('## Brand × Domain 오버라이드');
    out.push('');
    out.push('브랜드와 도메인 조합에서만 적용되는 토큰 오버라이드.');
    out.push('');

    for (const [key, tokens] of brandDomainTokens) {
      const [brand, domain] = key.split('+');
      const label = BRAND_LABELS[brand] ?? brand;
      out.push(`### ${label} × ${domain} (\`[data-brand='${brand}'][data-domain='${domain}']\`)`);
      out.push('');
      out.push(...toTable(tokens));
    }
  }

  const content = out.join('\n');
  const totalTokens =
    themeTokens.length +
    [...brandTokens.values()].reduce((sum, t) => sum + t.length, 0) +
    [...domainTokens.values()].reduce((sum, t) => sum + t.length, 0) +
    [...brandDomainTokens.values()].reduce((sum, t) => sum + t.length, 0);

  // ── 파일 출력 ──────────────────────────────────────────────────────
  mkdirSync(OUTPUT_DIR, { recursive: true });
  writeFileSync(OUTPUT_FILE, content, 'utf-8');
  console.log(`✅ design-tokens.md 생성 완료 (${totalTokens}개 토큰)`);
  console.log(`   출력: ${OUTPUT_FILE}`);

  // reactPlatform 프로젝트의 prompts 디렉토리에도 동기화한다.
  try {
    writeFileSync(REACT_PLATFORM_OUTPUT_FILE, content, 'utf-8');
    console.log(`   동기화: ${REACT_PLATFORM_OUTPUT_FILE}`);
  } catch {
    console.warn(`   ⚠️  reactPlatform 동기화 실패 (경로 확인 필요): ${REACT_PLATFORM_OUTPUT_FILE}`);
  }
}

main();
