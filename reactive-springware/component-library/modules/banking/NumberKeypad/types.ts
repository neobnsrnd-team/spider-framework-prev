/**
 * @file types.ts
 * @description NumberKeypad 컴포넌트의 TypeScript 타입 정의.
 *
 * 계좌 비밀번호 등 PIN 입력 화면에서 사용하는 보안 키패드 컴포넌트.
 * 숫자 재배열(셔플) 기능으로 화면 녹화 기반 비밀번호 탈취를 방지한다.
 *
 * 키패드 레이아웃 (3×4 그리드):
 *   [digits[0]] [digits[1]] [digits[2]]
 *   [digits[3]] [digits[4]] [digits[5]]
 *   [digits[6]] [digits[7]] [digits[8]]
 *   [ 재배열  ] [digits[9]] [   ⌫    ]
 */

export interface NumberKeypadProps {
  /**
   * 키패드에 표시할 숫자 배열 (0~9, 셔플 상태).
   * - digits[0..8]: 3×3 그리드 (행 우선 순서)
   * - digits[9]: 하단 행 중앙 버튼
   * 길이는 반드시 10이어야 한다. 미전달 시 [0..9] 순서로 표시.
   */
  digits?: number[];
  /**
   * 숫자 버튼 클릭 시 호출.
   * @param digit - 클릭된 숫자 (0~9)
   */
  onDigitPress?: (digit: number) => void;
  /** 지우기(⌫) 버튼 클릭 시 호출 */
  onDelete?: () => void;
  /** 재배열 버튼 클릭 시 호출 — 고객이 digits를 다시 셔플해서 전달해야 한다 */
  onShuffle?: () => void;
  /** 추가 Tailwind 클래스 */
  className?: string;
}
