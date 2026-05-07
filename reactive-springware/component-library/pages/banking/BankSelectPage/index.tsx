/**
 * @file index.tsx
 * @description 금융권 선택 BottomSheet 페이지 컴포넌트.
 *
 * Figma 원본:
 *   - node-id: 1:1282 (은행 탭 — 2열 선택 그리드)
 *   - node-id: 1:1827 (증권사 탭 — 빈 상태 + CTA)
 *
 * 이체 화면에서 출금·입금 금융기관을 선택하는 화면.
 * 은행/증권사 탭으로 구분되며, 각 탭에서 2열 그리드로 기관을 선택한다.
 * 증권사 목록이 비어있으면 EmptyState + "증권사 연결하기" CTA를 표시한다.
 *
 * 화면 구성:
 *   1. BottomSheet — 타이틀("금융권 선택") + X 닫기 버튼
 *   2. TabNav — 은행 | 증권사 탭
 *   3. (은행 탭) Grid cols={2} — SelectableItem 2열 그리드
 *   3. (증권사 탭 / 데이터 있음) Grid cols={2} — SelectableItem 2열 그리드
 *   3. (증권사 탭 / 빈 목록) EmptyState + Footer CTA
 *
 * @param open                 - BottomSheet 열림 여부
 * @param onClose              - 닫기 핸들러
 * @param activeTab            - 현재 활성 탭
 * @param onTabChange          - 탭 전환 핸들러
 * @param banks                - 은행 목록
 * @param selectedBankId       - 선택된 은행 id
 * @param onBankSelect         - 은행 선택 핸들러
 * @param securities           - 증권사 목록 (빈 배열이면 EmptyState 표시)
 * @param selectedSecuritiesId - 선택된 증권사 id
 * @param onSecuritiesSelect   - 증권사 선택 핸들러
 * @param onConnectSecurities  - 증권사 연결하기 CTA 핸들러
 */
import React from 'react';
import { TrendingUp } from 'lucide-react';

/* ── Modules ─────────────────────────────────────────────────────── */
import { BottomSheet } from '../../../modules/common/BottomSheet';
import { TabNav } from '../../../modules/common/TabNav';
import { EmptyState } from '../../../modules/common/EmptyState';
import { SelectableItem } from '../../../modules/common/SelectableItem';

/* ── Layout ──────────────────────────────────────────────────────── */
import { Grid } from '../../../layout/Grid';
import { Stack } from '../../../layout/Stack';

/* ── Core ────────────────────────────────────────────────────────── */
import { Button } from '../../../core/Button';

import type { BankSelectPageProps, BankSelectTab } from './types';

export type { BankSelectPageProps, BankSelectTab, FinancialItem } from './types';

// ── 탭 정의 (고정값이므로 상수로 분리) ──────────────────────────────

const TABS = [
  { id: 'bank'       as const, label: '은행' },
  { id: 'securities' as const, label: '증권사' },
];

// ── 증권사 빈 상태 일러스트 ────────────────────────────────────────

/**
 * 증권사 빈 상태에 표시할 일러스트.
 * Figma 원본: 회색 원형 배경 + 차트 아이콘.
 */
const SecuritiesEmptyIllustration = (
  <div className="flex items-center justify-center size-16 rounded-full bg-surface-raised" aria-hidden="true">
    <TrendingUp className="size-7 text-text-muted" />
  </div>
);

// ── 메인 컴포넌트 ─────────────────────────────────────────────────

export function BankSelectPage({
  open,
  onClose,
  activeTab,
  onTabChange,
  banks = [],
  selectedBankId,
  onBankSelect,
  securities = [],
  selectedSecuritiesId,
  onSecuritiesSelect,
  onConnectSecurities,
}: BankSelectPageProps) {
  /* 증권사 탭이 활성이고 목록이 비어있는지 여부 — 빈 상태 분기에 사용 */
  const isSecuritiesEmpty = activeTab === 'securities' && securities.length === 0;

  /* 탭에 따라 렌더링할 데이터·선택 id·핸들러를 미리 결정 — 그리드 렌더링 중복 제거 */
  const items      = activeTab === 'bank' ? banks      : securities;
  const selectedId = activeTab === 'bank' ? selectedBankId : selectedSecuritiesId;
  const onSelectItem = activeTab === 'bank' ? onBankSelect : onSecuritiesSelect;

  return (
    <BottomSheet
      open={open}
      onClose={onClose}
      title="금융권 선택"
      /* 증권사 빈 상태일 때만 Footer CTA 표시 */
      footer={
        isSecuritiesEmpty && onConnectSecurities ? (
          <Button fullWidth size="lg" onClick={onConnectSecurities}>
            증권사 연결하기
          </Button>
        ) : undefined
      }
    >
      <Stack gap="md">
        {/* 은행/증권사 가로 탭 */}
        <TabNav
          items={TABS}
          activeId={activeTab}
          onTabChange={(id) => onTabChange(id as BankSelectTab)}
        />

        {/* 탭 콘텐츠 */}
        {isSecuritiesEmpty ? (
          /* 증권사 — 빈 상태 */
          <EmptyState
            illustration={SecuritiesEmptyIllustration}
            title="등록된 증권사가 없습니다."
            description={`보유하신 증권 계좌를 연결하고\n한눈에 관리하며 이체해 보세요.`}
          />
        ) : (
          /* 은행 또는 증권사(데이터 있음) — 2열 선택 그리드 */
          <Grid cols={2} gap="sm">
            {items.map((item) => (
              <SelectableItem
                key={item.id}
                icon={item.icon}
                label={item.label}
                selected={selectedId === item.id}
                onClick={() => onSelectItem(item.id)}
              />
            ))}
          </Grid>
        )}
      </Stack>
    </BottomSheet>
  );
}
