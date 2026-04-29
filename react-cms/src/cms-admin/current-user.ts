/**
 * @file current-user.ts
 * @description Vite dev 서버(플러그인) 컨텍스트에서 현재 로그인 사용자를 조회하는 서버 사이드 모듈.
 *
 * 운영 환경에서는 Spider Admin API(`/api/auth/me`)를 호출해 사용자 정보를 파싱하고,
 * 개발 환경에서는 쿠키(`cms_bypass_role`) 값에 따라 역할을 고정 반환한다.
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

/** AUTH_BYPASS 모드 — cms_bypass_role=react-adm 쿠키 보유 시 */
const BYPASS_ADMIN: CurrentUser = {
  userId: "reactAdmin01",
  userName: "React CMS 관리자",
  roleId: "react-adm",
  authorities: ["REACT_CMS:R", "REACT_CMS:W"],
};

/** AUTH_BYPASS 모드 — 기본값(쿠키 미설정 또는 react-user) */
const BYPASS_USER: CurrentUser = {
  userId: "reactUser01",
  userName: "React CMS 제작자",
  roleId: "react-user",
  authorities: ["REACT_CMS:R", "REACT_CMS:W"],
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

// ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

/**
 * Cookie 헤더 문자열에서 특정 쿠키 값을 파싱한다.
 * @param cookieHeader req.headers.cookie 문자열
 * @param name 쿠키 이름
 */
function parseCookie(cookieHeader: string, name: string): string | undefined {
  return cookieHeader
    .split(";")
    .map((c) => c.trim())
    .find((c) => c.startsWith(`${name}=`))
    ?.split("=")
    .slice(1)
    .join("=");
}

/**
 * AUTH_BYPASS 모드에서 쿠키 기반으로 역할을 분기한다.
 * cms_bypass_role=react-adm → BYPASS_ADMIN 기반
 * 그 외                     → BYPASS_USER 기반
 *
 * cms_bypass_user_id 쿠키가 있으면 userId/userName을 해당 값으로 덮어씌운다.
 * 개발 환경에서 실제 계정 ID로 저장·조회를 테스트할 때 사용한다.
 * 예) document.cookie = "cms_bypass_user_id=cmsUser01"
 */
function getBypassUser(cookieHeader: string): CurrentUser {
  const base = parseCookie(cookieHeader, "cms_bypass_role") === "react-adm"
    ? BYPASS_ADMIN
    : BYPASS_USER;

  const overrideUserId = parseCookie(cookieHeader, "cms_bypass_user_id");
  if (!overrideUserId) return base;

  return { ...base, userId: overrideUserId, userName: overrideUserId };
}

// ── 공개 API ─────────────────────────────────────────────────────────────────

/**
 * 현재 로그인 사용자를 조회한다.
 * Vite 플러그인(Node.js 서버) 컨텍스트에서만 호출되어야 한다.
 *
 * @param cookieHeader HTTP 요청의 Cookie 헤더 문자열 (req.headers.cookie ?? '')
 * @returns CurrentUser — 인증 실패 또는 예외 발생 시 GUEST_USER 반환
 */
export async function getCurrentUser(
  cookieHeader: string,
): Promise<CurrentUser> {
  // 개발 우회 모드 — admin 미배포 환경 테스트용
  if (authEnv.AUTH_BYPASS === "true") {
    return getBypassUser(cookieHeader);
  }

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
      // 요청자의 쿠키(JWT 세션)를 Spider Admin에 그대로 전달해 사용자 식별
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
    // 네트워크 오류·JSON 파싱 실패 등 예외 시 GUEST 처리
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
