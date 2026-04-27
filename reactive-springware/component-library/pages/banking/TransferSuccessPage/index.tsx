/**
 * @file index.tsx
 * @description 이체 완료 페이지 컴포넌트.
 *
 * Figma 원본: node-id 1:1641 (Hana Bank Transfer Success)
 *
 * 화면 구성:
 *   - 헤더: "이체 완료" 타이틀 + 우측 닫기(×) 버튼
 *   - 성공 히어로: Confetti 장식 + 성공 아이콘 + 이체 결과 타이틀
 *   - 이체 요약 카드: 받는 계좌 / 내 통장 표시 / 받는 분 통장 표시 / 이체 후 잔액
 *   - 보조 액션: 카카오톡 공유(선택) / 자주 쓰는 계좌 등록
 *   - 하단 고정 버튼 바: 추가 이체(보조) + 확인(주)
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 데이터와 핸들러는 useTransferSuccess 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *
 * @param recipientName        - 받는 사람 이름
 * @param amount               - 이체 금액 (원화 단위 숫자)
 * @param targetAccount        - 받는 계좌 표시 문자열
 * @param myMemo               - 내 통장 표시 메모
 * @param recipientMemo        - 받는 분 통장 표시 메모
 * @param balanceAfterTransfer - 이체 후 잔액 (원화 단위 숫자)
 * @param showKakaoShare       - 카카오톡 공유 액션 표시 여부
 */
import React from 'react';
import { X, MessageSquare, Star } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack }      from '../../../layout/Stack';
import { Inline }     from '../../../layout/Inline';

/* ── Core ────────────────────────────────────────────────────── */
import { Button } from '../../../core/Button';

/* ── Modules ─────────────────────────────────────────────────── */
import { SuccessHero }                      from '../../../modules/common/SuccessHero';
import { ActionLinkItem }                   from '../../../modules/common/ActionLinkItem';
import { Card, CardRowPlain, CardHighlight } from '../../../modules/common/Card';

import type { TransferSuccessPageProps } from './types';

export type { TransferSuccessPageProps } from './types';

// ── 유틸 ──────────────────────────────────────────────────────

/** 숫자를 원화 표시 문자열로 변환 */
function formatKRW(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`;
}

// ── 서브 컴포넌트 ──────────────────────────────────────────────

/**
 * 이체 요약 정보 카드.
 * noPadding Card + CardRowPlain 행 목록 + CardHighlight 잔액 강조 구성.
 *
 * @param targetAccount        - 받는 계좌 표시 문자열
 * @param myMemo               - 내 통장 표시 메모
 * @param recipientMemo        - 받는 분 통장 표시 메모
 * @param balanceAfterTransfer - 이체 후 잔액
 */
function TransferSummaryCard({
  targetAccount,
  myMemo,
  recipientMemo,
  balanceAfterTransfer,
}: Pick<TransferSuccessPageProps, 'targetAccount' | 'myMemo' | 'recipientMemo' | 'balanceAfterTransfer'>) {
  return (
    /* noPadding: CardHighlight가 카드 전체 너비를 채울 수 있도록 padding 직접 제어 */
    <Card noPadding>
      <Stack gap="md" className="p-standard">
        <CardRowPlain label="받는 계좌"         value={targetAccount} />
        <CardRowPlain label="내 통장 표시"      value={myMemo} />
        <CardRowPlain label="받는 분 통장 표시" value={recipientMemo} />
      </Stack>
      <CardHighlight
        label="이체 후 잔액"
        value={formatKRW(balanceAfterTransfer)}
        valueClassName="font-numeric"
      />
    </Card>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

export function TransferSuccessPage({
  recipientName,
  amount,
  targetAccount,
  myMemo,
  recipientMemo,
  balanceAfterTransfer,
  showKakaoShare,
}: TransferSuccessPageProps) {
  return (
    <div data-brand="hana" data-domain="banking">
      <PageLayout
        title="이체 완료"
        /* 닫기(×) 버튼 — 이체 완료 후 히스토리 스택에 남지 않도록 뒤로가기 대신 사용 */
        rightAction={
          <Button
            variant="ghost"
            size="md"
            iconOnly
            leftIcon={<X className="size-4" />}
            onClick={() => console.log('닫기')}
            aria-label="닫기"
            className="text-text-heading"
          />
        }
        /* 하단 고정 버튼 바: 추가 이체(보조) + 확인(주) */
        bottomBar={
          <Inline gap="sm">
            <Button variant="outline" size="lg" onClick={() => console.log('추가 이체')} fullWidth>
              추가 이체
            </Button>
            <Button variant="primary" size="lg" onClick={() => console.log('확인')} fullWidth>
              확인
            </Button>
          </Inline>
        }
      >
        <Stack gap="lg">
          {/* ── 성공 히어로 ── */}
          <section>
            <SuccessHero
              recipientName={recipientName}
              amount={formatKRW(amount)}
            />
          </section>

          {/* ── 이체 요약 카드 ── */}
          <section className="px-standard">
            <TransferSummaryCard
              targetAccount={targetAccount}
              myMemo={myMemo}
              recipientMemo={recipientMemo}
              balanceAfterTransfer={balanceAfterTransfer}
            />
          </section>

          {/* ── 보조 액션 링크 ── */}
          <section>
            <Stack gap="sm" className="px-standard">
              {/* 카카오톡 공유 — showKakaoShare prop으로 표시 여부 제어 */}
              {showKakaoShare && (
                <ActionLinkItem
                  icon={<MessageSquare className="size-5" />}
                  iconBgClassName="bg-[#fee500] text-text-heading"
                  label="이체 결과를 카카오톡으로 공유하기"
                  onClick={() => console.log('카카오 공유')}
                />
              )}
              {/* 자주 쓰는 계좌 등록 */}
              <ActionLinkItem
                icon={<Star className="size-5 text-brand-text" />}
                label="자주 쓰는 계좌로 등록하기"
                onClick={() => console.log('즐겨찾기 등록')}
              />
            </Stack>
          </section>
        </Stack>
      </PageLayout>
    </div>
  );
}
