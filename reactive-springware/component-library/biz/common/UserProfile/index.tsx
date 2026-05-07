/**
 * @file index.tsx
 * @description 인증 사용자 프로필 컴포넌트.
 *
 * 전체 메뉴 화면 상단에 위치하며, 아바타 원형 배지·이름·최근 접속 시각·설정 버튼으로 구성된다.
 * 설정 버튼을 클릭하면 '내 정보 관리'와 '로그아웃' 항목이 담긴 드롭다운 메뉴가 표시된다.
 * Figma 원본 node-id: 1:459 (User Profile 영역)
 *
 * @param name                 - 표시할 사용자 이름
 * @param lastLogin            - 최근 접속 일시 (예: '2023.11.01 10:30:15'). 미전달 시 미표시
 * @param onProfileManageClick - 내 정보 관리 클릭 핸들러
 * @param onLogoutClick        - 로그아웃 클릭 핸들러
 * @param className            - 추가 Tailwind 클래스
 *
 * @example
 * <UserProfile
 *   name="김하나님"
 *   lastLogin="2023.11.01 10:30:15"
 *   onProfileManageClick={() => navigate('/profile')}
 *   onLogoutClick={() => logout()}
 * />
 */
import React from 'react';
import { User, Settings, LogOut } from 'lucide-react';
import { cn } from '@lib/cn';
import { DropdownMenu } from '../../../modules/common/DropdownMenu';
import type { UserProfileProps } from './types';

export type { UserProfileProps } from './types';

export function UserProfile({
  name,
  lastLogin,
  onProfileManageClick,
  onLogoutClick,
  className,
}: UserProfileProps) {
  /** 드롭다운 항목 — 전달된 핸들러가 있는 항목만 표시 */
  const menuItems = [
    ...(onProfileManageClick
      ? [
          {
            label: '내 정보 관리',
            icon: <User className="size-4" />,
            onClick: onProfileManageClick,
          },
        ]
      : []),
    ...(onLogoutClick
      ? [
          {
            label: '로그아웃',
            icon: <LogOut className="size-4" />,
            onClick: onLogoutClick,
            variant: 'danger' as const,
          },
        ]
      : []),
  ];

  return (
    <div className={cn('flex items-center justify-between px-standard py-md', className)}>
      {/* 좌측: 아바타 + 이름 + 최근 접속 */}
      <div className="flex items-center gap-md">
        {/* 아바타 원형 배지 — 브랜드 5% 배경, 브랜드 테두리, 사람 아이콘 */}
        <div
          className="flex items-center justify-center size-16 rounded-full bg-brand-5 border border-brand/20 shrink-0"
          aria-hidden="true"
        >
          <User className="size-6 text-brand" />
        </div>

        {/* 사용자 정보 */}
        <div className="flex flex-col gap-xs">
          {/* 이름 — 20px, 기본 폰트 웨이트 */}
          <span className="text-xl text-text-heading leading-tight">{name}</span>

          {/* 최근 접속 — lastLogin이 있을 때만 렌더링 */}
          {lastLogin && <span className="text-xs text-text-muted">최근 접속: {lastLogin}</span>}
        </div>
      </div>

      {/* 우측: 설정 버튼 — 드롭다운 항목이 하나라도 있을 때만 렌더링 */}
      {menuItems.length > 0 && (
        <DropdownMenu
          items={menuItems}
          align="right"
          triggerIcon={<Settings className="size-4 text-text-muted" aria-hidden="true" />}
          triggerVariant="rounded"
          triggerAriaLabel="설정 메뉴"
        />
      )}
    </div>
  );
}
