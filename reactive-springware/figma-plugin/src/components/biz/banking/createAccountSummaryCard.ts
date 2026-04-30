/**
 * @file createAccountSummaryCard.ts
 * @description Figma AccountSummaryCard 컴포넌트 세트 생성.
 * Deposit / Savings / Loan 3가지 타입을 variant로 가진 ComponentSet을 반환한다.
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, setStroke, addText } from '../../../helpers';

const CARD_WIDTH = 328;

type AccountType = 'Deposit' | 'Savings' | 'Loan';

async function createAccountSummaryVariant(type: AccountType): Promise<ComponentNode> {
  const isLoan = type === 'Loan';
  const comp = createComponent(`Type=${type}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.sm);
  setPadding(comp, SPACING.lg, SPACING.lg);
  comp.resize(CARD_WIDTH, 140);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, COLOR.surface);
  setStroke(comp, COLOR.borderSubtle);

  /* 헤더 */
  const header = figma.createFrame();
  setAutoLayout(header, 'HORIZONTAL', SPACING.sm);
  header.layoutAlign = 'STRETCH';
  header.fills = [];
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  await addText(header, `하나 ${type === 'Loan' ? '신용대출' : type === 'Savings' ? '정기적금' : '급여통장'}`, FONT_SIZE.sm, COLOR.textHeading, true);
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0);
  setPadding(badge, 2, SPACING.sm);
  badge.cornerRadius = RADIUS.full;
  setFill(badge, BRAND.bg);
  await addText(badge, '주거래', FONT_SIZE.xs, BRAND.text, true);
  header.appendChild(badge);
  comp.appendChild(header);

  await addText(comp, '123-456789-01234', FONT_SIZE.xs, COLOR.textMuted);

  const balanceRow = figma.createFrame();
  setAutoLayout(balanceRow, 'HORIZONTAL', SPACING.xs);
  balanceRow.layoutAlign = 'STRETCH';
  balanceRow.fills = [];
  balanceRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
  await addText(balanceRow, isLoan ? '대출 잔액' : '잔액', FONT_SIZE.xs, COLOR.textMuted);
  await addText(balanceRow, '1,234,567원', FONT_SIZE.sm, isLoan ? COLOR.danger : COLOR.textHeading, true);
  comp.appendChild(balanceRow);

  return comp;
}

export async function createAccountSummaryCard(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all(['Deposit', 'Savings', 'Loan'].map((t) => createAccountSummaryVariant(t as AccountType))),
    'AccountSummaryCard', 3,
  );
}
