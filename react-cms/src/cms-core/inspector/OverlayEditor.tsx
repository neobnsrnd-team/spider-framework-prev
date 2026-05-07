/**
 * @file OverlayEditor.tsx
 * @description 오버레이 props 편집기.
 * OverlayTemplate의 propSchema 또는 기본값 타입 추론으로 동적 폼을 생성합니다.
 * group / event 타입은 미지원이며, array / leaf 타입을 처리합니다.
 */
import type { CMSOverlay, LeafPropField, PropField, OverlayTemplate } from "../types";
import { FieldControl } from "./FieldControl";
import { ArrayField } from "./ArrayField";

interface OverlayEditorProps {
  overlay: CMSOverlay;
  template: OverlayTemplate | undefined;
  onChange: (props: Record<string, unknown>) => void;
}

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
        <div className="w-10 h-10 rounded-xl bg-surface-raised flex items-center justify-center text-lg">↖</div>
        <p className="text-sm text-text-muted">이 오버레이는 편집 가능한 속성이 없습니다</p>
      </div>
    );
  }

  return (
    <div className="p-4 flex flex-col gap-3">
      <p className="text-xs font-bold text-text-secondary uppercase tracking-wide">
        {template?.label ?? overlay.type} 속성
      </p>
      {entries.map(([key, field]) => {
        // array 타입
        if (field.type === "array") {
          const arrVal = (current[key] as Record<string, unknown>[]) ?? field.default;
          return (
            <ArrayField
              key={key}
              fieldKey={key}
              field={field}
              value={arrVal}
              onChange={(next) => onChange({ ...current, [key]: next })}
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

        // leaf 타입
        const leafField = field as LeafPropField;
        return (
          <FieldControl
            key={key}
            fieldKey={key}
            field={leafField}
            value={current[key] ?? leafField.default}
            onChange={(val) => onChange({ ...current, [key]: val })}
          />
        );
      })}
    </div>
  );
}
