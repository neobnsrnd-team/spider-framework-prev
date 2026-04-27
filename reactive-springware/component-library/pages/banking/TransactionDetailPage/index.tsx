/**
 * @file index.tsx
 * @description 거래 상세 정보 바텀시트 화면 컴포넌트.
 *
 * Figma 원본: node-id 1:1159 (Hana Bank Transaction History Detail)
 *
 * 화면 구성:
 *   - 배경: 거래내역 조회 화면 위에 반투명 오버레이
 *   - 바텀시트 헤더: "거래 상세 정보" 타이틀 + 닫기(X) 버튼
 *   - 금액 영역: 대형 금액 텍스트 + 거래 유형 배지 + 거래 일시
 *   - 상세 항목 행: 메모(편집 가능), 거래구분, 거래일시, 거래내용,
 *                  출금금액, 상대계좌(이체하기 버튼), 상대계좌 예금주명
 *   - 하단 고정 "확인" 버튼
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 상태와 핸들러는 useTransactionDetail 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *   - 여기서는 Storybook 시각 확인 목적으로만 예외 적용한다.
 *
 * @param initialOpen  - 바텀시트 초기 오픈 여부 (기본: true)
 * @param mockData     - 렌더링할 거래 상세 목업 데이터
 * @param isLoading    - 로딩 상태 시뮬레이션 여부
 * @param editingMemo  - 메모 편집 모드 초기화 여부
 */
import React, { useState, useEffect } from 'react';
import { Pencil, Check, X } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack }      from '../../../layout/Stack';
import { Inline }     from '../../../layout/Inline';

/* ── Core ────────────────────────────────────────────────────── */
import { Badge }      from '../../../core/Badge';
import { Button }     from '../../../core/Button';
import { Input }      from '../../../core/Input';
import { Typography } from '../../../core/Typography';

/* ── Modules ─────────────────────────────────────────────────── */
import { BottomSheet }                      from '../../../modules/common/BottomSheet';
import { CardRowPlain, CardActionRowPlain } from '../../../modules/common/Card';
import { EmptyState }                       from '../../../modules/common/EmptyState';

import type { TransactionDetailPageProps, TransactionDetailMockData } from './types';

// ── Mock 데이터 (Storybook 전용) ───────────────────────────────

/** Figma 시안 기준 출금 거래 상세 샘플 */
export const MOCK_WITHDRAWAL: TransactionDetailMockData = {
  type:                 'withdrawal',
  displayAmount:        '- 50,000원',
  displayDate:          '2023.11.01 14:20:05',
  memo:                 '스타벅스 커피',
  transactionCategory:  '체크카드',
  description:          '스타벅스 강남역점',
  counterAccount:       '하나 123-456-789012',
  counterAccountHolder: '스타벅스코리아',
};

/** Figma 시안 기준 입금 거래 상세 샘플 */
export const MOCK_DEPOSIT: TransactionDetailMockData = {
  type:                 'deposit',
  displayAmount:        '+ 2,500,000원',
  displayDate:          '2023.11.01 09:00:12',
  memo:                 '11월 급여',
  transactionCategory:  '자동이체',
  description:          '(주)하나소프트',
  counterAccount:       '하나 987-654-321098',
  counterAccountHolder: '(주)하나소프트',
};

// ── 서브 컴포넌트 ──────────────────────────────────────────────

/**
 * 거래 금액 헤더 섹션.
 * 대형 금액 텍스트(출금 danger / 입금 success), 거래 유형 배지, 거래 일시를 표시한다.
 *
 * @param displayAmount - 표시용 금액 문자열 (예: '- 50,000원')
 * @param type          - 거래 유형 (출금/입금에 따라 색상·배지 변형 결정)
 * @param displayDate   - 표시용 거래 일시 문자열 (예: '2023.11.01 14:20:05')
 */
function TransactionAmountHeader({
  displayAmount,
  type,
  displayDate,
}: Pick<TransactionDetailMockData, 'displayAmount' | 'type' | 'displayDate'>) {
  /* 출금은 danger 배지, 입금은 success 배지 */
  const badgeVariant = type === 'withdrawal' ? 'danger' : 'success';
  const typeLabel    = type === 'withdrawal' ? '출금'   : '입금';

  return (
    <Stack gap="sm" align="center" className="py-md border-b border-border-subtle">
      {/* 대형 금액 텍스트 — 출금은 danger, 입금은 success 색상 */}
      <Typography
        variant="heading"
        color={type === 'deposit' ? 'success' : 'danger'}
        numeric
        as="span"
      >
        {displayAmount}
      </Typography>

      {/* 거래 유형 배지 + 거래 일시 */}
      <Inline gap="sm" align="center">
        <Badge variant={badgeVariant}>{typeLabel}</Badge>
        <Typography variant="caption" color="muted">
          {displayDate}
        </Typography>
      </Inline>
    </Stack>
  );
}

/**
 * 메모 행 컴포넌트.
 * - 표시 모드: 메모 텍스트 + 연필 아이콘 버튼 (클릭 시 편집 모드 진입)
 * - 편집 모드: 텍스트 입력 필드 + 저장(✓) / 취소(✗) 버튼
 *
 * @param value       - 현재 메모 값
 * @param isEditing   - 편집 모드 여부
 * @param isSaving    - 저장 진행 중 여부
 * @param onEditStart - 연필 클릭 → 편집 모드 진입
 * @param onEditCancel - 취소 클릭 → 편집 모드 종료 및 원래 값 복구
 * @param onChange    - 입력값 변경 핸들러
 * @param onSave      - 저장 클릭 → API 호출 후 편집 모드 종료
 */
function MemoRow({
  value,
  isEditing,
  isSaving,
  onEditStart,
  onEditCancel,
  onChange,
  onSave,
}: {
  value:        string;
  isEditing:    boolean;
  isSaving:     boolean;
  onEditStart:  () => void;
  onEditCancel: () => void;
  onChange:     (v: string) => void;
  onSave:       () => void;
}) {
  if (isEditing) {
    return (
      <CardActionRowPlain label="메모">
        <Inline gap="xs" align="center">
          <Input
            value={value}
            onChange={e => onChange(e.target.value)}
            size="md"
            placeholder="메모를 입력하세요"
            /* 자동 포커스: 편집 모드 진입 시 즉시 타이핑 가능하도록 */
            autoFocus
            /* Enter 키로 저장, Escape 키로 취소 */
            onKeyDown={e => {
              if (e.key === 'Enter')  onSave();
              if (e.key === 'Escape') onEditCancel();
            }}
          />
          {/* 저장 버튼 */}
          <Button
            variant="primary" size="sm" iconOnly
            leftIcon={<Check className="size-3" aria-hidden="true" />}
            onClick={onSave}
            loading={isSaving}
            aria-label="메모 저장"
          />
          {/* 취소 버튼 */}
          <Button
            variant="ghost" size="sm" iconOnly
            leftIcon={<X className="size-3" aria-hidden="true" />}
            onClick={onEditCancel}
            disabled={isSaving}
            aria-label="메모 편집 취소"
          />
        </Inline>
      </CardActionRowPlain>
    );
  }

  return (
    <CardActionRowPlain label="메모">
      <Inline gap="xs" align="center" className="bg-surface-raised border-b border-border px-sm py-xs rounded-sm">
        <Typography variant="body-sm" color="heading" as="span">
          {value || '메모 없음'}
        </Typography>
        {/* pencil 클릭 시 편집 모드로 전환 */}
        <Button
          variant="ghost" size="sm" iconOnly
          leftIcon={<Pencil className="size-3" aria-hidden="true" />}
          onClick={onEditStart}
          aria-label="메모 편집"
        />
      </Inline>
    </CardActionRowPlain>
  );
}

/**
 * 상대계좌 행 컴포넌트.
 * 상대방 계좌번호와 "이체하기" pill 버튼을 나란히 표시한다.
 *
 * @param counterAccount - 상대방 계좌번호 문자열
 * @param onTransfer     - 이체하기 버튼 클릭 핸들러
 */
function CounterAccountRow({
  counterAccount,
  onTransfer,
}: {
  counterAccount: string;
  onTransfer:     () => void;
}) {
  return (
    <CardActionRowPlain label="상대계좌">
      <Typography variant="body-sm" color="heading" as="span">
        {counterAccount}
      </Typography>
      {/*
       * "이체하기": Figma에서 브랜드 색상의 pill 모양 버튼.
       * ghost + className으로 브랜드 배경·텍스트 색상을 덮어 쓴다.
       */}
      <Button
        variant="ghost" size="sm"
        onClick={onTransfer}
        className="rounded-full bg-brand-10 text-brand-text hover:bg-brand-20 px-md py-xs text-xs"
      >
        이체하기
      </Button>
    </CardActionRowPlain>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

export function TransactionDetailPage({
  initialOpen  = true,
  mockData     = MOCK_WITHDRAWAL,
  isLoading    = false,
  editingMemo  = false,
}: TransactionDetailPageProps) {
  /* 스토리북 전용 — 실제 앱에서는 useTransactionDetail 훅에서 받아야 함 */
  const [isOpen,    setIsOpen]    = useState(initialOpen);
  const [memo,      setMemo]      = useState(mockData.memo);
  const [isEditing, setIsEditing] = useState(editingMemo);
  const [isSaving,  setIsSaving]  = useState(false);

  /* Storybook 컨트롤에서 mockData.memo가 변경될 때 memo 상태를 동기화한다.
   * useState 초기값은 첫 렌더링에만 적용되므로, args 변경을 반영하려면
   * useEffect로 명시적으로 동기화해야 한다. */
  useEffect(() => {
    setMemo(mockData.memo);
  }, [mockData.memo]);

  function handleMemoSave() {
    /* 저장 로딩 시뮬레이션 (500ms) */
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      setIsEditing(false);
    }, 500);
  }

  return (
    <div data-brand="hana" data-domain="banking">
      <PageLayout
        title="거래내역 조회"
        onBack={() => console.log('뒤로가기')}
      >
        {/* 바텀시트 트리거 영역 */}
        <section className="flex flex-col items-center justify-center py-2xl px-standard gap-lg">
          <Typography variant="body-sm" color="secondary" className="text-center">
            아래 버튼을 눌러 거래 상세 정보를 확인하세요
          </Typography>
          <Button variant="outline" onClick={() => setIsOpen(true)}>
            거래 상세 보기
          </Button>
        </section>
      </PageLayout>

      {/* 거래 상세 정보 바텀시트 */}
      <BottomSheet
        open={isOpen}
        onClose={() => setIsOpen(false)}
        title="거래 상세 정보"
        footer={
          <Button variant="primary" size="lg" fullWidth onClick={() => setIsOpen(false)}>
            확인
          </Button>
        }
      >
        {/* 로딩 상태 */}
        {isLoading && (
          <EmptyState
            title="불러오는 중"
            description="거래 상세 정보를 가져오고 있습니다."
          />
        )}

        {/* 정상 데이터 렌더링 */}
        {!isLoading && (
          <Stack gap="xs">
            {/* 금액 헤더 섹션 */}
            <TransactionAmountHeader
              displayAmount={mockData.displayAmount}
              type={mockData.type}
              displayDate={mockData.displayDate}
            />

            {/* 상세 항목 목록 */}
            <Stack gap="xs" className="py-xs">
              {/* 메모 — 표시/편집 모드 전환 */}
              <MemoRow
                value={memo}
                isEditing={isEditing}
                isSaving={isSaving}
                onEditStart={() => setIsEditing(true)}
                onEditCancel={() => setIsEditing(false)}
                onChange={setMemo}
                onSave={handleMemoSave}
              />

              <CardRowPlain label="거래구분" value={mockData.transactionCategory} />
              <CardRowPlain label="거래일시" value={mockData.displayDate} />
              <CardRowPlain label="거래내용" value={mockData.description} />

              {/* 금액 — 출금은 danger, 입금은 success 색상 */}
              <CardRowPlain
                label={mockData.type === 'withdrawal' ? '출금금액' : '입금금액'}
                value={mockData.displayAmount}
                valueClassName={mockData.type === 'withdrawal' ? 'text-danger-text' : 'text-success-text'}
              />

              {/* 상대계좌 — 이체하기 버튼 포함 */}
              <CounterAccountRow
                counterAccount={mockData.counterAccount}
                onTransfer={() => console.log('이체하기')}
              />

              <CardRowPlain label="상대계좌 예금주명" value={mockData.counterAccountHolder} />
            </Stack>
          </Stack>
        )}
      </BottomSheet>
    </div>
  );
}
