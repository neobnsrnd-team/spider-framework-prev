/**
 * @file index.tsx
 * @description 일반 페이지 레이아웃 컴포넌트.
 * 상단 고정 헤더(타이틀 + 뒤로가기 + 우측 액션)와 스크롤 가능한 본문 영역으로 구성된다.
 * bottomBar prop 전달 시 iOS 스타일 하단 고정 액션 바를 함께 렌더링한다.
 * layoutType: 'page' (계좌 목록·상세, 이체 폼, 이체 완료 화면 등)
 *
 * CMS 브리지 props (onBack/rightAction/bottomBar 우선, 없으면 스칼라 props로 대체):
 *   showBack, rightBtnType, bottomBtnCnt, bottomBtn1Label, bottomBtn2Label
 *
 * @example
 * // 기본 사용
 * <PageLayout title="이체하기" onBack={() => router.back()}>
 *   <TransferForm ... />
 * </PageLayout>
 *
 * // 하단 고정 버튼 바 포함
 * <PageLayout
 *   title="이체 완료"
 *   rightAction={<CloseButton />}
 *   bottomBar={
 *     <Inline gap="sm">
 *       <Button variant="outline" size="lg">추가 이체</Button>
 *       <Button variant="primary" size="lg" fullWidth>확인</Button>
 *     </Inline>
 *   }
 * >
 *   <SuccessHero ... />
 * </PageLayout>
 */
import React from 'react';
import { ChevronLeft, X, Menu } from 'lucide-react';
import { cn } from '@lib/cn';
import { Button } from '../../core/Button';
import type { PageLayoutProps } from './types';

const backBtnCls = cn(
  'flex items-center justify-center size-9 rounded-lg -ml-sm',
  'text-text-muted hover:bg-surface-raised hover:text-text-heading',
  'transition-colors duration-150',
);

export function PageLayout({
  title,
  onBack,
  rightAction,
  bottomBar,
  showBack,
  rightBtnType = 'none',
  bottomBtnCnt = '0',
  bottomBtn1Label = '확인',
  bottomBtn2Label = '취소',
  className,
  children,
  ...props
}: PageLayoutProps) {
  /* 실제 props 우선, 없으면 CMS 스칼라 props로 대체 */
  const resolvedOnBack = onBack ?? (showBack ? () => {} : undefined);
  const resolvedBottomBar = bottomBar ?? buildBottomBar(bottomBtnCnt, bottomBtn1Label, bottomBtn2Label);
  const resolvedRightAction = rightAction ?? buildRightAction(rightBtnType);

  return (
    /* h-dvh: 컨테이너를 정확히 뷰포트 높이로 고정해 스크롤이 body가 아닌
       하위 main 영역 내부에서만 발생하도록 한다. min-h-dvh 사용 시 body가
       스크롤되어 sticky 헤더와 z-index 충돌이 발생할 수 있음. */
    <div className={cn('flex flex-col h-dvh', className)} {...props}>
      {/* ── 상단 고정 헤더 ────────────────────────────── */}
      <header className="sticky top-0 z-sticky bg-surface border-b border-border-subtle">
        {/* relative: 타이틀 absolute 포지셔닝의 기준점 */}
        <div className="relative flex items-center h-14 px-standard">
          {/* 뒤로가기 버튼 — onBack 또는 showBack이 있는 경우 렌더링 */}
          {resolvedOnBack && (
            <button
              type="button"
              onClick={resolvedOnBack}
              aria-label="이전 페이지로 이동"
              className={backBtnCls}
            >
              <ChevronLeft className="size-5" aria-hidden="true" />
            </button>
          )}

          {/* 페이지 타이틀 — absolute로 헤더 전체 너비 기준 진짜 중앙 정렬.
              px-14(56px): 좌우 버튼(size-9 = 36px + 여백)과 겹치지 않도록 여백 확보.
              pointer-events-none: 절대 위치 타이틀이 좌우 버튼 클릭을 막지 않도록 처리. */}
          <h1 className="absolute inset-x-0 px-14 text-center text-base font-bold text-text-heading truncate pointer-events-none">
            {title}
          </h1>

          {/* 우측 액션 슬롯 (닫기·알림·설정 버튼 등) — ml-auto로 우측 끝에 고정 */}
          {resolvedRightAction && <div className="ml-auto shrink-0">{resolvedRightAction}</div>}
        </div>
      </header>

      {/* ── 스크롤 가능한 본문 영역 ────────────────────── */}
      {/* overflow-x-hidden: overflow-y-auto 설정 시 CSS 스펙에 의해 overflow-x도 auto로
          강제 변경되어 수평 내용이 잘릴 수 있음. 모바일 레이아웃에서 수평 스크롤은
          불필요하므로 hidden으로 명시적으로 차단한다. */}
      {/* px-standard py-md: 좌우 기본 여백 + 상하 여백 — 고객 프로젝트에서 별도 패딩 없이 바로 사용 가능 */}
      {/* flex flex-col: 자식 컴포넌트가 flex-1을 사용해 남은 높이를 채울 수 있도록 flex 컨테이너로 설정 */}
      <main className="flex flex-col flex-1 overflow-y-auto overflow-x-hidden px-standard py-md [&::-webkit-scrollbar]:hidden" style={{ scrollbarWidth: 'none' }}>
        {children}
      </main>

      {/* ── 하단 고정 액션 바 (iOS 스타일) ──────────────
          backdrop-blur: 스크롤 중에도 하단 버튼이 배경에 묻히지 않도록 처리
          sticky: 화면 하단에 항상 고정 위치 */}
      {resolvedBottomBar && (
        <div className="sticky bottom-0 left-0 right-0 z-sticky backdrop-blur-sm bg-surface/80 border-t border-border-subtle px-standard pt-standard pb-2xl">
          {resolvedBottomBar}
        </div>
      )}
    </div>
  );
}

/** rightBtnType으로 헤더 우측 아이콘 버튼 빌드 */
function buildRightAction(type: 'close' | 'menu' | 'none'): React.ReactNode | undefined {
  if (type === 'close') return <X className="size-5" aria-hidden="true" />;
  if (type === 'menu')  return <Menu className="size-5" aria-hidden="true" />;
  return undefined;
}

/** bottomBtnCnt로 하단 버튼 바 빌드 */
function buildBottomBar(
  cnt: '0' | '1' | '2',
  label1: string,
  label2: string,
): React.ReactNode | undefined {
  if (cnt === '0') return undefined;
  return (
    <div className="flex gap-sm">
      {cnt === '2' && (
        <Button variant="outline" size="lg" fullWidth>{label2}</Button>
      )}
      <Button variant="primary" size="lg" fullWidth>{label1}</Button>
    </div>
  );
}
