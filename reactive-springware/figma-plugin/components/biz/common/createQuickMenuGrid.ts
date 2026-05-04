/**
 * @file createQuickMenuGrid.ts
 * @description Figma QuickMenuGrid 컴포넌트 세트 생성.
 * 3열 / 4열 두 가지 variant를 가진 ComponentSet을 반환한다.
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setFill, clearFill, addText } from '../../../helpers';
import { createIcon } from '../../../icons';

type GridCols = 3 | 4;

async function createQuickMenuGridVariant(cols: GridCols): Promise<ComponentNode> {
  const itemSize   = cols === 4 ? 72 : 88;
  const totalWidth = cols * itemSize + (cols - 1) * SPACING.sm;
  const comp       = createComponent(`Cols=${cols}`);
  comp.resize(totalWidth, itemSize * 2 + SPACING.sm);
  comp.layoutMode = 'NONE';
  clearFill(comp);

  /* 2행 × cols열 그리드 표현 */
  for (let row = 0; row < 2; row++) {
    for (let col = 0; col < cols; col++) {
      const item = figma.createFrame();
      setAutoLayout(item, 'VERTICAL', SPACING.xs);
      item.resize(itemSize, itemSize);
      item.primaryAxisSizingMode = 'FIXED';
      item.counterAxisSizingMode = 'FIXED';
      item.primaryAxisAlignItems = 'CENTER';
      item.counterAxisAlignItems = 'CENTER';
      clearFill(item);

      /* 아이콘 배경 Frame — 이전의 createRectangle() 대체 */
      const iconBg = figma.createFrame();
      setAutoLayout(iconBg, 'HORIZONTAL', 0);
      iconBg.resize(48, 48);
      iconBg.primaryAxisSizingMode = 'FIXED';
      iconBg.counterAxisSizingMode = 'FIXED';
      iconBg.primaryAxisAlignItems = 'CENTER';
      iconBg.counterAxisAlignItems = 'CENTER';
      iconBg.cornerRadius = RADIUS.md;
      /* BRAND.bg를 아이콘 배경으로 사용해 브랜드 색상을 일관되게 표현 */
      setFill(iconBg, BRAND.bg);

      /* comp → item → iconBg → icon 으로 3단계 중첩되어 componentPropertyReferences 적용 불가.
       * createIcon으로 정적 아이콘을 직접 삽입해 디폴트 아이콘을 표시한다. */
      iconBg.appendChild(createIcon('LayoutGrid', 28, BRAND.primary));
      item.appendChild(iconBg);

      await addText(item, '메뉴', FONT_SIZE.xs, COLOR.textBase);

      item.x = col * (itemSize + SPACING.sm);
      item.y = row * (itemSize + SPACING.sm);
      comp.appendChild(item);
    }
  }

  return comp;
}

export async function createQuickMenuGrid(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createQuickMenuGridVariant(3), createQuickMenuGridVariant(4)]),
    'QuickMenuGrid', 2,
  );
}
