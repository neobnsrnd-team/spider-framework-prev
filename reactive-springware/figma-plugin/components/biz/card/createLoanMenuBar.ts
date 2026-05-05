/**
 * @file createLoanMenuBar.ts
 * @description Figma LoanMenuBar 컴포넌트 세트 생성.
 * 카드 대출 메뉴 가로 바.
 * 2개 컴포넌트로 구성: LoanMenuBar/Item → LoanMenuBar.
 *
 * ── LoanMenuBar/Item ───────────────────────────────────────────
 * TEXT properties:
 *   - label — 메뉴 레이블 (기본값: '단기카드대출')
 * INSTANCE_SWAP properties:
 *   - icon  — 아이콘 (기본값: CreditCard)
 *
 * ── LoanMenuBar ────────────────────────────────────────────────
 * SLOT:
 *   - Items (LoanMenuBar/Item 인스턴스를 추가할 수 있는 슬롯)
 *
 * [레이아웃]
 *   LoanMenuBar/Item (HORIZONTAL gap=xs CENTER, p-[xs md], radiusLg)
 *     icon (16px, INSTANCE_SWAP)
 *     label (TEXT xs bold, textLabel, 자동 바인딩)
 *
 *   LoanMenuBar (HORIZONTAL gap=xs CENTER, p-[xs sm], FIXED 390, radiusXl, surfaceRaised)
 *     Items SlotNode (HORIZONTAL gap=xs CENTER, FIXED 374, Item×3)
 *
 * 컴포넌트 이름: "LoanMenuBar/Item", "LoanMenuBar"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';

const BAR_WIDTH    = 390;
const ITEMS_WIDTH  = BAR_WIDTH - SPACING.sm * 2; /* 양쪽 padding-sm 제외 */

/* ── LoanMenuBar/Item ───────────────────────────────────────── */

export async function createLoanMenuBarItem(): Promise<ComponentNode> {
  const comp = createComponent('LoanMenuBar/Item');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.primaryAxisAlignItems = 'CENTER';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.xs, SPACING.md);
  comp.resize(1, 1);                    /* resize 먼저 */
  comp.primaryAxisSizingMode = 'AUTO';  /* width 콘텐츠에 맞게 */
  comp.counterAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  clearFill(comp);

  /* icon — INSTANCE_SWAP, comp에 등록 */
  addIconSlot(comp, 'CreditCard', 16, COLOR.textSecondary, 'icon');

  /* label — comp 직접 자식: 자동 바인딩 */
  await addTextWithVar(comp, '단기카드대출', FONT_SIZE.xs, COLOR_VAR.textLabel, COLOR.textLabel, true, SIZE_VAR.fontSizeXs, 'label');

  figma.currentPage.appendChild(comp);
  return comp;
}

/* ── LoanMenuBar ─────────────────────────────────────────────── */

/* Items 슬롯에 기본 배치할 인스턴스의 (icon, label) 매핑 */
const DEFAULT_ITEMS: { icon: 'CreditCard' | 'Banknote' | 'RefreshCw'; label: string }[] = [
  { icon: 'CreditCard', label: '단기카드대출' },
  { icon: 'Banknote',   label: '장기카드대출' },
  { icon: 'RefreshCw',  label: '리볼빙' },
];

/**
 * @param item - createLoanMenuBarItem()이 반환한 ComponentNode.
 *               Items 슬롯의 기본 인스턴스 배치에 사용한다.
 */
export async function createLoanMenuBar(item: ComponentNode): Promise<ComponentNode> {
  const comp = createComponent('LoanMenuBar');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  comp.primaryAxisAlignItems = 'MIN';
  comp.counterAxisAlignItems = 'CENTER';
  setPadding(comp, SPACING.xs, SPACING.sm);
  comp.resize(BAR_WIDTH, 1);            /* resize 먼저 */
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';  /* height 콘텐츠에 맞게 */
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);

  /* Items 슬롯 — LoanMenuBar/Item 인스턴스를 추가할 수 있는 영역 */
  const itemsSlot = comp.createSlot();
  itemsSlot.name = 'Items';
  itemsSlot.layoutMode = 'HORIZONTAL';
  itemsSlot.itemSpacing = SPACING.xs;
  itemsSlot.primaryAxisAlignItems = 'MIN';
  itemsSlot.counterAxisAlignItems = 'CENTER';
  itemsSlot.resize(ITEMS_WIDTH, 1);     /* resize 먼저 */
  itemsSlot.primaryAxisSizingMode = 'FIXED';
  itemsSlot.counterAxisSizingMode = 'AUTO';
  clearFill(itemsSlot);

  /* 기본 Item 인스턴스 3개 배치 — 각 인스턴스에 label 오버라이드 적용
   * icon 오버라이드는 INSTANCE_SWAP key 매핑이 별도로 필요해 기본값(CreditCard) 유지 */
  for (const def of DEFAULT_ITEMS) {
    const inst = item.createInstance();
    inst.layoutGrow = 1; /* 균일 분할 */
    /* label TEXT property를 오버라이드해 인스턴스마다 다른 레이블 표시 */
    const labelKey = Object.keys(inst.componentProperties).find(k => k.startsWith('label#'));
    if (labelKey) {
      inst.setProperties({ [labelKey]: def.label });
    }
    itemsSlot.appendChild(inst);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
