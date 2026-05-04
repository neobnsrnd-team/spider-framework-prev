/**
 * @file createEmptyState.ts
 * @description Figma EmptyState 컴포넌트 생성.
 * 중앙 정렬 일러스트 + 타이틀 + 설명 + 액션 버튼 구조.
 * 컴포넌트 이름: "EmptyState" / Variant: "HasAction=True" | "HasAction=False"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, clearFill, setFill, addText } from '../../../helpers';

async function createEmptyStateVariant(hasAction: boolean): Promise<ComponentNode> {
  const comp = createComponent(`HasAction=${hasAction ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  setPadding(comp, SPACING['3xl'], SPACING.xl);
  comp.resize(328, hasAction ? 260 : 220);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 아이콘 placeholder */
  const icon = figma.createEllipse();
  icon.resize(64, 64);
  setFill(icon, COLOR.surfaceRaised);
  comp.appendChild(icon);

  const title = await addText(comp, '데이터가 없습니다', FONT_SIZE.base, COLOR.textHeading, true);
  title.textAlignHorizontal = 'CENTER';

  const desc = await addText(comp, '조건을 변경하거나 나중에 다시 확인해주세요.', FONT_SIZE.sm, COLOR.textMuted);
  desc.textAlignHorizontal = 'CENTER';

  if (hasAction) {
    const btn = figma.createFrame();
    setAutoLayout(btn, 'HORIZONTAL', 0);
    setPadding(btn, 0, SPACING.xl);
    btn.resize(160, 40);
    btn.primaryAxisSizingMode = 'AUTO';
    btn.counterAxisSizingMode = 'FIXED';
    btn.primaryAxisAlignItems = 'CENTER';
    btn.cornerRadius = RADIUS.md;
    setFill(btn, COLOR.surfaceRaised);
    await addText(btn, '다시 시도', FONT_SIZE.sm, COLOR.textBase, true);
    comp.appendChild(btn);
  }

  return comp;
}

export async function createEmptyState(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createEmptyStateVariant(false), createEmptyStateVariant(true)]),
    'EmptyState', 2,
  );
}
