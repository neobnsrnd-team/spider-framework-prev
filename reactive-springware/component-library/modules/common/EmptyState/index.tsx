/**
 * @file index.tsx
 * @description 빈 상태 화면 공통 컴포넌트.
 * 도메인별 일러스트·메시지·액션을 props로 주입해 재사용한다.
 * 컴포넌트 자체는 브랜드·도메인을 알지 못한다(modules/ 분류).
 *
 * @example
 * // 거래 내역 없음
 * <EmptyState
 *   illustration={<TransactionEmptyIllust />}
 *   title="거래 내역이 없어요"
 *   description="아직 이체하거나 받은 내역이 없습니다"
 * />
 *
 * // 계좌 없음 + 액션
 * <EmptyState
 *   title="등록된 계좌가 없어요"
 *   action={<Button onClick={onAdd}>계좌 추가하기</Button>}
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { EmptyStateProps } from './types';

export type { EmptyStateProps } from './types';

export function EmptyState({
  illustration,
  title,
  description,
  actionLabel,
  onAction,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center px-xl py-2xl gap-lg',
        className,
      )}
      role="status"
      aria-label={title}
    >
      {illustration && (
        <div className="shrink-0" aria-hidden="true">
          {illustration}
        </div>
      )}

      <div className="flex flex-col gap-sm">
        <p className="text-base font-bold text-text-heading">{title}</p>
        {description && (
          <p className="text-sm text-text-muted leading-relaxed">{description}</p>
        )}
      </div>

      {/* actionLabel 우선 — 없으면 커스텀 action 슬롯 */}
      {actionLabel ? (
        <button
          type="button"
          onClick={onAction}
          className="mt-sm px-lg py-sm text-sm font-medium rounded-xl border border-border text-text-base hover:bg-surface-subtle transition-colors"
        >
          {actionLabel}
        </button>
      ) : (
        action && <div className="mt-sm">{action}</div>
      )}
    </div>
  );
}