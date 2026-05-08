/**
 * @file FieldControl.tsx
 * @description CMS 인스펙터 단일 leaf 필드 컨트롤.
 * string / number / boolean / select / icon-picker 타입을 지원한다.
 * label을 내부에 포함하며, PropsEditor / OverlayEditor 양쪽에서 공용으로 사용한다.
 *
 * @param fieldKey - prop 키 (label 미지정 시 fallback으로 사용)
 * @param field    - LeafPropField 스키마
 * @param value    - 현재 값
 * @param onChange - 값 변경 핸들러
 */
import React, { useState } from "react";
import { ChevronDown } from "lucide-react";
import type { LeafPropField } from "../types";
import IconPicker from "./IconPicker";
import { resolveIcon } from "../utils/icon";

export interface FieldControlProps {
  fieldKey: string;
  field: LeafPropField;
  value: unknown;
  onChange: (val: unknown) => void;
}

const inputCls =
  "w-full h-8 px-3 rounded-lg border border-input-border bg-surface text-xs text-input-text outline-none focus:border-input-border-focus transition-colors";

/**
 * onChange는 매 렌더마다 새 참조가 생성될 수 있으므로 커스텀 비교 함수로 제외.
 * value / field / fieldKey 가 동일하면 리렌더링을 건너뜁니다.
 * set 함수는 항상 최신 props를 propsRef를 통해 읽으므로 stale closure 걱정 없음.
 */
export const FieldControl = React.memo(function FieldControl({
  fieldKey,
  field,
  value,
  onChange,
}: FieldControlProps) {
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs font-semibold text-text-secondary">
        {field.label ?? fieldKey}
      </label>

      {field.type === "string" && (
        <input
          className={inputCls}
          value={value as string}
          onChange={(e) => onChange(e.target.value)}
        />
      )}

      {field.type === "number" && (
        <input
          type="number"
          className={inputCls}
          value={value as number}
          onChange={(e) => onChange(Number(e.target.value))}
        />
      )}

      {field.type === "boolean" && (
        <button
          type="button"
          onClick={() => onChange(!value)}
          className={`relative w-9 h-5 rounded-full transition-colors border-none self-start ${
            value ? "bg-primary" : "bg-border"
          }`}
        >
          <span
            className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow-card transition-all ${
              value ? "left-4" : "left-0.5"
            }`}
          />
        </button>
      )}

      {field.type === "select" && (
        <div className="flex flex-wrap gap-1.5">
          {field.options?.map((opt) => (
            <button
              key={opt}
              type="button"
              onClick={() => onChange(opt)}
              className={`px-2.5 py-1 rounded-lg text-xs transition-colors border ${
                value === opt
                  ? "bg-primary/10 border-primary text-primary font-semibold"
                  : "bg-surface border-border text-text-secondary hover:bg-surface-hover"
              }`}
            >
              {opt}
            </button>
          ))}
        </div>
      )}

      {field.type === "icon-picker" && (
        <div className="flex flex-col gap-1.5">
          <button
            type="button"
            onClick={() => setPickerOpen((o) => !o)}
            className="flex items-center gap-2 h-8 px-3 rounded-lg border border-input-border bg-surface text-xs text-input-text hover:border-input-border-focus transition-colors"
          >
            {value
              ? resolveIcon(value as string, "w-4 h-4 text-primary shrink-0")
              : <span className="w-4 h-4 rounded bg-border shrink-0" />}
            <span className={`flex-1 text-left ${value ? "text-text-primary" : "text-text-muted"}`}>
              {value ? (value as string) : "아이콘 선택..."}
            </span>
            <ChevronDown
              className={`w-3 h-3 text-text-muted shrink-0 transition-transform ${pickerOpen ? "rotate-180" : ""}`}
            />
          </button>

          {pickerOpen && (
            <IconPicker
              value={value as string}
              onSelect={(name) => {
                onChange(name);
                setPickerOpen(false);
              }}
            />
          )}
        </div>
      )}
    </div>
  );
}, (prev, next) =>
  // onChange는 stale closure 없이 항상 최신 set을 호출하므로 비교에서 제외
  prev.fieldKey === next.fieldKey &&
  prev.value === next.value &&
  Object.is(prev.field, next.field),
);
