/**
 * @file index.tsx
 * @description PIN 입력 하단 시트 컴포넌트.
 *
 * BottomSheet + PinDotIndicator + NumberKeypad를 조합하여
 * 즉시결제·계좌 비밀번호 등 PIN 확인이 필요한 화면에서 재사용한다.
 *
 * 동작:
 * - 숫자 키패드로 PIN을 입력하면 PinDotIndicator가 채워진다.
 * - pinLength 자리가 완성되면 onConfirm이 자동 호출된다.
 * - 재배열 버튼으로 키패드 순서를 셔플할 수 있다.
 * - 닫힐 때 입력 상태가 초기화된다.
 *
 * @param open         - 시트 열림 여부
 * @param onClose      - 닫기 핸들러
 * @param onConfirm    - PIN 완료 핸들러 (pinLength 자리 입력 시 자동 호출, async 가능)
 * @param title        - 시트 타이틀 (기본: '비밀번호 입력')
 * @param subtitle     - 도트 위 안내 문구 (기본: '비밀번호를 입력하세요')
 * @param pinLength    - PIN 자릿수 (기본: 4)
 * @param errorMessage - 외부 에러 메시지 (설정 시 도트 아래 표시 + PIN 초기화)
 *
 * @example
 * <PinConfirmSheet
 *   open={pinOpen}
 *   onClose={() => setPinOpen(false)}
 *   onConfirm={(pin) => { console.log(pin); navigate('/next'); }}
 * />
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { BottomSheet }      from '../BottomSheet';
import { PinDotIndicator }  from '../../banking/PinDotIndicator';
import { NumberKeypad }     from '../../banking/NumberKeypad';
import { Typography }       from '../../../core/Typography';
import type { PinConfirmSheetProps } from './types';

export type { PinConfirmSheetProps } from './types';

/** 배열을 무작위로 섞어 반환한다 (Fisher-Yates 셔플). */
function shuffle(arr: number[]): number[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

const INITIAL_DIGITS = () => shuffle([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);

export function PinConfirmSheet({
  open,
  onClose,
  onConfirm,
  title = '비밀번호 입력',
  subtitle = '비밀번호를 입력하세요',
  pinLength = 4,
  errorMessage,
  container,
}: PinConfirmSheetProps) {
  const [pin,    setPin]    = useState('');
  const [digits, setDigits] = useState<number[]>(INITIAL_DIGITS);

  /* 중복 호출 방지 플래그 — Strict Mode의 effect 2회 실행이나 타이밍 이슈로
   * onConfirm이 두 번 호출되는 것을 막는다. open이 false로 바뀔 때 함께 초기화된다. */
  const isSubmittingRef = useRef(false);

  /* 시트가 닫힐 때 입력 상태 및 제출 플래그 초기화 */
  useEffect(() => {
    if (!open) {
      setPin('');
      setDigits(INITIAL_DIGITS());
      isSubmittingRef.current = false;
    }
  }, [open]);

  /* 외부 에러 메시지가 설정되면 입력한 PIN을 초기화해 재시도를 가능하게 한다 */
  useEffect(() => {
    if (errorMessage) {
      setPin('');
      isSubmittingRef.current = false; // 에러 후 재시도 허용
    }
  }, [errorMessage]);

  /* onConfirm을 ref로 관리 — 부모 리렌더로 함수 참조가 바뀌어도 effect가 재실행되지 않게 한다.
   * deps에 onConfirm을 포함하면 API 실패 후 setPinError → 부모 리렌더 → 새 onConfirm 참조 →
   * effect 재실행 → pin이 아직 4자리 → API 무한 호출 루프가 발생한다. */
  const onConfirmRef = useRef(onConfirm);
  useEffect(() => { onConfirmRef.current = onConfirm; }, [onConfirm]);

  /* pinLength 자리 완성 시 자동 확인 */
  useEffect(() => {
    if (pin.length === pinLength) {
      if (isSubmittingRef.current) return; // 이미 제출 중이면 중복 호출 차단
      isSubmittingRef.current = true;
      /* 렌더 완료 후 호출해 마지막 도트가 채워지는 것을 보여줌 */
      const id = setTimeout(() => onConfirmRef.current(pin), 150);
      return () => clearTimeout(id);
    }
  }, [pin, pinLength]); // onConfirm 제외 — ref로 최신값을 유지하므로 deps 불필요

  const handleDigit = useCallback((d: number) => {
    setPin((p) => (p.length < pinLength ? p + d : p));
  }, [pinLength]);

  const handleDelete = useCallback(() => {
    setPin((p) => p.slice(0, -1));
  }, []);

  const handleShuffle = useCallback(() => {
    setDigits(INITIAL_DIGITS());
  }, []);

  return (
    <BottomSheet
      open={open}
      onClose={onClose}
      title={title}
      disableBackdropClose
      hideCloseButton={false}
      container={container}
    >
      {/* PIN 도트 */}
      <div className="flex flex-col items-center gap-xl py-xl">
        <Typography variant="body" color="muted">
          {subtitle}
        </Typography>
        <PinDotIndicator length={pinLength} filledCount={pin.length} />
        {/* 에러 메시지 — 외부에서 errorMessage가 전달될 때만 표시 */}
        {errorMessage && (
          <Typography variant="caption" color="danger">
            {errorMessage}
          </Typography>
        )}
      </div>

      {/* 숫자 키패드 */}
      <NumberKeypad
        digits={digits}
        onDigitPress={handleDigit}
        onDelete={handleDelete}
        onShuffle={handleShuffle}
      />
    </BottomSheet>
  );
}
