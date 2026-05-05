/**
 * @file createSection.ts
 * @description Figma Section 레이아웃 컴포넌트 세트 생성.
 * HasBadge(True|False) × HasAction(True|False) = 4 variants.
 *
 * TEXT properties:
 *   - title       — 섹션 제목 (모든 variant)
 *   - actionLabel — 우측 액션 링크 텍스트 (HasAction=True variant)
 *
 * badge는 HasBadge=True variant에 브랜드 색상 pill 프레임으로 직접 표현된다 (컴포넌트 인스턴스 아님).
 *
 * [레이아웃 구조]
 *   HasBadge=False, HasAction=False: [title]
 *   HasBadge=True,  HasAction=False: [title][badge]
 *   HasBadge=False, HasAction=True:  [title(grow)          ][actionLabel][›]
 *   HasBadge=True,  HasAction=True:  [title][badge][spacer  ][actionLabel][›]
 *
 * Content 영역은 SlotNode로 구현되어 인스턴스에서 자유롭게 자식을 배치할 수 있다.
 *
 * 컴포넌트 이름: "Section"
 */
import { COLOR, BRAND, SPACING, FONT_SIZE, RADIUS, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFillWithVar, clearStroke, addTextWithVar,
} from '../../utils/helpers';
import { createIcon } from '../../utils/icons';

const SECTION_W = 390;
const HEADER_H  = 36;
const CONTENT_H = 80;
const GAP       = SPACING.md; /* 헤더↔콘텐츠 간격 */

/**
 * 브랜드 색상 badge pill 프레임을 생성해 header에 추가하고, comp에 badge TEXT property를 바인딩한다.
 * 호출 시점에 header가 이미 comp의 sublayer여야 한다 (timing 조건).
 */
async function addBadgePill(comp: ComponentNode, header: FrameNode): Promise<void> {
  const pill = figma.createFrame();
  pill.name = 'Badge';
  setAutoLayout(pill, 'HORIZONTAL', 0);
  pill.primaryAxisAlignItems = 'CENTER';
  pill.counterAxisAlignItems = 'CENTER';
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

async function createSectionVariant(hasBadge: boolean, hasAction: boolean): Promise<ComponentNode> {
  const comp = createComponent(
    `HasBadge=${hasBadge ? 'True' : 'False'}, HasAction=${hasAction ? 'True' : 'False'}`,
  );
  setAutoLayout(comp, 'VERTICAL', GAP, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.resize(SECTION_W, HEADER_H + GAP + CONTENT_H);
  clearFill(comp);

  /* ── SectionHeader ─────────────────────────────────────── */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', SPACING.xs, 'CENTER');
  header.primaryAxisAlignItems = 'MIN';
  header.resize(SECTION_W, HEADER_H);
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'FIXED';
  clearFill(header);
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  /* title (level 2) */
  const titleText = await addTextWithVar(
    header, '섹션 제목', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase, 'title', comp,
  );

  if (hasBadge) {
    await addBadgePill(comp, header);
  }

  if (hasAction) {
    if (hasBadge) {
      /* badge와 action 사이를 채우는 invisible spacer — level 2이므로 TEXT property 영향 없음 */
      const spacer = figma.createFrame();
      clearFill(spacer);
      clearStroke(spacer);
      spacer.resize(1, 1);
      header.appendChild(spacer);
      spacer.layoutGrow = 1; /* appendChild 이후 설정해야 auto-layout에 적용됨 */
    } else {
      /* title이 spacer 역할 — 나머지 공간을 채워 action을 우측으로 밀어냄 */
      titleText.layoutGrow = 1;
    }

    /* actionLabel (level 2) */
    await addTextWithVar(
      header, '전체보기', FONT_SIZE.sm,
      COLOR_VAR.brandText, BRAND.text,
      false, SIZE_VAR.fontSizeSm, 'actionLabel', comp,
    );

    /* ChevronRight 아이콘 (level 2) */
    const chevron = createIcon('ChevronRight', 14, BRAND.text);
    header.appendChild(chevron);
  }

  /* ── Content Slot ──────────────────────────────────────── */
  const slot = comp.createSlot();
  slot.name = 'Content';
  slot.resize(SECTION_W, CONTENT_H);
  slot.primaryAxisSizingMode = 'FIXED';
  slot.counterAxisSizingMode = 'FIXED';
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;
  slot.cornerRadius = RADIUS.sm;
  slot.fills   = [{ type: 'SOLID', color: COLOR.surfaceRaised }];
  slot.strokes = [{ type: 'SOLID', color: COLOR.border }];
  slot.strokeWeight = 1;
  slot.strokeAlign  = 'INSIDE';

  return comp;
}

export async function createSection(): Promise<ComponentSetNode> {
  const components = [
    await createSectionVariant(false, false),
    await createSectionVariant(true,  false),
    await createSectionVariant(false, true ),
    await createSectionVariant(true,  true ),
  ];
  /* cols=4: 4가지 variant를 한 행에 나란히 배치 */
  return combineVariants(components, 'Section', 4);
}
