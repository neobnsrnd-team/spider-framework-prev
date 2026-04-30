/**
 * @file current-user.ts
 * @description Vite dev 서버(플러그인) 컨텍스트에서 현재 로그인 사용자를 조회하는 서버 사이드 모듈.
 *
 * Spider Admin API(`/api/auth/me`)를 호출해 사용자 정보를 파싱한다.
 * 어드민 모드(npm run dev:proxy)에서만 동작하며, 단독 실행 모드(npm run dev)에서는
 * 파일 시스템 저장을 사용하므로 이 모듈이 호출되지 않는다.
 *
 * 이 모듈은 Node.js(Vite 플러그인) 에서만 실행되므로 브라우저 API(`document.cookie` 등)를
 * 사용하지 않으며, HTTP 요청 헤더의 Cookie 문자열을 직접 파싱한다.
 *
 * @example
 * // cmsBankPlugin.ts 내에서
 * const cookieHeader = req.headers.cookie ?? '';
 * const user = await getCurrentUser(cookieHeader);
 * if (!canWriteCms(user)) { jsonResponse(res, 403, { error: 'Forbidden' }); return; }
 */

import { authEnv } from "../lib/env";

// ── 타입 정의 ────────────────────────────────────────────────────────────────

export interface CurrentUser {
  userId: string;
  userName: string;
  roleId: string;
  /** Spider Admin이 부여한 권한 목록 (예: ['REACT_CMS:R', 'REACT_CMS:W']) */
  authorities: string[];
}

interface SpiderAdminCurrentUser {
  userId: string;
  userName: string;
  roleId: string;
  authorities?: string[];
}

interface SpiderAdminApiResponse<T> {
  success?: boolean;
  data?: T;
  message?: string;
}

// ── 상수 ─────────────────────────────────────────────────────────────────────

/** 인증 실패·미인증 상태의 기본 사용자 */
const GUEST_USER: CurrentUser = {
  userId: "guest",
  userName: "Guest",
  roleId: "guest",
  authorities: [],
};

export const CMS_ROLE = {
  SPIDER_ADMIN: "ADMIN",
  CMS_ADMIN: "react-adm",
  USER: "react-user",
} as const;

// ── 오류 ─────────────────────────────────────────────────────────────────────

export class UnauthorizedError extends Error {
  constructor(message = "Permission denied.") {
    super(message);
    this.name = "UnauthorizedError";
  }
}

// ── 공개 API ─────────────────────────────────────────────────────────────────

/**
 * 현재 로그인 사용자를 조회한다.
 * Vite 플러그인(Node.js 서버) 컨텍스트에서만 호출되어야 한다.
 *
 * Spider Admin API(`/api/auth/me`)에 요청자의 쿠키를 전달해 사용자를 식별한다.
 * SPIDER_ADMIN_API_URL 미설정 또는 인증 실패 시 GUEST_USER를 반환한다.
 *
 * @param cookieHeader HTTP 요청의 Cookie 헤더 문자열 (req.headers.cookie ?? '')
 * @returns CurrentUser — 인증 실패 또는 예외 발생 시 GUEST_USER 반환
 */
export async function getCurrentUser(
  cookieHeader: string,
): Promise<CurrentUser> {
  if (!authEnv.SPIDER_ADMIN_API_URL) {
    // 환경변수 미설정 시 경고 후 GUEST 처리 (운영 환경에서는 반드시 설정해야 함)
    console.warn(
      "[react-cms] SPIDER_ADMIN_API_URL이 설정되지 않았습니다. GUEST로 처리합니다.",
    );
    return GUEST_USER;
  }

  // 말미 슬래시 제거로 URL 이중 슬래시 방지
  const baseUrl = authEnv.SPIDER_ADMIN_API_URL.replace(/\/$/, "");

  // 5초 타임아웃 — Spider Admin 응답 지연 시 Vite 플러그인 전체가 블로킹되는 것을 방지
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 5_000);

  try {
    const response = await fetch(`${baseUrl}/api/auth/me`, {
      // 요청자의 쿠키(세션)를 Spider Admin에 그대로 전달해 사용자 식별
      headers: { cookie: cookieHeader },
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (!response.ok) {
      return GUEST_USER;
    }

    const body =
      (await response.json()) as SpiderAdminApiResponse<SpiderAdminCurrentUser>;
    if (!body.success || !body.data) {
      return GUEST_USER;
    }

    return {
      userId: body.data.userId,
      userName: body.data.userName,
      roleId: body.data.roleId,
      authorities: body.data.authorities ?? [],
    };
  } catch {
    // 네트워크 오류·JSON 파싱 실패·타임아웃 등 예외 시 GUEST 처리
    return GUEST_USER;
  }
}

/** 'CMS:R' 또는 'CMS:W' 권한 보유 여부 확인 */
export function hasAuthority(
  user: Pick<CurrentUser, "authorities">,
  authority: "REACT_CMS:R" | "REACT_CMS:W",
): boolean {
  return user.authorities.includes(authority);
}

/** React CMS 에디터/대시보드 읽기 접근 가능 여부 */
export function canReadCms(user: Pick<CurrentUser, "authorities">): boolean {
  // REACT_CMS:W를 가지면 읽기도 허용 (상위 권한)
  return hasAuthority(user, "REACT_CMS:R") || hasAuthority(user, "REACT_CMS:W");
}

/** React CMS 페이지 저장·삭제 가능 여부 */
export function canWriteCms(user: Pick<CurrentUser, "authorities">): boolean {
  return hasAuthority(user, "REACT_CMS:W");
}

/** 승인 관리 화면 접근 가능 여부 (cms_admin 또는 ADMIN 역할) */
export function canAdminScreen(user: Pick<CurrentUser, "roleId">): boolean {
  return (
    user.roleId === CMS_ROLE.SPIDER_ADMIN || user.roleId === CMS_ROLE.CMS_ADMIN
  );
}

/**
 * CMS:W 권한을 검증한다. 권한 없으면 UnauthorizedError를 throw한다.
 * API 엔드포인트 미들웨어에서 저장·삭제 요청 전에 호출한다.
 *
 * @param cookieHeader HTTP 요청의 Cookie 헤더 문자열
 * @returns 권한이 있는 CurrentUser
 * @throws UnauthorizedError
 */
export async function requireCmsWrite(
  cookieHeader: string,
): Promise<CurrentUser> {
  const user = await getCurrentUser(cookieHeader);
  if (!canWriteCms(user)) {
    throw new UnauthorizedError("Permission denied.");
  }
  return user;
}

/**
 * CMS:R 권한을 검증한다. 권한 없으면 UnauthorizedError를 throw한다.
 * 조회 전용 API 엔드포인트에서 호출한다.
 *
 * @param cookieHeader HTTP 요청의 Cookie 헤더 문자열
 * @returns 권한이 있는 CurrentUser
 * @throws UnauthorizedError
 */
export async function requireCmsRead(
  cookieHeader: string,
): Promise<CurrentUser> {
  const user = await getCurrentUser(cookieHeader);
  if (!canReadCms(user)) {
    throw new UnauthorizedError("Permission denied.");
  }
  return user;
}
