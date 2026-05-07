/**
 * @file index.tsx
 * @description 6자리(또는 4자리) OTP/PIN 입력 컴포넌트.
 *
 * - 각 자릿수가 독립 `<input>`으로 분리되어 시각적으로 명확한 입력 UI 제공
 * - 숫자 입력 시 다음 칸으로 자동 포커스 이동
 * - Backspace 시 현재 칸 비운 후 이전 칸으로 포커스 이동
 * - 붙여넣기(paste) 지원: 클립보드에서 숫자만 추출하여 자동 채움
 * - `masked` prop으로 비밀번호 스타일 마스킹 가능
 *
 * @param length - OTP 자릿수 (4 | 6). 기본: 6
 * @param onComplete - length개 모두 입력 완료 시 OTP 문자열 전달
 * @param onChange - 한 자릿수라도 변경될 때마다 현재 OTP 문자열 전달
 * @param error - 에러 상태 (빨간 테두리)
 * @param disabled - 비활성화
 * @param masked - true이면 type="password" 적용
 * @param className - 추가 Tailwind 클래스
 *
 * @example
 * <OtpInput
 *   length={6}
 *   onComplete={(otp) => verifyOtp(otp)}
 *   error={isOtpError}
 * />
 */
import React, { useState, useCallback, useRef, useEffect } from 'react';
import { cn } from '@lib/cn';
import type { OtpInputProps } from './types';

export function OtpInput({
  length = 6,
  onComplete,
  onChange,
  error = false,
  disabled = false,
  masked = false,
  className,
}: OtpInputProps) {
  /* 각 자릿수의 값을 배열로 관리 */
  const [values, setValues] = useState<string[]>(Array(length).fill(''));
  /* 각 input 참조 배열 — 포커스 이동에 사용 */
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  /* length 변경 시 입력값 초기화 — 자릿수가 바뀌면 기존 values 크기가 달라져 렌더링 불일치 발생 */
  useEffect(() => {
    setValues(Array(length).fill(''));
    onChange?.('');
  }, [length]); // eslint-disable-line react-hooks/exhaustive-deps

  /** 특정 인덱스의 값을 갱신하고 onChange/onComplete 호출 */
  const updateValues = useCallback(
    (next: string[], changedIdx: number) => {
      setValues(next);
      const joined = next.join('');
      onChange?.(joined);
      /* 모든 자릿수가 채워지면 onComplete 호출 */
      if (next.every(Boolean) && joined.length === length) {
        onComplete?.(joined);
      }
      /* 값을 입력한 경우 다음 칸으로 포커스 이동 */
      if (next[changedIdx] && changedIdx < length - 1) {
        inputRefs.current[changedIdx + 1]?.focus();
      }
    },
    [length, onChange, onComplete],
  );

  const handleChange = useCallback(
    (idx: number, e: React.ChangeEvent<HTMLInputElement>) => {
      /* 숫자만 허용 */
      const digit = e.target.value.replace(/\D/g, '').slice(-1);
      const next = [...values];
      next[idx] = digit;
      updateValues(next, idx);
    },
    [values, updateValues],
  );

  const handleKeyDown = useCallback(
    (idx: number, e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Backspace') {
        e.preventDefault();
        const next = [...values];
        if (next[idx]) {
          /* 현재 칸에 값이 있으면 지우기 */
          next[idx] = '';
          setValues(next);
          onChange?.(next.join(''));
        } else if (idx > 0) {
          /* 현재 칸이 비어있으면 이전 칸으로 이동 후 지우기 */
          next[idx - 1] = '';
          setValues(next);
          onChange?.(next.join(''));
          inputRefs.current[idx - 1]?.focus();
        }
      }
    },
    [values, onChange],
  );

  /** 붙여넣기 — 클립보드에서 숫자만 추출하여 순서대로 채움 */
  const handlePaste = useCallback(
    (e: React.ClipboardEvent<HTMLInputElement>) => {
      e.preventDefault();
      const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
      const next = Array(length).fill('');
      pasted.split('').forEach((ch, i) => { next[i] = ch; });
      setValues(next);
      const joined = next.join('');
      onChange?.(joined);
      if (pasted.length === length) {
        onComplete?.(joined);
        /* 마지막 칸으로 포커스 이동 */
        inputRefs.current[length - 1]?.focus();
      } else {
        /* 붙여넣기 후 다음 빈 칸으로 포커스 이동 */
        inputRefs.current[pasted.length]?.focus();
      }
    },
    [length, onChange, onComplete],
  );

  return (
    <div
      className={cn('flex items-center w-full gap-sm', className)}
      role="group"
      aria-label={`${length}자리 인증번호 입력`}
    >
      {values.map((val, idx) => (
        <input
          key={idx}
          ref={(el) => { inputRefs.current[idx] = el; }}
          type={masked ? 'password' : 'text'}
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={1}
          value={val}
          disabled={disabled}
          aria-label={`${idx + 1}번째 자리`}
          onChange={(e) => handleChange(idx, e)}
          onKeyDown={(e) => handleKeyDown(idx, e)}
          onPaste={handlePaste}
          className={cn(
            /* 반응형 정방형 입력칸:
               flex-1로 컨테이너 너비를 균등 분할하고 aspect-square로 정방형 유지.
               max-w-[2.75rem]으로 넓은 화면에서 과도하게 커지는 것을 방지. */
            'flex-1 min-w-0 aspect-square max-w-[2.75rem]',
            'text-center text-base sm:text-xl font-bold font-numeric',
            'border rounded-md outline-none',
            'transition-all duration-150',
            /* 기본 상태 */
            !error && [
              'border-border bg-surface-subtle text-text-heading',
              'focus:border-border-focus focus:ring-2 focus:ring-brand-10 focus:bg-surface',
            ],
            /* 에러 상태 */
            error && [
              'border-danger bg-danger-surface text-danger-text',
              'ring-1 ring-danger-border',
              'focus:ring-2 focus:ring-danger-border',
            ],
            /* 비활성화 */
            disabled && 'opacity-50 cursor-not-allowed bg-surface-raised',
          )}
        />
      ))}
    </div>
  );
}