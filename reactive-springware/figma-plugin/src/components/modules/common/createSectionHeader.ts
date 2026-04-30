/**
 * @file createSectionHeader.ts
 * @description Figma SectionHeader 컴포넌트 세트 생성.
 * 타이틀(좌) + 선택적 배지 + 선택적 우측 액션 링크(ChevronRight) 구조.
 *
 * React 대응 컴포넌트: packages/component-library/modules/common/SectionHeader
 * React props: title(string), badge?(number), actionLabel?(string), onAction?(() => void)
 *
 * Variant 구조: HasAction(True|False) × HasBadge(True|False) = 4가지
 *   HasAction=False, HasBadge=False  ← TitleOnly
 *   HasAction=False, HasBadge=True   ← WithBadge
 *   HasAction=True,  HasBadge=False  ← WithAction (Default)
 *   HasAction=True,  HasBadge=True   ← Full
 */
import { COLOR, BRAND, SPACING, FONT_SIZE, RADIUS, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, clearFill, addTextWithVar, setFloatVar,
} from '../../../helpers';
import { createIcon } from '../../../icons';

/**
 * HasAction × HasBadge 조합의 단일 SectionHeader ComponentNode를 생성한다.
 *
 * @param hasAction - true = 우측 액션 링크 + ChevronRight 표시
 * @param hasBadge  - true = 타이틀 우측에 숫자 배지 표시
 */
async function createSectionHeaderVariant(
  hasAction: boolean,
  hasBadge: boolean,
): Promise<ComponentNode> {
  const comp = createComponent(
    `HasAction=${hasAction ? 'True' : 'False'}, HasBadge=${hasBadge ? 'True' : 'False'}`,
  );

  /* 전체 레이아웃: SPACE_BETWEEN으로 좌우 분리 (React: justify-between) */
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingSm, SPACING.sm);
  setPadding(comp, 0, SPACING.standard);
  await setFloatVar(comp, 'paddingRight', SIZE_VAR.spacingStandard, SPACING.standard);
  await setFloatVar(comp, 'paddingLeft',  SIZE_VAR.spacingStandard, SPACING.standard);
  comp.resize(390, 40);
  comp.primaryAxisSizingMode  = 'FIXED';
  comp.counterAxisSizingMode  = 'FIXED';
  comp.primaryAxisAlignItems  = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems  = 'CENTER';
  clearFill(comp);

  /* ── 왼쪽 영역: 타이틀 + 선택적 배지 ──────────────────────── */
  const left = figma.createFrame();
  left.name = 'left';
  setAutoLayout(left, 'HORIZONTAL', SPACING.xs);
  await setFloatVar(left, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  left.primaryAxisSizingMode = 'AUTO';
  left.counterAxisSizingMode = 'AUTO';
  left.counterAxisAlignItems = 'CENTER';
  left.fills = [];

  /* 섹션 제목 — React: text-xl font-bold text-text-heading */
  await addTextWithVar(left, '섹션 제목', FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl);

  if (hasBadge) {
    /* 배지 — React: rounded-full px-sm py-0.5 text-xs font-bold bg-brand-10 text-brand-text */
    const badge = figma.createFrame();
    badge.name = 'badge';
    setAutoLayout(badge, 'HORIZONTAL', 0);
    /* py=2: React py-0.5(2px), px=sm(8px): React px-sm */
    setPadding(badge, 2, SPACING.sm);
    await setFloatVar(badge, 'paddingRight', SIZE_VAR.spacingSm, SPACING.sm);
    await setFloatVar(badge, 'paddingLeft',  SIZE_VAR.spacingSm, SPACING.sm);
    badge.primaryAxisSizingMode = 'AUTO';
    badge.counterAxisSizingMode = 'AUTO';
    await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(badge, COLOR_VAR.brandBg, BRAND.bg);
    /* Figma에서는 고정 문자열 '3'으로 표현 — React에서는 badge: number가 문자열로 변환됨 */
    await addTextWithVar(badge, '3', FONT_SIZE.xs, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeXs);
    left.appendChild(badge);
  }

  comp.appendChild(left);

  /* ── 우측 영역: 액션 텍스트 + ChevronRight ─────────────────── */
  if (hasAction) {
    const actionRow = figma.createFrame();
    actionRow.name = 'action';
    setAutoLayout(actionRow, 'HORIZONTAL', SPACING.xs);
    await setFloatVar(actionRow, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
    actionRow.primaryAxisSizingMode = 'AUTO';
    actionRow.counterAxisSizingMode = 'AUTO';
    actionRow.counterAxisAlignItems = 'CENTER';
    actionRow.fills = [];

    /* React: text-xs font-medium text-text-secondary / hover:text-brand-text */
    await addTextWithVar(actionRow, '전체보기', FONT_SIZE.xs, COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeXs);
    /* React: ChevronRight size-3.5(14px) — 아이콘 색상은 텍스트와 동일 */
    actionRow.appendChild(createIcon('ChevronRight', 14, COLOR.textSecondary));

    comp.appendChild(actionRow);
  }

  return comp;
}

/**
 * SectionHeader ComponentSet을 생성하고 캔버스에 추가한다.
 * HasAction × HasBadge 4종 variant를 2열 그리드로 배치한다.
 *
 * @returns Figma ComponentSetNode ('SectionHeader')
 */
export async function createSectionHeader(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];

  /* 행: HasBadge(False→True), 열: HasAction(False→True) */
  for (const hasBadge of [false, true]) {
    for (const hasAction of [false, true]) {
      variants.push(await createSectionHeaderVariant(hasAction, hasBadge));
    }
  }

  return combineVariants(variants, 'SectionHeader', 2);
}
