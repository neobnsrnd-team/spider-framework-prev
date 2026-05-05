/**
 * @file createSuccessHero.ts
 * @description Figma SuccessHero 컴포넌트 생성.
 * 이체 완료 화면의 성공 아이콘 + 이체 정보 + 부제목 중앙 배치 구조.
 *
 * TEXT properties:
 *   - recipientName — 수취인 이름 (기본값: '홍길동')
 *   - amount        — 이체 금액 (기본값: '1,000,000')
 *   - subtitle      — 부제목 (기본값: '2024.01.01 12:34:56')
 *
 * ※ 제목은 2줄로 표시된다:
 *      Line 1: "{recipientName}님께"
 *      Line 2: "{amount}원 이체 완료"
 *    Figma TEXT property는 TextNode 전체 바인딩이므로, 구간별 TextNode를 분리해
 *    TitleRow(VERTICAL) → Line1/Line2(HORIZONTAL) 구조로 배치한다.
 *    고정 텍스트("님께", "원 이체 완료")는 TEXT property 없음.
 *
 * [아이콘 구조 - IconArea layoutMode=NONE, 절대 좌표]
 *   OuterCircle(96×96, radiusFull, brandBg)   — 연한 halo 원
 *   InnerCircle(64×64, radiusFull, brandPrimary, x=16 y=16) — 진한 원, Check 아이콘 중앙
 *
 * TEXT property 바인딩 타이밍:
 *   recipientName / amount — comp.appendChild(titleRow) 이후 수동 바인딩
 *   subtitle               — comp 직접 자식, 자동 바인딩
 *
 * 컴포넌트 이름: "SuccessHero"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setFloatVar, addTextWithVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const OUTER_SIZE = 96;
const INNER_SIZE = 64;
const INNER_OFFSET = (OUTER_SIZE - INNER_SIZE) / 2; /* 16px — inner를 outer 중앙에 배치 */

export async function createSuccessHero(): Promise<ComponentNode> {
  const comp = createComponent('SuccessHero');
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'CENTER');
  setPadding(comp, SPACING['4xl'], SPACING.xl);
  comp.resize(390, 320);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* ── 아이콘 영역 (겹친 원 + Check) ────────────────────────── */
  const iconArea = figma.createFrame();
  iconArea.name = 'IconArea';
  iconArea.resize(OUTER_SIZE, OUTER_SIZE);
  iconArea.layoutMode = 'NONE'; /* 자식을 절대 좌표로 배치하기 위해 NONE */
  iconArea.clipsContent = false;
  clearFill(iconArea);
  comp.appendChild(iconArea);

  /* 연한 halo 원 (바깥) */
  const outerCircle = figma.createFrame();
  outerCircle.name = 'OuterCircle';
  outerCircle.resize(OUTER_SIZE, OUTER_SIZE);
  await setFloatVar(outerCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  outerCircle.layoutMode = 'NONE';
  await setFillWithVar(outerCircle, COLOR_VAR.brandBg, BRAND.bg);
  outerCircle.x = 0;
  outerCircle.y = 0;
  iconArea.appendChild(outerCircle);

  /* 진한 원 (안쪽) — Check 아이콘을 중앙 정렬하기 위해 AUTO LAYOUT */
  const innerCircle = figma.createFrame();
  innerCircle.name = 'InnerCircle';
  setAutoLayout(innerCircle, 'HORIZONTAL', 0, 'CENTER');
  innerCircle.resize(INNER_SIZE, INNER_SIZE);
  innerCircle.primaryAxisSizingMode = 'FIXED';
  innerCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(innerCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(innerCircle, COLOR_VAR.brandPrimary, BRAND.primary);
  innerCircle.x = INNER_OFFSET;
  innerCircle.y = INNER_OFFSET;
  innerCircle.appendChild(createIcon('Check', 32, { r: 1, g: 1, b: 1 }));
  iconArea.appendChild(innerCircle);

  /* ── 제목: 2줄 구성
   *   Line1: "{recipientName}님께"
   *   Line2: "{amount}원 이체 완료"
   * Figma TEXT property는 TextNode 전체 바인딩이므로 구간별 TextNode 분리.
   * TitleRow(VERTICAL) → Line1/Line2(HORIZONTAL) 구조로 각 줄을 중앙 정렬. */
  const titleRow = figma.createFrame();
  titleRow.name = 'TitleRow';
  setAutoLayout(titleRow, 'VERTICAL', SPACING.xs, 'CENTER');
  titleRow.primaryAxisSizingMode = 'AUTO';
  titleRow.counterAxisSizingMode = 'AUTO';
  clearFill(titleRow);

  /* comp.appendChild(titleRow) 이후 하위 노드들이 comp 서브트리에 포함됨 → 바인딩 가능 */
  comp.appendChild(titleRow);

  /* ── Line 1: {recipientName}님께 ─────────────────────────── */
  const line1 = figma.createFrame();
  line1.name = 'Line1';
  setAutoLayout(line1, 'HORIZONTAL', 0, 'CENTER');
  line1.primaryAxisSizingMode = 'AUTO';
  line1.counterAxisSizingMode = 'AUTO';
  clearFill(line1);
  titleRow.appendChild(line1);

  const recipientNameText = await addTextWithVar(
    line1, '홍길동', FONT_SIZE['2xl'],
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSize2xl,
  );
  const recipientKey = comp.addComponentProperty('recipientName', 'TEXT', '홍길동');
  recipientNameText.componentPropertyReferences = { characters: recipientKey };

  await addTextWithVar(
    line1, '님께', FONT_SIZE['2xl'],
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSize2xl,
  );

  /* ── Line 2: {amount}원 이체 완료 ───────────────────────── */
  const line2 = figma.createFrame();
  line2.name = 'Line2';
  setAutoLayout(line2, 'HORIZONTAL', 0, 'CENTER');
  line2.primaryAxisSizingMode = 'AUTO';
  line2.counterAxisSizingMode = 'AUTO';
  clearFill(line2);
  titleRow.appendChild(line2);

  const amountText = await addTextWithVar(
    line2, '1,000,000', FONT_SIZE['2xl'],
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSize2xl,
  );
  const amountKey = comp.addComponentProperty('amount', 'TEXT', '1,000,000');
  amountText.componentPropertyReferences = { characters: amountKey };

  await addTextWithVar(
    line2, '원 이체 완료', FONT_SIZE['2xl'],
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSize2xl,
  );

  /* ── 부제목 — comp 직접 자식, 자동 바인딩 ──────────────────── */
  const subtitleText = await addTextWithVar(
    comp, '2024.01.01 12:34:56', FONT_SIZE.sm,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeSm, 'subtitle',
  );
  subtitleText.textAlignHorizontal = 'CENTER';

  figma.currentPage.appendChild(comp);
  return comp;
}
