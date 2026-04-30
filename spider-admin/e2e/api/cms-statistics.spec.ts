/**
 * CMS 통계 API 계약 테스트 — /api/cms-admin/statistics
 *
 * 검증 범위:
 * - 목록 조회 (성공 / 날짜 필터 / 페이지네이션 스키마)
 * - 컴포넌트 클릭 상세 조회 (성공 / 빈 결과)
 * - 인증 검증 (비인증 → 401)
 */

import { test, expect } from '@playwright/test';

const BASE_URL = '/api/cms-admin/statistics';
const START_DATE = '2000-01-01';
const END_DATE = '2099-12-31';

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('GET /api/cms-admin/statistics — 통계 목록 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, startDate: START_DATE, endDate: END_DATE },
        });

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
    });

    test('조회된 항목에 pageId, pageName, viewCount, clickCount 필드가 있어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, startDate: START_DATE, endDate: END_DATE },
        });

        const body = await res.json();
        expect(body.success).toBe(true);

        if (body.data.content.length > 0) {
            const item = body.data.content[0];
            expect(item).toHaveProperty('pageId');
            expect(item).toHaveProperty('pageName');
            expect(item).toHaveProperty('viewCount');
            expect(item).toHaveProperty('clickCount');
        }
    });

    test('존재하지 않는 페이지 ID로 필터 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, startDate: START_DATE, endDate: END_DATE, pageId: 'NON_EXIST_PAGE_XYZ' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
        expect(body.data.totalElements).toBe(0);
    });

    test('미래 날짜 범위로 조회 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 1, size: 10, startDate: '2099-01-01', endDate: '2099-12-31' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
    });
});

// ─── 컴포넌트 클릭 상세 조회 ─────────────────────────────

test.describe('GET /api/cms-admin/statistics/detail — 컴포넌트 클릭 상세 조회', () => {

    test('존재하지 않는 페이지 ID로 조회 시 200과 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/detail`, {
            params: { pageId: 'NON_EXIST_PAGE_XYZ', startDate: START_DATE, endDate: END_DATE },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
        expect(body.data.length).toBe(0);
    });

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/detail`, {
            params: { pageId: 'E2E-PAGE-001', startDate: START_DATE, endDate: END_DATE },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);

        if (body.data.length > 0) {
            expect(body.data[0]).toHaveProperty('componentId');
            expect(body.data[0]).toHaveProperty('clickCount');
        }
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

    test('인증 없이 통계 목록 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(BASE_URL, {
            params: { page: 1, size: 10, startDate: START_DATE, endDate: END_DATE },
        });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 상세 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/detail`, {
            params: { pageId: 'ANY', startDate: START_DATE, endDate: END_DATE },
        });
        expect(res.status()).toBe(401);
    });
});
