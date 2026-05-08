/**
 * @file types.ts
 * @description EmptyState 컴포넌트의 TypeScript 타입 정의.
 * 도메인별 콘텐츠를 props로 주입하는 브랜드 중립 컴포넌트.
 */
import React from 'react';

export interface EmptyStateProps {
  /** SVG 또는 img 요소. 도메인별 일러스트를 외부에서 주입 */
  illustration?: React.ReactNode;
  title:         string;
  description?:  string;
  /**
   * 액션 버튼 레이블. 전달 시 내장 버튼을 렌더링한다.
   * action 슬롯보다 우선 적용된다.
   */
  actionLabel?:  string;
  /** actionLabel과 함께 사용하는 버튼 클릭 핸들러 */
  onAction?:     () => void;
  /** CTA 버튼 등 완전히 커스텀한 액션이 필요할 때 사용하는 슬롯 */
  action?:       React.ReactNode;
  className?:    string;
}