/**
 * @file LayoutEditor.tsx
 * @description 레이아웃 편집기.
 * LayoutTemplatesContext에서 레이아웃 목록을 읽어 동적으로 타입 선택 + props 폼을 생성합니다.
 * PropsEditor와 동일한 propSchema 기반 필드 렌더링 패턴(공용 FieldControl·ArrayField)을 사용합니다.
 */
import { useContext } from "react";
import type { LeafPropField, PropField } from "../types";
import { LayoutTemplatesContext } from "../context";
import { FieldControl } from "./FieldControl";
import { ArrayField } from "./ArrayField";

// ─── 블록 간격 옵션 ──────────────────────────────────────────
const GAP_OPTIONS = [
  { value: "none", label: "없음 (0px)" },
  { value: "xs",   label: "xs (4px)" },
  { value: "sm",   label: "sm (8px)" },
  { value: "md",   label: "md (16px)" },
  { value: "lg",   label: "lg (24px)" },
  { value: "xl",   label: "xl (32px)" },
];

// ─── 메인 컴포넌트 ────────────────────────────────────────────

interface LayoutEditorProps {
  layoutType: string | undefined;
  layoutProps: Record<string, unknown>;
  onLayoutTypeChange: (type: string | undefined, defaultProps?: Record<string, unknown>) => void;
  onLayoutPropsChange: (props: Record<string, unknown>) => void;
}

/**
 * @description 레이아웃 타입 선택 + 동적 props 편집기.
 * @param layoutType 현재 선택된 레이아웃 타입 ID
 * @param layoutProps 현재 레이아웃 props
 * @param onLayoutTypeChange 레이아웃 타입 변경 핸들러
 * @param onLayoutPropsChange 레이아웃 props 변경 핸들러
 */
export default function LayoutEditor({
  layoutType,
  layoutProps,
  onLayoutTypeChange,
  onLayoutPropsChange,
}: LayoutEditorProps) {
  const layouts = useContext(LayoutTemplatesContext);
  const currentTemplate = layouts.find((t) => t.id === layoutType);

  // propSchema 우선, 없으면 defaultProps에서 타입 자동 추론
  // group/event는 레이아웃 컨텍스트에서 사용하지 않으므로 제외, array는 ArrayField로 렌더
  const entries: [string, PropField][] = currentTemplate?.propSchema
    ? (Object.entries(currentTemplate.propSchema).filter(
        ([, f]) => f.type !== "group" && f.type !== "event",
      ) as [string, PropField][])
    : Object.entries(currentTemplate?.defaultProps ?? {})
        // 객체·배열은 추론 불가 — propSchema 없이는 편집 필드 생성 생략
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
          } as LeafPropField,
        ]);

  return (
    <div className="flex flex-col gap-5 p-4">
      {/* ── 블록 간격 (항상 표시) ── */}
      <section className="flex flex-col gap-3">
        <SectionLabel>블록 간격</SectionLabel>
        <Field label="블록 사이 간격">
          <select
            value={(layoutProps.blockGap as string | undefined) ?? "none"}
            onChange={(e) =>
              onLayoutPropsChange({ ...layoutProps, blockGap: e.target.value })
            }
            className={inputCls}
          >
            {GAP_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </Field>
      </section>

      {/* ── 레이아웃 타입 선택 ── */}
      <section className="flex flex-col gap-2">
        <SectionLabel>레이아웃 타입</SectionLabel>
        <div className="flex flex-col gap-1.5">
          {/* 없음 */}
          <LayoutOption
            label="없음"
            desc="블록만 렌더링"
            active={!layoutType}
            onClick={() => onLayoutTypeChange(undefined)}
          />
          {layouts.map((tpl) => (
            <LayoutOption
              key={tpl.id}
              label={tpl.label}
              desc={tpl.description}
              active={layoutType === tpl.id}
              onClick={() => onLayoutTypeChange(tpl.id, tpl.defaultProps)}
            />
          ))}
        </div>
      </section>

      {/* ── 동적 props 편집 ── */}
      {currentTemplate && entries.length > 0 && (
        <section className="flex flex-col gap-3">
          <SectionLabel>{currentTemplate.label} 설정</SectionLabel>
          {entries.map(([key, field]) => {
            // array: ArrayField로 렌더 (PropsEditor와 동일 패턴)
            if (field.type === "array") {
              const arrVal = (layoutProps[key] as Record<string, unknown>[]) ?? field.default ?? [];
              return (
                <ArrayField
                  key={key}
                  fieldKey={key}
                  field={field}
                  value={arrVal}
                  onChange={(next) => onLayoutPropsChange({ ...layoutProps, [key]: next })}
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

            // leaf: FieldControl로 렌더
            const leafField = field as LeafPropField;
            return (
              <FieldControl
                key={key}
                fieldKey={key}
                field={leafField}
                value={layoutProps[key] ?? leafField.default}
                onChange={(val) => onLayoutPropsChange({ ...layoutProps, [key]: val })}
              />
            );
          })}
        </section>
      )}

      {currentTemplate && entries.length === 0 && (
        <InfoBox>이 레이아웃은 편집 가능한 설정이 없습니다.</InfoBox>
      )}
    </div>
  );
}

// ─── 레이아웃 옵션 버튼 ──────────────────────────────────────

function LayoutOption({
  label,
  desc,
  active,
  onClick,
}: {
  label: string;
  desc?: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-start gap-2.5 px-3 py-2.5 rounded-lg border text-left transition-colors ${
        active
          ? "border-primary bg-primary-50"
          : "border-gray-200 hover:border-gray-300 hover:bg-gray-50"
      }`}
    >
      <span
        className={`mt-0.5 w-3.5 h-3.5 rounded-full border-2 flex-shrink-0 ${
          active ? "border-primary bg-primary" : "border-gray-400"
        }`}
      />
      <div>
        <p className={`text-sm font-medium ${active ? "text-primary" : "text-gray-700"}`}>
          {label}
        </p>
        {desc && <p className="text-xs text-gray-400 mt-0.5">{desc}</p>}
      </div>
    </button>
  );
}

// ─── 공통 서브 컴포넌트 ──────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
  return <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{children}</p>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs text-gray-500">{label}</label>
      {children}
    </div>
  );
}

function InfoBox({ children }: { children: React.ReactNode }) {
  return (
    <div className="px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-500">
      {children}
    </div>
  );
}

const inputCls =
  "border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 w-full";
