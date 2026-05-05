/**
 * @file createUserProfile.ts
 * @description Figma UserProfile 컴포넌트 생성.
 * 아바타(User 아이콘) + 이름/최근접속 텍스트 + Settings 버튼으로 구성된 단일 ComponentNode를 반환한다.
 *
 * TEXT properties:
 *   - name      — 사용자 이름 (기본값: '홍길동')
 *   - lastLogin — 최근 접속 일시 (기본값: '2024.01.01 12:34')
 *
 * [레이아웃 — 실제 컴포넌트 기준]
 *   comp(HORIZONTAL, SPACE_BETWEEN, FIXED 390×88, padding=md+standard, counterAxis=CENTER)
 *     leftGroup(HORIZONTAL, gap=md, counterAxis=CENTER)
 *       avatar(64×64, radiusFull, brandPrimary5 bg, brandPrimary20 stroke)
 *         User icon(24px, brandPrimary)
 *       textArea(VERTICAL, gap=xs)
 *         name(TEXT xl, textHeading)
 *         lastLogin(TEXT xs, textMuted)
 *     settingsBtn(40×40, radiusFull, surfaceRaised bg, border stroke)
 *       Settings icon(16px, textMuted)
 *
 * TEXT property 바인딩 타이밍:
 *   comp → leftGroup → textArea → text: 3단계 수동 바인딩
 *   (comp.appendChild(leftGroup) → leftGroup.appendChild(textArea) → text 추가 → 바인딩)
 *
 * 컴포넌트 이름: "UserProfile"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createUserProfile(): Promise<ComponentNode> {
  const comp = createComponent('UserProfile');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(comp, SPACING.md, SPACING.standard); /* py-md px-standard */
  comp.resize(390, 88);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 좌측 그룹: 아바타 + 텍스트 영역 */
  const leftGroup = figma.createFrame();
  setAutoLayout(leftGroup, 'HORIZONTAL', SPACING.md);
  leftGroup.counterAxisAlignItems = 'CENTER';
  leftGroup.primaryAxisSizingMode = 'AUTO';
  leftGroup.counterAxisSizingMode = 'AUTO';
  clearFill(leftGroup);
  comp.appendChild(leftGroup);

  /* 아바타: size-16(64px), rounded-full, bg-brand-5, border border-brand/20 */
  const avatar = figma.createFrame();
  setAutoLayout(avatar, 'HORIZONTAL', 0);
  avatar.resize(64, 64);
  avatar.primaryAxisSizingMode = 'FIXED';
  avatar.counterAxisSizingMode = 'FIXED';
  avatar.primaryAxisAlignItems = 'CENTER';
  avatar.counterAxisAlignItems = 'CENTER';
  await setFloatVar(avatar, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(avatar, COLOR_VAR.brandPrimary5, BRAND.bg);
  await setStrokeWithVar(avatar, COLOR_VAR.brandPrimary20, BRAND.primary);
  avatar.strokeWeight = 1;
  avatar.strokeAlign = 'INSIDE';
  avatar.appendChild(createIcon('User', 24, BRAND.primary));
  leftGroup.appendChild(avatar);

  /* 텍스트 영역 — leftGroup.appendChild 후 comp 서브트리에 포함됨 → 수동 바인딩 가능 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs);
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'AUTO';
  clearFill(textArea);
  leftGroup.appendChild(textArea);

  /* name — 3단계 수동 바인딩 (comp → leftGroup → textArea → text) */
  const nameNode = await addTextWithVar(
    textArea, '홍길동', FONT_SIZE.xl,
    COLOR_VAR.textHeading, COLOR.textHeading,
    false, SIZE_VAR.fontSizeXl,
  );
  const nameKey = comp.addComponentProperty('name', 'TEXT', '홍길동');
  nameNode.componentPropertyReferences = { characters: nameKey };

  /* lastLogin — 동일 3단계 수동 바인딩 */
  const lastLoginNode = await addTextWithVar(
    textArea, '최근 접속: 2024.01.01 12:34', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  const lastLoginKey = comp.addComponentProperty('lastLogin', 'TEXT', '2024.01.01 12:34');
  lastLoginNode.componentPropertyReferences = { characters: lastLoginKey };

  /* 우측 설정 버튼: size-10(40px), rounded-full, surfaceRaised bg, border stroke */
  const settingsBtn = figma.createFrame();
  setAutoLayout(settingsBtn, 'HORIZONTAL', 0);
  settingsBtn.resize(40, 40);
  settingsBtn.primaryAxisSizingMode = 'FIXED';
  settingsBtn.counterAxisSizingMode = 'FIXED';
  settingsBtn.primaryAxisAlignItems = 'CENTER';
  settingsBtn.counterAxisAlignItems = 'CENTER';
  await setFloatVar(settingsBtn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(settingsBtn, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setStrokeWithVar(settingsBtn, COLOR_VAR.border, COLOR.border);
  settingsBtn.strokeWeight = 1;
  settingsBtn.strokeAlign = 'INSIDE';
  settingsBtn.appendChild(createIcon('Settings', 16, COLOR.textMuted));
  comp.appendChild(settingsBtn);

  figma.currentPage.appendChild(comp);
  return comp;
}
