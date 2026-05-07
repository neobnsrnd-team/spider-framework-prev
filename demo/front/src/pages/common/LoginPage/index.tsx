/**
 * @file index.tsx
 * @description 로그인 페이지 컴포넌트.
 *
 * Figma 원본: Hana Bank App — node-id: 1-911
 *
 * 실제 앱 구현 시 주의사항:
 * - 입력 상태(id, password)와 핸들러는 useLoginForm 훅으로 분리한다.
 * - 로그인 API 호출은 loginRepository 를 통해 처리한다.
 * - Page에서 직접 useState 사용은 금지 (CLAUDE.md 아키텍처 원칙).
 *
 * @param hasError - true 시 비밀번호 에러 상태(빨간 테두리 + 안내 문구) 표시
 * @param onLogin  - 로그인 버튼 클릭 핸들러
 */
import React from "react";
import { Eye, EyeOff, KeyRound, Fingerprint, QrCode } from "lucide-react";

import { BlankPageLayout } from "@cl/layout/BlankPageLayout";
import { AppBrandHeader } from "@cl/layout/AppBrandHeader";
import { Stack } from "@cl/layout/Stack";
import { Inline } from "@cl/layout/Inline";
import { Typography } from "@cl/core/Typography";
import { Input } from "@cl/core/Input";
import { Button } from "@cl/core/Button";
import { Checkbox } from "@cl/modules/common/Checkbox";
import { DividerWithLabel } from "@cl/modules/common/DividerWithLabel";
import { QuickMenuGrid } from "@cl/biz/common/QuickMenuGrid";
import type { LoginPageProps } from "./types";

export type { LoginPageProps } from "./types";

export function LoginPage({
  userId = "",
  password = "",
  onUserIdChange,
  onPasswordChange,
  hasError = false,
  showPassword = false,
  onTogglePassword,
  onLogin,
  saveId = false,
  onSaveIdChange,
}: LoginPageProps) {
  const ALT_LOGIN_ITEMS = [
    {
      id: "pin",
      icon: <KeyRound size={20} />,
      label: "간편 비밀번호",
      onClick: () => {},
    },
    {
      id: "bio",
      icon: <Fingerprint size={20} />,
      label: "생체인증",
      onClick: () => {},
    },
    {
      id: "qr",
      icon: <QrCode size={20} />,
      label: "QR 로그인",
      onClick: () => {},
    },
  ];

  return (
    <BlankPageLayout>
      <AppBrandHeader brandInitial="H" brandName="하나카드" />

      <Stack gap="md" className="pt-xl pb-md">
        <Stack gap="xs" className="pb-md">
          <Typography
            as="h1"
            variant="heading"
            color="heading"
            className="text-3xl"
          >
            로그인
          </Typography>
          <Typography variant="body" color="muted">
            하나카드에 오신 것을 환영합니다
          </Typography>
        </Stack>

        <Stack gap="lg">
          <Input
            label="아이디"
            type="text"
            placeholder="아이디를 입력하세요"
            value={userId}
            onChange={(e) => onUserIdChange?.(e.target.value)}
            fullWidth
          />
          <Input
            label="비밀번호"
            type={showPassword ? "text" : "password"}
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => onPasswordChange?.(e.target.value)}
            // Enter 키 입력 시 로그인 버튼 클릭과 동일하게 처리
            onKeyDown={(e) => e.key === "Enter" && onLogin?.()}
            fullWidth
            validationState={hasError ? "error" : "default"}
            helperText={
              hasError ? "아이디 또는 비밀번호가 틀렸습니다" : undefined
            }
            // iconOnly 모드에서는 children이 숨겨지므로 leftIcon으로 아이콘을 전달한다
            rightElement={
              <Button
                variant="ghost"
                size="sm"
                iconOnly
                aria-label={showPassword ? "비밀번호 숨김" : "비밀번호 표시"}
                onClick={onTogglePassword}
                className="text-text-muted"
                leftIcon={
                  showPassword ? <Eye size={20} /> : <EyeOff size={20} />
                }
              />
            }
          />
        </Stack>

        {/* 아이디 저장 */}
        <Inline gap="lg" className="pt-xs">
          <Checkbox
            label="아이디 저장"
            checked={saveId}
            onChange={onSaveIdChange ?? (() => {})}
          />
        </Inline>

        <Inline justify="center" gap="sm" className="py-sm">
          <Button variant="ghost" size="sm" onClick={() => {}}>
            아이디 찾기
          </Button>
          <div
            className="w-px h-3 bg-border-subtle self-center"
            aria-hidden="true"
          />
          <Button variant="ghost" size="sm" onClick={() => {}}>
            비밀번호 변경
          </Button>
          <div
            className="w-px h-3 bg-border-subtle self-center"
            aria-hidden="true"
          />
          <Button variant="ghost" size="sm" onClick={() => {}}>
            회원가입
          </Button>
        </Inline>

        <div className="pt-md pb-lg">
          <Button variant="primary" size="lg" fullWidth onClick={onLogin}>
            로그인
          </Button>
        </div>
      </Stack>

      <Stack gap="xl" className="pb-2xl">
        <DividerWithLabel label="다른 로그인 방식" />
        <QuickMenuGrid cols={3} items={ALT_LOGIN_ITEMS} />
      </Stack>
    </BlankPageLayout>
  );
}
