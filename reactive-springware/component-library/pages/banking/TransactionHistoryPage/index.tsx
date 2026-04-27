/**
 * @file index.tsx
 * @description 거래내역 조회 페이지 컴포넌트.
 *
 * Figma 원본:
 *   - 기본 상태      (node-id: 1:2201) — 계좌 카드 + 필터 접힘 + 거래 목록
 *   - 필터 펼침 상태 (node-id: 1:1881) — 조회 조건 설정 패널 오픈
 *
 * 화면 구성:
 *   - 상단 헤더: 뒤로가기 + "거래내역 조회" 타이틀 + 우측 메뉴 아이콘
 *   - 계좌 카드: 계좌명 + 계좌번호(브랜드 색) + 계좌 아이콘
 *   - 조회 조건 설정 패널 (TransactionSearchFilter):
 *       · 접힘: 적용 기간 요약 표시
 *       · 펼침: 퀵 기간 탭 / 날짜 직접 입력 / 정렬 순서 / 거래 유형 / 조회 버튼
 *   - 거래 내역 목록 (TransactionList) — 날짜별 그룹 헤더 + 항목
 *   - 하단 더보기 버튼
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 상태와 핸들러는 useTransactionHistory 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *   - 여기서는 Storybook 시각 확인 목적으로만 예외 적용한다.
 *
 * @param initialState   - 초기 렌더링 상태 (Storybook args 제어용)
 * @param filterExpanded - 조회 조건 설정 패널 초기 펼침 여부
 */
import React, { useState } from 'react';
import { Menu, BookOpen, ChevronDown } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack }      from '../../../layout/Stack';

/* ── Core ────────────────────────────────────────────────────── */
import { Button } from '../../../core/Button';

/* ── Modules ─────────────────────────────────────────────────── */
import { Card, CardHeader }             from '../../../modules/common/Card';
import { TransactionList }              from '../../../modules/banking/TransactionList';
import { TransactionSearchFilter }      from '../../../modules/banking/TransactionSearchFilter';
import { EmptyState }                   from '../../../modules/common/EmptyState';
import type { TransactionItem }         from '../../../modules/banking/TransactionList/types';
import type { TransactionSearchParams } from '../../../modules/banking/TransactionSearchFilter/types';

import type { TransactionHistoryPageProps, TransactionHistoryPageState } from './types';

// ── Mock 데이터 (Storybook 전용) ───────────────────────────────

/** Figma 시안 기준 거래 내역 샘플 데이터 — 1페이지 */
const MOCK_TRANSACTIONS_PAGE1: TransactionItem[] = [
  { id: 'txn-001', date: '2023-11-01T14:20:05', title: '스타벅스',    amount: 5400,    balance: 2994600, type: 'withdrawal' },
  { id: 'txn-002', date: '2023-11-01T09:00:12', title: '급여',        amount: 2500000, balance: 3000000, type: 'deposit'    },
  { id: 'txn-003', date: '2023-10-31T21:15:40', title: 'GS25 강남점', amount: 3200,    balance: 500000,  type: 'withdrawal' },
  { id: 'txn-004', date: '2023-10-31T18:30:22', title: '쿠팡결제',    amount: 28900,   balance: 503200,  type: 'withdrawal' },
  { id: 'txn-005', date: '2023-10-31T12:45:10', title: '이하나',      amount: 50000,   balance: 532100,  type: 'deposit'    },
];

/** 더보기 클릭 시 추가로 불러오는 2페이지 데이터 */
const MOCK_TRANSACTIONS_PAGE2: TransactionItem[] = [
  { id: 'txn-006', date: '2023-10-30T17:10:00', title: '배달의민족',  amount: 18500,   balance: 482100,  type: 'withdrawal' },
  { id: 'txn-007', date: '2023-10-30T10:05:33', title: '카카오페이',  amount: 30000,   balance: 500600,  type: 'deposit'    },
  { id: 'txn-008', date: '2023-10-29T22:44:11', title: '넷플릭스',    amount: 17000,   balance: 470600,  type: 'withdrawal' },
  { id: 'txn-009', date: '2023-10-29T09:00:00', title: '이체',        amount: 100000,  balance: 487600,  type: 'deposit'    },
  { id: 'txn-010', date: '2023-10-28T15:20:55', title: 'CU 편의점',   amount: 4200,    balance: 387600,  type: 'withdrawal' },
];

/** 기본 조회 조건 */
const DEFAULT_FILTER: TransactionSearchParams = {
  startDate:       '2023-10-01',
  endDate:         '2023-11-01',
  sortOrder:       'recent',
  transactionType: 'all',
};

// ── 서브 컴포넌트 ──────────────────────────────────────────────

/**
 * 계좌 정보 카드.
 * 계좌명 + 계좌번호(브랜드 색) + 우측 계좌 아이콘을 표시한다.
 */
function AccountCard() {
  return (
    /* px-standard: Card의 w-full과 mx-standard를 같이 쓰면 width 100% + margin이 되어
       부모 너비를 초과한다. 부모 컨테이너에 padding을 주고 Card는 w-full로 채운다. */
    <div className="px-standard">
      <Card>
        <CardHeader
          title="하나 주거래 통장"
          subtitle="123-456-789012"
          action={
            /* 계좌 아이콘 — 브랜드 배경 원형 컨테이너 */
            <span className="flex items-center justify-center size-12 rounded-full bg-brand-10 text-brand-text">
              <BookOpen className="size-5" aria-hidden="true" />
            </span>
          }
        />
      </Card>
    </div>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

export function TransactionHistoryPage({
  initialState   = 'data',
  filterExpanded = false,
}: TransactionHistoryPageProps) {
  /* 스토리북 전용 — 실제 앱에서는 useTransactionHistory 훅에서 받아야 함 */
  const [pageState,      setPageState]      = useState<TransactionHistoryPageState>(initialState);
  /* displayedItems: 현재 화면에 표시 중인 거래 목록. 더보기 클릭 시 다음 페이지를 append한다. */
  const [displayedItems, setDisplayedItems] = useState<TransactionItem[]>(MOCK_TRANSACTIONS_PAGE1);
  /* hasMore: 아직 불러오지 않은 데이터가 있으면 true. false가 되면 더보기 버튼을 숨긴다. */
  const [hasMore,        setHasMore]        = useState(true);
  const [searchParams,   setSearchParams]   = useState<TransactionSearchParams>(DEFAULT_FILTER);

  const isLoading = pageState === 'loading';
  const isError   = pageState === 'error';
  const isEmpty   = pageState === 'empty';
  const items     = isEmpty || isError ? [] : displayedItems;

  /** 더보기 클릭: 다음 페이지 데이터를 append하고, 마지막 페이지면 버튼을 숨긴다. */
  const handleLoadMore = () => {
    setDisplayedItems(prev => [...prev, ...MOCK_TRANSACTIONS_PAGE2]);
    setHasMore(false); /* mock 데이터는 2페이지가 마지막이므로 이후엔 버튼 숨김 */
  };

  return (
    <div data-brand="hana" data-domain="banking">
      <PageLayout
        title="거래내역 조회"
        onBack={() => console.log('뒤로가기')}
        rightAction={
          <button
            type="button"
            aria-label="메뉴 열기"
            className="flex items-center justify-center size-9 rounded-lg text-text-muted hover:bg-surface-raised transition-colors duration-150"
          >
            <Menu className="size-5" aria-hidden="true" />
          </button>
        }
      >
        <Stack gap="xs">
          {/* ── 계좌 카드 ── */}
          <section className="pt-standard pb-xs bg-surface">
            <AccountCard />
          </section>

          {/* ── 조회 조건 설정 패널 ──
              클릭 시 퀵 기간 탭 / 날짜 입력 / 정렬·유형 드롭다운 / 조회 버튼이 펼쳐진다.
              조회 버튼 클릭 시 searchParams가 업데이트되고 패널이 자동으로 닫힌다. */}
          <section>
            <TransactionSearchFilter
              value={searchParams}
              onSearch={(params) => {
                setSearchParams(params);
                console.log('조회 실행:', params);
              }}
              defaultExpanded={filterExpanded}
            />
          </section>

          {/* ── 거래 내역 목록 ── */}
          <section>
            {/* 에러 상태 */}
            {isError && (
              <div className="px-standard py-2xl" role="alert">
                <EmptyState
                  title="거래 내역을 불러오지 못했습니다"
                  description="잠시 후 다시 시도해 주세요"
                  action={
                    <Button variant="outline" size="sm" onClick={() => setPageState('data')}>
                      다시 시도
                    </Button>
                  }
                />
              </div>
            )}

            {/* 로딩 / 빈 목록 / 데이터 상태는 TransactionList 내부에서 처리 */}
            {!isError && (
              <TransactionList
                items={items}
                loading={isLoading}
                emptyMessage="조회된 거래 내역이 없어요"
                onItemClick={(item) => console.log('거래 상세 열기:', item.id)}
                dateHeaderFormat="year-month-day"
              />
            )}
          </section>

          {/* ── 더보기 버튼 ── */}
          {hasMore && !isLoading && !isError && !isEmpty && (
            <section className="flex justify-center py-lg pb-2xl">
              <Button
                variant="ghost"
                size="sm"
                rightIcon={<ChevronDown className="size-4" aria-hidden="true" />}
                onClick={handleLoadMore}
              >
                더보기
              </Button>
            </section>
          )}
        </Stack>
      </PageLayout>
    </div>
  );
}
