import { cookies } from 'next/headers';

import { fetchJavaAdminApi } from './java-admin-api';

export interface CurrentUser {
    userId: string;
    userName: string;
    roleId: string;
    authorities: string[];
}

interface SpiderAdminCurrentUser {
    userId: string;
    userName: string;
    roleId: string;
    authorities?: string[];
}

const GUEST_USER: CurrentUser = {
    userId: 'guest',
    userName: 'Guest',
    roleId: 'guest',
    authorities: [],
};

export class UnauthorizedError extends Error {
    constructor(message = 'Authentication is required.') {
        super(message);
        this.name = 'UnauthorizedError';
    }
}

/** 인증 우회 모드 — 관리자 (AUTH_BYPASS + /dev/admin 쿠키) */
const BYPASS_ADMIN: CurrentUser = {
    userId: 'admin',
    userName: '관리자',
    roleId: 'cms_admin',
    authorities: ['CMS:W', 'CMS:R'], // 관리자는 쓰기(W)와 읽기(R) 모두 보유
};

/** 인증 우회 모드 — 일반 사용자 (AUTH_BYPASS=true 기본값) */
const BYPASS_USER: CurrentUser = {
    userId: 'cmsUser01',
    userName: 'cms일반유저',
    roleId: 'cms_user',
    authorities: ['CMS:R'],
};

/** AUTH_BYPASS 모드에서 쿠키 기반 역할 분기 */
async function getBypassUser(): Promise<CurrentUser> {
    try {
        const cookieStore = await cookies();
        if (cookieStore.get('cms_bypass_role')?.value === 'cms_admin') {
            return BYPASS_ADMIN;
        }
    } catch {
        // cookies() 사용 불가 환경 — 기본값 유지
    }
    return BYPASS_USER;
}

export async function getCurrentUser(): Promise<CurrentUser> {
    // 인증 우회 모드 — admin 미배포 환경 테스트용
    if (process.env.AUTH_BYPASS === 'true') {
        return getBypassUser();
    }

    try {
        const user = await fetchJavaAdminApi<SpiderAdminCurrentUser>('/api/auth/me');
        return {
            userId: user.userId,
            userName: user.userName,
            roleId: user.roleId,
            authorities: user.authorities ?? [],
        };
    } catch (err: unknown) {
        // Java가 401/403을 반환한 경우 — 세션 만료 또는 인증 오류
        // 호출자가 세션 만료와 일반 오류를 구분할 수 있도록 UnauthorizedError를 throw
        const status = (err as Error & { status?: number }).status;
        if (status === 401 || status === 403) {
            throw new UnauthorizedError('세션이 만료되었습니다. 다시 로그인해 주세요.');
        }
        // 네트워크 오류·서버 장애 등 그 외 에러는 fail-open: GUEST_USER 반환
        return GUEST_USER;
    }
}

export function hasAuthority(user: Pick<CurrentUser, 'authorities'>, authority: 'CMS:R' | 'CMS:W'): boolean {
    return user.authorities.includes(authority);
}

export function canReadCms(user: Pick<CurrentUser, 'authorities'>): boolean {
    return hasAuthority(user, 'CMS:R') || hasAuthority(user, 'CMS:W');
}

export function canWriteCms(user: Pick<CurrentUser, 'authorities'>): boolean {
    return hasAuthority(user, 'CMS:W');
}

export function canAccessCmsDashboard(user: Pick<CurrentUser, 'authorities'>): boolean {
    return hasAuthority(user, 'CMS:R');
}

export function canManageAllCmsPages(user: Pick<CurrentUser, 'authorities'>): boolean {
    return hasAuthority(user, 'CMS:W');
}

export function canAccessCmsEdit(user: Pick<CurrentUser, 'authorities'>): boolean {
    return hasAuthority(user, 'CMS:R') || hasAuthority(user, 'CMS:W');
}

export function canManageCmsPage(
    user: Pick<CurrentUser, 'authorities' | 'userId'>,
    ownerUserId?: string | null,
): boolean {
    if (canManageAllCmsPages(user)) {
        return true;
    }
    if (!hasAuthority(user, 'CMS:R')) {
        return false;
    }
    return !ownerUserId || ownerUserId === user.userId;
}

export const CMS_ROLE = {
    SPIDER_ADMIN: 'ADMIN',
    CMS_ADMIN: 'cms_admin',
    USER: 'cms_user',
} as const;

export function canAdminScreen(user: Pick<CurrentUser, 'roleId'>): boolean {
    return user.roleId === CMS_ROLE.SPIDER_ADMIN || user.roleId === CMS_ROLE.CMS_ADMIN;
}

export function getDefaultCmsPath(user: Pick<CurrentUser, 'roleId'>): '/approve' | '/dashboard' {
    return canAdminScreen(user) ? '/approve' : '/dashboard';
}

export async function requireCmsWrite(): Promise<CurrentUser> {
    const user = await getCurrentUser();
    if (!canWriteCms(user)) {
        throw new UnauthorizedError('Permission denied.');
    }
    return user;
}
