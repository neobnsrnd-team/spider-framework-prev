/**
 * @file createNoticeItem.ts
 * @description Figma NoticeItem 컴포넌트 생성.
 * 아이콘 + 제목 + 설명 + ChevronRight 구조.
 *
 * TEXT properties:
 *   - title       — 공지 제목 (기본값: '공지 제목')
 *   - description — 공지 설명 (기본값: '공지 설명 텍스트입니다.')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 좌측 아이콘 (기본값: Bell)
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textArea) 이후 title/description 바인딩 (2단계: comp → textArea → text).
 *
 * 컴포넌트 이름: "NoticeItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill, setFill,
  addTextWithVar, addIconSlot,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createNoticeItem(): Promise<ComponentNode> {
  const comp = createComponent('NoticeItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(390, 64);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 아이콘 원형 컨테이너 — comp.appendChild 후 addIconSlot 호출 */
  const iconWrap = figma.createFrame();
  iconWrap.name = 'IconWrap';
  setAutoLayout(iconWrap, 'HORIZONTAL', 0, 'CENTER');
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.cornerRadius = RADIUS.sm;
  setFill(iconWrap, BRAND.bg);
  comp.appendChild(iconWrap);
  /* Bell을 디폴트로 — 디자이너가 Figma 패널에서 공지 유형에 맞게 swap 가능 */
  addIconSlot(comp, 'Bell', 20, BRAND.primary, 'icon', iconWrap);

  /* 텍스트 영역 — comp.appendChild 이후 TEXT property 바인딩 (2단계 ✓) */
  const textArea = figma.createFrame();
  textArea.name = 'TextArea';
  setAutoLayout(textArea, 'VERTICAL', 2);
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'AUTO';
  clearFill(textArea);
  comp.appendChild(textArea);
  textArea.layoutGrow = 1; /* HORIZONTAL 부모에서 가로 공간 채움 */

  /* title */
  await addTextWithVar(
    textArea, '공지 제목', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'title', comp,
  );

  /* description */
  await addTextWithVar(
    textArea, '공지 설명 텍스트입니다.', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs, 'description', comp,
  );

  /* ChevronRight — 고정 네비게이션 화살표 */
  comp.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));

  figma.currentPage.appendChild(comp);
  return comp;
}
