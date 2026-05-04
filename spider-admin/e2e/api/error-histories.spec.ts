/**
 * 오류발생현황 API 계약 테스트 — /api/error-histories
 *
 * 검증 범위:
 * - 페이지네이션 조회 (성공 / 검색 필터)
 * - 단건 조회 (미존재 → 404)
 * - 엑셀 내보내기
 * - 인증 검증 (비인증 → 401)
 */

import { test, expect } from '@playwright/test';

const BASE_URL = '/api/error-histories';

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('GET /api/error-histories/page — 페이지네이션 조회', () => {

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

    test('존재하지 않는 오류코드로 검색 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, errorCode: 'NON_EXIST_CODE_XYZ_99999' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content.length).toBe(0);
        expect(body.data.totalElements).toBe(0);
    });

    test('대소문자 구분 없이 오류코드 검색이 동작해야 한다', async ({ request }) => {
        // 검색어 존재 여부와 관계없이 200을 반환해야 함
        const res = await request.get(`${BASE_URL}/page`, {
            params: { page: 1, size: 10, errorCode: 'error' },
        });
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });
});

// ─── 단건 조회 ────────────────────────────────────────────

test.describe('GET /api/error-histories/{errorCode}/{errorSerNo} — 단건 조회', () => {

    test('존재하지 않는 이력 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/NON_EXIST_CODE/NON_EXIST_SER`);

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('GET /api/error-histories/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export`);

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
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
        const res = await ctx.get(`${BASE_URL}/page`, { params: { page: 1, size: 10 } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 단건 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/ANY/ANY`);
        expect(res.status()).toBe(401);
    });

    test('인증 없이 엑셀 내보내기 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get(`${BASE_URL}/export`);
        expect(res.status()).toBe(401);
    });
});
