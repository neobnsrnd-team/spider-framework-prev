/**
 * @file types.ts
 * @description TabNav 컴포넌트의 TypeScript 타입 정의.
 * 홈 화면의 해당금융·다른금융·자산관리 같은 수평 탭 네비게이션에 사용한다.
 */

export interface TabNavItem {
  /** 탭 고유 식별자 */
  id:    string;
  /** 탭 레이블 텍스트 */
  label: string;
}

/**
 * 탭 스타일 변형.
 * - 'underline': 활성 탭 하단 인디케이터 라인 (기본값). 상단 내비게이션 탭에 사용.
 * - 'pill': 활성 탭에 둥근 배경 채움. 상품 카테고리·필터 탭에 사용.
 */
export type TabNavVariant = 'underline' | 'pill';

export interface TabNavProps {
  /** 탭 목록. 미전달 시 빈 탭바 렌더링 */
  items?:       TabNavItem[];
  /** 현재 활성 탭 id */
  activeId:     string;
  /**
   * 탭 변경 핸들러.
   * @param id - 클릭된 탭 id
   */
  onTabChange:  (id: string) => void;
  /**
   * 탭 스타일 변형.
   * @default 'underline'
   */
  variant?:     TabNavVariant;
  /**
   * 탭 버튼이 컨테이너 전체 너비를 균등하게 채울지 여부.
   * true: 각 탭이 flex-1로 균등 분할 (Figma의 "full width" 탭 패턴)
   * false: 탭이 콘텐츠 너비만큼만 차지 (기본값)
   * @default false
   */
  fullWidth?:   boolean;
  className?:   string;
}
