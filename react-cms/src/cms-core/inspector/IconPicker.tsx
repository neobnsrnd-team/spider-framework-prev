/**
 * @file IconPicker.tsx
 * @description Lucide 아이콘 선택기 컴포넌트.
 * lucide-react 전체 아이콘 목록을 6열 그리드로 표시하고 검색·선택 가능합니다.
 * 선택 값은 kebab-case 정규화 이름으로 반환됩니다 (예: "ChevronRight" → "chevron-right").
 * useDeferredValue로 검색 입력을 디퍼드 처리해 타이핑 중 렌더링 부하를 줄입니다.
 *
 * @param value 현재 선택된 아이콘 이름 (kebab-case)
 * @param onSelect 아이콘 선택 콜백
 */
import { memo, useDeferredValue, useMemo, useRef, useState } from "react";
import * as LucideIcons from "lucide-react";

const ALL_ICON_NAMES = (Object.entries(LucideIcons) as [string, unknown][])
  .filter(([originalName, val]) =>
    /^[A-Z]/.test(originalName) &&
    typeof val === 'object' &&
    val !== null &&
    '$$typeof' in (val as object) &&
    typeof (val as Record<string, unknown>).displayName === 'string'
  )
  .map(([name]) => name);

const toKebab = (name: string) =>
  name.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase();

// ─── 개별 아이콘 버튼 ────────────────────────────────────────
const IconGridItem = memo(function IconGridItem({
  name,
  selected,
  onSelect,
}: {
  name: string;
  selected: boolean;
  onSelect: (name: string) => void;
}) {
  const Icon = (LucideIcons as unknown as Record<string, React.ComponentType<React.SVGProps<SVGSVGElement>>>)[name];
  if (!Icon) return null;
  return (
    <button
      type="button"
      title={name}
      onClick={() => onSelect(toKebab(name))}
      className={`flex flex-col items-center justify-center gap-0.5 p-1.5 rounded-lg transition-colors border ${
        selected
          ? "bg-primary/10 border-primary"
          : "border-transparent hover:bg-surface-hover hover:border-border"
      }`}
    >
      <Icon className={`w-4 h-4 ${selected ? "text-primary" : "text-text-secondary"}`} />
      <span className="text-[8px] text-text-muted leading-none text-center line-clamp-1 w-full">
        {name}
      </span>
    </button>
  );
});

// 검색어 없을 때 초기 렌더링 비용을 줄이기 위한 표시 상한.
// ~1400개를 한 번에 렌더하면 DOM 생성이 느리므로 처음엔 6열×20행만 표시한다.
const INITIAL_LIMIT = 120;

// ─── IconPicker ───────────────────────────────────────────────
export interface IconPickerProps {
  value?: string;
  onSelect: (iconName: string) => void;
}

export default memo(function IconPicker({ value, onSelect }: IconPickerProps) {
  const [query, setQuery] = useState("");
  const [showAll, setShowAll] = useState(false);
  const deferred = useDeferredValue(query);

  // 검색어 변경 시 showAll을 초기화해 재검색 결과를 처음부터 보여준다.
  const prevDeferred = useRef(deferred);
  if (prevDeferred.current !== deferred) {
    prevDeferred.current = deferred;
    if (showAll) setShowAll(false);
  }

  const filtered = useMemo(
    () =>
      deferred.trim()
        ? ALL_ICON_NAMES.filter((n) => n.toLowerCase().includes(deferred.toLowerCase()))
        : ALL_ICON_NAMES,
    [deferred],
  );

  // 검색어가 있으면 결과 전체 표시, 없으면 INITIAL_LIMIT까지만 표시
  const display = useMemo(
    () => (!showAll && !deferred.trim() ? filtered.slice(0, INITIAL_LIMIT) : filtered),
    [filtered, deferred, showAll],
  );
  const remaining = !showAll && !deferred.trim() ? ALL_ICON_NAMES.length - INITIAL_LIMIT : 0;

  return (
    <div className="rounded-xl border border-border overflow-hidden bg-surface">
      {/* 검색 */}
      <div className="px-2.5 py-2 border-b border-divider">
        <input
          type="text"
          placeholder="아이콘 검색... (예: Arrow, Home)"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full h-7 px-2.5 rounded-lg bg-input-bg border border-input-border text-xs text-input-text placeholder:text-text-muted outline-none focus:border-input-border-focus transition-colors"
        />
      </div>

      {/* 아이콘 그리드 */}
      <div className="max-h-44 overflow-y-auto">
        {display.length === 0 ? (
          <p className="py-6 text-center text-xs text-text-muted">일치하는 아이콘이 없습니다.</p>
        ) : (
          <div className="grid grid-cols-6 gap-0.5 p-1.5">
            {display.map((name) => (
              <IconGridItem
                key={name}
                name={name}
                selected={toKebab(name) === value}
                onSelect={onSelect}
              />
            ))}
          </div>
        )}
        {/* 초기 표시 상한 초과분 — 클릭 시 전체 렌더 */}
        {remaining > 0 && (
          <button
            type="button"
            onClick={() => setShowAll(true)}
            className="w-full py-1.5 text-xs text-text-muted hover:text-text-secondary hover:bg-surface-hover transition-colors border-t border-divider"
          >
            더 보기 (+{remaining.toLocaleString()}개)
          </button>
        )}
      </div>

      {/* 선택된 아이콘 */}
      {value && (
        <div className="px-3 py-1.5 border-t border-divider bg-input-bg/60 flex items-center gap-2">
          <span className="text-xs text-text-muted">선택됨:</span>
          <span className="text-xs text-primary font-medium">{value}</span>
        </div>
      )}
    </div>
  );
});

