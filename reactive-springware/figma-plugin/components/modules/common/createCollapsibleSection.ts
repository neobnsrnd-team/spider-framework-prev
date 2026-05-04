/**
 * @file createCollapsibleSection.ts
 * @description Figma CollapsibleSection 컴포넌트 세트 생성.
 * Expanded(true|false) 2 variants.
 * 컴포넌트 이름: "CollapsibleSection"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, addText } from '../../../helpers';
import { createIcon } from '../../../icons';

async function createCollapsibleVariant(expanded: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Expanded=${expanded ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', 0);
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(328, expanded ? 120 : 56);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.sm;
  setFill(comp, COLOR.surface);

  /* 헤더 행 */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', SPACING.sm);
  header.layoutAlign = 'STRETCH';
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  header.fills = [];

  await addText(header, '섹션 제목', FONT_SIZE.sm, COLOR.textHeading, true);
  /* 펼침/닫힘 상태를 아이콘으로 표현 */
  header.appendChild(createIcon(expanded ? 'ChevronUp' : 'ChevronDown', 16, COLOR.textMuted));
  comp.appendChild(header);

  /* 펼침 콘텐츠 */
  if (expanded) {
    const divider = figma.createRectangle();
    divider.resize(300, 1);
    divider.layoutAlign = 'STRETCH';
    setFill(divider, COLOR.borderSubtle);
    comp.appendChild(divider);

    const content = figma.createFrame();
    setAutoLayout(content, 'VERTICAL', SPACING.xs);
    content.layoutAlign = 'STRETCH';
    content.paddingTop = SPACING.sm;
    content.fills = [];
    await addText(content, '펼쳐진 내용이 여기에 표시됩니다.', FONT_SIZE.sm, COLOR.textBase);
    comp.appendChild(content);
  }

  return comp;
}

export async function createCollapsibleSection(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createCollapsibleVariant(false), createCollapsibleVariant(true)]),
    'CollapsibleSection', 2,
  );
}
