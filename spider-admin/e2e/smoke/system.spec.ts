/**
 * Tier 0: 스모크 테스트 — 인프라에 문제가 있으면 즉시 실패한다.
 *
 * 서버 기동, 인증 세션, SPA 로드만 검증한다.
 * API 응답 스키마는 api/ 테스트에서 검증하므로 여기서는 다루지 않는다.
 */

import { test, expect } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

test.describe('스모크', () => {

    test('로그인 페이지가 정상 응답해야 한다', async ({ request }) => {
        const res = await request.get('/login');
        expect(res.status()).toBe(200);
    });

    test('인증된 세션으로 홈에 접근할 수 있어야 한다', async ({ page }) => {
        await page.goto('/home');
        await expect(page).toHaveURL(/\/home/);
    });

    test('URL 직접 접근 시 SPA 페이지가 로드되어야 한다', async ({ page }) => {
        await page.goto('/users');
        await expect(page.getByRole('table')).toBeVisible({ timeout: 10_000 });
    });

    test('시드 계정을 API로 조회할 수 있어야 한다', async ({ request }) => {
        const res = await request.get(`/api/users/${ADMIN.userId}`);
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.userId).toBe(ADMIN.userId);
    });
});
