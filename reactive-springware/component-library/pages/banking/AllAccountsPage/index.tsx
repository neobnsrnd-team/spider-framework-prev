/**
 * @file index.tsx
 * @description 전계좌 조회 페이지 컴포넌트.
 *
 * Figma 원본:
 *   - 해당금융 탭 (node-id: 1:3)  — 내 하나은행 계좌 목록 + 세그먼트 필터
 *   - 다른금융 탭 (node-id: 1:152) — 연결된 타행 계좌 없음 (빈 상태)
 *
 * 화면 구성:
 *   - 상단 헤더: 뒤로가기 + "전계좌 조회" 타이틀
 *   - TabNav (underline): 해당금융 | 다른금융
 *   - 해당금융 탭:
 *       · 세그먼트 TabNav (pill): 전체 | 예금 | 신탁 | 펀드 | 대출
 *       · CollapsibleSection: 예금 그룹 (계좌 2개 + 거래내역·이체 버튼)
 *       · CollapsibleSection: 외화예금 그룹 (계좌 1개)
 *       · CollapsibleSection: 퇴직연금 그룹 (빈 상태)
 *       · CollapsibleSection: 증권 그룹 (미보유 상태)
 *   - 다른금융 탭:
 *       · EmptyState + 연결하기 버튼
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 상태와 핸들러는 useAllAccounts 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *   - 여기서는 Storybook 시각 확인 목적으로만 예외 적용한다.
 *
 * @param initialTab    - 초기 활성 탭 ('mine' | 'other')
 * @param initialState  - 초기 렌더링 상태 (Storybook args 제어용)
 * @param onBack        - 뒤로가기 핸들러
 * @param onConnectAccount  - 연결하기 버튼 핸들러
 * @param onAccountClick    - 계좌 카드 클릭 핸들러
 * @param onTransactionHistory - 거래내역 버튼 핸들러
 * @param onTransfer    - 이체 버튼 핸들러
 */
import React, { useState } from 'react';
import { Landmark, Link, ChevronRight } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack } from '../../../layout/Stack';
import { Inline } from '../../../layout/Inline';

/* ── Core ────────────────────────────────────────────────────── */
import { Button } from '../../../core/Button';
import { Typography } from '../../../core/Typography';

/* ── Modules ─────────────────────────────────────────────────── */
import { TabNav } from '../../../modules/common/TabNav';
import { CollapsibleSection } from '../../../modules/common/CollapsibleSection';
import { EmptyState } from '../../../modules/common/EmptyState';

/* ── Biz ─────────────────────────────────────────────────────── */
import { AccountSummaryCard } from '../../../biz/banking/AccountSummaryCard';

import type { AllAccountsPageProps, AllAccountsTab, AccountSegment } from './types';

// ── 상단 탭 정의 ──────────────────────────────────────────────

const TOP_TABS: { id: AllAccountsTab; label: string }[] = [
  { id: 'mine', label: '해당금융' },
  { id: 'other', label: '다른금융' },
];

// ── 세그먼트 탭 정의 ──────────────────────────────────────────

const SEGMENT_TABS: { id: AccountSegment; label: string }[] = [
  { id: 'all', label: '전체' },
  { id: 'deposit', label: '예금' },
  { id: 'trust', label: '신탁' },
  { id: 'fund', label: '펀드' },
  { id: 'loan', label: '대출' },
];

// ── Mock 데이터 (Storybook 전용) ──────────────────────────────

/** 예금 계좌 샘플 (2개) */
const MOCK_DEPOSIT_ACCOUNTS = [
  {
    id: 'acc-001',
    type: 'deposit' as const,
    accountName: '하나 청년도약계좌',
    accountNumber: '180-910058-09304',
    balance: 1000000,
    badgeText: '주거래',
  },
  {
    id: 'acc-002',
    type: 'deposit' as const,
    accountName: '하나 자유적금',
    accountNumber: '180-910058-12201',
    balance: 500000,
  },
];

/** 펀드 계좌 샘플 (1개) */
const MOCK_FUND_ACCOUNTS = [
  {
    id: 'acc-004',
    type: 'securities' as const,
    accountName: '하나 글로벌성장펀드',
    accountNumber: '180-930058-00501',
    balance: 1500000,
    balanceLabel: '평가금액',
  },
];

/** 대출 계좌 샘플 (1개) */
const MOCK_LOAN_ACCOUNTS = [
  {
    id: 'acc-005',
    type: 'loan' as const,
    accountName: '하나 신용대출',
    accountNumber: '180-940058-00701',
    balance: 5000000,
  },
];

// ── 계좌 그룹 섹션 헤더 ───────────────────────────────────────

/**
 * CollapsibleSection 헤더 구성 — 그룹명 + 배지 + 총 잔액 표시.
 *
 * Figma: 각 그룹 헤더는 "예금 [2] 1,500,000원" 형식으로 좌우 구분 레이아웃.
 * 계좌가 아예 없는 경우(미보유)에는 count 대신 badgeLabel("미보유")을 중립 배지로 표시한다.
 *
 * @param count      - 계좌 수 (숫자 배지). badgeLabel 전달 시 무시됨
 * @param badgeLabel - 숫자 대신 표시할 텍스트 배지 (예: "미보유")
 */
function AccountGroupHeader({
  title,
  count,
  totalAmount,
  badgeLabel,
}: {
  title: string;
  count?: number;
  totalAmount?: string;
  /** 숫자 배지 대신 표시할 텍스트 (예: "미보유") — 중립 회색 배지로 렌더링 */
  badgeLabel?: string;
}) {
  return (
    <Inline justify="between" align="center" className="w-full">
      {/* 그룹명 + 배지 */}
      <Inline gap="xs" align="center">
        <Typography variant="body" weight="bold" color="heading" as="span">
          {title}
        </Typography>
        {badgeLabel ? (
          /* 미보유 등 텍스트 배지 — 회색 중립 스타일 */
          <span className="inline-flex items-center rounded-full px-xs py-0.5 text-xs font-medium bg-surface-raised text-text-muted">
            {badgeLabel}
          </span>
        ) : count !== undefined ? (
          /* 계좌 수 숫자 배지 — 브랜드 색상 */
          <span className="inline-flex items-center rounded-full px-xs py-0.5 text-xs font-bold bg-brand-10 text-brand-text">
            {count}
          </span>
        ) : null}
      </Inline>
      {/* 총 잔액 — 우측 표시, 숫자는 numeric 폰트 적용 */}
      {totalAmount && (
        <Typography variant="body-sm" color="secondary" numeric as="span">
          {totalAmount}
        </Typography>
      )}
    </Inline>
  );
}

// ── 세그먼트-그룹 펼침 매핑 ──────────────────────────────────

/**
 * 세그먼트 탭 선택에 따라 계좌 그룹을 펼칠지 결정한다.
 *
 * - 전체: 모든 그룹 펼침
 * - 예금·신탁·펀드·대출: 선택된 세그먼트와 일치하는 그룹만 펼침
 */
function isGroupExpanded(
  segment: AccountSegment,
  group: 'deposit' | 'trust' | 'fund' | 'loan',
): boolean {
  if (segment === 'all') return true;
  return segment === group;
}

// ── 해당금융 탭 콘텐츠 ────────────────────────────────────────

function MineTabContent({
  activeSegment,
  onSegmentChange,
  onAccountClick,
  onTransactionHistory,
  onTransfer,
}: {
  activeSegment: AccountSegment;
  onSegmentChange: (id: AccountSegment) => void;
  onAccountClick?: (id: string) => void;
  onTransactionHistory?: (id: string) => void;
  onTransfer?: (id: string) => void;
}) {
  return (
    <Stack gap="sm">
      {/* 세그먼트 탭: 예금·신탁·펀드·대출 유형 필터 */}
      <div className="bg-white rounded-lg px-md py-sm">
        <TabNav
          items={SEGMENT_TABS}
          activeId={activeSegment}
          onTabChange={(id) => onSegmentChange(id as AccountSegment)}
          variant="pill"
          fullWidth
        />
      </div>

      {/* ── 예금 그룹 ─────────────────────────────────────────── */}
      {/* key: 세그먼트 변경 시 remount → defaultExpanded 재적용 */}
      <CollapsibleSection
        key={`deposit-${activeSegment}`}
        header={
          <AccountGroupHeader
            title="예금"
            count={MOCK_DEPOSIT_ACCOUNTS.length}
            totalAmount="1,500,000원"
          />
        }
        defaultExpanded={isGroupExpanded(activeSegment, 'deposit')}
      >
        <Stack gap="sm">
          {MOCK_DEPOSIT_ACCOUNTS.map((account) => (
            <AccountSummaryCard
              key={account.id}
              type={account.type}
              accountName={account.accountName}
              accountNumber={account.accountNumber}
              balance={account.balance}
              badgeText={account.badgeText}
              onClick={() => onAccountClick?.(account.id)}
              actions={
                <>
                  <Button
                    size="sm"
                    variant="outline"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransactionHistory?.(account.id);
                    }}
                  >
                    거래내역
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransfer?.(account.id);
                    }}
                  >
                    이체
                  </Button>
                </>
              }
            />
          ))}
        </Stack>
      </CollapsibleSection>

      {/* ── 신탁 그룹 — 미보유 빈 상태 ──────────────────────────── */}
      <CollapsibleSection
        key={`trust-${activeSegment}`}
        header={
          /* 계좌 없음 → 숫자 배지 대신 "미보유" 텍스트 배지로 표시 */
          <AccountGroupHeader title="신탁" badgeLabel="미보유" />
        }
        defaultExpanded={isGroupExpanded(activeSegment, 'trust')}
      >
        <EmptyState
          title="보유하신 신탁 계좌가 없습니다."
          action={
            <Button
              variant="ghost"
              size="sm"
              rightIcon={<ChevronRight className="size-4" aria-hidden="true" />}
            >
              신탁 가입하기
            </Button>
          }
        />
      </CollapsibleSection>

      {/* ── 펀드 그룹 ─────────────────────────────────────────── */}
      <CollapsibleSection
        key={`fund-${activeSegment}`}
        header={
          <AccountGroupHeader
            title="펀드"
            count={MOCK_FUND_ACCOUNTS.length}
            totalAmount="1,500,000원"
          />
        }
        defaultExpanded={isGroupExpanded(activeSegment, 'fund')}
      >
        <Stack gap="sm">
          {MOCK_FUND_ACCOUNTS.map((account) => (
            <AccountSummaryCard
              key={account.id}
              type={account.type}
              accountName={account.accountName}
              accountNumber={account.accountNumber}
              balance={account.balance}
              balanceLabel={account.balanceLabel}
              onClick={() => onAccountClick?.(account.id)}
              actions={
                <>
                  <Button
                    size="sm"
                    variant="outline"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransactionHistory?.(account.id);
                    }}
                  >
                    거래내역
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransfer?.(account.id);
                    }}
                  >
                    이체
                  </Button>
                </>
              }
            />
          ))}
        </Stack>
      </CollapsibleSection>

      {/* ── 대출 그룹 ─────────────────────────────────────────── */}
      <CollapsibleSection
        key={`loan-${activeSegment}`}
        header={
          <AccountGroupHeader
            title="대출"
            count={MOCK_LOAN_ACCOUNTS.length}
            totalAmount="5,000,000원"
          />
        }
        defaultExpanded={isGroupExpanded(activeSegment, 'loan')}
      >
        <Stack gap="sm">
          {MOCK_LOAN_ACCOUNTS.map((account) => (
            <AccountSummaryCard
              key={account.id}
              type={account.type}
              accountName={account.accountName}
              accountNumber={account.accountNumber}
              balance={account.balance}
              onClick={() => onAccountClick?.(account.id)}
              actions={
                <>
                  <Button
                    size="sm"
                    variant="outline"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransactionHistory?.(account.id);
                    }}
                  >
                    거래내역
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    fullWidth
                    onClick={(e) => {
                      e.stopPropagation();
                      onTransfer?.(account.id);
                    }}
                  >
                    이체
                  </Button>
                </>
              }
            />
          ))}
        </Stack>
      </CollapsibleSection>
    </Stack>
  );
}

// ── 다른금융 탭 콘텐츠 ────────────────────────────────────────

function OtherTabContent({
  activeSegment,
  onSegmentChange,
}: {
  activeSegment: AccountSegment;
  onSegmentChange: (id: AccountSegment) => void;
}) {
  return (
    // flex-1: PageLayout main(flex flex-col)에서 남은 높이를 모두 채워 EmptyState 세로 중앙 정렬을 가능하게 함
    <Stack gap="sm" className="flex-1">
      {/* 세그먼트 탭 — 해당금융과 동일하게 다른금융에도 표시 */}
      <div className="bg-white rounded-lg px-md py-sm">
        <TabNav
          items={SEGMENT_TABS}
          activeId={activeSegment}
          onTabChange={(id) => onSegmentChange(id as AccountSegment)}
          variant="pill"
          fullWidth
        />
      </div>

      {/* flex-1 + items-center: 세그먼트 탭 아래 남은 공간에서 EmptyState 세로 중앙 정렬 */}
      <div className="flex flex-1 items-center justify-center">
        <EmptyState
          illustration={<Landmark className="size-16 text-text-muted" aria-hidden="true" />}
          title="연결된 다른 금융 계좌가 없습니다."
          description="다른 금융사 계좌를 연결하면 한 곳에서 모든 계좌를 조회할 수 있어요."
        />
      </div>
    </Stack>
  );
}

// ── 로딩 스켈레톤 ─────────────────────────────────────────────

function LoadingSkeleton() {
  return (
    <Stack gap="sm">
      {/* 세그먼트 탭 스켈레톤 */}
      <div className="bg-white rounded-lg p-md animate-pulse">
        <div className="h-8 bg-border-subtle rounded-full" />
      </div>
      {/* 계좌 카드 스켈레톤 3개 */}
      {[1, 2, 3].map((i) => (
        <div key={i} className="bg-white rounded-lg p-md animate-pulse">
          <div className="h-4 w-1/3 bg-border-subtle rounded mb-sm" />
          <div className="h-3 w-1/2 bg-border-subtle rounded mb-md" />
          <div className="h-6 w-2/5 bg-border-subtle rounded" />
        </div>
      ))}
    </Stack>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

export function AllAccountsPage({
  initialTab = 'mine',
  initialState = 'data',
  onBack,
  onConnectAccount,
  onAccountClick,
  onTransactionHistory,
  onTransfer,
}: AllAccountsPageProps) {
  /* 상단 탭 상태 (Storybook 확인용 — 실제 앱에서는 Hook에서 관리) */
  const [activeTab, setActiveTab] = useState<AllAccountsTab>(initialTab);

  /* 세그먼트 탭 상태 (Storybook 확인용) */
  const [activeSegment, setActiveSegment] = useState<AccountSegment>('all');

  return (
    <PageLayout
      title="전계좌 조회"
      onBack={onBack}
      bottomBar={
        // 다른금융 탭일 때만 연결하기 버튼을 PageLayout 최하단 고정 바로 노출
        activeTab === 'other' ? (
          <Button
            variant="primary"
            size="lg"
            fullWidth
            leftIcon={<Link className="size-4" aria-hidden="true" />}
            onClick={onConnectAccount}
          >
            연결하기
          </Button>
        ) : undefined
      }
    >
      {/* flex-1: OtherTabContent가 남은 높이를 채워 EmptyState 세로 중앙 정렬을 위한 부모 flex 컨텍스트 */}
      <Stack gap="sm" className="flex-1">
        {/* 상단 탭: 해당금융 | 다른금융 */}
        <div className="bg-white -mx-standard px-standard">
          <TabNav
            items={TOP_TABS}
            activeId={activeTab}
            onTabChange={(id) => setActiveTab(id as AllAccountsTab)}
            variant="underline"
            fullWidth
          />
        </div>

        {/* 탭별 콘텐츠 렌더링 */}
        {activeTab === 'mine' && (
          <>
            {initialState === 'loading' && <LoadingSkeleton />}
            {initialState === 'data' && (
              <MineTabContent
                activeSegment={activeSegment}
                onSegmentChange={setActiveSegment}
                onAccountClick={onAccountClick}
                onTransactionHistory={onTransactionHistory}
                onTransfer={onTransfer}
              />
            )}
            {initialState === 'error' && (
              <EmptyState
                title="계좌 정보를 불러올 수 없어요"
                description="잠시 후 다시 시도해 주세요."
                action={
                  <Button variant="outline" onClick={() => {}}>
                    다시 시도
                  </Button>
                }
              />
            )}
          </>
        )}

        {activeTab === 'other' && (
          <OtherTabContent activeSegment={activeSegment} onSegmentChange={setActiveSegment} />
        )}
      </Stack>
    </PageLayout>
  );
}
