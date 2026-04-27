/**
 * @file types.ts
 * @description UsageHistoryFilterSheet 컴포넌트 타입 정의.
 */

export interface CardOption {
  value: string;
  label: string;
}

export type ApprovalType = 'approved' | 'confirmed';
export type CardType     = 'all' | 'credit' | 'check';
export type RegionType   = 'all' | 'domestic' | 'overseas';
export type UsageType    = 'all' | 'lump' | 'installment' | 'cashAdvance' | 'cancel';
export type PeriodType   = 'thisMonth' | '1month' | '3months' | 'custom';

export interface SearchFilter {
  approval:      ApprovalType;
  cardType:      CardType;
  /** 선택된 카드 value. 'all' 이면 전체 */
  selectedCard:  string;
  region:        RegionType;
  usageType:     UsageType;
  period:        PeriodType;
  /** period === 'custom' 일 때 선택된 월. 예: '2026-03' */
  customMonth?:  string;
}

export interface UsageHistoryFilterSheetProps {
  open: boolean;
  onClose: () => void;
  cardOptions: CardOption[];
  /** 필터 확정 시 호출. filter.customMonth에 선택 월이 담긴다. */
  onApply: (filter: SearchFilter) => void;
  /**
   * Portal 렌더링 대상 요소. 기본값: document.body.
   * CMS 캔버스처럼 특정 컨테이너 안에 오버레이를 가두고 싶을 때 전달한다.
   * 전달 시 내부 BottomSheet 백드롭 포지션이 fixed → absolute로 전환된다.
   */
  container?: HTMLElement;
}
