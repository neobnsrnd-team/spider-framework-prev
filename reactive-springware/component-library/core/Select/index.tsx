/**
 * @file index.tsx
 * @description 커스텀 드롭다운 Select 컴포넌트.
 *
 * 네이티브 <select>를 대체하며 디자인 토큰 기반 스타일이 열린 상태에서도 일관되게 적용된다.
 * - 트리거 버튼 클릭 시 옵션 목록을 아래에 절대 위치로 표시
 * - 외부 클릭 / ESC 키 입력 시 닫힘
 * - 현재 선택된 항목은 브랜드 색상으로 강조
 *
 * @example
 * <Select
 *   options={[{ value: 'recent', label: '최근순' }, { value: 'old', label: '과거순' }]}
 *   value={sortOrder}
 *   onChange={setSortOrder}
 *   aria-label="정렬 순서"
 * />
 */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@lib/cn';
import type { SelectProps } from './types';

export type { SelectProps, SelectOption } from './types';

export function Select({
  options = [],
  value,
  onChange,
  className,
  'aria-label': ariaLabel,
}: SelectProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedLabel = options.find(o => o.value === value)?.label ?? '';

  /** 외부 클릭 시 드롭다운 닫기 */
  useEffect(() => {
    if (!open) return;
    function handleOutsideClick(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [open]);

  /** ESC 키 입력 시 드롭다운 닫기 */
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open]);

  const handleSelect = useCallback((optValue: string) => {
    onChange(optValue);
    setOpen(false);
  }, [onChange]);

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      {/* ── 트리거 버튼 ───────────────────────────────────────── */}
      <button
        type="button"
        onClick={() => setOpen(prev => !prev)}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={cn(
          'w-full flex items-center justify-between',
          'bg-surface border border-border rounded-2xl',
          'px-[13px] py-[9px]',
          'text-xs text-text-heading',
          'transition-colors duration-150',
          open && 'border-brand-text',
        )}
      >
        <span>{selectedLabel}</span>
        <ChevronDown
          size={8}
          className={cn(
            'shrink-0 text-text-muted transition-transform duration-200',
            open && 'rotate-180',
          )}
          aria-hidden="true"
        />
      </button>

      {/* ── 드롭다운 목록 ─────────────────────────────────────── */}
      {open && (
        <ul
          role="listbox"
          aria-label={ariaLabel}
          className={cn(
            'absolute top-full left-0 right-0 mt-1',
            /* z-overlay(10): sticky 헤더(20) 아래이지만 일반 콘텐츠 위에 표시 */
            'z-[10]',
            'bg-surface border border-border rounded-xl shadow-md',
            'overflow-hidden',
          )}
        >
          {options.map(opt => {
            const isSelected = opt.value === value;
            return (
              <li key={opt.value} role="option" aria-selected={isSelected}>
                <button
                  type="button"
                  onClick={() => handleSelect(opt.value)}
                  className={cn(
                    'w-full flex items-center px-[13px] py-[9px] text-xs text-left',
                    'transition-colors duration-100',
                    /* 선택된 항목: 브랜드 색상 강조 */
                    isSelected
                      ? 'text-brand-text font-medium bg-brand-5'
                      : 'text-text-heading hover:bg-surface-raised',
                  )}
                >
                  {opt.label}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
