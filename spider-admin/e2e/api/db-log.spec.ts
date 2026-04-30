/**
 * 거래추적로그조회(DB) API 계약 테스트 — /api/message-instances
 *
 * 검증 범위:
 * - 페이지네이션 조회 (성공 / 검색 필터 / 응답 스키마)
 * - 거래추적번호 상세 조회
 * - 인증 검증 (비인증 → 401)
 *
 * FWK_MESSAGE_INSTANCE 시드 데이터 (e2e-seed.sql):
 *   E2E-TRX-TRACKING-001: e2e-admin / E2EORG01 / 응답 (Q+S 2건)
 *   E2E-TRX-TRACKING-002: e2e-admin / E2EORG02 / 오류 (Q 1건)
 */

import { test, expect } from '@playwright/test';

const BASE_URL = '/api/message-instances';

// ─── 페이지네이션 조회 ─────────────────────────────────────

test.describe('GET /api/message-instances/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, { params: { page: 1, size: 10 } });

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

    test('content 항목에 FWK_MESSAGE_INSTANCE 필드가 모두 포함되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, userId: 'e2e-admin' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThanOrEqual(1);

        const item = body.data.content[0];
        expect(item).toHaveProperty('messageSno');
        expect(item).toHaveProperty('trxId');
        expect(item).toHaveProperty('orgId');
        expect(item).toHaveProperty('reqResType');
        expect(item).toHaveProperty('messageId');
        expect(item).toHaveProperty('trxTrackingNo');
        expect(item).toHaveProperty('userId');
        expect(item).toHaveProperty('trxDtime');
        expect(item).toHaveProperty('lastRtCode');
    });

    test('userId 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, userId: 'e2e-admin' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.totalElements).toBeGreaterThanOrEqual(2);

        for (const item of body.data.content) {
            expect(item.userId).toContain('e2e-admin');
        }
    });

    test('orgId 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, orgId: 'E2EORG01' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.totalElements).toBeGreaterThanOrEqual(1);

        for (const item of body.data.content) {
            expect(item.orgId).toContain('E2EORG01');
        }
    });

    test('trxTrackingNo 필터가 적용되어 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, trxTrackingNo: 'E2E-TRX-TRACKING-001' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.totalElements).toBeGreaterThanOrEqual(1);

        for (const item of body.data.content) {
            expect(item.trxTrackingNo).toContain('E2E-TRX-TRACKING-001');
        }
    });

    test('거래일자 범위 필터가 적용되어 범위 내 결과만 반환해야 한다', async ({ request }) => {
        const today = new Date();
        const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');

        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, trxDateFrom: dateStr, trxDateTo: dateStr },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThanOrEqual(1);
    });

    test('존재하지 않는 조건으로 검색 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, trxTrackingNo: 'NON_EXIST_TRX_XYZ_99999' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
        expect(body.data.totalElements).toBe(0);
    });

    test('size 파라미터가 응답 건수에 반영되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 1 },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(1);
        expect(body.data.size).toBe(1);
    });
});

// ─── 거래추적번호 상세 조회 ────────────────────────────────

test.describe('GET /api/message-instances/tracking/{trxTrackingNo} — 상세 조회', () => {

    test('존재하는 거래추적번호 조회 시 HTTP 200과 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/tracking/E2E-TRX-TRACKING-001`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
        expect(body.data.length).toBe(2); // Q + S 2건 시드
    });

    test('상세 응답에 CLOB 필드(messageData)가 포함되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/tracking/E2E-TRX-TRACKING-001`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        const item = body.data[0];
        expect(item).toHaveProperty('messageData');
        expect(item.messageData).not.toBeNull();
    });

    test('상세 결과는 logDtime 오름차순으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/tracking/E2E-TRX-TRACKING-001`);

        expect(res.status()).toBe(200);
        const items = (await res.json()).data;

        if (items.length > 1) {
            for (let i = 0; i < items.length - 1; i++) {
                expect(items[i].logDtime <= items[i + 1].logDtime).toBe(true);
            }
        }
    });

    test('존재하지 않는 거래추적번호 조회 시 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/tracking/NON_EXIST_TRX_99999`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBe(0);
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

    test('인증 없이 페이지네이션 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/page`, { params: { page: 1, size: 10 } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 상세 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/tracking/ANY_TRX_NO`);
        expect(res.status()).toBe(401);
    });
});
