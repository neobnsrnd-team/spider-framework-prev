/**
 * @file createAccountSelectorCard.ts
 * @description Figma AccountSelectorCard 컴포넌트 생성.
 * 계좌 선택 영역을 표현하는 단일 ComponentNode를 반환한다.
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, setFill, setStroke, addText, addIconSlot,
} from '../../../helpers';
import { createIcon } from '../../../icons';

export async function createAccountSelectorCard(): Promise<ComponentNode> {
  const comp = createComponent('AccountSelectorCard');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(390, 100);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  comp.cornerRadius = RADIUS.xl;
  setFill(comp, COLOR.surface);
  setStroke(comp, COLOR.borderSubtle);

  const left = figma.createFrame();
  setAutoLayout(left, 'VERTICAL', 4);
  left.layoutGrow = 1;
  left.fills = [];
  const nameRow = figma.createFrame();
  setAutoLayout(nameRow, 'HORIZONTAL', SPACING.xs);
  nameRow.fills = [];
  await addText(nameRow, '하나 급여통장', FONT_SIZE.sm, COLOR.textHeading, true);
  nameRow.appendChild(createIcon('ChevronDown', 14, COLOR.textMuted));
  left.appendChild(nameRow);
  await addText(left, '123-456789-01234', FONT_SIZE.xs, BRAND.text);
  await addText(left, '잔액 1,234,567원', FONT_SIZE.xs, COLOR.textMuted);
  comp.appendChild(left);

  /* 우측 원형 액션 버튼 Frame — 이전의 createEllipse() 대체 */
  const rightBtn = figma.createFrame();
  setAutoLayout(rightBtn, 'HORIZONTAL', 0);
  rightBtn.resize(48, 48);
  rightBtn.primaryAxisSizingMode = 'FIXED';
  rightBtn.counterAxisSizingMode = 'FIXED';
  rightBtn.primaryAxisAlignItems = 'CENTER';
  rightBtn.counterAxisAlignItems = 'CENTER';
  rightBtn.cornerRadius = RADIUS.full;
  setFill(rightBtn, BRAND.bg);
  comp.appendChild(rightBtn);
  /* ChevronRight를 디폴트로 설정 — 디자이너가 액션 유형에 맞게 swap 가능 */
  addIconSlot(comp, 'ChevronRight', 24, BRAND.primary, 'actionIcon', rightBtn);

  figma.currentPage.appendChild(comp);
  return comp;
}
