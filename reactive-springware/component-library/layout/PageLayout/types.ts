/**
 * @file types.ts
 * @description PageLayout 컴포넌트의 TypeScript 타입 정의.
 * 계좌 목록·상세, 이체 폼 등 일반 페이지에 사용하는 레이아웃.
 */
import React from 'react';

export interface PageLayoutProps extends React.HTMLAttributes<HTMLDivElement> {
  /** 상단 헤더 타이틀 */
  title:         string;
  /** 전달 시 헤더 좌측에 뒤로가기(<) 버튼 표시 */
  onBack?:       () => void;
  /** 헤더 우측 슬롯 (알림·설정·닫기 버튼 등) */
  rightAction?:  React.ReactNode;
  /**
   * 화면 하단 고정 액션 바 슬롯 (iOS 스타일 하단 버튼 영역).
   * 전달 시 화면 하단에 blur 배경 고정 바가 렌더링된다.
   * flex 컨테이너(h-dvh) 구조상 main이 남은 높이를 차지하므로
   * 별도 spacer 없이도 마지막 콘텐츠가 고정 바에 가려지지 않는다.
   */
  bottomBar?:    React.ReactNode;
  /**
   * CMS 브리지 전용: onBack이 없을 때 뒤로가기 버튼 표시 여부 (noop 핸들러).
   * onBack이 있으면 무시된다.
   */
  showBack?: boolean;
  /**
   * CMS 브리지 전용: rightAction이 없을 때 헤더 우측 버튼 종류.
   * rightAction이 있으면 무시된다.
   * @default "none"
   */
  rightBtnType?: 'close' | 'menu' | 'none';
  /**
   * CMS 브리지 전용: bottomBar가 없을 때 하단 버튼 수로 bottomBar를 자동 생성.
   * bottomBar가 있으면 무시된다.
   * @default "0"
   */
  bottomBtnCnt?: '0' | '1' | '2';
  /** CMS 브리지 전용: 자동 생성 bottomBar의 첫 번째(primary) 버튼 텍스트. @default "확인" */
  bottomBtn1Label?: string;
  /** CMS 브리지 전용: 자동 생성 bottomBar의 두 번째(secondary) 버튼 텍스트. @default "취소" */
  bottomBtn2Label?: string;
  className?:    string;
}