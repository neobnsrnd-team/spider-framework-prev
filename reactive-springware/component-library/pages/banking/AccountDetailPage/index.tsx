/**
 * @file index.tsx
 * @description 계좌상세 페이지 컴포넌트.
 *
 * Figma 원본: node-id: 1:692
 *
 * 화면 구성:
 *   - 상단 헤더: 뒤로가기 + "계좌상세" 타이틀 + 메뉴 버튼
 *   - 계좌 정보 Hero 영역:
 *       · [예금 배지] [계좌번호] — 같은 줄 인라인
 *       · 계좌명
 *       · 잔액 (대형 숫자)
 *       · 출금가능액: X원 (단일 텍스트)
 *   - 예금자보호법 카드 박스 (제목 + 설명 2줄, 우측 ChevronRight)
 *   - 최근거래내역 섹션:
 *       · 헤더: "최근거래내역 (7일)" + 검색 아이콘 + "거래내역 더보기" 링크
 *       · TransactionList
 *       · 하단: "최근 7일간의 거래내역입니다."
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 상태와 핸들러는 useAccountDetail 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *   - 여기서는 Storybook 시각 확인 목적으로만 예외 적용한다.
 *
 * @param accountType        - 계좌 유형 (배지 텍스트·금액 색상 결정)
 * @param accountName        - 계좌명
 * @param accountNumber      - 계좌번호
 * @param balance            - 잔액 (원화 정수)
 * @param availableBalance   - 출금가능액 (원화 정수)
 * @param transactions       - 거래내역 목록
 * @param initialState       - 초기 렌더링 상태 (Storybook args 제어용)
 * @param onBack             - 뒤로가기 핸들러
 * @param onMenu             - 메뉴 버튼 핸들러
 * @param onInsuranceInfo    - 예금자보호 카드 클릭 핸들러
 * @param onTransactionSearch - 거래내역 검색 아이콘 클릭 핸들러
 * @param onTransactionMore  - "거래내역 더보기" 클릭 핸들러
 * @param onTransactionClick - 거래 항목 클릭 핸들러
 */
import React, { useState, useRef, useMemo } from 'react';
import { Menu, ChevronRight, Search, Calendar } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack } from '../../../layout/Stack';
import { Inline } from '../../../layout/Inline';

/* ── Core ────────────────────────────────────────────────────── */
import { Badge } from '../../../core/Badge';
import { Button } from '../../../core/Button';
import { Typography } from '../../../core/Typography';

/* ── Modules ─────────────────────────────────────────────────── */
import { Card } from '../../../modules/common/Card';
import { TransactionList } from '../../../modules/banking/TransactionList';
import { Divider } from '../../../modules/common/Divider';
import { DatePicker } from '../../../modules/common/DatePicker';

import type { TransactionItem } from '../../../modules/banking/TransactionList/types';
import type { AccountDetailPageProps, AccountDetailType } from './types';

// ── 원화 포맷터 ──────────────────────────────────────────────

const krwFormatter = new Intl.NumberFormat('ko-KR');

/** 숫자를 "N,NNN,NNN원" 형식으로 변환 */
function formatKrw(amount: number): string {
  return `${krwFormatter.format(amount)}원`;
}

// ── 계좌 유형 → 배지 텍스트 매핑 ────────────────────────────

const accountTypeBadgeLabel: Record<AccountDetailType, string> = {
  deposit: '예금',
  savings: '적금',
  loan: '대출',
  foreignDeposit: '외화예금',
  retirement: '퇴직연금',
  securities: '증권',
};

// ── Mock 데이터 (Storybook 전용) ──────────────────────────────

/** Figma 시안(node-id: 1:692) 기준 거래내역 샘플 데이터 */
const MOCK_TRANSACTIONS: TransactionItem[] = [
  /* 오늘 */
  {
    id: 'txn-001',
    date: '2024-10-24T14:30:00',
    title: '스타벅스',
    amount: 5500,
    balance: 2994500,
    type: 'withdrawal',
  },
  {
    id: 'txn-002',
    date: '2024-10-24T09:00:00',
    title: '급여',
    amount: 2500000,
    balance: 3000000,
    type: 'deposit',
  },
  /* 어제 */
  {
    id: 'txn-003',
    date: '2024-10-23T19:15:00',
    title: '배달의민족',
    amount: 18000,
    balance: 500000,
    type: 'withdrawal',
  },
  {
    id: 'txn-004',
    date: '2024-10-23T12:40:00',
    title: 'GS25',
    amount: 2400,
    balance: 518000,
    type: 'withdrawal',
  },
  /* 그저께 */
  {
    id: 'txn-005',
    date: '2024-10-22T10:20:00',
    title: '김철수',
    amount: 10000,
    balance: 520400,
    type: 'deposit',
  },
];

// ── 로딩 스켈레톤 ─────────────────────────────────────────────

function LoadingSkeleton() {
  return (
    <Stack gap="md">
      {/* Hero 영역 스켈레톤 */}
      <div className="animate-pulse py-md">
        <div className="flex gap-sm mb-sm">
          <div className="h-5 w-10 bg-border-subtle rounded-full" />
          <div className="h-5 w-28 bg-border-subtle rounded" />
        </div>
        <div className="h-4 w-48 bg-border-subtle rounded mb-md" />
        <div className="h-8 w-40 bg-border-subtle rounded mb-sm" />
        <div className="h-3 w-36 bg-border-subtle rounded" />
      </div>
      <Divider />
      {/* 거래내역 스켈레톤 */}
      {[1, 2, 3].map((i) => (
        <div key={i} className="animate-pulse flex justify-between py-sm">
          <div>
            <div className="h-3 w-24 bg-border-subtle rounded mb-xs" />
            <div className="h-3 w-16 bg-border-subtle rounded" />
          </div>
          <div className="text-right">
            <div className="h-3 w-20 bg-border-subtle rounded mb-xs" />
            <div className="h-3 w-24 bg-border-subtle rounded" />
          </div>
        </div>
      ))}
    </Stack>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

/** 날짜 → 'YYYY.MM.DD' 표시용 문자열 */
function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}.${m}.${day}`;
}

export function AccountDetailPage({
  accountType = 'deposit',
  accountName = '하나 주거래 통장',
  accountNumber = '123-456-789012',
  balance = 3000000,
  availableBalance = 3000000,
  transactions = MOCK_TRANSACTIONS,
  initialState = 'data',
  onBack,
  onMenu,
  onInsuranceInfo,
  onTransactionSearch,
  onTransactionMore,
  onTransactionClick,
}: AccountDetailPageProps) {
  /* 거래내역 기간 선택 DatePicker 상태 (Storybook 확인용 — 실제 앱에서는 Hook에서 관리) */
  const [datePickerOpen, setDatePickerOpen] = useState(false);
  const [dateRange, setDateRange] = useState<[Date | null, Date | null]>([null, null]);

  /* "거래내역 더보기" 버튼의 DOM 위치를 DatePicker 달력 패널 위치 계산에 사용 */
  const moreButtonRef = useRef<HTMLButtonElement>(null);

  /* 선택된 날짜 범위를 섹션 헤더 텍스트에 반영 */
  const transactionSectionTitle = useMemo(() => {
    const [start, end] = dateRange;
    if (start && end) return `거래내역 (${formatDate(start)} ~ ${formatDate(end)})`;
    return '최근거래내역 (7일)';
  }, [dateRange]);

  /* 오늘 이후 날짜는 거래 내역이 없으므로 미래 날짜 선택 제한 */
  const today = useMemo(() => new Date(), []);

  return (
    <PageLayout
      title="계좌상세"
      onBack={onBack}
      rightAction={
        /* 우측 메뉴 버튼 — 계좌 설정·해지 등 추가 액션 진입점 */
        <button
          type="button"
          onClick={onMenu}
          aria-label="계좌 메뉴 열기"
          className="flex items-center justify-center size-9 rounded-lg text-text-muted hover:bg-surface-raised hover:text-text-heading transition-colors duration-150"
        >
          <Menu className="size-5" aria-hidden="true" />
        </button>
      }
    >
      {initialState === 'loading' && <LoadingSkeleton />}

      {initialState === 'error' && (
        <Stack gap="md" align="center" className="py-xl">
          <Typography variant="body" color="secondary">
            계좌 정보를 불러올 수 없어요.
          </Typography>
          <Button variant="outline" onClick={() => {}}>
            다시 시도
          </Button>
        </Stack>
      )}

      {initialState === 'data' && (
        <Stack gap="md">
          {/* ── 계좌 정보 Hero 영역 ─────────────────────── */}
          <Stack gap="xs" className="pb-sm">
            {/* 계좌 유형 배지 + 계좌번호 — Figma: 같은 줄 인라인 배치 */}
            <Inline gap="sm" align="center">
              <Badge variant="brand">{accountTypeBadgeLabel[accountType]}</Badge>
              <Typography variant="body-sm" color="secondary" numeric as="span">
                {accountNumber}
              </Typography>
            </Inline>

            {/* 계좌명 */}
            <Typography variant="body" weight="medium" color="heading">
              {accountName}
            </Typography>

            {/* 잔액 — 대형 숫자 표시, Manrope numeric 폰트로 가독성 향상 */}
            <Typography variant="heading" weight="bold" color="heading" numeric>
              {formatKrw(balance)}
            </Typography>

            {/* 출금가능액 — Figma: "출금가능액: X원" 단일 텍스트 (라벨·값 분리 없음) */}
            <Typography variant="body-sm" color="secondary">
              출금가능액:{' '}
              <Typography variant="body-sm" color="secondary" numeric as="span">
                {formatKrw(availableBalance)}
              </Typography>
            </Typography>
          </Stack>

          {/* ── 예금자보호법 카드 박스 ───────────────────────
              Figma: 제목(보호법명 + ChevronRight) + 설명 2줄 구성의 bordered 박스 */}
          <Card onClick={onInsuranceInfo} interactive>
            <Inline justify="between" align="start">
              <Stack gap="xs" className="flex-1 min-w-0">
                {/* 제목 행: 법 이름 + 한도 */}
                <Typography variant="body-sm" weight="medium" color="heading">
                  예금자보호법 (5천만원 한도)
                </Typography>
                {/* 설명 행 */}
                <Typography variant="caption" color="secondary">
                  이 예금은 예금자보호법에 따라 보호됩니다.
                </Typography>
              </Stack>
              {/* 상세 안내로 이동 — 탐색 가능 표시 */}
              <ChevronRight className="size-4 text-text-muted shrink-0 mt-xs" aria-hidden="true" />
            </Inline>
          </Card>

          <Divider />

          {/* ── 최근거래내역 섹션 ─────────────────────────── */}
          <Stack gap="sm">
            {/* 섹션 헤더: 제목+검색아이콘(좌) / 더보기 링크(우) */}
            <Inline justify="between" align="center">
              <Inline gap="xs" align="center">
                {/* 선택된 날짜 범위가 있으면 헤더 텍스트를 해당 기간으로 업데이트 */}
                <Typography variant="body" weight="bold" color="heading" as="span">
                  {transactionSectionTitle}
                </Typography>
                {/* 검색 아이콘 — 거래 내역 검색 진입점 */}
                <button
                  type="button"
                  onClick={onTransactionSearch}
                  aria-label="거래내역 검색"
                  className="flex items-center justify-center size-7 rounded text-text-muted hover:text-text-heading transition-colors"
                >
                  <Search className="size-4" aria-hidden="true" />
                </button>
              </Inline>

              {/* 거래내역 더보기 — 클릭 시 기간 선택 DatePicker를 팝업으로 출력 */}
              <button
                ref={moreButtonRef}
                type="button"
                onClick={() => {
                  onTransactionMore?.();
                  setDatePickerOpen((o) => !o);
                }}
                aria-haspopup="dialog"
                aria-expanded={datePickerOpen}
                className="flex items-center gap-xs text-xs text-text-secondary hover:text-brand-text transition-colors"
                aria-label="거래내역 기간 선택"
              >
                거래내역 더보기
                <Calendar className="size-3.5" aria-hidden="true" />
              </button>
            </Inline>

            {/*
             * DatePicker는 제어 모드(open/onOpenChange/anchorRef)로 렌더링된다.
             * 달력 패널은 document.body portal이므로 여기서의 DOM 위치는 무관하다.
             * maxDate=today: 미래 날짜 선택 차단 (존재하지 않는 거래 내역)
             */}
            <DatePicker
              mode="range"
              open={datePickerOpen}
              onOpenChange={setDatePickerOpen}
              anchorRef={moreButtonRef}
              rangeValue={dateRange}
              onRangeChange={setDateRange}
              maxDate={today}
            />

            {/*
             * -mx-standard: PageLayout main의 px-standard를 상쇄해 TransactionList를
             * 좌우 full-bleed로 확장한다. TransactionList 내부가 자체 px-standard를
             * 갖고 있으므로, 결과적으로 섹션 헤더와 같은 x축에 정렬된다.
             */}
            <div className="-mx-standard">
              <TransactionList items={transactions} onItemClick={onTransactionClick} />
            </div>

            {/* 하단 안내 문구 — 날짜 범위 선택 시 해당 기간으로 안내 문구 업데이트 */}
            <Typography variant="caption" color="muted" className="text-center py-sm">
              {dateRange[0] && dateRange[1]
                ? `${formatDate(dateRange[0])} ~ ${formatDate(dateRange[1])} 거래내역입니다.`
                : '최근 7일간의 거래내역입니다.'}
            </Typography>
          </Stack>
        </Stack>
      )}
    </PageLayout>
  );
}
