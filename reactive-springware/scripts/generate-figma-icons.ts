/**
 * @file generate-figma-icons.ts
 * @description lucide-react 패키지의 모든 아이콘을 SVG 문자열로 추출해
 *              figma-plugin/utils/icons.ts 파일을 자동 생성한다.
 *
 * @usage
 *   npm run generate:icons
 *
 * @outputs
 *   figma-plugin/utils/icons.ts — ICON_SVGS, IconName, createIcon 포함
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

  /* renderToStaticMarkup으로 SVG 문자열 추출 후 {COLOR} 플레이스홀더 삽입 */
  let svg: string;
  try {
    svg = renderToStaticMarkup(
      createElement(Icon, { color: '{COLOR}', size: 24, strokeWidth: 2 }),
    );
  } catch {
    svg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"></svg>`;
  }

  /* 백틱·백슬래시 이스케이프 */
  const escaped = svg.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$\{/g, '\\${');
  return `  ${name}: \`${escaped}\``;
});

const banner = [
  '/// <reference types="@figma/plugin-typings" />',
  '// AUTO-GENERATED — 직접 편집하지 마세요.',
  '// 갱신: npm run generate:icons (scripts/generate-figma-icons.ts)',
  `// lucide-react 아이콘 수: ${iconNames.length}`,
].join('\n');

/* createIcon 함수와 toHex 헬퍼는 스크립트가 생성하는 utils/icons.ts에 포함된다.
   Figma API(figma.createNodeFromSvg)를 사용하므로 플러그인 런타임에서만 실행된다. */
const runtime = `
import type { RGB } from './tokens';

function toHex({ r, g, b }: RGB): string {
  const ch = (v: number) => Math.round(v * 255).toString(16).padStart(2, '0');
  return \`#\${ch(r)}\${ch(g)}\${ch(b)}\`;
}

export const ICON_SVGS: Record<string, string> = {
${entries.join(',\n')},
};

export type IconName = keyof typeof ICON_SVGS;

/**
 * Lucide 아이콘을 Figma FrameNode로 생성한다.
 *
 * @param name  아이콘 이름 (예: 'ChevronLeft')
 * @param size  아이콘 크기(px). 정방형으로 리사이즈된다.
 * @param color 아이콘 stroke 색상 (RGB 0~1)
 * @returns     Figma FrameNode (아이콘 SVG가 내부에 벡터로 포함됨)
 */
export function createIcon(name: IconName, size: number, color: RGB): FrameNode {
  const svg = ICON_SVGS[name].replace(/\\{COLOR\\}/g, toHex(color));
  const node = figma.createNodeFromSvg(svg);
  node.name = name as string;
  node.resize(size, size);
  return node;
}
`;

const output = `${banner}\n${runtime}`;

const outPath = resolve(__dirname, '../figma-plugin/utils/icons.ts');
writeFileSync(outPath, output, 'utf-8');

console.log(`✅ ${outPath} 생성 완료 (${iconNames.length}개)`);
