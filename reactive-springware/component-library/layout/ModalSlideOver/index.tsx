/**
 * @file index.tsx
 * @description 슬라이드 오버 모달 레이아웃 컴포넌트.
 *
 * Route 기반 모달 패턴에서 사용한다.
 * React Router의 background location 패턴과 함께 쓰면
 * 뒤에 배경 페이지를 유지하면서 슬라이드 오버를 표시할 수 있다.
 *
 * @example
 * // routes.tsx
 * export const modalRoutes = [
 *   { path: '/card/menu', element: <ModalSlideOver onClose={() => navigate(-1)}><HanaCardMenuPage ... /></ModalSlideOver> }
 * ]
 */
import React from 'react';
import { createPortal } from 'react-dom';
import { cn } from '@lib/cn';
import type { ModalSlideOverProps } from './types';

export type { ModalSlideOverProps } from './types';

export function ModalSlideOver({
  children,
  onClose,
  direction = 'right',
  zIndex = 50,
  container,
}: ModalSlideOverProps) {
  const isRight  = direction === 'right';
  const isBottom = direction === 'bottom';

  // container 제공 시 absolute(캔버스 기준), 미제공 시 fixed(뷰포트 기준)
  const positionClass = container ? 'absolute' : 'fixed';

  return createPortal(
    <div
      className={`${positionClass} inset-0 flex`}
      style={{ zIndex }}
      role="dialog"
      aria-modal="true"
    >
      {/* 백드롭 */}
      <div
        className={cn(
          'absolute inset-0 bg-black/40',
          onClose ? 'cursor-pointer' : 'cursor-default',
        )}
        onClick={onClose}
        aria-hidden="true"
      />

      {/* 콘텐츠 패널 */}
      <div
        className={cn(
          'relative bg-white overflow-y-auto shadow-2xl',
          /* right: 오른쪽에서 슬라이드 */
          isRight  && 'ml-auto w-full max-w-[390px] h-full animate-slide-in-right',
          /* bottom: 아래에서 슬라이드 */
          isBottom && 'mt-auto w-full max-h-[90dvh] rounded-t-2xl animate-slide-in-bottom',
        )}
      >
        {children}
      </div>
    </div>,
    container ?? document.body,
  );
}
