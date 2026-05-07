/**
 * @file index.tsx
 * @description 카드 대출 메뉴 가로 바 컴포넌트.
 *
 * 단기카드대출 / 장기카드대출 / 리볼빙 등 카드 금융 서비스 진입점을
 * 가로로 배열한 pill 형태의 메뉴 바다. 아이콘 + 텍스트 조합으로 각 서비스를 표시한다.
 * 반응형 동작:
 * - 아이템이 컨테이너 너비에 맞게 균등 분배 (flex-1)
 * - 아이템이 많아 넘칠 경우 가로 스크롤로 전환 (min-w-max 보장)
 * - 높이·패딩·텍스트·간격이 sm/md breakpoint에서 단계적으로 확장
 *
 * 스크롤 상호작용:
 * - 터치(모바일): 스와이프로 자연스럽게 가로 스크롤
 * - 마우스(데스크탑): 드래그 패닝으로 가로 스크롤
 * - 스크롤바는 webkit / Firefox 모두 숨김 처리
 *
 * @param items - 메뉴 항목 목록 (id / icon / label / onClick)
 *
 * @example
 * <LoanMenuBar
 *   items={[
 *     { id: 'short',    icon: <CreditCard size={14} />, label: '단기카드대출', onClick: () => {} },
 *     { id: 'long',     icon: <Banknote size={14} />,   label: '장기카드대출', onClick: () => {} },
 *     { id: 'revolving',icon: <RefreshCw size={14} />,  label: '리볼빙',       onClick: () => {} },
 *   ]}
 * />
 */
import React, { useRef } from 'react';
import { cn } from '@lib/cn';
import type { LoanMenuBarProps } from './types';

export function LoanMenuBar({ items = [], className }: LoanMenuBarProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  /* 드래그 패닝 상태 — ref로 관리해 리렌더 없이 처리 */
  const isDragging = useRef(false);
  const startX = useRef(0);
  const scrollLeft = useRef(0);

  function onMouseDown(e: React.MouseEvent) {
    if (!scrollRef.current) return;
    isDragging.current = true;
    startX.current = e.pageX - scrollRef.current.offsetLeft;
    scrollLeft.current = scrollRef.current.scrollLeft;
  }

  function onMouseMove(e: React.MouseEvent) {
    if (!isDragging.current || !scrollRef.current) return;
    e.preventDefault();
    const x = e.pageX - scrollRef.current.offsetLeft;
    /* 이동 거리에 1.5 배율 적용해 패닝 감도 향상 */
    scrollRef.current.scrollLeft = scrollLeft.current - (x - startX.current) * 1.5;
  }

  function stopDragging() {
    isDragging.current = false;
  }

  return (
    /* 회색 pill 컨테이너 — 전체 너비, 고정 높이, 가로 스크롤 허용
       스크롤바 숨김: [&::-webkit-scrollbar]:hidden(webkit) + style.scrollbarWidth(Firefox) */
    <div
      ref={scrollRef}
      className={cn(
        /* 고정 높이 대신 반응형 패딩으로 높이 자연 결정 */
        'flex items-center w-full',
        'py-xs sm:py-sm md:py-md',
        'bg-surface-raised rounded-xl',
        'overflow-x-auto [&::-webkit-scrollbar]:hidden',
        'px-sm sm:px-md gap-xs',
        /* 드래그 패닝 커서 — 마우스 사용자에게 드래그 가능 힌트 제공 */
        'cursor-grab active:cursor-grabbing select-none',
        className,
      )}
      /* Firefox 스크롤바 숨김 */
      style={{ scrollbarWidth: 'none' }}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={stopDragging}
      onMouseLeave={stopDragging}
    >
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          onClick={item.onClick}
          className={cn(
            /* flex-1로 여유 공간을 균등 분배, min-w-max로 텍스트 줄바꿈 방지 */
            'flex items-center justify-center gap-xs sm:gap-sm',
            'flex-1 min-w-max',
            'px-sm sm:px-md py-xs sm:py-sm rounded-lg',
            'text-xs sm:text-sm font-bold text-text-label',
            'hover:bg-surface transition-colors duration-150',
            'whitespace-nowrap',
          )}
        >
          {/* 아이콘 */}
          <span className="text-text-secondary shrink-0">{item.icon}</span>
          {/* 레이블 */}
          <span>{item.label}</span>
        </button>
      ))}
    </div>
  );
}
