/**
 * @file OverlayCanvas.tsx
 * @description 오버레이 편집 모드용 캔버스 프레임.
 * LayoutCanvas와 동일한 크롬 오버레이 패턴 사용:
 * - overflow-hidden 제거 → 첫 번째 블록 컨트롤 바(-top-6)가 폰 프레임 상단 밖으로 노출
 * - 시각 크롬(border·shadow·border-radius)을 absolute 오버레이 div로 분리
 * - pt-7 스페이서 불필요 → 빌더/런타임 간 WYSIWYG 일치
 * transform: translateZ(0) — fixed 포지셔닝도 프레임 안에 포함.
 */
import { useContext, useState } from "react";
import type { CMSOverlay } from "../types";
import { OverlayTemplatesContext } from "../context";
import { UserScopeWrapper } from "../UserScopeWrapper";

const PHONE_FRAME_W = 390;
/** CSS 변수로 툴바 높이·패딩 참조 — cms-theme.css의 --cms-toolbar-h, --cms-canvas-pad 값 사용 */
const CANVAS_MIN_H =
  "calc(100dvh - var(--cms-toolbar-h, 3rem) - var(--cms-canvas-pad, 1.5rem) * 2)";

/**
 * @description 오버레이 편집 모드용 캔버스 프레임.
 * @param overlay 편집 중인 오버레이
 * @param children 오버레이 내부에 렌더링할 블록 목록
 */
export function OverlayCanvas({
  overlay,
  children,
}: {
  overlay: CMSOverlay;
  children?: React.ReactNode;
}) {
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const overlayTemplates = useContext(OverlayTemplatesContext);
  const template = overlayTemplates.find((t) => t.type === overlay.type);
  const Renderer = template?.renderer;

  if (!Renderer) {
    return (
      <div className="p-6 flex justify-center items-start">
        {/*
         * overflow-hidden 제거 → 컨트롤 바(-top-6)가 프레임 밖으로 노출.
         * 시각 크롬(border·shadow)은 DOM 마지막의 absolute 오버레이로 표현.
         */}
        <div
          className="relative bg-gray-100/80"
          style={{ width: PHONE_FRAME_W, minHeight: CANVAS_MIN_H }}
        >
          <p className="text-xs text-gray-400 mb-2 px-4 pt-4">
            오버레이: {overlay.type}
          </p>
          <div className="bg-white rounded-xl shadow p-4">{children}</div>
          <div className="absolute inset-0 rounded-xl shadow-lg border border-gray-200 pointer-events-none" />
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 flex justify-center items-start">
      {/*
       * UserScopeWrapper를 containerRef div 바깥에 감싸, portal로 렌더링되는
       * overlay renderer의 backdrop/header/footer 등 전체 구조가 [data-cms-user-scope] 하위에 포함되도록 함.
       * display: contents로 감싸 flex 레이아웃에 영향을 주지 않음.
       *
       * overflow-hidden 제거 → 컨트롤 바(-top-6)가 프레임 밖으로 노출.
       * transform: translateZ(0) — fixed 포지셔닝도 프레임 안에 포함.
       * 시각 크롬(border·shadow)은 DOM 마지막의 absolute 오버레이로 표현.
       */}
      <UserScopeWrapper style={{ display: "contents" }}>
        <div
          ref={setContainer}
          className="relative bg-gray-100/80"
          style={{
            width: PHONE_FRAME_W,
            minHeight: CANVAS_MIN_H,
            transform: "translateZ(0)",
          }}
        >
          {container && (
            <Renderer
              open
              onClose={() => {}}
              container={container}
              props={overlay.props}
            >
              {/* pt-7: Renderer body 안에 overflow가 있을 경우 첫 번째 블록 컨트롤 바(-top-6)가 잘리지 않도록 공간 확보 */}
              <div className="pt-7">{children}</div>
            </Renderer>
          )}
          {/*
           * 시각 크롬 오버레이: 테두리·그림자·border-radius를 표현.
           * DOM 마지막에 위치해 내용 위에 렌더링되지만 pointer-events-none으로 인터랙션에 영향 없음.
           * 컨트롤 바(-top-6)는 이 오버레이 영역(inset-0) 밖에 위치하므로 가려지지 않음.
           */}
          <div className="absolute inset-0 rounded-xl shadow-lg border border-gray-200 pointer-events-none" />
        </div>
      </UserScopeWrapper>
    </div>
  );
}
