/**
 * @file types.ts
 * @description CardBenefitSummary 컴포넌트 props 타입 정의.
 */

/** 혜택 항목 단일 정보 */
export interface BenefitItem {
  /** 혜택 레이블. 예: '이번달 할인', '캐시백' */
  label: string;
  /** 혜택 금액 또는 포인트 (원 또는 P). 예: 12500 */
  amount: number;
  /** 단위 표시. 예: '원', 'P' (기본: '원') */
  unit?: string;
}

export interface CardBenefitSummaryProps {
  /** 보유 포인트 잔액 */
  points: number;
  /** 이번달 혜택 항목 목록 (할인·캐시백 등). 미전달 시 빈 혜택 영역 렌더링 */
  benefits?: BenefitItem[];
  /** 포인트 상세 클릭 핸들러 */
  onPointDetail?: () => void;
  /** 혜택 상세 클릭 핸들러 */
  onBenefitDetail?: () => void;
}
