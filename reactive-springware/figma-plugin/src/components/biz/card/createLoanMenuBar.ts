/**
 * @file createLoanMenuBar.ts
 * @description Figma LoanMenuBar 컴포넌트 생성.
 * 카드 대출 메뉴 가로 바 (단기카드대출 | 장기카드대출 | 리볼빙).
 * 단일 variant.
 * 컴포넌트 이름: "LoanMenuBar"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

const BAR_WIDTH = 390;

const MENU_ITEMS = [
  { icon: 'CreditCard', label: '단기카드대출' },
  { icon: 'Banknote',   label: '장기카드대출' },
  { icon: 'RefreshCw',  label: '리볼빙' },
] as const;

export async function createLoanMenuBar(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  setPadding(comp, SPACING.xs, SPACING.sm);
  comp.counterAxisAlignItems = 'CENTER';
  comp.resize(BAR_WIDTH, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);

  for (const { icon, label } of MENU_ITEMS) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', SPACING.xs);
    setPadding(btn, SPACING.xs, SPACING.md);
    btn.counterAxisAlignItems = 'CENTER';
    btn.layoutGrow = 1;
    btn.counterAxisSizingMode = 'AUTO';
    btn.primaryAxisSizingMode = 'FIXED';
    btn.resize(1, 1);
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
    clearFill(btn);

    btn.appendChild(createIcon(icon, 14, COLOR.textSecondary));
    await addTextWithVar(btn, label, FONT_SIZE.xs, COLOR_VAR.textLabel, COLOR.textLabel, true, SIZE_VAR.fontSizeXs);
    comp.appendChild(btn);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
