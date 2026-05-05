/**
 * @file createSummaryCard.ts
 * @description Figma SummaryCard 컴포넌트 세트 생성.
 * 카드 도메인 요약 카드 (총 자산 / 이번달 지출).
 * 2개 컴포넌트로 구성: SummaryCard/ActionItem → SummaryCard.
 *
 * ── SummaryCard/ActionItem ─────────────────────────────────────
 * VARIANT properties:
 *   - active  — 활성 상태 (False | True)
 *   - variant — 카드 종류 (Asset | Spending)
 *     - active=False:              surfaceRaised 배경, textHeading 텍스트
 *     - active=True, variant=Asset:    brandPrimary 배경, brandFg 텍스트
 *     - active=True, variant=Spending: domainCardAccent 배경, domainCardAccentText 텍스트
 * TEXT properties:
 *   - label — 액션 레이블 (기본값: '내역')
 *
 * ── SummaryCard ────────────────────────────────────────────────
 * VARIANT properties:
 *   - variant — Asset | Spending (Spending은 왼쪽 4px 액센트 바)
 * TEXT properties:
 *   - title  — 카드 제목 (기본값: variant별)
 *   - amount — 금액      (기본값: variant별)
 * INSTANCE_SWAP properties:
 *   - icon — 아이콘 (기본값: variant=Asset → Building2, variant=Spending → CreditCard)
 * SLOT:
 *   - Actions (SummaryCard/ActionItem 인스턴스를 추가할 수 있는 슬롯)
 *
 * 컴포넌트 이름: "SummaryCard/ActionItem", "SummaryCard"
 */
import { BRAND, COLOR, DOMAIN, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

const CARD_WIDTH    = 390;
const CONTENT_WIDTH = CARD_WIDTH - SPACING.xl * 2; /* 양쪽 padding-xl 제외 */

/* ── SummaryCard/ActionItem ──────────────────────────────────── */

async function createActionItemVariant(active: boolean, variant: 'Asset' | 'Spending'): Promise<ComponentNode> {
  const comp = createComponent(`active=${active ? 'True' : 'False'}, variant=${variant}`);
  setAutoLayout(comp, 'HORIZONTAL', 0);
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.sm, SPACING.md);
  comp.resize(1, 44);                   /* resize 먼저 — height 44 명시 */
  comp.primaryAxisSizingMode = 'AUTO';  /* width 텍스트+padding에 맞게 (인스턴스에서 layoutGrow=1로 확장) */
  comp.counterAxisSizingMode = 'FIXED'; /* height 44 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

  /* active=False: variant 무관하게 동일 스타일 */
  if (!active) {
    await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  } else if (variant === 'Asset') {
    await setFillWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    /* active=True, variant=Spending: 도메인 카드 액센트 색상 */
    await setFillWithVar(comp, COLOR_VAR.domainCardAccent, DOMAIN.cardAccent);
  }

  const textColorVar = !active ? COLOR_VAR.textHeading
    : variant === 'Asset' ? COLOR_VAR.brandFg
    : COLOR_VAR.domainCardAccentText;
  const textColor = !active ? COLOR.textHeading
    : variant === 'Asset' ? BRAND.fg
    : DOMAIN.cardAccentText;

  /* label — comp 직접 자식: 자동 바인딩 */
  const text = await addTextWithVar(
    comp, '내역', FONT_SIZE.xs,
    textColorVar, textColor,
    true, SIZE_VAR.fontSizeXs, 'label',
  );
  text.textAlignHorizontal = 'CENTER';

  return comp;
}

export async function createSummaryCardActionItem(): Promise<ComponentSetNode> {
  /* 4개 variant: active × variant 조합 */
  return combineVariants(
    [
      await createActionItemVariant(false, 'Asset'),
      await createActionItemVariant(false, 'Spending'),
      await createActionItemVariant(true,  'Asset'),
      await createActionItemVariant(true,  'Spending'),
    ],
    'SummaryCard/ActionItem', 2,
  );
}

/* ── SummaryCard ─────────────────────────────────────────────── */

const VARIANT_CONFIG = {
  Asset: {
    title: '총 자산',
    amount: '42,850,000원',
    amountColorVar: COLOR_VAR.brandText,
    amountColor: BRAND.text,
    actions: ['내 계좌', '금융진단'],
    icon: 'Building2' as const,
    accentBar: false,
  },
  Spending: {
    title: '이번달 지출',
    amount: '1,250,000원',
    amountColorVar: COLOR_VAR.textHeading,
    amountColor: COLOR.textHeading,
    actions: ['내역', '분석'],
    icon: 'CreditCard' as const,
    accentBar: true,
  },
} as const;

async function createSummaryVariant(variant: 'Asset' | 'Spending', actionItemSet: ComponentSetNode): Promise<ComponentNode> {
  const cfg = VARIANT_CONFIG[variant];

  const comp = createComponent(`variant=${variant}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.lg, 'MIN');
  comp.primaryAxisAlignItems = 'MIN';
  setPadding(comp, SPACING.xl, SPACING.xl);
  comp.resize(CARD_WIDTH, 1);             /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';    /* height 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'FIXED';   /* width 고정 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  if (cfg.accentBar) {
    /* Spending: 왼쪽 4px 카드 도메인 액센트 바 — domain-card-accent 변수 바인딩
     * setStrokeWithVar 내부에서 strokeWeight=1을 덮어쓰므로 개별 두께는 호출 이후에 설정 */
    await setStrokeWithVar(comp, COLOR_VAR.domainCardAccent, DOMAIN.cardAccent);
    comp.strokeLeftWeight   = 4;
    comp.strokeRightWeight  = 0;
    comp.strokeTopWeight    = 0;
    comp.strokeBottomWeight = 0;
    comp.strokeAlign        = 'INSIDE';
  } else {
    await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
    comp.strokeWeight = 1;
    comp.strokeAlign  = 'INSIDE';
  }

  /* ── 상단: 제목 + 금액 / 우측 아이콘 ── */
  const topRow = figma.createFrame();
  setAutoLayout(topRow, 'HORIZONTAL', SPACING.md);
  topRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  topRow.counterAxisAlignItems = 'MIN';
  topRow.resize(CONTENT_WIDTH, 1);        /* resize 먼저 */
  topRow.primaryAxisSizingMode = 'FIXED';
  topRow.counterAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  clearFill(topRow);
  comp.appendChild(topRow);

  /* 텍스트 영역 (좌측) */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs, 'MIN');
  textArea.primaryAxisAlignItems = 'MIN';
  textArea.layoutGrow = 1;
  textArea.primaryAxisSizingMode = 'AUTO';
  textArea.counterAxisSizingMode = 'AUTO';
  clearFill(textArea);
  topRow.appendChild(textArea);

  /* title — textArea → topRow → comp: 수동 바인딩 */
  await addTextWithVar(textArea, cfg.title, FONT_SIZE.xl, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl, 'title', comp);
  /* amount — textArea → topRow → comp: 수동 바인딩 */
  await addTextWithVar(textArea, cfg.amount, FONT_SIZE.lg, cfg.amountColorVar, cfg.amountColor, true, SIZE_VAR.fontSizeLg, 'amount', comp);

  /* 아이콘 원형 (우측) — INSTANCE_SWAP 등록 */
  const iconCircle = figma.createFrame();
  setAutoLayout(iconCircle, 'HORIZONTAL', 0);
  iconCircle.primaryAxisAlignItems = 'CENTER';
  iconCircle.counterAxisAlignItems = 'CENTER';
  iconCircle.resize(48, 48);
  iconCircle.primaryAxisSizingMode = 'FIXED';
  iconCircle.counterAxisSizingMode = 'FIXED';
  await setFloatVar(iconCircle, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(iconCircle, COLOR_VAR.brandBg, BRAND.bg);
  /* iconCircle을 topRow에 먼저 추가해야 INSTANCE_SWAP 바인딩 가능 */
  topRow.appendChild(iconCircle);
  /* icon INSTANCE_SWAP — iconCircle 내부에 배치, comp에 속성 등록 */
  addIconSlot(comp, cfg.icon, 24, BRAND.primary, 'icon', iconCircle);

  /* ── 하단: Actions 슬롯 ── */
  /* ActionItem 인스턴스를 추가할 수 있는 SlotNode */
  const actionsSlot = comp.createSlot();
  actionsSlot.name = 'Actions';
  actionsSlot.layoutMode = 'HORIZONTAL';
  actionsSlot.itemSpacing = SPACING.sm;
  actionsSlot.primaryAxisAlignItems = 'MIN';
  actionsSlot.counterAxisAlignItems = 'CENTER';
  actionsSlot.resize(CONTENT_WIDTH, 1);   /* resize 먼저 */
  actionsSlot.primaryAxisSizingMode = 'FIXED';
  actionsSlot.counterAxisSizingMode = 'AUTO';
  clearFill(actionsSlot);

  /* 기본 ActionItem 인스턴스 2개 배치 — active=False, 카드 variant 일치 */
  /* defaultVariant는 active=False, variant=Asset */
  const defaultItem = actionItemSet.defaultVariant;
  for (const label of cfg.actions) {
    const inst = defaultItem.createInstance();
    inst.layoutGrow = 1; /* 균일 분할 */
    /* label TEXT property + variant VARIANT property 오버라이드 */
    const props = inst.componentProperties;
    const labelKey   = Object.keys(props).find(k => k.startsWith('label#'));
    const variantKey = Object.keys(props).find(k => k === 'variant' || k.startsWith('variant#'));
    const overrides: Record<string, string> = {};
    if (labelKey)   overrides[labelKey]   = label;
    if (variantKey) overrides[variantKey] = variant; /* SummaryCard variant와 일치 */
    if (Object.keys(overrides).length) inst.setProperties(overrides);
    actionsSlot.appendChild(inst);
  }

  return comp;
}

export async function createSummaryCard(actionItemSet: ComponentSetNode): Promise<ComponentSetNode> {
  return combineVariants(
    [
      await createSummaryVariant('Asset', actionItemSet),
      await createSummaryVariant('Spending', actionItemSet),
    ],
    'SummaryCard', 1,
  );
}
