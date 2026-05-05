/**
 * @file createSectionHeader.ts
 * @description Figma SectionHeader 컴포넌트 세트 생성.
 * Layout/Section 헤더 영역과 동일한 구조.
 * HasBadge(True|False) × HasAction(True|False) = 4 variants.
 *
 * TEXT properties:
 *   - title       — 섹션 제목 (기본값: '섹션 제목')
 *   - badge       — 배지 텍스트 (HasBadge=True, 기본값: 'NEW')
 *   - actionLabel — 우측 액션 텍스트 (HasAction=True, 기본값: '전체보기')
 *
 * [레이아웃 구조]
 *   HasBadge=False, HasAction=False: [title]
 *   HasBadge=True,  HasAction=False: [title][badge]
 *   HasBadge=False, HasAction=True:  [title(grow=1)][actionLabel][›]
 *   HasBadge=True,  HasAction=True:  [title][badge][spacer(grow=1)][actionLabel][›]
 *
 * React 대응: packages/component-library/modules/common/SectionHeader
 */
import { COLOR, BRAND, SPACING, FONT_SIZE, RADIUS, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, clearStroke, setFillWithVar, addTextWithVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const HEADER_W = 390;
const HEADER_H = 36;

/**
 * 브랜드 pill badge를 header에 추가하고 comp에 badge TEXT property를 바인딩한다.
 * 호출 시점에 header가 이미 comp의 sublayer여야 한다 (2단계 바인딩 조건).
 */
async function addBadgePill(comp: ComponentNode, header: FrameNode): Promise<void> {
  const pill = figma.createFrame();
  pill.name = 'Badge';
  setAutoLayout(pill, 'HORIZONTAL', 0);
  setPadding(pill, 2, SPACING.xs);
  pill.primaryAxisSizingMode = 'AUTO';
  pill.counterAxisSizingMode = 'AUTO';
  pill.cornerRadius = RADIUS.full;
  clearStroke(pill);
  await setFillWithVar(pill, COLOR_VAR.brandBg, BRAND.bg);
  header.appendChild(pill);

  const badgeText = await addTextWithVar(
    pill, 'NEW', FONT_SIZE.xs,
    COLOR_VAR.brandText, BRAND.text,
    true, SIZE_VAR.fontSizeXs,
  );
  const badgeKey = comp.addComponentProperty('badge', 'TEXT', 'NEW');
  badgeText.componentPropertyReferences = { characters: badgeKey };
}

async function createSectionHeaderVariant(
  hasBadge: boolean,
  hasAction: boolean,
): Promise<ComponentNode> {
  const comp = createComponent(
    `HasBadge=${hasBadge ? 'True' : 'False'}, HasAction=${hasAction ? 'True' : 'False'}`,
  );
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs, 'CENTER');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, 0, SPACING.standard);
  comp.resize(HEADER_W, HEADER_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* title — comp 직접 자식, 자동 바인딩 */
  const titleText = await addTextWithVar(
    comp, '섹션 제목', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase, 'title',
  );

  if (hasBadge) {
    /* badge pill — comp.appendChild(header) 이 comp 자체이므로 즉시 바인딩 가능 */
    const pill = figma.createFrame();
    pill.name = 'Badge';
    setAutoLayout(pill, 'HORIZONTAL', 0);
    setPadding(pill, 2, SPACING.xs);
    pill.primaryAxisSizingMode = 'AUTO';
    pill.counterAxisSizingMode = 'AUTO';
    pill.cornerRadius = RADIUS.full;
    clearStroke(pill);
    await setFillWithVar(pill, COLOR_VAR.brandBg, BRAND.bg);
    comp.appendChild(pill);

    const badgeText = await addTextWithVar(
      pill, 'NEW', FONT_SIZE.xs,
      COLOR_VAR.brandText, BRAND.text,
      true, SIZE_VAR.fontSizeXs,
    );
    const badgeKey = comp.addComponentProperty('badge', 'TEXT', 'NEW');
    badgeText.componentPropertyReferences = { characters: badgeKey };
  }

  if (hasAction) {
    if (hasBadge) {
      /* badge와 action 사이를 채우는 spacer — action을 우측으로 밀어냄 */
      const spacer = figma.createFrame();
      spacer.name = 'Spacer';
      clearFill(spacer);
      clearStroke(spacer);
      spacer.resize(1, 1);
      comp.appendChild(spacer);
      spacer.layoutGrow = 1;
    } else {
      /* title이 spacer 역할 — 나머지 공간을 채워 action을 우측으로 밀어냄 */
      titleText.layoutGrow = 1;
    }

    /* actionLabel — comp 직접 자식, 자동 바인딩 */
    await addTextWithVar(
      comp, '전체보기', FONT_SIZE.sm,
      COLOR_VAR.brandText, BRAND.text,
      false, SIZE_VAR.fontSizeSm, 'actionLabel',
    );

    comp.appendChild(createIcon('ChevronRight', 14, BRAND.text));
  }

  return comp;
}

export async function createSectionHeader(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];
  /* 행: HasBadge(False→True), 열: HasAction(False→True) */
  for (const hasBadge of [false, true]) {
    for (const hasAction of [false, true]) {
      variants.push(await createSectionHeaderVariant(hasBadge, hasAction));
    }
  }
  return combineVariants(variants, 'SectionHeader', 2);
}
