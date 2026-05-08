/**
 * @file index.tsx
 * @description 하나카드 내카드관리 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: 뒤로가기 / "내카드관리" 타이틀 / 닫기(X) 아이콘
 *   - 카드 선택기: 보유 카드를 가로 스크롤 칩 탭으로 표시, 클릭 시 즉시 전환
 *   - CardVisual: 선택된 카드 이미지·브랜드·카드명
 *   - Divider
 *   - CardLinkedBalance: 연결계좌 잔액 (보기/숨기기 토글)
 *   - CardManagementPanel: 카드 관리 네비게이션 행 목록 (rows prop으로 동적 구성)
 *
 * Storybook 확인 목적으로 내부 useState 사용.
 * 실제 앱 구현 시 모든 상태·핸들러는 useMyCardManagement Hook으로 분리한다.
 *
 * @param cards           - 보유 카드 목록
 * @param initialCardId   - 초기 선택 카드 ID (기본: 첫 번째)
 * @param managementRows  - CardManagementPanel 행 목록
 * @param onBack          - 뒤로가기 핸들러
 * @param onClose         - 닫기 핸들러
 */
import React, { useState, useRef, useEffect } from "react";
import { X } from "lucide-react";

import { PageLayout } from "@cl/layout/PageLayout";
import { Button } from "@cl/core/Button";
import { Divider } from "@cl/modules/common/Divider";
import { CardVisual } from "@cl/biz/card/CardVisual";
import { CardLinkedBalance } from "@cl/biz/card/CardLinkedBalance";
import { CardManagementPanel } from "@cl/biz/card/CardManagementPanel";

import { CardPillTab } from "@cl/biz/card/CardPillTab";
import type { MyCardManagementPageProps } from "./types";
import { cn } from "@lib/cn";

export function MyCardManagementPage({
  cards,
  initialCardId,
  managementRows,
  onCardSelect,
  onBack,
  onClose,
}: MyCardManagementPageProps) {
  const [selectedCardId, setSelectedCardId] = useState(
    initialCardId ?? cards[0]?.id,
  );
  const [balanceHidden, setBalanceHidden] = useState(false);
  const [isCompact, setIsCompact] = useState(false);

  const cardVisualRef = useRef<HTMLDivElement>(null);

  /* CardVisual이 뷰포트에서 벗어나면 compact 모드로 전환 */
  useEffect(() => {
    const el = cardVisualRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => setIsCompact(!entry.isIntersecting),
      {
        threshold: 0.1,
      },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const selectedCard = cards.find((c) => c.id === selectedCardId) ?? cards[0];

  /** 헤더 아이콘 버튼 공통 스타일 */
  const iconBtnCls = cn(
    "flex items-center justify-center size-9 rounded-full",
    "text-text-muted hover:bg-surface-raised hover:text-text-heading",
    "transition-colors duration-150",
  );

  return (
    <PageLayout
      title="내카드관리"
      onBack={onBack}
      rightAction={
        <Button
          variant="ghost"
          size="md"
          iconOnly
          leftIcon={<X className="size-5" />}
          onClick={onClose}
          aria-label="닫기"
          className={iconBtnCls}
        />
      }
    >
      <div className="flex flex-col pt-standard">
        {/* ── 카드 선택 칩 탭 (가로 스크롤) ─────────────
         * 카드가 많아져도 가로 스크롤로 자연스럽게 확장.
         * 선택된 칩만 브랜드 색상 배경으로 강조.*/}
        <div className="overflow-x-auto pb-md [&::-webkit-scrollbar]:hidden">
          <div className="flex gap-xs w-max">
            {cards.map((card) => {
              const isSelected = card.id === selectedCardId;
              return (
                <CardPillTab
                  key={card.id}
                  label={card.name}
                  isSelected={isSelected}
                  onClick={() => { setSelectedCardId(card.id); onCardSelect?.(card.id); }}
                />
              );
            })}
          </div>
        </div>

        {/* ── compact 서브헤더 — 카드 영역이 스크롤 밖으로 벗어날 때 노출
         * top-14: PageLayout 헤더(h-14) 바로 아래 고정 */}
        {isCompact && (
          <div className="sticky top-14 z-10 bg-surface border-b border-border-subtle px-standard py-sm -mx-standard">
            <CardVisual
              cardImage={selectedCard?.image}
              brand={selectedCard?.brand ?? "VISA"}
              cardName={selectedCard?.name ?? ""}
              compact
            />
          </div>
        )}

        {/* ── 카드 비주얼 (풀 모드) ── ref로 IntersectionObserver 관찰 */}
        <div ref={cardVisualRef}>
          <CardVisual
            cardImage={selectedCard?.image}
            brand={selectedCard?.brand ?? "VISA"}
            cardName={selectedCard?.name ?? ""}
            className="pb-lg"
          />
        </div>

        <Divider />

        {/* ── 연결계좌 잔액 ────────────────────────────── */}
        <CardLinkedBalance
          balance={selectedCard?.balance ?? 0}
          hidden={balanceHidden}
          onToggle={() => setBalanceHidden((h) => !h)}
          className="py-lg"
        />

        {/* ── 카드 관리 패널 ───────────────────────────── */}
        <CardManagementPanel
          rows={managementRows}
          className="pb-standard pt-lg"
        />
      </div>
    </PageLayout>
  );
}
