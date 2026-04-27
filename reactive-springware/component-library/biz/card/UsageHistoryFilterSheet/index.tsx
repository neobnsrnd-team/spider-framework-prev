/**
 * @file index.tsx
 * @description 카드 이용내역 검색 필터 BottomSheet 컴포넌트.
 *
 * 검색 조건(승인구분·카드구분·카드선택·지역선택·이용구분·조회기간)을 편집하고
 * "조회" 버튼으로 확정한다.
 * 카드 선택 / 월별 선택은 중첩 BottomSheet로 처리한다.
 *
 * 필터 draft 상태·카드 피커·월 피커는 모두 내부에서 관리한다.
 * 외부로는 "조회" 확정 시 onApply(filter)를 호출한다.
 *
 * 월 옵션: 오늘 기준 이번 달 ~ 5개월 전 (총 6개월)
 *
 * @param open        - 시트 열림 여부
 * @param onClose     - 시트 닫기 핸들러
 * @param cardOptions - 카드 선택 목록
 * @param onApply     - 필터 확정 핸들러 (SearchFilter 전달)
 */
import React, { useState, useMemo } from 'react';
import { ChevronRight } from 'lucide-react';
import { cn } from '@lib/cn';

import { Button } from '../../../core/Button';
import { Typography } from '../../../core/Typography';
import { BottomSheet } from '../../../modules/common/BottomSheet';

import type {
  UsageHistoryFilterSheetProps,
  SearchFilter,
  ApprovalType,
  CardType,
  RegionType,
  UsageType,
  PeriodType,
} from './types';

export type { SearchFilter, CardOption, UsageHistoryFilterSheetProps } from './types';

// ── 상수 ────────────────────────────────────────────────────

const DEFAULT_FILTER: SearchFilter = {
  approval: 'approved',
  cardType: 'all',
  selectedCard: 'all',
  region: 'all',
  usageType: 'all',
  period: 'thisMonth',
};

// ── 월 옵션 (이번 달 ~ 5개월 전, 총 6개월) ───────────────

interface MonthOption {
  value: string;
  label: string;
}

function generateMonthOptions(): MonthOption[] {
  const today = new Date();
  const options: MonthOption[] = [];
  for (let offset = 0; offset >= -5; offset--) {
    const d = new Date(today.getFullYear(), today.getMonth() + offset, 1);
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    options.push({
      value: `${year}-${String(month).padStart(2, '0')}`,
      label: `${String(year).slice(2)}년 ${month}월`,
    });
  }
  return options;
}

function todayMonthValue(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

// ── 필터 칩 그룹 ──────────────────────────────────────────

function ChipGroup<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="flex flex-wrap gap-xs">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={cn(
            'px-md py-xs rounded-full text-xs font-medium transition-colors duration-150 border',
            opt.value === value
              ? 'bg-brand text-brand-fg border-brand'
              : 'bg-surface text-text-secondary border-border-subtle hover:bg-surface-raised',
          )}
          aria-pressed={opt.value === value}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function FilterRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-xs">
      <Typography variant="caption" color="muted">
        {label}
      </Typography>
      {children}
    </div>
  );
}

// ── 메인 컴포넌트 ─────────────────────────────────────────

export function UsageHistoryFilterSheet({
  open,
  onClose,
  cardOptions,
  onApply,
  container,
}: UsageHistoryFilterSheetProps) {
  const [filterDraft, setFilterDraft] = useState<SearchFilter>(DEFAULT_FILTER);
  const [cardPickerOpen, setCardPickerOpen] = useState(false);
  const [monthPickerOpen, setMonthPickerOpen] = useState(false);
  const [selectedMonth, setSelectedMonth] = useState(todayMonthValue);

  const monthOptions = useMemo(generateMonthOptions, []);

  const allCardOptions = [{ value: 'all', label: '전체' }, ...cardOptions];
  const selectedCardLabel =
    allCardOptions.find((c) => c.value === filterDraft.selectedCard)?.label ?? '전체';
  const selectedMonthLabel = monthOptions.find((m) => m.value === selectedMonth)?.label ?? '';

  function handleReset() {
    setFilterDraft(DEFAULT_FILTER);
    setSelectedMonth(todayMonthValue());
  }

  function handleApply() {
    onApply({
      ...filterDraft,
      customMonth: filterDraft.period === 'custom' ? selectedMonth : undefined,
    });
    onClose();
  }

  function handleCardSelect(value: string) {
    setFilterDraft((p) => ({ ...p, selectedCard: value }));
    setCardPickerOpen(false);
  }

  function handleMonthSelect(value: string) {
    setSelectedMonth(value);
    setFilterDraft((p) => ({ ...p, period: 'custom', customMonth: value }));
    setMonthPickerOpen(false);
  }

  return (
    <>
      <BottomSheet
        open={open}
        onClose={onClose}
        title="검색"
        snap="full"
        container={container}
        footer={
          <div className="flex gap-sm">
            <Button variant="outline" size="lg" onClick={handleReset} className="w-24 shrink-0">
              초기화
            </Button>
            <Button variant="primary" size="lg" fullWidth onClick={handleApply}>
              조회
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-lg px-lg">
          <FilterRow label="승인구분">
            <ChipGroup<ApprovalType>
              options={[
                { value: 'approved', label: '승인' },
                { value: 'confirmed', label: '결제확정' },
              ]}
              value={filterDraft.approval}
              onChange={(v) => setFilterDraft((p) => ({ ...p, approval: v }))}
            />
          </FilterRow>

          <FilterRow label="카드구분">
            <ChipGroup<CardType>
              options={[
                { value: 'all', label: '전체' },
                { value: 'credit', label: '신용카드' },
                { value: 'check', label: '체크카드' },
              ]}
              value={filterDraft.cardType}
              onChange={(v) => setFilterDraft((p) => ({ ...p, cardType: v }))}
            />
          </FilterRow>

          <FilterRow label="카드선택">
            <button
              type="button"
              onClick={() => setCardPickerOpen(true)}
              className="flex items-center justify-between w-full px-md py-sm bg-surface border border-border rounded-xl text-sm"
            >
              <span className="text-text-heading">{selectedCardLabel}</span>
              <ChevronRight className="size-4 text-text-muted" aria-hidden="true" />
            </button>
          </FilterRow>

          <FilterRow label="지역선택">
            <ChipGroup<RegionType>
              options={[
                { value: 'all', label: '전체' },
                { value: 'domestic', label: '국내' },
                { value: 'overseas', label: '해외' },
              ]}
              value={filterDraft.region}
              onChange={(v) => setFilterDraft((p) => ({ ...p, region: v }))}
            />
          </FilterRow>

          <FilterRow label="이용구분">
            <ChipGroup<UsageType>
              options={[
                { value: 'all', label: '전체' },
                { value: 'lump', label: '일시불' },
                { value: 'installment', label: '할부' },
                { value: 'cashAdvance', label: '단기카드대출' },
                { value: 'cancel', label: '취소' },
              ]}
              value={filterDraft.usageType}
              onChange={(v) => setFilterDraft((p) => ({ ...p, usageType: v }))}
            />
          </FilterRow>

          <FilterRow label="조회기간">
            <ChipGroup<PeriodType>
              options={[
                { value: 'thisMonth', label: '이번달' },
                { value: '1month', label: '1개월' },
                { value: '3months', label: '3개월' },
                { value: 'custom', label: '월별선택' },
              ]}
              value={filterDraft.period}
              onChange={(v) => {
                if (v === 'custom') {
                  setMonthPickerOpen(true);
                } else {
                  setFilterDraft((p) => ({ ...p, period: v, customMonth: undefined }));
                }
              }}
            />
            {filterDraft.period === 'custom' && (
              <button
                type="button"
                onClick={() => setMonthPickerOpen(true)}
                className="flex items-center gap-xs text-xs text-brand hover:underline"
              >
                {selectedMonthLabel}
                <ChevronRight className="size-3" aria-hidden="true" />
              </button>
            )}
          </FilterRow>
        </div>
      </BottomSheet>

      {/* ── 카드 선택 (중첩) ──────────────────────────────── */}
      <BottomSheet
        open={cardPickerOpen}
        onClose={() => setCardPickerOpen(false)}
        title="카드 선택"
        snap="auto"
        container={container}
      >
        <ul className="flex flex-col">
          {allCardOptions.map((opt) => {
            const isSelected = filterDraft.selectedCard === opt.value;
            return (
              <li key={opt.value}>
                <button
                  type="button"
                  onClick={() => handleCardSelect(opt.value)}
                  className={cn(
                    'w-full text-left px-md py-lg text-sm',
                    'border-b border-border-subtle last:border-b-0 transition-colors duration-100',
                    isSelected
                      ? 'text-brand font-bold'
                      : 'text-text-heading hover:bg-surface-raised',
                  )}
                  aria-pressed={isSelected}
                >
                  {opt.label}
                </button>
              </li>
            );
          })}
        </ul>
      </BottomSheet>

      {/* ── 월별 선택 (중첩) ──────────────────────────────── */}
      <BottomSheet
        open={monthPickerOpen}
        onClose={() => setMonthPickerOpen(false)}
        title="조회 기간 선택"
        snap="auto"
        container={container}
      >
        <ul className="flex flex-col">
          {monthOptions.map((opt) => {
            const isSelected = selectedMonth === opt.value;
            return (
              <li key={opt.value}>
                <button
                  type="button"
                  onClick={() => handleMonthSelect(opt.value)}
                  className={cn(
                    'w-full text-left px-md py-lg text-base',
                    'border-b border-border-subtle last:border-b-0 transition-colors duration-100',
                    isSelected
                      ? 'text-brand font-bold'
                      : 'text-text-heading hover:bg-surface-raised',
                  )}
                  aria-pressed={isSelected}
                >
                  {opt.label}
                </button>
              </li>
            );
          })}
        </ul>
      </BottomSheet>
    </>
  );
}
