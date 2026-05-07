/**
 * @file index.tsx
 * @description 하나카드 전체메뉴 페이지 컴포넌트.
 *
 * 화면 구성:
 *   - 상단바: "전체메뉴" 타이틀 + 뒤로가기
 *   - UserProfile: 사용자 이름 / 최근 접속 / 설정 버튼 (클릭 시 내 정보 관리·로그아웃 드롭다운)
 *   - 2열 레이아웃:
 *     - 좌측 SidebarNav: 전체 / 이용내역 / 결제 / 카드관리 / 혜택 / 서비스
 *     - 우측 ActionLinkItem 목록: 선택된 카테고리에 해당하는 메뉴 항목
 *
 * Storybook 확인 목적으로 내부 useState 사용.
 * 실제 앱 구현 시 카테고리 상태와 핸들러는 useHanaCardMenu Hook으로 분리한다.
 *
 * @param onBack           - 뒤로가기 클릭
 * @param onProfileManage  - 내 정보 관리 클릭 (UserProfile 설정 드롭다운)
 * @param onLogout         - 로그아웃 클릭 (UserProfile 설정 드롭다운)
 * @param userName         - 표시할 사용자 이름
 * @param lastLogin        - 최근 접속 일시
 * @param menuItems        - 우측 메뉴 항목 목록 (icon·label·category·onClick 포함)
 */
import React, { useState } from "react";

import { PageLayout } from "@cl/layout/PageLayout";
import { UserProfile } from "@cl/biz/common/UserProfile";
import { SidebarNav } from "@cl/modules/common/SidebarNav";
import { ActionLinkItem } from "@cl/modules/common/ActionLinkItem";

import type { HanaCardMenuPageProps, MenuCategoryId } from "./types";

export function HanaCardMenuPage({
  onBack,
  onProfileManage,
  onLogout,
  userName,
  lastLogin,
  categories = [],
  menuItems = [],
}: HanaCardMenuPageProps) {
  /** Storybook 확인용 내부 상태 — 실제 앱에서는 useHanaCardMenu Hook에서 관리 */
  const [activeCategoryId, setActiveCategoryId] =
    useState<MenuCategoryId>("all");

  /** 선택된 카테고리에 해당하는 항목만 필터링 ('all' 선택 시 전체 노출) */
  const filteredItems =
    activeCategoryId === "all"
      ? menuItems
      : menuItems.filter((item) => item.category === activeCategoryId);

  return (
    <div data-brand="hana" data-domain="card">
      <PageLayout title="전체메뉴" onBack={onBack}>
        {/* ── 사용자 프로필 ─────────────────────────────── */}
        <div className="pt-standard pb-md">
          <UserProfile
            name={userName ?? ""}
            lastLogin={lastLogin}
            onProfileManageClick={onProfileManage}
            onLogoutClick={onLogout}
          />
        </div>

        {/* ── 카테고리 사이드바 + 메뉴 목록 2열 레이아웃 ── */}
        <div className="flex min-h-0 flex-1">
          {/* 좌측: 세로 카테고리 탭 */}
          <SidebarNav
            items={categories}
            activeId={activeCategoryId}
            onItemChange={(id) => setActiveCategoryId(id as MenuCategoryId)}
          />

          {/* 우측: 선택된 카테고리의 메뉴 항목 목록 */}
          <div className="flex-1 overflow-y-auto">
            {filteredItems.map((item) => (
              <ActionLinkItem
                key={item.id}
                icon={item.icon}
                label={item.label}
                showBorder={false}
                size="sm"
                onClick={item.onClick}
              />
            ))}
          </div>
        </div>
      </PageLayout>
    </div>
  );
}
