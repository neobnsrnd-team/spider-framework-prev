/**
 * @file types.ts
 * @description BankSelectGrid 컴포넌트 props 타입 정의.
 */

/** 은행 목록 단일 항목 */
export interface BankItem {
  /** 은행 식별 코드. 예: 'hana', 'kb', 'shinhan' */
  code: string;
  /** 화면에 표시할 은행명. 예: '하나은행' */
  name: string;
  /** 은행 로고/아이콘 (ReactNode). 미전달 시 기본 아이콘 표시 */
  icon?: React.ReactNode;
}

export interface BankSelectGridProps {
  /** 선택 가능한 은행 목록. 미전달 시 빈 그리드 렌더링 */
  banks?: BankItem[];
  /** 현재 선택된 은행 코드 */
  selectedCode?: string;
  /** 은행 선택 핸들러 */
  onSelect: (code: string) => void;
  /** 한 행에 표시할 열 수 (기본: 4) */
  columns?: 3 | 4;
}
