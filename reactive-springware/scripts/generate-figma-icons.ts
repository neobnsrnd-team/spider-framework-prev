/**
 * @file generate-figma-icons.ts
 * @description lucide-react 패키지의 모든 아이콘을 SVG 문자열로 추출해
 *              figma-plugin/icons-generated.ts 파일을 자동 생성한다.
 *
 * @usage
 *   npm run generate:icons
 *
 * @outputs
 *   figma-plugin/icons-generated.ts  — ICON_SVGS_GENERATED Record<string, string>
 *
 * @note
 *   - 이 스크립트는 빌드 타임에만 실행되며, 플러그인 번들에는 포함되지 않는다.
 *   - lucide-react 버전 업 시 재실행하면 아이콘 목록이 갱신된다.
 *   - {COLOR} 플레이스홀더를 삽입해 Figma createIcons 런타임에서 색상을 치환한다.
 */

import * as lucide from 'lucide-react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createElement } from 'react';
import { writeFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname  = dirname(__filename);

/* lucide-react에서 아이콘 컴포넌트만 추출.
   v0.4+ 이후 모든 아이콘은 forwardRef 객체(typeof === 'object')로 export된다.
   *Icon suffix alias(e.g. ChevronRightIcon)는 중복이므로 제외한다. */
const iconNames = (Object.keys(lucide) as string[]).filter(k => {
  const val = (lucide as Record<string, unknown>)[k];
  return (
    val !== null &&
    typeof val === 'object' &&
    /^[A-Z]/.test(k) &&
    !k.endsWith('Icon')
  );
});

console.log(`📦 lucide-react 아이콘 ${iconNames.length}개 발견`);

const entries: string[] = iconNames.map(name => {
  /* forwardRef 객체이므로 any로 단언해 createElement에 전달 */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const Icon = (lucide as Record<string, unknown>)[name] as any;

  /* renderToStaticMarkup으로 SVG 문자열 추출 후 {COLOR} 치환 삽입 */
  let svg: string;
  try {
    svg = renderToStaticMarkup(
      createElement(Icon, { color: '{COLOR}', size: 24, strokeWidth: 2 }),
    );
  } catch {
    /* 렌더 실패 시 빈 SVG fallback */
    svg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"></svg>`;
  }

  /* 백틱·백슬래시 이스케이프 */
  const escaped = svg.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$\{/g, '\\${');
  return `  ${name}: \`${escaped}\``;
});

const banner = [
  '// AUTO-GENERATED — 직접 편집하지 마세요.',
  '// 갱신: npm run generate:icons (scripts/generate-figma-icons.ts)',
  `// lucide-react 아이콘 수: ${iconNames.length}`,
].join('\n');

const output = `${banner}\nexport const ICON_SVGS_GENERATED: Record<string, string> = {\n${entries.join(',\n')},\n};\n\nexport type GeneratedIconName = keyof typeof ICON_SVGS_GENERATED;\n`;

const outPath = resolve(__dirname, '../figma-plugin/icons-generated.ts');
writeFileSync(outPath, output, 'utf-8');

console.log(`✅ ${outPath} 생성 완료 (${iconNames.length}개)`);
