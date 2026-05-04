/**
 * @file createNoticeItem.ts
 * @description Figma NoticeItem 컴포넌트 생성.
 * 아이콘 + 제목 + 설명 + ChevronRight 구조.
 * 컴포넌트 이름: "NoticeItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, setPadding, clearFill, setFill, addText } from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createNoticeItem(): Promise<ComponentNode> {
  const comp = createComponent('NoticeItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(390, 64);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 아이콘 원형 컨테이너 */
  const iconWrap = figma.createFrame();
  setAutoLayout(iconWrap, 'HORIZONTAL', 0);
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.primaryAxisAlignItems = 'CENTER';
  iconWrap.counterAxisAlignItems = 'CENTER';
  iconWrap.cornerRadius = RADIUS.sm;
  setFill(iconWrap, BRAND.bg);
  iconWrap.appendChild(createIcon('Bell', 20, BRAND.primary));
  comp.appendChild(iconWrap);

  /* 텍스트 영역 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', 2);
  textArea.layoutGrow = 1;
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'FIXED';
  textArea.fills = [];
  await addText(textArea, '공지 제목', FONT_SIZE.sm, COLOR.textHeading, true);
  await addText(textArea, '공지 설명 텍스트입니다.', FONT_SIZE.xs, COLOR.textMuted);
  comp.appendChild(textArea);

  /* ChevronRight 아이콘 */
  comp.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));

  figma.currentPage.appendChild(comp);
  return comp;
}
