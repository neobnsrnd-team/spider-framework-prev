/**
 * @file createPageHeaders.ts
 * @description Figma PageLayout / HomePageLayout 헤더 컴포넌트 생성.
 *
 * 컴포넌트:
 * - "PageHeader" (PageLayout 상단 헤더: 뒤로가기 + 타이틀 + 우측 액션)
 * - "HomeHeader"  (HomePageLayout 상단 헤더: 브랜드 타이틀 + 프로필·벨·메뉴 3버튼)
 */

import { COLOR, BRAND, SPACING, FONT_SIZE } from '../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, clearFill, addText } from '../../helpers';
import { createIcon } from '../../icons';

const HEADER_WIDTH  = 390;
const HEADER_HEIGHT = 56;

export async function createPageHeader(): Promise<ComponentNode> {
  const comp = createComponent('PageHeader');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, 0, SPACING.standard);
  comp.resize(HEADER_WIDTH, HEADER_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setFill(comp, COLOR.surface);

  /* 뒤로가기 버튼 — ChevronLeft 아이콘 */
  const backBtn = figma.createFrame();
  backBtn.name = 'BackButton';
  setAutoLayout(backBtn, 'HORIZONTAL', 0);
  backBtn.resize(36, 36);
  backBtn.primaryAxisAlignItems = 'CENTER';
  backBtn.counterAxisAlignItems = 'CENTER';
  backBtn.cornerRadius = 8;
  clearFill(backBtn);
  backBtn.appendChild(createIcon('ChevronLeft', 20, COLOR.textMuted));
  comp.appendChild(backBtn);

  const title = await addText(comp, '페이지 제목', FONT_SIZE.base, COLOR.textHeading, true);
  title.textAlignHorizontal = 'CENTER';
  title.layoutGrow = 1;

  /* 우측 액션 슬롯 (빈 공간 — 너비 맞춤용) */
  const actionSlot = figma.createFrame();
  actionSlot.name = 'RightAction';
  actionSlot.resize(36, 36);
  clearFill(actionSlot);
  comp.appendChild(actionSlot);

  figma.currentPage.appendChild(comp);
  return comp;
}

export async function createHomeHeader(): Promise<ComponentNode> {
  const comp = createComponent('HomeHeader');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, 0, SPACING.standard);
  comp.resize(HEADER_WIDTH, HEADER_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  /* backdrop-blur는 Figma에서 직접 표현 불가 — 흰 배경 80% 불투명도로 대체.
   * Figma API의 SolidPaint.color는 {r,g,b}만 허용하므로 opacity는 setFill 두 번째 인자로 전달 */
  setFill(comp, COLOR.surface, 0.8);

  /* 좌측: 브랜드 타이틀 (greeting 제거됨) */
  const left = figma.createFrame();
  left.name = 'TitleArea';
  setAutoLayout(left, 'HORIZONTAL', SPACING.xs);
  left.layoutGrow = 1;
  left.primaryAxisSizingMode = 'AUTO';
  left.counterAxisSizingMode = 'FIXED';
  left.counterAxisAlignItems = 'CENTER';
  clearFill(left);
  /* 타이틀: text-xl + text-brand (teal) */
  await addText(left, '하나은행', FONT_SIZE.xl, BRAND.primary, true);
  comp.appendChild(left);

  /* 우측: 프로필·벨·메뉴 3버튼 — Figma node 1:227 기준 */
  const rightGroup = figma.createFrame();
  rightGroup.name = 'RightActions';
  setAutoLayout(rightGroup, 'HORIZONTAL', SPACING.xs);
  rightGroup.primaryAxisAlignItems = 'CENTER';
  rightGroup.counterAxisAlignItems = 'CENTER';
  clearFill(rightGroup);

  const makeIconBtn = (name: string, icon: string): FrameNode => {
    const btn = figma.createFrame();
    btn.name = name;
    setAutoLayout(btn, 'HORIZONTAL', 0);
    btn.resize(36, 36);
    btn.primaryAxisAlignItems = 'CENTER';
    btn.counterAxisAlignItems = 'CENTER';
    btn.cornerRadius = 9999; /* rounded-full */
    clearFill(btn);
    btn.appendChild(createIcon(icon, 16, COLOR.textMuted));
    return btn;
  };

  rightGroup.appendChild(makeIconBtn('ProfileButton', 'User'));

  /* 벨 버튼 — 알림 뱃지(빨간 원) 포함 */
  const bellBtn = makeIconBtn('BellButton', 'Bell');
  const badge = figma.createEllipse();
  badge.name = 'NotificationBadge';
  badge.resize(8, 8);
  badge.x = 22; /* 우측 상단 — top-1.5 right-1.5 근사값 */
  badge.y = 4;
  /* --color-danger-badge: #ef4444 */
  badge.fills = [{ type: 'SOLID', color: { r: 0.937, g: 0.267, b: 0.267 }, opacity: 1 }];
  /* 흰 테두리 2px */
  badge.strokes = [{ type: 'SOLID', color: { r: 1, g: 1, b: 1 } }];
  badge.strokeWeight = 2;
  badge.strokeAlign = 'OUTSIDE';
  bellBtn.appendChild(badge);
  rightGroup.appendChild(bellBtn);

  rightGroup.appendChild(makeIconBtn('MenuButton', 'Menu'));
  comp.appendChild(rightGroup);

  figma.currentPage.appendChild(comp);
  return comp;
}
