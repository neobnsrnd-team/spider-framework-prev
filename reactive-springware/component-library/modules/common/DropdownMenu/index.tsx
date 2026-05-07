/**
 * @file index.tsx
 * @description 드롭다운 메뉴 컴포넌트.
 *
 * 트리거 요소를 클릭하면 항목 목록이 담긴 플로팅 패널이 나타나고,
 * 패널 바깥을 클릭하거나 항목을 선택하면 닫힌다.
 *
 * 접근성:
 * - 트리거에 aria-expanded, aria-haspopup 적용
 * - 패널에 role="menu", 각 항목에 role="menuitem" 적용
 *
 * @example
 * <DropdownMenu
 *   items={[
 *     { label: '내 정보 관리', icon: <User className="size-4" />, onClick: () => {} },
 *     { label: '로그아웃', icon: <LogOut className="size-4" />, onClick: () => {}, variant: 'danger' },
 *   ]}
 *   align="right"
 * >
 *   <button>설정</button>
 * </DropdownMenu>
 *
 * @param children - 드롭다운을 여는 트리거 요소
 * @param items    - 드롭다운 메뉴 항목 목록
 * @param align    - 패널 정렬 방향 ('left' | 'right'). 기본: 'right'
 */
import React, { useRef, useState, useEffect } from 'react';
import { cn } from '@lib/cn';
import type { DropdownMenuProps } from './types';

export function DropdownMenu({
  triggerIcon,
  triggerVariant = 'default',
  triggerAriaLabel,
  children,
  items,
  align = 'right',
  className,
}: DropdownMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  /* 패널 바깥 클릭 시 닫기 */
  useEffect(() => {
    if (!isOpen) return;

    const handleOutsideClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isOpen]);

  const handleItemClick = (onClick: () => void) => {
    onClick();
    setIsOpen(false);
  };

  return (
    <div ref={containerRef} className={cn('relative inline-block', className)}>
      {/* 트리거 — triggerIcon 제공 시 내장 버튼, 없으면 children 사용 */}
      {triggerIcon ? (
        <button
          type="button"
          aria-label={triggerAriaLabel}
          aria-expanded={isOpen}
          aria-haspopup="menu"
          onClick={() => setIsOpen((prev) => !prev)}
          className={cn(
            'transition-colors duration-150',
            triggerVariant === 'rounded'
              ? 'flex items-center justify-center size-10 rounded-full shrink-0 bg-surface-raised border border-border hover:bg-surface'
              : 'p-2 rounded text-text-muted hover:bg-surface-subtle',
          )}
        >
          {triggerIcon}
        </button>
      ) : (
        <div
          role="button"
          tabIndex={0}
          aria-expanded={isOpen}
          aria-haspopup="menu"
          onClick={() => setIsOpen((prev) => !prev)}
          onKeyDown={(e) => e.key === 'Enter' && setIsOpen((prev) => !prev)}
        >
          {children}
        </div>
      )}

      {/* 플로팅 패널 — isOpen일 때만 렌더링 */}
      {isOpen && (
        <div
          role="menu"
          className={cn(
            'absolute top-full mt-xs z-50 min-w-35',
            'bg-surface border border-border rounded-md shadow-md',
            /* align prop에 따라 패널 위치 결정 */
            align === 'right' ? 'right-0' : 'left-0',
          )}
        >
          {items.map((item, index) => (
            <button
              key={index}
              type="button"
              role="menuitem"
              onClick={() => handleItemClick(item.onClick)}
              className={cn(
                'flex items-center gap-sm w-full px-md py-sm text-sm text-left',
                'transition-colors duration-150',
                /* 첫 항목 위쪽 모서리 둥글게, 마지막 항목 아래쪽 모서리 둥글게 */
                index === 0 && 'rounded-t-md',
                index === items.length - 1 && 'rounded-b-md',
                /* variant에 따라 텍스트 색상 분기 */
                item.variant === 'danger'
                  ? 'text-danger hover:bg-danger-surface'
                  : 'text-text-base hover:bg-surface-raised',
              )}
            >
              {item.icon && (
                <span className="shrink-0" aria-hidden="true">
                  {item.icon}
                </span>
              )}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
