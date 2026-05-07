/**
 * @file types.ts
 * @description LoanMenuBar 컴포넌트 타입 정의.
 */
import React from 'react'

export interface LoanMenuBarItem {
  id: string
  /** 메뉴 아이콘 (lucide-react ReactNode) */
  icon: React.ReactNode
  /** 메뉴 레이블 텍스트 */
  label: string
  /** 메뉴 클릭 핸들러 */
  onClick: () => void
}

export interface LoanMenuBarProps {
  /** 메뉴 항목 목록. 보통 단기카드대출 / 장기카드대출 / 리볼빙 3종. 미전달 시 빈 바 렌더링 */
  items?: LoanMenuBarItem[]
  className?: string
}
