/**
 * @file createTransferLimitInfo.ts
 * @description Figma TransferLimitInfo 컴포넌트 세트 생성.
 * 1회·1일 이체 한도와 오늘 사용 누적 금액을 표시하는 안내 컴포넌트.
 * State(WithUsedAmount|WithoutUsedAmount) = 2 variants.
 * 컴포넌트 이름: "TransferLimitInfo"
 */

import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../helpers';

type TransferLimitVariant = 'WithUsedAmount' | 'WithoutUsedAmount';

/** LabelValueRow 단일 행 생성 */
async function createRow(
  parent: FrameNode | ComponentNode,
  label: string,
  value: string,
): Promise<void> {
  const row = figma.createFrame();
  setAutoLayout(row, 'HORIZONTAL', 0);
  row.primaryAxisAlignItems = 'SPACE_BETWEEN';
  row.resize(350, 20);
  row.primaryAxisSizingMode = 'FIXED';
  row.counterAxisSizingMode = 'AUTO';
  clearFill(row);

  const labelText = await addTextWithVar(
    row, label, FONT_SIZE.sm,
    COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm,
  );
  labelText.layoutGrow = 1;

  await addTextWithVar(
    row, value, FONT_SIZE.sm,
    COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm,
  );

  parent.appendChild(row);
}

async function createTransferLimitInfoVariant(variant: TransferLimitVariant): Promise<ComponentNode> {
  const comp = createComponent(`State=${variant}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  /* 헤더 */
  const header = await addTextWithVar(
    comp, '이체 한도 안내', FONT_SIZE.xs,
    COLOR_VAR.textSecondary, COLOR.textSecondary, true, SIZE_VAR.fontSizeXs,
  );
  header.layoutAlign = 'STRETCH';

  /* 기본 행 */
  await createRow(comp, '1회 이체 한도', '10,000,000원');
  await createRow(comp, '1일 이체 한도', '100,000,000원');

  /* usedAmount 전달 시 추가 행 */
  if (variant === 'WithUsedAmount') {
    await createRow(comp, '오늘 이체 누적', '5,000,000원');
    await createRow(comp, '1일 잔여 한도', '95,000,000원');
  }

  return comp;
}

export async function createTransferLimitInfo(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createTransferLimitInfoVariant('WithUsedAmount'),
    createTransferLimitInfoVariant('WithoutUsedAmount'),
  ]);
  return combineVariants(components, 'TransferLimitInfo', 2);
}
