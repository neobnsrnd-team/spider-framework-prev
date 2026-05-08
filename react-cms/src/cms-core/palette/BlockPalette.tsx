/**
 * @file BlockPalette.tsx
 * @description 블록 팔레트 컴포넌트. 카테고리·도메인 2단계 계층으로 표시. 카테고리는 블록 목록에서 동적으로 파생됨.
 */
import { useDraggable } from "@dnd-kit/core";
import type { BlockMeta } from "../types";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { UserScopeWrapper } from "../UserScopeWrapper";

// ─── 썸네일 상수 ────────────────────────────────────────────
const INNER_W = 390;
const SCALE = 0.5;
const THUMB_H = 80;

// ─── 카테고리 / 도메인 레이블 ────────────────────────────────

/** 알려진 카테고리의 한글/영문 레이블. 없는 카테고리는 그대로 표시. */
const CATEGORY_LABEL: Record<string, string> = {
  base:      "Base",
  composite: "Composite",
  page:      "Page",
  core:      "Core",
  biz:       "Biz",
  modules:   "Modules",
  pages:     "Pages"
};

/** 알려진 도메인의 한글/영문 레이블. 없는 도메인은 그대로 표시. */
const DOMAIN_LABEL: Record<string, string> = {
  bank:     "Bank",
  card:     "Card",
  auth:     "Auth",
  home:     "Home",
  action:   "Action",
  feedback: "Feedback",
  account:  "Account",
};

/** 카테고리 정렬 우선순위 (목록에 없는 카테고리는 뒤에 순서대로 추가).
 *  주 사용 카테고리(core → modules → biz) 먼저 노출. */
const PREFERRED_CATEGORY_ORDER = ["core", "modules", "biz", "base", "composite", "page", "pages"];

// ─── 라이브 썸네일 ──────────────────────────────────────────

interface BlockThumbnailProps {
  type: string;
  blockRegistry: Record<string, React.ComponentType<Record<string, unknown>>>;
  blockMeta: Record<string, BlockMeta>;
}

/**
 * @description 실제 컴포넌트를 CSS scale로 축소해 썸네일로 렌더링.
 * Intersection Observer로 뷰포트에 진입할 때만 실제 컴포넌트를 마운트합니다.
 * 진입 전에는 같은 높이의 placeholder를 표시해 레이아웃이 밀리지 않도록 합니다.
 * 한 번 visible 처리되면 다시 해제되지 않아(observer.disconnect) 스크롤 복귀 시에도 유지됩니다.
 * @param type 블록 타입 문자열
 * @param blockRegistry 블록 렌더러 레지스트리
 * @param blockMeta 블록 메타 정보 맵
 * @returns React 컴포넌트 | null
 */
const BlockThumbnail = React.memo(function BlockThumbnail({ type, blockRegistry, blockMeta }: BlockThumbnailProps) {
  const Component = blockRegistry[type];
  const meta = blockMeta[type];
  const containerRef = useRef<HTMLDivElement>(null);
  // 화면에 진입한 적이 있으면 true — 한 번 true가 되면 다시 false로 돌아가지 않음
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const el = containerRef.current;
    if (!el || !Component || !meta) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          // 한 번 진입하면 더 이상 관찰 불필요 — 컴포넌트는 계속 유지
          observer.disconnect();
        }
      },
      // 100px 여유를 두어 스크롤 직전에 미리 렌더링 시작
      { rootMargin: "100px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [Component, meta]);

  if (!Component || !meta) return null;

  return (
    <div ref={containerRef} className="w-full overflow-hidden rounded-t-xl bg-white" style={{ height: THUMB_H }}>
      {isVisible ? (
        <UserScopeWrapper
          style={{
            width: INNER_W,
            transform: `scale(${SCALE})`,
            transformOrigin: "top left",
            pointerEvents: "none",
            userSelect: "none",
          }}
        >
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <Component {...(meta.defaultProps as any)} />
        </UserScopeWrapper>
      ) : (
        // 실제 컴포넌트와 동일한 높이를 유지해 레이아웃 shift 방지
        <div className="w-full h-full bg-gray-100 animate-pulse rounded-t-xl" />
      )}
    </div>
  );
});

// ─── 드래그 가능한 팔레트 아이템 ───────────────────────────

interface PaletteItemProps {
  type: string;
  name: string;
  onAdd: (type: string) => void;
  blockRegistry: Record<string, React.ComponentType<Record<string, unknown>>>;
  blockMeta: Record<string, BlockMeta>;
}

/**
 * @description 드래그 & 클릭으로 캔버스에 추가할 수 있는 팔레트 아이템.
 * React.memo로 감싸 type/name/onAdd가 바뀌지 않으면 리렌더링을 건너뜁니다.
 * @param type 블록 타입 문자열
 * @param name 표시 이름
 * @param onAdd 클릭 시 블록 추가 콜백 (stable 참조 권장)
 * @returns React 컴포넌트
 */
export const DraggablePaletteItem = React.memo(function DraggablePaletteItem({ type, name, onAdd, blockRegistry, blockMeta }: PaletteItemProps) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `palette::${type}`,
    data: { type: "palette-item", blockType: type },
  });

  // onAdd와 type이 stable하면 이 핸들러도 재생성되지 않음
  const handleClick = useCallback(() => onAdd(type), [onAdd, type]);

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      onClick={handleClick}
      className={`rounded-xl border overflow-hidden transition-all cursor-grab active:cursor-grabbing select-none bg-white ${
        isDragging
          ? "opacity-40 border-primary ring-2 ring-primary/40"
          : "border-gray-200 hover:border-primary/50 hover:shadow-sm"
      }`}
    >
      <BlockThumbnail type={type} blockRegistry={blockRegistry} blockMeta={blockMeta} />
      <div className="px-2.5 py-1.5 border-t border-gray-100 bg-gray-50">
        <span className="text-xs font-medium text-gray-600">{name}</span>
      </div>
    </div>
  );
});

// ─── 접기/펼치기 섹션 헤더 ──────────────────────────────────

interface SectionHeaderProps {
  label: string;
  collapsed: boolean;
  onToggle: () => void;
  indent?: boolean;
}

/**
 * @description 접기/펼치기 가능한 섹션 헤더.
 * indent=false는 카테고리(강조 스타일, 좌측 primary 바 + 진한 텍스트),
 * indent=true는 도메인(미니멀 서브 헤더)으로 시각 위계를 분리한다.
 *
 * @param label 표시할 레이블
 * @param collapsed 현재 접힌 상태 여부
 * @param onToggle 토글 콜백
 * @param indent 들여쓰기 여부 (도메인 서브 섹션용)
 * @returns React 컴포넌트
 */
function SectionHeader({ label, collapsed, onToggle, indent = false }: SectionHeaderProps) {
  // 도메인(서브) 헤더: 미니멀
  if (indent) {
    return (
      <button
        onClick={onToggle}
        className="flex items-center justify-between w-full mb-2 group pl-2"
      >
        <p className="font-bold uppercase tracking-widest text-[9px] text-gray-400">{label}</p>
        <svg
          className={`w-3 h-3 text-gray-400 transition-transform duration-200 ${collapsed ? "-rotate-90" : ""}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
    );
  }

  // 카테고리(메인) 헤더: 좌측 primary 액센트 바 + 진한 텍스트로 강조
  return (
    <button
      onClick={onToggle}
      className="flex items-center justify-between w-full mb-3 group"
    >
      <div className="flex items-center gap-2">
        <span className="w-1 h-4 rounded-full bg-primary" aria-hidden="true" />
        <p className="font-bold uppercase tracking-wider text-xs text-gray-800">{label}</p>
      </div>
      <svg
        className={`w-4 h-4 text-gray-500 transition-transform duration-200 ${collapsed ? "-rotate-90" : ""}`}
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2.5}
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    </button>
  );
}

// ─── 블록 팔레트 ─────────────────────────────────────────────

interface BlockPaletteProps {
  onAdd: (type: string) => void;
  filter?: string;
  blockMeta: Record<string, BlockMeta>;
  blockRegistry: Record<string, React.ComponentType<Record<string, unknown>>>;
}

/**
 * @description 블록 팔레트. 검색 시 평탄 목록, 미검색 시 카테고리/도메인 2단계 계층으로 표시.
 * 카테고리는 전달된 blockMeta에서 동적으로 파생되며, PREFERRED_CATEGORY_ORDER 기준으로 정렬됩니다.
 * 도메인이 없는 카테고리는 단일 목록, 도메인이 있는 경우 도메인별 서브 섹션으로 표시합니다.
 * @param onAdd 블록 추가 콜백
 * @param filter 검색 필터 문자열
 * @param blockMeta 블록 메타 정보 맵
 * @param blockRegistry 블록 렌더러 레지스트리
 * @returns React 컴포넌트
 */
export function BlockPalette({ onAdd, filter, blockMeta, blockRegistry }: BlockPaletteProps) {
  const q = filter?.trim().toLowerCase() ?? "";
  const [collapsed, setCollapsed] = React.useState<Record<string, boolean>>({});

  // setCollapsed는 stable setter이므로 deps 불필요
  const toggle = useCallback(
    (key: string) => setCollapsed((prev) => ({ ...prev, [key]: !prev[key] })),
    [],
  );

  // blockMeta 참조가 바뀔 때만 재계산
  const allEntries = useMemo(() => Object.entries(blockMeta), [blockMeta]);

  // 검색 필터 결과 메모이제이션 — 훅 규칙상 조건문 이전에 선언
  const filteredEntries = useMemo(() => {
    if (!q) return null;
    return allEntries.filter(
      ([type, meta]) =>
        meta.name.toLowerCase().includes(q) || type.toLowerCase().includes(q),
    );
  }, [q, allEntries]);

  // 카테고리 목록: blockMeta에서 동적 파생 후 우선순위 순 정렬
  const allCategories = useMemo(
    () => [...new Set(allEntries.map(([, m]) => m.category))],
    [allEntries],
  );
  const categories = useMemo(
    () => [
      ...PREFERRED_CATEGORY_ORDER.filter((c) => allCategories.includes(c)),
      ...allCategories.filter((c) => !PREFERRED_CATEGORY_ORDER.includes(c)),
    ],
    [allCategories],
  );

  // ── 검색 결과 ──
  if (filteredEntries !== null) {
    if (filteredEntries.length === 0) {
      return <p className="text-xs text-gray-400 text-center py-8">검색 결과가 없습니다</p>;
    }
    return (
      <div className="flex flex-col gap-2">
        {filteredEntries.map(([type, meta]) => (
          <DraggablePaletteItem
            key={type}
            type={type}
            name={meta.name}
            onAdd={onAdd}
            blockMeta={blockMeta}
            blockRegistry={blockRegistry}
          />
        ))}
      </div>
    );
  }

  // ── 카테고리별 계층 ──
  return (
    <div className="flex flex-col gap-5">
      {categories.map((category) => {
        const items = allEntries.filter(([, meta]) => meta.category === category);
        if (items.length === 0) return null;

        const catKey = category;
        const isCatCollapsed = collapsed[catKey] ?? false;
        // 도메인이 하나라도 있으면 도메인별 서브 섹션으로 표시
        const hasDomain = items.some(([, meta]) => !!meta.domain);

        return (
          <div key={category}>
            <SectionHeader
              label={CATEGORY_LABEL[category] ?? category}
              collapsed={isCatCollapsed}
              onToggle={() => toggle(catKey)}
            />

            {!isCatCollapsed && (
              !hasDomain
                // 도메인 없음: 단일 목록
                ? (
                  <div className="flex flex-col gap-2">
                    {items.map(([type, meta]) => (
                      <DraggablePaletteItem
                        key={type}
                        type={type}
                        name={meta.name}
                        onAdd={onAdd}
                        blockMeta={blockMeta}
                        blockRegistry={blockRegistry}
                      />
                    ))}
                  </div>
                )
                // 도메인 있음: 도메인별 서브 섹션
                : (() => {
                  const domains = [...new Set(items.map(([, meta]) => meta.domain ?? "기타"))];
                  return (
                    <div className="flex flex-col gap-4">
                      {domains.map((domain) => {
                        const domainKey = `${category}:${domain}`;
                        const isDomainCollapsed = collapsed[domainKey] ?? false;
                        const domainItems = items.filter(([, meta]) => (meta.domain ?? "기타") === domain);

                        return (
                          <div key={domain} className="pl-1">
                            <SectionHeader
                              label={DOMAIN_LABEL[domain] ?? domain}
                              collapsed={isDomainCollapsed}
                              onToggle={() => toggle(domainKey)}
                              indent
                            />
                            {!isDomainCollapsed && (
                              <div className="flex flex-col gap-2 pl-1">
                                {domainItems.map(([type, meta]) => (
                                  <DraggablePaletteItem
                                    key={type}
                                    type={type}
                                    name={meta.name}
                                    onAdd={onAdd}
                                    blockMeta={blockMeta}
                                    blockRegistry={blockRegistry}
                                  />
                                ))}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  );
                })()
            )}
          </div>
        );
      })}
    </div>
  );
}
