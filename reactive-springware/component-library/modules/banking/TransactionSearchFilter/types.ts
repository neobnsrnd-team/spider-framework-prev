/**
 * @file types.ts
 * @description TransactionSearchFilter 컴포넌트의 TypeScript 타입 정의.
 *
 * 거래내역 조회 필터 조건: 기간(퀵 선택 또는 직접 입력), 정렬 순서, 거래 유형.
 */

/**
 * 퀵 기간 선택 옵션.
 * 선택 시 오늘 기준으로 startDate·endDate를 자동 계산한다.
 */
export type QuickPeriod = '1m' | '3m' | '6m' | '12m';

/**
 * 거래 내역 정렬 순서.
 * - recent: 최근순 (기본값)
 * - old: 과거순
 */
export type SortOrder = 'recent' | 'old';

/**
 * 거래 유형 필터.
 * - all: 전체 (기본값)
 * - deposit: 입금만
 * - withdrawal: 출금만
 */
export type TransactionType = 'all' | 'deposit' | 'withdrawal';

/** 조회 버튼 클릭 시 상위로 전달되는 검색 파라미터 */
export interface TransactionSearchParams {
  /** ISO 날짜 형식 'YYYY-MM-DD' */
  startDate: string;
  /** ISO 날짜 형식 'YYYY-MM-DD' */
  endDate: string;
  sortOrder: SortOrder;
  transactionType: TransactionType;
}

export interface TransactionSearchFilterProps {
  /** 현재 적용된 검색 조건. 미전달 시 최근 1개월·최근순·전체가 기본값 */
  value?: TransactionSearchParams;
  /**
   * 조회 버튼 클릭 시 호출.
   * 폼 내부 상태(localParams)를 기준으로 호출되므로
   * 조회 전까지는 외부 value에 영향을 주지 않는다.
   * 미전달 시 no-op.
   */
  onSearch?: (params: TransactionSearchParams) => void;
  /** 초기 펼침 여부. 기본: false (접힌 상태) */
  defaultExpanded?: boolean;
  className?: string;
}