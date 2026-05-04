/**
 * @file createUserProfile.ts
 * @description Figma UserProfile 컴포넌트 생성.
 * 아바타 + 이름/최근접속 텍스트 + 설정 버튼으로 구성된 단일 ComponentNode를 반환한다.
 */
import { COLOR, BRAND, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, clearFill, addText } from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createUserProfile(): Promise<ComponentNode> {
  const comp = createComponent('UserProfile');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.lg, SPACING.standard);
  comp.resize(390, 88);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 아바타 */
  const avatar = figma.createEllipse();
  avatar.resize(64, 64);
  setFill(avatar, BRAND.bg);
  comp.appendChild(avatar);

  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', 4);
  textArea.layoutGrow = 1;
  textArea.fills = [];
  await addText(textArea, '홍길동', FONT_SIZE.xl, COLOR.textHeading, true);
  await addText(textArea, '최근 접속: 2024.01.01 12:34', FONT_SIZE.xs, COLOR.textMuted);
  comp.appendChild(textArea);

  /* 설정 버튼 — Settings 아이콘 */
  const settingsBtn = figma.createFrame();
  setAutoLayout(settingsBtn, 'HORIZONTAL', 0);
  settingsBtn.resize(40, 40);
  settingsBtn.primaryAxisSizingMode = 'FIXED';
  settingsBtn.counterAxisSizingMode = 'FIXED';
  settingsBtn.primaryAxisAlignItems = 'CENTER';
  settingsBtn.counterAxisAlignItems = 'CENTER';
  settingsBtn.cornerRadius = 20;
  setFill(settingsBtn, COLOR.surfaceRaised);
  settingsBtn.appendChild(createIcon('Settings', 20, COLOR.textMuted));
  comp.appendChild(settingsBtn);

  figma.currentPage.appendChild(comp);
  return comp;
}
