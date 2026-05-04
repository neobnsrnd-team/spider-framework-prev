/**
 * @file createDividerWithLabel.ts
 * @description Figma DividerWithLabel 컴포넌트 생성.
 * 양쪽 선 + 중앙 레이블 패턴.
 * 컴포넌트 이름: "DividerWithLabel"
 */
import { COLOR, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, clearFill, addText, setFill } from '../../../helpers';

export async function createDividerWithLabel(): Promise<ComponentNode> {
  const comp = createComponent('DividerWithLabel');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  comp.resize(328, 24);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  const makeLine = () => {
    const line = figma.createRectangle();
    line.resize(100, 1);
    line.layoutGrow = 1;
    setFill(line, COLOR.border);
    return line;
  };

  comp.appendChild(makeLine());
  const label = await addText(comp, 'OR', FONT_SIZE.xs, COLOR.textMuted);
  label.textAlignHorizontal = 'CENTER';
  comp.appendChild(makeLine());

  figma.currentPage.appendChild(comp);
  return comp;
}
