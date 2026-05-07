/**
 * @file index.tsx
 * @description 은행 선택 그리드 컴포넌트.
 *
 * 이체·계좌 등록 시 수취 은행을 선택하는 그리드 UI.
 * 각 셀은 은행 아이콘 + 이름으로 구성되며, 선택된 항목은 브랜드 색상으로 강조된다.
 * columns prop으로 3열 또는 4열 레이아웃을 선택할 수 있다.
 *
 * @param banks        - 선택 가능한 은행 목록
 * @param selectedCode - 현재 선택된 은행 코드
 * @param onSelect     - 은행 선택 핸들러
 * @param columns      - 한 행 열 수 (기본: 4)
 */
import React from 'react';
import { Landmark } from 'lucide-react';
import { cn } from '@lib/cn';

import { Typography } from '../../../core/Typography';
import type { BankSelectGridProps } from './types';

export type { BankSelectGridProps, BankItem } from './types';

export function BankSelectGrid({ banks = [], selectedCode, onSelect, columns = 4 }: BankSelectGridProps) {
  return (
    <div
      className={cn(
        'grid gap-sm',
        columns === 3 ? 'grid-cols-3' : 'grid-cols-4',
      )}
    >
      {banks.map((bank) => {
        const isSelected = bank.code === selectedCode;
        return (
          <button
            key={bank.code}
            type="button"
            onClick={() => onSelect(bank.code)}
            className={cn(
              'flex flex-col items-center justify-center gap-xs',
              'rounded-xl py-md px-xs border transition-colors duration-150',
              isSelected
                ? 'border-brand bg-brand-10 text-brand'
                : 'border-border bg-surface text-text-secondary hover:bg-surface-raised',
            )}
            aria-pressed={isSelected}
            aria-label={bank.name}
          >
            {/* 은행 로고 — 미전달 시 기본 Landmark 아이콘 사용 */}
            <span className="flex items-center justify-center size-7">
              {bank.icon ?? <Landmark className="size-5" aria-hidden="true" />}
            </span>
            <Typography
              variant="caption"
              weight={isSelected ? 'bold' : 'medium'}
              color={isSelected ? 'brand' : 'secondary'}
            >
              {bank.name}
            </Typography>
          </button>
        );
      })}
    </div>
  );
}
