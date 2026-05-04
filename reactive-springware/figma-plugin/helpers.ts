/**
 * @file helpers.ts
 * @description Figma Plugin API 반복 작업을 줄이기 위한 헬퍼 함수 모음.
 * ComponentNode / FrameNode 생성·스타일·레이아웃 설정을 단순화한다.
 */

import type { RGB } from './tokens';
import { FONT_FAMILY, FONT_VAR } from './tokens';
import type { IconName } from './icons';

/** SolidPaint 생성 */
export function solid(color: RGB, opacity = 1): SolidPaint {
  return { type: 'SOLID', color, opacity };
}

/** 단색 fill 적용 */
export function setFill(node: GeometryMixin, color: RGB, opacity = 1): void {
  node.fills = [solid(color, opacity)];
}

/**
 * Figma 색상 변수를 fill에 바인딩한다.
 * 변수명이 파일에 존재하지 않으면 fallback RGB를 직접 적용한다.
 *
 * @param node         - fill을 적용할 노드
 * @param variableName - Figma 변수 전체 경로 (예: 'brand/text', 'success/surface')
 * @param fallback     - 변수를 찾지 못했을 때 사용할 RGB 값
 */
/**
 * Figma 색상 변수를 fill에 비동기로 바인딩한다.
 * dynamic-page documentAccess 환경에서는 getLocalVariablesAsync()를 사용해야 한다.
 * 변수를 찾지 못하거나 오류 발생 시 fallback RGB를 직접 적용한다.
 *
 * @param node         - fill을 적용할 노드
 * @param variableName - Figma 변수 전체 경로 (예: 'color/brand/text')
 * @param fallback     - 변수를 찾지 못했을 때 사용할 RGB 값
 */
export async function setFillWithVar(
  node: GeometryMixin,
  variableName: string,
  fallback: RGB,
): Promise<void> {
  try {
    const allVars = await figma.variables.getLocalVariablesAsync('COLOR');
    const variable = allVars.find(v => v.name === variableName);

    if (variable) {
      const paint: SolidPaint = { type: 'SOLID', color: fallback };
      node.fills = [figma.variables.setBoundVariableForPaint(paint, 'color', variable)];
      return;
    }
  } catch (err) {
    /* 변수 API 오류 시 fallback으로 진행 */
  }
  node.fills = [solid(fallback)];
}

/** fill 제거 (투명) */
export function clearFill(node: GeometryMixin): void {
  node.fills = [];
}

/** 단색 stroke 적용 */
export function setStroke(
  node: GeometryMixin & IndividualStrokesMixin,
  color: RGB,
  weight = 1,
): void {
  node.strokes = [solid(color)];
  node.strokeWeight = weight;
  node.strokeAlign = 'INSIDE';
}

/** stroke 제거 */
export function clearStroke(node: GeometryMixin): void {
  node.strokes = [];
}

/**
 * Auto Layout(Flex) 설정.
 * @param direction 'HORIZONTAL' | 'VERTICAL'
 * @param gap itemSpacing (gap)
 * @param align counterAxisAlignItems — 기본 CENTER
 */
export function setAutoLayout(
  node: FrameNode | ComponentNode,
  direction: 'HORIZONTAL' | 'VERTICAL',
  gap = 0,
  align: 'MIN' | 'CENTER' | 'MAX' | 'BASELINE' = 'CENTER',
): void {
  node.layoutMode = direction;
  node.itemSpacing = gap;
  node.counterAxisAlignItems = align;
  node.primaryAxisAlignItems = 'CENTER';
}

/**
 * 상하좌우 padding 설정.
 * 인자를 (vertical, horizontal) 또는 (top, right, bottom, left) 방식으로 전달.
 */
export function setPadding(
  node: FrameNode | ComponentNode,
  top: number,
  right: number,
  bottom = top,
  left = right,
): void {
  node.paddingTop = top;
  node.paddingRight = right;
  node.paddingBottom = bottom;
  node.paddingLeft = left;
}

/**
 * Figma STRING 변수를 TextNode의 특정 필드에 비동기로 바인딩한다.
 * 변수를 찾지 못하거나 오류 발생 시 기존 값을 유지한다.
 *
 * @param node         - 바인딩할 TextNode
 * @param field        - 바인딩 대상 필드 (예: 'fontFamily')
 * @param variableName - Figma 변수 전체 경로 (예: 'font/sans')
 */
export async function setStringVar(
  node: TextNode,
  field: VariableBindableTextField,
  variableName: string,
): Promise<void> {
  try {
    const allVars = await figma.variables.getLocalVariablesAsync('STRING');
    const variable = allVars.find(v => v.name === variableName);
    if (variable) {
      node.setBoundVariable(field, variable);
    }
  } catch {
    /* 변수 API 오류 시 fontName 값 유지 */
    figma.notify(`⚠️ '${variableName}' 변수 API 오류로 fontName 값 유지합니다.`, { error: true });
  }
}

/**
 * TextNode 폰트·크기·색상 일괄 설정 후 fontFamily를 Figma 변수에 바인딩한다.
 * loadFontAsync 호출 후 사용해야 한다.
 */
export async function applyText(
  text: TextNode,
  characters: string,
  fontSize: number,
  color: RGB,
  bold = false,
): Promise<void> {
  text.fontName = { family: FONT_FAMILY.sans, style: bold ? 'Bold' : 'Regular' };
  text.fontSize = fontSize;
  text.characters = characters;
  setFill(text, color);
  /* fontFamily를 Figma font/sans 변수에 바인딩 — 변수 미존재 시 fontName 값 유지 */
  await setStringVar(text, 'fontFamily', FONT_VAR.sans);
}

/** TextNode를 생성하고 부모에 추가한 뒤 반환 */
export async function addText(
  parent: FrameNode | ComponentNode,
  characters: string,
  fontSize: number,
  color: RGB,
  bold = false,
): Promise<TextNode> {
  const text = figma.createText();
  await applyText(text, characters, fontSize, color, bold);
  parent.appendChild(text);
  return text;
}

/**
 * Figma FLOAT 변수를 노드의 수치 속성에 바인딩한다.
 * COLOR 변수와 달리 fill이 아닌 cornerRadius/padding/itemSpacing/fontSize 등
 * 수치 필드에 직접 바인딩하며, 변수를 찾지 못하면 fallback 숫자값을 적용한다.
 *
 * @param node         - 바인딩할 노드 (FrameNode, ComponentNode, TextNode 등)
 * @param field        - 바인딩 대상 필드 (예: 'cornerRadius', 'paddingTop', 'fontSize')
 * @param variableName - Figma 변수 전체 경로 (예: 'radius/full', 'spacing/xl')
 * @param fallback     - 변수를 찾지 못했을 때 사용할 숫자값
 */
export async function setFloatVar(
  node: BaseNode,
  field: string,
  variableName: string,
  fallback: number,
): Promise<void> {
  try {
    const allVars = await figma.variables.getLocalVariablesAsync('FLOAT');
    const variable = allVars.find(v => v.name === variableName);
    if (variable) {
      /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
      (node as any).setBoundVariable(field, variable);
      return;
    }
  } catch {
    /* 변수 API 오류 시 fallback으로 진행 */
  }
  /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
  (node as any)[field] = fallback;
}

/**
 * TextNode를 생성·설정하고 fill과 fontSize를 Figma 변수에 바인딩한 뒤 부모에 추가한다.
 * addText + setFillWithVar + setFloatVar(fontSize) 세 단계를 하나로 합친 헬퍼다.
 *
 * @param parent       - 부모 노드
 * @param characters   - 표시할 텍스트
 * @param fontSize     - 폰트 크기 (fallback)
 * @param colorVar     - Figma 색상 변수 경로 (예: 'color/text/heading')
 * @param fallback     - 색상 변수를 찾지 못했을 때 사용할 RGB 값
 * @param bold         - 굵게 여부 (기본 false)
 * @param fontSizeVar  - Figma fontSize 변수 경로 (예: 'text/base/fontSize'). 생략 시 바인딩 건너뜀
 */
export async function addTextWithVar(
  parent: FrameNode | ComponentNode,
  characters: string,
  fontSize: number,
  colorVar: string,
  fallback: RGB,
  bold = false,
  fontSizeVar?: string,
): Promise<TextNode> {
  const text = figma.createText();
  await applyText(text, characters, fontSize, fallback, bold);
  await setFillWithVar(text, colorVar, fallback);
  if (fontSizeVar) {
    await setFloatVar(text, 'fontSize', fontSizeVar, fontSize);
  }
  parent.appendChild(text);
  return text;
}

/**
 * ComponentNode 생성 헬퍼.
 * name은 Figma variant 형식 "Property=Value, ..." 또는 단순 이름.
 */
export function createComponent(name: string): ComponentNode {
  const node = figma.createComponent();
  node.name = name;
  clearFill(node);
  clearStroke(node);
  return node;
}

/**
 * 여러 ComponentNode를 그리드로 배치한 뒤 ComponentSetNode로 묶는다.
 * setName은 React 컴포넌트 이름과 동일하게 설정한다.
 *
 * combineAsVariants 호출 전에 각 variant의 x,y를 직접 지정해야
 * Figma 캔버스에서 겹치지 않고 나열된다.
 *
 * @param cols 한 행에 배치할 variant 수. 기본값 3.
 */
export function combineVariants(
  components: ComponentNode[],
  setName: string,
  cols = 3,
): ComponentSetNode {
  const GAP = 16;

  /* 열별 최대 너비, 행별 최대 높이 계산 — 크기가 다른 variant도 정렬 보장 */
  const colWidths  = new Array<number>(cols).fill(0);
  const rowHeights = new Array<number>(Math.ceil(components.length / cols)).fill(0);

  components.forEach((comp, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);
    if (comp.width  > colWidths[col])   colWidths[col]   = comp.width;
    if (comp.height > rowHeights[row])  rowHeights[row]  = comp.height;
  });

  /* 누적 offset 계산 */
  const colOffsets = colWidths.map((_, i) =>
    colWidths.slice(0, i).reduce((sum, w) => sum + w + GAP, 0),
  );
  const rowOffsets = rowHeights.map((_, i) =>
    rowHeights.slice(0, i).reduce((sum, h) => sum + h + GAP, 0),
  );

  /* appendChild를 x/y 설정보다 먼저 호출해야 한다.
   * Figma는 append 시 좌표를 (0, 0)으로 리셋하므로, append 이후에 좌표를 지정한다. */
  components.forEach((comp, i) => {
    figma.currentPage.appendChild(comp);
    comp.x = colOffsets[i % cols];
    comp.y = rowOffsets[Math.floor(i / cols)];
  });

  const set = figma.combineAsVariants(components, figma.currentPage);
  set.name = setName;
  clearFill(set);
  return set;
}

/** 사각형 레이어 생성 헬퍼 */
export function addRect(
  parent: FrameNode | ComponentNode,
  width: number,
  height: number,
  color: RGB,
  radius = 0,
): RectangleNode {
  const rect = figma.createRectangle();
  rect.resize(width, height);
  setFill(rect, color);
  rect.cornerRadius = radius;
  parent.appendChild(rect);
  return rect;
}

/** 구분선(Divider) 생성 헬퍼 */
export function addDivider(
  parent: FrameNode | ComponentNode,
  color: RGB,
): RectangleNode {
  const line = figma.createRectangle();
  /* layoutAlign STRETCH: Auto Layout 부모에서 가로폭을 꽉 채움 */
  line.layoutAlign = 'STRETCH';
  line.resize(parent.width || 100, 1);
  setFill(line, color);
  parent.appendChild(line);
  return line;
}

/**
 * Figma 색상 변수를 stroke에 바인딩한다.
 * setStroke는 RGB만 받으므로 변수 바인딩이 필요한 경우 이 함수를 사용한다.
 * 변수를 찾지 못하거나 오류 발생 시 fallback RGB를 직접 적용한다.
 *
 * @param node         - stroke를 적용할 노드
 * @param variableName - Figma 변수 전체 경로 (예: 'color/border')
 * @param fallback     - 변수를 찾지 못했을 때 사용할 RGB 값
 * @param weight       - stroke 두께 (기본 1)
 */
export async function setStrokeWithVar(
  node: GeometryMixin & IndividualStrokesMixin,
  variableName: string,
  fallback: RGB,
  weight = 1,
): Promise<void> {
  const basePaint: SolidPaint = { type: 'SOLID', color: fallback };
  try {
    const allVars = await figma.variables.getLocalVariablesAsync('COLOR');
    const variable = allVars.find(v => v.name === variableName);
    if (variable) {
      node.strokes = [figma.variables.setBoundVariableForPaint(basePaint, 'color', variable)];
    } else {
      node.strokes = [basePaint];
    }
  } catch {
    node.strokes = [basePaint];
  }
  node.strokeWeight = weight;
  node.strokeAlign = 'INSIDE';
}

/**
 * TextNode lineHeight를 Figma FLOAT 변수에 바인딩한다.
 * setFloatVar는 lineHeight의 객체 구조({ value, unit })를 처리하지 못하므로
 * 별도 함수로 분리한다. 변수를 찾지 못하면 { value: fallback, unit: 'PIXELS' }로 설정한다.
 *
 * @param node         - 바인딩할 TextNode
 * @param variableName - Figma 변수 전체 경로 (예: 'text/base/lineHeight')
 * @param fallback     - 변수를 찾지 못했을 때 사용할 px 값
 */
export async function setLineHeightVar(
  node: TextNode,
  variableName: string,
  fallback: number,
): Promise<void> {
  try {
    const allVars = await figma.variables.getLocalVariablesAsync('FLOAT');
    const variable = allVars.find(v => v.name === variableName);
    if (variable) {
      node.setBoundVariable('lineHeight', variable);
      return;
    }
  } catch { /* fallback */ }
  node.lineHeight = { value: fallback, unit: 'PIXELS' };
}

/**
 * 아이콘 슬롯을 ComponentNode에 추가한다.
 *
 * Icons/{name} 컴포넌트가 현재 페이지에 존재하면
 * 인스턴스를 생성하고 INSTANCE_SWAP Property로 등록한다.
 * → Figma 우측 패널에서 원하는 아이콘으로 swap 가능
 *
 * 아이콘 컴포넌트가 없으면 rect 플레이스홀더로 대체한다.
 * → createIcons 커맨드를 먼저 실행해야 인스턴스 방식이 활성화된다.
 *
 * @param comp         - 아이콘을 추가할 부모 ComponentNode
 * @param name         - 기본 아이콘 이름 (IconName)
 * @param size         - 아이콘 크기(px)
 * @param fallbackColor - 아이콘 컴포넌트 미존재 시 rect에 사용할 색상
 * @param propertyName - INSTANCE_SWAP Property 이름 (기본 'icon')
 */
export function addIconSlot(
  comp: ComponentNode,
  name: IconName,
  size: number,
  fallbackColor: RGB,
  propertyName = 'icon',
): void {
  const iconComp = _findIconComponent(name);

  if (iconComp) {
    const instance = iconComp.createInstance();
    instance.resize(size, size);
    comp.appendChild(instance);
    /* INSTANCE_SWAP Property 등록 — Figma에서 아이콘 swap 드롭다운 표시 */
    const propKey = comp.addComponentProperty(propertyName, 'INSTANCE_SWAP', iconComp.id);
    instance.componentPropertyReferences = { mainComponent: propKey };
  } else {
    /* createIcons 미실행 시 rect 플레이스홀더로 fallback */
    addRect(comp, size, size, fallbackColor, 2);
  }
}

/**
 * Icons/* 컴포넌트를 이름으로 검색한다.
 * 최초 호출 시 페이지 전체를 스캔하고 이후엔 캐시를 반환한다.
 */
let _iconCache: Map<string, ComponentNode> | null = null;

function _findIconComponent(name: IconName): ComponentNode | null {
  if (_iconCache === null) {
    _iconCache = new Map();
    figma.currentPage
      .findAllWithCriteria({ types: ['COMPONENT'] })
      .filter(c => c.name.startsWith('Icons/'))
      .forEach(c => _iconCache!.set(c.name, c as ComponentNode));
  }
  return _iconCache.get(`Icons/${name}`) ?? null;
}
