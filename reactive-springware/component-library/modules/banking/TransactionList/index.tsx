/**
 * @file index.tsx
 * @description 날짜별 그룹 헤더가 있는 거래 내역 목록 컴포넌트.
 *
 * 도메인 방침(6.3):
 * - 날짜 형식 표준: YYYY.MM.DD (Intl.DateTimeFormat 사용)
 * - API 응답 flat 배열 → 프론트엔드에서 날짜별 그룹으로 변환
 * - 날짜 헤더는 sticky top-0으로 항상 노출
 * - 입금/출금/이체에 따라 금액 색상 분기
 *
 * @example
 * <TransactionList items={transactions} loading={isLoading} />
 */
import React, { useMemo } from 'react';
import { cn } from '@lib/cn';
import type {
  TransactionItem,
  TransactionGroup,
  TransactionListItemProps,
  TransactionListProps,
  DateHeaderFormat,
} from './types';

/** 원화 금액 포맷터 */
const krwFormatter = new Intl.NumberFormat('ko-KR');

/**
 * ISO 8601 날짜 문자열 → 날짜 그룹 헤더용 한국어 형식 변환.
 * - 'month-day'      → 'MM월 DD일'       (기본, 연도 생략)
 * - 'year-month-day' → 'YYYY년 MM월 DD일' (연도 포함)
 */
function formatDateKey(isoDate: string, format: DateHeaderFormat): string {
  const d     = new Date(isoDate);
  const year  = d.getFullYear();
  const month = d.getMonth() + 1;
  const day   = String(d.getDate()).padStart(2, '0');
  if (format === 'year-month-day') return `${year}년 ${month}월 ${day}일`;
  return `${month}월 ${day}일`;
}

/**
 * ISO 8601 날짜 문자열 → 시간만 추출 'HH:MM:SS' 형식 변환.
 * 날짜 그룹 헤더가 날짜를 이미 표시하므로, 항목 행에는 시간만 노출한다.
 */
function formatTimeOnly(isoDate: string): string {
  const d = new Date(isoDate);
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  const s = String(d.getSeconds()).padStart(2, '0');
  return `${h}:${m}:${s}`;
}

/** flat 배열 → 날짜별 그룹 배열로 변환 */
function groupByDate(items: TransactionItem[], format: DateHeaderFormat): TransactionGroup[] {
  const map = new Map<string, TransactionItem[]>();

  for (const item of items) {
    const key = formatDateKey(item.date, format);
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(item);
  }

  /* Map 삽입 순서 유지 → 최신 날짜가 먼저 오도록 items를 역순 정렬 후 그룹핑 권장 */
  return Array.from(map.entries()).map(([date, groupItems]) => ({ date, items: groupItems }));
}

/** 거래 유형별 금액 색상 */
function amountColor(type: TransactionListItemProps['type']): string {
  if (type === 'deposit')    return 'text-success-text';
  if (type === 'withdrawal') return 'text-danger-text';
  return 'text-text-heading'; /* 이체: 기본 헤딩 색상 */
}

/** 거래 유형별 금액 접두사 (입금: +, 출금: -, 이체: -) */
function amountPrefix(type: TransactionListItemProps['type']): string {
  return type === 'deposit' ? '+' : '-';
}

function TransactionListItem({
  type, title, date, amount, balance, onClick,
}: TransactionListItemProps) {
  const formattedAmount  = `${amountPrefix(type)}${krwFormatter.format(Math.abs(amount))}원`;
  const formattedBalance = balance != null ? `${krwFormatter.format(balance)}원` : undefined;

  const Inner = (
    <>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-bold text-text-heading truncate">{title}</p>
        <p className="text-xs text-text-muted mt-xs">{date}</p>
      </div>
      <div className="shrink-0 text-right">
        <p className={cn('text-sm font-bold font-numeric tabular-nums', amountColor(type))}>
          {formattedAmount}
        </p>
        {formattedBalance && (
          <p className="text-xs text-text-muted font-numeric tabular-nums mt-xs">
            잔액 {formattedBalance}
          </p>
        )}
      </div>
    </>
  );

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        className="w-full flex items-center gap-standard py-md px-standard text-left hover:bg-surface-raised active:bg-surface-raised transition-colors duration-100"
        aria-label={`${title} ${formattedAmount}`}
      >
        {Inner}
      </button>
    );
  }

  return (
    <div className="flex items-center gap-standard py-md px-standard">
      {Inner}
    </div>
  );
}

export function TransactionList({
  items = [],
  loading = false,
  emptyMessage = '거래 내역이 없어요',
  onItemClick,
  dateHeaderFormat = 'month-day',
  className,
}: TransactionListProps) {
  /* items 또는 dateHeaderFormat이 바뀔 때만 그룹핑 재계산 */
  const groups = useMemo(() => groupByDate(items, dateHeaderFormat), [items, dateHeaderFormat]);

  if (loading) {
    /* 스켈레톤 UI — 3개 플레이스홀더 */
    return (
      <div className={cn('flex flex-col', className)} aria-busy="true" aria-label="거래 내역 로딩 중">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="flex items-center gap-standard py-md px-standard animate-pulse">
            <div className="flex-1">
              <div className="h-4 bg-surface-raised rounded-sm w-1/2 mb-xs" />
              <div className="h-3 bg-surface-raised rounded-sm w-1/4" />
            </div>
            <div className="h-4 bg-surface-raised rounded-sm w-20" />
          </div>
        ))}
      </div>
    );
  }

  if (groups.length === 0) {
    return (
      <div className={cn('flex items-center justify-center py-2xl text-text-muted text-sm', className)}>
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className={cn('flex flex-col', className)} role="list" aria-label="거래 내역">
      {groups.map((group) => (
        <section key={group.date}>
          {/*
           * 날짜 헤더 — sticky top-0으로 스크롤 시 항상 노출
           * z-sticky(20) 사용. bg-surface로 하단 콘텐츠를 덮음
           */}
          <p
            className={cn(
              'sticky top-0 z-sticky',
              'text-xs text-text-muted font-medium',
              'bg-surface py-xs px-standard',
            )}
            aria-label={`${group.date} 거래 내역`}
          >
            {group.date}
          </p>

          {/* 날짜 내 거래 항목 목록 */}
          {group.items.map((item) => (
            <div key={item.id} role="listitem">
              <TransactionListItem
                type={item.type}
                title={item.title}
                /* date: 그룹 헤더에 날짜를 이미 표시하므로 시간만 노출 */
                date={formatTimeOnly(item.date)}
                amount={item.amount}
                balance={item.balance}
                /* onItemClick이 있으면 클릭 핸들러 전달 → <button> 렌더링 */
                onClick={onItemClick ? () => onItemClick(item) : undefined}
              />
              <hr className="border-border-subtle mx-standard" />
            </div>
          ))}
        </section>
      ))}
    </div>
  );
}
