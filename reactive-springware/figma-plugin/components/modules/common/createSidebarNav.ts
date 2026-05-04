/**
 * @file createSidebarNav.ts
 * @description Figma SidebarNav 컴포넌트 생성.
 * 세로 방향 사이드바 탭 네비게이션.
 * 활성 항목: 흰 배경 + 브랜드 텍스트 + 좌측 4px 인디케이터 바.
 * 비활성 항목: secondary 텍스트.
 * 단일 variant (4개 항목, 2번째 활성).
 * 컴포넌트 이름: "SidebarNav"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

const NAV_WIDTH  = 117;  /* 예시 usage: className="w-[117px]" */
const ITEM_HEIGHT = 56;  /* h-14 = 56px */

/** 네비게이션 단일 항목 프레임 생성 */
async function createNavItem(label: string, isActive: boolean): Promise<FrameNode> {
  const item = figma.createFrame();
  setAutoLayout(item, 'HORIZONTAL', 0);
  item.paddingLeft = SPACING.standard;
  item.resize(NAV_WIDTH, ITEM_HEIGHT);
  item.primaryAxisSizingMode = 'FIXED';
  item.counterAxisSizingMode = 'FIXED';

  if (isActive) {
    /* 활성: 흰 배경 */
    await setFillWithVar(item, COLOR_VAR.surface, COLOR.surface);

    /* 좌측 브랜드 인디케이터 바 (4px 너비, 상하 25% 여백) */
    const bar = figma.createRectangle();
    bar.resize(4, Math.round(ITEM_HEIGHT * 0.5)); /* 상하 25% 여백 → 높이 50% */
    bar.cornerRadius = RADIUS.xs;
    bar.fills = [{ type: 'SOLID', color: BRAND.primary }];
    /* 절대 위치: 좌측 0, 수직 중앙 ((56 - 28) / 2 = 14) */
    bar.x = 0;
    bar.y = Math.round((ITEM_HEIGHT - bar.height) / 2);
    item.appendChild(bar);

    const text = await addTextWithVar(
      item, label, FONT_SIZE.sm,
      COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeSm,
    );
    text.layoutGrow = 1;
  } else {
    clearFill(item);
    const text = await addTextWithVar(
      item, label, FONT_SIZE.sm,
      COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm,
    );
    text.layoutGrow = 1;
  }

  return item;
}

export async function createSidebarNav(): Promise<ComponentNode> {
  const comp = createComponent('SidebarNav');
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(NAV_WIDTH, ITEM_HEIGHT * 4);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  /* 우측 구분선 */
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);

  const labels = ['뱅킹', '카드', '보험', '전체'];
  for (let i = 0; i < labels.length; i++) {
    comp.appendChild(await createNavItem(labels[i], i === 1 /* 2번째 항목 활성 */));
  }

  return comp;
}
