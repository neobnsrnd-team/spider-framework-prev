/**
 * @file types.ts
 * @description CMS 페이지 데이터 모델 타입 및 블록 메타데이터 타입 정의.
 * 외부 프로젝트가 CMSApp/CMSRuntimeProvider에 블록·레이아웃·오버레이를 제공할 때 사용합니다.
 *
 * 주요 타입:
 * - PropField: CMS 인스펙터에서 편집 가능한 prop 필드 스키마
 * - BlockDefinition: 팔레트에 등록할 블록 메타데이터 + 컴포넌트
 * - LayoutTemplate: 레이아웃 크롬(header/footer) 템플릿
 * - OverlayTemplate: 바텀시트·모달 등 오버레이 템플릿
 * - CMSPage: 저장·불러오기 포맷 (blocks + overlays + layoutType)
 */
import type React from "react";

// ─── PropField 타입 ────────────────────────────────────────────

/** 단일 값 필드 (string / boolean / number / select / icon-picker) */
export type LeafPropField = {
  type: "string" | "boolean" | "number" | "select" | "icon-picker";
  label?: string;
  options?: string[];
  default: string | boolean | number;
};

/** 서브 props 그룹 (1단계 중첩만 허용) */
export type GroupPropField = {
  type: "group";
  label?: string;
  default: Record<string, string | boolean | number>;
  fields: Record<string, LeafPropField>;
};

/**
 * 배열 아이템 내 중첩 배열 필드 (2단계 중첩까지만 허용).
 * ArrayPropField.itemFields 에서만 사용되며, 최상위 propSchema에는 사용하지 않는다.
 */
export type NestedArrayPropField = {
  type: "array";
  label?: string;
  default: Record<string, string | boolean | number>[];
  itemFields: Record<string, LeafPropField>;
};

/** 동적 배열 필드 (항목 추가/삭제 가능) */
export type ArrayPropField = {
  type: "array";
  label?: string;
  default: Record<string, string | boolean | number>[] | string[] | boolean[] | number[];
  /** LeafPropField 외에 NestedArrayPropField도 허용 (2단계 중첩) */
  itemFields: Record<string, LeafPropField | NestedArrayPropField>;
};

/** 이벤트 핸들러 prop (인터랙션 탭에서 액션 바인딩에 사용) */
export type EventPropField = {
  type: "event";
  label?: string;
};

export type PropField = LeafPropField | GroupPropField | ArrayPropField | EventPropField;

// ─── 블록 ──────────────────────────────────────────────────────

/** 팔레트 카테고리. 외부 프로젝트에서 자유롭게 정의 가능. */
export type BlockCategory = string;

/** 팔레트 도메인 그룹. 외부 프로젝트에서 자유롭게 정의 가능. */
export type BlockDomain = string;

export interface BlockMeta {
  name: string;
  category: BlockCategory;
  /** composite / page 카테고리에서 도메인별 그룹핑에 사용 */
  domain?: BlockDomain;
  defaultProps: Record<string, unknown>;
  propSchema: Record<string, PropField>;
}

/**
 * BlockDefinition: BlockMeta(메타데이터)와 React 컴포넌트를 통합한 타입.
 * 외부 프로젝트가 CMSApp에 블록 목록을 제공할 때 사용합니다.
 */
export interface BlockDefinition {
  meta: BlockMeta;
  component: React.ComponentType<Record<string, unknown>>;
  /**
   * codegen에서 실제 컴포넌트 API 형태로 변환.
   * component 함수에 props 변환 로직이 있으면 반드시 함께 정의해야 합니다.
   * { __jsx: "..." } 마커를 반환하면 해당 값이 {} 안에 raw JSX expression으로 출력됩니다.
   * 미정의 시 propSchema의 icon-picker 필드는 자동으로 JSX로 변환됩니다.
   */
  codegenProps?: (props: Record<string, unknown>) => Record<string, unknown>;
  /** codegenProps가 생성하는 JSX 안에서 참조하는 추가 컴포넌트명. generateJSX가 import에 포함. */
  codegenImports?: string[];
}

export type BlockPadding = {
  top: number;
  right: number;
  bottom: number;
  left: number;
};

/** 블록 클릭 시 실행할 액션 */
export type Action =
  | { type: "openOverlay"; target: string }
  | { type: "closeOverlay" }
  | { type: "navigate"; path: string };

/**
 * 블록 이벤트 바인딩 맵.
 * key는 컴포넌트의 실제 함수 prop 이름 (예: "onClick", "onAction", "onSearch").
 * 값은 실행할 Action.
 * @example { onClick: { type: "openOverlay", target: "sheet" }, onAction: { type: "navigate", path: "/detail" } }
 */
export type BlockInteraction = Record<string, Action>;

export type CMSBlock = {
  id: string;
  /** blockRegistry / componentCatalog 의 키값 (예: "Badge", "Button") */
  component: string;
  props: Record<string, unknown>;
  padding: BlockPadding;
  /** 클릭 등 이벤트에 연결할 액션 */
  interaction?: BlockInteraction;
};

// ─── 오버레이 ──────────────────────────────────────────────────

/** 오버레이 타입 식별자 — 외부 프로젝트가 자유롭게 정의 */
export type CMSOverlayType = string;

/** CMS 오버레이 — type은 OverlayTemplate.type과 매칭 */
export type CMSOverlay = {
  id: string;
  type: CMSOverlayType;
  blocks: CMSBlock[];
  props?: Record<string, unknown>;
};

/** OverlayCanvas / OverlayShell에서 받는 렌더러 props */
export interface OverlayRendererProps {
  open: boolean;
  onClose: () => void;
  children?: React.ReactNode;
  container: HTMLElement | null;
  props?: Record<string, unknown>;
}

/**
 * 오버레이 템플릿.
 * 외부 프로젝트가 CMSApp에 제공하며, renderer 컴포넌트로 캔버스/런타임 렌더링을 담당합니다.
 */
export interface OverlayTemplate {
  /** 템플릿 식별자 */
  id: string;
  /** 팔레트에 표시할 이름 */
  label: string;
  /** 부가 설명 */
  description?: string;
  /** 오버레이 타입 식별자 (CMSOverlay.type과 매칭) */
  type: string;
  /** 생성 시 기본 overlay id */
  defaultId: string;
  /** BottomSheet 내부 초기 블록 */
  blocks: CMSBlock[];
  /** 컴포넌트 기반 오버레이 기본 props */
  props?: Record<string, unknown>;
  /**
   * 오버레이 props 편집 스키마.
   * 정의 시 RightSidebar 오버레이 탭에서 동적 폼을 생성합니다.
   * 미정의 시 props 값의 타입에서 자동 추론합니다 (string/boolean/number만 지원).
   */
  propSchema?: Record<string, PropField>;
  /**
   * JSX 코드 생성 시 사용할 컴포넌트 이름 (예: "BottomSheet", "AlertModal").
   * 미정의 시 overlay.type을 그대로 사용합니다.
   */
  componentName?: string;
  /** 오버레이 래퍼 렌더러 컴포넌트 */
  renderer?: React.ComponentType<OverlayRendererProps>;
}

// ─── 레이아웃 ──────────────────────────────────────────────────

/**
 * 레이아웃 타입 식별자. 외부 프로젝트에서 자유롭게 정의 가능.
 * CMSPage.layoutType 및 LayoutTemplate.id와 매칭됩니다.
 */
export type LayoutType = string;

/** LayoutRenderer가 반환하는 레이아웃 슬롯 */
export interface LayoutSlots {
  header?: React.ReactNode;
  footer?: React.ReactNode;
}

/**
 * 레이아웃 크롬(header/footer)을 렌더링하는 함수.
 * LayoutTemplate.renderer에서 파생됩니다.
 */
export type LayoutRenderer = (
  layoutType: string,
  layoutProps: Record<string, unknown>,
) => LayoutSlots;

/**
 * 레이아웃 템플릿.
 * 외부 프로젝트가 CMSApp에 제공하며, CMS 빌더의 레이아웃 탭에서 선택·편집됩니다.
 * OverlayTemplate과 동일한 패턴으로 propSchema 기반 동적 폼을 지원합니다.
 */
export interface LayoutTemplate {
  /** 레이아웃 타입 식별자 (CMSPage.layoutType과 매칭) */
  id: string;
  /** 레이아웃 탭에 표시할 이름 */
  label: string;
  /** 부가 설명 */
  description?: string;
  /** 생성 시 기본 props */
  defaultProps?: Record<string, unknown>;
  /**
   * 레이아웃 props 편집 스키마.
   * 정의 시 RightSidebar 레이아웃 탭에서 동적 폼을 생성합니다.
   * 미정의 시 defaultProps 값의 타입에서 자동 추론합니다.
   */
  propSchema?: Record<string, PropField>;
  /**
   * 레이아웃 크롬(header/footer) 렌더러.
   * layoutProps를 받아 header/footer ReactNode를 반환합니다.
   * 미정의 시 header/footer 없이 콘텐츠만 렌더링됩니다.
   */
  renderer?: (layoutProps: Record<string, unknown>) => LayoutSlots;
  /**
   * JSX 코드 생성 시 사용할 레이아웃 래퍼 컴포넌트 이름 (예: "PageLayout").
   * 미정의 시 레이아웃 래퍼 없이 블록만 생성합니다.
   */
  componentName?: string;
}

// ─── 페이지 ────────────────────────────────────────────────────

export type CMSPage = {
  layoutType?: LayoutType;
  layoutProps?: Record<string, unknown>;
  blocks: CMSBlock[];
  /** 페이지에 속한 오버레이 목록 */
  overlays?: CMSOverlay[];
};

// ─── 코드 생성 설정 ────────────────────────────────────────────

/**
 * generateJSX 전역 코드 생성 설정.
 * CMSApp의 codegenConfig prop으로 전달합니다.
 */
export interface CMSCodegenConfig {
  /** 블록 컴포넌트 import 소스 (기본값: "@neobnsrnd-team/cms-ui") */
  blockImportFrom?: string;
  /**
   * 레이아웃 컴포넌트 import 소스.
   * 미정의 시 blockImportFrom과 동일한 패키지로 간주하여 하나의 import로 합칩니다.
   */
  layoutImportFrom?: string;
}

// ─── 기본값 ───────────────────────────────────────────────────

export const DEFAULT_PADDING: BlockPadding = { top: 0, right: 0, bottom: 0, left: 0 };
