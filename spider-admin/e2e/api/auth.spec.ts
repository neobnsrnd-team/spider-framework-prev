/**
 * 인증 및 인가 API 계약 테스트.
 *
 * 검증 범위:
 * - 인증된/비인증 요청에 대한 접근 제어
 * - 메뉴 권한 조회 응답 스키마
 */

import { test, expect } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

test.describe('/api/users/page — 인증 검증', () => {

    test('인증된 요청 시 HTTP 200과 성공 응답을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/page', { params: { page: 1, size: 1 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBeDefined();
        expect(body.data.content).toBeDefined();
    });

    test('인증 없이 요청 시 HTTP 401과 실패 응답을 반환해야 한다', async ({ playwright }) => {
        // config의 baseURL을 참조하여 하드코딩 방지
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/users/page', { params: { page: 1, size: 1 } });

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
            expect(body.code).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});

test.describe('/api/auth/permission/menu — 메뉴 권한 조회', () => {

    test('권한 조회 시 HTTP 200과 boolean 결과를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/auth/permission/menu', {
            params: { userId: ADMIN.userId, menuId: 'USER' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(typeof body.data).toBe('boolean');
        expect(body.data).toBe(true);
    });
});
