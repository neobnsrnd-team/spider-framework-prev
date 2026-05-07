/**
 * @file index.tsx
 * @description 거래내역 조회 조건 설정 필터 컴포넌트.
 *
 * 접기/펼치기 아코디언 구조:
 * - 접힌 상태: "조회 조건 설정" 헤더 + 현재 기간 요약 텍스트
 * - 펼친 상태: 퀵 기간 탭 / 날짜 직접 입력 / 정렬 순서 / 거래 유형 / 조회 버튼 노출
 *
 * 폼 상태는 컴포넌트 내부에서 관리하며 조회 버튼 클릭 시에만 onSearch를 호출한다.
 * 조회 전까지는 외부 value prop이 변경되지 않는다.
 *
 * @param value       - 현재 적용된 검색 조건 (초기값으로 사용)
 * @param onSearch    - 조회 버튼 클릭 시 호출. 새 검색 파라미터를 전달
 * @param defaultExpanded - 초기 펼침 여부 (기본: false)
 *
 * @example
 * <TransactionSearchFilter
 *   value={{ startDate: '2023-10-01', endDate: '2023-11-01', sortOrder: 'recent', transactionType: 'all' }}
 *   onSearch={handleSearch}
 *   defaultExpanded={false}
 * />
 */
import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@lib/cn';
import { DatePicker } from '../../../modules/common/DatePicker';
import { Select } from '../../../core/Select';
import type {
  QuickPeriod,
  SortOrder,
  TransactionType,
  TransactionSearchParams,
  TransactionSearchFilterProps,
} from './types';

// ── 상수 ──────────────────────────────────────────────────────────────

/** 퀵 기간 선택 탭 레이블 */
const QUICK_PERIOD_LABELS: Record<QuickPeriod, string> = {
  '1m': '1개월',
  '3m': '3개월',
  '6m': '6개월',
  '12m': '12개월',
};

const QUICK_PERIOD_LIST: QuickPeriod[] = ['1m', '3m', '6m', '12m'];

/** 정렬 순서 드롭다운 옵션 */
const SORT_ORDER_OPTIONS: { value: SortOrder; label: string }[] = [
  { value: 'recent', label: '최근순' },
  { value: 'old', label: '과거순' },
];

/** 거래 유형 드롭다운 옵션 */
const TRANSACTION_TYPE_OPTIONS: { value: TransactionType; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'deposit', label: '입금' },
  { value: 'withdrawal', label: '출금' },
];

// ── 유틸리티 ─────────────────────────────────────────────────────────

/** QuickPeriod → 소급 개월 수 매핑 */
const PERIOD_TO_MONTHS: Record<QuickPeriod, number> = {
  '1m': 1,
  '3m': 3,
  '6m': 6,
  '12m': 12,
};

/**
 * Date → 'YYYY-MM-DD' 로컬 날짜 문자열 변환.
 * toISOString()은 UTC 기준이라 UTC+9 환경에서 로컬 자정이 전날로 밀린다.
 * 로컬 연/월/일을 직접 추출해 timezone 오프셋 문제를 방지한다.
 */
function toLocalDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * 오늘 기준으로 QuickPeriod에 해당하는 startDate를 반환한다.
 * @returns 'YYYY-MM-DD' 형식
 */
function calcStartDate(period: QuickPeriod): string {
  const d = new Date();
  d.setMonth(d.getMonth() - PERIOD_TO_MONTHS[period]);
  return toLocalDateISO(d);
}

/** 'YYYY-MM-DD' → 'YYYY.MM.DD' 형식 변환 (접힌 상태 기간 요약 표시용) */
function toDisplayDate(iso: string): string {
  return iso.replace(/-/g, '.');
}

/**
 * 'YYYY-MM-DD' ISO 문자열 → Date 객체 변환.
 * DatePicker의 value / minDate / maxDate prop은 Date 타입을 요구하므로
 * 내부 상태(ISO string)를 전달 전에 변환한다.
 * 시간대 오프셋으로 날짜가 밀리는 것을 방지하기 위해 T00:00:00 로컬 시간으로 파싱한다.
 */
function isoToDate(iso: string): Date {
  return new Date(iso + 'T00:00:00');
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────

export function TransactionSearchFilter({
  value,
  onSearch = () => {},
  defaultExpanded = false,
  className,
}: TransactionSearchFilterProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  /* value 미전달 시 기본값. 빈 deps로 마운트 시 1회만 계산해 참조를 안정화한다.
     인라인 객체 기본값은 매 렌더마다 새 참조를 생성해 useEffect 무한 루프를 유발하므로 금지 */
  const fallbackValue = useMemo<TransactionSearchParams>(() => ({
    startDate: calcStartDate('1m'),
    endDate: toLocalDateISO(new Date()),
    sortOrder: 'recent',
    transactionType: 'all',
  }), []);

  const resolvedValue = value ?? fallbackValue;

  /* 폼 내부 상태 — 조회 버튼 클릭 전까지 외부 value에 반영되지 않음 */
  const [localParams, setLocalParams] = useState<TransactionSearchParams>(resolvedValue);

  /* 종료일 DatePicker의 maxDate로 사용. 렌더마다 new Date()를 생성하지 않도록 메모이제이션 */
  const today = useMemo(() => new Date(), []);

  /**
   * value prop이 외부에서 변경될 때 localParams를 동기화한다.
   * 부모에서 필터 초기화(예: '전체 기간으로 리셋') 시 내부 폼 상태가 함께 갱신된다.
   */
  useEffect(() => {
    setLocalParams(resolvedValue);
    /* 객체 참조 대신 개별 프로퍼티에 의존 — 부모가 비메모 객체를 전달해도 실제 값이
       같으면 React가 bailout하여 무한 루프가 발생하지 않는다 */
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resolvedValue.startDate, resolvedValue.endDate, resolvedValue.sortOrder, resolvedValue.transactionType]);

  /* 퀵 기간 탭 선택 상태 (직접 날짜를 바꾸면 null로 초기화됨).
   * 기본값 '1m': 필터를 처음 펼쳤을 때 1개월 탭이 활성 상태로 표시된다. */
  const [selectedPeriod, setSelectedPeriod] = useState<QuickPeriod | null>('1m');

  /** 퀵 기간 탭 클릭: startDate/endDate 자동 계산 후 로컬 상태 업데이트 */
  const handleQuickPeriod = useCallback(
    (period: QuickPeriod) => {
      setSelectedPeriod(period);
      setLocalParams((prev) => ({
        ...prev,
        startDate: calcStartDate(period),
        endDate: toLocalDateISO(today),
      }));
    },
    [today],
  );

  /**
   * DatePicker onChange는 Date | null을 전달한다.
   * null(선택 해제)은 무시하고, 유효한 날짜만 ISO 문자열로 변환해 localParams에 반영한다.
   * 직접 날짜를 고르면 퀵 기간 탭 선택이 해제된다.
   */
  const handleStartDateChange = useCallback((date: Date | null) => {
    if (!date) return;
    setSelectedPeriod(null);
    setLocalParams((prev) => ({ ...prev, startDate: toLocalDateISO(date) }));
  }, []);

  const handleEndDateChange = useCallback((date: Date | null) => {
    if (!date) return;
    setSelectedPeriod(null);
    setLocalParams((prev) => ({ ...prev, endDate: toLocalDateISO(date) }));
  }, []);

  const handleSortOrderChange = useCallback((val: string) => {
    setLocalParams((prev) => ({ ...prev, sortOrder: val as SortOrder }));
  }, []);

  const handleTransactionTypeChange = useCallback((val: string) => {
    setLocalParams((prev) => ({ ...prev, transactionType: val as TransactionType }));
  }, []);

  /** 조회 버튼: 현재 폼 상태를 상위로 전달 후 패널 접기 */
  const handleSearch = useCallback(() => {
    onSearch(localParams);
    setExpanded(false);
  }, [localParams, onSearch]);

  /* 접힌 상태에서 헤더 하단에 표시할 기간 요약 */
  const periodSummary = `${toDisplayDate(resolvedValue.startDate)} ~ ${toDisplayDate(resolvedValue.endDate)}`;

  return (
    <div
      className={cn(
        'w-full',
        'bg-surface-raised',
        'border-t border-b border-border-subtle',
        className,
      )}
    >
      {/* ── 헤더: 조회 조건 설정 + 아코디언 토글 ─────────────── */}
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className="w-full flex items-center justify-between px-standard py-[21px] text-left"
        aria-expanded={expanded}
        aria-controls="transaction-search-filter-body"
      >
        <div className="flex flex-col gap-xs">
          {/* 펼친 상태에서만 "조회 조건 설정" 타이틀 표시 */}
          {expanded ? (
            <span className="text-sm text-text-heading">조회 조건 설정</span>
          ) : (
            <span className="text-sm font-medium text-text-muted tabular-nums">
              {periodSummary}
            </span>
          )}
        </div>
        {/* expanded 시 rotate-180으로 위 방향 화살표 표시 */}
        <ChevronDown
          size={10}
          className={cn(
            'text-text-muted transition-transform duration-200',
            expanded && 'rotate-180',
          )}
          aria-hidden="true"
        />
      </button>

      {/* ── 펼친 영역: 필터 폼 ────────────────────────────────── */}
      {expanded && (
        <div
          id="transaction-search-filter-body"
          className="flex flex-col gap-md px-standard pb-[21px]"
        >
          {/* 퀵 기간 선택 세그먼트 탭 */}
          <div
            className={cn(
              'flex h-10 rounded-2xl overflow-hidden',
              'bg-surface border border-border',
              'p-[5px]',
            )}
            role="group"
            aria-label="빠른 기간 선택"
          >
            {QUICK_PERIOD_LIST.map((period, idx) => {
              const isActive = selectedPeriod === period;
              return (
                <button
                  key={period}
                  type="button"
                  onClick={() => handleQuickPeriod(period)}
                  className={cn(
                    'flex-1 flex items-center justify-center',
                    'text-xs rounded-[6px]',
                    'transition-colors duration-150',
                    /* 활성 탭: 브랜드 배경 */
                    isActive && 'bg-brand-10 text-brand-text',
                    /* 비활성 탭: 구분선 추가 (첫 탭 제외) */
                    !isActive && 'text-text-muted',
                    !isActive && idx > 0 && 'border-l border-border-subtle',
                  )}
                  aria-pressed={isActive}
                  aria-label={`${QUICK_PERIOD_LABELS[period]} 선택`}
                >
                  {QUICK_PERIOD_LABELS[period]}
                </button>
              );
            })}
          </div>

          {/* 날짜 선택 (시작일 | 종료일) — DatePicker 컴포넌트 사용 */}
          <div className="flex gap-sm">
            {/* 시작일: 종료일 이전 날짜만 선택 가능 */}
            <DatePicker
              value={isoToDate(localParams.startDate)}
              onChange={handleStartDateChange}
              maxDate={isoToDate(localParams.endDate)}
              placeholder="시작일"
              className="flex-1"
            />

            {/* 종료일: 시작일 이후 ~ 오늘까지만 선택 가능 */}
            <DatePicker
              value={isoToDate(localParams.endDate)}
              onChange={handleEndDateChange}
              minDate={isoToDate(localParams.startDate)}
              maxDate={today}
              placeholder="종료일"
              className="flex-1"
            />
          </div>

          {/* 정렬 순서 | 거래 유형 | 조회 버튼 */}
          <div className="flex items-center gap-sm">
            {/* 정렬 순서 커스텀 Select */}
            <Select
              options={SORT_ORDER_OPTIONS}
              value={localParams.sortOrder}
              onChange={handleSortOrderChange}
              aria-label="정렬 순서"
              className="flex-1"
            />

            {/* 거래 유형 커스텀 Select */}
            <Select
              options={TRANSACTION_TYPE_OPTIONS}
              value={localParams.transactionType}
              onChange={handleTransactionTypeChange}
              aria-label="거래 유형"
              className="flex-1"
            />

            {/* 조회 버튼 */}
            <button
              type="button"
              onClick={handleSearch}
              className={cn(
                'shrink-0',
                'bg-brand text-white text-xs',
                'px-standard py-[10px]',
                'rounded-2xl shadow-sm',
                'transition-opacity duration-150',
                'hover:opacity-90 active:opacity-75',
              )}
            >
              조회
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
