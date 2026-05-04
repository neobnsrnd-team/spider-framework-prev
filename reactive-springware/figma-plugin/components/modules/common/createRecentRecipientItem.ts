/**
 * @file createRecentRecipientItem.ts
 * @description Figma RecentRecipientItem 컴포넌트 생성.
 * 최근 이체 수취인 목록 단일 항목: [아바타] [이름 + 은행/계좌] [ChevronRight].
 * 단일 variant.
 * 컴포넌트 이름: "RecentRecipientItem"
 */

import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createRecentRecipientItem(): Promise<ComponentNode> {
  const comp = createComponent('RecentRecipientItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.sm, SPACING.md);
  comp.resize(390, 60);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* 아바타 원형 프레임 (36×36, size-9) */
  const avatar = figma.createFrame();
  setAutoLayout(avatar, 'HORIZONTAL', 0);
  avatar.primaryAxisAlignItems = 'CENTER';
  avatar.counterAxisAlignItems = 'CENTER';
  avatar.resize(36, 36);
  avatar.primaryAxisSizingMode = 'FIXED';
  avatar.counterAxisSizingMode = 'FIXED';
  await setFloatVar(avatar, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(avatar, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  avatar.appendChild(createIcon('User', 16, COLOR.textMuted));
  comp.appendChild(avatar);

  /* 텍스트 그룹 (이름 + 은행/계좌) */
  const textGroup = figma.createFrame();
  setAutoLayout(textGroup, 'VERTICAL', SPACING.xs, 'MIN');
  textGroup.primaryAxisSizingMode = 'AUTO';
  textGroup.counterAxisSizingMode = 'AUTO';
  clearFill(textGroup);

  /* textGroup을 먼저 comp에 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(textGroup);
  textGroup.layoutGrow = 1;

  const name = await addTextWithVar(
    textGroup, '홍길동', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'name', comp,
  );
  name.textAlignHorizontal = 'LEFT';

  const sub = await addTextWithVar(
    textGroup, '하나은행 123-456-789012', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeXs, 'accountNumber', comp,
  );
  sub.textAlignHorizontal = 'LEFT';

  /* ChevronRight 아이콘 */
  comp.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));

  return comp;
}
