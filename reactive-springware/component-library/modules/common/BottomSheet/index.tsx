/**
 * @file index.tsx
 * @description 모바일 전용 하단 오버레이 시트 컴포넌트.
 *
 * Modal과의 차이:
 * - 뷰포트 너비와 무관하게 항상 화면 하단에 고정 (Modal은 md 이상에서 중앙 다이얼로그로 전환)
 * - snap prop으로 높이 프리셋 제어 ('auto' | 'half' | 'full')
 * - 이체 확인, 약관 동의, 옵션 선택 등 모바일 액션 시트에 사용
 *
 * @example
 * // 이체 확인 시트
 * <BottomSheet
 *   open={open}
 *   onClose={() => setOpen(false)}
 *   title="이체 확인"
 *   footer={
 *     <div className="flex gap-sm">
 *       <Button variant="outline" fullWidth onClick={() => setOpen(false)}>취소</Button>
 *       <Button fullWidth onClick={handleConfirm}>이체 확인</Button>
 *     </div>
 *   }
 * >
 *   <CardRow label="받는 분" value="홍길동" />
 *   <CardRow label="금액" value="50,000원" />
 * </BottomSheet>
 */
import React, { useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@lib/cn';
import { Button } from '../../../core/Button';
import type { BottomSheetProps, BottomSheetSnap } from './types';

export type { BottomSheetProps, BottomSheetSnap } from './types';

/** snap 프리셋 → 최대 높이 Tailwind 클래스 */
const SNAP_CLASS: Record<BottomSheetSnap, string> = {
  /* auto: 콘텐츠 높이 자동. max-h로 90dvh 상한만 지정 */
  auto: 'max-h-[90dvh]',
  /* half: 화면 절반 고정. 콘텐츠가 넘치면 본문 내부 스크롤 */
  half: 'max-h-[50dvh]',
  /* full: auto와 동일한 90dvh 상한. 콘텐츠가 많은 시트에서 명시적으로 최대 높이 의도를 드러낼 때 사용 */
  full: 'max-h-[90dvh]',
};

/** footer가 없을 때 bottomBtnCnt 기반으로 footer ReactNode를 빌드한다. */
function buildFooter(
  onClose: () => void,
  cnt: '0' | '1' | '2',
  label1: string,
  label2: string,
): React.ReactNode | undefined {
  if (cnt === '0') return undefined;
  return (
    <div className="flex gap-sm">
      {cnt === '2' && (
        <Button variant="outline" size="lg" fullWidth onClick={onClose}>{label2}</Button>
      )}
      <Button variant="primary" size="lg" fullWidth onClick={onClose}>{label1}</Button>
    </div>
  );
}

export function BottomSheet({
  open,
  onClose,
  title,
  children,
  footer,
  snap = 'auto',
  disableBackdropClose = false,
  hideCloseButton = false,
  container,
  bottomBtnCnt = '0',
  bottomBtn1Label = '확인',
  bottomBtn2Label = '취소',
  className,
}: BottomSheetProps) {
  /* footer가 없으면 CMS 브리지 props로 자동 생성 */
  const resolvedFooter = footer ?? buildFooter(onClose, bottomBtnCnt, bottomBtn1Label, bottomBtn2Label);
  /* ESC 키로 닫기 */
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) return;
    document.addEventListener('keydown', handleKeyDown);
    /* 시트가 열린 동안 body 스크롤 잠금 */
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, handleKeyDown]);

  if (!open) return null;

  // container 제공 시 absolute(캔버스 기준), 미제공 시 fixed(뷰포트 기준)
  const positionClass = container ? 'absolute' : 'fixed';

  return createPortal(
    /*
     * 백드롭: container 있으면 absolute inset-0(캔버스 기준), 없으면 fixed inset-0(뷰포트 기준)
     * items-end justify-center: 항상 하단 고정 (Modal처럼 md:items-center 반응형 없음)
     */
    <div
      role="presentation"
      onClick={disableBackdropClose ? undefined : onClose}
      className={`${positionClass} inset-0 z-modal flex items-end justify-center bg-black/50 backdrop-blur-sm`}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'bottom-sheet-title' : undefined}
        onClick={(e) => e.stopPropagation()}
        className={cn(
          'flex flex-col w-full',
          SNAP_CLASS[snap],
          'bg-surface shadow-2xl',
          'rounded-t-2xl',
          className,
        )}
      >
        {/* 드래그 핸들 */}
        <div className="flex shrink-0 justify-center pt-3 pb-1" aria-hidden="true">
          <span className="w-10 h-1 rounded-full bg-border" />
        </div>

        {/* 헤더 — 타이틀 중앙 정렬, 닫기 버튼 우측 절대 배치 */}
        {/* title 또는 닫기 버튼 중 하나라도 있을 때만 헤더 영역 렌더링 */}
        {(title || !hideCloseButton) && (
          <div className="relative flex shrink-0 items-center justify-center px-xl pt-sm pb-md">
            {title && (
              <h2
                id="bottom-sheet-title"
                className="text-base font-bold text-text-heading text-center"
              >
                {title}
              </h2>
            )}
            {/* hideCloseButton=false(기본)일 때만 X 버튼 렌더링.
                absolute 배치로 타이틀 중앙 정렬에 영향을 주지 않음 */}
            {!hideCloseButton && (
              <button
                type="button"
                onClick={onClose}
                aria-label="시트 닫기"
                className={cn(
                  'absolute right-xl flex items-center justify-center size-8 rounded-lg',
                  'text-text-muted hover:bg-surface-raised hover:text-text-heading',
                  'transition-colors duration-150',
                )}
              >
                <X className="size-4" aria-hidden="true" />
              </button>
            )}
          </div>
        )}

        {/*
         * 본문 (스크롤 영역)
         * flex-1 + min-h-0: 헤더·푸터 제외 공간을 차지하면서 내부 스크롤 허용
         * pt-md: 헤더 유무와 관계없이 콘텐츠 상단 여백 보장
         *   - 헤더 있음: 헤더 pb-md(12px) + 본문 pt-md(12px) = 24px 간격
         *   - 헤더 없음: 드래그 핸들 pb-1(4px) + 본문 pt-md(12px) = 16px 간격
         */}
        <div
          className="flex-1 min-h-0 overflow-y-auto [&::-webkit-scrollbar]:hidden p-md"
          style={{ scrollbarWidth: 'none' }}
        >
          {children}
        </div>

        {/* 푸터 (고정) — 하단 안전 영역(Safe Area) 여백 pb-safe 포함 */}
        {resolvedFooter && (
          <div className="shrink-0 px-xl pt-md pb-[calc(env(safe-area-inset-bottom,0px)+theme(spacing.xl))]">
            {resolvedFooter}
          </div>
        )}
      </div>
    </div>,
    container ?? document.body,
  );
}
