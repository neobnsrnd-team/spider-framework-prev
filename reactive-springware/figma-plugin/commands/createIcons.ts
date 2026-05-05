/// <reference types="@figma/plugin-typings" />
/**
 * @file createIcons.ts
 * @description icons.ts의 lucide-react SVG 데이터를 Figma ComponentNode로 일괄 생성하고
 *              "Icons" 섹션 안에 그리드로 배치한다.
 *
 * - 생성된 컴포넌트 이름 형식: "Icons/ChevronLeft"
 * - createComponents 실행 시 아이콘 슬롯(INSTANCE_SWAP)의 기본값으로 참조된다.
 * - 재실행 시 기존 Icons 섹션과 Icons/* 컴포넌트를 삭제하고 새로 생성한다.
 *
 * @returns 완료 메시지 문자열
 */

import { ICON_SVGS } from '../utils/icons';

const ICON_SIZE       = 24;
const COLS            = 10;
const GAP             = 16;
const SECTION_PADDING = 48;
/** 컴포넌트 섹션들(y=0 시작) 위에 배치 */
const SECTION_NAME    = 'Icons';

function buildIconComponent(name: string): ComponentNode {
  const comp = figma.createComponent();
  comp.name = `Icons/${name}`;
  comp.resize(ICON_SIZE, ICON_SIZE);
  comp.fills = [];
  comp.clipsContent = false;

  /* SVG를 어두운 기본 색으로 임베드 — 디자이너가 인스턴스에서 색상 변경 가능 */
  const svg   = ICON_SVGS[name].replace(/\{COLOR\}/g, '#1a1a1a');
  const frame = figma.createNodeFromSvg(svg);
  frame.name  = 'vector';
  frame.resize(ICON_SIZE, ICON_SIZE);
  frame.fills = [];
  /* SCALE constraints — 인스턴스를 resize() 해도 벡터가 프레임에 맞게 비례 축소·확대 */
  frame.constraints = { horizontal: 'SCALE', vertical: 'SCALE' };
  comp.appendChild(frame);
  frame.x = 0;
  frame.y = 0;

  return comp;
}

export async function createIcons(): Promise<string> {
  await figma.currentPage.loadAsync();

  /* 기존 Icons 섹션 및 Icons/* 컴포넌트 제거 — 재실행 시 중복 방지 */
  figma.currentPage
    .findAllWithCriteria({ types: ['SECTION'] })
    .filter(s => s.name === SECTION_NAME)
    .forEach(s => s.remove());

  figma.currentPage
    .findAllWithCriteria({ types: ['COMPONENT'] })
    .filter(c => c.name.startsWith('Icons/'))
    .forEach(c => c.remove());

  const names = Object.keys(ICON_SVGS);
  const rows  = Math.ceil(names.length / COLS);

  /* 섹션 크기 계산 */
  const sectionW = SECTION_PADDING + COLS * ICON_SIZE + (COLS - 1) * GAP + SECTION_PADDING;
  const sectionH = SECTION_PADDING + rows * ICON_SIZE + (rows - 1) * GAP + SECTION_PADDING;

  /* 섹션 생성 — 컴포넌트 섹션(y=0) 위에 배치 */
  const section = figma.createSection();
  section.name  = SECTION_NAME;
  figma.currentPage.appendChild(section);
  section.resizeWithoutConstraints(sectionW, sectionH);
  section.x = 0;
  section.y = -(sectionH + 100);

  /* 아이콘 컴포넌트 생성 후 섹션 안에 그리드로 배치 */
  const components = names.map((name, i) => {
    const comp = buildIconComponent(name);
    section.appendChild(comp);
    comp.x = SECTION_PADDING + (i % COLS) * (ICON_SIZE + GAP);
    comp.y = SECTION_PADDING + Math.floor(i / COLS) * (ICON_SIZE + GAP);
    return comp;
  });

  figma.viewport.scrollAndZoomIntoView([section]);
  return `✅ 아이콘 ${names.length}개 생성 완료 (Icons 섹션)`;
}
