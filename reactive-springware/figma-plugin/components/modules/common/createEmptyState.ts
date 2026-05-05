/**
 * @file createEmptyState.ts
 * @description Figma EmptyState 컴포넌트 세트 생성.
 * HasAction(True|False) = 2 variants.
 *
 * TEXT properties:
 *   - title       — 상태 제목 (기본값: '데이터가 없습니다')
 *   - description — 보조 설명 (기본값: '조건을 변경하거나 나중에 다시 확인해주세요.')
 *   - actionLabel — 액션 버튼 레이블 (HasAction=True 전용, 기본값: '다시 시도')
 *
 * [레이아웃]
 *   comp(VERTICAL, CENTER, FIXED 390, AUTO)
 *     IconPlaceholder(64×64, surfaceRaised)
 *     TextGroup(VERTICAL, CENTER, AUTO)
 *       title(TEXT)
 *       description(TEXT)
 *     [ActionButton — HasAction=True 전용]
 *       actionLabel(TEXT)
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textGroup) 이후 title/description 바인딩.
 *   comp.appendChild(btn) 이후 actionLabel 바인딩.
 *
 * 컴포넌트 이름: "EmptyState"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setFloatVar, addTextWithVar,
} from '../../../utils/helpers';

async function createEmptyStateVariant(hasAction: boolean): Promise<ComponentNode> {
  const comp = createComponent(`HasAction=${hasAction ? 'True' : 'False'}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md, 'CENTER');
  setPadding(comp, SPACING['3xl'], SPACING.xl);
  comp.resize(390, 1); /* 높이는 AUTO가 조정 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  clearFill(comp);

  /* 아이콘 자리표시자 — 64px 원형 surfaceRaised */
  const iconWrap = figma.createFrame();
  iconWrap.name = 'IconPlaceholder';
  iconWrap.resize(64, 64);
  iconWrap.cornerRadius = RADIUS.full;
  iconWrap.layoutMode = 'NONE';
  await setFillWithVar(iconWrap, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  comp.appendChild(iconWrap);

  /* 텍스트 그룹 — comp.appendChild 후 TEXT property 바인딩 (2단계 ✓) */
  const textGroup = figma.createFrame();
  textGroup.name = 'TextGroup';
  setAutoLayout(textGroup, 'VERTICAL', SPACING.sm, 'CENTER');
  textGroup.primaryAxisSizingMode = 'AUTO';
  textGroup.counterAxisSizingMode = 'AUTO';
  textGroup.primaryAxisAlignItems = 'CENTER';
  clearFill(textGroup);
  comp.appendChild(textGroup);

  /* title — comp.addComponentProperty 경유 바인딩 */
  const title = await addTextWithVar(
    textGroup, '데이터가 없습니다', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase, 'title', comp,
  );
  title.textAlignHorizontal = 'CENTER';

  /* description */
  const desc = await addTextWithVar(
    textGroup, '조건을 변경하거나 나중에 다시 확인해주세요.', FONT_SIZE.sm,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeSm, 'description', comp,
  );
  desc.textAlignHorizontal = 'CENTER';

  /* 액션 버튼 — HasAction=True 전용 */
  if (hasAction) {
    const btn = figma.createFrame();
    btn.name = 'ActionButton';
    setAutoLayout(btn, 'HORIZONTAL', 0, 'CENTER');
    setPadding(btn, SPACING.sm, SPACING.lg);
    btn.primaryAxisSizingMode = 'AUTO';
    btn.counterAxisSizingMode = 'AUTO';
    await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    await setFillWithVar(btn, COLOR_VAR.brandPrimary, BRAND.primary);

    /* btn을 comp에 먼저 추가해야 actionLabel TEXT property 바인딩 가능 */
    comp.appendChild(btn);

    await addTextWithVar(
      btn, '다시 시도', FONT_SIZE.sm,
      COLOR_VAR.brandFg, BRAND.fg,
      true, SIZE_VAR.fontSizeSm, 'actionLabel', comp,
    );
  }

  return comp;
}

export async function createEmptyState(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([
      createEmptyStateVariant(false),
      createEmptyStateVariant(true),
    ]),
    'EmptyState',
    2,
  );
}
