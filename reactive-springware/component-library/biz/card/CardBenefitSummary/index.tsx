/**
 * @file index.tsx
 * @description 카드 포인트·혜택 요약 컴포넌트.
 *
 * 보유 포인트 잔액과 이번달 혜택(할인·캐시백 등) 항목을 한 카드에 표시한다.
 * 포인트 영역(상단)과 혜택 영역(하단)은 가로 구분선으로 나뉘며, 각각 상세 클릭 핸들러를 가진다.
 * 카드 대시보드 내 혜택 섹션에 사용한다.
 *
 * @param points          - 보유 포인트 잔액
 * @param benefits        - 이번달 혜택 항목 목록 (할인·캐시백 등)
 * @param onPointDetail   - 포인트 상세 클릭 핸들러
 * @param onBenefitDetail - 혜택 상세 클릭 핸들러
 */
import React from 'react';
import { ChevronRight } from 'lucide-react';

import { Typography } from '../../../core/Typography';
import type { CardBenefitSummaryProps } from './types';

export type { CardBenefitSummaryProps, BenefitItem } from './types';

/** 숫자 포맷터 (콤마 구분) */
const numFmt = new Intl.NumberFormat('ko-KR');

export function CardBenefitSummary({
  points,
  benefits = [],
  onPointDetail,
  onBenefitDetail,
}: CardBenefitSummaryProps) {
  /** 이번달 혜택 항목 전체 합계 (단위가 '원'인 항목만) */
  const totalBenefit = benefits
    .filter((b) => !b.unit || b.unit === '원')
    .reduce((sum, b) => sum + b.amount, 0);

  return (
    <div className="bg-surface rounded-lg shadow-card border border-border-subtle overflow-hidden">
      <div className="flex flex-col divide-y divide-border-subtle">
        {/* ── 상단: 포인트 잔액 ─────────────────────── */}
        <button
          type="button"
          onClick={onPointDetail}
          className="flex items-center justify-between px-md py-md text-left transition-colors duration-150 disabled:cursor-default"
          disabled={!onPointDetail}
        >
          <Typography variant="caption" color="muted">
            보유 포인트
          </Typography>
          <div className="flex items-center gap-xs">
            <Typography variant="body-sm" weight="bold" color="brand" numeric>
              {numFmt.format(points)}P
            </Typography>
            {onPointDetail && (
              <ChevronRight className="size-3.5 text-text-muted shrink-0" aria-hidden="true" />
            )}
          </div>
        </button>

        {/* ── 하단: 이번달 혜택 ─────────────────────── */}
        <button
          type="button"
          onClick={onBenefitDetail}
          className="flex flex-col gap-xs px-md py-md text-left transition-colors duration-150 disabled:cursor-default"
          disabled={!onBenefitDetail}
        >
          <div className="flex items-center justify-between">
            <Typography variant="caption" color="muted">
              이번달 혜택
            </Typography>
            <div className="flex items-center gap-xs">
              <Typography variant="body-sm" weight="bold" color="heading" numeric>
                {numFmt.format(totalBenefit)}원
              </Typography>
              {onBenefitDetail && (
                <ChevronRight className="size-3.5 text-text-muted shrink-0" aria-hidden="true" />
              )}
            </div>
          </div>
          {/* 혜택 항목 상세 — 2개까지만 표시 */}
          <div className="flex gap-sm">
            {benefits.slice(0, 2).map((b) => (
              <Typography key={b.label} variant="caption" color="muted">
                {b.label} {numFmt.format(b.amount)}
                {b.unit ?? '원'}
              </Typography>
            ))}
          </div>
        </button>
      </div>
    </div>
  );
}
