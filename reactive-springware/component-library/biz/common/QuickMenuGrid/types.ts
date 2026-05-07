/**
 * @file types.ts
 * @description QuickMenuGrid 컴포넌트의 TypeScript 타입 정의.
 * 홈 화면 퀵메뉴 2×N 그리드 패턴 전용.
 */
import React from 'react';

export interface QuickMenuItem {
  id:      string;
  icon:    React.ReactNode;
  label:   string;
  onClick: () => void;
  /** 알림 배지 숫자. 0 또는 미전달 시 배지 미노출 */
  badge?:  number;
  /**
   * 아이콘 컨테이너 형태.
   * - 'circle'  : 원형 (기본값)
   * - 'rounded' : 각이 둥근 사각형
   */
  iconShape?: 'circle' | 'rounded';
}

export interface QuickMenuGridProps {
  /** 메뉴 항목 목록. 미전달 시 빈 그리드 렌더링 */
  items?:     QuickMenuItem[];
  /**
   * 열 수. 기본: 4.
   * 아이템 수에 따라 4열(기본) 또는 3열 권장.
   */
  cols?:      2 | 3 | 4;
  className?: string;
}