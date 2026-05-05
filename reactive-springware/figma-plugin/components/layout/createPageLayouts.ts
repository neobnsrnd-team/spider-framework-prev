/**
 * @file createPageLayouts.ts
 * @description Figma 페이지 레이아웃 컴포넌트 생성. 3종 분리.
 *
 * ● BlankPageLayout  — 헤더·내비 없는 빈 레이아웃 (로그인·온보딩)
 * ● PageLayout       — ShowBack(True|False) × RightBtnType(Close|Menu|None) × BottomBtnCnt(0|1|2) = 18 variants
 *                      TEXT: title, bottomBtn1Label('확인'|'취소'), bottomBtn2Label('확인')
 * ● HomePageLayout   — WithBottomNav(True|False) = 2 variants
 *                      TEXT: title
 *
 * [TEXT property 바인딩 타이밍]
 *   buildPageHeader / buildHomeHeader 는 titleText를 반환만 하고 바인딩하지 않는다.
 *   각 variant 생성 함수에서 comp.appendChild(header) 이후 바인딩해야
 *   "Can only set component property references on symbol sublayer" 오류를 피할 수 있다.
 *
 * @param bottomNavComp - createBottomNav()가 반환한 ComponentNode (HomePageLayout용)
 */
import { COLOR, BRAND, SPACING, FONT_SIZE, RADIUS, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFill, setStroke, clearStroke, addTextWithVar,
} from '../../utils/helpers';
import { createIcon } from '../../utils/icons';

const SCREEN_W    = 390;
const SCREEN_H    = 800;
const HEADER_H    = 56;
const NAV_H       = 60;
const BTN_BAR_PAD = 16;
const BTN_H       = 56; /* Large 버튼 높이 */
const BTN_BAR_H   = BTN_H + BTN_BAR_PAD * 2; /* 88px */

type RightBtnType = 'Close' | 'Menu' | 'None';

/* ────────────────────────────────────────────────────────── */
/* 공통 빌드 헬퍼                                              */
/* ────────────────────────────────────────────────────────── */

/**
 * PageHeader 프레임 생성.
 * titleText는 반환 후 호출부에서 comp.appendChild(frame) 완료 뒤에 TEXT property를 바인딩한다.
 * (componentPropertyReferences는 노드가 comp의 하위에 있을 때만 설정 가능)
 */
async function buildPageHeader(
  showBack: boolean,
  rightBtnType: RightBtnType,
): Promise<{ frame: FrameNode; titleText: TextNode }> {
  const frame = figma.createFrame();
  frame.name = 'PageHeader';
  setAutoLayout(frame, 'HORIZONTAL', 0);
  frame.primaryAxisAlignItems = 'MIN';
  frame.counterAxisAlignItems = 'CENTER';
  setPadding(frame, 0, SPACING.standard);
  frame.resize(SCREEN_W, HEADER_H);
  frame.primaryAxisSizingMode = 'FIXED';
  frame.counterAxisSizingMode = 'FIXED';
  setFill(frame, COLOR.surface);

  /* 좌측 슬롯 — 뒤로가기 버튼 or 빈 프레임 (우측 버튼과 너비를 맞춰 타이틀 중앙 정렬 유지) */
  const leftBtn = figma.createFrame();
  leftBtn.name = showBack ? 'BackButton' : 'LeftSlot';
  setAutoLayout(leftBtn, 'HORIZONTAL', 0);
  leftBtn.resize(36, 36);
  leftBtn.primaryAxisAlignItems = 'CENTER';
  leftBtn.counterAxisAlignItems = 'CENTER';
  leftBtn.cornerRadius = 8;
  clearFill(leftBtn);
  if (showBack) {
    leftBtn.appendChild(createIcon('ChevronLeft', 20, COLOR.textMuted));
  }
  frame.appendChild(leftBtn);

  /* 타이틀 — TEXT property 바인딩은 호출부에서 append 후 수행 */
  const titleText = await addTextWithVar(
    frame, '페이지 제목', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase,
  );
  titleText.layoutGrow = 1;
  titleText.textAlignHorizontal = 'CENTER';

  /* 우측 버튼 슬롯 */
  const rightBtn = figma.createFrame();
  rightBtn.name = 'RightButton';
  setAutoLayout(rightBtn, 'HORIZONTAL', 0);
  rightBtn.resize(36, 36);
  rightBtn.primaryAxisAlignItems = 'CENTER';
  rightBtn.counterAxisAlignItems = 'CENTER';
  rightBtn.cornerRadius = 8;
  clearFill(rightBtn);
  if (rightBtnType === 'Close') {
    rightBtn.appendChild(createIcon('X', 20, COLOR.textMuted));
  } else if (rightBtnType === 'Menu') {
    rightBtn.appendChild(createIcon('Menu', 20, COLOR.textMuted));
  }
  frame.appendChild(rightBtn);

  return { frame, titleText };
}

/**
 * HomeHeader 프레임 생성.
 * titleText는 반환 후 호출부에서 comp.appendChild(frame) 완료 뒤에 TEXT property를 바인딩한다.
 */
async function buildHomeHeader(): Promise<{ frame: FrameNode; titleText: TextNode }> {
  const frame = figma.createFrame();
  frame.name = 'HomeHeader';
  setAutoLayout(frame, 'HORIZONTAL', SPACING.sm);
  frame.primaryAxisAlignItems = 'MIN';
  frame.counterAxisAlignItems = 'CENTER';
  setPadding(frame, 0, SPACING.standard);
  frame.resize(SCREEN_W, HEADER_H);
  frame.primaryAxisSizingMode = 'FIXED';
  frame.counterAxisSizingMode = 'FIXED';
  /* backdrop-blur 미지원 → 흰 배경 80% 불투명도 근사 */
  setFill(frame, COLOR.surface, 0.8);

  /* 브랜드 타이틀 — TEXT property 바인딩은 호출부에서 append 후 수행 */
  const titleText = await addTextWithVar(
    frame, '하나은행', FONT_SIZE.xl,
    COLOR_VAR.brandText, BRAND.text,
    true, SIZE_VAR.fontSizeXl,
  );
  titleText.layoutGrow = 1;

  /* 우측 아이콘 버튼 그룹 (level 2) — 내부 버튼은 level 3이지만 TEXT property 불필요 */
  const rightGroup = figma.createFrame();
  rightGroup.name = 'RightActions';
  setAutoLayout(rightGroup, 'HORIZONTAL', SPACING.xs);
  rightGroup.primaryAxisAlignItems = 'CENTER';
  rightGroup.counterAxisAlignItems = 'CENTER';
  clearFill(rightGroup);
  frame.appendChild(rightGroup);

  const makeIconBtn = (name: string, icon: string): FrameNode => {
    const btn = figma.createFrame();
    btn.name = name;
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.resize(36, 36);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.cornerRadius = 9999;
    clearFill(btn);
    btn.appendChild(createIcon(icon, 16, COLOR.textMuted));
    return btn;
  };

  rightGroup.appendChild(makeIconBtn('ProfileButton', 'User'));

  const bellBtn = makeIconBtn('BellButton', 'Bell');
  const badge = figma.createEllipse();
  badge.name = 'NotificationBadge';
  badge.resize(8, 8);
  badge.x = 22;
  badge.y = 4;
  badge.fills  = [{ type: 'SOLID', color: { r: 0.937, g: 0.267, b: 0.267 }, opacity: 1 }];
  badge.strokes = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
  badge.strokeWeight = 2;
  badge.strokeAlign = 'OUTSIDE';
  bellBtn.appendChild(badge);
  rightGroup.appendChild(bellBtn);

  rightGroup.appendChild(makeIconBtn('MenuButton', 'Menu'));

  return { frame, titleText };
}

/**
 * 하단 버튼 바 프레임 — 1개(Primary 전체폭) or 2개(Secondary+Primary).
 * 버튼 레이블 TextNode를 반환해 호출부에서 comp.appendChild 후 TEXT property를 바인딩한다.
 */
async function buildBottomBar(
  bottomBtnCnt: 1 | 2,
): Promise<{ bar: FrameNode; btn1Text: TextNode; btn2Text?: TextNode }> {
  const bar = figma.createFrame();
  bar.name = 'BottomBar';
  setAutoLayout(bar, 'HORIZONTAL', SPACING.sm);
  bar.primaryAxisAlignItems = 'CENTER';
  bar.counterAxisAlignItems = 'CENTER';
  setPadding(bar, BTN_BAR_PAD, BTN_BAR_PAD);
  bar.resize(SCREEN_W, BTN_BAR_H);
  bar.primaryAxisSizingMode = 'FIXED';
  bar.counterAxisSizingMode = 'FIXED';
  setFill(bar, COLOR.surface);

  const makeBtn = async (
    name: string,
    style: 'primary' | 'outline',
    label: string,
  ): Promise<{ btn: FrameNode; text: TextNode }> => {
    const btn = figma.createFrame();
    btn.name = name;
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.resize(100, BTN_H);
    btn.cornerRadius = RADIUS.xl;
    if (style === 'primary') {
      setFill(btn, BRAND.primary);
      clearStroke(btn);
    } else {
      clearFill(btn);
      setStroke(btn, BRAND.primary);
    }
    /* primary: 흰 텍스트(brand/fg #fff), outline: 브랜드 컬러 텍스트 */
    const text = await addTextWithVar(
      btn, label, FONT_SIZE.base,
      style === 'primary' ? COLOR_VAR.brandFg   : COLOR_VAR.brandText,
      style === 'primary' ? BRAND.fg            : BRAND.text,
      true, SIZE_VAR.fontSizeBase,
    );
    text.textAlignHorizontal = 'CENTER';
    return { btn, text };
  };

  if (bottomBtnCnt === 1) {
    const { btn, text: btn1Text } = await makeBtn('PrimaryButton', 'primary', '확인');
    bar.appendChild(btn);
    btn.layoutSizingHorizontal = 'FILL';
    return { bar, btn1Text };
  } else {
    const { btn: btn1, text: btn1Text } = await makeBtn('PrimaryButton',   'primary', '확인');
    const { btn: btn2, text: btn2Text } = await makeBtn('SecondaryButton', 'outline', '취소');
    bar.appendChild(btn2);
    bar.appendChild(btn1);
    btn1.layoutGrow = 1;
    btn2.layoutGrow = 1;
    return { bar, btn2Text, btn1Text };
  }
}

/* ────────────────────────────────────────────────────────── */
/* BlankPageLayout                                            */
/* ────────────────────────────────────────────────────────── */

export async function createBlankPageLayout(): Promise<ComponentNode> {
  const comp = createComponent('BlankPageLayout');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];

  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.resize(SCREEN_W, SCREEN_H);
  clearFill(slot);
  slot.x = 0;
  slot.y = 0;

  return comp;
}

/* ────────────────────────────────────────────────────────── */
/* PageLayout                                                 */
/* ────────────────────────────────────────────────────────── */

async function createPageLayoutVariant(
  showBack: boolean,
  rightBtnType: RightBtnType,
  bottomBtnCnt: 0 | 1 | 2,
): Promise<ComponentNode> {
  const comp = createComponent(
    `ShowBack=${showBack ? 'True' : 'False'}, RightBtnType=${rightBtnType}, BottomBtnCnt=${bottomBtnCnt}`,
  );
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];

  const { frame: header, titleText } = await buildPageHeader(showBack, rightBtnType);
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  /* comp.appendChild 완료 후 바인딩 — sublayer 조건 충족 */
  const titleKey = comp.addComponentProperty('title', 'TEXT', '페이지 제목');
  titleText.componentPropertyReferences = { characters: titleKey };

  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.resize(SCREEN_W, SCREEN_H - HEADER_H - (bottomBtnCnt > 0 ? BTN_BAR_H : 0));
  clearFill(slot);
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;

  if (bottomBtnCnt > 0) {
    const { bar, btn1Text, btn2Text } = await buildBottomBar(bottomBtnCnt as 1 | 2);
    comp.appendChild(bar);
    bar.layoutSizingHorizontal = 'FILL';

    /* TEXT property 바인딩 — comp.appendChild 완료 후 수행 */
    /* bottomBtnCnt=1: btn1은 단독 확인 버튼 / =2: btn1은 취소(outline), btn2는 확인(primary) */
    const btn1Key = comp.addComponentProperty('bottomBtn1Label', 'TEXT', '확인');
    btn1Text.componentPropertyReferences = { characters: btn1Key };

    if (btn2Text) {
      const btn2Key = comp.addComponentProperty('bottomBtn2Label', 'TEXT', '취소');
      btn2Text.componentPropertyReferences = { characters: btn2Key };
    }
  }

  return comp;
}

/**
 * PageLayout 컴포넌트 세트 생성.
 * ShowBack(2) × RightBtnType(3) × BottomBtnCnt(3) = 18 variants, cols=9.
 * 한 행 = ShowBack 값이 동일한 9개 variant.
 */
export async function createPageLayout(): Promise<ComponentSetNode> {
  const showBacks: boolean[]         = [true, false];
  const rightBtnTypes: RightBtnType[] = ['Close', 'Menu', 'None'];
  const bottomBtnCnts: (0|1|2)[]     = [0, 1, 2];

  const components: ComponentNode[] = [];
  for (const showBack of showBacks) {
    for (const rightBtnType of rightBtnTypes) {
      for (const bottomBtnCnt of bottomBtnCnts) {
        components.push(await createPageLayoutVariant(showBack, rightBtnType, bottomBtnCnt));
      }
    }
  }

  /* cols=9: 한 행 = ShowBack 동일 × RightBtnType(3) × BottomBtnCnt(3) */
  return combineVariants(components, 'PageLayout', 9);
}

/* ────────────────────────────────────────────────────────── */
/* HomePageLayout                                             */
/* ────────────────────────────────────────────────────────── */

async function createHomePageLayoutVariant(
  bottomNavComp: ComponentNode,
  withBottomNav: boolean,
): Promise<ComponentNode> {
  const comp = createComponent(`WithBottomNav=${withBottomNav ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(SCREEN_W, SCREEN_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.fills = [{ type: 'SOLID', color: COLOR.surfacePage }];

  const { frame: header, titleText } = await buildHomeHeader();
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  /* comp.appendChild 완료 후 바인딩 — sublayer 조건 충족 */
  const titleKey = comp.addComponentProperty('title', 'TEXT', '하나은행');
  titleText.componentPropertyReferences = { characters: titleKey };

  const contentH = SCREEN_H - HEADER_H - (withBottomNav ? NAV_H : 0);
  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.resize(SCREEN_W, contentH);
  clearFill(slot);
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;

  if (withBottomNav) {
    const navInst = bottomNavComp.createInstance();
    comp.appendChild(navInst);
    navInst.layoutSizingHorizontal = 'FILL';
  }

  return comp;
}

/**
 * HomePageLayout 컴포넌트 세트 생성.
 * WithBottomNav(True|False) = 2 variants.
 *
 * @param bottomNavComp - createBottomNav()가 반환한 ComponentNode
 */
export async function createHomePageLayout(
  bottomNavComp: ComponentNode,
): Promise<ComponentSetNode> {
  const components = [
    await createHomePageLayoutVariant(bottomNavComp, true),
    await createHomePageLayoutVariant(bottomNavComp, false),
  ];
  return combineVariants(components, 'HomePageLayout', 2);
}
