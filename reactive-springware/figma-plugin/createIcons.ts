/// <reference types="@figma/plugin-typings" />
/**
 * @file createIcons.ts
 * @description lucide-react 기반 SVG 데이터를 Figma ComponentNode로 일괄 생성한다.
 *
 * - 생성된 컴포넌트 이름 형식: "Icons/ChevronLeft"
 * - createComponents 실행 시 아이콘 슬롯(INSTANCE_SWAP)의 기본값으로 참조된다.
 * - 재실행 시 기존 Icons/* 컴포넌트를 삭제하고 새로 생성한다.
 * - 아이콘 데이터는 `npm run generate:icons`로 생성된 icons-generated.ts를 우선 사용하고,
 *   없으면 수동 관리 icons.ts를 fallback으로 사용한다.
 *
 * @returns 완료 메시지 문자열
 */

/* icons-generated.ts 가 존재하면 자동 생성 데이터를 사용, 없으면 수동 icons.ts fallback */
let ICON_MAP: Record<string, string>;

try {
  /* eslint-disable-next-line @typescript-eslint/no-require-imports */
  const generated = require('./icons-generated') as { ICON_SVGS_GENERATED: Record<string, string> };
  ICON_MAP = generated.ICON_SVGS_GENERATED;
} catch {
  /* generate:icons 를 아직 실행하지 않은 경우 수동 icons.ts 사용 */
  const fallback = require('./icons') as { ICON_SVGS: Record<string, string> };
  ICON_MAP = fallback.ICON_SVGS;
}

const ICON_SIZE = 24;
const COLS      = 10;
const GAP       = 16;
/** 컴포넌트 섹션들(y=0 시작) 위에 배치 */
const START_Y   = -400;

function buildIconComponent(name: string): ComponentNode {
  const comp = figma.createComponent();
  comp.name = `Icons/${name}`;
  comp.resize(ICON_SIZE, ICON_SIZE);
  comp.fills = [];
  comp.clipsContent = false;

  /* SVG를 어두운 기본 색으로 임베드 — 디자이너가 인스턴스에서 색상 변경 가능 */
  const svg   = ICON_MAP[name].replace(/\{COLOR\}/g, '#1a1a1a');
  const frame = figma.createNodeFromSvg(svg);
  frame.name  = 'vector';
  frame.resize(ICON_SIZE, ICON_SIZE);
  frame.fills = [];
  comp.appendChild(frame);
  frame.x = 0;
  frame.y = 0;

  return comp;
}

export async function createIcons(): Promise<string> {
  /* 기존 Icons/* 컴포넌트 제거 — 재실행 시 중복 방지 */
  figma.currentPage
    .findAllWithCriteria({ types: ['COMPONENT'] })
    .filter(c => c.name.startsWith('Icons/'))
    .forEach(c => c.remove());

  const names = Object.keys(ICON_MAP);

  const components = names.map((name, i) => {
    const comp = buildIconComponent(name);
    figma.currentPage.appendChild(comp);
    comp.x = (i % COLS) * (ICON_SIZE + GAP);
    comp.y = START_Y + Math.floor(i / COLS) * (ICON_SIZE + GAP);
    return comp;
  });

  figma.viewport.scrollAndZoomIntoView(components);
  return `✅ 아이콘 ${names.length}개 생성 완료 (Icons/*)`;
}
