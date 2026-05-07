/**
 * @file index.tsx
 * @description 홈 화면 퀵메뉴 그리드 컴포넌트.
 * 아이콘 + 레이블 형식의 메뉴 항목을 2×N 또는 N열 그리드로 배치한다.
 * badge prop이 있을 때만 빨간 알림 배지를 표시한다.
 *
 * @example
 * <QuickMenuGrid
 *   cols={4}
 *   items={[
 *     { id: 'transfer', icon: <ArrowRightLeft size={20} />, label: '이체', onClick: () => {} },
 *     { id: 'history',  icon: <ClockIcon size={20} />,     label: '거래내역', onClick: () => {}, badge: 3 },
 *   ]}
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { QuickMenuGridProps, QuickMenuItem } from './types';

/** 열 수 → Tailwind grid-cols 클래스 */
const colsClass: Record<NonNullable<QuickMenuGridProps['cols']>, string> = {
  2: 'grid-cols-2',
  3: 'grid-cols-3',
  4: 'grid-cols-4',
};

interface QuickMenuItemButtonProps {
  item: QuickMenuItem;
}

function QuickMenuItemButton({ item }: QuickMenuItemButtonProps) {
  /* 배지 표시 조건: badge가 1 이상인 경우만 */
  const showBadge = item.badge != null && item.badge > 0;
  /* 99 초과 시 '99+' 표시 */
  const badgeLabel = item.badge != null && item.badge > 99 ? '99+' : String(item.badge);

  return (
    <button
      type="button"
      onClick={item.onClick}
      aria-label={`${item.label}${showBadge ? `, 알림 ${badgeLabel}건` : ''}`}
      className={cn(
        'relative flex flex-col items-center gap-xs py-md',
        'rounded-xl transition-all duration-150',
        'hover:bg-brand-5 active:scale-[0.96]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-20',
      )}
    >
      {/* 아이콘 컨테이너 — iconShape에 따라 원형/둥근 사각형 전환 */}
      <span
        className={cn(
          'relative flex items-center justify-center size-12 bg-brand-5 text-brand-text',
          /* 기본(circle): rounded-full / rounded: rounded-2xl */
          item.iconShape === 'rounded' ? 'rounded-2xl' : 'rounded-full',
        )}
        aria-hidden="true"
      >
        {item.icon}

        {/* 알림 배지 — badge > 0 일 때만 렌더링 */}
        {showBadge && (
          <span
            className={cn(
              'absolute -top-1 -right-1',
              'flex items-center justify-center',
              'min-w-[18px] h-[18px] px-xs',
              'rounded-full bg-danger text-white text-[10px] font-bold',
            )}
            aria-hidden="true"
          >
            {badgeLabel}
          </span>
        )}
      </span>

      {/* 메뉴 레이블 */}
      <span className="text-xs font-medium text-text-base text-center leading-tight">
        {item.label}
      </span>
    </button>
  );
}

export function QuickMenuGrid({ items = [], cols = 4, className }: QuickMenuGridProps) {
  return (
    <nav aria-label="퀵메뉴" className={cn('grid', colsClass[cols], className)}>
      {items.map((item) => (
        <QuickMenuItemButton key={item.id} item={item} />
      ))}
    </nav>
  );
}
