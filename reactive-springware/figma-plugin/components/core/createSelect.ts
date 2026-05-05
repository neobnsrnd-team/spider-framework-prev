/**
 * @file createSelect.ts
 * @description Figma Select 컴포넌트 세트 생성.
 *
 * State=Closed | State=Open  2개 variant로 구성한다.
 *   - Closed: 닫힌 드롭다운 트리거
 *   - Open:   열린 트리거 + 샘플 옵션 목록 (시각적 참조용)
 *
 * TEXT property:
 *   - value — 트리거에 표시되는 현재 선택값 (인스턴스에서 직접 편집 가능)
 *
 * options / onChange 는 비즈니스 코드에서 제공하므로 Figma variant로 표현하지 않는다.
 * 색상은 Figma 색상 변수에 바인딩하며, 변수가 없으면 tokens.ts의 RGB fallback 적용.
 */

import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, setStrokeWithVar, clearFill, clearStroke, addTextWithVar, setFloatVar,
} from '../../utils/helpers';
import { createIcon } from '../../utils/icons';

const SELECT_WIDTH  = 280;
const OPTION_HEIGHT = 44;  // 터치 최소 권장 크기

/**
 * 트리거 프레임의 레이아웃·스타일을 공통 적용한다.
 * ComponentNode 와 FrameNode 모두 지원하기 위해 유니온 타입을 사용한다.
 * @param open - true이면 브랜드 테두리(열린 상태), false이면 기본 테두리(닫힌 상태)
 */
async function applyTriggerStyle(
  node: FrameNode | ComponentNode,
  open: boolean,
): Promise<void> {
  setAutoLayout(node, 'HORIZONTAL', SPACING.xs);
  await setFloatVar(node, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  setPadding(node, 0, SPACING.md);
  await setFloatVar(node, 'paddingTop',    SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(node, 'paddingBottom', SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(node, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(node, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
  node.resize(SELECT_WIDTH, 48);
  node.primaryAxisSizingMode = 'FIXED';
  node.counterAxisSizingMode = 'FIXED';
  node.primaryAxisAlignItems = 'SPACE_BETWEEN';
  await setFloatVar(node, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(node, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(
    node,
    open ? COLOR_VAR.brandPrimary : COLOR_VAR.border,
    open ? BRAND.primary         : COLOR.border,
  );
}

/**
 * 샘플 옵션 항목 하나를 생성해 컨테이너에 추가한다.
 * @param selected - true이면 브랜드 배경 + Check 아이콘으로 강조 표시
 */
async function createOptionItem(
  container: FrameNode,
  label: string,
  selected: boolean,
): Promise<void> {
  const item = figma.createFrame();
  setAutoLayout(item, 'HORIZONTAL', 0);
  item.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(item, SPACING.sm, SPACING.md);
  await setFloatVar(item, 'paddingTop',    SIZE_VAR.spacingSm, SPACING.sm);
  await setFloatVar(item, 'paddingBottom', SIZE_VAR.spacingSm, SPACING.sm);
  await setFloatVar(item, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(item, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
  item.resize(SELECT_WIDTH, OPTION_HEIGHT);
  item.primaryAxisSizingMode = 'FIXED';
  item.counterAxisSizingMode = 'FIXED';
  clearStroke(item);

  if (selected) {
    await setFillWithVar(item, COLOR_VAR.brandBg, BRAND.bg);
  } else {
    clearFill(item);
  }

  const text = await addTextWithVar(
    item, label, FONT_SIZE.sm,
    selected ? COLOR_VAR.brandText : COLOR_VAR.textBase,
    selected ? BRAND.text          : COLOR.textBase,
    false, SIZE_VAR.fontSizeSm,
  );
  /* layoutGrow=1: 텍스트가 가로를 채워 Check 아이콘이 항상 우측에 위치 */
  text.layoutGrow = 1;

  if (selected) {
    item.appendChild(createIcon('Check', 16, BRAND.primary));
  }

  container.appendChild(item);
}

/**
 * State=Closed — 닫힌 드롭다운 트리거.
 * value TEXT property가 트리거 레이블에 직접 바인딩된다.
 */
async function createClosedVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=Closed');
  await applyTriggerStyle(comp, false);

  const label = await addTextWithVar(
    comp, '옵션 선택', FONT_SIZE.sm,
    COLOR_VAR.textPlaceholder, COLOR.textPlaceholder,
    false, SIZE_VAR.fontSizeSm, 'value',
  );
  label.layoutGrow = 1;

  comp.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));
  return comp;
}

/**
 * State=Open — 열린 트리거 + 샘플 옵션 목록.
 * value TEXT property는 트리거 내부 텍스트에 바인딩되며, comp(ComponentNode)에 등록된다.
 */
async function createOpenVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=Open');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';

  /* ── 트리거 ── */
  const trigger = figma.createFrame();
  await applyTriggerStyle(trigger, true);

  /* trigger를 comp에 먼저 추가해야 TEXT property reference 바인딩 가능 */
  comp.appendChild(trigger);

  /* value TEXT property: INSTANCE_SWAP처럼 comp에 등록, 노드는 trigger에 배치 */
  const label = await addTextWithVar(
    trigger, '옵션 선택', FONT_SIZE.sm,
    COLOR_VAR.brandText, BRAND.text,
    false, SIZE_VAR.fontSizeSm, 'value', comp,
  );
  label.layoutGrow = 1;
  trigger.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));

  /* ── 옵션 목록 컨테이너 ── */
  const container = figma.createFrame();
  setAutoLayout(container, 'VERTICAL', 0);
  container.primaryAxisSizingMode = 'AUTO';
  container.counterAxisSizingMode = 'FIXED';
  container.resize(SELECT_WIDTH, OPTION_HEIGHT * 4);
  await setFillWithVar(container, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(container, COLOR_VAR.border, COLOR.border);
  await setFloatVar(container, 'cornerRadius', SIZE_VAR.radiusMd, RADIUS.md);
  /* 모서리 밖으로 항목이 삐져나오지 않도록 클리핑 */
  container.clipsContent = true;

  await createOptionItem(container, '옵션 1', true);
  await createOptionItem(container, '옵션 2', false);
  await createOptionItem(container, '옵션 3', false);
  await createOptionItem(container, '옵션 4', false);

  comp.appendChild(container);
  return comp;
}

export async function createSelect(): Promise<ComponentSetNode> {
  const [closed, open] = await Promise.all([
    createClosedVariant(),
    createOpenVariant(),
  ]);
  return combineVariants([closed, open], 'Select', 2);
}
