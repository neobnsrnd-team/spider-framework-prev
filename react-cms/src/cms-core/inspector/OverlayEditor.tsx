/**
 * @file OverlayEditor.tsx
 * @description 오버레이 props 편집기.
 * OverlayTemplate의 propSchema 또는 기본값 타입 추론으로 동적 폼을 생성합니다.
 */
import { useState } from "react";
import { ChevronDown, Plus, Trash2 } from "lucide-react";
import type { CMSOverlay, LeafPropField, NestedArrayPropField, PropField, OverlayTemplate } from "../types";
import IconPicker, { renderLucideIcon } from "./IconPicker";

interface OverlayEditorProps {
  overlay: CMSOverlay;
  template: OverlayTemplate | undefined;
  onChange: (props: Record<string, unknown>) => void;
}

const fieldInputCls =
  "w-full h-8 px-3 rounded-lg border border-gray-200 bg-white text-xs text-gray-700 outline-none focus:border-primary/60 transition-colors";

/**
 * @description 오버레이 props 편집기.
 * template.propSchema가 정의되어 있으면 해당 스키마로 동적 폼을 생성하고,
 * 없으면 template.props 값의 타입에서 자동 추론합니다.
 * group / array / event 이외의 LeafPropField와 array 타입을 지원합니다.
 */
export default function OverlayEditor({ overlay, template, onChange }: OverlayEditorProps) {
  const current = overlay.props ?? {};

  // propSchema가 있으면 event/group 제외 후 사용, 없으면 template.props에서 leaf 타입 추론
  const entries: [string, PropField][] = template?.propSchema
    ? Object.entries(template.propSchema).filter(
        ([, f]) => f.type !== "group" && f.type !== "event",
      )
    : Object.entries(template?.props ?? {})
        // 배열·객체는 추론 불가 — propSchema 없이는 편집 필드 생성 생략
        .filter(([, v]) => typeof v === "string" || typeof v === "boolean" || typeof v === "number")
        .map(([k, v]): [string, PropField] => [
          k,
          {
            type:
              typeof v === "boolean" ? "boolean"
              : typeof v === "number"  ? "number"
              : "string",
            label: k,
            default: v as string | boolean | number,
          },
        ]);

  if (entries.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full min-h-48 text-center gap-2 p-4">
        <div className="w-10 h-10 rounded-xl bg-gray-100 flex items-center justify-center text-lg">↖</div>
        <p className="text-sm text-gray-400">이 오버레이는 편집 가능한 속성이 없습니다</p>
      </div>
    );
  }

  return (
    <div className="p-4 flex flex-col gap-3">
      <p className="text-xs font-bold text-gray-500 uppercase tracking-wide">
        {template?.label ?? overlay.type} 속성
      </p>
      {entries.map(([key, field]) => {
        // array 타입
        if (field.type === "array") {
          const arrVal = (current[key] as Record<string, unknown>[]) ?? field.default;
          const newItem = Object.fromEntries(
            Object.entries(field.itemFields).map(([k, f]) => [k, f.default]),
          );
          return (
            <div key={key} className="rounded-xl border border-gray-200 overflow-hidden">
              <div className="px-3 py-1.5 bg-gray-50 border-b border-gray-200 flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
                <span className="text-xs font-bold text-gray-500 flex-1">{field.label ?? key}</span>
                <span className="text-xs text-gray-400 mr-1">{arrVal.length}개</span>
                <button
                  type="button"
                  onClick={() => onChange({ ...current, [key]: [...arrVal, newItem] })}
                  className="flex items-center gap-0.5 text-xs text-primary font-semibold hover:opacity-70 bg-transparent border-none"
                >
                  <Plus className="w-3 h-3" />
                  추가
                </button>
              </div>
              <div className="flex flex-col">
                {arrVal.length === 0 && (
                  <p className="px-3 py-2.5 text-xs text-gray-400">항목이 없습니다.</p>
                )}
                {arrVal.map((item, idx) => (
                  <div
                    key={idx}
                    className={`px-3 py-2.5 flex flex-col gap-2 ${idx > 0 ? "border-t border-gray-100" : ""}`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-gray-400">#{idx + 1}</span>
                      <button
                        type="button"
                        onClick={() => onChange({ ...current, [key]: arrVal.filter((_, i) => i !== idx) })}
                        className="flex items-center gap-0.5 text-xs text-red-400 hover:opacity-70 bg-transparent border-none"
                      >
                        <Trash2 className="w-3 h-3" />
                        삭제
                      </button>
                    </div>
                    {Object.entries(field.itemFields).map(([subKey, subField]) => {
                      // 중첩 배열 필드
                      if (subField.type === "array") {
                        const nested = subField as NestedArrayPropField;
                        const nestedArr = (item[subKey] as Record<string, unknown>[]) ?? nested.default;
                        const newSubItem = Object.fromEntries(
                          Object.entries(nested.itemFields).map(([k, f]) => [k, f.default]),
                        );
                        const updateNested = (newNested: Record<string, unknown>[]) =>
                          onChange({
                            ...current,
                            [key]: arrVal.map((it, i) => i === idx ? { ...it, [subKey]: newNested } : it),
                          });
                        return (
                          <div key={subKey} className="rounded-lg border border-gray-200 overflow-hidden">
                            <div className="px-2.5 py-1 bg-gray-50 border-b border-gray-200 flex items-center gap-1.5">
                              <span className="text-xs font-semibold text-gray-500 flex-1">{nested.label ?? subKey}</span>
                              <span className="text-xs text-gray-400 mr-1">{nestedArr.length}개</span>
                              <button
                                type="button"
                                onClick={() => updateNested([...nestedArr, newSubItem])}
                                className="flex items-center gap-0.5 text-xs text-primary font-semibold hover:opacity-70 bg-transparent border-none"
                              >
                                <Plus className="w-3 h-3" />
                                추가
                              </button>
                            </div>
                            <div className="flex flex-col">
                              {nestedArr.length === 0 && (
                                <p className="px-2.5 py-2 text-xs text-gray-400">항목이 없습니다.</p>
                              )}
                              {nestedArr.map((subItem, subIdx) => (
                                <div
                                  key={subIdx}
                                  className={`px-2.5 py-2 flex flex-col gap-1.5 ${subIdx > 0 ? "border-t border-gray-100" : ""}`}
                                >
                                  <div className="flex items-center justify-between">
                                    <span className="text-xs text-gray-400">#{subIdx + 1}</span>
                                    <button
                                      type="button"
                                      onClick={() => updateNested(nestedArr.filter((_, si) => si !== subIdx))}
                                      className="flex items-center gap-0.5 text-xs text-red-400 hover:opacity-70 bg-transparent border-none"
                                    >
                                      <Trash2 className="w-3 h-3" />
                                      삭제
                                    </button>
                                  </div>
                                  {Object.entries(nested.itemFields).map(([leafKey, leafField]) => (
                                    <div key={leafKey} className="flex flex-col gap-1">
                                      <label className="text-xs font-semibold text-gray-600">{leafField.label ?? leafKey}</label>
                                      <OverlayFieldControl
                                        field={leafField}
                                        value={(subItem[leafKey] ?? leafField.default) as unknown}
                                        onChange={(val) => updateNested(
                                          nestedArr.map((si, i) => i === subIdx ? { ...si, [leafKey]: val } : si),
                                        )}
                                      />
                                    </div>
                                  ))}
                                </div>
                              ))}
                            </div>
                          </div>
                        );
                      }
                      // leaf 필드
                      return (
                        <div key={subKey} className="flex flex-col gap-1">
                          <label className="text-xs font-semibold text-gray-600">{subField.label ?? subKey}</label>
                          <OverlayFieldControl
                            field={subField as LeafPropField}
                            value={(item[subKey] ?? subField.default) as unknown}
                            onChange={(val) =>
                              onChange({
                                ...current,
                                [key]: arrVal.map((it, i) => i === idx ? { ...it, [subKey]: val } : it),
                              })
                            }
                          />
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          );
        }

        // leaf 타입
        const leafField = field as LeafPropField;
        return (
          <div key={key} className="flex flex-col gap-1">
            <label className="text-xs font-semibold text-gray-600">{leafField.label ?? key}</label>
            <OverlayFieldControl
              field={leafField}
              value={current[key] ?? leafField.default}
              onChange={(val) => onChange({ ...current, [key]: val })}
            />
          </div>
        );
      })}
    </div>
  );
}

/**
 * @description 오버레이 단일 필드 컨트롤 (string / number / boolean / select / icon-picker).
 */
function OverlayFieldControl({
  field,
  value,
  onChange,
}: {
  field: LeafPropField;
  value: unknown;
  onChange: (val: unknown) => void;
}) {
  const [pickerOpen, setPickerOpen] = useState(false);

  if (field.type === "icon-picker") {
    return (
      <div className="flex flex-col gap-1.5">
        <button
          type="button"
          onClick={() => setPickerOpen((o) => !o)}
          className="flex items-center gap-2 h-8 px-3 rounded-lg border border-gray-200 bg-white text-xs text-gray-700 hover:border-gray-300 transition-colors w-full"
        >
          {value
            ? renderLucideIcon(value as string, "w-4 h-4 text-primary shrink-0")
            : <span className="w-4 h-4 rounded bg-gray-200 shrink-0" />}
          <span className={`flex-1 text-left ${value ? "text-gray-700" : "text-gray-400"}`}>
            {value ? (value as string) : "아이콘 선택..."}
          </span>
          <ChevronDown
            className={`w-3 h-3 text-gray-400 shrink-0 transition-transform ${pickerOpen ? "rotate-180" : ""}`}
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
    );
  }

  if (field.type === "string") {
    return (
      <input
        className={fieldInputCls}
        value={(value as string) ?? ""}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }
  if (field.type === "number") {
    return (
      <input
        type="number"
        className={fieldInputCls}
        value={(value as number) ?? 0}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    );
  }
  if (field.type === "boolean") {
    return (
      <button
        type="button"
        onClick={() => onChange(!value)}
        className={`relative w-9 h-5 rounded-full transition-colors border-none self-start ${
          value ? "bg-primary" : "bg-border"
        }`}
      >
        <span
          className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-all ${
            value ? "left-4" : "left-0.5"
          }`}
        />
      </button>
    );
  }
  if (field.type === "select") {
    return (
      <div className="flex flex-wrap gap-1.5">
        {field.options?.map((opt) => (
          <button
            key={opt}
            type="button"
            onClick={() => onChange(opt)}
            className={`px-2.5 py-1 text-xs rounded-lg border transition-colors ${
              value === opt
                ? "bg-primary text-white border-primary"
                : "bg-white text-gray-600 border-gray-200 hover:border-primary/60"
            }`}
          >
            {opt}
          </button>
        ))}
      </div>
    );
  }
  return null;
}
