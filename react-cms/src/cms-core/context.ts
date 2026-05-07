/**
 * @file context.ts
 * @description cms-core 전역 컨텍스트.
 * CMSApp이 제공하고, LayoutCanvas / renderPage / OverlayCanvas 등이 소비합니다.
 */
import { createContext } from "react";
import type { BlockMeta, BlockDefinition, LayoutRenderer, LayoutTemplate, OverlayTemplate, CMSCodegenConfig } from "./types";

/** 외부 프로젝트 스타일 격리 설정 */
export interface StylesheetConfig {
  /**
   * 외부 프로젝트 CSS URL — 런타임에 fetch 후 캔버스/썸네일/미리보기 영역에만 로드.
   * 주로 프로덕션 환경에서 배포된 CSS URL을 지정할 때 사용.
   */
  stylesheet?: string;
  /**
   * 외부 프로젝트 CSS 문자열 — fetch 없이 직접 주입.
   * 개발 환경에서 Vite의 `?inline` import로 컴파일된 CSS를 전달할 때 사용.
   * `stylesheet`보다 우선순위가 높음.
   * @example
   * // vite.config.ts에 @source 추가 후:
   * import userCSS from "./user-scope.css?inline";
   * <CMSApp stylesheetContent={userCSS} />
   */
  stylesheetContent?: string;
  /** CSS 변수 활성화용 data 속성 (예: { "data-brand": "kb" }) */
  stylesheetScope?: Record<string, string>;
}

export const StylesheetContext = createContext<StylesheetConfig>({});

/** 블록 타입명 → 렌더러 컴포넌트 맵 */
export const BlockRegistryContext = createContext<
  Record<string, React.ComponentType<Record<string, unknown>>>
>({});

/** 블록 타입명 → BlockMeta 맵 */
export const BlockMetaContext = createContext<Record<string, BlockMeta>>({});

/** 외부 프로젝트가 제공한 BlockDefinition 원본 목록 */
export const BlockDefinitionsContext = createContext<BlockDefinition[]>([]);

/** 레이아웃 크롬(header/footer) 렌더러 */
export const LayoutRendererContext = createContext<LayoutRenderer | undefined>(undefined);

/** 오버레이 템플릿 목록 */
export const OverlayTemplatesContext = createContext<OverlayTemplate[]>([]);

/** 레이아웃 템플릿 목록 */
export const LayoutTemplatesContext = createContext<LayoutTemplate[]>([]);

/** generateJSX 코드 생성 설정 */
export const CodegenConfigContext = createContext<CMSCodegenConfig>({});

/**
 * CMSApp이 관리하는 portal 전용 호스트 엘리먼트.
 * document.body 직하에 위치해 transform 조상이 없으므로 position:fixed가 뷰포트 기준으로 작동.
 * stylesheetScope data-* 속성이 자동으로 동기화되어 브랜드·도메인 토큰이 portal에도 적용됨.
 */
export const PortalHostContext = createContext<HTMLElement | null>(null);
