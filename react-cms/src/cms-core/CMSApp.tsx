/**
 * @file CMSApp.tsx
 * @description CMS 컨텍스트 프로바이더. 외부 프로젝트가 제공하는 blocks/overlays/layouts를 컨텍스트로 제공합니다.
 *
 * Router는 이 컴포넌트 밖(main.tsx)에서 소유합니다.
 * RouterProvider를 children으로 전달하면 CMS 컨텍스트 안에서 라우팅이 동작합니다.
 */
import { useMemo, useEffect } from "react";
import type { ReactNode } from "react";
import type { BlockDefinition, LayoutTemplate, OverlayTemplate, CMSCodegenConfig } from "./types";
import {
  BlockRegistryContext,
  BlockMetaContext,
  BlockDefinitionsContext,
  LayoutRendererContext,
  LayoutTemplatesContext,
  OverlayTemplatesContext,
  StylesheetContext,
  CodegenConfigContext,
  PortalHostContext,
} from "./context";
import { useCMSContextValues } from "./useCMSContextValues";

// ─── Portal Host (모듈 싱글턴) ────────────────────────────────────────────────
// CMSApp 인스턴스 간/StrictMode 이중 mount 사이에 portal target이 detached되지 않도록
// 모듈 수명 동안 단일 host를 유지한다. cleanup으로 제거하지 않아 dev 더블 mount 사이의
// 짧은 detach 윈도우가 발생하지 않는다 (빈 div 1개라 정리 비용은 무시 가능).

let portalHost: HTMLDivElement | null = null;

function ensurePortalHost(): HTMLDivElement {
  if (!portalHost) {
    portalHost = document.createElement("div");
    portalHost.id = "cms-portal-host";
    portalHost.setAttribute("data-cms-user-scope", "");
  }
  if (!portalHost.isConnected) document.body.appendChild(portalHost);
  return portalHost;
}

export interface CMSAppProps {
  /** 팔레트에 표시할 블록 목록 — 필수 */
  blocks: BlockDefinition[];
  /** 오버레이 템플릿 목록 */
  overlays?: OverlayTemplate[];
  /**
   * 레이아웃 템플릿 목록.
   * 각 템플릿의 renderer 함수로 레이아웃 크롬(header/footer)을 렌더링합니다.
   */
  layouts?: LayoutTemplate[];
  /**
   * 외부 프로젝트 컴파일된 CSS 문자열 — 캔버스/썸네일/미리보기 영역에만 스코프 적용.
   * 개발 환경에서 Vite의 `?inline` import로 얻은 CSS를 전달합니다. `stylesheet`보다 우선합니다.
   * @example
   * import userCSS from "./user-scope.css?inline";
   * <CMSApp stylesheetContent={userCSS} />
   */
  stylesheetContent?: string;
  /**
   * 외부 프로젝트 CSS URL — 런타임 fetch 후 캔버스/썸네일/미리보기 영역에만 로드.
   * 주로 프로덕션에서 배포된 CSS URL을 지정할 때 사용합니다.
   */
  stylesheet?: string;
  /** CSS 변수 활성화용 data 속성 (예: { "data-brand": "kb" }) */
  stylesheetScope?: Record<string, string>;
  /**
   * generateJSX 코드 생성 설정.
   * blockImportFrom으로 블록 컴포넌트 import 소스를 지정합니다.
   * 미정의 시 "@neobnsrnd-team/cms-ui"를 기본값으로 사용합니다.
   */
  codegenConfig?: CMSCodegenConfig;
  /**
   * RouterProvider를 children으로 전달합니다.
   * Router는 main.tsx에서 직접 소유하며, CMSApp은 컨텍스트만 제공합니다.
   */
  children?: ReactNode;
}

export function CMSApp({ blocks, overlays = [], layouts = [], stylesheetContent, stylesheet, stylesheetScope, codegenConfig = {}, children }: CMSAppProps) {
  const { blockMeta, blockRegistry, derivedRenderer } = useCMSContextValues(blocks, layouts);

  const stylesheetConfig = useMemo(
    () => ({ stylesheetContent, stylesheet, stylesheetScope }),
    [stylesheetContent, stylesheet, stylesheetScope],
  );

  // portal host: 첫 렌더부터 context가 non-null이 되도록 모듈 싱글턴을 동기 조회.
  // 모듈 수명 동안 유지되므로 unmount cleanup이 필요 없다 (detach 윈도우 제거).
  const portalHostElement = useMemo(() => ensurePortalHost(), []);

  // stylesheetScope 변경 시 portal host의 data-* attributes 동기화
  useEffect(() => {
    portalHostElement.setAttribute("data-cms-user-scope", "");
    // 이전 scope 속성 제거 후 재적용
    Array.from(portalHostElement.attributes)
      .filter((a) => a.name.startsWith("data-") && a.name !== "data-cms-user-scope")
      .forEach((a) => portalHostElement.removeAttribute(a.name));
    Object.entries(stylesheetScope ?? {}).forEach(([k, v]) => portalHostElement.setAttribute(k, v));
  }, [stylesheetScope, portalHostElement]);

  return (
    <PortalHostContext.Provider value={portalHostElement}>
      <StylesheetContext.Provider value={stylesheetConfig}>
        <BlockDefinitionsContext.Provider value={blocks}>
          <BlockMetaContext.Provider value={blockMeta}>
            <BlockRegistryContext.Provider value={blockRegistry}>
              <OverlayTemplatesContext.Provider value={overlays}>
                <LayoutTemplatesContext.Provider value={layouts}>
                  <LayoutRendererContext.Provider value={derivedRenderer}>
                    <CodegenConfigContext.Provider value={codegenConfig}>
                      {children}
                    </CodegenConfigContext.Provider>
                  </LayoutRendererContext.Provider>
                </LayoutTemplatesContext.Provider>
              </OverlayTemplatesContext.Provider>
            </BlockRegistryContext.Provider>
          </BlockMetaContext.Provider>
        </BlockDefinitionsContext.Provider>
      </StylesheetContext.Provider>
    </PortalHostContext.Provider>
  );
}
