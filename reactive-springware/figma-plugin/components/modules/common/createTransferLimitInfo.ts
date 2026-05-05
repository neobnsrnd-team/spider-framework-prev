/**
 * @file createTransferLimitInfo.ts
 * @description Figma TransferLimitInfo 컴포넌트 세트 생성.
 * 1회·1일 이체 한도와 오늘 사용 누적 금액을 표시하는 안내 컴포넌트.
 * State(WithUsedAmount|WithoutUsedAmount) = 2 variants.
 *
 * TEXT properties:
 *   - perTransferLimit — 1회 이체 한도 (기본값: '10,000,000원')
 *   - dailyLimit       — 1일 이체 한도 (기본값: '100,000,000원')
 *   - usedAmount       — 오늘 이체 누적 금액, WithUsedAmount 전용 (기본값: '5,000,000원')
 *
 * [1일 잔여 한도 표현 — WithUsedAmount]
 *   값 영역: [dailyLimit TEXT 노드] " - " [usedAmount TEXT 노드]
 *   → dailyLimit / usedAmount 프로퍼티 키를 각 행 값 노드와 잔여 한도 계산식 노드에
 *     동시 바인딩한다. 하나의 TEXT property를 여러 노드에 참조시키는 방식으로
 *     "{dailyLimit} - {usedAmount}" 표현을 구현한다.
 *
 * [레이아웃]
 *   comp (VERTICAL, FIXED 390, AUTO, surfaceRaised, radiusXl)
 *     perTransferRow : '1회 이체 한도'  | [perTransferLimit]
 *     dailyLimitRow  : '1일 이체 한도'  | [dailyLimit]
 *     usedAmountRow  : '오늘 이체 누적' | [usedAmount]           — WithUsedAmount only
 *     remainingRow   : '1일 잔여 한도'  | [dailyLimit] - [usedAmount] — WithUsedAmount only
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(row) 이후 수동 바인딩 (2단계: comp → row → valueText).
 *   잔여 한도 계산식 노드는 3단계(comp → remainingRow → valueArea → text)이나
 *   comp 서브트리 내에 있으면 depth 무관하게 바인딩 가능.
 *
 * 컴포넌트 이름: "TransferLimitInfo"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';

type TransferLimitVariant = 'WithUsedAmount' | 'WithoutUsedAmount';

const ROW_H = 22; /* 행 기본 높이 */

async function createTransferLimitInfoVariant(
  variant: TransferLimitVariant,
): Promise<ComponentNode> {
  const comp = createComponent(`State=${variant}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MIN');
  setPadding(comp, SPACING.md, SPACING.md);
  comp.resize(390, 1);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(comp, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  /**
   * 레이블-값 단일 행 생성 후 comp에 추가.
   * comp.appendChild(row) 완료 후 반환된 TextNode에 componentPropertyReferences 바인딩 가능.
   */
  const makeRow = async (labelStr: string, valueStr: string): Promise<TextNode> => {
    const row = figma.createFrame();
    row.name = labelStr;
    setAutoLayout(row, 'HORIZONTAL', 0, 'CENTER');
    row.primaryAxisAlignItems = 'SPACE_BETWEEN';
    row.resize(390 - SPACING.md * 2, ROW_H);
    row.primaryAxisSizingMode = 'FIXED';
    row.counterAxisSizingMode = 'AUTO';
    clearFill(row);

    const labelText = await addTextWithVar(
      row, labelStr, FONT_SIZE.sm,
      COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm,
    );
    labelText.layoutGrow = 1;

    const valueText = await addTextWithVar(
      row, valueStr, FONT_SIZE.sm,
      COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm,
    );

    comp.appendChild(row);
    row.layoutSizingHorizontal = 'FILL'; /* comp에 append 이후 FILL 설정 */
    return valueText;
  };

  /* ── 1회 이체 한도 ───────────────────────────────────────── */
  const perTransferValueText = await makeRow('1회 이체 한도', '10,000,000원');
  const perTransferKey = comp.addComponentProperty('perTransferLimit', 'TEXT', '10,000,000원');
  perTransferValueText.componentPropertyReferences = { characters: perTransferKey };

  /* ── 1일 이체 한도 ───────────────────────────────────────── */
  const dailyLimitValueText = await makeRow('1일 이체 한도', '100,000,000원');
  const dailyLimitKey = comp.addComponentProperty('dailyLimit', 'TEXT', '100,000,000원');
  dailyLimitValueText.componentPropertyReferences = { characters: dailyLimitKey };

  if (variant === 'WithUsedAmount') {
    /* ── 오늘 이체 누적 ────────────────────────────────────── */
    const usedAmountValueText = await makeRow('오늘 이체 누적', '5,000,000원');
    const usedAmountKey = comp.addComponentProperty('usedAmount', 'TEXT', '5,000,000원');
    usedAmountValueText.componentPropertyReferences = { characters: usedAmountKey };

    /* ── 1일 잔여 한도: [dailyLimit] " - " [usedAmount] ─────
     * dailyLimitKey / usedAmountKey를 각 행 값 노드에 이미 바인딩했으나,
     * 같은 property key는 여러 노드에 동시 참조 가능하다.
     * → 잔여 한도 계산식 노드에도 동일 키를 재사용한다.                */
    const remainingRow = figma.createFrame();
    remainingRow.name = '1일 잔여 한도';
    setAutoLayout(remainingRow, 'HORIZONTAL', 0, 'CENTER');
    remainingRow.primaryAxisAlignItems = 'SPACE_BETWEEN';
    remainingRow.resize(390 - SPACING.md * 2, ROW_H);
    remainingRow.primaryAxisSizingMode = 'FIXED';
    remainingRow.counterAxisSizingMode = 'AUTO';
    clearFill(remainingRow);

    const remainingLabel = await addTextWithVar(
      remainingRow, '1일 잔여 한도', FONT_SIZE.sm,
      COLOR_VAR.textSecondary, COLOR.textSecondary, false, SIZE_VAR.fontSizeSm,
    );
    remainingLabel.layoutGrow = 1;

    /* 계산식 영역: [dailyLimit] " - " [usedAmount] */
    const valueArea = figma.createFrame();
    valueArea.name = 'RemainingValue';
    setAutoLayout(valueArea, 'HORIZONTAL', 0, 'CENTER');
    valueArea.primaryAxisSizingMode = 'AUTO';
    valueArea.counterAxisSizingMode = 'AUTO';
    clearFill(valueArea);
    remainingRow.appendChild(valueArea);

    /* comp.appendChild(remainingRow) 이후 valueArea의 자식들이 comp 서브트리에 포함됨 */
    comp.appendChild(remainingRow);
    remainingRow.layoutSizingHorizontal = 'FILL';

    /* dailyLimit 참조 — 같은 키를 재사용해 '1일 이체 한도' 행과 동기화 */
    const remainingDailyText = await addTextWithVar(
      valueArea, '100,000,000원', FONT_SIZE.sm,
      COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm,
    );
    remainingDailyText.componentPropertyReferences = { characters: dailyLimitKey };

    /* " - " 구분 텍스트 (고정) */
    await addTextWithVar(
      valueArea, ' - ', FONT_SIZE.sm,
      COLOR_VAR.textMuted, COLOR.textMuted, false, SIZE_VAR.fontSizeSm,
    );

    /* usedAmount 참조 — 같은 키를 재사용해 '오늘 이체 누적' 행과 동기화 */
    const remainingUsedText = await addTextWithVar(
      valueArea, '5,000,000원', FONT_SIZE.sm,
      COLOR_VAR.textBase, COLOR.textBase, false, SIZE_VAR.fontSizeSm,
    );
    remainingUsedText.componentPropertyReferences = { characters: usedAmountKey };
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
