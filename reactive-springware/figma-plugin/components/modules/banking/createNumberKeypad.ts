/**
 * @file createNumberKeypad.ts
 * @description Figma NumberKeypad 컴포넌트 생성.
 * 계좌 비밀번호 입력용 보안 숫자 키패드.
 *
 * 레이아웃 (3×4 그리드):
 *   행 1~3: 숫자 1~9 (샘플 배열)
 *   행 4:   재배열 | 0 | ⌫ (Delete)
 *
 * 컴포넌트 이름: "NumberKeypad"
 */

import { COLOR, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, clearFill,
  addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const KEYPAD_WIDTH = 390;
const CELL_HEIGHT  = 56; /* h-14 */
const CELL_WIDTH   = Math.floor(KEYPAD_WIDTH / 3); /* 3열 균등 분할 */

/** 숫자 셀 프레임 생성 */
async function createDigitCell(label: string): Promise<FrameNode> {
  const cell = figma.createFrame();
  setAutoLayout(cell, 'HORIZONTAL', 0);
  cell.resize(CELL_WIDTH, CELL_HEIGHT);
  cell.primaryAxisSizingMode = 'FIXED';
  cell.counterAxisSizingMode = 'FIXED';
  clearFill(cell);
  await setFloatVar(cell, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  await addTextWithVar(
    cell, label, FONT_SIZE.xl,
    COLOR_VAR.textHeading, COLOR.textHeading, true, SIZE_VAR.fontSizeXl,
  );
  return cell;
}

/** 아이콘 셀 프레임 생성 (삭제 버튼) */
function createDeleteCell(): FrameNode {
  const cell = figma.createFrame();
  setAutoLayout(cell, 'HORIZONTAL', 0);
  cell.resize(CELL_WIDTH, CELL_HEIGHT);
  cell.primaryAxisSizingMode = 'FIXED';
  cell.counterAxisSizingMode = 'FIXED';
  clearFill(cell);
  cell.appendChild(createIcon('Delete', 24, COLOR.textHeading));
  return cell;
}

export async function createNumberKeypad(): Promise<ComponentNode> {
  const comp = createComponent('NumberKeypad');
  comp.layoutMode = 'NONE';
  comp.resize(KEYPAD_WIDTH, CELL_HEIGHT * 4);
  clearFill(comp);

  /* 행 1~3: 숫자 1~9 샘플 배열 */
  const topDigits = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];
  for (let i = 0; i < topDigits.length; i++) {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const cell = await createDigitCell(topDigits[i]);
    cell.x = col * CELL_WIDTH;
    cell.y = row * CELL_HEIGHT;
    comp.appendChild(cell);
  }

  /* 행 4: 재배열 */
  const shuffleCell = await createDigitCell('재배열');
  /* 재배열 버튼은 base 크기로 표현 */
  const shuffleText = shuffleCell.findOne(n => n.type === 'TEXT') as TextNode | null;
  if (shuffleText) shuffleText.fontSize = FONT_SIZE.base;
  shuffleCell.x = 0;
  shuffleCell.y = CELL_HEIGHT * 3;
  comp.appendChild(shuffleCell);

  /* 행 4: 숫자 0 */
  const zeroCell = await createDigitCell('0');
  zeroCell.x = CELL_WIDTH;
  zeroCell.y = CELL_HEIGHT * 3;
  comp.appendChild(zeroCell);

  /* 행 4: 삭제 아이콘 */
  const deleteCell = createDeleteCell();
  deleteCell.x = CELL_WIDTH * 2;
  deleteCell.y = CELL_HEIGHT * 3;
  comp.appendChild(deleteCell);

  return comp;
}
