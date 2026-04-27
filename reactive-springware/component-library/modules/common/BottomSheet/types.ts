/**
 * @file types.ts
 * @description BottomSheet 컴포넌트의 TypeScript 타입 정의.
 * 모바일 전용 하단 오버레이 시트. Modal과 달리 뷰포트 크기와 무관하게
 * 항상 화면 하단에 고정 표시된다.
 */
import React from 'react';

/**
 * 시트 최대 높이 프리셋.
 * - 'auto': 콘텐츠 높이에 맞춤 (최대 90dvh)
 * - 'half': 화면 절반(50dvh)
 * - 'full': 최대 높이 90dvh (auto와 동일 값. 콘텐츠가 많은 시트에서 의도를 명시할 때 사용)
 */
export type BottomSheetSnap = 'auto' | 'half' | 'full';

export interface BottomSheetProps {
  /** 시트 열림 여부 */
  open: boolean;
  /** 시트 닫기 핸들러 (백드롭 클릭, ESC 키, 닫기 버튼 공통) */
  onClose: () => void;
  /** 시트 상단 타이틀. 미전달 시 타이틀 영역 렌더링 안 함 */
  title?: string;
  /** 본문 슬롯 */
  children?: React.ReactNode;
  /** 하단 고정 버튼 영역 슬롯 */
  footer?: React.ReactNode;
  /**
   * 시트 최대 높이 프리셋. 기본: 'auto'
   * 콘텐츠가 프리셋 높이를 초과하면 본문 영역이 내부 스크롤로 전환
   */
  snap?: BottomSheetSnap;
  /**
   * 백드롭 클릭으로 닫기 비활성화 여부.
   * 필수 액션이 있는 시트(예: 약관 동의)에서 true로 설정.
   * 기본: false
   */
  disableBackdropClose?: boolean;
  /**
   * 헤더 우측 X(닫기) 버튼 숨김 여부. 기본: false
   * Footer 버튼으로만 닫기를 처리하는 시트(예: 이체 확인)에서 true로 설정한다.
   * true로 설정해도 ESC 키·백드롭 클릭 닫기는 유지된다.
   */
  hideCloseButton?: boolean;
  /**
   * Portal 렌더링 대상 요소. 기본값: document.body.
   * CMS 캔버스처럼 특정 컨테이너 안에 오버레이를 가두고 싶을 때 전달한다.
   * 전달 시 백드롭 포지션이 fixed → absolute로 전환되므로
   * 컨테이너 요소에 `position: relative`와 `overflow: hidden`이 필요하다.
   */
  container?: HTMLElement;
  /**
   * CMS 브리지 전용: footer ReactNode가 없을 때 버튼 수를 지정해 footer를 자동 생성한다.
   * footer prop이 있으면 무시된다.
   * @default "0"
   */
  bottomBtnCnt?: '0' | '1' | '2';
  /** CMS 브리지 전용: 자동 생성 footer의 첫 번째(primary) 버튼 텍스트. @default "확인" */
  bottomBtn1Label?: string;
  /** CMS 브리지 전용: 자동 생성 footer의 두 번째(secondary) 버튼 텍스트. @default "취소" */
  bottomBtn2Label?: string;
  className?: string;
}