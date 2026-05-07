/**
 * @file types.ts
 * @description BannerCarousel 컴포넌트의 TypeScript 타입 정의.
 * 6.4 도메인 방침: 배너 수에 따라 단일/멀티 동작 자동 결정.
 */
import React from 'react';

export type BannerVariant = 'promo' | 'info' | 'warning';

export interface BannerCarouselItem {
  id:           string;
  /** 배너 색상 변형. 기본: 'promo' */
  variant?:     BannerVariant;
  title:        string;
  description?: string;
  /** 우측 CTA 슬롯 */
  action?:      React.ReactNode;
  /** 전달 시 닫기(×) 버튼 표시 */
  onClose?:     () => void;
}

export interface BannerCarouselProps {
  /** 배너 목록. 미전달 또는 빈 배열이면 아무것도 렌더링하지 않음 */
  items?:              BannerCarouselItem[];
  /**
   * 자동 재생 간격(ms). 기본: 3000.
   * items.length < 2이면 자동 재생 비활성화.
   */
  autoPlayInterval?:   number;
  className?:          string;
}