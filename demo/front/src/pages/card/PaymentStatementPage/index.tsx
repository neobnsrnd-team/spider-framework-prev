/**
 * @file index.tsx
 * @description 결제예정금액 / 이용대금명세서 통합 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / 활성 탭명 타이틀 / 닫기(X) 버튼
 *   - TabNav(underline, fullWidth): 결제예정금액 | 이용대금명세서
 *   - [결제예정금액 탭]
 *       CardPaymentSummary → CardInfoPanel → 카드별 금액(CardPaymentItem)
 *   - [이용대금명세서 탭]
 *       StatementTotalCard → 카드별 금액(CardPaymentItem) → CardInfoPanel
 *
 * 상태 관리 원칙:
 *   Storybook 확인용으로 내부 useState를 최소한으로 사용.
 *   실제 앱 구현 시 모든 상태·핸들러는 usePaymentStatement Hook으로 분리한다.
 *
 * @param initialTab          - 초기 활성 탭 ('payment' | 'statement', 기본: 'payment')
 * @param paymentData         - 결제예정금액 탭 데이터
 * @param statementData       - 이용대금명세서 탭 데이터
 * @param onDateClick         - 날짜 클릭 핸들러
 * @param onRevolving         - 리볼빙 버튼 클릭
 * @param onCardLoan          - 카드론 버튼 클릭
 * @param onCashAdvance       - 현금서비스 버튼 클릭
 * @param onStatementDetail   - 이용내역 화살표 클릭
 * @param onInstallment       - 분할납부 버튼 클릭
 * @param onImmediatePayment  - 즉시결제 버튼 클릭
 * @param onBack              - 뒤로가기 핸들러
 * @param onClose             - 닫기(X) 핸들러
 */
import { useState, useMemo } from "react";
import { X, ChevronDown } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { TabNav } from "@cl/modules/common/TabNav";
import { Divider } from "@cl/modules/common/Divider";
import { BottomSheet } from "@cl/modules/common/BottomSheet";
import { SelectableListItem } from "@cl/modules/common/SelectableListItem";
import { CollapsibleSection } from "@cl/modules/common/CollapsibleSection";
import { Typography } from "@cl/core/Typography";
import { EmptyState } from "@cl/modules/common/EmptyState";
import { CardPaymentSummary } from "@cl/biz/card/CardPaymentSummary";
import { CardInfoPanel } from "@cl/biz/card/CardInfoPanel";
import { CardPaymentItem } from "@cl/biz/card/CardPaymentItem";
import { StatementTotalCard } from "@cl/biz/card/StatementTotalCard";

import type { PaymentStatementPageProps, StatementTab } from "./types";
import { cn } from "@lib/cn";

/** 탭 메타 — id/label 한 곳에서 관리해 타이틀·TabNav 둘 다 참조 */
const TAB_ITEMS: { id: StatementTab; label: string }[] = [
  { id: "payment", label: "결제예정금액" },
  { id: "statement", label: "이용대금명세서" },
];

/** 월 옵션 단일 항목 */
interface MonthOption {
  /** 비교·키 용 값. 예: '2026-04' */
  value: string;
  /** CardPaymentSummary dateYM 표시용. 예: '26년 4월' */
  dateYM: string;
}

/**
 * 오늘 기준 이전 10개월 ~ 이후 2개월 옵션 생성 (총 13개).
 * 목록은 최신(이후)→과거 순으로 정렬된다.
 */
function generateMonthOptions(): MonthOption[] {
  const today = new Date();
  const options: MonthOption[] = [];
  /* offset +2 → -10 순서로 push → 최신이 위에 오도록 */
  for (let offset = 2; offset >= -10; offset--) {
    const d = new Date(today.getFullYear(), today.getMonth() + offset, 1);
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    options.push({
      value: `${year}-${String(month).padStart(2, "0")}`,
      /* 두 자리 연도 + 월. 예: '26년 4월' */
      dateYM: `${String(year).slice(2)}년 ${month}월`,
    });
  }
  return options;
}

/** 오늘 기준 현재 월 value. 예: '2026-04' */
function todayMonthValue(): string {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`;
}

/** 헤더 아이콘 버튼 공통 스타일 */
const iconBtnCls = cn(
  "flex items-center justify-center size-9 rounded-full",
  "text-text-muted hover:bg-surface-raised hover:text-text-heading",
  "transition-colors duration-150",
);

export function PaymentStatementPage({
  initialTab = "payment",
  initialMonth,
  paymentData,
  statementData,
  onDateClick,
  onRevolving,
  onCardLoan,
  onCashAdvance,
  onStatementDetail,
  onInstallment,
  onImmediatePayment,
  onBack,
  onClose,
}: PaymentStatementPageProps) {
  const [activeTab, setActiveTab] = useState<StatementTab>(initialTab);
  const [dateSheetOpen, setDateSheetOpen] = useState(false);
  /** 초기값: 전달받은 initialMonth 우선, 없으면 오늘 기준 월 */
  const [selectedMonth, setSelectedMonth] = useState(
    initialMonth ?? todayMonthValue(),
  );

  /** 월 옵션 목록 — 렌더마다 재계산하지 않도록 메모이제이션 */
  const monthOptions = useMemo(() => generateMonthOptions(), []);

  /** 현재 선택된 월의 dateYM 레이블. CardPaymentSummary에 전달 */
  const selectedDateYM =
    monthOptions.find((o) => o.value === selectedMonth)?.dateYM ??
    paymentData.dateYM;

  /** 활성 탭 레이블을 헤더 타이틀로 사용 */
  const activeLabel = TAB_ITEMS.find((t) => t.id === activeTab)?.label ?? "";

  function handleMonthSelect(option: MonthOption) {
    setSelectedMonth(option.value);
    setDateSheetOpen(false);
    /* 선택된 월 값('YYYY-MM')을 외부 핸들러에 전파 */
    onDateClick?.(option.value);
  }

  return (
    <>
      <PageLayout
        title={activeLabel}
        onBack={onBack}
        rightAction={
          <Button
            variant="ghost"
            size="md"
            iconOnly
            leftIcon={<X className="size-5" />}
            onClick={onClose}
            aria-label="닫기"
            className={iconBtnCls}
          />
        }
      >
        {/* ── 탭 + 조회 월 선택 버튼 — 스크롤 영역 최상단 ────────── */}
        <div className="flex flex-col pt-md pb-sm gap-sm">
          {/* underline 탭: 두 탭이 균등하게 너비를 나눔 */}
          <TabNav
            items={TAB_ITEMS}
            activeId={activeTab}
            onTabChange={(id) => setActiveTab(id as StatementTab)}
            variant="pill"
            fullWidth
          />
          {/* 조회 월 선택 버튼 — 가운데 정렬, 두 탭 공통 기간 필터 역할 */}
          <Button
            variant="ghost"
            size="md"
            rightIcon={<ChevronDown className="size-4" />}
            onClick={() => setDateSheetOpen(true)}
            className="mx-auto text-lg text-text-muted"
          >
            {selectedDateYM}
          </Button>
        </div>

        {/* ── 결제예정금액 탭 콘텐츠 ─────────────────────────────────── */}
        {activeTab === "payment" && (
          <div className="flex flex-col gap-lg pb-xl">
            {/* 총 청구금액 요약 카드 — 날짜 클릭 시 월 선택 BottomSheet 오픈 */}
            <CardPaymentSummary
              dateFull={paymentData.dateFull}
              dateYM={selectedDateYM}
              dateMD={paymentData.dateMD}
              totalAmount={paymentData.totalAmount}
              revolving={paymentData.revolving}
              cardLoan={paymentData.cardLoan}
              cashAdvance={paymentData.cashAdvance}
              hideDateButton
              onDateClick={() => setDateSheetOpen(true)}
              onRevolving={onRevolving}
              onCardLoan={onCardLoan}
              onCashAdvance={onCashAdvance}
            />

            <Divider />

            {/* 결제정보 · 카드 이용기간 안내 */}
            <CardInfoPanel sections={paymentData.infoSections} />

            {/* 카드별 금액 목록 — items가 없으면 빈 상태 메시지 표시 */}
            {paymentData.paymentItems.length > 0 ? (
              <div className="flex flex-col">
                <Typography
                  variant="body-sm"
                  weight="bold"
                  color="heading"
                  className="mb-xs"
                >
                  카드별 금액
                </Typography>
                <div className="flex flex-col divide-y divide-border-subtle">
                  {paymentData.paymentItems.map((item) => (
                    <CardPaymentItem
                      key={item.id}
                      icon={item.icon}
                      iconBgClassName={item.iconBgClassName}
                      cardEnName={item.cardEnName}
                      cardName={item.cardName}
                      amount={item.amount}
                      onDetailClick={item.onDetailClick}
                      onClick={item.onClick}
                    />
                  ))}
                </div>
              </div>
            ) : (
              /* 선택 청구월에 결제 예정 내역이 없는 경우 */
              <EmptyState
                title="결제 예정 내역이 없습니다"
                description="선택한 청구월에 청구된 금액이 없습니다."
              />
            )}
          </div>
        )}

        {/* ── 이용대금명세서 탭 콘텐츠 ──────────────────────────────── */}
        {activeTab === "statement" && (
          <div className="flex flex-col gap-lg pb-xl">
            {/* 총 결제금액 카드 — 분할납부·즉시결제·리볼빙 액션 포함 */}
            <StatementTotalCard
              amount={statementData.totalAmount}
              badge={statementData.badge}
              onDetailClick={onStatementDetail}
              onInstallment={onInstallment}
              onImmediatePayment={onImmediatePayment}
              onRevolving={onRevolving}
            />

            <Divider />

            {/* 카드별 금액 목록 — items가 없으면 빈 상태 메시지 표시 */}
            {statementData.paymentItems.length > 0 ? (
              <div className="flex flex-col">
                <Typography
                  variant="body-sm"
                  weight="bold"
                  color="heading"
                  className="mb-xs"
                >
                  카드별 금액
                </Typography>
                <div className="flex flex-col divide-y divide-border-subtle">
                  {statementData.paymentItems.map((item) => (
                    <CardPaymentItem
                      key={item.id}
                      icon={item.icon}
                      iconBgClassName={item.iconBgClassName}
                      cardEnName={item.cardEnName}
                      cardName={item.cardName}
                      amount={item.amount}
                      onDetailClick={item.onDetailClick}
                      onClick={item.onClick}
                    />
                  ))}
                </div>
              </div>
            ) : (
              /* 선택 청구월에 이용 내역이 없는 경우 */
              <EmptyState
                title="이용 내역이 없습니다"
                description="선택한 청구월에 이용한 내역이 없습니다."
              />
            )}

            {/* 결제정보 안내 */}
            <CardInfoPanel sections={statementData.infoSections} />

            <Divider />

            {/* ── 꼭! 알아두세요 — 아코디언 안내 섹션 ──────────────
             * 각 항목은 기본 접힘(defaultExpanded=false)으로 시작.
             * 실제 안내 본문은 운영 정책에 따라 교체한다. */}
            <div className="flex flex-col gap-xs">
              <Typography
                variant="body-sm"
                weight="bold"
                color="heading"
                className="mb-xs"
              >
                꼭! 알아두세요
              </Typography>

              {[
                {
                  title: "이용안내",
                  content:
                    "이용대금명세서는 매월 결제일 기준으로 발행됩니다. 결제 금액은 카드사 정책에 따라 변동될 수 있으며, 정확한 내용은 고객센터로 문의하시기 바랍니다.",
                  defaultExpanded: true,
                },
                {
                  title: "해외 이용안내",
                  content:
                    "해외에서 사용한 금액은 국제 브랜드사(Visa, Mastercard 등)의 환율 및 해외서비스 수수료가 적용됩니다. 결제일 기준 환율이 적용되어 실제 청구 금액이 달라질 수 있습니다.",
                },
                {
                  title: "일부결제금액이월약정(리볼빙) 안내",
                  content:
                    "리볼빙 이용 시 일정 금액만 결제하고 나머지는 다음 달로 이월됩니다. 이월된 금액에는 수수료가 부과되며, 장기 이용 시 이자 부담이 증가할 수 있습니다.",
                },
                {
                  title: "마이너스대출 안내",
                  content:
                    "마이너스대출(한도대출)은 승인된 한도 내에서 자유롭게 이용 가능합니다. 이용 금액에 대해 일별 이자가 발생하며, 약정된 이율이 적용됩니다.",
                },
              ].map(({ title, content }) => (
                <CollapsibleSection
                  key={title}
                  header={
                    <Typography
                      variant="body-sm"
                      weight="medium"
                      color="heading"
                    >
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
        )}
      </PageLayout>

      {/* ── 월 선택 BottomSheet ───────────────────────────────────────
       * 오늘 기준 이전 10개월 ~ 이후 2개월(총 13개)을 목록으로 표시.
       * 현재 선택된 월은 브랜드 색상으로 강조.
       * snap="auto": 13개 항목이 화면 90% 이내에 들어오므로 auto 사용. */}
      <BottomSheet
        open={dateSheetOpen}
        onClose={() => setDateSheetOpen(false)}
        title="조회 기간 선택"
        snap="auto"
      >
        <div className="flex flex-col">
          {monthOptions.map((option) => (
            <SelectableListItem
              key={option.value}
              label={option.dateYM}
              isSelected={option.value === selectedMonth}
              onClick={() => handleMonthSelect(option)}
            />
          ))}
        </div>
      </BottomSheet>
    </>
  );
}
