/**
 * 거래중지이력 API 계약 테스트 — /api/trx-stop-histories
 *
 * 검증 범위:
 * - 페이지네이션 조회 (성공 / 검색 필터)
 * - 거래ID별 전체 이력 조회
 * - 인증 검증 (비인증 → 401)
 */

import { test, expect } from '@playwright/test';

const BASE_URL = '/api/trx-stop-histories';

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('GET /api/trx-stop-histories — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, { params: { page: 0, size: 10 } });

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

    test('page=0, size=10 요청 시 첫 페이지 데이터가 조회되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, { params: { page: 0, size: 10 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        // PageResponse.currentPage는 1-based (page + 1) 변환됨
        expect(body.data.currentPage).toBe(1);
        expect(body.data.size).toBe(10);
    });

    test('구분유형 T(거래) 필터 적용 시 거래 유형만 조회되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 0, size: 10, gubunType: 'T' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const items: Array<{ gubunType: string }> = body.data.content;
        items.forEach(item => {
            expect(item.gubunType).toBe('T');
        });
    });

    test('구분유형 S(서비스) 필터 적용 시 서비스 유형 데이터만 조회되어야 한다', async ({ request }) => {
        // E2E 시드 데이터로 S타입 5건 추가됨
        const res = await request.get(BASE_URL, {
            params: { page: 0, size: 10, gubunType: 'S' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const items: Array<{ gubunType: string }> = body.data.content;
        expect(items.length).toBeGreaterThan(0);
        items.forEach(item => {
            expect(item.gubunType).toBe('S');
        });
    });

    test('거래ID 부분 문자열로 검색 시 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ request }) => {
        // E2E 시드 데이터 E2E-TRX-001~010 기준 부분 문자열 검색
        const res = await request.get(BASE_URL, {
            params: { page: 0, size: 50, trxId: 'E2E-TRX' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const items: Array<{ trxId: string }> = body.data.content;
        items.forEach(item => {
            expect(item.trxId.toUpperCase()).toContain('E2E-TRX');
        });
    });

    test('존재하지 않는 거래ID로 검색 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: { page: 0, size: 10, trxId: 'NON_EXIST_TRX_ID_XYZ_99999' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
        expect(body.data.totalElements).toBe(0);
    });

    test('날짜 범위 필터 적용 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, {
            params: {
                page: 0,
                size: 10,
                startDtime: '20200101000000',
                endDtime: '20991231235959',
            },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data.content)).toBe(true);
    });

    test('응답 항목은 gubunType, trxId, trxStopUpdateDtime 필드를 포함해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, { params: { page: 0, size: 10 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const items: Array<Record<string, unknown>> = body.data.content;
        if (items.length > 0) {
            const item = items[0];
            expect(item).toHaveProperty('gubunType');
            expect(item).toHaveProperty('trxId');
            expect(item).toHaveProperty('trxStopUpdateDtime');
            expect(item).toHaveProperty('trxStopReason');
            expect(item).toHaveProperty('lastUpdateUserId');
        }
    });
});

// ─── 거래ID별 전체 이력 조회 ──────────────────────────────

test.describe('GET /api/trx-stop-histories/trx/{trxId} — 거래ID별 이력 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/trx/ANY_TRX_ID`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('존재하지 않는 거래ID 조회 시 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/trx/NON_EXIST_TRX_ID_XYZ_99999`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toEqual([]);
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
        const res = await ctx.get(BASE_URL, { params: { page: 0, size: 10 } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 거래ID별 이력 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/trx/ANY`);
        expect(res.status()).toBe(401);
    });
});
