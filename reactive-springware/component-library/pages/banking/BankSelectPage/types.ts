/**
 * @file types.ts
 * @description BankSelectPage 컴포넌트의 TypeScript 타입 정의.
 *
 * 이체 화면에서 출금·입금 금융기관을 선택하는 BottomSheet 페이지.
 * 은행 탭(2열 선택 그리드)과 증권사 탭(데이터 없으면 EmptyState)으로 구성된다.
 *
 * Figma 원본:
 *   - node-id: 1:1282 (은행 탭 — 선택 그리드)
 *   - node-id: 1:1827 (증권사 탭 — 빈 상태)
 */
import React from 'react';

/** 선택 가능한 금융기관 항목 */
export interface FinancialItem {
  /** 항목 고유 식별자 */
  id: string;
  /**
   * 아이콘 슬롯.
   * lucide-react 아이콘을 전달한다.
   * 선택 상태에 따라 SelectableItem이 색상을 자동 적용한다.
   */
  icon: React.ReactNode;
  /** 금융기관 명칭 (예: "하나은행", "미래에셋증권") */
  label: string;
}

/** 탭 식별자 */
export type BankSelectTab = 'bank' | 'securities';

export interface BankSelectPageProps {
  /** BottomSheet 열림 여부 */
  open: boolean;
  /** 닫기 핸들러 */
  onClose: () => void;

  /** 현재 활성 탭 */
  activeTab: BankSelectTab;
  /** 탭 전환 핸들러 */
  onTabChange: (tab: BankSelectTab) => void;

  // ── 은행 탭 ──────────────────────────────────────────────────
  /** 은행 목록. 미전달 시 빈 그리드 렌더링 */
  banks?: FinancialItem[];
  /** 선택된 은행 id */
  selectedBankId?: string;
  /** 은행 선택 핸들러 */
  onBankSelect: (id: string) => void;

  // ── 증권사 탭 ─────────────────────────────────────────────────
  /**
   * 증권사 목록.
   * 빈 배열 또는 미전달이면 EmptyState("등록된 증권사가 없습니다.")를 표시한다.
   */
  securities?: FinancialItem[];
  /** 선택된 증권사 id */
  selectedSecuritiesId?: string;
  /** 증권사 선택 핸들러 */
  onSecuritiesSelect: (id: string) => void;
  /**
   * 증권사 빈 상태의 "증권사 연결하기" CTA 핸들러.
   * 미전달 시 CTA 버튼 미노출.
   */
  onConnectSecurities?: () => void;
}
