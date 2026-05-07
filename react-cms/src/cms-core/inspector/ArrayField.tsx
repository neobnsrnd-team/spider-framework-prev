/**
 * @file ArrayField.tsx
 * @description CMS 인스펙터 공용 배열 필드 컴포넌트.
 * ArrayPropField의 외곽 구조(헤더·추가 버튼·아이템 행·삭제 버튼)와
 * NestedArrayPropField 2단계 중첩을 처리한다.
 * leaf 필드 렌더링은 renderLeafField prop으로 주입받아
 * PropsEditor / OverlayEditor 양쪽에서 재사용한다.
 *
 * @param fieldKey       - prop 키 (label 미지정 시 fallback)
 * @param field          - ArrayPropField 스키마
 * @param value          - 현재 배열 값
 * @param onChange       - 배열 전체 교체 핸들러
 * @param renderLeafField - leaf 필드 렌더러 주입 (각 Editor가 자신의 컨트롤을 전달)
 */
import React from "react";
import { Plus, Trash2 } from "lucide-react";
import type { ArrayPropField, LeafPropField, NestedArrayPropField } from "../types";

export interface ArrayFieldProps {
  fieldKey: string;
  field: ArrayPropField;
  value: Record<string, unknown>[];
  onChange: (next: Record<string, unknown>[]) => void;
  renderLeafField: (
    key: string,
    field: LeafPropField,
    value: unknown,
    onChange: (val: unknown) => void,
  ) => React.ReactNode;
}

/**
 * 배열 아이템 내 중첩 배열 렌더러.
 * NestedArrayPropField는 2단계 제한으로 itemFields가 leaf만 허용되므로
 * renderLeafField를 그대로 위임한다.
 */
function NestedArrayField({
  fieldKey,
  field,
  value,
  onChange,
  renderLeafField,
}: {
  fieldKey: string;
  field: NestedArrayPropField;
  value: Record<string, unknown>[];
  onChange: (next: Record<string, unknown>[]) => void;
  renderLeafField: ArrayFieldProps["renderLeafField"];
}) {
  const newItem = Object.fromEntries(
    Object.entries(field.itemFields).map(([k, f]) => [k, f.default]),
  );

  return (
    <div className="rounded-lg border border-divider overflow-hidden">
      <div className="px-2.5 py-1 bg-surface-hover border-b border-divider flex items-center gap-1.5">
        <span className="text-xs font-semibold text-text-muted flex-1">{field.label ?? fieldKey}</span>
        <span className="text-xs text-text-muted mr-1">{value.length}개</span>
        <button
          type="button"
          onClick={() => onChange([...value, newItem])}
          className="flex items-center gap-0.5 text-xs text-primary font-semibold hover:opacity-70 bg-transparent border-none"
        >
          <Plus className="w-3 h-3" />
          추가
        </button>
      </div>
      <div className="flex flex-col">
        {value.length === 0 && (
          <p className="px-2.5 py-2 text-xs text-text-muted">항목이 없습니다.</p>
        )}
        {value.map((item, idx) => (
          <div
            key={idx}
            className={`px-2.5 py-2 flex flex-col gap-1.5 ${idx > 0 ? "border-t border-divider" : ""}`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs text-text-muted">#{idx + 1}</span>
              <button
                type="button"
                onClick={() => onChange(value.filter((_, i) => i !== idx))}
                className="flex items-center gap-0.5 text-xs text-error hover:opacity-70 bg-transparent border-none"
              >
                <Trash2 className="w-3 h-3" />
                삭제
              </button>
            </div>
            {Object.entries(field.itemFields).map(([leafKey, leafField]) => (
              <React.Fragment key={leafKey}>
                {renderLeafField(
                  leafKey,
                  leafField,
                  item[leafKey] ?? leafField.default,
                  (val) => onChange(value.map((it, i) => i === idx ? { ...it, [leafKey]: val } : it)),
                )}
              </React.Fragment>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

export function ArrayField({ fieldKey, field, value, onChange, renderLeafField }: ArrayFieldProps) {
  const newItem = Object.fromEntries(
    Object.entries(field.itemFields).map(([k, f]) => [k, f.default]),
  );

  return (
    <div className="rounded-xl border border-border overflow-hidden">
      <div className="px-3 py-1.5 bg-surface-hover border-b border-divider flex items-center gap-1.5">
        <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
        <span className="text-xs font-bold text-text-secondary flex-1">{field.label ?? fieldKey}</span>
        <span className="text-xs text-text-muted mr-1">{value.length}개</span>
        <button
          type="button"
          onClick={() => onChange([...value, newItem])}
          className="flex items-center gap-0.5 text-xs text-primary font-semibold hover:opacity-70 bg-transparent border-none"
        >
          <Plus className="w-3 h-3" />
          추가
        </button>
      </div>
      <div className="flex flex-col">
        {value.length === 0 && (
          <p className="px-3 py-2.5 text-xs text-text-muted">항목이 없습니다.</p>
        )}
        {value.map((item, idx) => (
          <div
            key={idx}
            className={`px-3 py-2.5 flex flex-col gap-2 ${idx > 0 ? "border-t border-divider" : ""}`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-text-muted">#{idx + 1}</span>
              <button
                type="button"
                onClick={() => onChange(value.filter((_, i) => i !== idx))}
                className="flex items-center gap-0.5 text-xs text-error hover:opacity-70 bg-transparent border-none"
              >
                <Trash2 className="w-3 h-3" />
                삭제
              </button>
            </div>
            {Object.entries(field.itemFields).map(([subKey, subField]) => {
              if (subField.type === "array") {
                return (
                  <NestedArrayField
                    key={subKey}
                    fieldKey={subKey}
                    field={subField as NestedArrayPropField}
                    value={(item[subKey] as Record<string, unknown>[]) ?? subField.default}
                    onChange={(next) => onChange(value.map((it, i) => i === idx ? { ...it, [subKey]: next } : it))}
                    renderLeafField={renderLeafField}
                  />
                );
              }
              return (
                <React.Fragment key={subKey}>
                  {renderLeafField(
                    subKey,
                    subField as LeafPropField,
                    item[subKey] ?? subField.default,
                    (val) => onChange(value.map((it, i) => i === idx ? { ...it, [subKey]: val } : it)),
                  )}
                </React.Fragment>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
