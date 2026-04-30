/**
 * @file createCard.ts
 * @description Figma Card 컴포넌트 세트 생성.
 * React Card + CardHeader + CardRow 서브컴포넌트를 Figma variant로 표현한다.
 *
 * 컴포넌트 이름: "Card"
 * Variant 형식: "Type=Default" | "Type=Interactive" | "Type=WithHeader" | "Type=WithRow"
 */

import { COLOR, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setStroke, addText, addDivider,
} from '../../../helpers';

const CARD_WIDTH = 328; // 390px 화면 기준 양쪽 standard(16px) 패딩 제외

async function createDefaultCard(): Promise<ComponentNode> {
  const comp = createComponent('Type=Default');
  setAutoLayout(comp, 'VERTICAL', SPACING.sm);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(CARD_WIDTH, 80);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, COLOR.surface);
  setStroke(comp, COLOR.borderSubtle);

  await addText(comp, '카드 내용', FONT_SIZE.sm, COLOR.textBase);
  return comp;
}

async function createInteractiveCard(): Promise<ComponentNode> {
  const comp = createComponent('Type=Interactive');
  setAutoLayout(comp, 'VERTICAL', SPACING.sm);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(CARD_WIDTH, 80);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, COLOR.surface);
  /* interactive hover 상태를 테두리 색상으로 표현 */
  setStroke(comp, COLOR.border, 1.5);

  await addText(comp, '클릭 가능한 카드', FONT_SIZE.sm, COLOR.textBase);
  return comp;
}

async function createCardWithHeader(): Promise<ComponentNode> {
  const comp = createComponent('Type=WithHeader');
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(CARD_WIDTH, 120);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, COLOR.surface);
  setStroke(comp, COLOR.borderSubtle);

  /* CardHeader 영역 */
  const header = figma.createFrame();
  header.name = 'CardHeader';
  setAutoLayout(header, 'HORIZONTAL', SPACING.sm);
  header.layoutAlign = 'STRETCH';
  header.primaryAxisSizingMode = 'FIXED';
  header.counterAxisSizingMode = 'AUTO';
  header.fills = [];

  /* 타이틀 */
  const title = await addText(header, '카드 제목', FONT_SIZE.sm, COLOR.textHeading, true);
  title.layoutGrow = 1;
  const subtitle = await addText(header, '부제목', FONT_SIZE.xs, COLOR.textMuted);
  subtitle.layoutGrow = 0;

  comp.appendChild(header);
  addDivider(comp, COLOR.borderSubtle);
  await addText(comp, '카드 내용 영역', FONT_SIZE.sm, COLOR.textBase);

  return comp;
}

async function createCardWithRow(): Promise<ComponentNode> {
  const comp = createComponent('Type=WithRow');
  setAutoLayout(comp, 'VERTICAL', 0);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(CARD_WIDTH, 140);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'FIXED';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, COLOR.surface);
  setStroke(comp, COLOR.borderSubtle);

  /* CardRow 2개 예시 */
  const rows = [
    { label: '레이블', value: '값' },
    { label: '잔액', value: '1,234,567원' },
  ];

  for (let i = 0; i < rows.length; i++) {
    const { label, value } = rows[i];
    const row = figma.createFrame();
    row.name = 'CardRow';
    setAutoLayout(row, 'HORIZONTAL', SPACING.md);
    row.layoutAlign = 'STRETCH';
    row.primaryAxisSizingMode = 'FIXED';
    row.counterAxisSizingMode = 'AUTO';
    row.primaryAxisAlignItems = 'SPACE_BETWEEN';
    row.fills = [];
    row.paddingTop = row.paddingBottom = SPACING.xs;

    const lbl = await addText(row, label, FONT_SIZE.xs, COLOR.textMuted);
    lbl.layoutGrow = 0;
    const val = await addText(row, value, FONT_SIZE.sm, COLOR.textHeading, true);
    val.layoutGrow = 0;

    comp.appendChild(row);
    /* 마지막 행 제외 구분선 */
    if (i < rows.length - 1) addDivider(comp, COLOR.borderSubtle);
  }

  return comp;
}

export async function createCard(): Promise<ComponentSetNode> {
  const components = [
    await createDefaultCard(),
    await createInteractiveCard(),
    await createCardWithHeader(),
    await createCardWithRow(),
  ];
  /* cols=2: 4개 variant를 2행 2열로 배치 */
  return combineVariants(components, 'Card', 2);
}
