/**
 * @file types.ts
 * @description DropdownMenu 컴포넌트의 TypeScript 타입 정의.
 */
import React from 'react';

/** 드롭다운 메뉴 단일 항목 */
export interface DropdownMenuItem {
  /** 표시 레이블 */
  label: string;
  /** 좌측 아이콘 (선택) */
  icon?: React.ReactNode;
  /** 항목 클릭 핸들러 */
  onClick: () => void;
  /**
   * 항목 스타일 변형. 기본: 'default'
   * - 'default' : 일반 텍스트 색상
   * - 'danger'  : 위험 액션(예: 로그아웃·삭제)에 사용, semantic-danger 색상 적용
   */
  variant?: 'default' | 'danger';
}

export interface DropdownMenuProps {
  /** 드롭다운을 열고 닫는 트리거 요소 */
  children: React.ReactNode;
  /** 드롭다운에 표시할 항목 목록. 미전달 시 빈 패널 렌더링 */
  items?: DropdownMenuItem[];
  /**
   * 패널 정렬 방향. 기본: 'right'
   * - 'right' : 트리거 우측 기준 좌측으로 패널 정렬 (우측 끝 버튼에 적합)
   * - 'left'  : 트리거 좌측 기준 우측으로 패널 정렬
   */
  align?: 'left' | 'right';
  className?: string;
}
