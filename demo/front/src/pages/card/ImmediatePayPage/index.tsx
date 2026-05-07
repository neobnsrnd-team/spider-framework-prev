/**
 * @file index.tsx
 * @description 즉시결제 페이지 컴포넌트 (STEP 1 — 결제정보 선택).
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / '즉시결제' 타이틀 / 닫기(X) 버튼
 *   - STEP 1 레이블 + 안내 문구
 *   - 결제 유형 선택 버튼 그룹: 총 이용금액 결제 | 이용건별 결제
 *   - 청구단위 카드 Select
 *   - 청구단위 보유카드 아코디언 (카드명 + 마스킹된 카드번호)
 *
 * @param cards               - 보유 카드 목록 (Select 옵션 + 아코디언에 공통 사용)
 * @param initialCardId       - 초기 선택 카드 id
 * @param initialPaymentType  - 초기 결제 유형 (기본: 'total')
 * @param onPaymentTypeChange - 결제 유형 변경 핸들러
 * @param onCardChange        - 카드 변경 핸들러
 * @param onBack              - 뒤로가기 핸들러
 * @param onClose             - 닫기(X) 핸들러
 * @param onNext              - 다음 버튼 클릭 핸들러
 */
import React, { useState } from "react";
import { X } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { Typography } from "@cl/core/Typography";
import { Select } from "@cl/core/Select";
import { TabNav } from "@cl/modules/common/TabNav";
import { CollapsibleSection } from "@cl/modules/common/CollapsibleSection";
import { StepIndicator } from "@cl/modules/common/StepIndicator";
import { CardChipItem } from "@cl/biz/card/CardChipItem";
import { Modal } from "@cl/modules/common/Modal";

import type { ImmediatePayPageProps, PaymentType } from "./types";

/** 결제 유형 탭 항목 */
const PAYMENT_TYPE_TABS = [
  { id: "total", label: "총 이용금액 결제" },
  { id: "per-item", label: "이용건별 결제" },
] as const;

export function ImmediatePayPage({
  cards,
  initialCardId,
  initialPaymentType = "total",
  onPaymentTypeChange,
  onCardChange,
  onBack,
  onClose,
  onNext,
}: ImmediatePayPageProps) {
  const [paymentType, setPaymentType] =
    useState<PaymentType>(initialPaymentType);
  const [selectedCardId, setSelectedCardId] = useState(
    /* 초기값 미전달 시 첫 번째 카드로 fallback */
    initialCardId ?? cards[0]?.id ?? "",
  );
  // 이용건별 결제 미지원 안내 Modal 표시 여부
  const [perItemModalOpen, setPerItemModalOpen] = useState(false);

  /** Select 옵션: 카드명 + 마스킹번호를 label로 조합 */
  const selectOptions = cards.map((c) => ({
    value: c.id,
    label: `${c.name} ${c.maskedNumber}`,
  }));

  function handlePaymentTypeChange(id: string) {
    // 이용건별 결제는 현재 미지원 — 탭 전환을 막고 Modal로 안내한다.
    if (id === "per-item") {
      setPerItemModalOpen(true);
      return;
    }
    const type = id as PaymentType;
    setPaymentType(type);
    onPaymentTypeChange?.(type);
  }

  function handleCardChange(value: string) {
    setSelectedCardId(value);
    onCardChange?.(value);
  }

  return (
    <>
      <Modal
        open={perItemModalOpen}
        onClose={() => setPerItemModalOpen(false)}
        title="안내"
        size="sm"
        titleAlign="center"
        className="text-sm text-center"
      >
        이용건별 결제는 현재 사용할 수 없습니다.
        <br />
        추후 업데이트를 통해 제공될 예정입니다.
      </Modal>
      <PageLayout
        title="즉시결제"
        onBack={onBack}
        bottomBar={
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => onNext?.(selectedCardId)}
          >
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
          {/* ── STEP 1 + 안내 문구 ────────────────────────────────── */}
          <div className="flex flex-col gap-xs">
            <Typography variant="caption" color="brand">
              STEP 1
            </Typography>
            <StepIndicator total={3} current={1} />
            <Typography variant="subheading" weight="bold" color="heading">
              신청하실 결제정보를 <br />
              선택해 주세요.
            </Typography>
          </div>

          {/* ── 결제 유형 선택 버튼 그룹 ──────────────────────────────
           * pill 탭으로 '총 이용금액 결제' / '이용건별 결제' 선택.
           * 선택된 탭이 강조(active) 된다. */}
          <TabNav
            items={PAYMENT_TYPE_TABS}
            activeId={paymentType}
            onTabChange={handlePaymentTypeChange}
            variant="pill"
            fullWidth
          />

          {/* ── 청구단위 카드 Select ───────────────────────────────── */}
          <div className="flex flex-col gap-xs">
            <Typography variant="caption" color="muted">
              청구단위
            </Typography>
            <Select
              options={selectOptions}
              value={selectedCardId}
              onChange={handleCardChange}
              aria-label="청구단위 카드 선택"
            />
          </div>

          {/* ── 청구단위 보유카드 아코디언 ────────────────────────────
           * 보유 카드 목록을 기본 펼침 상태로 표시해
           * 사용자가 어떤 카드가 있는지 바로 확인할 수 있게 한다. */}
          <CollapsibleSection
            header={
              <Typography variant="body-sm" weight="bold" color="heading">
                청구단위 보유카드
              </Typography>
            }
            defaultExpanded
            headerAlign="left"
          >
            {/* 청구단위 Select에서 선택된 카드 한 장만 표시 */}
            {cards
              .filter((c) => c.id === selectedCardId)
              .map((card) => (
                <CardChipItem
                  key={card.id}
                  name={card.name}
                  maskedNumber={card.maskedNumber}
                />
              ))}
          </CollapsibleSection>
        </div>
      </PageLayout>
    </>
  );
}
