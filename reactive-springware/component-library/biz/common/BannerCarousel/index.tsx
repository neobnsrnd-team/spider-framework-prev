/**
 * @file index.tsx
 * @description 홈 화면 배너 슬라이더 컴포넌트.
 *
 * 도메인 방침(6.4):
 * - 배너 1개: 인디케이터·자동 재생·스와이프 모두 비활성화
 * - 배너 2개 이상: 인디케이터·자동 재생(3초)·스와이프 모두 활성화
 *
 * @example
 * <BannerCarousel
 *   items={[
 *     { id: '1', title: '하나원큐 특별 금리 이벤트', description: '최대 연 4.5%' },
 *     { id: '2', variant: 'info', title: '공지사항', description: '서비스 점검 안내' },
 *   ]}
 * />
 */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { X } from 'lucide-react';
import { cn } from '@lib/cn';
import type { BannerCarouselProps, BannerCarouselItem, BannerVariant } from './types';

/** variant별 배경 그라데이션 스타일 */
const variantStyles: Record<BannerVariant, string> = {
  /* promo: 브랜드 그라데이션 배경 */
  promo:   'bg-gradient-to-r from-brand to-brand-alt text-brand-fg',
  info:    'bg-surface-raised text-text-heading border border-border',
  warning: 'bg-warning-surface text-warning-text border border-warning-border',
};

interface SingleBannerProps {
  item:    BannerCarouselItem;
  /** true이면 닫기 버튼 표시 */
  showClose?: boolean;
}

function SingleBanner({ item, showClose }: SingleBannerProps) {
  const variant = item.variant ?? 'promo';

  return (
    <div
      className={cn(
        'relative flex items-center gap-standard rounded-xl p-lg',
        variantStyles[variant],
      )}
      role="region"
      aria-label={item.title}
    >
      {/* 본문 */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-bold truncate">{item.title}</p>
        {item.description && (
          <p className="text-xs mt-xs opacity-80 truncate">{item.description}</p>
        )}
      </div>

      {/* CTA 슬롯 */}
      {item.action && (
        <div className="shrink-0" onClick={e => e.stopPropagation()}>
          {item.action}
        </div>
      )}

      {/* 닫기 버튼 */}
      {showClose && item.onClose && (
        <button
          type="button"
          onClick={e => { e.stopPropagation(); item.onClose?.(); }}
          aria-label="배너 닫기"
          className="absolute top-sm right-sm flex items-center justify-center size-6 rounded-full opacity-70 hover:opacity-100 transition-opacity"
        >
          <X className="size-3" />
        </button>
      )}
    </div>
  );
}

export function BannerCarousel({
  items = [],
  autoPlayInterval = 3000,
  className,
}: BannerCarouselProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  /* 멀티 배너 여부 */
  const isMulti = items.length >= 2;

  /* 자동 재생 타이머 */
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const startAutoPlay = useCallback(() => {
    if (!isMulti) return;
    timerRef.current = setInterval(() => {
      setActiveIndex(i => (i + 1) % items.length);
    }, autoPlayInterval);
  }, [isMulti, items.length, autoPlayInterval]);

  const stopAutoPlay = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  useEffect(() => {
    startAutoPlay();
    return stopAutoPlay;
  }, [startAutoPlay, stopAutoPlay]);

  /* 스와이프 제스처 지원 */
  const touchStartX = useRef<number | null>(null);

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    stopAutoPlay(); /* 터치 시 자동 재생 일시 중단 */
  }, [stopAutoPlay]);

  const handleTouchEnd = useCallback(
    (e: React.TouchEvent) => {
      if (touchStartX.current == null) return;
      const diff = touchStartX.current - e.changedTouches[0].clientX;
      if (Math.abs(diff) > 50) {
        /* 50px 이상 스와이프 시 슬라이드 이동 */
        setActiveIndex(i =>
          diff > 0
            ? (i + 1) % items.length          /* 왼쪽으로 스와이프 → 다음 */
            : (i - 1 + items.length) % items.length, /* 오른쪽 → 이전 */
        );
      }
      touchStartX.current = null;
      startAutoPlay(); /* 스와이프 후 자동 재생 재시작 */
    },
    [items.length, startAutoPlay],
  );

  /* 배너 없음: 아무것도 렌더링하지 않음 */
  if (items.length === 0) return null;

  /* 배너 1개: 슬라이더 기능 없이 단순 렌더링 */
  if (!isMulti) {
    return (
      <div className={className}>
        <SingleBanner item={items[0]} showClose />
      </div>
    );
  }

  return (
    <div className={cn('flex flex-col gap-sm', className)}>
      {/* 슬라이드 영역 */}
      <div
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        aria-roledescription="carousel"
        aria-label="프로모션 배너"
      >
        <SingleBanner item={items[activeIndex]} />
      </div>

      {/* 인디케이터 (점) */}
      <div
        className="flex justify-center gap-xs"
        role="tablist"
        aria-label="배너 선택"
      >
        {items.map((item, i) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={i === activeIndex}
            aria-label={`${i + 1}번 배너 ${item.title}`}
            onClick={() => {
              setActiveIndex(i);
              stopAutoPlay();
              startAutoPlay();
            }}
            className={cn(
              'rounded-full transition-all duration-200',
              i === activeIndex
                ? 'w-4 h-2 bg-brand'
                : 'w-2 h-2 bg-border',
            )}
          />
        ))}
      </div>
    </div>
  );
}