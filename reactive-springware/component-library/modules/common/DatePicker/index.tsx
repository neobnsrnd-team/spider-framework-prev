/**
 * @file index.tsx
 * @description 날짜 선택 컴포넌트.
 * date-fns 미도입 방침(6.6)에 따라 네이티브 Intl.DateTimeFormat만 사용한다.
 * 달력 UI를 직접 렌더링하며, single / range 두 가지 모드를 지원한다.
 *
 * @example
 * // 단일 날짜
 * const [date, setDate] = useState<Date | null>(null);
 * <DatePicker value={date} onChange={setDate} label="조회 날짜" />
 *
 * // 범위
 * const [range, setRange] = useState<[Date | null, Date | null]>([null, null]);
 * <DatePicker mode="range" rangeValue={range} onRangeChange={setRange} label="조회 기간" />
 */
import React, { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@lib/cn';
import type { DatePickerProps } from './types';

/** 날짜 → 'YYYY년 MM월' 형식 */
function formatYearMonth(d: Date): string {
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long' }).format(d);
}

/** 날짜 → 표시용 문자열 'YYYY.MM.DD' */
function formatDisplay(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}.${m}.${day}`;
}

/** 해당 월의 1일 요일(0=일) 및 마지막 일 계산 */
function getMonthInfo(year: number, month: number) {
  const firstDay  = new Date(year, month, 1).getDay();
  const lastDate  = new Date(year, month + 1, 0).getDate();
  return { firstDay, lastDate };
}

/** 두 Date가 같은 날인지 비교 */
function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth()    === b.getMonth()    &&
    a.getDate()     === b.getDate()
  );
}

/** 날짜가 범위 내에 있는지 확인 */
function isInRange(d: Date, start: Date | null, end: Date | null): boolean {
  if (!start || !end) return false;
  return d > start && d < end;
}

export function DatePicker({
  mode        = 'single',
  value,
  rangeValue  = [null, null],
  onChange,
  onRangeChange,
  minDate,
  maxDate,
  placeholder = '날짜를 선택하세요',
  label,
  disabled    = false,
  open:             openProp,
  onOpenChange,
  anchorRef:        anchorRefProp,
  portalContainer,
  className,
}: DatePickerProps) {
  const today = useMemo(() => new Date(), []);

  /* 제어 모드: openProp이 주입된 경우 내장 open 상태를 사용하지 않음 */
  const isControlled = openProp !== undefined;
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = isControlled ? (openProp ?? false) : internalOpen;

  const [viewYear,  setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());

  const triggerRef  = useRef<HTMLButtonElement>(null);
  const calendarRef = useRef<HTMLDivElement>(null);

  /**
   * 달력 패널의 fixed 위치 — 앵커 요소의 getBoundingClientRect 기반.
   * overflow: hidden/auto 조상 안에서도 달력이 잘리지 않도록 portal로 렌더링한다.
   */
  const [calendarStyle, setCalendarStyle] = useState<React.CSSProperties>({});

  /**
   * isOpen이 true로 바뀔 때 달력 위치를 계산한다.
   * 제어 모드: anchorRefProp(외부 트리거 버튼)를 기준으로 위치를 잡는다.
   * 비제어 모드: 내장 triggerRef를 기준으로 위치를 잡는다.
   */
  useEffect(() => {
    if (!isOpen) return;
    const anchor = anchorRefProp?.current ?? triggerRef.current;
    if (!anchor) return;

    const rect = anchor.getBoundingClientRect();
    const CALENDAR_WIDTH = 288; /* w-72 = 288px */
    /* 오른쪽 화면 밖으로 나가면 오른쪽 정렬로 전환 */
    const left = rect.left + CALENDAR_WIDTH > window.innerWidth
      ? rect.right - CALENDAR_WIDTH
      : rect.left;
    setCalendarStyle({
      position: 'fixed',
      top:      rect.bottom + 4,
      left,
      width:    CALENDAR_WIDTH,
      zIndex:   9999,
    });

    /* 달력 뷰를 현재 선택된 value의 연/월로 맞춘다.
       value가 없으면 오늘 기준으로 유지 */
    const base = mode === 'single' ? value : (rangeValue[0] ?? null);
    if (base) {
      setViewYear(base.getFullYear());
      setViewMonth(base.getMonth());
    }
  }, [isOpen, mode, value, rangeValue, anchorRefProp]);

  /** 달력을 닫는 공통 함수 */
  const closeCalendar = useCallback(() => {
    if (isControlled) onOpenChange?.(false);
    else setInternalOpen(false);
  }, [isControlled, onOpenChange]);

  /** 내장 트리거 버튼 클릭 — 비제어 모드에서만 호출 */
  const handleToggle = useCallback(() => {
    setInternalOpen(o => !o);
  }, []);

  /** 외부 클릭 시 달력 닫기.
   * triggerRef / anchorRefProp / calendarRef 내부 클릭은 무시한다.
   * calendarRef도 체크하지 않으면 날짜 셀 mousedown 시 portal이 먼저 닫혀
   * click 이벤트가 발생하기 전에 날짜 버튼이 DOM에서 사라지고 onChange가 호출되지 않는다. */
  useEffect(() => {
    if (!isOpen) return;
    function handleOutside(e: MouseEvent) {
      if (triggerRef.current?.contains(e.target as Node))       return;
      if (anchorRefProp?.current?.contains(e.target as Node))   return;
      if (calendarRef.current?.contains(e.target as Node))      return;
      closeCalendar();
    }
    document.addEventListener('mousedown', handleOutside);
    return () => document.removeEventListener('mousedown', handleOutside);
  }, [isOpen, closeCalendar, anchorRefProp]);

  /* range 모드에서 첫 번째 클릭 임시 저장 */
  const [rangeStart, setRangeStart] = useState<Date | null>(null);

  const { firstDay, lastDate } = useMemo(
    () => getMonthInfo(viewYear, viewMonth),
    [viewYear, viewMonth],
  );

  /* 달력 셀 배열 생성 (빈 칸 포함) */
  const cells = useMemo(() => {
    const arr: Array<Date | null> = Array(firstDay).fill(null);
    for (let d = 1; d <= lastDate; d++) {
      arr.push(new Date(viewYear, viewMonth, d));
    }
    return arr;
  }, [viewYear, viewMonth, firstDay, lastDate]);

  const prevMonth = useCallback(() => {
    if (viewMonth === 0) { setViewYear(y => y - 1); setViewMonth(11); }
    else setViewMonth(m => m - 1);
  }, [viewMonth]);

  const nextMonth = useCallback(() => {
    if (viewMonth === 11) { setViewYear(y => y + 1); setViewMonth(0); }
    else setViewMonth(m => m + 1);
  }, [viewMonth]);

  const isDisabledDay = useCallback(
    (d: Date) => {
      if (minDate && d < minDate) return true;
      if (maxDate && d > maxDate) return true;
      return false;
    },
    [minDate, maxDate],
  );

  const handleDayClick = useCallback(
    (d: Date) => {
      if (isDisabledDay(d)) return;
      if (mode === 'single') {
        onChange?.(d);
        closeCalendar();
      } else {
        /* range 모드: 첫 클릭 → 시작, 두 번째 클릭 → 종료 */
        if (!rangeStart || (rangeStart && d < rangeStart)) {
          setRangeStart(d);
          onRangeChange?.([d, null]);
        } else {
          onRangeChange?.([rangeStart, d]);
          setRangeStart(null);
          closeCalendar();
        }
      }
    },
    [mode, rangeStart, isDisabledDay, onChange, onRangeChange, closeCalendar],
  );

  /* 트리거 버튼 표시 텍스트 */
  const triggerText = useMemo(() => {
    if (mode === 'single') {
      return value ? formatDisplay(value) : placeholder;
    }
    const [s, e] = rangeValue;
    if (s && e) return `${formatDisplay(s)} ~ ${formatDisplay(e)}`;
    if (s)      return `${formatDisplay(s)} ~`;
    return placeholder;
  }, [mode, value, rangeValue, placeholder]);

  const [s, e] = rangeValue;

  return (
    <div className={cn('relative flex flex-col gap-xs', className)}>
      {label && <span className="text-xs font-bold text-text-label">{label}</span>}

      {/* 내장 트리거 버튼 — 비제어 모드(open prop 미제공)에서만 렌더링
          제어 모드에서는 부모가 anchorRef 버튼을 직접 트리거로 사용한다 */}
      {!isControlled && (
        <button
          ref={triggerRef}
          type="button"
          disabled={disabled}
          onClick={handleToggle}
          aria-haspopup="dialog"
          aria-expanded={isOpen}
          className={cn(
            'h-12 px-standard rounded-lg border text-sm text-left',
            'bg-surface border-border',
            'hover:border-brand-text transition-colors duration-150',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            value || (s ?? null) ? 'text-text-heading font-bold' : 'text-text-placeholder',
          )}
        >
          {triggerText}
        </button>
      )}

      {/* 달력 패널 — overflow:hidden/auto 조상에 잘리지 않도록 portal로 렌더링.
          portalContainer가 제공되면 해당 요소 안으로 portal (CSS @scope 환경 대응) */}
      {isOpen && typeof document !== 'undefined' && createPortal(
        <div
          ref={calendarRef}
          role="dialog"
          aria-label="날짜 선택"
          style={calendarStyle}
          className={cn(
            'bg-surface border border-border rounded-xl shadow-lg',
            'p-md',
          )}
        >
          {/* 월 이동 헤더 */}
          <div className="flex items-center justify-between mb-md">
            <button
              type="button"
              onClick={prevMonth}
              aria-label="이전 달"
              className="flex items-center justify-center size-8 rounded-lg hover:bg-surface-raised text-text-muted"
            >
              <ChevronLeft className="size-4" />
            </button>
            <span className="text-sm font-bold text-text-heading">
              {formatYearMonth(new Date(viewYear, viewMonth))}
            </span>
            <button
              type="button"
              onClick={nextMonth}
              aria-label="다음 달"
              className="flex items-center justify-center size-8 rounded-lg hover:bg-surface-raised text-text-muted"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>

          {/* 요일 헤더 */}
          <div className="grid grid-cols-7 mb-xs">
            {['일', '월', '화', '수', '목', '금', '토'].map(day => (
              <span key={day} className="text-center text-xs text-text-muted py-xs">
                {day}
              </span>
            ))}
          </div>

          {/* 날짜 셀 */}
          <div className="grid grid-cols-7 gap-xs">
            {cells.map((d, i) => {
              if (!d) return <span key={`empty-${i}`} />;

              const isSelected =
                (mode === 'single' && value && isSameDay(d, value)) ||
                (mode === 'range'  && ((s && isSameDay(d, s)) || (e && isSameDay(d, e))));
              const inRange   = mode === 'range' && isInRange(d, s ?? null, e ?? null);
              const isToday   = isSameDay(d, today);
              const isOff     = isDisabledDay(d);

              return (
                <button
                  key={d.toISOString()}
                  type="button"
                  onClick={() => handleDayClick(d)}
                  disabled={isOff}
                  aria-label={formatDisplay(d)}
                  aria-pressed={!!isSelected}
                  className={cn(
                    'relative flex items-center justify-center size-8 rounded-lg text-xs font-medium',
                    'transition-colors duration-100',
                    isSelected && 'bg-brand text-brand-fg font-bold',
                    !isSelected && inRange && 'bg-brand-5 text-brand-text',
                    !isSelected && !inRange && isToday && 'text-brand-text font-bold',
                    !isSelected && !inRange && !isToday && 'text-text-base hover:bg-surface-raised',
                    isOff && 'opacity-30 cursor-not-allowed',
                  )}
                >
                  {d.getDate()}
                </button>
              );
            })}
          </div>
        </div>,
        portalContainer ?? document.body,
      )}
    </div>
  );
}