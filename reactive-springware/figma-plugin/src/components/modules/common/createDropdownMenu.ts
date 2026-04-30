/**
 * @file createDropdownMenu.ts
 * @description Figma DropdownMenu 컴포넌트 세트 생성.
 *
 * 트리거 버튼 + 플로팅 패널 구조를 2개의 variant로 구성한다.
 * - Open=False : 트리거 버튼만 표시 (패널 숨김)
 * - Open=True  : 트리거 버튼 + 항목 패널 표시 (내 정보 관리 · 로그아웃)
 *
 * React 대응 컴포넌트: packages/component-library/modules/common/DropdownMenu
 * React props: items(DropdownMenuItem[]), align('left'|'right'), children(trigger)
 */
import { COLOR, SPACING, FONT_SIZE, RADIUS } from '../../../tokens';
import {
  createComponent,
  combineVariants,
  setAutoLayout,
  setPadding,
  setFill,
  clearFill,
  addText,
  solid,
} from '../../../helpers';
import { createIcon, type IconName } from '../../../icons';

/** 단일 메뉴 항목 행 생성 */
async function createMenuItem(
  label: string,
  iconName: IconName,
  isDanger: boolean,
): Promise<FrameNode> {
  const row = figma.createFrame();
  row.name = `item/${label}`;
  setAutoLayout(row, 'HORIZONTAL', SPACING.sm);
  setPadding(row, SPACING.sm, SPACING.md);
  row.primaryAxisSizingMode = 'FIXED';
  row.counterAxisSizingMode = 'AUTO';
  row.resize(140, row.height);
  row.counterAxisAlignItems = 'CENTER';
  clearFill(row);

  /* isDanger: 위험 액션(로그아웃) — danger 색상, 일반: textBase */
  const color = isDanger ? COLOR.danger : COLOR.textBase;
  row.appendChild(createIcon(iconName, 16, color));
  await addText(row, label, FONT_SIZE.sm, color);
  return row;
}

async function createDropdownMenuVariant(open: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Open=${open ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', 0);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 트리거 버튼 — Settings 아이콘 원형 버튼 */
  const trigger = figma.createFrame();
  trigger.name = 'trigger';
  setAutoLayout(trigger, 'HORIZONTAL', 0);
  trigger.resize(40, 40);
  trigger.primaryAxisSizingMode = 'FIXED';
  trigger.counterAxisSizingMode = 'FIXED';
  trigger.primaryAxisAlignItems = 'CENTER';
  trigger.counterAxisAlignItems = 'CENTER';
  trigger.cornerRadius = RADIUS.full;
  setFill(trigger, COLOR.surfaceRaised);
  trigger.appendChild(createIcon('Settings', 16, COLOR.textMuted));
  comp.appendChild(trigger);

  if (open) {
    /* 플로팅 패널 */
    const panel = figma.createFrame();
    panel.name = 'panel';
    setAutoLayout(panel, 'VERTICAL', 0);
    panel.primaryAxisSizingMode = 'AUTO';
    panel.counterAxisSizingMode = 'AUTO';
    panel.cornerRadius = RADIUS.md;
    setFill(panel, COLOR.surface);
    /* solid() 헬퍼로 SolidPaint 생성 */
    panel.strokes = [solid(COLOR.border)];
    panel.strokeWeight = 1;
    panel.strokeAlign = 'INSIDE';

    panel.appendChild(await createMenuItem('내 정보 관리', 'User', false));

    /* 구분선 — 1px 높이 Frame + layoutAlign STRETCH로 패널 너비에 맞춤 */
    const divider = figma.createFrame();
    divider.name = 'divider';
    divider.layoutAlign = 'STRETCH';
    divider.resize(140, 1);
    divider.primaryAxisSizingMode = 'FIXED';
    divider.counterAxisSizingMode = 'FIXED';
    setFill(divider, COLOR.border);
    panel.appendChild(divider);

    panel.appendChild(await createMenuItem('로그아웃', 'LogOut', true));
    comp.appendChild(panel);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}

export async function createDropdownMenu(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([
      createDropdownMenuVariant(false), // Open=False
      createDropdownMenuVariant(true),  // Open=True
    ]),
    'DropdownMenu',
    2,
  );
}
