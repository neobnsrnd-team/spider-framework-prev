/**
 * @file types.ts
 * @description Select 컴포넌트 TypeScript 타입 정의.
 */

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps {
  /** 드롭다운 선택지 목록. 미전달 시 빈 드롭다운 렌더링 */
  options?:   SelectOption[];
  /** 현재 선택된 값 */
  value:      string;
  /** 선택 변경 핸들러 */
  onChange:   (value: string) => void;
  /** 접근성 레이블 (aria-label) */
  'aria-label'?: string;
  className?: string;
}
