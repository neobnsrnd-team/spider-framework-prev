/**
 * 이행데이터 반영 API 계약 테스트 — /api/trans
 *
 * 검증 범위:
 * - 이행 실행 이력 페이지네이션 조회 (목록, 검색 필터, 정렬)
 * - 이행 상세 조회
 * - 이행 상세 이력 페이지네이션
 * - 엑셀 내보내기
 * - 인증 검증
 *
 * 시드 데이터: e2e/docker/e2e-seed.sql (FWK_TRANS_DATA_TIMES 12건, FWK_TRANS_DATA_HIS 15건)
 */

import { test, expect } from '@playwright/test';

const SEED_USER_ID = 'e2e-admin';
const SEED_TRAN_SEQ = '99000000000001';

test.describe('GET /api/trans/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 5 },
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

    test('userId 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 10, userId: SEED_USER_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const allMatch = body.data.content.every(
            (item: { userId: string }) => item.userId.includes(SEED_USER_ID),
        );
        expect(allMatch).toBe(true);
    });

    test('tranResult 검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 10, userId: SEED_USER_ID, tranResult: 'F' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);

        const allMatch = body.data.content.every(
            (item: { tranResult: string }) => item.tranResult === 'F',
        );
        expect(allMatch).toBe(true);
    });

    test('페이지네이션이 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 5, userId: SEED_USER_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
        expect(body.data.currentPage).toBe(1);
        expect(body.data.size).toBe(5);
    });

    test('정렬 파라미터 적용 시 정상 응답해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 10, userId: SEED_USER_ID, sortBy: 'tranTime', sortDirection: 'ASC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBeGreaterThan(0);
    });

    test('검색 결과가 없으면 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/page', {
            params: { page: 1, size: 10, userId: 'no-such-user-xyz' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content).toHaveLength(0);
        expect(body.data.totalElements).toBe(0);
    });
});

test.describe('GET /api/trans/{tranSeq}/details — 이행 상세 조회', () => {

    test('존재하는 이행순번 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/trans/${SEED_TRAN_SEQ}/details`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('tranSeq');
        expect(body.data).toHaveProperty('tranResult');
    });

    test('존재하지 않는 이행순번 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/99999999999999/details');

        expect(res.status()).toBe(404);
    });
});

test.describe('GET /api/trans/{tranSeq}/details/page — 이행 상세 이력 페이지네이션', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/trans/${SEED_TRAN_SEQ}/details/page`, {
            params: { page: 1, size: 10 },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('content');
        expect(data).toHaveProperty('totalElements');
        expect(Array.isArray(data.content)).toBe(true);
        expect(data.totalElements).toBeGreaterThan(0);
    });

    test('tranResult 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/trans/${SEED_TRAN_SEQ}/details/page`, {
            params: { page: 1, size: 10, tranResult: 'S' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const allMatch = body.data.content.every(
            (item: { tranResult: string }) => item.tranResult === 'S',
        );
        expect(allMatch).toBe(true);
    });

    test('tranType 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/trans/${SEED_TRAN_SEQ}/details/page`, {
            params: { page: 1, size: 10, tranType: 'T' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const allMatch = body.data.content.every(
            (item: { tranType: string }) => item.tranType === 'T',
        );
        expect(allMatch).toBe(true);
    });
});

test.describe('GET /api/trans/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/export', {
            params: { userId: SEED_USER_ID },
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
            const res = await ctx.get('/api/trans/page', { params: { page: 1, size: 10 } });
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
            const res = await ctx.get('/api/trans/export');
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('인증 없이 상세 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get(`/api/trans/${SEED_TRAN_SEQ}/details`);
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
