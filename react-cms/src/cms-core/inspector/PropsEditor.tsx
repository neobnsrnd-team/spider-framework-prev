/**
 * @file PropsEditor.tsx
 * @description 블록 props 편집기 (인스펙터 버전).
 * group / array / leaf / icon-picker / event 타입을 모두 지원하는 완전판 props 편집 UI.
 * 선택된 블록의 prop 스키마(propSchema)를 기반으로 동적 폼을 생성합니다.
 * 이벤트 prop(type: "event")은 인터랙션 섹션에서 Action 바인딩으로 처리됩니다.
 * 패딩 편집 섹션이 항상 하단에 표시됩니다.
 *
 * @param block 편집 대상 CMSBlock
 * @param onChange props 변경 핸들러
 * @param onPaddingChange 패딩 변경 핸들러
 * @param blockMeta 블록 메타 정보 맵 (propSchema 조회에 사용)
 * @param overlays 현재 페이지 오버레이 목록 (openOverlay 액션 바인딩에 사용)
 * @param onInteractionChange 이벤트 인터랙션 변경 핸들러
 */
import React, { useCallback, useEffect, useRef } from "react";
import type { Action, BlockInteraction, BlockPadding, CMSBlock, CMSOverlay } from "../types";
import type { BlockMeta, LeafPropField, PropField } from "../types";
import { FieldControl } from "./FieldControl";
import { ArrayField } from "./ArrayField";

interface PropsEditorProps {
  block: CMSBlock;
  onChange: (newProps: Record<string, unknown>) => void;
  onPaddingChange: (padding: BlockPadding) => void;
  blockMeta: Record<string, BlockMeta>;
  overlays?: CMSOverlay[];
  onInteractionChange?: (interaction: BlockInteraction) => void;
}

const inputCls =
  "w-full h-8 px-3 rounded-lg border border-input-border bg-surface text-xs text-input-text outline-none focus:border-input-border-focus transition-colors";

// ── 이벤트 필드 (인터랙션 바인딩) ─────────────────────────────────────────────

/**
 * @description 단일 이벤트 prop에 대한 Action 바인딩 UI.
 * action/overlays가 바뀌지 않으면 리렌더링을 건너뜁니다.
 * onChange/onClear는 항상 최신 interactionRef를 통해 동작하므로 비교에서 제외합니다.
 */
const EventField = React.memo(function EventField({
  eventKey,
  label,
  action,
  overlays,
  onChange,
  onClear,
}: {
  eventKey: string;
  label: string;
  action?: Action;
  overlays: CMSOverlay[];
  /** eventKey를 첫 번째 인자로 받아 부모에서 직접 전달 가능 */
  onChange: (key: string, action: Action) => void;
  /** eventKey를 인자로 받아 부모에서 직접 전달 가능 */
  onClear: (key: string) => void;
}) {
  const actionType = action?.type ?? "none";

  // openOverlay target이 유효하지 않으면 첫 번째 오버레이로 보정
  useEffect(() => {
    if (
      action?.type === "openOverlay" &&
      overlays.length > 0 &&
      !overlays.some((o) => o.id === action.target)
    ) {
      onChange(eventKey, { type: "openOverlay", target: overlays[0].id });
    }
  }, [action, overlays, onChange, eventKey]);

  function handleTypeChange(type: string) {
    if (type === "none") { onClear(eventKey); return; }
    if (type === "openOverlay") onChange(eventKey, { type: "openOverlay", target: overlays[0]?.id ?? "" });
    else if (type === "closeOverlay") onChange(eventKey, { type: "closeOverlay" });
    else onChange(eventKey, { type: "navigate", path: "/" });
  }

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between">
        <label className="text-xs font-semibold text-text-secondary font-mono">{label !== eventKey ? `${eventKey} (${label})` : eventKey}</label>
        {action && (
          <button
            onClick={() => onClear(eventKey)}
            className="text-xs text-text-muted hover:text-red-400 transition-colors"
          >
            해제
          </button>
        )}
      </div>

      <select
        className={inputCls}
        value={actionType}
        onChange={(e) => handleTypeChange(e.target.value)}
      >
        <option value="none">— 없음 —</option>
        <option value="openOverlay">오버레이 열기</option>
        <option value="closeOverlay">오버레이 닫기</option>
        <option value="navigate">페이지 이동</option>
      </select>

      {actionType === "openOverlay" && (
        overlays.length === 0 ? (
          <p className="text-xs text-text-muted italic px-1">오버레이를 먼저 추가하세요</p>
        ) : (
          <select
            className={inputCls}
            value={(action as Extract<Action, { type: "openOverlay" }>).target}
            onChange={(e) => onChange(eventKey, { type: "openOverlay", target: e.target.value })}
          >
            {overlays.map((o) => (
              <option key={o.id} value={o.id}>{o.type}: {o.id}</option>
            ))}
          </select>
        )
      )}

      {actionType === "navigate" && (
        <input
          type="text"
          className={inputCls}
          placeholder="/accounts"
          value={(action as Extract<Action, { type: "navigate" }>).path}
          onChange={(e) => onChange(eventKey, { type: "navigate", path: e.target.value })}
        />
      )}
    </div>
  );
}, (prev, next) =>
  prev.eventKey === next.eventKey &&
  prev.label === next.label &&
  Object.is(prev.action, next.action) &&
  Object.is(prev.overlays, next.overlays),
);

// ── 메인 PropsEditor ───────────────────────────────────────────────────────────
export default function PropsEditor({
  block,
  onChange,
  onPaddingChange,
  blockMeta,
  overlays = [],
  onInteractionChange,
}: PropsEditorProps) {
  const meta = blockMeta[block.component];
  const props = block.props ?? {};
  const padding = block.padding ?? { top: 0, right: 0, bottom: 0, left: 0 };
  const interaction = block.interaction ?? {};

  // ref로 최신 값을 항상 유지 — useCallback deps 없이도 stale closure 방지
  const propsRef = useRef(props);
  propsRef.current = props;
  const paddingRef = useRef(padding);
  paddingRef.current = padding;
  const interactionRef = useRef(interaction);
  interactionRef.current = interaction;

  // onChange(props 부모 콜백)이 stable하면 set도 stable → FieldControl 불필요한 리렌더 방지
  const set = useCallback((key: string, value: unknown) => {
    onChange({ ...propsRef.current, [key]: value });
  }, [onChange]);

  // 그룹 필드: propsRef에서 최신 그룹 값을 읽어 stale 덮어쓰기 방지
  const setGroupField = useCallback((groupKey: string, subKey: string, value: unknown) => {
    const latestGroup = (propsRef.current[groupKey] as Record<string, unknown>) ?? {};
    onChange({ ...propsRef.current, [groupKey]: { ...latestGroup, [subKey]: value } });
  }, [onChange]);

  const setPadding = useCallback((side: keyof BlockPadding, value: number) => {
    onPaddingChange({ ...paddingRef.current, [side]: Math.max(0, value) });
  }, [onPaddingChange]);

  // 이벤트 콜백: interactionRef로 최신 interaction 읽기
  const handleEventChange = useCallback((key: string, action: Action) => {
    onInteractionChange?.({ ...interactionRef.current, [key]: action });
  }, [onInteractionChange]);

  const handleEventClear = useCallback((key: string) => {
    const { [key]: _removed, ...rest } = interactionRef.current;
    onInteractionChange?.(rest);
  }, [onInteractionChange]);

  const schema = meta?.propSchema ?? {};
  // event 타입은 별도 이벤트 섹션에서 처리
  const valueSchema = Object.fromEntries(
    Object.entries(schema).filter(([, f]) => f.type !== "event"),
  );
  const eventSchema = Object.entries(schema).filter(([, f]) => f.type === "event");
  const hasProps = Object.keys(valueSchema).length > 0;

  return (
    <div className="flex flex-col">
      {/* ── 블록 Props ── */}
      {hasProps ? (
        <div className="px-4 py-3 flex flex-col gap-3 border-b border-border">
          {Object.entries(valueSchema).map(([key, field]: [string, PropField]) => {
            // group
            if (field.type === "group") {
              const groupVal =
                (props[key] as Record<string, unknown>) ?? field.default;
              return (
                <div key={key} className="rounded-xl border border-border overflow-hidden">
                  <div className="px-3 py-1.5 bg-surface-hover border-b border-divider flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
                    <span className="text-xs font-bold text-text-secondary">
                      {field.label ?? key}
                    </span>
                  </div>
                  <div className="px-3 py-2.5 flex flex-col gap-3">
                    {Object.entries(field.fields).map(([subKey, subField]) => (
                      <FieldControl
                        key={subKey}
                        fieldKey={subKey}
                        field={subField}
                        value={groupVal[subKey] ?? subField.default}
                        // setGroupField는 stable — FieldControl 커스텀 memo comparator로 리렌더 스킵
                        onChange={(val) => setGroupField(key, subKey, val)}
                      />
                    ))}
                  </div>
                </div>
              );
            }

            // array
            if (field.type === "array") {
              const arrVal = (props[key] as Record<string, unknown>[]) ?? field.default;
              return (
                <ArrayField
                  key={key}
                  fieldKey={key}
                  field={field}
                  value={arrVal}
                  onChange={(next) => set(key, next)}
                  renderLeafField={(subKey, subField, val, handleChange) => (
                    <FieldControl
                      fieldKey={subKey}
                      field={subField}
                      value={val}
                      onChange={handleChange}
                    />
                  )}
                />
              );
            }

            // leaf (group / array / event 이후 남는 경우는 LeafPropField)
            const leafField = field as LeafPropField;
            return (
              <FieldControl
                key={key}
                fieldKey={key}
                field={leafField}
                value={props[key] ?? leafField.default}
                onChange={(val) => set(key, val)}
              />
            );
          })}
        </div>
      ) : (
        <div className="px-4 py-3 text-xs text-text-muted border-b border-border">
          {meta?.name ?? block.component} — 편집 가능한 속성이 없습니다.
        </div>
      )}

      {/* ── 이벤트 ── */}
      {eventSchema.length > 0 && onInteractionChange && (
        <div className="px-4 py-3 flex flex-col gap-3 border-b border-border">
          <p className="text-xs font-bold text-text-secondary uppercase tracking-wide">이벤트</p>
          {eventSchema.map(([key, field]) => {
            const label = field.type === "event" ? (field.label ?? key) : key;
            return (
              <EventField
                key={key}
                eventKey={key}
                label={label}
                action={interaction[key]}
                overlays={overlays}
                // handleEventChange/handleEventClear가 (key, ...) 시그니처와 일치 — 직접 전달
                onChange={handleEventChange}
                onClear={handleEventClear}
              />
            );
          })}
        </div>
      )}

      {/* ── 패딩 편집 ── */}
      <div className="px-4 py-3 flex flex-col gap-2.5">
        <p className="text-xs font-bold text-text-secondary uppercase tracking-wide">패딩 (px)</p>
        <div className="grid grid-cols-2 gap-2">
          {(["top", "bottom", "left", "right"] as const).map((side) => (
            <div key={side} className="flex flex-col gap-1">
              <label className="text-xs text-text-muted">
                {{ top: "위", bottom: "아래", left: "왼쪽", right: "오른쪽" }[side]}
              </label>
              <input
                type="number"
                min={0}
                value={padding[side]}
                // side는 루프 내 상수 — setPadding은 stable useCallback
                onChange={(e) => setPadding(side, Number(e.target.value))}
                className={inputCls}
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
