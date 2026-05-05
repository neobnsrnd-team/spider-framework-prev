/**
 * @file createAccountSelectorCard.ts
 * @description Figma AccountSelectorCard 컴포넌트 생성.
 * 계좌 선택 영역을 표현하는 단일 ComponentNode를 반환한다.
 *
 * TEXT properties:
 *   - accountName      — 계좌명 (기본값: '하나 급여통장')
 *   - accountNumber    — 계좌번호 (기본값: '123-456789-01234')
 *   - availableBalance — 출금가능금액 (기본값: '출금가능 1,234,567원')
 *
 * INSTANCE_SWAP properties:
 *   - icon — 우측 원형 버튼 아이콘 (기본값: WalletMinimal, swap 가능)
 *
 * [레이아웃 — 실제 컴포넌트 기준]
 *   comp(HORIZONTAL, gap=md, FIXED 390, AUTO height, padding=21+lg, radiusXl)
 *     left(VERTICAL, gap=xs, FILL)
 *       nameRow(HORIZONTAL, gap=sm, FILL)
 *         accountName(TEXT base, textHeading, FILL, LEFT, truncate)
 *         ChevronDown(10px)
 *       accountNumber(TEXT sm, brandText, bold, FILL, LEFT)
 *       availableBalance(TEXT sm, brandText, bold, FILL, LEFT)
 *     rightBtn(48×48, radiusFull, brandBg)
 *       icon(INSTANCE_SWAP, 18px)
 *
 * TEXT property 바인딩 타이밍:
 *   accountName     — comp → left → nameRow → text: 수동 바인딩
 *   accountNumber   — comp → left → text: 수동 바인딩
 *   availableBalance — comp → left → text: 수동 바인딩
 *
 * 컴포넌트 이름: "AccountSelectorCard"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, setStrokeWithVar, addTextWithVar, setFloatVar, addIconSlot,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createAccountSelectorCard(): Promise<ComponentNode> {
  const comp = createComponent('AccountSelectorCard');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.md);
  setPadding(comp, 21, SPACING.lg); /* py-[21px] px-lg */
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO'; /* height는 콘텐츠에 맞게 */
  comp.counterAxisAlignItems = 'CENTER';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  comp.strokeWeight = 1;
  comp.strokeAlign = 'INSIDE';

  /* 좌측 텍스트 영역 — comp.appendChild 후 FILL 설정 */
  const left = figma.createFrame();
  setAutoLayout(left, 'VERTICAL', SPACING.xs);
  left.primaryAxisSizingMode = 'AUTO';
  left.counterAxisAlignItems = 'MIN'; /* 자식을 왼쪽 정렬 */
  clearFill(left);
  comp.appendChild(left);
  left.layoutSizingHorizontal = 'FILL'; /* 텍스트 overflow 방지 */

  /* ── 계좌명 행: accountName + ChevronDown ── */
  const nameRow = figma.createFrame();
  setAutoLayout(nameRow, 'HORIZONTAL', SPACING.sm);
  nameRow.primaryAxisAlignItems = 'MIN'; /* setAutoLayout 기본값 CENTER를 LEFT로 덮어씀 */
  nameRow.counterAxisAlignItems = 'CENTER';
  nameRow.primaryAxisSizingMode = 'AUTO';
  nameRow.counterAxisSizingMode = 'AUTO'; /* height 텍스트 높이에 맞게 */
  nameRow.clipsContent = true; /* 텍스트가 nameRow 밖으로 삐져나가지 않도록 클리핑 */
  clearFill(nameRow);
  left.appendChild(nameRow);
  nameRow.layoutSizingHorizontal = 'FILL'; /* 행 너비를 left에 맞춤 */

  const accountNameNode = await addTextWithVar(
    nameRow, '하나 급여통장', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    false, SIZE_VAR.fontSizeBase,
  );
  /* FILL 미사용: ChevronDown이 텍스트 바로 옆에 붙도록 AUTO 너비 유지 */
  accountNameNode.textAlignHorizontal = 'LEFT';
  const accountNameKey = comp.addComponentProperty('accountName', 'TEXT', '하나 급여통장');
  accountNameNode.componentPropertyReferences = { characters: accountNameKey };

  nameRow.appendChild(createIcon('ChevronDown', 10, COLOR.textMuted));

  /* ── 계좌번호 ── */
  const accountNumberNode = await addTextWithVar(
    left, '123-456789-01234', FONT_SIZE.sm,
    COLOR_VAR.brandText, BRAND.text,
    true, SIZE_VAR.fontSizeSm,
  );
  accountNumberNode.layoutSizingHorizontal = 'FILL';
  accountNumberNode.textAlignHorizontal = 'LEFT';
  const accountNumberKey = comp.addComponentProperty('accountNumber', 'TEXT', '123-456789-01234');
  accountNumberNode.componentPropertyReferences = { characters: accountNumberKey };

  /* ── 출금가능금액 ── */
  const availableBalanceNode = await addTextWithVar(
    left, '출금가능 1,234,567원', FONT_SIZE.sm,
    COLOR_VAR.brandText, BRAND.text,
    true, SIZE_VAR.fontSizeSm,
  );
  availableBalanceNode.layoutSizingHorizontal = 'FILL';
  availableBalanceNode.textAlignHorizontal = 'LEFT';
  const availableBalanceKey = comp.addComponentProperty('availableBalance', 'TEXT', '출금가능 1,234,567원');
  availableBalanceNode.componentPropertyReferences = { characters: availableBalanceKey };

  /* ── 우측 원형 아이콘 버튼 ── */
  const rightBtn = figma.createFrame();
  setAutoLayout(rightBtn, 'HORIZONTAL', 0);
  rightBtn.resize(48, 48);
  rightBtn.primaryAxisSizingMode = 'FIXED';
  rightBtn.counterAxisSizingMode = 'FIXED';
  rightBtn.primaryAxisAlignItems = 'CENTER';
  rightBtn.counterAxisAlignItems = 'CENTER';
  await setFloatVar(rightBtn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(rightBtn, COLOR_VAR.brandBg, BRAND.bg);
  comp.appendChild(rightBtn);

  /* INSTANCE_SWAP: icon (기본값 WalletMinimal, Figma 패널에서 swap 가능) */
  addIconSlot(comp, 'WalletMinimal', 18, BRAND.primary, 'icon', rightBtn);

  figma.currentPage.appendChild(comp);
  return comp;
}
