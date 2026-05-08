/**
 * @file index.tsx
 * @description 즉시결제(선결제) 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / '즉시결제(선결제)' 타이틀 / 닫기(X) 버튼
 *   - 서비스 설명 텍스트: 결제일 이전에 미리 카드대금을 납입하는 서비스 안내
 *   - 결제 수단 버튼 그룹 (2열 + 1열):
 *       즉시결제(선결제) / 건별즉시결제(건별선결제) / 매주 자동 선결제
 *   - 즉시결제 출금 가능시간: 하나은행 카드 + 타행 카드 (PaymentAccountCard)
 *   - 유의사항 아코디언 (항목별 접기/펼치기)
 *
 * @param hanaAccount        - 하나은행 결제계좌 카드 데이터
 * @param otherAccount       - 타행 결제계좌 카드 데이터
 * @param cautions           - 유의사항 항목 목록
 * @param onImmediatePayment - 즉시결제(선결제) 버튼 클릭
 * @param onItemPayment      - 건별즉시결제(건별선결제) 버튼 클릭
 * @param onAutoPayment      - 매주 자동 선결제 버튼 클릭
 * @param onBack             - 뒤로가기 핸들러
 * @param onClose            - 닫기(X) 핸들러
 */
import React from 'react';
import { X } from 'lucide-react';

import { PageLayout }         from '@cl/layout/PageLayout';
import { Button }             from '@cl/core/Button';
import { Typography }         from '@cl/core/Typography';
import { CollapsibleSection } from '@cl/modules/common/CollapsibleSection';
import { Divider }            from '@cl/modules/common/Divider';
import { PaymentAccountCard } from '@cl/biz/card/PaymentAccountCard';

import type { ImmediatePaymentPageProps } from './types';

export function ImmediatePaymentPage({
  hanaAccount,
  otherAccount,
  cautions,
  onImmediatePayment,
  onItemPayment,
  onAutoPayment,
  onBack,
  onClose,
}: ImmediatePaymentPageProps) {
  return (
    <PageLayout
      title="즉시결제(선결제)"
      onBack={onBack}
      rightAction={
        <Button
          variant="ghost"
          size="md"
          iconOnly
          leftIcon={<X className="size-5" />}
          onClick={onClose}
          aria-label="닫기"
        />
      }
    >
      <div className="flex flex-col gap-lg pt-md pb-xl">

        {/* ── 서비스 설명 텍스트 ───────────────────────────────────── */}
        <Typography variant="subheading" color="secondary">
          결제일 이전에 미리 카드대금을
          <br /> 납입하는 서비스입니다.
        </Typography>

        {/* ── 결제 수단 버튼 그룹 ────────────────────────────────────
         * 상단 2열: 즉시결제(선결제) / 건별즉시결제(건별선결제)
         * 하단 1열: 매주 자동 선결제 (전체 너비) */}
        <div className="flex flex-col gap-sm">
          <div className="flex gap-sm">
            <Button variant="outline" size="md" fullWidth onClick={onImmediatePayment}>
              즉시결제(선결제)
            </Button>
            <Button variant="outline" size="md" fullWidth onClick={onItemPayment}>
              건별즉시결제(건별선결제)
            </Button>
          </div>
          <Button variant="outline" size="md" fullWidth onClick={onAutoPayment}>
            매주 자동 선결제
          </Button>
        </div>

        <Divider />

        {/* ── 즉시결제 출금 가능시간 ─────────────────────────────────
         * 하나은행 / 타행 결제계좌를 각각 카드로 표시.
         * 클릭 시 해당 계좌 선택 흐름으로 이동한다. */}
        <div className="flex flex-col gap-sm">
          <Typography variant="body-sm" weight="bold" color="heading">
            즉시결제 출금 가능시간
          </Typography>
          <PaymentAccountCard {...hanaAccount} />
          <PaymentAccountCard {...otherAccount} />
        </div>

        <Divider />

        {/* ── 유의사항 아코디언 ──────────────────────────────────────
         * 각 항목은 기본 접힘(defaultExpanded=false)으로 시작. */}
        <div className="flex flex-col gap-xs">
          <Typography variant="body-sm" weight="bold" color="heading" className="mb-xs">
            유의사항
          </Typography>
          {cautions.map(({ title, content }) => (
            <CollapsibleSection
              key={title}
              header={<Typography variant="body-sm" weight="medium" color="heading">{title}</Typography>}
              defaultExpanded={false}
              headerAlign="left"
              className="px-0! py-1!"
            >
              <Typography variant="caption" color="secondary">{content}</Typography>
            </CollapsibleSection>
          ))}
        </div>

      </div>
    </PageLayout>
  );
}
