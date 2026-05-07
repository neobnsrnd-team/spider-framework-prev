/**
 * @file types.ts
 * @description DatePicker 컴포넌트의 TypeScript 타입 정의
 */
import type React from 'react';

export type DatePickerMode = 'single' | 'range';

export interface DatePickerProps {
  /** 선택 모드. 기본: 'single' */
  mode?:          DatePickerMode;
  /** single 모드에서 사용하는 선택된 날짜 */
  value?:         Date | null;
  /** range 모드에서 사용하는 시작·종료 날짜 */
  rangeValue?:    [Date | null, Date | null];
  onChange?:      (date: Date | null) => void;
  onRangeChange?: (range: [Date | null, Date | null]) => void;
  minDate?:       Date;
  maxDate?:       Date;
  placeholder?:   string;
  label?:         string;
  disabled?:      boolean;
  className?:     string;
  /**
   * 외부에서 달력 열림 상태를 제어할 때 사용 (제어 모드).
   * 이 prop이 제공되면 내장 트리거 버튼을 렌더링하지 않는다.
   */
  open?:          boolean;
  /** 달력 열림 상태 변경 콜백 (제어 모드 전용) */
  onOpenChange?:  (open: boolean) => void;
  /**
   * 달력 패널 위치 계산의 기준이 되는 외부 트리거 요소 (제어 모드 전용).
   * 미제공 시 내장 triggerRef를 사용한다.
   */
  anchorRef?:     React.RefObject<HTMLElement | null>;
  /**
   * 달력 패널 portal 대상 요소.
   * 기본값: document.body
   * CSS @scope로 격리된 환경(예: CMS 캔버스)에서는 스코프 루트 요소를 전달하면
   * 스코프 내 스타일이 달력에도 적용된다.
   */
  portalContainer?: HTMLElement | null;
}