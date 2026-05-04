/**
 * @file createTabNav.ts
 * @description Figma TabNav 컴포넌트 세트 생성.
 * React TabNav의 variant(underline|pill)를 Figma variant로 매핑한다.
 *
 * 컴포넌트 이름: "TabNav"
 * Variant 형식: "Variant=Underline" | "Variant=Pill"
 */

import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setStroke, clearFill, addText,
} from '../../../helpers';

const TAB_LABELS = ['탭 1', '탭 2', '탭 3'];
const TAB_WIDTH = 390;

async function createUnderlineTabNav(): Promise<ComponentNode> {
  const comp = createComponent('Variant=Underline');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.resize(TAB_WIDTH, 44);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);
  setStroke(comp, COLOR.borderSubtle);
  comp.strokeAlign = 'OUTSIDE';
  /* 하단 테두리만 표시하기 위해 개별 stroke 적용 */
  comp.strokes = [{ type: 'SOLID', color: COLOR.borderSubtle }];
  comp.strokeBottomWeight = 1;
  comp.strokeTopWeight = 0;
  comp.strokeLeftWeight = 0;
  comp.strokeRightWeight = 0;

  for (let i = 0; i < TAB_LABELS.length; i++) {
    const label = TAB_LABELS[i];
    const tab = figma.createFrame();
    tab.name = i === 0 ? 'Tab (Active)' : 'Tab';
    setAutoLayout(tab, 'HORIZONTAL', 0);
    setPadding(tab, SPACING.xs, SPACING.standard, SPACING.md, SPACING.standard);
    tab.layoutGrow = 1;
    tab.primaryAxisSizingMode = 'FIXED';
    tab.counterAxisSizingMode = 'FIXED';
    tab.primaryAxisAlignItems = 'CENTER';
    tab.resize(TAB_WIDTH / TAB_LABELS.length, 44);
    clearFill(tab);

    const isActive = i === 0;
    const text = await addText(
      tab, label, FONT_SIZE.sm,
      isActive ? BRAND.text : COLOR.textMuted,
      isActive,
    );
    text.textAlignHorizontal = 'CENTER';
    text.layoutGrow = 1;

    /* 활성 탭 하단 인디케이터 */
    if (isActive) {
      const indicator = figma.createRectangle();
      indicator.resize(TAB_WIDTH / TAB_LABELS.length, 2);
      setFill(indicator, BRAND.primary);
      indicator.y = 42; // 탭 하단
      tab.appendChild(indicator);
    }

    comp.appendChild(tab);
  }

  return comp;
}

async function createPillTabNav(): Promise<ComponentNode> {
  const comp = createComponent('Variant=Pill');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  setPadding(comp, SPACING.xs, SPACING.xs);
  comp.resize(TAB_WIDTH, 44);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.full;
  setFill(comp, COLOR.surfaceRaised);

  for (let i = 0; i < TAB_LABELS.length; i++) {
    const label = TAB_LABELS[i];
    const isActive = i === 0;
    const tab = figma.createFrame();
    tab.name = isActive ? 'Tab (Active)' : 'Tab';
    setAutoLayout(tab, 'HORIZONTAL', 0);
    setPadding(tab, SPACING.xs, SPACING.standard);
    tab.layoutGrow = 1;
    tab.primaryAxisSizingMode = 'FIXED';
    tab.counterAxisSizingMode = 'FIXED';
    tab.primaryAxisAlignItems = 'CENTER';
    tab.resize((TAB_WIDTH - SPACING.xs * 4) / TAB_LABELS.length, 36);
    tab.cornerRadius = RADIUS.full;

    if (isActive) {
      setFill(tab, COLOR.surface);
    } else {
      clearFill(tab);
    }

    const text = await addText(
      tab, label, FONT_SIZE.sm,
      isActive ? COLOR.textHeading : COLOR.textMuted,
      isActive,
    );
    text.textAlignHorizontal = 'CENTER';
    text.layoutGrow = 1;

    comp.appendChild(tab);
  }

  return comp;
}

export async function createTabNav(): Promise<ComponentSetNode> {
  const components = [
    await createUnderlineTabNav(),
    await createPillTabNav(),
  ];
  /* cols=1: Underline / Pill을 세로로 나열 */
  return combineVariants(components, 'TabNav', 1);
}
