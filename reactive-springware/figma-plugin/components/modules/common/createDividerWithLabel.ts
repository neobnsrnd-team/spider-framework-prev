/**
 * @file createDividerWithLabel.ts
 * @description Figma DividerWithLabel 컴포넌트 생성.
 * 양쪽 선 + 중앙 레이블 패턴.
 *
 * TEXT properties:
 *   - label — 중앙 구분 텍스트 (기본값: 'OR')
 *
 * 컴포넌트 이름: "DividerWithLabel"
 */
import { COLOR, SPACING, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import { createComponent, setAutoLayout, clearFill, setFill, addTextWithVar } from '../../../utils/helpers';

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

  /* label — comp 직접 자식, 자동 바인딩 */
  const label = await addTextWithVar(
    comp, 'OR', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs, 'label',
  );
  label.textAlignHorizontal = 'CENTER';

  comp.appendChild(makeLine());

  figma.currentPage.appendChild(comp);
  return comp;
}
