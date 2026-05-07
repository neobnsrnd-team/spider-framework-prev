/**
 * @file types.ts
 * @description CardInfoPanel 컴포넌트의 TypeScript 타입 정의.
 *
 * 카드 도메인 정보 패널 — 결제정보·카드 이용기간 등 섹션 단위 레이블-값 목록.
 */

export interface CardInfoRow {
  /** 좌측 레이블. 예: '결제 계좌', '결제일' */
  label: string;
  /**
   * 우측 값. '\n' 포함 시 줄바꿈으로 표시.
   * 예: '하나은행\n123456****1234'
   */
  value: string;
}

export interface CardInfoSection {
  /** 섹션 제목. 예: '결제정보', '카드 이용기간' */
  title: string;
  rows: CardInfoRow[];
}

export interface CardInfoPanelProps {
  /** 섹션 목록. 미전달 시 빈 패널 렌더링 */
  sections?: CardInfoSection[];
  className?: string;
}
