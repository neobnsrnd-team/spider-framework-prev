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
  /**
   * 내장 트리거 버튼에 표시할 아이콘.
   * 이 prop이 제공되면 children 대신 내장 버튼을 렌더링한다.
   */
  triggerIcon?: React.ReactNode;
  /**
   * 내장 트리거 버튼 스타일. 기본: 'default'
   * - 'default' : 간단한 사각형 아이콘 버튼
   * - 'rounded' : 원형 배경 버튼 (UserProfile 설정 버튼 등)
   */
  triggerVariant?: 'default' | 'rounded';
  /** 내장 트리거 버튼의 접근성 레이블 */
  triggerAriaLabel?: string;
  /**
   * 완전히 커스텀한 트리거가 필요할 때 사용하는 fallback.
   * triggerIcon이 없을 때만 렌더링된다.
   */
  children?: React.ReactNode;
  /** 드롭다운에 표시할 항목 목록 */
  items: DropdownMenuItem[];
  /**
   * 패널 정렬 방향. 기본: 'right'
   * - 'right' : 트리거 우측 기준 좌측으로 패널 정렬 (우측 끝 버튼에 적합)
   * - 'left'  : 트리거 좌측 기준 우측으로 패널 정렬
   */
  align?: 'left' | 'right';
  className?: string;
}
