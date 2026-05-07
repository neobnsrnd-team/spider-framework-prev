/**
 * @file index.tsx
 * @description 로그인·온보딩 전용 레이아웃 컴포넌트.
 * layoutType: 'no-header' (component-map.md §1.3)
 *
 * 상단 헤더 없이 전체 화면을 자유롭게 사용한다.
 * 로그인 폼, 온보딩 스텝, 스플래시 화면 등에 사용.
 *
 * @example
 * // 로그인 화면 (수직 중앙 정렬)
 * <BlankPageLayout align="center">
 *   <Stack gap="xl" className="w-full px-xl">
 *     <img src="/logo.svg" alt="하나은행" className="h-10 mx-auto" />
 *     <Input label="아이디" />
 *     <Input label="비밀번호" type="password" />
 *     <Button fullWidth>로그인</Button>
 *   </Stack>
 * </BlankPageLayout>
 *
 * // 온보딩 화면 (상단 정렬 + 스크롤)
 * <BlankPageLayout>
 *   <img src="/onboarding-1.png" alt="" className="w-full" />
 *   <Stack gap="lg" className="px-xl pt-xl">
 *     <Typography variant="heading">간편하게 시작하세요</Typography>
 *     <Typography variant="body-sm" color="muted">...</Typography>
 *   </Stack>
 * </BlankPageLayout>
 */
import React from 'react';
import { cn } from '@lib/cn';
import type { BlankPageLayoutProps } from './types';

export type { BlankPageLayoutProps } from './types';

export function BlankPageLayout({
  align = 'top',
  className,
  children,
  ...props
}: BlankPageLayoutProps) {
  return (
    <div
      className={cn(
        'flex flex-col min-h-dvh px-standard py-md',
        /* 수직 중앙 정렬 — 로그인 폼처럼 단일 블록을 화면 중앙에 놓을 때 사용 */
        align === 'center' && 'items-center justify-center',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}
