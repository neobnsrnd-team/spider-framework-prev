/**
 * @file index.tsx
 * @description 내카드관리 화면 카드 관리 패널 컴포넌트.
 *
 * SectionHeader로 "카드 관리" 제목을 표시하고,
 * rows prop으로 전달된 네비게이션 행(레이블 + 서브텍스트 + ChevronRight)을 세로로 배치한다.
 * rows 배열에 항목을 추가하면 행이 동적으로 늘어난다.
 *
 * 반응형 동작:
 * - 행 패딩: py-md → sm:py-lg
 * - 레이블: text-sm → sm:text-base
 * - 서브텍스트: text-xs → sm:text-sm
 *
 * @param rows      - 네비게이션 행 목록. { label, subText?, onClick? } 배열
 *
 * @example
 * <CardManagementPanel
 *   rows={[
 *     { label: '카드정보 확인', subText: '1234 **** **** 5678', onClick: () => {} },
 *     { label: '결제계좌', subText: '하나은행 123-****-5678', onClick: () => {} },
 *     { label: '카드 비밀번호 설정', onClick: () => {} },
 *     { label: '해외 결제 신청', onClick: () => {} },
 *   ]}
 * />
 */
import React from 'react';
import { ChevronRight } from 'lucide-react';
import { cn } from '@lib/cn';
import { SectionHeader } from '../../../modules/common/SectionHeader';
import type { CardManagementNavRow, CardManagementPanelProps } from './types';

/** 카드 관리 네비게이션 단일 행 */
function NavRow({ label, subText, onClick }: CardManagementNavRow) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'flex items-center justify-between w-full',
        'sm:py-md px-md mt-2 p-md',
        'border border-border-subtle rounded-md hover:bg-surface-subtle transition-colors duration-150',
      )}
    >
      <span className="text-sm sm:text-base font-medium text-text-heading">{label}</span>
      <div className="flex items-center gap-xs shrink-0">
        {subText && <span className="text-xs sm:text-sm text-text-muted">{subText}</span>}
        <ChevronRight size={16} className="text-text-muted" aria-hidden="true" />
      </div>
    </button>
  );
}

export function CardManagementPanel({ rows = [], className }: CardManagementPanelProps) {
  return (
    <div className={cn('flex flex-col', className)}>
      {/* SectionHeader — "카드 관리" 섹션 제목 */}
      <SectionHeader title="카드 관리" className="mb-xs" />

      {/* border로 감싸 행 목록을 하나의 카드처럼 표시 */}
      <div className="overflow-hidden">
        {rows.map((row, index) => (
          /* subText가 없는 행도 있으므로 label+index로 key 구성 */
          <NavRow key={`${row.label}-${index}`} {...row} />
        ))}
      </div>
    </div>
  );
}
