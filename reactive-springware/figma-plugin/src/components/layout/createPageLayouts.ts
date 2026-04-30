/**
 * @file createPageLayouts.ts
 * @description Figma 페이지 레이아웃 템플릿 컴포넌트 세트 생성.
 * Type(Blank|Page|Home) = 3 variants.
 * - Blank: 콘텐츠 슬롯만 있는 기본 레이아웃 (로그인·온보딩)
 * - Page:  PageHeader 인스턴스 + 콘텐츠 슬롯
 * - Home:  HomeHeader 인스턴스 + 콘텐츠 슬롯 + BottomNav 인스턴스
 *
 * 이미 캔버스에 생성된 PageHeader / HomeHeader / BottomNav 컴포넌트 참조를
 * 인수로 받아 각 변형 내부에 인스턴스로 삽입한다.
 *
 * @param pageHeaderComp  - createPageHeader()가 반환한 ComponentNode
 * @param homeHeaderComp  - createHomeHeader()가 반환한 ComponentNode
 * @param bottomNavComp   - createBottomNav()가 반환한 ComponentNode
 * @returns Figma ComponentSetNode ('PageLayouts')
 */
import { COLOR } from '../../tokens';
import { createComponent, combineVariants, setAutoLayout, clearFill } from '../../helpers';

const SCREEN_W = 390;
const SCREEN_H = 800;
const HEADER_H = 53; /* PageHeader / HomeHeader 고정 높이 */
const NAV_H    = 60; /* BottomNav 고정 높이 */

async function createBlankVariant(): Promise<ComponentNode> {
  const comp = createComponent('Type=Blank');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];

  /* 콘텐츠 슬롯 전체 영역 */
  const content = figma.createFrame();
  content.name = 'Content';
  content.resize(SCREEN_W, SCREEN_H);
  clearFill(content);
  comp.appendChild(content);
  content.x = 0;
  content.y = 0;

  return comp;
}

async function createPageVariant(pageHeaderComp: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('Type=Page');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surface }];

  /* PageHeader 인스턴스 — 상단 고정 */
  const headerInst = pageHeaderComp.createInstance();
  comp.appendChild(headerInst);
  headerInst.layoutSizingHorizontal = 'FILL';

  /* 콘텐츠 슬롯 — 나머지 영역 채움 */
  const content = figma.createFrame();
  content.name = 'Content';
  content.resize(SCREEN_W, SCREEN_H - HEADER_H);
  content.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];
  comp.appendChild(content);
  content.layoutSizingHorizontal = 'FILL';
  content.layoutGrow = 1;

  return comp;
}

async function createHomeVariant(
  homeHeaderComp: ComponentNode,
  bottomNavComp: ComponentNode,
): Promise<ComponentNode> {
  const comp = createComponent('Type=Home');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];

  /* HomeHeader 인스턴스 — 상단 고정 */
  const headerInst = homeHeaderComp.createInstance();
  comp.appendChild(headerInst);
  headerInst.layoutSizingHorizontal = 'FILL';

  /* 콘텐츠 슬롯 — 헤더·바텀탭 사이 공간 */
  const content = figma.createFrame();
  content.name = 'Content';
  content.resize(SCREEN_W, SCREEN_H - HEADER_H - NAV_H);
  clearFill(content);
  comp.appendChild(content);
  content.layoutSizingHorizontal = 'FILL';
  content.layoutGrow = 1;

  /* BottomNav 인스턴스 — 하단 고정 */
  const navInst = bottomNavComp.createInstance();
  comp.appendChild(navInst);
  navInst.layoutSizingHorizontal = 'FILL';

  return comp;
}

export async function createPageLayouts(
  pageHeaderComp: ComponentNode,
  homeHeaderComp: ComponentNode,
  bottomNavComp: ComponentNode,
): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createBlankVariant(),
      await createPageVariant(pageHeaderComp),
      await createHomeVariant(homeHeaderComp, bottomNavComp),
    ],
    'PageLayouts',
    3, /* Figma 디자인과 동일하게 3열로 나란히 배치 */
  );
}
