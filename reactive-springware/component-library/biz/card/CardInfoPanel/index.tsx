/**
 * @file index.tsx
 * @description 카드 도메인 정보 패널 컴포넌트.
 *
 * 결제정보·카드 이용기간 등 섹션 제목과 레이블-값 행 목록을 렌더링한다.
 * 값에 '\n'이 포함된 경우 두 줄로 표시한다.
 * 섹션이 여러 개인 경우 구분선으로 시각적으로 분리한다.
 *
 * @param sections - 섹션 목록 (title + rows)
 *
 * @example
 * <CardInfoPanel
 *   sections={[
 *     {
 *       title: '결제정보',
 *       rows: [
 *         { label: '결제 계좌', value: '하나은행\n123456****1234' },
 *         { label: '결제일',   value: '매월 25일' },
 *       ],
 *     },
 *     {
 *       title: '카드 이용기간',
 *       rows: [
 *         { label: '일시불/할부',          value: '2026.03.13\n~ 2026.04.12' },
 *         { label: '단기카드대출(현금서비스)', value: '2026.02.26\n~ 2026.03.25' },
 *       ],
 *     },
 *   ]}
 * />
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { CardInfoPanelProps, CardInfoRow } from './types';

/** 레이블-값 단일 행 — 값에 '\n' 포함 시 여러 줄로 분리 표시 */
function InfoRow({ label, value }: CardInfoRow) {
  /* '\n'을 기준으로 분리해 각 줄을 개별 span으로 렌더링 */
  const lines = value.split('\n');

  return (
    <div className="flex justify-between items-start gap-md py-xs">
      <span className="text-xs text-text-muted shrink-0">{label}</span>
      <div className="flex flex-col items-end text-right">
        {lines.map((line, i) => (
          <span key={i} className="text-xs text-text-heading">
            {line}
          </span>
        ))}
      </div>
    </div>
  );
}

export function CardInfoPanel({ sections = [], className }: CardInfoPanelProps) {
  return (
    <div className={cn('flex flex-col', className)}>
      {sections.map((section, sectionIdx) => (
        <React.Fragment key={section.title}>
          {/* 섹션 간 구분선 — 첫 번째 섹션에는 미적용 */}
          {sectionIdx > 0 && <hr className="border-border-subtle my-md" />}

          {/* 섹션 제목 */}
          <p className="text-sm font-bold text-text-heading mb-xs">{section.title}</p>

          {/* 행 목록 */}
          <div className="flex flex-col">
            {section.rows.map((row) => (
              <InfoRow key={row.label} label={row.label} value={row.value} />
            ))}
          </div>
        </React.Fragment>
      ))}
    </div>
  );
}
