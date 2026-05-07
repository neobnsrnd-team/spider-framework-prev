/**
 * @file exportCode.ts
 * @description CMSPage → React JSX 코드 문자열 변환기.
 * 저장 포맷은 항상 JSON이며, 이 모듈은 부가 기능입니다.
 * overlays가 있는 경우 useState + open/close 핸들러 + OverlayShell을 함께 생성합니다.
 * blockDefinitions가 있으면 codegenProps / 스키마 기반 icon-picker 자동 변환을 적용합니다.
 */
import type { Action, CMSPage, CMSOverlay, LayoutTemplate, OverlayTemplate, CMSCodegenConfig, BlockDefinition, ArrayPropField, PropField } from "../types";

// cms-ui LayoutRenderer 기반 코드 생성에서만 사용하는 로컬 타입 (cms-core에서 제거됨)
type LayoutProps = Record<string, unknown>;

// ─── JSX expression 마커 ──────────────────────────────────────────────────────

/**
 * codegenProps에서 반환하는 raw JSX expression 마커.
 * { __jsx: "<ChevronRight className=\"size-5\" />" } 형태로 사용한다.
 */
type JSXExpr = { __jsx: string };

function isJSXExpr(v: unknown): v is JSXExpr {
  return typeof v === "object" && v !== null && "__jsx" in v && typeof (v as JSXExpr).__jsx === "string";
}

// ─── 재귀 값 직렬화 ────────────────────────────────────────────────────────────

/**
 * 값을 JSX expression 안에 쓸 수 있는 JS 리터럴 문자열로 변환한다.
 * JSXExpr 마커를 만나면 __jsx 문자열을 raw expression으로 인라인한다.
 */
function serializeValue(v: unknown): string {
  if (v === null || v === undefined) return "undefined";
  if (isJSXExpr(v)) return v.__jsx;
  if (typeof v === "string") return JSON.stringify(v);
  if (typeof v === "number") return String(v);
  if (typeof v === "boolean") return String(v);
  if (Array.isArray(v)) {
    if (v.length === 0) return "[]";
    return `[${v.map(serializeValue).join(", ")}]`;
  }
  if (typeof v === "object") {
    const entries = Object.entries(v as Record<string, unknown>)
      .filter(([, val]) => val !== undefined && val !== null)
      .map(([k, val]) => `${k}: ${serializeValue(val)}`);
    return entries.length > 0 ? `{ ${entries.join(", ")} }` : "{}";
  }
  return String(v);
}

// ─── 패딩 변환 ────────────────────────────────────────────────────────────────

function paddingToClassName(pd: { top: number; right: number; bottom: number; left: number }): string {
  const { top, right, bottom, left } = pd;
  if (!top && !right && !bottom && !left) return "";
  if (top === right && right === bottom && bottom === left) return `p-[${top}px]`;

  const parts: string[] = [];
  if (top === bottom) { if (top) parts.push(`py-[${top}px]`); }
  else { if (top) parts.push(`pt-[${top}px]`); if (bottom) parts.push(`pb-[${bottom}px]`); }
  if (left === right) { if (left) parts.push(`px-[${left}px]`); }
  else { if (right) parts.push(`pr-[${right}px]`); if (left) parts.push(`pl-[${left}px]`); }
  return parts.join(" ");
}

// ─── props 직렬화 ─────────────────────────────────────────────────────────────

/** props 없이 생성할 수 없는 컴포넌트의 필수 콜백 기본값 */
const REQUIRED_CALLBACKS: Record<string, string> = {
  TransactionFilter:            'onSearch={() => {}}',
  PinInput:                     'onComplete={() => {}}',
  LoginContainer:               'onSubmit={() => {}}',
  AlertModal:                   'open={false} onClose={() => {}}',
  ConfirmModal:                 'open={false} onClose={() => {}} onConfirm={() => {}}',
  BottomSheet:                  'open={false} onClose={() => {}}',
  TransactionFilterBottomSheet: 'open={false} onClose={() => {}}',
  AccountSelectBottomSheet:     'open={false} onClose={() => {}}',
};

/** JSX 어트리뷰트 문법: key="value" key={expr} */
function propsToStr(props: Record<string, unknown>): string {
  return Object.entries(props)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([k, v]) => {
      if (isJSXExpr(v)) return `${k}={${v.__jsx}}`;
      if (typeof v === "string") return `${k}="${v}"`;
      if (typeof v === "boolean") return v ? k : `${k}={false}`;
      return `${k}={${serializeValue(v)}}`;
    })
    .join(" ");
}

// ─── icon-picker 자동 변환 ─────────────────────────────────────────────────────

function kebabToPascal(name: string): string {
  return name.split("-").map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join("");
}

/**
 * propSchema를 기반으로 icon-picker 필드를 JSXExpr로 자동 변환한다.
 * codegenProps가 정의된 블록에서는 사용하지 않는다.
 * 빈 문자열은 건너뛰어 prop 자체를 생략한다 (컴포넌트 자체 기본 아이콘이 사용됨).
 */
function autoConvertIconPickers(
  props: Record<string, unknown>,
  schema: Record<string, PropField>,
): Record<string, unknown> {
  const result = { ...props };
  for (const [key, field] of Object.entries(schema)) {
    if (field.type === "icon-picker" && typeof result[key] === "string" && result[key]) {
      result[key] = { __jsx: `<${kebabToPascal(result[key] as string)} className="size-5" />` };
    }
    if (field.type === "array" && Array.isArray(result[key])) {
      const arrayField = field as ArrayPropField;
      result[key] = (result[key] as Record<string, unknown>[]).map((item) => {
        const newItem = { ...item };
        for (const [subKey, subField] of Object.entries(arrayField.itemFields)) {
          if (
            subField.type === "icon-picker" &&
            typeof newItem[subKey] === "string" &&
            newItem[subKey]
          ) {
            newItem[subKey] = { __jsx: `<${kebabToPascal(newItem[subKey] as string)} className="size-5" />` };
          }
        }
        return newItem;
      });
    }
  }
  return result;
}

// ─── Lucide 아이콘 import 수집 ────────────────────────────────────────────────

/**
 * JSXExpr 마커를 재귀적으로 탐색해 Lucide 아이콘 이름을 수집한다.
 * "<IconName ..." 패턴에서 PascalCase 이름만 추출한다.
 */
function collectLucideIcons(v: unknown, result: Set<string>): void {
  if (isJSXExpr(v)) {
    // 문자열 전체에서 PascalCase JSX 요소를 모두 수집 (중첩된 icon={<X />} 포함)
    for (const m of v.__jsx.matchAll(/<([A-Z][a-zA-Z0-9]*)/g)) result.add(m[1]);
    return;
  }
  if (Array.isArray(v)) { v.forEach((i) => collectLucideIcons(i, result)); return; }
  if (typeof v === "object" && v !== null) {
    Object.values(v as Record<string, unknown>).forEach((i) => collectLucideIcons(i, result));
  }
}

function applyCodegenTransform(
  props: Record<string, unknown>,
  def: BlockDefinition | undefined,
): Record<string, unknown> {
  if (def?.codegenProps) return def.codegenProps(props);
  if (def?.meta.propSchema) return autoConvertIconPickers(props, def.meta.propSchema);
  return props;
}

// ─── 블록 JSX 라인 생성 ───────────────────────────────────────────────────────

/**
 * @description interaction 맵의 Action을 인라인 핸들러 문자열로 변환합니다.
 */
function actionToHandler(action: Action, overlayId?: string): string {
  if (action.type === "openOverlay") {
    if (!action.target) return `() => {}`;
    return `() => set${capitalize(action.target)}Open(true)`;
  }
  if (action.type === "closeOverlay") {
    if (overlayId) return `() => set${capitalize(overlayId)}Open(false)`;
    return `() => {}`;
  }
  if (action.type === "navigate") return `() => navigate(${JSON.stringify(action.path)})`;
  return "() => {}";
}

function blockToJSXLine(
  block: CMSPage["blocks"][number],
  indent: string,
  blockDefs: BlockDefinition[],
  overlayId?: string,
): string {
  const def = blockDefs.find((d) => d.meta.name === block.component);
  const transformedProps = applyCodegenTransform(block.props ?? {}, def);

  const { children, ...rest } = transformedProps;

  // interaction → 이벤트 props 자동 매핑
  const interactionEntries = Object.entries(block.interaction ?? {});
  const interactionKeys = new Set(interactionEntries.map(([k]) => k));
  const interactionPropsStr = interactionEntries
    .map(([key, action]) => `${key}={${actionToHandler(action, overlayId)}}`)
    .join(" ");

  // propSchema의 event 필드 중 interaction에 포함되지 않은 것에 noop 자동 주입.
  // 이벤트 props는 JSON에 저장되지 않으므로 명시하지 않으면 required 콜백 타입 에러가 발생한다.
  // TypeScript는 () => void를 (value: T) => void에 할당 가능하므로 시그니처 무관하게 안전하다.
  const eventNoopStr = def?.meta.propSchema
    ? Object.entries(def.meta.propSchema)
        .filter(([key, field]) => field.type === "event" && !interactionKeys.has(key))
        .map(([key]) => `${key}={() => {}}`)
        .join(" ")
    : "";

  const propsStr = [propsToStr(rest), REQUIRED_CALLBACKS[block.component] ?? "", interactionPropsStr, eventNoopStr]
    .filter(Boolean).join(" ");
  const openTag = `<${block.component}${propsStr ? " " + propsStr : ""}`;
  const pdCls = block.padding ? paddingToClassName(block.padding) : "";

  // children이 JSXExpr이면 __jsx 문자열을 직접 삽입, 아닌 경우 toString
  const childrenStr = children !== undefined
    ? (isJSXExpr(children) ? children.__jsx : String(children))
    : undefined;
  let jsx: string;
  if (childrenStr !== undefined) jsx = `${openTag}>${childrenStr}</${block.component}>`;
  else jsx = `${openTag} />`;

  if (pdCls) return `${indent}<div className="${pdCls}">${jsx}</div>`;
  // 패딩 없는 경우 불필요한 div 래핑 없이 컴포넌트를 직접 렌더링
  return `${indent}${jsx}`;
}

// ─── Overlay 코드 생성 ────────────────────────────────────────────────────────

/**
 * @description 오버레이 하나를 실제 컴포넌트 JSX로 생성합니다.
 */
function overlayToJSX(
  overlay: CMSOverlay,
  template?: OverlayTemplate,
  blockDefs: BlockDefinition[] = [],
): string[] {
  const varName = `${overlay.id}Open`;
  const closer = `() => set${capitalize(overlay.id)}Open(false)`;
  const componentName = template?.componentName ?? overlay.type;
  const p = overlay.props ?? {};

  // 블록이 있는 오버레이 — 자식 블록을 감싸는 래퍼 형태
  if (overlay.blocks.length > 0) {
    const blockLines = overlay.blocks.map((b) => blockToJSXLine(b, "        ", blockDefs, overlay.id));
    return [
      `      <${componentName} open={${varName}} onClose={${closer}}>`,
      ...blockLines,
      `      </${componentName}>`,
    ];
  }

  // props만 있는 오버레이 — overlay.props를 JSX 속성으로 직렬화
  const propEntries = Object.entries(p);
  if (propEntries.length === 0) {
    return [`      <${componentName} open={${varName}} onClose={${closer}} />`];
  }

  const propLines = propEntries.map(([k, v]) => {
    if (typeof v === "string") return `        ${k}="${v}"`;
    if (typeof v === "boolean") return v ? `        ${k}` : `        ${k}={false}`;
    return `        ${k}={${JSON.stringify(v)}}`;
  });

  return [
    `      <${componentName}`,
    `        open={${varName}}`,
    `        onClose={${closer}}`,
    ...propLines,
    `      />`,
  ];
}

function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

// ─── generateJSX ─────────────────────────────────────────────────────────────

/**
 * @description CMSPage를 React JSX 컴포넌트 코드 문자열로 변환합니다.
 * @param page CMSPage 데이터
 * @param layouts 레이아웃 템플릿 목록 — componentName이 있으면 해당 컴포넌트로 래퍼 코드 생성
 * @param codegenConfig 전역 코드 생성 설정 (blockImportFrom, layoutImportFrom 등)
 * @param overlayTemplates 오버레이 템플릿 목록 — componentName으로 import 이름 결정
 * @param blockDefinitions 블록 정의 목록 — codegenProps/propSchema 기반 props 변환에 사용
 * @returns JSX 코드 문자열
 */
export function generateJSX(
  page: CMSPage,
  layouts?: LayoutTemplate[],
  codegenConfig?: CMSCodegenConfig,
  overlayTemplates?: OverlayTemplate[],
  blockDefinitions?: BlockDefinition[],
): string {
  const { layoutType, layoutProps, blocks, overlays = [] } = page;
  const defs = blockDefinitions ?? [];

  // 사용된 컴포넌트 수집
  const allBlocks = [
    ...blocks,
    ...overlays.flatMap((o) => o.blocks),
  ];
  const usedTypes = [...new Set(allBlocks.map((b) => b.component))];

  // 사용된 오버레이 컴포넌트 이름 수집 (componentName 우선, 없으면 type)
  const overlayComponentTypes = [
    ...new Set(overlays.map((o) => {
      const tpl = overlayTemplates?.find((t) => t.type === o.type);
      return tpl?.componentName ?? o.type;
    })),
  ];

  const lp: LayoutProps = layoutProps ?? {};
  const blockGap = (lp.blockGap as string | undefined) ?? "none";
  const blockImportFrom = codegenConfig?.blockImportFrom ?? "@neobnsrnd-team/cms-ui";

  // Lucide 아이콘 import 수집 — codegenProps/auto-convert 후 JSXExpr에서 추출
  const lucideIconSet = new Set<string>();
  for (const block of allBlocks) {
    const def = defs.find((d) => d.meta.name === block.component);
    const transformed = applyCodegenTransform(block.props ?? {}, def);
    Object.values(transformed).forEach((v) => collectLucideIcons(v, lucideIconSet));
  }

  // 블록 JSX 라인 (페이지 레벨 블록)
  const blockLines = blocks.map((b) => blockToJSXLine(b, "        ", defs));

  // ── 레이아웃 래퍼 코드 생성 ────────────────────────────────────
  let layoutOpen = "";
  let layoutClose = "";
  let importLine: string;

  // codegenImports에서 추가 컴포넌트 이름 수집 (사용된 블록만)
  const usedDefs = defs.filter((d) => usedTypes.includes(d.meta.name));
  const extraImports = usedDefs.flatMap((d) => d.codegenImports ?? []);
  const blockNames = [...new Set(["Stack", ...usedTypes, ...overlayComponentTypes, ...extraImports])];
  const currentTemplate = layouts?.find((t) => t.id === layoutType);
  const layoutComponentName = currentTemplate?.componentName;
  const layoutImportFrom = codegenConfig?.layoutImportFrom ?? blockImportFrom;

  // 레이아웃 icon-picker props에서도 lucide 아이콘 수집 (blocks와 동일 방식)
  if (currentTemplate?.propSchema) {
    const { blockGap: _bg, ...lpRaw } = lp;
    Object.values(autoConvertIconPickers(lpRaw, currentTemplate.propSchema))
      .forEach((v) => collectLucideIcons(v, lucideIconSet));
  }

  if (layoutComponentName) {
    // ── componentName 방식: LayoutTemplate.componentName 기반 동적 생성 ──
    // blockGap은 CMS 내부 prop이므로 JSX props에서 제외
    const { blockGap: _bg, ...jsxProps } = lp;
    // 레이아웃 propSchema의 icon-picker 필드를 JSXExpr로 변환 (blocks와 동일 방식)
    const transformedJsxProps = currentTemplate?.propSchema
      ? autoConvertIconPickers(jsxProps, currentTemplate.propSchema)
      : jsxProps;
    const propsStr = propsToStr(transformedJsxProps);
    layoutOpen = `    <${layoutComponentName}${propsStr ? " " + propsStr : ""}>`;
    layoutClose = `    </${layoutComponentName}>`;

    if (layoutImportFrom === blockImportFrom) {
      // 같은 패키지: 하나의 import로 합침
      const allNames = [...new Set([layoutComponentName, ...blockNames])].filter((n): n is string => !!n);
      importLine = `import { ${allNames.join(", ")} } from "${blockImportFrom}";`;
    } else {
      // 다른 패키지: 레이아웃 import 별도
      const layoutImportStr = `import { ${layoutComponentName} } from "${layoutImportFrom}";`;
      const blockImportStr = `import { ${blockNames.join(", ")} } from "${blockImportFrom}";`;
      importLine = [layoutImportStr, blockImportStr].join("\n");
    }
  } else {
    // componentName 미설정 — 레이아웃 래퍼 없이 블록만 렌더링
    importLine = `import { ${blockNames.join(", ")} } from "${blockImportFrom}";`;
  }

  // overlay useState 선언
  const interactionTargets = allBlocks
    .flatMap((b) => Object.values(b.interaction ?? {}))
    .filter((a): a is Extract<Action, { type: "openOverlay" }> => a.type === "openOverlay" && !!a.target)
    .map((a) => a.target);
  const overlayStateIds = [
    ...new Set([...overlays.map((o) => o.id), ...interactionTargets]),
  ];
  const overlayStateLines = overlayStateIds.map(
    (id) => `  const [${id}Open, set${capitalize(id)}Open] = useState(false);`,
  );

  // overlay JSX 블록
  const overlayJSXLines = overlays.flatMap((o) => {
    const tpl = overlayTemplates?.find((t) => t.type === o.type);
    return overlayToJSX(o, tpl, defs);
  });

  // 전체 코드 조합
  const hasLayout = !!layoutComponentName;
  const hasOverlays = overlays.length > 0 || interactionTargets.length > 0;

  const contentLines = blockLines.length ? blockLines : ["      {/* 블록을 추가하세요 */}"];
  const stackOpen = `      <Stack${blockGap !== "none" ? ` gap="${blockGap}"` : ""}>`;
  const stackClose = `      </Stack>`;

  let body: string[];
  if (hasLayout) {
    body = ["  return (", layoutOpen, stackOpen, ...contentLines, stackClose, layoutClose, "  );"];
  } else {
    body = ["  return (", stackOpen, ...contentLines, stackClose, "  );"];
  }

  // overlay가 있으면 return 앞에 overlay JSX를 <>로 감쌈
  if (hasOverlays) {
    const returnIdx = body.findIndex((l) => l.trim() === "return (");
    body.splice(returnIdx, 1, "  return (", "    <>");
    const closingIdx = body.lastIndexOf("  );");
    body.splice(closingIdx, 1, ...overlayJSXLines, "    </>", "  );");
  }

  // navigate action 사용 여부 확인
  const allActions = [
    ...blocks.flatMap((b) => Object.values(b.interaction ?? {})),
    ...overlays.flatMap((o) => o.blocks.flatMap((b) => Object.values(b.interaction ?? {}))),
  ];
  const hasNavigate = allActions.some((a) => a.type === "navigate");

  const reactImport = hasOverlays ? 'import { useState } from "react";' : "";
  const routerImport = hasNavigate ? 'import { useNavigate } from "react-router-dom";' : "";
  // blockNames(컴포넌트 라이브러리)와 겹치는 이름은 lucide import에서 제외
  const lucideNames = [...lucideIconSet].filter((n) => !blockNames.includes(n)).sort();
  const lucideImport = lucideNames.length > 0
    ? `import { ${lucideNames.join(", ")} } from "lucide-react";`
    : "";

  // navigate 훅 선언
  const navigateHook = hasNavigate ? ["  const navigate = useNavigate();", ""] : [];

  // import 섹션: react → router → lucide → 컴포넌트 (상단 import 있으면 빈 줄 구분)
  const hasTopImports = !!(reactImport || routerImport || lucideImport);

  return [
    ...(layoutComponentName ? [`// 레이아웃: ${layoutType}`, ""] : []),
    ...(reactImport ? [reactImport] : []),
    ...(routerImport ? [routerImport] : []),
    ...(lucideImport ? [lucideImport] : []),
    ...(hasTopImports ? [""] : []),
    importLine,
    "",
    "export default function NewPage() {",
    ...navigateHook,
    ...overlayStateLines,
    ...(overlayStateLines.length ? [""] : []),
    ...body,
    "}",
  ].join("\n");
}
