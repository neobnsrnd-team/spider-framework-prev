/**
 * @file index.tsx
 * @description 세로 방향 사이드바 탭 네비게이션 컴포넌트.
 *
 * 전체 메뉴 화면처럼 화면 좌측에 세로로 카테고리 목록을 표시하고
 * 선택된 항목에 좌측 브랜드 컬러 인디케이터를 보여주는 패턴에 사용한다.
 * 가로 탭이 필요한 경우 TabNav를 사용한다.
 *
 * @param items        - 네비게이션 항목 배열
 * @param activeId     - 현재 활성화된 항목 id
 * @param onItemChange - 항목 선택 시 호출되는 콜백 (선택된 id 전달)
 * @param className    - 추가 Tailwind 클래스
 *
 * @example
 * <SidebarNav
 *   items={[
 *     { id: 'banking', label: '뱅킹' },
 *     { id: 'card',    label: '카드' },
 *   ]}
 *   activeId={activeCategory}
 *   onItemChange={handleCategoryChange}
 *   className="w-[117px] bg-surface-raised border-r border-border-subtle"
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { SidebarNavProps } from './types';

export type { SidebarNavProps, SidebarNavItem } from './types';

export function SidebarNav({
  items = [],
  activeId,
  onItemChange,
  className,
}: SidebarNavProps) {
  return (
    /* role="tablist" + aria-orientation: 스크린리더가 세로 탭 목록으로 인식 */
    <nav
      role="tablist"
      aria-orientation="vertical"
      className={cn('flex flex-col', className)}
    >
      {items.map((item) => {
        const isActive = item.id === activeId;

        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onItemChange(item.id)}
            className={cn(
              /* 모든 항목 공통: 높이 56px, 좌측 패딩, 텍스트 크기 */
              'relative flex items-center h-14 px-standard text-sm text-left',
              'transition-colors duration-150',
              isActive
                /* 활성: 흰 배경 + 브랜드 텍스트 + medium 폰트 */
                ? 'bg-surface text-brand-text font-medium'
                /* 비활성: 투명 배경 + 보조 텍스트 */
                : 'text-text-secondary hover:bg-surface-raised hover:text-text-heading',
            )}
          >
            {/* 활성 상태 좌측 인디케이터 바 — 4px 너비, 상하 25% 여백, 우측 모서리만 둥글게 */}
            {isActive && (
              <span
                aria-hidden="true"
                className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-brand rounded-r"
              />
            )}
            {item.label}
          </button>
        );
      })}
    </nav>
  );
}
