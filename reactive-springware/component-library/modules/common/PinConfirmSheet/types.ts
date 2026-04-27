/**
 * @file types.ts
 * @description PinConfirmSheet 컴포넌트 타입 정의.
 */

export interface PinConfirmSheetProps {
  /** 시트 열림 여부 */
  open: boolean;
  /** 닫기 핸들러 */
  onClose: () => void;
  /**
   * PIN 입력 완료 핸들러.
   * pinLength 자리가 모두 입력되면 자동 호출된다.
   * async 함수를 전달해도 된다 (API 호출 등).
   * @param pin - 입력된 PIN 문자열
   */
  onConfirm: (pin: string) => void | Promise<void>;
  /** 시트 상단 타이틀. 기본: '비밀번호 입력' */
  title?: string;
  /** 도트 위에 표시할 안내 문구. 기본: '비밀번호를 입력하세요' */
  subtitle?: string;
  /** PIN 자릿수. 기본: 4 */
  pinLength?: number;
  /**
   * 외부에서 주입하는 에러 메시지.
   * 값이 설정되면 도트 아래에 표시하고 입력된 PIN을 초기화한다.
   */
  errorMessage?: string;
  /**
   * Portal 렌더링 대상 요소. 기본값: document.body.
   * CMS 캔버스처럼 특정 컨테이너 안에 오버레이를 가두고 싶을 때 전달한다.
   * 전달 시 내부 BottomSheet 백드롭 포지션이 fixed → absolute로 전환된다.
   */
  container?: HTMLElement;
}
