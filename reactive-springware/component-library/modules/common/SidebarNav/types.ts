/**
 * @file types.ts
 * @description SidebarNav 컴포넌트의 TypeScript 타입 정의.
 *
 * 세로 방향 탭 네비게이션으로, 전체 메뉴 화면의 좌측 카테고리 목록 등
 * 세로로 나열된 탭 선택 UI에 사용한다.
 * 가로 방향 탭이 필요한 경우에는 TabNav를 사용한다.
 *
 * @see component-map.md §3.2-H (탭 네비게이션 처리 지침)
 */

/** 사이드바 네비게이션 개별 항목 */
export interface SidebarNavItem {
  /** 항목 고유 식별자 */
  id: string;
  /** 표시할 레이블 텍스트 */
  label: string;
}

export interface SidebarNavProps {
  /** 네비게이션 항목 목록. 미전달 시 빈 사이드바 렌더링 */
  items?: SidebarNavItem[];
  /** 현재 활성화된 항목 id */
  activeId: string;
  /**
   * 항목 클릭 시 호출되는 콜백.
   * 선택된 항목의 id를 인자로 전달한다.
   */
  onItemChange: (id: string) => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
