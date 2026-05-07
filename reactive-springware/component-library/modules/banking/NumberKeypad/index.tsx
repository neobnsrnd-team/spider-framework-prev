/**
 * @file index.tsx
 * @description 계좌 비밀번호 입력용 보안 숫자 키패드 컴포넌트.
 *
 * Figma 원본: Hana Bank App node-id: 1:1576
 *
 * 3×4 그리드로 구성된 숫자 키패드.
 * 매 입력 세션마다 숫자 위치를 섞어(셔플) 화면 녹화 기반 비밀번호 탈취를 방지한다.
 *
 * 레이아웃:
 *   행 1~3: digits[0..8] 숫자 버튼 (행 우선 배치)
 *   행 4:   [재배열] [digits[9]] [⌫]
 *
 * @param digits        - 셔플된 숫자 배열 (길이 10, 값 0~9 각 1회)
 * @param onDigitPress  - 숫자 버튼 클릭 핸들러
 * @param onDelete      - 지우기 버튼 클릭 핸들러
 * @param onShuffle     - 재배열 버튼 클릭 핸들러
 * @param className     - 추가 Tailwind 클래스
 *
 * @example
 * const [digits, setDigits] = useState(() => shuffle([0,1,2,3,4,5,6,7,8,9]));
 *
 * <NumberKeypad
 *   digits={digits}
 *   onDigitPress={(d) => setPin((p) => p + d)}
 *   onDelete={() => setPin((p) => p.slice(0, -1))}
 *   onShuffle={() => setDigits(shuffle([0,1,2,3,4,5,6,7,8,9]))}
 * />
 */
import React from 'react';
import { Delete } from 'lucide-react';
import { cn } from '@lib/cn';
import type { NumberKeypadProps } from './types';

export type { NumberKeypadProps } from './types';

/** 숫자·특수 버튼 공통 base className */
const BTN_BASE =
  'h-14 flex items-center justify-center rounded-xl ' +
  'transition-colors duration-100 active:bg-surface-raised select-none';

/** 미전달 시 사용하는 기본 순서 배열 */
const DEFAULT_DIGITS: number[] = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

export function NumberKeypad({
  digits = DEFAULT_DIGITS,
  onDigitPress = () => {},
  onDelete = () => {},
  onShuffle = () => {},
  className,
}: NumberKeypadProps) {
  /* digits[0..8]: 3×3 숫자 영역, digits[9]: 하단 행 중앙 숫자 */
  const topNineDigits = digits.slice(0, 9);
  const bottomMiddleDigit = digits[9];

  return (
    /* grid-cols-3: 3열 고정 그리드 — 모든 셀이 동일 너비로 자동 분배 */
    <div className={cn('grid grid-cols-3', className)}>
      {/* 행 1~3: 숫자 버튼 9개 */}
      {topNineDigits.map((digit) => (
        <button
          key={digit}
          type="button"
          onClick={() => onDigitPress(digit)}
          aria-label={`${digit}`}
          className={cn(BTN_BASE, 'text-xl text-text-heading')}
        >
          {digit}
        </button>
      ))}

      {/* 행 4: 재배열 | 숫자 | 지우기 */}
      <button
        type="button"
        onClick={onShuffle}
        aria-label="숫자 재배열"
        className={cn(BTN_BASE, 'text-base text-text-heading')}
      >
        재배열
      </button>
      <button
        type="button"
        onClick={() => onDigitPress(bottomMiddleDigit)}
        aria-label={`${bottomMiddleDigit}`}
        className={cn(BTN_BASE, 'text-xl text-text-heading')}
      >
        {bottomMiddleDigit}
      </button>
      <button
        type="button"
        onClick={onDelete}
        aria-label="지우기"
        className={cn(BTN_BASE)}
      >
        {/* Delete: lucide-react의 키보드 백스페이스 키 아이콘 (⌫) */}
        <Delete className="size-6 text-text-heading" aria-hidden="true" />
      </button>
    </div>
  );
}
