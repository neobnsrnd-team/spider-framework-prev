/**
 * @file index.tsx
 * @description 즉시결제 완료/오류 페이지 (STEP 4).
 *
 * 화면 구성:
 *   - 성공: 체크 아이콘 + "즉시결제 신청이 완료되었습니다." + 결제 정보 요약
 *   - 오류: 경고 아이콘 + 오류 메시지 + 신청 정보 요약 (처리일시·이용가능한도 제외)
 *
 * @param cardName      - 결제 카드명
 * @param cardNumber    - 마스킹된 카드번호
 * @param amount        - 결제 금액 (원)
 * @param account       - 출금 계좌
 * @param availableLimit - 결제 후 이용가능한도 (원)
 * @param completedAt   - 처리일시 (성공 시에만 표시)
 * @param error         - 오류 메시지. 값이 있으면 오류 화면을 렌더링한다.
 * @param onConfirm     - 확인 버튼 클릭
 */
import React from 'react';
import { CheckCircle, XCircle } from 'lucide-react';

import { PageLayout } from '@cl/layout/PageLayout';
import { Button } from '@cl/core/Button';
import { Typography } from '@cl/core/Typography';
import { LabelValueRow } from '@cl/modules/common/LabelValueRow';
import { Divider } from '@cl/modules/common/Divider';

import { formatAmount } from '@/utils/format';
import type { ImmediatePayCompletePageProps } from './types';

export function ImmediatePayCompletePage({
  cardName,
  cardNumber,
  amount,
  account,
  availableLimit,
  completedAt,
  error,
  onConfirm,
}: ImmediatePayCompletePageProps) {
  const isError = !!error;

  return (
    <PageLayout
      title="즉시결제"
      bottomBar={
        <Button variant="primary" size="lg" fullWidth onClick={onConfirm}>
          확인
        </Button>
      }
    >
      <div className="flex flex-col gap-lg pt-md pb-xl">
        {/* ── 상태 아이콘 + 메시지 ─────────────────────────────────
         * 성공: 브랜드 색상 체크 아이콘 / 오류: 빨간 X 아이콘 */}
        <div className="flex flex-col items-center gap-md py-xl">
          {isError ? (
            <div className="flex items-center justify-center size-16 rounded-full bg-red-50">
              <XCircle className="size-9 text-red-500" aria-hidden="true" />
            </div>
          ) : (
            <div className="flex items-center justify-center size-16 rounded-full bg-brand-10">
              <CheckCircle className="size-9 text-brand" aria-hidden="true" />
            </div>
          )}
          <div className="flex flex-col items-center gap-xs text-center">
            <Typography variant="subheading" weight="bold" color="heading">
              {isError ? '즉시결제에 실패하였습니다.' : '즉시결제 신청이 완료되었습니다.'}
            </Typography>
            {/* 오류일 때만 구체적인 사유를 표시한다 */}
            {isError && (
              <Typography variant="body-sm" color="muted">
                {error}
              </Typography>
            )}
          </div>
        </div>

        <Divider />

        {/* ── 결제 정보 요약 ────────────────────────────────────────
         * 성공: 처리일시·이용가능한도 포함 / 오류: 신청 정보만 표시 */}
        <div className="flex flex-col gap-sm bg-surface rounded-xl px-md py-md shadow-card">
          <Typography variant="body-sm" weight="bold" color="heading">
            {isError ? '신청 정보' : '결제 정보'}
          </Typography>
          <div className="flex flex-col gap-xs">
            <LabelValueRow label="카드명"    value={cardName} />
            <LabelValueRow label="카드번호"  value={cardNumber} />
            <LabelValueRow label="결제금액"  value={formatAmount(amount)} />
            <LabelValueRow label="출금계좌"  value={account} />
            {/* 성공 시에만 표시 — 오류 시 한도·처리일시는 변경되지 않았으므로 제외 */}
            {!isError && (
              <>
                <LabelValueRow label="이용가능한도" value={formatAmount(availableLimit)} />
                <LabelValueRow label="처리일시"     value={completedAt} />
              </>
            )}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
