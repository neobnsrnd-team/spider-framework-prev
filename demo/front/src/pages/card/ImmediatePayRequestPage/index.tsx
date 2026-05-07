/**
 * @file index.tsx
 * @description 즉시결제 신청 페이지 (STEP 2).
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / '즉시결제' 타이틀 / 닫기(X)
 *   - 청구단위 보유카드 + 변경하기 버튼 (STEP 1로 복귀)
 *   - 이용구분 선택: 일시불 | 결제금액 (칩 토글)
 *   - 결제가능금액 영역 ('결제금액' 선택 시만 노출):
 *       결제금액 표시 / 결제할 금액 입력 / 전액 버튼
 *   - 꼭! 알아두세요 아코디언
 *   - 하단 고정 다음 버튼
 *
 * @param card           - STEP 1에서 선택된 카드 정보
 * @param payableAmount  - 결제가능금액 (원)
 * @param cautions       - 꼭! 알아두세요 항목 목록
 * @param onChangeCard   - 변경하기 클릭 (STEP 1로 복귀)
 * @param onNext         - 다음 클릭 (이용구분·결제금액 전달)
 * @param onBack         - 뒤로가기
 * @param onClose        - 닫기(X)
 */
import React, { useState } from "react";
import { X, CreditCard } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { Input } from "@cl/core/Input";
import { Typography } from "@cl/core/Typography";
import { CollapsibleSection } from "@cl/modules/common/CollapsibleSection";
import { Divider } from "@cl/modules/common/Divider";
import { StepIndicator } from "@cl/modules/common/StepIndicator";
import { TabNav } from "@cl/modules/common/TabNav";
import { LabelValueRow } from "@cl/modules/common/LabelValueRow";

import { formatAmount } from "@/utils/format";
import type { ImmediatePayRequestPageProps, PaymentType } from "./types";

/** 결제 유형 탭 항목 */
const PAYMENT_TYPE_TABS = [
  { id: "total", label: "총 이용금액 결제" },
  { id: "per-item", label: "이용건별 결제" },
] as const;

export function ImmediatePayRequestPage({
  initialPaymentType = "total",
  card,
  payableAmount,
  paymentBreakdown = [],
  amountHelperText,
  cautions,
  onChangeCard,
  onNext,
  onBack,
  onClose,
}: ImmediatePayRequestPageProps) {
  const [paymentType, setPaymentType] =
    useState<PaymentType>(initialPaymentType);
  /* 결제할 금액 — 숫자 문자열로 관리, 콤마 없이 저장 */
  const [payAmount, setPayAmount] = useState("");
  /* 금액 입력 에러 메시지. 빈 문자열이면 에러 없음 */
  const [amountError, setAmountError] = useState("");

  function handleAmountChange(e: React.ChangeEvent<HTMLInputElement>) {
    /* 숫자만 허용 */
    const digits = e.target.value.replace(/\D/g, "");
    setPayAmount(digits);

    /* [검증 3] 최대금액 초과 여부를 입력 즉시 검사 */
    if (digits && Number(digits) > payableAmount) {
      setAmountError(`결제가능금액(${formatAmount(payableAmount)})을 초과할 수 없습니다.`);
    } else {
      setAmountError("");
    }
  }

  function handleFullAmount() {
    setPayAmount(String(payableAmount));
    setAmountError("");
  }

  function handleAmountBlur() {
    /* 포커스 아웃 시 결제가능금액 초과면 결제가능금액으로 자동 보정 */
    if (payAmount && Number(payAmount) > payableAmount) {
      setPayAmount(String(payableAmount));
      setAmountError("");
    }
  }

  function handleNext() {
    const amount = Number(payAmount);

    /* [검증 1] 금액 미입력 */
    if (!payAmount) {
      setAmountError("결제할 금액을 입력해 주세요.");
      return;
    }
    /* [검증 2] 최소금액 (0원 이하) */
    if (amount <= 0) {
      setAmountError("1원 이상 입력해 주세요.");
      return;
    }
    /* [검증 3] 최대금액 초과 — 입력 중 이미 표시되나 제출 시도 시에도 차단 */
    if (amount > payableAmount) {
      setAmountError(`결제가능금액(${formatAmount(payableAmount)})을 초과할 수 없습니다.`);
      return;
    }

    /* 이용구분은 일시불 고정 — 현재 화면에서 다른 구분 선택 불가 */
    onNext?.("lump", amount);
  }

  return (
    <PageLayout
      title="즉시결제"
      onBack={onBack}
      bottomBar={
        <Button variant="primary" size="lg" fullWidth onClick={handleNext}>
          다음
        </Button>
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
        {/* ── STEP 2 레이블 ─────────────────────────────────────── */}
        <div className="flex flex-col gap-xs">
          <Typography variant="caption" color="brand">
            STEP 2
          </Typography>
          <StepIndicator total={3} current={2} />
          <Typography variant="subheading" weight="bold" color="heading">
            즉시결제를 신청해 주세요.
          </Typography>
        </div>

        {/* ── 청구단위 보유카드 ──────────────────────────────────
         * STEP 1에서 선택한 카드를 표시.
         * 변경하기 버튼으로 STEP 1로 돌아간다. */}
        <div className="flex flex-col gap-sm">
          <div className="flex items-center justify-between">
            <Typography variant="body-sm" weight="bold" color="heading">
              청구단위 보유카드
            </Typography>
            <Button variant="ghost" size="sm" onClick={onChangeCard}>
              변경하기
            </Button>
          </div>
          <div className="flex items-center gap-sm bg-surface rounded-xl px-md py-md shadow-card">
            <div className="flex items-center justify-center size-8 rounded-full bg-brand-10 text-brand shrink-0">
              <CreditCard className="size-4" aria-hidden="true" />
            </div>
            <div className="flex flex-col gap-xs">
              <Typography variant="body-sm" weight="bold" color="heading">
                {card.name}
              </Typography>
              <Typography variant="caption" color="muted">
                {card.maskedNumber}
              </Typography>
            </div>
          </div>
        </div>

        <Divider />

        {/* ── 이용구분 선택 ──────────────────────────────────────
         * 제목행: 좌측 "이용구분 선택" + 우측 "결제가능금액 N원"
         * 카드(세로 배치): 일시불(금액 표시) / 금액별 결제
         * 선택된 카드는 브랜드 색상 테두리·텍스트로 강조된다. */}
        <div className="flex flex-col gap-sm">
          {/* 제목 + 결제가능금액 한 행 */}
          <div className="flex items-center justify-between">
            <Typography variant="body-sm" weight="bold" color="heading">
              이용구분 선택
            </Typography>
            <Typography variant="caption" color="muted">
              결제가능금액 {formatAmount(payableAmount)}
            </Typography>
          </div>
          {/* 일시불 카드 — LabelValueRow로 좌/우 레이아웃, brand 스타일 wrapper */}
          <div className="px-md py-md rounded-lg border border-brand bg-brand-10">
            <LabelValueRow
              label="일시불"
              className="text-sm font-bold"
              value={
                <Typography variant="body-sm" weight="bold" color="brand">
                  {formatAmount(payableAmount)}
                </Typography>
              }
            />
          </div>
        </div>

        {/* ── 결제할 금액 입력 ────────────────────────────────────── */}
        <div className="flex flex-col gap-sm">
          <Typography variant="body-sm" weight="bold" color="heading">
            결제할 금액 입력
          </Typography>
          <Input
            value={payAmount ? Number(payAmount).toLocaleString("ko-KR") : ""}
            onChange={handleAmountChange}
            placeholder={`총 ${formatAmount(payableAmount)}`}
            inputMode="numeric"
            fullWidth
            /* 에러 발생 시 빨간 테두리 + 에러 문구, 정상 시 안내 문구 표시 */
            onBlur={handleAmountBlur}
            validationState={amountError ? "error" : "default"}
            helperText={amountError || amountHelperText}
            rightElement={
              <Button variant="outline" size="sm" onClick={handleFullAmount}>
                전액
              </Button>
            }
          />
        </div>

        <Divider />

        {/* ── 꼭! 알아두세요 아코디언 ──────────────────────────── */}
        <div className="flex flex-col gap-xs">
          <Typography
            variant="body-sm"
            weight="bold"
            color="heading"
            className="mb-xs"
          >
            꼭! 알아두세요
          </Typography>
          {cautions.map(({ title, content }) => (
            <CollapsibleSection
              key={title}
              header={
                <Typography variant="body-sm" weight="medium" color="heading">
                  {title}
                </Typography>
              }
              defaultExpanded={false}
              headerAlign="left"
              className="px-0! py-1!"
            >
              <Typography variant="caption" color="secondary">
                {content}
              </Typography>
            </CollapsibleSection>
          ))}
        </div>
      </div>
    </PageLayout>
  );
}
