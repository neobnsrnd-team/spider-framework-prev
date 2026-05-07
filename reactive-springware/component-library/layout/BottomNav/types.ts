/**
 * @file types.ts
 * @description BottomNav 컴포넌트의 TypeScript 타입 정의.
 * 홈 화면 하단 고정 탭바 (자산·상품·홈·카드·챗봇 등).
 */
import React from 'react';

export interface BottomNavItem {
  /** 항목 고유 식별자 (activeId 비교에 사용) */
  id:          string;
  /** 비활성 상태 아이콘 */
  icon:        React.ReactNode;
  /** 활성 상태 아이콘. 미전달 시 icon 재사용 */
  activeIcon?: React.ReactNode;
  /** 탭 레이블 텍스트 */
  label:       string;
  /** 탭 클릭 핸들러 */
  onClick:     () => void;
}

export interface BottomNavProps {
  /** 탭 항목 목록. 미전달 시 빈 탭바 렌더링 */
  items?:     BottomNavItem[];
  /** 현재 활성 탭 id */
  activeId:   string;
  className?: string;
}
