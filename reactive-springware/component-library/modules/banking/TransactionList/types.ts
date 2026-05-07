/**
 * @file types.ts
 * @description 거래 내역 목록 컴포넌트의 TypeScript 타입 정의.
 * 6.3 도메인 방침: 날짜별 스티키 그룹 헤더 상시 노출.
 */

/** API 응답 flat 배열의 단일 거래 항목 */
export interface TransactionItem {
  id:       string;
  /** ISO 8601 형식. 예: '2026-03-26T14:30:00Z' */
  date:     string;
  title:    string;
  amount:   number;
  balance?: number;
  type:     'deposit' | 'withdrawal' | 'transfer';
}

/** 날짜별로 그룹핑된 거래 내역 (프론트엔드 변환 후 구조) */
export interface TransactionGroup {
  /** 표시용 날짜 문자열 (dateHeaderFormat에 따라 형식이 결정됨) */
  date:   string;
  items:  TransactionItem[];
}

/**
 * 날짜 그룹 헤더 표시 형식.
 * - 'month-day'      : 'MM월 DD일'       (기본값, 연도 생략)
 * - 'year-month-day' : 'YYYY년 MM월 DD일' (연도 포함)
 */
export type DateHeaderFormat = 'month-day' | 'year-month-day';

export type TransactionType = 'deposit' | 'withdrawal' | 'transfer';

export interface TransactionListItemProps {
  type:      TransactionType;
  title:     string;
  /** 표시용 날짜 문자열 */
  date:      string;
  /** 원화 단위 숫자. 컴포넌트 내부에서 Intl.NumberFormat으로 포맷 */
  amount:    number;
  /** 거래 후 잔액 */
  balance?:  number;
  onClick?:  () => void;
}

export interface TransactionListProps {
  /** API 응답 flat 배열. 컴포넌트 내부에서 날짜별로 그룹핑. 미전달 시 emptyMessage 표시 */
  items?:     TransactionItem[];
  /** 로딩 상태 */
  loading?:   boolean;
  /** 빈 목록 표시 메시지. 기본: '거래 내역이 없어요' */
  emptyMessage?: string;
  /**
   * 거래 항목 클릭 핸들러.
   * 전달 시 각 항목이 <button>으로 렌더링되며 hover/active 인터랙션이 활성화된다.
   * 예: 항목 클릭 → 거래 상세 바텀시트 오픈
   */
  onItemClick?: (item: TransactionItem) => void;
  /**
   * 날짜 그룹 헤더 표시 형식.
   * - 'month-day'      : 'MM월 DD일'       (기본값)
   * - 'year-month-day' : 'YYYY년 MM월 DD일'
   */
  dateHeaderFormat?: DateHeaderFormat;
  className?: string;
}
