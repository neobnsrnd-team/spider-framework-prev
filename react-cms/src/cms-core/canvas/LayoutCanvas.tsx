/**
 * @file LayoutCanvas.tsx
 * @description 캔버스 영역 — 레이아웃 크롬 + 드롭 존 + 정렬 가능 블록
 */
import React, { useCallback, useContext, useMemo } from "react";
import { DragOverlay, useDroppable } from "@dnd-kit/core";
import { SortableContext, verticalListSortingStrategy } from "@dnd-kit/sortable";
import type { CMSBlock, CMSOverlay, CMSPage } from "../types";
import type { BlockMeta } from "../types";
import { SortableBlockWrapper } from "./BlockControls";
import { LayoutRendererContext } from "../context";
import { UserScopeWrapper } from "../UserScopeWrapper";
import { OverlayCanvas } from "./OverlayCanvas";

/** 폰 프레임 너비 */
const PHONE_FRAME_W = 390;

// ─── 블록 갭 헬퍼 ─────────────────────────────────────────────

const GAP_CLASS: Record<string, string> = {
  none: "",
  xs: "gap-1",
  sm: "gap-2",
  md: "gap-4",
  lg: "gap-6",
  xl: "gap-8",
};

function BlockStack({ gap, children }: { gap?: string; children: React.ReactNode }) {
  return (
    <div className={`flex flex-col ${GAP_CLASS[gap ?? "none"] ?? ""}`}>
      {children}
    </div>
  );
}

// ─── 드롭 존 ─────────────────────────────────────────────────

export function CanvasDropZone({ children }: { children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: "canvas-drop-zone" });
  return (
    <div
      ref={setNodeRef}
      className={`min-h-full transition-colors ${isOver ? "bg-primary/5" : ""}`}
    >
      {children}
    </div>
  );
}

// ─── 레이아웃 크롬 ────────────────────────────────────────────

interface LayoutCanvasProps {
  layoutType: string | undefined;
  layoutProps: Record<string, unknown>;
  children: React.ReactNode;
}

export function LayoutCanvas({ layoutType, layoutProps, children }: LayoutCanvasProps) {
  const layoutRenderer = useContext(LayoutRendererContext);
  // layoutType / layoutProps / layoutRenderer가 실제로 바뀔 때만 헤더·푸터 재생성
  // 블록 선택·props 수정 등 무관한 리렌더에서 헤더·푸터가 재생성되는 것을 방지
  const slots = useMemo(
    () =>
      layoutType && layoutRenderer
        ? layoutRenderer(layoutType, layoutProps)
        : {},
    [layoutType, layoutProps, layoutRenderer],
  );

  if (!layoutType) {
    return (
      <div className="p-6 flex justify-center">
        <div style={{ width: PHONE_FRAME_W }}>{children}</div>
      </div>
    );
  }

  return (
    <div className="p-6 flex justify-center items-start">
      {/*
       * transform: translateZ(0) — 외부 레이아웃 컴포넌트가 `fixed` 포지셔닝을 사용할 경우
       * 이 요소를 containing block으로 만들어 fixed 자식이 뷰포트가 아닌 폰 프레임 안에 배치되도록 함.
       *
       * overflow-hidden을 제거하고 시각 크롬(border·shadow)을 절대 위치 오버레이로 분리.
       * 이를 통해 첫 번째 블록의 컨트롤 바(-top-6)가 폰 프레임 상단 밖으로 시각적으로 노출됩니다.
       * (pt-7 스페이서 불필요 → 빌더/런타임 간 WYSIWYG 일치)
       *
       * min-height: CSS 변수(--cms-toolbar-h, --cms-canvas-pad) 사용으로 툴바/패딩 변경 시 단일 수정 지점 확보.
       */}
      <div
        className="relative flex flex-col"
        style={{
          width: PHONE_FRAME_W,
          transform: "translateZ(0)",
          minHeight: "calc(100dvh - var(--cms-toolbar-h, 3rem) - var(--cms-canvas-pad, 1.5rem) * 2)",
        }}
      >
        {slots.header && (
          <div className="pointer-events-none overflow-hidden rounded-t-xl">
            <UserScopeWrapper>{slots.header}</UserScopeWrapper>
          </div>
        )}
        {/* 헤더·푸터 유무에 따라 bg-white 모서리 라운딩 조건부 적용 */}
        {/* px-standard py-md: 실제 레이아웃 컴포넌트의 콘텐츠 영역 패딩과 동일하게 맞춰 WYSIWYG 유지 */}
        <div
          className={`flex-1 bg-white px-4 py-3${!slots.header ? " rounded-t-xl" : ""}${!slots.footer ? " rounded-b-xl" : ""}`}
        >
          {children}
        </div>
        {slots.footer && (
          <div className="pointer-events-none overflow-hidden rounded-b-xl">
            <UserScopeWrapper>{slots.footer}</UserScopeWrapper>
          </div>
        )}
        {/*
         * 시각 크롬 오버레이: 테두리·그림자·border-radius를 표현.
         * DOM 마지막에 위치해 내용 위에 렌더링되지만 pointer-events-none으로 인터랙션에 영향 없음.
         * 컨트롤 바(-top-6)는 이 오버레이 영역(inset-0) 밖에 위치하므로 가려지지 않음.
         */}
        <div className="absolute inset-0 rounded-xl shadow-lg border border-gray-200 pointer-events-none" />
      </div>
    </div>
  );
}

// ─── 정렬 가능 블록 ───────────────────────────────────────────

/**
 * @description 드래그 정렬 + 선택/삭제 컨트롤이 붙은 블록 래퍼.
 * React.memo로 감싸 isSelected나 block 내용이 바뀌지 않으면 리렌더링을 건너뜁니다.
 * onSelectBlock/onRemoveBlock은 부모에서 stable한 참조(useCallback)로 전달받아야 합니다.
 */
export const SortableBlock = React.memo(function SortableBlock({
  block,
  isSelected,
  onSelectBlock,
  onRemoveBlock,
  blockRegistry,
  blockMeta,
}: {
  block: CMSBlock;
  isSelected: boolean;
  /** block.id를 인자로 받는 stable 콜백 */
  onSelectBlock: (id: string) => void;
  /** block.id를 인자로 받는 stable 콜백 */
  onRemoveBlock: (id: string) => void;
  blockRegistry: Record<string, React.ComponentType<Record<string, unknown>>>;
  blockMeta: Record<string, BlockMeta>;
}) {
  const Component = blockRegistry[block.component];

  // block.id와 부모 콜백이 바뀌지 않으면 재생성되지 않음
  const handleSelect = useCallback(() => onSelectBlock(block.id), [onSelectBlock, block.id]);
  const handleRemove = useCallback(() => onRemoveBlock(block.id), [onRemoveBlock, block.id]);
  const handleContentClick = useCallback(
    (e: React.MouseEvent) => { e.stopPropagation(); handleSelect(); },
    [handleSelect],
  );

  return (
    <SortableBlockWrapper
      block={block}
      isSelected={isSelected}
      onSelect={handleSelect}
      onRemove={handleRemove}
      blockMeta={blockMeta}
    >
      <div onClick={handleContentClick}>
        <UserScopeWrapper>
          {Component ? (
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <Component {...(block.props as any)} />
          ) : (
            <UnknownBlock type={block.component} />
          )}
        </UserScopeWrapper>
      </div>
    </SortableBlockWrapper>
  );
});

function UnknownBlock({ type }: { type: string }) {
  return (
    <div className="px-4 py-3 text-sm text-gray-400 border border-dashed border-gray-300 rounded-xl">
      알 수 없는 블록: {type}
    </div>
  );
}

// ─── 캔버스 빈 상태 ──────────────────────────────────────────

export function CanvasEmpty({ isDragging }: { isDragging: boolean }) {
  return (
    <div
      className={`flex flex-col items-center justify-center min-h-48 text-center gap-3 py-16 transition-colors ${
        isDragging ? "bg-primary/5 border-2 border-dashed border-primary/40 rounded-xl mx-4 my-4" : ""
      }`}
    >
      <div className="w-12 h-12 rounded-2xl bg-primary/10 flex items-center justify-center text-xl">
        🧱
      </div>
      <p className="text-sm font-medium text-gray-500">
        {isDragging ? "여기에 놓으세요" : "좌측에서 블록을 드래그하거나 클릭하세요"}
      </p>
    </div>
  );
}

// ─── 드래그 오버레이 ─────────────────────────────────────────

export function PaletteDragOverlay({
  activePaletteType,
  blockMeta,
}: {
  activePaletteType: string | null;
  blockMeta: Record<string, BlockMeta>;
}) {
  return (
    <DragOverlay dropAnimation={null}>
      {activePaletteType && (
        <div className="px-4 py-2 text-sm font-medium text-white bg-primary rounded-xl shadow-lg cursor-grabbing select-none">
          {blockMeta[activePaletteType]?.name ?? activePaletteType}
        </div>
      )}
    </DragOverlay>
  );
}

// ─── 전체 캔버스 (Sortable Context 포함) ─────────────────────

interface CanvasProps {
  blocks: CMSBlock[];
  page: CMSPage;
  selectedBlockId: string | null;
  activePaletteType: string | null;
  editingOverlay?: CMSOverlay;
  onSelectBlock: (id: string) => void;
  onRemoveBlock: (id: string) => void;
  onDeselect: () => void;
  blockRegistry: Record<string, React.ComponentType<Record<string, unknown>>>;
  blockMeta: Record<string, BlockMeta>;
}

export function Canvas({
  blocks,
  page,
  selectedBlockId,
  activePaletteType,
  editingOverlay,
  onSelectBlock,
  onRemoveBlock,
  onDeselect,
  blockRegistry,
  blockMeta,
}: CanvasProps) {
  // blocks 배열 참조가 바뀔 때만 재계산
  const blockIds = useMemo(() => blocks.map((b) => b.id), [blocks]);
  const blockGap = (page.layoutProps as Record<string, unknown>)?.blockGap as string | undefined;

  const blockList = (
    <CanvasDropZone>
      {blocks.length === 0 ? (
        <CanvasEmpty isDragging={!!activePaletteType} />
      ) : (
        <BlockStack gap={blockGap}>
          {blocks.map((block) => (
            <SortableBlock
              key={block.id}
              block={block}
              isSelected={block.id === selectedBlockId}
              onSelectBlock={onSelectBlock}
              onRemoveBlock={onRemoveBlock}
              blockRegistry={blockRegistry}
              blockMeta={blockMeta}
            />
          ))}
        </BlockStack>
      )}
    </CanvasDropZone>
  );

  return (
    <main className="flex-1 overflow-y-auto bg-gray-100" onClick={onDeselect}>
      <SortableContext items={blockIds} strategy={verticalListSortingStrategy}>
        {editingOverlay ? (
          <OverlayCanvas overlay={editingOverlay}>{blockList}</OverlayCanvas>
        ) : (
          <LayoutCanvas layoutType={page.layoutType} layoutProps={page.layoutProps ?? {}}>
            {blockList}
          </LayoutCanvas>
        )}
      </SortableContext>
    </main>
  );
}
