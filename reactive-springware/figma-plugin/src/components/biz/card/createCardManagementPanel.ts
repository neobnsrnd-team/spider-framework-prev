/**
 * @file createCardManagementPanel.ts
 * @description Figma CardManagementPanel 컴포넌트 생성.
 * "카드 관리" 섹션 헤더 + 네비게이션 행 목록 패널.
 * 단일 variant.
 * 컴포넌트 이름: "CardManagementPanel"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const PANEL_WIDTH = 390;

const NAV_ROWS = [
  { label: '카드정보 확인',   subText: '1234 **** **** 5678' },
  { label: '결제계좌',        subText: '하나은행 123-****-5678' },
  { label: '카드 비밀번호 설정', subText: undefined },
  { label: '해외 결제 신청',   subText: undefined },
];

async function createNavRow(label: string, subText?: string): Promise<FrameNode> {
  const row = figma.createFrame();
  setAutoLayout(row, 'HORIZONTAL', 0);
  row.primaryAxisAlignItems = 'SPACE_BETWEEN';
  row.counterAxisAlignItems = 'CENTER';
  setPadding(row, SPACING.md, SPACING.md);
  row.resize(PANEL_WIDTH, 1);
  row.primaryAxisSizingMode = 'FIXED';
  row.counterAxisSizingMode = 'AUTO';
  await setFillWithVar(row, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(row, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  await setFloatVar(row, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);

  const labelText = await addTextWithVar(row, label, FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, false, SIZE_VAR.fontSizeSm);
  labelText.layoutGrow = 1;

  const right = figma.createFrame();
  setAutoLayout(right, 'HORIZONTAL', SPACING.xs);
  right.counterAxisAlignItems = 'CENTER';
  right.primaryAxisSizingMode = 'AUTO';
  right.counterAxisSizingMode = 'AUTO';
  clearFill(right);

  if (subText) {
    await addTextWithVar(right, subText, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs);
  }
  right.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));
  row.appendChild(right);

  return row;
}

export async function createCardManagementPanel(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  comp.resize(PANEL_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 섹션 헤더 — "카드 관리" */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', 0);
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  header.counterAxisAlignItems = 'CENTER';
  setPadding(header, SPACING.md, SPACING.standard);
  header.layoutAlign = 'STRETCH';
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  header.resize(PANEL_WIDTH, 1);
  clearFill(header);
  await addTextWithVar(header, '카드 관리', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase);
  comp.appendChild(header);

  for (const { label, subText } of NAV_ROWS) {
    comp.appendChild(await createNavRow(label, subText));
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
