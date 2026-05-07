/**
 * @file LayoutEditor.tsx
 * @description 레이아웃 편집기.
 * LayoutTemplatesContext에서 레이아웃 목록을 읽어 동적으로 타입 선택 + props 폼을 생성합니다.
 * OverlayPropsEditor와 동일한 propSchema 기반 필드 렌더링 패턴을 사용합니다.
 */
import { useContext, useState } from "react";
import { ChevronDown } from "lucide-react";
import type { LeafPropField } from "../types";
import { LayoutTemplatesContext } from "../context";
import IconPicker from "./IconPicker";
import { resolveIcon } from "../utils/icon";

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
  const entries: [string, LeafPropField][] = currentTemplate?.propSchema
    ? (Object.entries(currentTemplate.propSchema).filter(
        ([, f]) => f.type !== "group" && f.type !== "array" && f.type !== "event",
      ) as [string, LeafPropField][])
    : Object.entries(currentTemplate?.defaultProps ?? {})
        // 배열·객체는 추론 불가 — propSchema 없이는 편집 필드 생성 생략
        .filter(([, v]) => typeof v === "string" || typeof v === "boolean" || typeof v === "number")
        .map(([k, v]): [string, LeafPropField] => [
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
          {entries.map(([key, field]) => (
            <Field key={key} label={field.label ?? key}>
              <LayoutFieldControl
                field={field}
                value={layoutProps[key] ?? field.default}
                onChange={(val) => onLayoutPropsChange({ ...layoutProps, [key]: val })}
              />
            </Field>
          ))}
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

// ─── 필드 컨트롤 ─────────────────────────────────────────────

/**
 * @description 레이아웃 단일 필드 컨트롤 (string / number / boolean / select / icon-picker).
 */
function LayoutFieldControl({
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
            ? resolveIcon(value as string, "w-4 h-4 text-primary shrink-0")
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
        className={inputCls}
        value={(value as string) ?? ""}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }
  if (field.type === "number") {
    return (
      <input
        type="number"
        className={inputCls}
        value={(value as number) ?? 0}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    );
  }
  if (field.type === "boolean") {
    return (
      <label className="flex items-center gap-2 cursor-pointer self-start">
        <input
          type="checkbox"
          checked={!!value}
          onChange={(e) => onChange(e.target.checked)}
          className="w-4 h-4 rounded accent-primary"
        />
      </label>
    );
  }
  if (field.type === "select") {
    return (
      <select
        value={(value as string) ?? ""}
        onChange={(e) => onChange(e.target.value)}
        className={inputCls}
      >
        {field.options?.map((opt) => (
          <option key={opt} value={opt}>{opt}</option>
        ))}
      </select>
    );
  }
  return null;
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
