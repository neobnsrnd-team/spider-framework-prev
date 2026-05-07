/**
 * @file index.tsx
 * @description 홈 화면 하단 sticky 탭 네비게이션 컴포넌트.
 * 화면 하단에 sticky로 달라붙으며, backdrop-blur 배경을 적용한다.
 * 활성 탭은 브랜드 색상 아이콘 + 텍스트 + 하단 점 인디케이터로 표시된다.
 *
 * HomePageLayout의 withBottomNav prop과 함께 사용하면
 * 본문 콘텐츠가 탭바에 가려지지 않도록 pb-nav 여백이 자동 처리된다.
 *
 * @example
 * <BottomNav
 *   activeId="home"
 *   items={[
 *     { id: 'asset',   icon: <Wallet        className="size-5" />, label: '자산',  onClick: () => {} },
 *     { id: 'product', icon: <ShoppingBag   className="size-5" />, label: '상품',  onClick: () => {} },
 *     { id: 'home',    icon: <Home          className="size-6" />, label: '홈',    onClick: () => {} },
 *     { id: 'card',    icon: <CreditCard    className="size-5" />, label: '카드',  onClick: () => {} },
 *     { id: 'chat',    icon: <MessageSquare className="size-5" />, label: '챗봇',  onClick: () => {} },
 *   ]}
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { BottomNavProps } from './types';

export function BottomNav({ items = [], activeId, className }: BottomNavProps) {
  return (
    <nav
      aria-label="하단 메뉴"
      className={cn(
        /* 화면 하단 sticky — 스크롤 시 부모 컨테이너 내에서 하단에 달라붙음 */
        'sticky bottom-0 left-0 right-0 z-sticky',
        'flex items-end justify-around',
        'pt-sm pb-2 px-standard',
        /* blur 배경: 스크롤 중에도 콘텐츠가 배경에 묻히지 않도록 처리 */
        'backdrop-blur-sm bg-surface/95 border-t border-border-subtle',
        className,
      )}
    >
      {items.map((item) => {
        const isActive = item.id === activeId;
        const displayIcon = isActive ? (item.activeIcon ?? item.icon) : item.icon;

        return (
          <button
            key={item.id}
            type="button"
            onClick={item.onClick}
            aria-current={isActive ? 'page' : undefined}
            aria-label={item.label}
            className={cn(
              'relative flex flex-col items-center gap-xs',
              /* pb-3: dot 인디케이터(-bottom-1)와 label 사이 여백 확보 */
              'min-w-[50px] pt-xs pb-3 px-md rounded-lg',
              'transition-colors duration-150',
              isActive ? 'text-brand-text' : 'text-text-muted',
              'hover:text-brand-text',
            )}
          >
            {displayIcon}
            <span className="text-[10px] font-medium leading-none">{item.label}</span>
            {/* 활성 탭 하단 인디케이터 점 — label 아래 4px 간격 확보 */}
            {isActive && (
              <span
                aria-hidden="true"
                className="absolute -bottom-1 left-1/2 -translate-x-1/2 size-1 rounded-full bg-brand"
              />
            )}
          </button>
        );
      })}
    </nav>
  );
}
