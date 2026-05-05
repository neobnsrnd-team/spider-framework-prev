/**
 * @file createAccountSelectItem.ts
 * @description Figma AccountSelectItem 컴포넌트 세트 생성.
 * Selected(True|False) 2 variants.
 *
 * TEXT properties:
 *   - accountName   — 계좌 명칭 (기본값: '하나 급여통장')
 *   - accountNumber — 계좌번호 (기본값: '123-456789-01234')
 *   - balance       — 잔액 (기본값: '1,234,567원')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 은행/계좌 아이콘 (기본값: Landmark, swap 가능)
 *
 * [레이아웃]
 *   comp(HORIZONTAL, gap=md, padding=md+standard, FIXED 390×72)
 *     IconWrap(HORIZONTAL, CENTER, 40×40, radiusFull)
 *       icon(INSTANCE_SWAP, 24px)
 *     TextArea(VERTICAL, gap=xs, grow=1)
 *       accountName(TEXT, sm, textHeading, bold)
 *       accountNumber(TEXT, xs, textSecondary)
 *       balance(TEXT, sm, textHeading, bold)
 *     Check(20, brandPrimary) — selected only
 *
 * 배경색:
 *   Selected=False: surface(흰색) / Selected=True: brandBg(연한 초록)
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textArea) 이후 수동 바인딩 (2단계: comp → textArea → text)
 *
 * 컴포넌트 이름: "AccountSelectItem"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFillWithVar, addTextWithVar, addIconSlot,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

async function createAccountSelectVariant(selected: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Selected=${selected ? 'True' : 'False'}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, SPACING.md, SPACING.standard);
  comp.resize(390, 72);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.counterAxisAlignItems = 'CENTER';

  /* 배경: 미선택=surface(흰색), 선택=brandBg(연한 초록) */
  if (selected) {
    await setFillWithVar(comp, COLOR_VAR.brandBg, BRAND.bg);
  } else {
    await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  }

  /* 아이콘 원형 컨테이너 — 선택 여부에 따라 배경 전환 */
  const iconWrap = figma.createFrame();
  setAutoLayout(iconWrap, 'HORIZONTAL', 0, 'CENTER');
  iconWrap.resize(40, 40);
  iconWrap.primaryAxisSizingMode = 'FIXED';
  iconWrap.counterAxisSizingMode = 'FIXED';
  iconWrap.primaryAxisAlignItems = 'CENTER';
  iconWrap.cornerRadius = RADIUS.full;
  if (selected) {
    await setFillWithVar(iconWrap, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(iconWrap, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  }
  comp.appendChild(iconWrap);

  /* INSTANCE_SWAP: icon (bankIcon → icon으로 변경, Landmark 기본값) */
  const iconColor = selected ? BRAND.fg : COLOR.textMuted;
  addIconSlot(comp, 'Landmark', 24, iconColor, 'icon', iconWrap);

  /* 텍스트 영역 — comp.appendChild 이후 하위 노드 바인딩 가능 */
  const textArea = figma.createFrame();
  setAutoLayout(textArea, 'VERTICAL', SPACING.xs);
  clearFill(textArea);
  comp.appendChild(textArea);
  textArea.layoutGrow = 1;

  /* accountName — 2단계 수동 바인딩 (comp → textArea → text) */
  const accountNameText = await addTextWithVar(
    textArea, '하나 급여통장', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );
  const accountNameKey = comp.addComponentProperty('accountName', 'TEXT', '하나 급여통장');
  accountNameText.componentPropertyReferences = { characters: accountNameKey };

  /* accountNumber */
  const accountNumberText = await addTextWithVar(
    textArea, '123-456789-01234', FONT_SIZE.xs,
    COLOR_VAR.textSecondary, COLOR.textSecondary,
    false, SIZE_VAR.fontSizeXs,
  );
  const accountNumberKey = comp.addComponentProperty('accountNumber', 'TEXT', '123-456789-01234');
  accountNumberText.componentPropertyReferences = { characters: accountNumberKey };

  /* balance */
  const balanceText = await addTextWithVar(
    textArea, '1,234,567원', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm,
  );
  const balanceKey = comp.addComponentProperty('balance', 'TEXT', '1,234,567원');
  balanceText.componentPropertyReferences = { characters: balanceKey };

  /* 선택 체크 아이콘 — selected 상태에서만 표시 */
  if (selected) {
    comp.appendChild(createIcon('Check', 20, BRAND.primary));
  }

  return comp;
}

export async function createAccountSelectItem(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([
      createAccountSelectVariant(false),
      createAccountSelectVariant(true),
    ]),
    'AccountSelectItem', 1,
  );
}
