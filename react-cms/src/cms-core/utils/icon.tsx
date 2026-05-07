/**
 * @file icon.tsx
 * @description Lucide 아이콘 유틸리티.
 * IconPicker UI와 resolveIcon이 동일한 아이콘 셋을 공유하도록 단일 source로 관리한다.
 *
 * lucide-react 전체 아이콘을 모듈 초기화 시 PascalCase → kebab-case 맵으로 빌드한다.
 * PascalCase 역변환 방식("AArrowDown" → "a-arrow-down" 복원 불가)을 쓰지 않고
 * 정방향 변환 맵을 직접 구축하여 연속 대문자 아이콘도 정확히 조회한다.
 *
 * 필터 정책 — IconPicker / resolveIcon 양쪽이 일치해야 한다:
 *  - 첫 글자 대문자 (export 컨벤션)
 *  - $$typeof 보유 (forwardRef 컴포넌트)
 *  - displayName 문자열 보유 (lucide의 정식 컴포넌트 export 식별)
 */
import React from "react";
import * as LucideIcons from "lucide-react";

type LucideComponent = React.ComponentType<React.SVGProps<SVGSVGElement>>;

/** lucide-react export 중 정식 컴포넌트만 식별 */
function isLucideComponent(name: string, val: unknown): val is LucideComponent {
  return (
    /^[A-Z]/.test(name) &&
    typeof val === "object" &&
    val !== null &&
    "$$typeof" in (val as object) &&
    typeof (val as Record<string, unknown>).displayName === "string"
  );
}

/** PascalCase → kebab-case 변환 (예: "ChevronRight" → "chevron-right") */
export function toKebabIcon(name: string): string {
  return name.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase();
}

/** kebab-case → PascalCase 변환 (예: "chevron-right" → "ChevronRight") */
export function kebabToPascal(name: string): string {
  return name.split("-").map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join("");
}

const LUCIDE_ENTRIES: ReadonlyArray<[string, LucideComponent]> =
  (Object.entries(LucideIcons) as [string, unknown][])
    .filter(([n, v]) => isLucideComponent(n, v))
    .map(([n, v]) => [n, v as LucideComponent]);

/** IconPicker UI에서 사용하는 PascalCase 이름 목록 (resolveIcon과 동일 필터로 빌드) */
export const ALL_ICON_NAMES: readonly string[] = LUCIDE_ENTRIES.map(([n]) => n);

const lucideIconMap: Record<string, LucideComponent> = Object.fromEntries(
  LUCIDE_ENTRIES.map(([name, val]) => [toKebabIcon(name), val]),
);

/**
 * kebab-case 아이콘 이름을 Lucide ReactNode로 변환.
 * @param name     kebab-case 아이콘 이름 (예: "chevron-right", "a-arrow-down")
 * @param className 아이콘 엘리먼트에 적용할 CSS 클래스 (기본값: "size-5")
 * @returns 매칭된 아이콘 ReactNode, 이름이 비어있거나 맵에 없으면 null
 */
export function resolveIcon(name: string, className = "size-5"): React.ReactNode {
  if (!name) return null;
  const Icon = lucideIconMap[name];
  return Icon ? <Icon className={className} /> : null;
}
