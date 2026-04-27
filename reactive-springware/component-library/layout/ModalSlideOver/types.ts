import type { ReactNode } from 'react';

export interface ModalSlideOverProps {
  /** 모달 내부에 렌더링할 콘텐츠 */
  children: ReactNode;
  /** 백드롭 클릭 시 호출. 미전달 시 백드롭 클릭으로 닫기 비활성 */
  onClose?: () => void;
  /** 슬라이드 방향 (기본: 'right') */
  direction?: 'right' | 'bottom';
  /** z-index 레벨 (기본: 50) */
  zIndex?: number;
  /**
   * Portal 렌더링 대상 요소. 기본값: document.body.
   * CMS 캔버스처럼 특정 컨테이너 안에 오버레이를 가두고 싶을 때 전달한다.
   * 전달 시 백드롭 포지션이 fixed → absolute로 전환되므로
   * 컨테이너 요소에 `position: relative`와 `overflow: hidden`이 필요하다.
   */
  container?: HTMLElement;
}
