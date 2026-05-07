/**
 * @file layouts.tsx
 * @description CMS LayoutRenderer 구현.
 * CMS 빌더의 캔버스·미리보기 영역에서 페이지 크롬(헤더·푸터)을 렌더링한다.
 *
 * 지원 layoutType:
 * - "home"  : HomePageLayout 헤더 + BottomNav 푸터
 * - "page"  : PageLayout 헤더 + 선택적 하단 버튼 바 푸터
 * - "blank" : 헤더·푸터 없이 콘텐츠만 렌더링
 *
 * @example
 * // your-cms-app/src/main.tsx
 * import { layoutRenderer } from "@reactivespringware/component-library/cms/layouts";
 * <CMSApp blocks={blocks} layoutRenderer={layoutRenderer} />
 */
import React from 'react';
import type { LayoutTemplate } from "@cms-core";
import { resolveIcon } from "@cms-core";
import {
  ChevronLeft,
  User,
  Bell,
  Menu,
  Home,
  Wallet,
  ShoppingBag,
  CreditCard,
  MessageSquare,
  X,
  MenuIcon,
} from 'lucide-react';
import { cn } from '@lib/cn';
import { Button, ButtonGroup, BottomNav } from '@cl';

// ─────────────────────────────────────────────────────────────────────────────
// HomePageLayout 헤더
// ─────────────────────────────────────────────────────────────────────────────

/**
 * HomePageLayout의 상단 헤더 전용 컴포넌트.
 * CMS 캔버스에서 header 슬롯으로 렌더링된다.
 *
 * @param title - 헤더 타이틀 (예: '하나카드')
 * @param logo - 헤더 아이콘 이름 (예: landmark)
 */
function HomeHeader({ title, logo }: { title:string; logo?:string; }) {
  const iconBtnCls = cn(
    'flex items-center justify-center size-9 rounded-full',
    'text-text-muted hover:bg-surface-raised hover:text-text-heading',
    'transition-colors duration-150',
  );

  return (
    <header className="sticky top-0 z-sticky backdrop-blur-sm bg-white/80 border-b border-border-subtle">
      <div className="flex items-center h-14 px-standard gap-sm">
        <div className="flex-1 flex flex-col justify-center">
          <div className="flex items-center gap-xs">
            {/* 로고 아이콘 — logo 전달 시만 노출 */}
            {logo && resolveIcon(logo, "size-4")}
            {/* 타이틀: 브랜드 컬러(teal) + 볼드 — Figma node 1:226 */}
            <h1 className="text-xl font-bold text-brand leading-none">{title}</h1>
          </div>
        </div>
        <div className="shrink-0">
            <div className="flex items-center gap-1">
              {/* 프로필 버튼 */}
              <button type="button" aria-label="프로필" className={iconBtnCls}>
                <User className="size-4" aria-hidden="true" />
              </button>

              {/* 알림(벨) 버튼 — hasNotification 시 빨간 뱃지 표시 */}
              <button type="button" aria-label="알림" className={cn(iconBtnCls, 'relative')}>
                <Bell className="size-4" aria-hidden="true" />
                  <span
                    className="absolute top-1.5 right-1.5 size-2 rounded-full bg-danger-badge border-2 border-white"
                    aria-hidden="true"
                  />
              </button>

              {/* 메뉴 버튼 */}
              <button type="button" aria-label="메뉴" className={iconBtnCls}>
                <Menu className="size-4" aria-hidden="true" />
              </button>
            </div>
        </div>
      </div>
    </header>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// HomePageLayout 푸터 (BottomNav)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 BottomNav 정적 렌더링.
 * 클릭 이벤트 없이 시각적 구조만 제공한다.
 * 
 * @param activeId 활성화 탭 지정 (기본 홈 탭 활성화)
 */
function HomeFooter({ activeId }: { activeId: string; }) {
  const tabs = [
    { id: 'asset',   icon: <Wallet className="size-5" />,        label: '자산',   onClick: () => {}  },
    { id: 'product', icon: <ShoppingBag className="size-5" />,   label: '상품',   onClick: () => {}  },
    { id: 'home',    icon: <Home className="size-6" />,          label: '홈',     onClick: () => {}    },
    { id: 'card',    icon: <CreditCard className="size-5" />,    label: '카드',   onClick: () => {}  },
    { id: 'chat',    icon: <MessageSquare className="size-5" />, label: '챗봇',   onClick: () => {}  },
  ];

  return (
    <BottomNav items={tabs} activeId={activeId} />
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// PageLayout 헤더
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PageLayout의 상단 헤더 전용 컴포넌트.
 * CMS 캔버스에서 header 슬롯으로 렌더링된다.
 *
 * @param title    - 헤더 타이틀
 * @param showBack - true이면 좌측에 뒤로가기(<) 버튼 표시
 * @param rightBtnType - 헤더 오른쪽 버튼 종류 ('close', 'menu', 'none')
 */
function PageHeader({ title, showBack, rightBtnType }: { title:string; showBack:boolean; rightBtnType:string; }) {
  return (
    <header className="sticky top-0 z-sticky bg-surface border-b border-border-subtle">
      <div className="relative flex items-center h-14 px-standard">
        {showBack && (
          <button
            type="button"
            onClick={() => {}}
            aria-label="이전 페이지로 이동"
            className={cn(
              'flex items-center justify-center size-9 rounded-lg -ml-sm',
              'text-text-muted hover:bg-surface-raised hover:text-text-heading',
              'transition-colors duration-150',
            )}
          >
            <ChevronLeft className="size-5" aria-hidden="true" />
          </button>
        )}
        <h1 className="absolute inset-x-0 px-14 text-center text-base font-bold text-text-heading truncate pointer-events-none">{title}</h1>
        { rightBtnType == 'close' && <div className="ml-auto shrink-0"><X className="size-5" aria-hidden="true" /></div> }
        { rightBtnType == 'menu' && <div className="ml-auto shrink-0"><MenuIcon className="size-5" aria-hidden="true" /></div> }
      </div>
    </header>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// PageLayout 푸터 (하단 고정 버튼 바)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PageLayout의 하단 고정 버튼 바 전용 컴포넌트.
 * buttonCount에 따라 1개(primary) 또는 2개(secondary + primary) 버튼을 렌더링한다.
 *
 * @param bottomBtnCnt  - 하단 고정 버튼 개수
 * @param footerButton1Label - 버튼1 텍스트
 * @param footerButton2Label - 버튼2 텍스트
 */
function PageFooter({ bottomBtnCnt, bottomBtn1Label, bottomBtn2Label }: { bottomBtnCnt: "0" | "1" | "2", bottomBtn1Label: string; bottomBtn2Label: string; }) {
  return (
    <ButtonGroup className="sticky bottom-0 left-0 right-0 z-sticky backdrop-blur-sm bg-surface/80 border-t border-border-subtle px-standard pt-standard pb-2xl">
      {/* 2버튼 모드: secondary(outline) + primary 순서 */}
      {bottomBtnCnt == "2" && (
        <Button variant="outline" size="lg" fullWidth>{bottomBtn2Label}</Button>
      )}
      <Button variant="primary" size="lg" fullWidth>{bottomBtn1Label}</Button>
    </ButtonGroup>
  );
}



// ─── 페이지 래퍼 컴포넌트 (생성된 코드에서 사용) ─────────────────────────────

/**
 * @description 홈 페이지 레이아웃 래퍼 컴포넌트.
 * generateJSX로 생성된 코드에서 import하여 사용합니다.
 *
 * @param title       헤더 타이틀
 * @param logo        로고 아이콘 이름
 * @param withBottomNav 하단 탭 바 표시 여부 (기본값: true)
 * @param activeId    하단 탭 바 활성화 ID (기본값: "home")
 * @param children    페이지 콘텐츠
 */
export function HomePageLayout({
  title = "홈",
  logo,
  withBottomNav = true,
  activeId = "home",
  children,
}: {
  title?: string;
  logo?: string;
  withBottomNav?: boolean;
  activeId?: string;
  children?: React.ReactNode;
}) {
  const tabs = [
    { id: "asset",   icon: <Wallet className="size-5" />,        label: "자산",  onClick: () => {} },
    { id: "product", icon: <ShoppingBag className="size-5" />,   label: "상품",  onClick: () => {} },
    { id: "home",    icon: <Home className="size-6" />,           label: "홈",    onClick: () => {} },
    { id: "card",    icon: <CreditCard className="size-5" />,     label: "카드",  onClick: () => {} },
    { id: "chat",    icon: <MessageSquare className="size-5" />,  label: "챗봇",  onClick: () => {} },
  ];
  return (
    <div className="flex flex-col min-h-screen">
      <HomeHeader title={title} logo={logo} />
      <main className="flex-1">{children}</main>
      {withBottomNav && <BottomNav items={tabs} activeId={activeId} />}
    </div>
  );
}

/**
 * @description 일반 페이지 레이아웃 래퍼 컴포넌트.
 * generateJSX로 생성된 코드에서 import하여 사용합니다.
 *
 * @param title           헤더 타이틀
 * @param showBack        뒤로가기 버튼 표시 여부 (기본값: true)
 * @param rightBtnType    헤더 오른쪽 버튼 종류 ("close" | "menu" | "none", 기본값: "close")
 * @param bottomBtnCnt    하단 버튼 수 ("0" | "1" | "2", 기본값: "0")
 * @param bottomBtn1Label 버튼1 텍스트 (기본값: "확인")
 * @param bottomBtn2Label 버튼2 텍스트 (기본값: "취소")
 * @param children        페이지 콘텐츠
 */
export function PageLayout({
  title = "페이지",
  showBack = true,
  rightBtnType = "none",
  bottomBtnCnt = "0",
  bottomBtn1Label = "확인",
  bottomBtn2Label = "취소",
  children,
}: {
  title?: string;
  showBack?: boolean;
  rightBtnType?: "close" | "menu" | "none";
  bottomBtnCnt?: "0" | "1" | "2";
  bottomBtn1Label?: string;
  bottomBtn2Label?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
      <PageHeader title={title} showBack={showBack} rightBtnType={rightBtnType} />
      <main className="flex-1">{children}</main>
      {bottomBtnCnt !== "0" && (
        <PageFooter
          bottomBtnCnt={bottomBtnCnt}
          bottomBtn1Label={bottomBtn1Label}
          bottomBtn2Label={bottomBtn2Label}
        />
      )}
    </div>
  );
}

// ─── LayoutTemplate 목록 ──────────────────────────────────────────────────────

/**
 * CMS 빌더에 제공하는 레이아웃 템플릿 목록.
 * CMSApp의 `layouts` prop에 그대로 전달합니다.
 *
 * | id          | 설명                                 |
 * |-------------|--------------------------------------|
 * | page        | 헤더 + 내용 (+ 선택적 하단 버튼)       |
 * | home        | 홈 헤더 + 내용 + 탭 바               |
 * | blank       | 헤더 없음, 전체 화면                  |
 */
export const layouts: LayoutTemplate[] = [
  // ── Home ─────────────────────────────────────────────────────
  {
    id: "home",
    label: "Home Page Layout",
    description: "홈 레이아웃 (홈 헤더 + 내용 + 바텀 탭 바)",
    componentName: "HomePageLayout",
    defaultProps: {
      title: "홈 타이틀",
      logo: "landmark",
      withBottomNav: true,
      activeId: "home"
    },
    propSchema: {
      title:         { type: "string",      label: "타이틀",              default: "홈 타이틀" },
      logo:          { type: "icon-picker", label: "로고 아이콘",          default: "landmark" },
      withBottomNav: { type: "boolean",     label: "하단 바텀 탭 바 유무", default: true },
      activeId:      { type: "select",      label: "하단 메뉴 활성화 탭",  options: ["asset", "product", "home", "card", "chat"], default: "home" },
    },
    renderer: (p) => ({
      header: <HomeHeader title={(p.title as string | undefined) ?? "홈 타이틀"} logo={p.logo as string | undefined} />,
      footer: (p.withBottomNav as boolean | undefined) !== false
        ? <HomeFooter activeId={(p.activeId as string | undefined) ?? "home"} />
        : undefined,
    }),
  },
  // ── Page ─────────────────────────────────────────────────────
  {
    id: "page",
    label: "Page Layout",
    description: "페이지 레이아웃 (헤더 + 내용 + 하단 버튼)",
    componentName: "PageLayout",
    defaultProps: {
      title: "페이지 제목",
      showBack: true,
      rightBtnType: "close",
      bottomBtnCnt: "0",
      bottomBtn1Label: "확인",
      bottomBtn2Label: "취소"
    },
    propSchema: {
      title:           { type: "string",  label: "타이틀",          default: "페이지 제목" },
      showBack:        { type: "boolean", label: "뒤로가기 버튼",    default: true },
      rightBtnType:    { type: "select",  label: "헤더 오른쪽 버튼", options: ["close", "menu", "none"], default: "close" },
      bottomBtnCnt:    { type: "select",  label: "하단 버튼 수",     options: ["0", "1", "2"],           default: "0" },
      bottomBtn1Label: { type: "string",  label: "버튼1 텍스트",     default: "확인" },
      bottomBtn2Label: { type: "string",  label: "버튼2 텍스트",     default: "취소" },
    },
    renderer: (p) => {
      const bottomBtnCnt = (p.bottomBtnCnt as "0" | "1" | "2" | undefined) ?? "0";
      return {
        header: (
          <PageHeader
            title={(p.title as string | undefined) ?? "페이지 제목"}
            showBack={(p.showBack as boolean | undefined) ?? true}
            rightBtnType={(p.rightBtnType as "close" | "menu" | "none" | undefined) ?? "close"}
          />
        ),
        footer: bottomBtnCnt !== "0" ? (
          <PageFooter
            bottomBtnCnt={bottomBtnCnt}
            bottomBtn1Label={(p.bottomBtn1Label as string | undefined) ?? "확인"}
            bottomBtn2Label={(p.bottomBtn2Label as string | undefined) ?? "취소"}
          />
        ) : undefined,
      };
    },
  },
  // ── Blank ───────────────────────────────────────────────
  {
    id: "blank",
    label: "Blank Page Layout",
    componentName: "BlankPageLayout",
    description: "빈 레이아웃",
    defaultProps: {},
  },
];