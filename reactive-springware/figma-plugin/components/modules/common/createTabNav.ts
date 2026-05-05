/**
 * @file createTabNav.ts
 * @description Figma TabNav 컴포넌트 세트 생성.
 *
 * TabNav/Item — Variant(underline|pill) × State(default|selected) = 4 variants.
 * TEXT properties:
 *   - label — 탭 레이블 (기본값: '탭')
 *
 * [TabNav/Item 레이아웃]
 *   underline (VERTICAL, FIXED 130×44):
 *     label (TEXT, grow=1, textAlignVertical=CENTER) ← label이 위 공간을 채움
 *     Indicator (Frame, FILL×2px, bottom)            ← selected: brand, default: 투명
 *     → VERTICAL 구조로 indicator가 반드시 하단에 위치 (HORIZONTAL+y절대좌표 버그 수정)
 *
 *   pill (HORIZONTAL AUTO, padding=xs+standard, radiusFull, label CENTER):
 *     label (TEXT, textAlignHorizontal=CENTER)
 *     selected: brandBg fill + brandText / default: clearFill + textMuted
 *
 * TabNav — Variant(underline|pill) × FullWidth(true|false) = 4 variants.
 * [TabNav 레이아웃]
 *   comp (VERTICAL, FIXED 390×44)
 *     Items (Slot, HORIZONTAL, FILL)
 *       TabNav/Item 인스턴스 추가·교체 가능
 *       FullWidth=true:  인스턴스 layoutSizingHorizontal='FILL' (균등 분할)
 *       FullWidth=false: 인스턴스 기본 너비 유지
 *
 * 컴포넌트 이름: "TabNav/Item", "TabNav"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setFloatVar, addTextWithVar, solid,
} from '../../../utils/helpers';

const TAB_W     = 390;
const TAB_H     = 44;
const ITEM_W    = 130; /* underline item 기본 너비 (390 / 3) */

type ItemVariant = 'underline' | 'pill';
type ItemState   = 'default'   | 'selected';

/* ── TabNav/Item ───────────────────────────────────────────── */

async function createTabNavItemVariant(
  variant: ItemVariant,
  state: ItemState,
): Promise<ComponentNode> {
  const comp = createComponent(`Variant=${variant}, State=${state}`);
  clearFill(comp);

  if (variant === 'underline') {
    /* VERTICAL: label(grow=1) + Indicator(2px)
     * ⚑ 이전 HORIZONTAL+y절대좌표 방식은 auto-layout 흐름 상 indicator가 텍스트 옆에
     *   배치되는 버그가 있었다. VERTICAL 구조로 indicator를 반드시 하단에 고정한다. */
    setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
    comp.resize(ITEM_W, TAB_H);
    comp.primaryAxisSizingMode = 'FIXED';
    comp.counterAxisSizingMode = 'FIXED';

    /* label — comp 직접 자식, 자동 바인딩 */
    const label = await addTextWithVar(
      comp, '탭', FONT_SIZE.sm,
      state === 'selected' ? COLOR_VAR.brandText      : COLOR_VAR.textMuted,
      state === 'selected' ? BRAND.text               : COLOR.textMuted,
      state === 'selected', SIZE_VAR.fontSizeSm, 'label',
    );
    label.textAlignHorizontal = 'CENTER';
    label.textAlignVertical   = 'CENTER';
    label.layoutGrow = 1;               /* 2px indicator 위의 남은 공간을 모두 채움 */
    label.layoutSizingHorizontal = 'FILL';

    /* Indicator (2px, 하단 고정) */
    const indicator = figma.createFrame();
    indicator.name = 'Indicator';
    indicator.resize(ITEM_W, 2);
    indicator.primaryAxisSizingMode = 'FIXED';
    indicator.counterAxisSizingMode = 'FIXED';
    if (state === 'selected') {
      await setFillWithVar(indicator, COLOR_VAR.brandPrimary, BRAND.primary);
    } else {
      clearFill(indicator);
    }
    comp.appendChild(indicator);
    /* FILL은 auto-layout 부모에 append 이후에 설정해야 함 */
    indicator.layoutSizingHorizontal = 'FILL';

  } else {
    /* pill: HORIZONTAL AUTO, padding, radiusFull */
    setAutoLayout(comp, 'HORIZONTAL', 0, 'CENTER');
    comp.primaryAxisAlignItems = 'CENTER';
    setPadding(comp, SPACING.xs, SPACING.standard);
    comp.primaryAxisSizingMode = 'AUTO';
    comp.counterAxisSizingMode = 'AUTO';
    await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

    if (state === 'selected') {
      /* selected: 브랜드 배경(연한 초록) + 브랜드 텍스트 */
      await setFillWithVar(comp, COLOR_VAR.brandBg, BRAND.bg);
    }

    /* label — comp 직접 자식, 자동 바인딩 */
    const label = await addTextWithVar(
      comp, '탭', FONT_SIZE.sm,
      state === 'selected' ? COLOR_VAR.brandText : COLOR_VAR.textMuted,
      state === 'selected' ? BRAND.text          : COLOR.textMuted,
      state === 'selected', SIZE_VAR.fontSizeSm, 'label',
    );
    label.textAlignHorizontal = 'CENTER';
  }

  return comp;
}

export async function createTabNavItem(): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createTabNavItemVariant('underline', 'default'),
      await createTabNavItemVariant('underline', 'selected'),
      await createTabNavItemVariant('pill',      'default'),
      await createTabNavItemVariant('pill',      'selected'),
    ],
    'TabNav/Item',
    2,
  );
}

/* ── TabNav ────────────────────────────────────────────────── */

async function createTabNavVariant(
  variant: ItemVariant,
  fullWidth: boolean,
  itemSet: ComponentSetNode,
): Promise<ComponentNode> {
  const comp = createComponent(`Variant=${variant}, FullWidth=${fullWidth ? 'true' : 'false'}`);
  setAutoLayout(comp, 'VERTICAL', 0, 'MIN');
  comp.resize(TAB_W, TAB_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';

  if (variant === 'underline') {
    clearFill(comp);
    /* 하단 구분선 — 탭 트랙 역할 */
    comp.strokes          = [solid(COLOR.borderSubtle)];
    comp.strokeBottomWeight = 1;
    comp.strokeTopWeight    = 0;
    comp.strokeLeftWeight   = 0;
    comp.strokeRightWeight  = 0;
    comp.strokeAlign        = 'INSIDE';
  } else {
    /* pill: 배경 + 둥근 모서리 + 내부 여백 */
    await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
    await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
    setPadding(comp, SPACING.xs, SPACING.xs);
  }

  /* Items 슬롯 — TabNav/Item 인스턴스 추가·교체 가능
   * layoutMode를 먼저 설정한 뒤 sizing을 적용해야 올바른 축에 반영된다 */
  const slot = comp.createSlot();
  slot.name = 'Items';
  (slot as any).layoutMode            = 'HORIZONTAL';
  (slot as any).itemSpacing           = variant === 'pill' ? SPACING.xs : 0;
  (slot as any).counterAxisAlignItems = 'CENTER';
  (slot as any).primaryAxisAlignItems = 'MIN';
  slot.layoutSizingHorizontal = 'FILL'; /* layoutMode 설정 이후에 적용 */
  slot.layoutGrow = 1;

  /* 시각 프리뷰: selected 1개 + default 2개 */
  const defaultSrc  = itemSet.children.find(
    c => c.name === `Variant=${variant}, State=default`,
  ) as ComponentNode | undefined;
  const selectedSrc = itemSet.children.find(
    c => c.name === `Variant=${variant}, State=selected`,
  ) as ComponentNode | undefined;

  const sources = [selectedSrc, defaultSrc, defaultSrc];
  for (const src of sources) {
    if (!src) continue;
    const inst = src.createInstance();
    slot.appendChild(inst);
    if (variant === 'pill') {
      /* pill 아이템은 AUTO 높이라 슬롯을 채우지 못함 — 수직 FILL로 슬롯 높이에 맞춤
       * appendChild 이후에 설정해야 "FILL can only be set on children of auto-layout" 오류 방지 */
      inst.layoutSizingVertical = 'FILL';
    }
    if (fullWidth) {
      /* FullWidth=true: 각 탭이 컨테이너를 균등 분할 — appendChild 이후 설정 필수 */
      inst.layoutSizingHorizontal = 'FILL';
    }
  }

  return comp;
}

export async function createTabNav(
  itemSet: ComponentSetNode,
): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];
  for (const variant of ['underline', 'pill'] as ItemVariant[]) {
    for (const fullWidth of [false, true]) {
      variants.push(await createTabNavVariant(variant, fullWidth, itemSet));
    }
  }
  /* cols=2: FullWidth False/True를 나란히 배치, 행별 variant */
  return combineVariants(variants, 'TabNav', 2);
}
