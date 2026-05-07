/**
 * @file types.ts
 * @description CardManagementPanel 컴포넌트의 TypeScript 타입 정의.
 */

/** 카드 관리 네비게이션 단일 행 데이터 */
export interface CardManagementNavRow {
  /** 행 레이블 */
  label:    string;
  /** 우측 보조 텍스트 (카드번호·계좌번호 등). 미전달 시 미노출 */
  subText?: string;
  /** 행 클릭 핸들러 */
  onClick?: () => void;
}

export interface CardManagementPanelProps {
  /** 네비게이션 행 목록. 순서대로 렌더링되며 개수 제한 없음. 미전달 시 빈 패널 렌더링 */
  rows?:      CardManagementNavRow[];
  className?: string;
}
