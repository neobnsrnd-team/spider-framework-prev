/**
 * @file extract-components.ts
 * @description component-library 아래 모든 types.ts 파일을 읽어
 *   component-types.md 마크다운 파일로 변환한다.
 *
 *   Claude API system prompt에 포함할 컴포넌트 API 레퍼런스를 생성하는 용도.
 *   런타임에 TypeScript 파일을 직접 읽을 수 없는 Java 백엔드를 위해 텍스트로 변환.
 *
 * @example
 *   npx tsx scripts/extract-components.ts
 */

import { readdirSync, readFileSync, mkdirSync, writeFileSync, statSync } from 'fs';
import { join, relative, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const COMPONENT_LIB = join(ROOT, 'component-library');
const OUTPUT_DIR = join(ROOT, 'generated');
const OUTPUT_FILE = join(OUTPUT_DIR, 'component-types.md');
// reactPlatform 프로젝트의 Claude 시스템 프롬프트 디렉토리에도 동기화
const REACT_PLATFORM_OUTPUT_FILE = join(
  ROOT, '..', 'reactPlatform', 'src', 'main', 'resources', 'prompts', 'component-types.md'
);

/** component-library 하위 카테고리 순서 (Claude에게 계층 구조를 명확히 전달하기 위해 고정) */
const CATEGORY_ORDER = ['core', 'layout', 'modules', 'biz'];

interface ComponentEntry {
  category: string;     // core / layout / modules / biz
  subCategory: string;  // banking, card, common, ... (biz 전용)
  name: string;         // 컴포넌트명 (디렉토리명)
  content: string;      // types.ts 파일 전체 내용
}

/** 재귀적으로 types.ts 파일을 탐색하여 ComponentEntry 배열로 반환 */
function collectTypes(dir: string): ComponentEntry[] {
  const entries: ComponentEntry[] = [];

  const walk = (current: string) => {
    const items = readdirSync(current);

    // types.ts 가 있는 디렉토리 = 컴포넌트 루트
    if (items.includes('types.ts')) {
      const rel = relative(COMPONENT_LIB, current);
      const parts = rel.split(/[/\\]/);

      // parts[0] = category (core/layout/modules/biz)
      // parts[1] = subCategory 또는 componentName
      // parts[2] = componentName (biz의 경우)
      const category = parts[0] ?? 'unknown';
      const subCategory = parts.length >= 3 ? parts[1] : '';
      const name = parts[parts.length - 1];

      const content = readFileSync(join(current, 'types.ts'), 'utf-8');
      entries.push({ category, subCategory, name, content });
    }

    // 하위 디렉토리 재귀 탐색 (node_modules, .storybook 제외)
    for (const item of items) {
      if (item === 'node_modules' || item.startsWith('.')) continue;
      const full = join(current, item);
      if (statSync(full).isDirectory()) walk(full);
    }
  };

  walk(dir);
  return entries;
}

/** 컴포넌트 목록을 카테고리별로 그룹핑 */
function groupByCategory(entries: ComponentEntry[]): Map<string, ComponentEntry[]> {
  const map = new Map<string, ComponentEntry[]>();
  for (const entry of entries) {
    const list = map.get(entry.category) ?? [];
    list.push(entry);
    map.set(entry.category, list);
  }
  return map;
}

/** 카테고리 레이블 (Claude가 계층을 이해하도록 설명 포함) */
function categoryLabel(category: string): string {
  const labels: Record<string, string> = {
    core: 'Core (원자 컴포넌트 — Button, Input, Badge 등 단일 HTML 요소 수준)',
    layout: 'Layout (페이지 구조 컴포넌트 — PageLayout, Stack, Grid, Inline 등)',
    modules: 'Modules (분자 컴포넌트 — 2개 이상 Core 조합, 도메인 무관)',
    biz: 'Biz (도메인 특화 컴포넌트 — 금융 비즈니스 로직 포함)',
  };
  return labels[category] ?? category;
}

/** JSDoc 주석을 제거하고 타입 정의만 추출 (파일 상단 JSDoc 제거, 인라인 주석은 유지) */
function stripFileJsDoc(content: string): string {
  // 파일 최상단의 /** ... */ 블록 제거 (import 전의 JSDoc만)
  return content.replace(/^\/\*\*[\s\S]*?\*\/\s*\n/, '');
}

function main() {
  mkdirSync(OUTPUT_DIR, { recursive: true });

  const entries = collectTypes(COMPONENT_LIB);
  const grouped = groupByCategory(entries);

  const lines: string[] = [
    '# Component Types',
    '',
    '> 이 파일은 `scripts/extract-components.ts`로 자동 생성됩니다. 직접 수정하지 마세요.',
    '> Claude API system prompt에 포함되어 컴포넌트 API 레퍼런스로 사용됩니다.',
    '',
    '## 사용 규칙',
    '',
    '- 아래 컴포넌트만 사용할 것. 목록에 없는 컴포넌트는 존재하지 않음',
    '- import 경로: `@cl`',
    '- TypeScript로 작성하고 props interface를 포함할 것',
    '',
  ];

  // 카테고리 순서대로 출력
  for (const category of CATEGORY_ORDER) {
    const categoryEntries = grouped.get(category);
    if (!categoryEntries || categoryEntries.length === 0) continue;

    lines.push(`## ${categoryLabel(category)}`);
    lines.push('');

    // biz는 subCategory(banking/card/common 등)로 한 번 더 그룹핑
    if (category === 'biz') {
      const subGroups = new Map<string, ComponentEntry[]>();
      for (const entry of categoryEntries) {
        const sub = entry.subCategory || 'common';
        const list = subGroups.get(sub) ?? [];
        list.push(entry);
        subGroups.set(sub, list);
      }
      for (const [sub, subEntries] of [...subGroups.entries()].sort()) {
        lines.push(`### biz/${sub}`);
        lines.push('');
        for (const entry of subEntries.sort((a, b) => a.name.localeCompare(b.name))) {
          lines.push(`#### ${entry.name}`);
          lines.push('');
          lines.push('```typescript');
          lines.push(stripFileJsDoc(entry.content).trim());
          lines.push('```');
          lines.push('');
        }
      }
    } else {
      for (const entry of categoryEntries.sort((a, b) => a.name.localeCompare(b.name))) {
        lines.push(`### ${entry.name}`);
        lines.push('');
        lines.push('```typescript');
        lines.push(stripFileJsDoc(entry.content).trim());
        lines.push('```');
        lines.push('');
      }
    }
  }

  const content = lines.join('\n');
  writeFileSync(OUTPUT_FILE, content, 'utf-8');
  console.log(`✅ component-types.md 생성 완료 (${entries.length}개 컴포넌트)`);
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
