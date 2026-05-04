/**
 * @file createSelect.ts
 * @description Figma Select 컴포넌트 세트 생성.
 * 드롭다운 닫힘/열림/옵션펼침 세 상태를 Figma variant로 매핑한다.
 * 컴포넌트 이름: "Select"
 * Variant 형식: "State=Closed" | "State=Open" | "State=OpenWithOptions"
 *
 * 색상 → COLOR_VAR + setFillWithVar / setStrokeWithVar / addTextWithVar
 * 수치(spacing·radius·fontSize) → SIZE_VAR + setFloatVar
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, setStrokeWithVar, clearFill, clearStroke, addTextWithVar, setFloatVar,
} from '../../helpers';
import { createIcon } from '../../icons';

const SELECT_WIDTH = 280;
/** 옵션 항목 높이 — 터치 최소 권장 크기 44px */
const OPTION_HEIGHT = 44;

async function createSelectVariant(state: 'Closed' | 'Open'): Promise<ComponentNode> {
  const comp = createComponent(`State=${state}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.xs);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  setPadding(comp, 0, SPACING.md);
  await setFloatVar(comp, 'paddingTop',    SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(comp, 'paddingBottom', SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(comp, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(comp, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
  comp.resize(SELECT_WIDTH, 48);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);

  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);
  if (state === 'Open') {
    await setStrokeWithVar(comp, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setStrokeWithVar(comp, COLOR_VAR.border, COLOR.border);
  }

  const label = await addTextWithVar(
    comp, '옵션 선택', FONT_SIZE.sm,
    state === 'Open' ? COLOR_VAR.brandText : COLOR_VAR.textPlaceholder,
    state === 'Open' ? BRAND.text : COLOR.textPlaceholder,
    false,
    SIZE_VAR.fontSizeSm,
  );
  label.layoutGrow = 1;

  /* ChevronDown 아이콘: createIcon은 변수 바인딩 미지원 — fallback RGB 사용 */
  comp.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));

  return comp;
}

/**
 * 옵션 항목 하나를 FrameNode로 생성해 부모 컨테이너에 추가한다.
 * selected=true인 항목은 브랜드 컬러 배경 + Check 아이콘으로 강조 표시된다.
 *
 * @param parent   - 항목을 추가할 부모 프레임 (옵션 목록 컨테이너)
 * @param label    - 옵션 텍스트
 * @param selected - 현재 선택된 항목 여부
 */
async function createOptionItem(
  parent: FrameNode | ComponentNode,
  label: string,
  selected: boolean,
): Promise<void> {
  const item = figma.createFrame();
  setAutoLayout(item, 'HORIZONTAL', 0);
  /* label은 좌측, Check 아이콘은 우측에 배치 */
  item.primaryAxisAlignItems = 'SPACE_BETWEEN';
  setPadding(item, SPACING.sm, SPACING.md);
  await setFloatVar(item, 'paddingTop',    SIZE_VAR.spacingSm, SPACING.sm);
  await setFloatVar(item, 'paddingBottom', SIZE_VAR.spacingSm, SPACING.sm);
  await setFloatVar(item, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(item, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
  item.resize(SELECT_WIDTH, OPTION_HEIGHT);
  item.primaryAxisSizingMode = 'FIXED';
  item.counterAxisSizingMode = 'FIXED';
  /* 항목 간 구분은 컨테이너 VERTICAL gap이 아닌 배경색으로만 표현 */
  clearStroke(item);

  if (selected) {
    /* 선택된 항목: 브랜드 배경색으로 강조 */
    await setFillWithVar(item, COLOR_VAR.brandBg, BRAND.bg);
  } else {
    clearFill(item);
  }

  const text = await addTextWithVar(
    item, label, FONT_SIZE.sm,
    selected ? COLOR_VAR.brandText : COLOR_VAR.textBase,
    selected ? BRAND.text : COLOR.textBase,
    false,
    SIZE_VAR.fontSizeSm,
  );
  /* 텍스트가 가로 공간을 채워 아이콘이 항상 우측 끝에 위치하도록 설정 */
  text.layoutGrow = 1;

  if (selected) {
    /* 선택 항목에만 Check 아이콘 표시 */
    item.appendChild(createIcon('Check', 16, BRAND.primary));
  }

  parent.appendChild(item);
}

/**
 * State=OpenWithOptions variant 생성.
 * 트리거 프레임(Open 스타일) 아래 옵션 목록 컨테이너(4개 항목)를 렌더링한다.
 * 첫 번째 항목이 선택된 상태로 표현된다.
 */
async function createOpenWithOptionsVariant(): Promise<ComponentNode> {
  const comp = createComponent('State=OpenWithOptions');
  setAutoLayout(comp, 'VERTICAL', SPACING.xs);
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  /* 높이는 자식(트리거 + 컨테이너)에 맞게 자동 조정 */
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';

  /* ── 트리거 (Open 상태와 동일한 스타일) ── */
  const trigger = figma.createFrame();
  setAutoLayout(trigger, 'HORIZONTAL', SPACING.xs);
  await setFloatVar(trigger, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  setPadding(trigger, 0, SPACING.md);
  await setFloatVar(trigger, 'paddingTop',    SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(trigger, 'paddingBottom', SIZE_VAR.spacing0,  SPACING['0']);
  await setFloatVar(trigger, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
  await setFloatVar(trigger, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
  trigger.resize(SELECT_WIDTH, 48);
  trigger.primaryAxisSizingMode = 'FIXED';
  trigger.counterAxisSizingMode = 'FIXED';
  trigger.primaryAxisAlignItems = 'SPACE_BETWEEN';
  await setFloatVar(trigger, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(trigger, COLOR_VAR.surface, COLOR.surface);
  await setStrokeWithVar(trigger, COLOR_VAR.brandPrimary, BRAND.primary);

  const triggerLabel = await addTextWithVar(
    trigger, '옵션 선택', FONT_SIZE.sm,
    COLOR_VAR.brandText, BRAND.text, false, SIZE_VAR.fontSizeSm,
  );
  triggerLabel.layoutGrow = 1;
  trigger.appendChild(createIcon('ChevronDown', 16, COLOR.textMuted));
  comp.appendChild(trigger);

  /* ── 옵션 목록 컨테이너 ── */
  const container = figma.createFrame();
  setAutoLayout(container, 'VERTICAL', 0);
  /* 높이는 항목 개수에 맞게 자동 조정 (hug content) */
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
    createSelectVariant('Closed'),
    createSelectVariant('Open'),
  ]);
  /* variant가 3개이므로 3열로 배치 */
  return combineVariants([closed, open, await createOpenWithOptionsVariant()], 'Select', 3);
}
