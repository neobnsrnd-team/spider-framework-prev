/**
 * @file index.tsx
 * @description 카드 이용내역 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / "이용내역" 타이틀 / 닫기(X)
 *   - 결제 요약 카드: 결제 예정일 + 총 청구금액 + 분할납부·즉시결제·리볼빙 버튼
 *   - 검색 헤더: 총 건수 + 필터(돋보기) 아이콘
 *   - 이용기간 레이블 (적용된 필터 조회기간 반영)
 *   - 이용내역 목록 (PAGE_SIZE=10건씩 더보기)
 *     - UsageTransactionItem: onClick 전달 → 상세 BottomSheet 노출
 *   - UsageHistoryFilterSheet: 검색 필터 (카드/월 중첩 BottomSheet 포함)
 *
 * @param transactions       - 이용내역 목록
 * @param totalCount         - 전체 건수
 * @param paymentSummary     - 결제 요약 (날짜·금액)
 * @param cardOptions        - 카드 선택 옵션
 * @param onLoadMore         - 더보기 핸들러
 * @param onInstallment      - 분할납부 클릭
 * @param onImmediatePayment - 즉시결제 클릭
 * @param onRevolving        - 리볼빙 클릭
 * @param onSearch           - 검색 필터 적용
 * @param onBack             - 뒤로가기
 * @param onClose            - 닫기(X)
 */
import React, { useState } from "react";
import { X, Search } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { Typography } from "@cl/core/Typography";
import { BillingPeriodLabel } from "@cl/biz/card/BillingPeriodLabel";
import { CardPaymentActions } from "@cl/biz/card/CardPaymentActions";

import { UsageTransactionItem } from "@cl/biz/card/UsageTransactionItem";
import { UsageHistoryFilterSheet } from "@cl/biz/card/UsageHistoryFilterSheet";

import { formatAmount } from "@/utils/format";
import type { UsageHistoryPageProps, SearchFilter } from "./types";

// ── 상수 ────────────────────────────────────────────────────

const PAGE_SIZE = 10;

// ── 조회기간 → 이용기간 레이블 계산 ──────────────────────────

/**
 * 적용된 필터의 조회기간을 기반으로 BillingPeriodLabel의 시작/종료일을 계산한다.
 * @param filter - 적용된 검색 필터 (filter.customMonth 포함)
 */
function computeBillingPeriod(filter: SearchFilter): {
  startDate: string;
  endDate: string;
} {
  const today = new Date();
  let start: Date;
  let end: Date;

  if (filter.period === "thisMonth") {
    start = new Date(today.getFullYear(), today.getMonth(), 1);
    end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  } else if (filter.period === "1month") {
    start = new Date(
      today.getFullYear(),
      today.getMonth() - 1,
      today.getDate(),
    );
    end = today;
  } else if (filter.period === "3months") {
    start = new Date(
      today.getFullYear(),
      today.getMonth() - 3,
      today.getDate(),
    );
    end = today;
  } else {
    /* custom: SearchFilter.customMonth 기준 월 1일 ~ 말일 */
    const [year, month] = (filter.customMonth ?? "").split("-").map(Number);
    start = new Date(year, month - 1, 1);
    end = new Date(year, month, 0);
  }

  const fmt = (d: Date) =>
    `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
  return { startDate: fmt(start), endDate: fmt(end) };
}

// ── 메인 페이지 ───────────────────────────────────────────

const DEFAULT_FILTER: SearchFilter = {
  approval: "approved",
  cardType: "all",
  selectedCard: "all",
  region: "all",
  usageType: "all",
  period: "thisMonth",
};

export function UsageHistoryPage({
  transactions,
  totalCount,
  paymentSummary,
  cardOptions,
  onLoadMore,
  onInstallment,
  onImmediatePayment,
  onRevolving,
  onSearch,
  onBack,
  onClose,
}: UsageHistoryPageProps) {
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [searchOpen, setSearchOpen] = useState(false);
  const [appliedFilter, setAppliedFilter] =
    useState<SearchFilter>(DEFAULT_FILTER);

  const visibleTx = transactions.slice(0, visibleCount);
  const hasMore = visibleCount < transactions.length;

  function handleLoadMore() {
    setVisibleCount((prev) => prev + PAGE_SIZE);
    onLoadMore?.();
  }

  function handleApply(filter: SearchFilter) {
    setAppliedFilter(filter);
    onSearch?.(filter);
  }

  const billingPeriod = computeBillingPeriod(appliedFilter);

  return (
    <>
      <PageLayout
        title="이용내역"
        onBack={onBack}
        rightAction={
          <Button
            variant="ghost"
            size="md"
            iconOnly
            leftIcon={<X className="size-5" />}
            onClick={onClose}
            aria-label="닫기"
          />
        }
      >
        {/* ── 결제 요약 카드 ─────────────────────────────── */}
        <div className="pt-md">
          <div className="flex flex-col gap-md bg-surface rounded-2xl shadow-card px-md">
            <div className="flex flex-col gap-xs">
              <Typography variant="body-sm" color="muted">
                {paymentSummary.date
                  ? `${paymentSummary.date} 출금예정`
                  : "출금예정일 정보 없음"}
              </Typography>
              <Typography
                variant="heading"
                weight="bold"
                color="heading"
                numeric
              >
                {formatAmount(Math.abs(paymentSummary.totalAmount))}
              </Typography>
            </div>
            <CardPaymentActions
              onInstallment={onInstallment}
              onImmediatePayment={onImmediatePayment}
              onRevolving={onRevolving}
            />
          </div>
        </div>

        {/* ── 검색 헤더: 총 건수 + 필터 아이콘 ─────────── */}
        <div className="flex items-center justify-between py-sm">
          <Typography variant="caption" color="muted">
            총 {totalCount}건
          </Typography>
          <Button
            variant="ghost"
            size="md"
            iconOnly
            leftIcon={<Search className="size-4" aria-hidden="true" />}
            onClick={() => setSearchOpen(true)}
            aria-label="검색 필터 열기"
          />
        </div>

        {/* ── 이용기간 레이블 — 적용된 필터 조회기간 반영 */}
        <BillingPeriodLabel
          startDate={billingPeriod.startDate}
          endDate={billingPeriod.endDate}
          className="pb-sm"
        />

        {/* ── 이용내역 목록 ──────────────────────────────── */}
        <div className="flex flex-col">
          {visibleTx.length === 0 ? (
            <div className="py-3xl text-center">
              <Typography variant="body-sm" color="muted">
                이용내역이 없습니다.
              </Typography>
            </div>
          ) : (
            <>
              <div className="flex flex-col divide-y divide-border-subtle">
                {visibleTx.map((tx) => (
                  /* onClick 전달 → 항목 클릭 시 상세 BottomSheet 노출 */
                  <UsageTransactionItem
                    key={tx.id}
                    tx={tx}
                    onClick={() => {}}
                  />
                ))}
              </div>
              {hasMore && (
                <Button
                  variant="outline"
                  size="md"
                  fullWidth
                  onClick={handleLoadMore}
                  className="mt-lg mb-xl"
                >
                  더보기
                </Button>
              )}
            </>
          )}
        </div>
      </PageLayout>

      {/* ── 검색 필터 BottomSheet (내부 상태 자체 관리) ── */}
      <UsageHistoryFilterSheet
        open={searchOpen}
        onClose={() => setSearchOpen(false)}
        cardOptions={cardOptions}
        onApply={handleApply}
      />
    </>
  );
}
