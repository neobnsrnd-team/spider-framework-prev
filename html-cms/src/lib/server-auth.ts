// src/lib/server-auth.ts
// 서버 간 호출 인증 유틸 — x-deploy-token 헤더 검증

import { timingSafeEqual } from 'crypto';

import { DEPLOY_SECRET } from '@/lib/env';

/**
 * x-deploy-token 헤더 값이 서버에 설정된 DEPLOY_SECRET과 일치하는지 검증한다.
 * Admin 백엔드 등 서버 간 호출 시 세션 없이 인증하는 용도로 사용한다.
 * 타이밍 공격 방지를 위해 timingSafeEqual로 비교한다.
 */
export function isValidDeployToken(token: string | null): boolean {
    if (!DEPLOY_SECRET || !token) return false;
    try {
        const expected = Buffer.from(DEPLOY_SECRET, 'utf8');
        const received = Buffer.from(token, 'utf8');
        if (expected.length !== received.length) return false;
        return timingSafeEqual(expected, received);
    } catch {
        return false;
    }
}
