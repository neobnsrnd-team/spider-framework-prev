/**
 * @file index.tsx
 * @description 수평 탭 네비게이션 컴포넌트.
 * - underline: 활성 탭은 브랜드 색상 텍스트 + 하단 2px 라인으로 표시.
 * - pill: 활성 탭에 둥근 배경 채움. 전체 탭을 감싸는 둥근 테두리 컨테이너 포함.
 *
 * @example
 * <TabNav
 *   items={[
 *     { id: 'mine',  label: '해당금융' },
 *     { id: 'other', label: '다른금융' },
 *     { id: 'asset', label: '자산관리' },
 *   ]}
 *   activeId={activeTabId}
 *   onTabChange={handleTabChange}
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { TabNavProps } from './types';

export function TabNav({ items = [], activeId, onTabChange, variant = 'underline', fullWidth = false, className }: TabNavProps) {
  /* ── underline 변형: 하단 인디케이터 라인 방식 (기본) ────────────── */
  if (variant === 'underline') {
    return (
      /* 컨테이너 하단 border: 전체 너비에 걸쳐 구분선 역할 */
      <div
        role="tablist"
        className={cn('flex border-b border-border-subtle', className)}
      >
        {items.map((item) => {
          const isActive = item.id === activeId;
          return (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => onTabChange(item.id)}
              className={cn(
                'relative px-standard pb-md pt-xs',
                'text-sm text-center whitespace-nowrap',
                'transition-colors duration-150',
                /* fullWidth: 탭이 균등하게 컨테이너 너비를 나눠 채움 */
                fullWidth && 'flex-1',
                isActive ? 'text-brand-text font-medium' : 'text-text-muted font-normal',
                'hover:text-brand-text',
              )}
            >
              {item.label}
              {/* 활성 탭 하단 인디케이터 라인 — absolute로 컨테이너 border 위에 겹침 */}
              {isActive && (
                <span
                  aria-hidden="true"
                  className="absolute bottom-[-1px] left-0 right-0 h-0.5 bg-brand rounded-full"
                />
              )}
            </button>
          );
        })}
      </div>
    );
  }

  /* ── pill 변형: 둥근 배경 채움 방식 ────────────────────────────────
   * 상품 카테고리·필터처럼 탭 수가 적고 좌측 정렬로 나열되는 패턴에 사용.
   * 활성: 브랜드 10% 배경 + 브랜드 텍스트 / 비활성: 투명 배경 + muted 텍스트
   * ─────────────────────────────────────────────────────────────── */
  // className은 외부 위치·여백 전용 wrapper — border 컨테이너와 분리해 px/py가 내부 패딩을 덮어쓰지 않도록 함
  return (
    <div className={cn(className)}>
    <div
      role="tablist"
      /* border 컨테이너: 선택 배경(bg-brand-10) 바로 바깥에 테두리가 붙도록 p-0 고정 */
      className="flex items-center gap-xs p-0 rounded-full border border-brand-10"
    >
      {items.map((item) => {
        const isActive = item.id === activeId;
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onTabChange(item.id)}
            className={cn(
              'px-md py-sm rounded-full',
              'text-sm text-center whitespace-nowrap',
              'transition-colors duration-150',
              /* fullWidth: gap을 유지하면서 각 탭이 균등하게 너비를 나눠 채움 */
              fullWidth && 'flex-1',
              isActive
                ? 'bg-brand-10 text-brand-text font-medium'
                : 'bg-transparent text-text-muted font-normal hover:text-brand-text',
            )}
          >
            {item.label}
          </button>
        );
      })}
    </div>
    </div>
  );
}
