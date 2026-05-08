/**
 * @file index.tsx
 * @description 결제방식 선택 페이지 (STEP 3).
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / '즉시결제' 타이틀 / 닫기(X)
 *   - STEP 3 레이블 + 안내 문구
 *   - 신청정보 확인: STEP 1·2 선택값 요약 (LabelValueRow 목록)
 *   - 출금계좌 선택: 계좌 목록 (클릭으로 선택)
 *   - 하단 고정 신청 버튼
 *
 * @param summaryItems      - 신청정보 확인 항목 (STEP 1·2 요약)
 * @param accounts          - 출금계좌 목록
 * @param initialAccountId  - 초기 선택 계좌 id
 * @param onApply           - 신청 클릭 (선택 계좌 id 전달)
 * @param onBack            - 뒤로가기
 * @param onClose           - 닫기(X)
 */
import React, { useState } from "react";
import { X, RotateCcw } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { Typography } from "@cl/core/Typography";
import { LabelValueRow } from "@cl/modules/common/LabelValueRow";
import { Divider } from "@cl/modules/common/Divider";
import { StepIndicator } from "@cl/modules/common/StepIndicator";
import { TabNav } from "@cl/modules/common/TabNav";
import { AccountSelectCard } from "@cl/biz/card/AccountSelectCard";

import type { ImmediatePayMethodPageProps, PaymentType } from "./types";

const PAYMENT_TYPE_TABS = [
  { id: "total", label: "총 이용금액 결제" },
  { id: "per-item", label: "이용건별 결제" },
] as const;

export function ImmediatePayMethodPage({
  initialPaymentType = "total",
  summaryItems,
  accounts,
  initialAccountId,
  onApply,
  onBack,
  onClose,
  pinExceeded = false,
  onResetPinAttempts,
}: ImmediatePayMethodPageProps) {
  const [paymentType, setPaymentType] =
    useState<PaymentType>(initialPaymentType);
  const [selectedId, setSelectedId] = useState(
    initialAccountId ?? accounts[0]?.id ?? "",
  );

  return (
    <PageLayout
      title="즉시결제"
      onBack={onBack}
      bottomBar={
        pinExceeded ? (
          /* PIN 횟수 초과 시 신청 버튼 대신 초기화 버튼을 표시한다 */
          <Button
            variant="outline"
            size="lg"
            fullWidth
            leftIcon={<RotateCcw className="size-4" />}
            onClick={onResetPinAttempts}
          >
            PIN 입력 횟수 초기화
          </Button>
        ) : (
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => onApply?.(selectedId)}
          >
            신청
          </Button>
        )
      }
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
      <div className="flex flex-col gap-lg pt-md pb-xl">
        {/* ── STEP 3 레이블 ─────────────────────────────────────── */}
        <div className="flex flex-col gap-xs">
          <Typography variant="caption" color="brand">
            STEP 3
          </Typography>
          <StepIndicator total={3} current={3} />
          <Typography variant="subheading" weight="bold" color="heading">
            결제방식을 선택해 주세요.
          </Typography>
        </div>

        {/* ── 신청정보 확인 ───────────────────────────────────────
         * STEP 1·2에서 선택한 값을 요약해서 보여준다. */}
        <div className="flex flex-col gap-sm bg-surface rounded-xl px-md py-md shadow-card">
          <Typography variant="body-sm" weight="bold" color="heading">
            신청정보 확인
          </Typography>
          <div className="flex flex-col gap-xs">
            {summaryItems.map((item) => (
              <LabelValueRow
                key={item.label}
                label={item.label}
                value={item.value}
              />
            ))}
          </div>
        </div>

        <Divider />

        {/* ── 출금계좌 선택 ──────────────────────────────────────
         * 선택된 계좌는 우측 체크 아이콘으로 표시된다. */}
        <div className="flex flex-col gap-sm">
          <Typography variant="body-sm" weight="bold" color="heading">
            출금계좌 선택
          </Typography>
          <div className="flex flex-col gap-sm">
            {accounts.map((account) => (
              <AccountSelectCard
                key={account.id}
                bankName={account.bankName}
                maskedAccount={account.maskedAccount}
                isSelected={account.id === selectedId}
                onClick={() => setSelectedId(account.id)}
              />
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
