/**
 * @file index.tsx
 * @description 보험 요약 카드 컴포넌트.
 *
 * AccountSummaryCard와 동일한 레이아웃 구조를 따름:
 * - 상단: 보험명 + 상태 배지 (status 기반 색상 자동 결정)
 * - 중단: 증권 번호
 * - 금액 영역: 월 납입 보험료 + 다음 납입일
 * - 하단: actions 슬롯
 *
 * status별 배지 색상:
 * - active:  success (초록)
 * - pending: warning (노랑)
 * - expired: danger  (빨강)
 *
 * @example
 * <InsuranceSummaryCard
 *   type="health"
 *   insuranceName="하나생명 건강보험"
 *   contractNumber="2024-001-123456"
 *   premium={52000}
 *   nextPaymentDate="2026.04.01"
 *   status="active"
 *   actions={<Button size="sm" variant="outline">보장내역</Button>}
 * />
 */
import React from 'react';
import { Heart, Car, Shield } from 'lucide-react';
import { cn } from '@lib/cn';
import type { InsuranceSummaryCardProps, InsuranceStatus, InsuranceType } from './types';

/** 원화 금액 포맷터 */
const krwFormatter = new Intl.NumberFormat('ko-KR');

/** status별 기본 배지 텍스트 */
const defaultBadgeText: Record<InsuranceStatus, string> = {
  active:  '정상',
  pending: '유예',
  expired: '만기',
};

/** status별 배지 색상 클래스 */
const statusBadgeClass: Record<InsuranceStatus, string> = {
  active:  'bg-success-surface text-success-text',
  pending: 'bg-warning-surface text-warning-text',
  /* expired는 위험/종료 상태이므로 danger 계열 사용 */
  expired: 'bg-danger-surface text-danger-text',
};

/** type별 아이콘 — Figma 플러그인 TYPE_CONFIG 기준: Health→Heart, Car→Car, Life→Shield */
const typeIcon: Record<InsuranceType, React.ReactNode> = {
  health: <Heart  size={20} className="text-text-muted shrink-0" />,
  car:    <Car    size={20} className="text-text-muted shrink-0" />,
  life:   <Shield size={20} className="text-text-muted shrink-0" />,
};

export function InsuranceSummaryCard({
  type,
  insuranceName,
  contractNumber,
  premium,
  nextPaymentDate,
  status,
  badgeText,
  onClick,
  actions,
  className,
}: InsuranceSummaryCardProps) {
  const Tag = onClick ? 'button' : 'div';

  /* badgeText가 없으면 status 기반 기본값 사용 */
  const resolvedBadge   = badgeText ?? defaultBadgeText[status];
  const formattedPremium = `${krwFormatter.format(premium)}원`;

  return (
    <Tag
      onClick={onClick}
      className={cn(
        'w-full text-left',
        'bg-surface rounded-xl p-lg',
        'border border-border-subtle shadow-sm',
        'transition-all duration-150',
        onClick && 'cursor-pointer hover:border-brand hover:shadow-brand active:scale-[0.99]',
        /* expired 상태이면 카드 전체에 미세한 비활성 표시 */
        status === 'expired' && 'opacity-70',
        className,
      )}
    >
      {/* ── 상단: 타입 아이콘 + 보험명 + 상태 배지 ──────────── */}
      <div className="flex items-center gap-sm mb-xs">
        {/* 보험 유형 아이콘 — Figma 플러그인 기준: health→Heart, car→Car, life→Shield */}
        {typeIcon[type]}
        <span className="text-sm font-bold text-text-heading truncate">{insuranceName}</span>
        {/* 상태 배지: status별 색상 자동 결정 */}
        <span
          className={cn(
            'shrink-0 px-sm py-0.5 rounded-full text-xs font-bold',
            statusBadgeClass[status],
          )}
        >
          {resolvedBadge}
        </span>
      </div>

      {/* ── 증권 번호 ─────────────────────────────────────── */}
      <p className="text-xs text-text-muted mb-md">{contractNumber}</p>

      {/* ── 보험료 영역 ───────────────────────────────────── */}
      {/* actions 있을 때만 mb-lg 적용 — 없으면 하단 여백을 잡지 않음 */}
      <div className={cn(actions && 'mb-lg')}>
        <p className="text-xs text-text-muted mb-xs">월 납입 보험료</p>
        <p
          className="text-xl font-bold font-numeric tabular-nums text-text-heading"
          aria-label={`월 납입 보험료 ${formattedPremium}`}
        >
          {formattedPremium}
        </p>
        {/* 다음 납입일 — 전달된 경우에만 표시 */}
        {nextPaymentDate && (
          <p className="text-xs text-text-muted mt-xs">
            다음 납입일 <span className="font-medium text-text-secondary">{nextPaymentDate}</span>
          </p>
        )}
      </div>

      {/* ── 액션 버튼 슬롯 ───────────────────────────────── */}
      {actions && (
        <div
          className="flex gap-sm"
          /* 카드 자체 onClick으로 이벤트가 버블링되지 않도록 방지 */
          onClick={e => e.stopPropagation()}
        >
          {actions}
        </div>
      )}
    </Tag>
  );
}