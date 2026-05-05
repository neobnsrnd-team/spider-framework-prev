/**
 * @file createOtpInput.ts
 * @description Figma OtpInput 컴포넌트 세트 생성.
 * Length(4|6) × Error(True|False) × Masked(True|False) = 8 variants.
 *
 * [Masked 구분]
 *   Masked=False: 활성 셀에 숫자('1') 표시 — 입력값 노출
 *   Masked=True : 활성 셀에 마스크('●') 표시 — 입력값 숨김
 *
 * [셀 상태]
 *   활성(i=0): surface 배경 + brandPrimary 테두리
 *   비활성   : surfaceSubtle 배경 + border 테두리
 *   Error=True: 전체 셀 dangerSurface 배경 + danger 테두리
 *
 * 컴포넌트 이름: "OtpInput"
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, clearFill,
  setFill, setStroke, addText,
} from '../../../utils/helpers';

async function createOtpVariant(
  length: 4 | 6,
  error: boolean,
  masked: boolean,
): Promise<ComponentNode> {
  const cellSize  = 44;
  const totalWidth = length * cellSize + (length - 1) * SPACING.sm;

  const comp = createComponent(
    `Length=${length}, Error=${error ? 'True' : 'False'}, Masked=${masked ? 'True' : 'False'}`,
  );
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  comp.resize(totalWidth, cellSize);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  for (let i = 0; i < length; i++) {
    const cell = figma.createFrame();
    cell.resize(cellSize, cellSize);
    cell.cornerRadius = RADIUS.sm;
    cell.primaryAxisSizingMode = 'FIXED';
    cell.counterAxisSizingMode = 'FIXED';
    setAutoLayout(cell, 'HORIZONTAL', 0);
    cell.primaryAxisAlignItems = 'CENTER';
    cell.counterAxisAlignItems = 'CENTER';
    cell.strokeWeight = 1;
    cell.strokeAlign  = 'INSIDE';

    const isActive = i === 0; /* 첫 번째 셀을 활성(입력 중) 상태로 표현 */

    if (error) {
      setFill(cell, COLOR.dangerSurface);
      setStroke(cell, COLOR.danger);
    } else if (isActive) {
      setFill(cell, COLOR.surface);
      setStroke(cell, BRAND.primary);
    } else {
      setFill(cell, COLOR.surfaceSubtle);
      setStroke(cell, COLOR.border);
    }

    /* 활성 셀: Masked에 따라 마스크 문자(●) 또는 숫자(1) 표시 */
    if (isActive) {
      const displayChar = masked ? '●' : '1';
      const textColor   = error ? COLOR.danger : COLOR.textHeading;
      await addText(cell, displayChar, FONT_SIZE.base, textColor, true);
    }
    /* 비활성 셀: 텍스트 없음 (빈 셀) */

    comp.appendChild(cell);
  }

  return comp;
}

export async function createOtpInput(): Promise<ComponentSetNode> {
  const components: ComponentNode[] = [];
  for (const length of [4, 6] as (4 | 6)[]) {
    for (const error of [false, true]) {
      for (const masked of [false, true]) {
        components.push(await createOtpVariant(length, error, masked));
      }
    }
  }
  /* cols=4: Error(F/T) × Masked(F/T) 를 한 행에 배치, Length별로 행 구분 */
  return combineVariants(components, 'OtpInput', 4);
}
