/**
 * @file index.tsx
 * @description 카드 대시보드 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: "마이" 타이틀 + 알림·메뉴 아이콘
 *   - StatementHeroCard: 이번 달 명세서 금액 + 결제일 (클릭 시 상세 이동)
 *   - LoanMenuBar: 단기카드대출 / 장기카드대출 / 리볼빙 진입 버튼
 *   - SummaryCard (asset): 총 자산 — 내 계좌·금융진단·보험진단 액션
 *   - SummaryCard (spending): 이번 달 소비 — 가계부·소비브리핑·고정지출 액션
 *   - QuickMenuGrid: 카드별 실적 / 이용내역 / 보유카드 / 쿠폰함 / 한도조회 / 무이자할부 / 카드신청 (4열, 사각형 아이콘)
 *   - QuickShortcutCard × 3: 카드추천 / 금융·대출 / 보험 (2열 그리드)
 *   - BannerCarousel: 이벤트 배너 3개 (자동 슬라이드)
 *   - 하단 탭바: 마이 / 혜택·실적 / 결제 / 쇼핑·여행 / 자산
 *
 * Storybook 확인 목적으로 내부 useState 사용.
 * 실제 앱 구현 시 모든 상태·핸들러는 useCardDashboard Hook으로 분리한다.
 *
 * @param onNotification   - 알림 클릭
 * @param onMenu           - 메뉴 클릭
 * @param onStatementDetail - 명세서 상세 이동
 * @param onCardRecommend  - 카드추천 클릭
 * @param onFinanceLoan    - 금융/대출 클릭
 * @param onInsurance      - 보험 클릭
 * @param onCardPerformance - 카드별 실적 클릭
 * @param onUsageHistory   - 이용내역 클릭
 * @param onMyCards        - 보유카드 클릭
 * @param onCoupons        - 쿠폰함 클릭
 * @param onLimitCheck     - 한도조회 클릭
 * @param onInstallment    - 무이자할부 클릭
 * @param onCardApply      - 카드신청 클릭
 * @param activeBottomTab  - 하단 탭바 활성 ID (기본: 'my')
 * @param onBottomNavChange - 하단 탭바 탭 변경 핸들러
 */
import React, { useState } from "react";
import {
  Bell,
  Menu,
  Star,
  Banknote,
  Shield,
  RefreshCw,
  BarChart2,
  List,
  CreditCard,
  Ticket,
  Gauge,
  Percent,
  Plus,
  User,
  Gift,
  Wallet,
  ShoppingBag,
  PieChart,
  Building2,
} from "lucide-react";

import { HomePageLayout } from "@cl/layout/HomePageLayout";
import { Button } from "@cl/core/Button";
import { SectionHeader } from "@cl/modules/common/SectionHeader";
import { StatementHeroCard } from "@cl/biz/card/StatementHeroCard";
import { LoanMenuBar } from "@cl/biz/card/LoanMenuBar";
import { SummaryCard } from "@cl/biz/card/SummaryCard";
import { QuickMenuGrid } from "@cl/biz/common/QuickMenuGrid";
import { QuickShortcutCard } from "@cl/biz/card/QuickShortcutCard";
import { BannerCarousel } from "@cl/biz/common/BannerCarousel";
import { BalanceToggle } from "@cl/modules/common/BalanceToggle";

import type { CardDashboardPageProps } from "./types";
import { cn } from "@lib/cn";
import { Typography } from "@cl/core/Typography";

/** 하단 탭바 항목 정의 */
const BOTTOM_NAV_ITEMS = (onBottomNavChange: (id: string) => void) => [
  {
    id: "my",
    icon: <User className="size-5" />,
    label: "마이",
    onClick: () => onBottomNavChange("my"),
  },
  {
    id: "benefit",
    icon: <Gift className="size-5" />,
    label: "혜택/실적",
    onClick: () => onBottomNavChange("benefit"),
  },
  {
    id: "payment",
    icon: <Wallet className="size-5" />,
    label: "결제",
    onClick: () => onBottomNavChange("payment"),
  },
  {
    id: "shopping",
    icon: <ShoppingBag className="size-5" />,
    label: "쇼핑/여행",
    onClick: () => onBottomNavChange("shopping"),
  },
  {
    id: "asset",
    icon: <PieChart className="size-5" />,
    label: "자산",
    onClick: () => onBottomNavChange("asset"),
  },
];

export function CardDashboardPage({
  userName,
  statementAmount,
  statementDueDate,
  spendingAmount,
  onNotification,
  onMenu,
  onStatementDetail,
  onShortLoan,
  onLongLoan,
  onRevolving,
  onMyAccount,
  onDiagnosis,
  onInsuranceDiag,
  onHouseholdBook,
  onSpendingBriefing,
  onFixedExpenses,
  onCardRecommend,
  onFinanceLoan,
  onInsurance,
  onCardPerformance,
  onUsageHistory,
  onMyCards,
  onCoupons,
  onLimitCheck,
  onInstallment,
  onCardApply,
  activeBottomTab: activeBottomTabProp,
  onBottomNavChange,
}: CardDashboardPageProps) {
  /** Storybook 확인용 내부 상태 — 실제 앱에서는 Hook에서 관리 */
  const [activeBottomTab, setActiveBottomTab] = useState(
    activeBottomTabProp ?? "my",
  );
  /** 금액 숨김 여부 — 토글 버튼으로 대시보드 전체 금액을 일괄 마스킹 */
  const [amountHidden, setAmountHidden] = useState(false);

  const handleBottomNavChange = (id: string) => {
    setActiveBottomTab(id);
    onBottomNavChange?.(id);
  };

  /** QuickMenuGrid 항목 — iconShape="rounded"로 사각형 아이콘 */
  const quickMenuItems = [
    {
      id: "performance",
      icon: <BarChart2 className="size-5" />,
      label: "카드별 실적",
      onClick: onCardPerformance,
      iconShape: "rounded" as const,
    },
    {
      id: "history",
      icon: <List className="size-5" />,
      label: "이용내역",
      onClick: onUsageHistory,
      iconShape: "rounded" as const,
    },
    {
      id: "my-cards",
      icon: <CreditCard className="size-5" />,
      label: "보유카드",
      onClick: onMyCards,
      iconShape: "rounded" as const,
    },
    {
      id: "coupons",
      icon: <Ticket className="size-5" />,
      label: "쿠폰함",
      onClick: onCoupons,
      iconShape: "rounded" as const,
    },
    {
      id: "limit",
      icon: <Gauge className="size-5" />,
      label: "한도조회",
      onClick: onLimitCheck,
      iconShape: "rounded" as const,
    },
    {
      id: "installment",
      icon: <Percent className="size-5" />,
      label: "무이자할부",
      onClick: onInstallment,
      iconShape: "rounded" as const,
    },
    {
      id: "apply",
      icon: <Plus className="size-5" />,
      label: "카드신청",
      onClick: onCardApply,
      iconShape: "rounded" as const,
    },
  ];

  /** 헤더 아이콘 버튼 공통 스타일 */
  const iconBtnCls = cn(
    "flex items-center justify-center size-9 rounded-full",
    "text-text-muted hover:bg-surface-raised hover:text-text-heading",
    "transition-colors duration-150",
  );

  return (
    <div data-brand="hana" data-domain="card">
      <HomePageLayout
        title="마이"
        rightAction={
          /* 알림 + 메뉴 아이콘 2개 */
          <div className="flex items-center gap-xs">
            <Button
              variant="ghost"
              size="md"
              iconOnly
              leftIcon={<Bell className="size-5" />}
              onClick={onNotification}
              aria-label="알림"
              className={iconBtnCls}
            />
            <Button
              variant="ghost"
              size="md"
              iconOnly
              leftIcon={<Menu className="size-5" />}
              onClick={onMenu}
              aria-label="메뉴"
              className={iconBtnCls}
            />
          </div>
        }
        withBottomNav
        activeId={activeBottomTab}
        bottomNavItems={BOTTOM_NAV_ITEMS(handleBottomNavChange)}
      >
        {/* ── 인사말 ────────────────────────────────────── */}
        {userName && (
          <div className="pt-standard">
            <Typography variant="heading" color="heading">
              안녕하세요, {userName} 님!
            </Typography>
          </div>
        )}

        {/* ── 이번 달 명세서 히어로 카드 ────────────────── */}
        <div className="pt-standard">
          <StatementHeroCard
            amount={statementAmount ?? 0}
            dueDate={statementDueDate ?? '—'}
            onDetail={onStatementDetail}
            hidden={amountHidden}
          />
        </div>

        {/* ── 대출 메뉴 바 (단기카드대출 / 장기카드대출 / 리볼빙) ── */}
        <div className="pt-standard">
          <LoanMenuBar
            items={[
              {
                id: "short-loan",
                icon: <CreditCard size={14} />,
                label: "단기카드대출(현금서비스)",
                onClick: onShortLoan,
              },
              {
                id: "long-loan",
                icon: <Banknote size={14} />,
                label: "장기카드대출(카드론)",
                onClick: onLongLoan,
              },
              {
                id: "revolving",
                icon: <RefreshCw size={14} />,
                label: "일부결제금액이월약정(리볼빙)",
                onClick: onRevolving,
              },
            ]}
          />
        </div>

        {/* ── 총 자산 요약 카드 ──────────────────────────── */}
        <div className="pt-standard">
          <SummaryCard
            variant="asset"
            title="총 자산"
            amount={42_850_000}
            hidden={amountHidden}
            icon={<Building2 size={36} />}
            actions={[
              { label: "내 계좌", onClick: onMyAccount ?? (() => {}) },
              { label: "금융진단", onClick: onDiagnosis ?? (() => {}) },
              { label: "보험진단", onClick: onInsuranceDiag ?? (() => {}) },
            ]}
          />
        </div>

        {/* ── 이번 달 소비 요약 카드 ─────────────────────── */}
        <div className="pt-standard">
          <SummaryCard
            variant="spending"
            title="이번 달 소비"
            amount={spendingAmount ?? 0}
            hidden={amountHidden}
            icon={<Wallet size={32} />}
            actions={[
              { label: "가계부", onClick: onHouseholdBook ?? (() => {}) },
              {
                label: "소비브리핑",
                onClick: onSpendingBriefing ?? (() => {}),
                active: true,
              },
              { label: "고정지출", onClick: onFixedExpenses ?? (() => {}) },
            ]}
          />
        </div>

        {/* ── 퀵메뉴 그리드 4열 (사각형 아이콘) ──────────── */}
        <div className="pt-standard">
          <QuickMenuGrid items={quickMenuItems} cols={4} />
        </div>

        {/* ── 바로가기 카드 2열 (카드추천 / 금융·대출 / 보험) ── */}
        <div className="pt-standard">
          <div className="grid grid-cols-2 gap-sm">
            <QuickShortcutCard
              title="카드추천"
              subtitle="맞춤 추천"
              icon={<Star className="size-5" />}
              onClick={onCardRecommend}
            />
            <QuickShortcutCard
              title="금융/대출"
              subtitle="한도 조회"
              icon={<Banknote className="size-5" />}
              onClick={onFinanceLoan}
            />
            <QuickShortcutCard
              title="보험"
              subtitle="내 보험 확인"
              icon={<Shield className="size-5" />}
              onClick={onInsurance}
            />
          </div>
        </div>

        {/* ── 이벤트 배너 (3개, 자동 슬라이드) ───────────── */}
        <div className="pt-standard pb-standard">
          <BannerCarousel
            items={[
              {
                id: "event-1",
                variant: "promo",
                title: "카드 신규 발급 이벤트",
                description: "첫 달 연회비 면제 + 캐시백 최대 3만원",
              },
              {
                id: "event-2",
                variant: "promo",
                title: "무이자 할부 혜택",
                description: "5만원 이상 결제 시 2~3개월 무이자",
              },
              {
                id: "event-3",
                variant: "promo",
                title: "카드 이용 실적 안내",
                description: "이번 달 실적을 확인하고 혜택을 챙겨보세요",
              },
            ]}
          />
        </div>

        {/* ── 금액 보기/숨기기 — 스크롤 콘텐츠 맨 아래 ── */}
        <div className="flex justify-center py-sm">
          <BalanceToggle
            hidden={amountHidden}
            onToggle={() => setAmountHidden((prev) => !prev)}
          />
        </div>
      </HomePageLayout>

    </div>
  );
}
