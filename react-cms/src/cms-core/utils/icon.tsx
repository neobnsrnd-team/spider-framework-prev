/**
 * @file icon.tsx
 * @description Lucide 아이콘 유틸리티.
 * kebab-case 아이콘 이름 → Lucide ReactNode 변환.
 *
 * lucide-react 전체 아이콘을 모듈 초기화 시 PascalCase → kebab-case 맵으로 빌드한다.
 * PascalCase 역변환 방식("AArrowDown" → "a-arrow-down" 복원 불가)을 쓰지 않고
 * 정방향 변환 맵을 직접 구축하여 연속 대문자 아이콘도 정확히 조회한다.
 *
 * @param name     kebab-case 아이콘 이름 (예: "chevron-right", "a-arrow-down")
 * @param className 아이콘 엘리먼트에 적용할 CSS 클래스 (기본값: "size-5")
 * @returns Lucide 아이콘 ReactNode, 이름이 없거나 맵에 없으면 null
 */
import React from "react";
import * as LucideIcons from "lucide-react";

// lucide-react 아이콘은 React.forwardRef() 반환값이므로 typeof가 "object".
// $$typeof 보유 여부로 컴포넌트 여부를 판별한다.
const lucideIconMap: Record<string, React.ComponentType<React.SVGProps<SVGSVGElement>>> =
  Object.fromEntries(
    (Object.entries(LucideIcons) as [string, unknown][])
      .filter(([name, val]) =>
        /^[A-Z]/.test(name) &&
        typeof val === "object" &&
        val !== null &&
        "$$typeof" in (val as object),
      )
      .map(([name, val]) => [
        name.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase(),
        val,
      ]),
  );

export function resolveIcon(name: string, className = "size-5"): React.ReactNode {
  if (!name) return null;
  const Icon = lucideIconMap[name];
  return Icon ? <Icon className={className} /> : null;
}
