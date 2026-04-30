/**
 * @file CMSBuilder.tsx
 * @description 외부 프로젝트에 임베드 가능한 CMS 빌더 컴포넌트.
 * dnd-kit DndContext 위에 좌측 팔레트(LeftSidebar), 중앙 캔버스(Canvas),
 * 우측 인스펙터(RightSidebar)를 배치합니다.
 *
 * 주요 기능:
 * - 블록/오버레이 드래그·추가·삭제·정렬
 * - 레이아웃 타입 및 props 편집
 * - JSON 가져오기/내보내기
 * - JSX 코드 보기 및 페이지 저장 모달
 * - 새 탭 미리보기 (localStorage → PreviewPage)
 *
 * @param onSave 페이지 저장 핸들러. 생략 시 defaultSave 사용
 * @param initialPage 초기 페이지 데이터 (불러오기용)
 */

import { useState, useRef, useMemo, useCallback } from "react";
import { createPortal } from "react-dom";
import {
  DndContext,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";

import type { CMSPage, CMSOverlay } from "./types";
import { useBuilderState } from "./state/builderStore";
import { BlockMetaContext, BlockRegistryContext, LayoutTemplatesContext, OverlayTemplatesContext, CodegenConfigContext } from "./context";
import { useContext } from "react";
import LeftSidebar from "./palette/LeftSidebar";
import RightSidebar from "./inspector/RightSidebar";
import { Canvas, PaletteDragOverlay } from "./canvas/LayoutCanvas";
import { downloadPageJson, parsePageJson } from "./codegen/exportJson";
import { generateJSX } from "./codegen/exportCode";
import SavePageModal from "./SavePageModal";
import type { SavePageParams } from "./SavePageModal";

// ── Props ──────────────────────────────────────────────────────────────────────

export interface CMSBuilderProps {
  /** 페이지 저장 핸들러 */
  onSave?: (page: CMSPage, params: SavePageParams) => void | Promise<void>;
  /** 초기 페이지 데이터 (불러오기용) */
  initialPage?: CMSPage;
  /**
   * 빌더 모드.
   * 'edit': 기존 페이지 수정 — 가져오기 버튼 비표시.
   * 'create': 새 페이지 생성 (기본값).
   */
  mode?: "create" | "edit";
  /** 편집 모드에서 저장 모달의 초기 페이지명 */
  initialPageName?: string;
  /** 현재 페이지 승인 상태 (WORK / PENDING / APPROVED / REJECTED) */
  approveState?: string;
  /** 반려 사유 — REJECTED 상태일 때 배너에 표시 */
  rejectedReason?: string | null;
}

// ── CMSBuilder ─────────────────────────────────────────────────────────────────

export function CMSBuilder({ onSave, initialPage, mode = "create", initialPageName, approveState, rejectedReason }: CMSBuilderProps) {
  const blockMeta = useContext(BlockMetaContext);
  const blockRegistry = useContext(BlockRegistryContext);
  const overlayTemplates = useContext(OverlayTemplatesContext);
  const layouts = useContext(LayoutTemplatesContext);
  const codegenConfig = useContext(CodegenConfigContext);

  const builder = useBuilderState(blockMeta, initialPage);
  const [rightTab, setRightTab] = useState<"props" | "layout" | "json">("layout");
  const [codeOpen, setCodeOpen] = useState(false);
  const [saveOpen, setSaveOpen] = useState(false);
  const [activeDragId, setActiveDragId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
  );

  // activeBlocks/overlays는 builderStore에서 이미 useMemo로 관리되므로 여기서 find만 메모이제이션
  const selectedBlock = useMemo(
    () => builder.activeBlocks.find((b) => b.id === builder.selectedBlockId) ?? null,
    [builder.activeBlocks, builder.selectedBlockId],
  );
  const editingOverlay = useMemo<CMSOverlay | undefined>(
    () =>
      builder.editingOverlayId
        ? builder.overlays.find((o) => o.id === builder.editingOverlayId)
        : undefined,
    [builder.editingOverlayId, builder.overlays],
  );

  const activePaletteType = activeDragId?.startsWith("palette::")
    ? activeDragId.replace("palette::", "")
    : null;

  // ── 안정화된 핸들러 ─────────────────────────────────────────────────────────
  // builder 액션들은 builderStore에서 useCallback으로 안정화되어 있으므로
  // 여기서 복합 동작만 useCallback으로 감쌈

  // ref로 최신 id만 유지 — 객체 전체를 deps에 넣지 않아도 stale 없음
  const selectedBlockIdRef = useRef<string | null>(null);
  selectedBlockIdRef.current = builder.selectedBlockId;
  const editingOverlayIdRef = useRef<string | undefined>(undefined);
  editingOverlayIdRef.current = editingOverlay?.id;

  const handleSelectBlock = useCallback(
    (id: string) => { builder.selectBlock(id); setRightTab("props"); },
    [builder.selectBlock],
  );

  const handleDeselect = useCallback(
    () => builder.selectBlock(null),
    [builder.selectBlock],
  );

  const handleEnterOverlay = useCallback(
    (id: string) => { builder.enterOverlay(id); setRightTab("layout"); },
    [builder.enterOverlay],
  );

  // selectedBlock 객체 대신 ref로 id만 읽어 props 수정 중 핸들러 재생성 방지
  const handlePropsChange = useCallback(
    (newProps: Record<string, unknown>) => {
      const id = selectedBlockIdRef.current;
      if (id) builder.updateBlockProps(id, newProps);
    },
    [builder.updateBlockProps],
  );

  const handlePaddingChange = useCallback(
    (padding: Parameters<typeof builder.updateBlockPadding>[1]) => {
      const id = selectedBlockIdRef.current;
      if (id) builder.updateBlockPadding(id, padding);
    },
    [builder.updateBlockPadding],
  );

  const handleInteractionChange = useCallback(
    (interaction: Parameters<typeof builder.updateBlockInteraction>[1]) => {
      const id = selectedBlockIdRef.current;
      if (id) builder.updateBlockInteraction(id, interaction);
    },
    [builder.updateBlockInteraction],
  );

  // editingOverlay 객체 대신 ref로 id만 읽어 오버레이 편집 중 핸들러 재생성 방지
  const handleOverlayPropsChange = useCallback(
    (props: Record<string, unknown>) => {
      const id = editingOverlayIdRef.current;
      if (id) builder.updateOverlayProps(id, props);
    },
    [builder.updateOverlayProps],
  );

  function handleDragStart(event: DragStartEvent) {
    setActiveDragId(event.active.id as string);
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveDragId(null);
    const { active, over } = event;
    if (!over) return;

    const activeData = active.data.current;
    const blockIds = builder.activeBlocks.map((b) => b.id);

    if (activeData?.type === "palette-item") {
      if (over.id === "canvas-drop-zone") {
        builder.addBlock(activeData.blockType as string);
      } else {
        const overIdx = blockIds.indexOf(over.id as string);
        if (overIdx !== -1) {
          builder.addBlock(activeData.blockType as string, overIdx);
        } else {
          builder.addBlock(activeData.blockType as string);
        }
      }
      return;
    }

    if (active.id !== over.id) {
      const oldIdx = blockIds.indexOf(active.id as string);
      const newIdx = blockIds.indexOf(over.id as string);
      if (oldIdx !== -1 && newIdx !== -1) builder.reorderBlocks(oldIdx, newIdx);
    }
  }

  function handleImport() {
    fileInputRef.current?.click();
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        builder.loadPage(parsePageJson(ev.target?.result as string));
      } catch {
        alert("유효하지 않은 JSON 파일입니다.");
      }
    };
    reader.readAsText(file);
    e.target.value = "";
  }

  const page = builder.getPage();

  // APPROVED 상태에서 저장 시 확인 후 모달 열기
  function handleSavePageClick() {
    if (approveState === "APPROVED") {
      if (!window.confirm(
        "이 페이지는 승인된 상태입니다.\n수정하면 승인이 취소되고 '작업 중' 상태로 돌아갑니다.\n계속 수정하시겠습니까?"
      )) return;
    }
    setSaveOpen(true);
  }

  // 편집 모드: DB 저장 상태로 복원 / 생성 모드: 빈 상태로 초기화
  const handleReset = useCallback(() => {
    if (mode === "edit" && initialPage) {
      if (!window.confirm("마지막 저장 상태로 되돌아가시겠습니까?\n현재 편집 내용은 사라집니다.")) return;
      builder.loadPage(initialPage);
    } else {
      builder.clearBlocks();
    }
  }, [mode, initialPage, builder.loadPage, builder.clearBlocks]);

  // page 선언 이후에 위치해야 TDZ 에러가 발생하지 않음
  const handlePreview = useCallback(() => {
    localStorage.setItem("cms_preview", JSON.stringify(page));
    // BASE_URL(=import.meta.env.BASE_URL)을 prefix로 붙여 nginx 프록시 모드에서도 올바른 경로로 이동
    const base = import.meta.env.BASE_URL.replace(/\/$/, "");
    window.open(`${base}/preview`, "_blank");
  }, [page]);

  // Context 정보(layouts, codegenConfig, overlayTemplates)를 포함해 코드를 생성한 뒤
  // SavePageParams.code에 주입하여 저장 함수가 올바른 코드를 받도록 래핑
  const wrappedOnSave = useMemo(() => {
    if (!onSave) return undefined;
    return (savePage: CMSPage, params: SavePageParams) =>
      onSave(savePage, {
        ...params,
        code: generateJSX(savePage, layouts, codegenConfig, overlayTemplates),
      });
  }, [onSave, layouts, codegenConfig, overlayTemplates]);

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="flex flex-col h-screen bg-gray-50 overflow-hidden">
        {/* ── 툴바 ── */}
        <Toolbar
          blockCount={builder.blocks.length}
          layoutType={builder.layoutType}
          editingOverlay={editingOverlay}
          onExitOverlay={builder.exitOverlay}
          // 편집 모드에서는 외부 파일 가져오기가 기존 편집 내용을 덮어쓸 수 있어 비표시
          showImport={mode !== "edit"}
          onImport={handleImport}
          onExport={() => downloadPageJson(page)}
          onViewCode={() => setCodeOpen(true)}
          onSavePage={handleSavePageClick}
          onPreview={handlePreview}
          onClear={handleReset}
          hasInitialPage={mode === "edit" && !!initialPage}
          approveState={approveState}
        />

        {/* ── 승인 상태 배너 ── */}
        {approveState === "PENDING" && (
          <div className="px-4 py-2 bg-yellow-50 border-b border-yellow-200 text-xs text-yellow-800 flex items-center gap-2 flex-shrink-0">
            <span>⏳</span>
            <span>승인 요청 중인 페이지입니다. 승인이 완료되거나 요청이 취소된 후 수정할 수 있습니다.</span>
          </div>
        )}
        {approveState === "REJECTED" && (
          <div className="px-4 py-2 bg-red-50 border-b border-red-200 text-xs text-red-800 flex items-center gap-2 flex-shrink-0">
            <span>⚠</span>
            <span>반려 사유: {rejectedReason || "(사유 없음)"}</span>
          </div>
        )}

        <div className="flex flex-1 overflow-hidden">
          {/* ── 좌측: 블록 팔레트 + Overlays ── */}
          <LeftSidebar
            onAdd={builder.addBlock}
            blockMeta={blockMeta}
            blockRegistry={blockRegistry}
            overlayTemplates={overlayTemplates}
            overlays={builder.overlays}
            editingOverlayId={builder.editingOverlayId}
            onAddOverlayFromTemplate={builder.addOverlayFromTemplate}
            onRemoveOverlay={builder.removeOverlay}
            onRenameOverlay={builder.renameOverlay}
            onEnterOverlay={handleEnterOverlay}
            onExitOverlay={builder.exitOverlay}
          />

          {/* ── 중앙: 캔버스 ── */}
          <Canvas
            blocks={builder.activeBlocks}
            page={page}
            selectedBlockId={builder.selectedBlockId}
            activePaletteType={activePaletteType}
            editingOverlay={editingOverlay}
            blockMeta={blockMeta}
            blockRegistry={blockRegistry}
            onSelectBlock={handleSelectBlock}
            onRemoveBlock={builder.removeBlock}
            onDeselect={handleDeselect}
          />

          {/* ── 우측: 속성 / 레이아웃 / JSON ── */}
          <RightSidebar
            selectedBlock={selectedBlock}
            layoutType={builder.layoutType}
            layoutProps={builder.layoutProps}
            page={page}
            overlays={builder.overlays}
            overlayTemplates={overlayTemplates}
            editingOverlay={editingOverlay}
            activeTab={rightTab}
            isEditingOverlay={!!editingOverlay}
            blockMeta={blockMeta}
            onTabChange={setRightTab}
            onPropsChange={handlePropsChange}
            onPaddingChange={handlePaddingChange}
            onInteractionChange={handleInteractionChange}
            onOverlayPropsChange={handleOverlayPropsChange}
            onLayoutTypeChange={builder.updateLayoutType}
            onLayoutPropsChange={builder.updateLayoutProps}
          />
        </div>

        <input ref={fileInputRef} type="file" accept=".json" className="hidden" onChange={handleFileChange} />

        {/* createPortal로 document.body에 직접 마운트 — overflow/flex 컨테이너의 fixed 포지셔닝 제약을 우회 */}
        {codeOpen && createPortal(
          <CodeModal code={generateJSX(page, layouts, codegenConfig, overlayTemplates)} onClose={() => setCodeOpen(false)} />,
          document.body,
        )}

        {saveOpen && createPortal(
          <SavePageModal
            page={page}
            onSave={wrappedOnSave}
            onClose={() => setSaveOpen(false)}
            initialPageName={initialPageName}
          />,
          document.body,
        )}
      </div>

      <PaletteDragOverlay activePaletteType={activePaletteType} blockMeta={blockMeta} />
    </DndContext>
  );
}

// ── 툴바 ──────────────────────────────────────────────────────────────────────

function Toolbar({
  blockCount,
  layoutType,
  editingOverlay,
  onExitOverlay,
  showImport,
  onImport,
  onExport,
  onViewCode,
  onSavePage,
  onPreview,
  onClear,
  hasInitialPage,
  approveState,
}: {
  blockCount: number;
  layoutType: string | undefined;
  editingOverlay?: CMSOverlay;
  onExitOverlay: () => void;
  showImport: boolean;
  onImport: () => void;
  onExport: () => void;
  onViewCode: () => void;
  onSavePage: () => void;
  onPreview: () => void;
  onClear: () => void;
  hasInitialPage: boolean;
  approveState?: string;
}) {
  const isPending = approveState === "PENDING";
  return (
    <header className="flex items-center justify-between px-4 h-12 border-b border-gray-200 bg-white flex-shrink-0">
      <div className="flex items-center gap-2">
        <span className="font-bold text-primary text-sm tracking-tight">CMS Builder</span>
        {editingOverlay ? (
          <>
            <button
              onClick={onExitOverlay}
              className="px-2 py-0.5 bg-gray-100 hover:bg-gray-200 text-gray-500 text-xs rounded-full"
            >
              ← 페이지
            </button>
            <span className="px-2 py-0.5 bg-amber-100 text-amber-700 text-xs rounded-full font-medium">
              {editingOverlay.type}: {editingOverlay.id}
            </span>
          </>
        ) : (
          <>
            {layoutType && <LayoutBadge layoutType={layoutType} />}
            {blockCount > 0 && (
              <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">
                {blockCount}개
              </span>
            )}
          </>
        )}
      </div>
      <div className="flex items-center gap-1.5">
        {showImport && <ToolbarButton onClick={onImport}>가져오기</ToolbarButton>}
        <ToolbarButton onClick={onExport} disabled={blockCount === 0 && !layoutType}>
          내보내기
        </ToolbarButton>
        <ToolbarButton onClick={onViewCode} disabled={blockCount === 0 && !layoutType}>
          코드 보기
        </ToolbarButton>
        <ToolbarButton
          onClick={onSavePage}
          variant="success"
          disabled={(blockCount === 0 && !layoutType) || isPending}
        >
          {isPending ? "승인 요청 중" : "페이지 저장"}
        </ToolbarButton>
        <ToolbarButton onClick={onPreview} variant="primary" disabled={blockCount === 0}>
          미리보기
        </ToolbarButton>
        {(blockCount > 0 || layoutType || hasInitialPage) && (
          <ToolbarButton onClick={onClear} variant="danger">초기화</ToolbarButton>
        )}
      </div>
    </header>
  );
}

function LayoutBadge({ layoutType }: { layoutType: string }) {
  const layouts = useContext(LayoutTemplatesContext);
  const label = layouts.find((t) => t.id === layoutType)?.label ?? layoutType;
  return (
    <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full font-medium">
      {label}
    </span>
  );
}

function ToolbarButton({
  children,
  onClick,
  variant = "default",
  disabled = false,
}: {
  children: React.ReactNode;
  onClick: () => void;
  variant?: "default" | "primary" | "success" | "danger";
  disabled?: boolean;
}) {
  const styles = {
    default: "bg-gray-100 hover:bg-gray-200 text-gray-700",
    primary: "bg-primary hover:bg-primary-dark text-white",
    success: "bg-green-600 hover:bg-green-700 text-white",
    danger: "bg-red-50 hover:bg-red-100 text-red-600",
  };

  return (
    <button
      className={`px-3 py-1.5 text-xs rounded-lg font-medium transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${styles[variant]}`}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

// ── 코드 모달 ─────────────────────────────────────────────────────────────────

function CodeModal({ code, onClose }: { code: string; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-6"
      onClick={onClose}
    >
      <div
        className="relative bg-gray-900 rounded-2xl shadow-2xl w-full max-w-[42rem] max-h-[80vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-2.5 border-b border-gray-700 flex-shrink-0">
          <span className="text-xs font-medium text-gray-400">생성된 JSX 코드</span>
          <div className="flex items-center gap-2">
            <button
              className="px-3 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 rounded"
              onClick={() => navigator.clipboard.writeText(code)}
            >
              복사
            </button>
            <button className="text-gray-400 hover:text-gray-200 text-lg leading-none" onClick={onClose}>
              ✕
            </button>
          </div>
        </div>
        <pre className="flex-1 overflow-auto p-4 text-xs text-green-300 font-mono whitespace-pre">
          {code}
        </pre>
      </div>
    </div>
  );
}
