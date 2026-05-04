/**
 * @file createSuccessHero.ts
 * @description Figma SuccessHero 컴포넌트 생성.
 * 이체 완료 화면의 성공 아이콘 + 금액 + 부제목 중앙 배치 구조.
 * 컴포넌트 이름: "SuccessHero"
 */
import { COLOR, BRAND, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, clearFill, addText } from '../../../helpers';

export async function createSuccessHero(): Promise<ComponentNode> {
  const comp = createComponent('SuccessHero');
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  setPadding(comp, SPACING['4xl'], SPACING.xl);
  comp.resize(390, 320);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 성공 아이콘 (브랜드 원형) */
  const outerCircle = figma.createEllipse();
  outerCircle.resize(96, 96);
  setFill(outerCircle, BRAND.bg);
  comp.appendChild(outerCircle);

  const innerCircle = figma.createEllipse();
  innerCircle.resize(64, 64);
  setFill(innerCircle, BRAND.primary);
  comp.appendChild(innerCircle);

  /* 금액 */
  const amount = await addText(comp, '1,000,000원', FONT_SIZE['2xl'], COLOR.textHeading, true);
  amount.textAlignHorizontal = 'CENTER';

  /* 수신자 */
  const recipient = await addText(comp, '홍길동님에게 이체했어요', FONT_SIZE.base, COLOR.textBase);
  recipient.textAlignHorizontal = 'CENTER';

  /* 부제목 */
  const subtitle = await addText(comp, '2024.01.01 12:34:56', FONT_SIZE.sm, COLOR.textMuted);
  subtitle.textAlignHorizontal = 'CENTER';

  figma.currentPage.appendChild(comp);
  return comp;
}
