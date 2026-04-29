/**
 * @file renderPage.tsx
 * @description CMSPage 데이터를 런타임에 렌더링하는 컴포넌트.
 * BlockRegistryContext / LayoutRendererContext / OverlayTemplatesContext를 소비합니다.
 */
import { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { Action, CMSOverlay, CMSPage } from "../types";
import { BlockRegistryContext, LayoutRendererContext, OverlayTemplatesContext } from "../context";
import { OverlayProvider, useOverlayStore } from "../state/overlayStore";

// ─── Action Handler ──────────────────────────────────────────────────────────

/**
 * @description 블록 인터랙션 Action을 실제 동작으로 연결하는 훅.
 * OverlayStore(open/close)와 react-router-dom navigate를 결합해
 * openOverlay / closeOverlay / navigate 세 가지 액션을 처리합니다.
 * @returns handleAction(action?: Action) — 이벤트 핸들러에서 직접 호출하는 함수
 */
function useActionHandler() {
  const { open, close } = useOverlayStore();
  const navigate = useNavigate();

  return function handleAction(action?: Action) {
    if (!action) return;
    switch (action.type) {
      case "openOverlay": open(action.target); break;
      case "closeOverlay": close(); break;
      case "navigate": navigate(action.path); break;
    }
  };
}

// ─── 블록 목록 렌더러 ────────────────────────────────────────────────────────

function BlockList({
  blocks,
  handleAction,
}: {
  blocks: CMSPage["blocks"];
  handleAction: (action?: Action) => void;
}) {
  const blockRegistry = useContext(BlockRegistryContext);

  return (
    <>
      {blocks.map((block) => {
        const Component = blockRegistry[block.component];
        if (!Component) return null;
        const pd = block.padding;
        const paddingStyle =
          pd && (pd.top || pd.right || pd.bottom || pd.left)
            ? { paddingTop: pd.top, paddingBottom: pd.bottom, paddingLeft: pd.left, paddingRight: pd.right }
            : undefined;
        const eventProps = Object.fromEntries(
          Object.entries(block.interaction ?? {}).map(([key, action]) => [
            key,
            () => handleAction(action),
          ]),
        );
        return (
          <div key={block.id} style={paddingStyle}>
            <Component {...block.props} {...eventProps} />
          </div>
        );
      })}
    </>
  );
}

// ─── Overlay Shell ───────────────────────────────────────────────────────────

function OverlayShell({
  overlay,
  onClose,
  handleAction,
  container,
}: {
  overlay: CMSOverlay;
  onClose: () => void;
  handleAction: (action?: Action) => void;
  container: HTMLDivElement | null;
}) {
  const overlayTemplates = useContext(OverlayTemplatesContext);
  // overlay.id는 사용자가 rename 가능하므로, 불변값인 type으로 템플릿을 매칭한다
  const template = overlayTemplates.find((t) => t.type === overlay.type);
  const Renderer = template?.renderer;

  if (!Renderer) return null;

  return (
    <Renderer open onClose={onClose} container={container} props={overlay.props}>
      <BlockList blocks={overlay.blocks} handleAction={handleAction} />
    </Renderer>
  );
}

// ─── OverlayRenderer ─────────────────────────────────────────────────────────

function OverlayRenderer({
  overlays,
  handleAction,
  container,
}: {
  overlays?: CMSOverlay[];
  handleAction: (action?: Action) => void;
  container: HTMLDivElement | null;
}) {
  const { current, close } = useOverlayStore();
  if (!current || !overlays?.length) return null;
  const overlay = overlays.find((o) => o.id === current);
  if (!overlay) return null;
  return <OverlayShell overlay={overlay} onClose={close} handleAction={handleAction} container={container} />;
}

// ─── PageContent ─────────────────────────────────────────────────────────────

const GAP_CLASS: Record<string, string> = {
  none: "", xs: "gap-1", sm: "gap-2", md: "gap-4", lg: "gap-6", xl: "gap-8",
};

function PageContent({ page }: { page: CMSPage }) {
  const { layoutType, layoutProps, blocks, overlays } = page;
  const handleAction = useActionHandler();
  const layoutRenderer = useContext(LayoutRendererContext);
  // ref callback 패턴 — React가 DOM 연결/해제 시 직접 state를 업데이트하므로
  // useLayoutEffect + containerReady 조합보다 StrictMode에서 안전하다.
  // mount: setContainer(div), unmount: setContainer(null)
  const [container, setContainer] = useState<HTMLDivElement | null>(null);

  const blockGap = (layoutProps?.blockGap as string | undefined) ?? "none";
  const blockList = (
    <div className={`flex flex-col ${GAP_CLASS[blockGap] ?? ""}`}>
      <BlockList blocks={blocks} handleAction={handleAction} />
    </div>
  );

  const slots = layoutType && layoutRenderer
    ? layoutRenderer(layoutType, (layoutProps ?? {}) as Record<string, unknown>)
    : {};

  return (
    <>
      <div className="flex flex-col min-h-screen">
        {slots.header}
        <div className="flex-1">{blockList}</div>
        {slots.footer}
      </div>
      {/* portal 타깃: UserScopeWrapper 안에 위치해 스코프 CSS가 적용되도록 함 */}
      <div ref={setContainer} />
      {container && (
        <OverlayRenderer overlays={overlays} handleAction={handleAction} container={container} />
      )}
    </>
  );
}

// ─── PageRenderer ─────────────────────────────────────────────────────────────

interface PageRendererProps {
  page: CMSPage;
}

/**
 * @description CMSPage를 런타임에 렌더링합니다.
 * @param page 렌더링할 CMSPage 데이터
 */
export default function PageRenderer({ page }: PageRendererProps) {
  return (
    <OverlayProvider>
      <PageContent page={page} />
    </OverlayProvider>
  );
}
