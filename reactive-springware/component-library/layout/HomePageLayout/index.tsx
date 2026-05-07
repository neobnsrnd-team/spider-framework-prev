/**
 * @file index.tsx
 * @description 홈/메인 대시보드 전용 레이아웃 컴포넌트.
 * layoutType: 'home' (component-map.md §1.3)
 *
 * PageLayout과의 차이:
 * - 뒤로가기 버튼 없음 (홈은 루트 화면)
 * - 인사말(greeting) 영역 지원
 * - 하단 글로벌 탭바(BottomNav) 렌더링 내장
 * - 우측 기본 액션: Bell(알림) 아이콘
 *
 * CMS 브리지:
 * - logo: string(kebab-case 아이콘 이름) | ReactNode
 * - activeId: 하단 탭바 활성 탭 ID
 *
 * @example
 * <HomePageLayout title="하나은행" greeting="홍길동님, 안녕하세요">
 *   <BannerCarousel items={banners} />
 *   <QuickMenuGrid items={menus} />
 * </HomePageLayout>
 */
import React from 'react';
import { Bell, User, Menu, Home, Wallet, ShoppingBag, CreditCard, MessageSquare } from 'lucide-react';
import { cn } from '@lib/cn';
import { BottomNav } from '../BottomNav';
import type { HomePageLayoutProps } from './types';

export type { HomePageLayoutProps } from './types';

/** 헤더 아이콘 버튼 공통 스타일 */
const iconBtnCls = cn(
  'flex items-center justify-center size-9 rounded-full',
  'text-text-muted hover:bg-surface-raised hover:text-text-heading',
  'transition-colors duration-150',
);

/** 홈 하단 탭바 기본 탭 목록 */
const DEFAULT_TABS = [
  { id: 'asset',   icon: <Wallet className="size-5" />,        label: '자산',  onClick: () => {} },
  { id: 'product', icon: <ShoppingBag className="size-5" />,   label: '상품',  onClick: () => {} },
  { id: 'home',    icon: <Home className="size-6" />,           label: '홈',    onClick: () => {} },
  { id: 'card',    icon: <CreditCard className="size-5" />,     label: '카드',  onClick: () => {} },
  { id: 'chat',    icon: <MessageSquare className="size-5" />,  label: '챗봇',  onClick: () => {} },
];

export function HomePageLayout({
  title,
  logo,
  rightAction,
  hasNotification = false,
  withBottomNav = true,
  activeId = 'home',
  bottomNavItems,
  className,
  children,
  ...props
}: HomePageLayoutProps) {
  /* 커스텀 탭이 없으면 기본 탭 사용 */
  const navItems = bottomNavItems ?? DEFAULT_TABS;

  return (
    <div className={cn('flex flex-col h-dvh', className)} {...props}>
      {/* ── 상단 고정 헤더 ────────────────────────────── */}
      {/*
       * backdrop-blur + 반투명 흰 배경: 스크롤 시 콘텐츠가 헤더 아래로 자연스럽게 가려짐.
       * Figma 디자인(node 1:221) 기준: backdrop-blur-sm / bg-white/80 / border-b
       */}
      <header className="sticky top-0 z-sticky backdrop-blur-sm bg-white/80 border-b border-border-subtle">
        <div className="flex items-center h-14 px-standard gap-sm">
          <div className="flex-1 flex flex-col justify-center">
            <div className="flex items-center gap-xs">
              {/* 로고 아이콘 — logo 전달 시만 노출 */}
              {logo && <span aria-hidden="true">{logo}</span>}
              {/* 타이틀: 브랜드 컬러(teal) + 볼드 — Figma node 1:226 */}
              <h1 className="text-xl font-bold text-brand leading-none">{title}</h1>
            </div>
          </div>

          {/* 우측 액션 슬롯 — 미전달 시 프로필·벨·메뉴 기본 3버튼 */}
          <div className="shrink-0">
            {rightAction ?? (
              <div className="flex items-center gap-1">
                {/* 프로필 버튼 */}
                <button type="button" aria-label="프로필" className={iconBtnCls}>
                  <User className="size-4" aria-hidden="true" />
                </button>

                {/* 알림(벨) 버튼 — hasNotification 시 빨간 뱃지 표시 */}
                <button type="button" aria-label="알림" className={cn(iconBtnCls, 'relative')}>
                  <Bell className="size-4" aria-hidden="true" />
                  {hasNotification && (
                    /* 알림 뱃지: Figma node 1:234 — 빨간 원, 흰 테두리 */
                    <span
                      className="absolute top-1.5 right-1.5 size-2 rounded-full bg-danger-badge border-2 border-white"
                      aria-hidden="true"
                    />
                  )}
                </button>

                {/* 메뉴 버튼 */}
                <button type="button" aria-label="메뉴" className={iconBtnCls}>
                  <Menu className="size-4" aria-hidden="true" />
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* ── 스크롤 가능한 본문 영역 ────────────────────── */}
      <main
        className={cn(
          'flex-1 overflow-y-auto [&::-webkit-scrollbar]:hidden',
          /* 좌우 기본 여백 + 상하 여백 — 콘텐츠가 화면 끝에 붙지 않도록 */
          'px-standard py-md',
        )}
        style={{ scrollbarWidth: 'none' }}
      >
        {children}
      </main>

      {/* ── 하단 글로벌 탭바 ──────────────────────────── */}
      {withBottomNav && <BottomNav items={navItems} activeId={activeId} />}
    </div>
  );
}
