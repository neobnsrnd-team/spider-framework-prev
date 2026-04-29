/**
 * 관리자 작업이력 로그 API 계약 테스트 — /api/admin-action-logs
 *
 * 검증 범위:
 * - 페이지네이션 조회 (성공 / 검색 필터 / 응답 스키마)
 * - 엑셀 내보내기 (성공 / 헤더 검증)
 * - 인증 검증 (비인증 → 401)
 *
 * FWK_USER_ACCESS_HIS 시드 데이터 (e2e-seed.sql):
 *   e2e-admin / 99000101120000: [GET] /api/users, IP=127.0.0.1, SUCCESS
 *   e2e-admin / 99000101120001: [POST] /api/users, IP=192.168.1.100, SUCCESS
 *   test-user / 99000101120002: [GET] /api/roles, IP=10.0.0.1, NULL
 */

import { test, expect } from '@playwright/test';

const BASE_URL = '/api/admin-action-logs';

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('GET /api/admin-action-logs — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, { params: { page: 1, size: 10 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('content');
        expect(data).toHaveProperty('totalElements');
        expect(data).toHaveProperty('currentPage');
        expect(data).toHaveProperty('totalPages');
        expect(data).toHaveProperty('size');
        expect(data).toHaveProperty('hasNext');
        expect(data).toHaveProperty('hasPrevious');
        expect(Array.isArray(data.content)).toBe(true);
        expect(data.content.length).toBeLessThanOrEqual(10);
    });

    test('content 항목에 FWK_USER_ACCESS_HIS 필드가 모두 포함되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, userId: 'e2e-admin' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBeGreaterThanOrEqual(1);

        const item = body.data.content[0];
        expect(item).toHaveProperty('userId');
        expect(item).toHaveProperty('accessDtime');
        expect(item).toHaveProperty('accessIp');
        expect(item).toHaveProperty('accessUrl');
        expect(item).toHaveProperty('inputData');
        expect(item).toHaveProperty('resultMessage');
    });

    test('userId 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, userId: 'e2e-admin' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThanOrEqual(2);

        for (const item of body.data.content) {
            expect(item.userId).toContain('e2e-admin');
        }
    });

    test('accessIp 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, accessIp: '127.0.0.1' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        for (const item of body.data.content) {
            expect(item.accessIp).toContain('127.0.0.1');
        }
    });

    test('accessUrl 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, accessUrl: '/api/users' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        for (const item of body.data.content) {
            expect(item.accessUrl).toContain('/api/users');
        }
    });

    test('존재하지 않는 사용자ID로 검색 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, userId: 'NON_EXIST_USER_XYZ_99999' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
        expect(body.data.totalElements).toBe(0);
    });

    test('sortBy와 sortDirection 파라미터로 정렬 조회가 되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, sortBy: 'accessDtime', sortDirection: 'DESC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('GET /api/admin-action-logs/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export`);

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });

    test('검색 필터를 포함한 엑셀 내보내기 요청이 성공해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export`, {
            params: { userId: 'e2e-admin' },
        });

        expect(res.status()).toBe(200);
        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');
    });

    test('존재하지 않는 조건으로 내보내기 시 빈 xlsx를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export`, {
            params: { userId: 'NON_EXIST_USER_XYZ_99999' },
        });

        expect(res.status()).toBe(200);
        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');
    });
});

// ─── 인증 검증 ────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {
    let ctx: import('@playwright/test').APIRequestContext;

    test.beforeAll(async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
    });

    test.afterAll(async () => {
        await ctx.dispose();
    });

    test('인증 없이 목록 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(BASE_URL, { params: { page: 1, size: 10 } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 엑셀 내보내기 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/export`);
        expect(res.status()).toBe(401);
    });
});
