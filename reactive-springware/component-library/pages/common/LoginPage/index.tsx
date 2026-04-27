/**
 * @file index.tsx
 * @description 로그인 페이지 컴포넌트.
 *
 * Figma 원본: Hana Bank App — node-id: 1-911
 *
 * 실제 앱 구현 시 주의사항:
 * - 입력 상태(id, password)와 핸들러는 useLoginForm 훅으로 분리한다.
 * - 로그인 API 호출은 loginRepository 를 통해 처리한다.
 * - Page에서 직접 useState 사용은 금지 (page-generation-rules.md 아키텍처 원칙).
 *
 * @param hasError - true 시 비밀번호 에러 상태(빨간 테두리 + 안내 문구) 표시
 */
import React from 'react';
import { EyeOff, KeyRound, Fingerprint, QrCode } from 'lucide-react';

import { BlankPageLayout }  from '../../../layout/BlankPageLayout';
import { AppBrandHeader }   from '../../../layout/AppBrandHeader';
import { Stack }            from '../../../layout/Stack';
import { Inline }           from '../../../layout/Inline';
import { Typography }             from '../../../core/Typography';
import { Input }            from '../../../core/Input';
import { Button }           from '../../../core/Button';
import { DividerWithLabel } from '../../../modules/common/DividerWithLabel';
import { QuickMenuGrid }    from '../../../biz/common/QuickMenuGrid';
import type { LoginPageProps } from './types';

export type { LoginPageProps } from './types';

// ── 대체 로그인 방식 항목 ──────────────────────────────────────────
// QuickMenuGrid 전달용. 로그인 메서드별 아이콘·레이블 정의.
const ALT_LOGIN_ITEMS = [
  {
    id:      'pin',
    icon:    <KeyRound size={20} />,
    label:   '간편 비밀번호',
    onClick: () => console.log('간편 비밀번호 로그인'),
  },
  {
    id:      'bio',
    icon:    <Fingerprint size={20} />,
    label:   '생체인증',
    onClick: () => console.log('생체인증 로그인'),
  },
  {
    id:      'qr',
    icon:    <QrCode size={20} />,
    label:   'QR 로그인',
    onClick: () => console.log('QR 로그인'),
  },
];

export function LoginPage({ hasError = false }: LoginPageProps) {
  return (
    <BlankPageLayout>
      {/* ── 브랜드 로고 헤더 ─────────────────────────────── */}
      <AppBrandHeader brandInitial="H" brandName="하나은행" />

      {/* ── 메인 콘텐츠 영역 ─────────────────────────────── */}
      <Stack gap="md" className="flex-1 px-standard pt-xl pb-md">

        {/* 타이틀 섹션 */}
        <Stack gap="xs" className="pb-md">
          {/* text-3xl(30px) 로 Figma 원본 크기 재현.
              Text의 heading variant(text-2xl=24px)보다 커서 className으로 override. */}
          <Typography as="h1" variant="heading" color="heading" className="text-3xl">
            로그인
          </Typography>
          <Typography variant="body" color="muted">
            하나원큐에 오신 것을 환영합니다
          </Typography>
        </Stack>

        {/* 입력 폼 */}
        <Stack gap="lg">
          {/* 아이디 입력 — 기본 상태 */}
          <Input
            label="아이디"
            type="text"
            placeholder="아이디를 입력하세요"
            defaultValue="hanabank_user"
            fullWidth
          />

          {/* 비밀번호 입력 — 에러 상태 시 빨간 테두리 + 안내 문구 표시 */}
          <Input
            label="비밀번호"
            type="password"
            placeholder="비밀번호를 입력하세요"
            defaultValue="••••••••"
            fullWidth
            validationState={hasError ? 'error' : 'default'}
            helperText={hasError ? '아이디 또는 비밀번호가 틀렸습니다' : undefined}
            /* EyeOff 아이콘: 비밀번호 표시/숨기기 토글 트리거.
               실제 앱에서는 onClick으로 type을 'text'/'password' 전환한다. */
            rightElement={<EyeOff size={20} className="text-text-muted" aria-label="비밀번호 숨김" />}
          />
        </Stack>

        {/* 텍스트 링크 그룹: 아이디 찾기 | 비밀번호 변경 | 회원가입
            Button variant="ghost"로 언더라인 없는 텍스트 버튼 구현 */}
        <Inline justify="center" gap="sm" className="py-sm">
          <Button variant="ghost" size="sm" onClick={() => console.log('아이디 찾기')}>
            아이디 찾기
          </Button>

          {/* 수직 구분선(|) — 토큰 색상 bg-border-subtle 사용 */}
          <div className="w-px h-3 bg-border-subtle self-center" aria-hidden="true" />

          <Button variant="ghost" size="sm" onClick={() => console.log('비밀번호 변경')}>
            비밀번호 변경
          </Button>

          <div className="w-px h-3 bg-border-subtle self-center" aria-hidden="true" />

          <Button variant="ghost" size="sm" onClick={() => console.log('회원가입')}>
            회원가입
          </Button>
        </Inline>

        {/* 로그인 버튼 — 가중치를 높이기 위해 나머지 공간 아래 밀어넣기 */}
        <div className="mt-auto pt-xl">
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => console.log('로그인')}
          >
            로그인
          </Button>
        </div>
      </Stack>

      {/* ── 대체 로그인 방식 영역 ─────────────────────────── */}
      <Stack gap="xl" className="px-standard pb-2xl">
        <DividerWithLabel label="다른 로그인 방식" />

        {/* 간편 비밀번호 / 생체인증 / QR 로그인 — 3열 그리드 */}
        <QuickMenuGrid cols={3} items={ALT_LOGIN_ITEMS} />
      </Stack>
    </BlankPageLayout>
  );
}