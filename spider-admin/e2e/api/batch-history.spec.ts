/**
 * 배치 수행 내역 API 계약 테스트 — /api/batch/history
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 엑셀 내보내기
 * - 인증 검증
 *
 * 시드 데이터: e2e/docker/e2e-seed.sql (e2e-batch-his, 15건)
 */

import { test, expect } from '@playwright/test';

const SEED_BATCH_APP_ID = 'e2e-batch-his';
const SEED_INSTANCE_ID = 'E2E1';
const SEED_BATCH_DATE = '20260310';

test.describe('GET /api/batch/history/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 5, batchDate: SEED_BATCH_DATE },
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
        expect(Array.isArray(data.content)).toBe(true);
    });

    test('batchAppId 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, batchAppId: SEED_BATCH_APP_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const allMatch = body.data.content.every(
            (item: { batchAppId: string }) => item.batchAppId.includes(SEED_BATCH_APP_ID),
        );
        expect(allMatch).toBe(true);
    });

    test('instanceId 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, instanceId: SEED_INSTANCE_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const allMatch = body.data.content.every(
            (item: { instanceId: string }) => item.instanceId === SEED_INSTANCE_ID,
        );
        expect(allMatch).toBe(true);
    });

    test('resRtCode 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, batchAppId: SEED_BATCH_APP_ID, resRtCode: '-1' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const allMatch = body.data.content.every(
            (item: { resRtCode: string }) => item.resRtCode === '-1',
        );
        expect(allMatch).toBe(true);
    });

    test('batchDate 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, batchAppId: SEED_BATCH_APP_ID, batchDate: SEED_BATCH_DATE },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const allMatch = body.data.content.every(
            (item: { batchDate: string }) => item.batchDate === SEED_BATCH_DATE,
        );
        expect(allMatch).toBe(true);
    });

    test('페이지네이션이 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 5, batchAppId: SEED_BATCH_APP_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
        expect(body.data.currentPage).toBe(1);
        expect(body.data.size).toBe(5);
    });

    test('정렬 파라미터 적용 시 정상 응답해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, batchAppId: SEED_BATCH_APP_ID, sortBy: 'batchDate', sortDirection: 'ASC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBeGreaterThan(0);
    });

    test('검색 결과가 없으면 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/page', {
            params: { page: 1, size: 10, batchAppId: 'no-such-batch-app-id-xyz' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content).toHaveLength(0);
        expect(body.data.totalElements).toBe(0);
    });
});

test.describe('GET /api/batch/history/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/history/export', {
            params: { batchAppId: SEED_BATCH_APP_ID },
        });

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('인증 없이 목록 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/batch/history/page', { params: { page: 1, size: 10 } });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('인증 없이 엑셀 내보내기 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/batch/history/export');
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
