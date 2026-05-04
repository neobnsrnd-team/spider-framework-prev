/**
 * @file createSection.ts
 * @description Figma Section 레이아웃 컴포넌트 세트 생성.
 * SectionHeader(제목+액션)와 콘텐츠 블록을 수직으로 묶는 페이지 내 섹션 패턴을 표현한다.
 * HasTitle=True|False = 2 variants.
 *
 * 컴포넌트 이름: "Section"
 */
import { COLOR, BRAND, SPACING, FONT_SIZE, RADIUS } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout,
  setFill, clearFill, addText,
} from '../../helpers';

const SECTION_W = 390;
const HEADER_H  = 36;
const CONTENT_H = 80;
const GAP       = SPACING.md; /* 헤더↔콘텐츠 간격 */

function makeContentBlock(): FrameNode {
  const block = figma.createFrame();
  block.resize(SECTION_W, CONTENT_H);
  block.cornerRadius = RADIUS.sm;
  setFill(block, COLOR.surfaceRaised);
  block.strokes = [{ type: 'SOLID', color: COLOR.border }];
  block.strokeWeight = 1;
  block.strokeAlign = 'INSIDE';
  return block;
}

async function createSectionVariant(hasTitle: boolean): Promise<ComponentNode> {
  const totalH = hasTitle ? HEADER_H + GAP + CONTENT_H : CONTENT_H;
  const comp = createComponent(`HasTitle=${hasTitle ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', GAP, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.resize(SECTION_W, totalH);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  if (hasTitle) {
    /* SectionHeader: 제목(Bold) ↔ 전체보기 링크 좌우 분리 */
    const header = figma.createFrame();
    setAutoLayout(header, 'HORIZONTAL', 0);
    header.primaryAxisAlignItems = 'SPACE_BETWEEN';
    header.counterAxisAlignItems = 'CENTER';
    header.resize(SECTION_W, HEADER_H);
    header.primaryAxisSizingMode = 'FIXED';
    header.counterAxisSizingMode = 'FIXED';
    clearFill(header);

    await addText(header, '섹션 제목', FONT_SIZE.base, COLOR.textHeading, true);
    await addText(header, '전체보기', FONT_SIZE.sm, BRAND.text);

    comp.appendChild(header);
    /* VERTICAL 레이아웃에서 자식의 가로폭을 부모에 맞게 확장 — appendChild 이후에 설정 */
    header.layoutSizingHorizontal = 'FILL';
  }

  const content = makeContentBlock();
  comp.appendChild(content);
  content.layoutSizingHorizontal = 'FILL';

  return comp;
}

export async function createSection(): Promise<ComponentSetNode> {
  const components = [
    await createSectionVariant(true),
    await createSectionVariant(false),
  ];
  return combineVariants(components, 'Section', 1);
}
