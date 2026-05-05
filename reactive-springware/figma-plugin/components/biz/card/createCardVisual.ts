/**
 * @file createCardVisual.ts
 * @description Figma CardVisual 컴포넌트 세트 생성.
 * 카드 이미지 + 브랜드 + 카드명 표시 컴포넌트.
 * compact(False|True) × brand(VISA|Mastercard|AMEX|JCB|UnionPay) = 10 variants.
 * - compact=False: 카드 이미지(16:10) + 브랜드 텍스트 + 카드명 세로 배치
 * - compact=True:  브랜드 텍스트 + 카드명 한 줄 (스티키 헤더용)
 *
 * TEXT properties:
 *   - cardName — 카드명 (기본값: '하나 머니 체크카드')
 *
 * VARIANT properties:
 *   - compact — 컴팩트 모드 (False | True)
 *   - brand   — 카드 브랜드 (VISA | Mastercard | AMEX | JCB | UnionPay)
 *
 * INSTANCE_SWAP properties (compact=False only):
 *   - cardImage — 카드 이미지 슬롯 (기본값: CardVisual/Image 플레이스홀더)
 *                 디자이너가 실제 카드 이미지 컴포넌트로 교체 가능
 *
 * 컴포넌트 이름: "CardVisual", "CardVisual/Image"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const CARD_W = 260;  /* max-w-[260px] */
const CARD_H = 163;  /* 16:10 비율 → 260 × 10/16 ≈ 163 */

type Brand = 'VISA' | 'Mastercard' | 'AMEX' | 'JCB' | 'UnionPay';
const BRANDS: Brand[] = ['VISA', 'Mastercard', 'AMEX', 'JCB', 'UnionPay'];

/**
 * CardVisual/Image 플레이스홀더 컴포넌트를 생성한다.
 * compact=False 변형의 cardImage INSTANCE_SWAP 기본값으로 사용된다.
 * 디자이너는 이 슬롯을 실제 카드 이미지 컴포넌트로 교체한다.
 * 재실행 시 기존 컴포넌트를 삭제하고 새로 생성한다.
 * createComponents.ts에서 CardVisual 옆에 배치하기 위해 export한다.
 */
export async function createCardImagePlaceholder(): Promise<ComponentNode> {
  /* 재실행 시 중복 방지 — 기존 CardVisual/Image 컴포넌트 제거 */
  figma.currentPage
    .findAllWithCriteria({ types: ['COMPONENT'] })
    .filter(c => c.name === 'CardVisual/Image')
    .forEach(c => c.remove());

  const comp = figma.createComponent();
  comp.name = 'CardVisual/Image';
  comp.resize(CARD_W, CARD_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.clipsContent = true;
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  comp.effects = [{
    type: 'DROP_SHADOW',
    color: { r: 0, g: 0, b: 0, a: 0.15 },
    offset: { x: 0, y: 4 },
    radius: 12, spread: 0,
    visible: true, blendMode: 'NORMAL',
  }];

  /* 플레이스홀더 내용: 아이콘 + '카드 이미지' 안내 텍스트 */
  comp.layoutMode = 'VERTICAL';
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  comp.itemSpacing = SPACING.xs;

  comp.appendChild(createIcon('CreditCard', 32, COLOR.textMuted));
  const label = figma.createText();
  await figma.loadFontAsync({ family: 'Inter', style: 'Regular' });
  label.fontName = { family: 'Inter', style: 'Regular' };
  label.characters = '카드 이미지';
  label.fontSize = 12;
  label.fills = [{ type: 'SOLID', color: COLOR.textMuted }];
  comp.appendChild(label);

  figma.currentPage.appendChild(comp);
  return comp;
}

async function createCardVisualVariant(
  compact: boolean,
  brand: Brand,
  imgPlaceholder: ComponentNode,
): Promise<ComponentNode> {
  const comp = createComponent(`compact=${compact ? 'True' : 'False'}, brand=${brand}`);

  if (!compact) {
    /* ── compact=False: 카드 이미지 슬롯 + 브랜드 + 카드명 세로 ── */
    setAutoLayout(comp, 'VERTICAL', SPACING.md);
    comp.primaryAxisAlignItems = 'CENTER';
    comp.counterAxisAlignItems = 'CENTER';
    comp.resize(1, 1);                   /* resize 먼저 */
    comp.primaryAxisSizingMode = 'AUTO';
    comp.counterAxisSizingMode = 'AUTO';
    clearFill(comp);

    /* 카드 이미지 — INSTANCE_SWAP: 디자이너가 실제 카드 이미지 컴포넌트로 교체 */
    const imgInstance = imgPlaceholder.createInstance();
    comp.appendChild(imgInstance);
    const imgPropKey = comp.addComponentProperty('cardImage', 'INSTANCE_SWAP', imgPlaceholder.id);
    imgInstance.componentPropertyReferences = { mainComponent: imgPropKey };

    /* 브랜드 텍스트 + 카드명 */
    const info = figma.createFrame();
    setAutoLayout(info, 'VERTICAL', SPACING.xs);
    info.primaryAxisAlignItems = 'MIN';
    info.counterAxisAlignItems = 'CENTER';
    info.resize(1, 1);                   /* resize 먼저 */
    info.primaryAxisSizingMode = 'AUTO';
    info.counterAxisSizingMode = 'AUTO';
    clearFill(info);
    comp.appendChild(info);

    /* 브랜드 텍스트 — variant로 결정되는 고정값, TEXT property 없음 */
    await addTextWithVar(info, brand, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, true, SIZE_VAR.fontSizeXs);
    /* cardName — info → comp: 수동 바인딩 */
    await addTextWithVar(info, '하나 머니 체크카드', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeBase, 'cardName', comp);

  } else {
    /* ── compact=True: 브랜드 텍스트 + 카드명 한 줄 (카드 이미지 없음) ── */
    setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
    comp.primaryAxisAlignItems = 'MIN';
    comp.counterAxisAlignItems = 'CENTER';
    comp.resize(1, 1);                   /* resize 먼저 */
    comp.primaryAxisSizingMode = 'AUTO';
    comp.counterAxisSizingMode = 'AUTO';
    clearFill(comp);

    /* 브랜드 텍스트 — variant로 결정되는 고정값, TEXT property 없음 */
    await addTextWithVar(comp, brand, FONT_SIZE.xs, COLOR_VAR.textMuted, COLOR.textMuted, true, SIZE_VAR.fontSizeXs);
    /* cardName — comp 직접 자식: 자동 바인딩 */
    await addTextWithVar(comp, '하나 머니 체크카드', FONT_SIZE.sm, COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeSm, 'cardName');
  }

  return comp;
}

export async function createCardVisual(
  imgPlaceholder?: ComponentNode,
): Promise<ComponentSetNode> {
  /* 외부(createComponents)에서 이미 생성된 플레이스홀더를 받거나, 없으면 직접 생성 */
  const placeholder = imgPlaceholder ?? await createCardImagePlaceholder();

  /* brand 순서로 외부 루프, compact 순서로 내부 루프 → cols=2 (False|True 2열 배치) */
  const variants: ComponentNode[] = [];
  for (const brand of BRANDS) {
    variants.push(await createCardVisualVariant(false, brand, placeholder));
    variants.push(await createCardVisualVariant(true,  brand, placeholder));
  }
  return combineVariants(variants, 'CardVisual', 2);
}
