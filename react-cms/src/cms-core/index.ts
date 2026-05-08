/**
 * @file index.ts
 * @description cms-core 공개 API 진입점.
 * 외부 프로젝트는 이 파일에서 CMS 타입, 컴포넌트, 훅을 import 합니다.
 *
 * 주요 카테고리:
 * - 블록/페이지 데이터 모델 타입: CMSBlock, CMSPage, CMSOverlay 등
 * - 블록 메타데이터 타입: BlockDefinition, PropField, BlockMeta 등
 * - 레이아웃/오버레이 주입 타입: LayoutTemplate, OverlayTemplate 등
 * - CMS Runtime: CMSRuntimeProvider, PageRenderer
 * - CMS Builder: CMSBuilder
 * - CMS App: CMSApp (빌더 UI 전체 포함)
 * - 스타일 격리: UserScopeWrapper
 */
// ── 블록/페이지 데이터 모델 타입 ──────────────────────────────────────────────
export type {
  CMSBlock,
  CMSPage,
  CMSOverlay,
  CMSOverlayType,
  Action,
  BlockInteraction,
  LayoutType,
  BlockPadding,
} from "./types";
export { DEFAULT_PADDING } from "./types";

// ── 블록 메타데이터 타입 (외부 프로젝트 구현용) ───────────────────────────────
export type {
  PropField,
  LeafPropField,
  GroupPropField,
  ArrayPropField,
  EventPropField,
  BlockCategory,
  BlockDomain,
  BlockMeta,
  BlockDefinition,
} from "./types";

// ── 레이아웃 / 오버레이 주입 타입 ─────────────────────────────────────────────
export type {
  LayoutSlots,
  LayoutRenderer,
  LayoutTemplate,
  CMSCodegenConfig,
  OverlayRendererProps,
  OverlayTemplate,
} from "./types";

// ── CMS Runtime (외부 프로젝트 앱에서 CMS 페이지 렌더링용) ────────────────────────
export { CMSRuntimeProvider } from "./CMSRuntimeProvider";
export type { CMSRuntimeProviderProps } from "./CMSRuntimeProvider";
export { default as PageRenderer } from "./runtime/renderPage";

// ── CMS Builder ────────────────────────────────────────────────────────────────
export { CMSBuilder } from "./CMSBuilder";
export type { CMSBuilderProps } from "./CMSBuilder";
export type { SavePageParams } from "./SavePageModal";
export { generateJSX } from "./codegen/exportCode";

// ── CMS App ────────────────────────────────────────────────────────────────────
export { CMSApp } from "./CMSApp";
export type { CMSAppProps } from "./CMSApp";

// ── 스타일 격리 ────────────────────────────────────────────────────────────────
export { UserScopeWrapper } from "./UserScopeWrapper";
export type { StylesheetConfig } from "./context";

// ── 아이콘 유틸리티 ────────────────────────────────────────────────────────────
export { resolveIcon, toKebabIcon, kebabToPascal, ALL_ICON_NAMES } from "./utils/icon";

// ── 저장 경로 검증 ─────────────────────────────────────────────────────────────
// 클라이언트(SavePageModal)와 서버(cmsBankPlugin) 양쪽에서 공용으로 사용
export { validateRelativeSavePath } from "./utils/savePath";
