/**
 * 이행데이터 조회 API 계약 테스트 — /api/trans/trans-data-inqlist
 *
 * 검증 범위:
 * - 파일 목록 페이지네이션 조회 (서버 파일시스템 기반 — CI에서 0건일 수 있음)
 * - 파일 유형 목록 조회
 * - 엑셀 내보내기
 * - 인증 검증
 *
 * NOTE: 이 API는 서버 로컬 파일시스템에서 파일을 읽으므로
 *       CI 환경에서는 결과가 0건일 수 있다. 빈 결과도 정상으로 처리한다.
 */

import { test, expect } from '@playwright/test';

test.describe('GET /api/trans/trans-data-inqlist/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/page', {
            params: { page: 1, size: 10 },
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

    test('페이지네이션 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/page', {
            params: { page: 1, size: 5 },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
        expect(body.data.currentPage).toBe(1);
        expect(body.data.size).toBe(5);
    });

    test('정렬 파라미터 적용 시 정상 응답해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/page', {
            params: { page: 1, size: 10, sortBy: 'fileName', sortDirection: 'ASC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('fileName 검색 필터 적용 시 정상 응답해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/page', {
            params: { page: 1, size: 10, fileName: 'nonexistent-file-xyz' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        // 존재하지 않는 파일명 검색 → 0건 또는 그 이상 (서버 상태에 따라 다름)
        expect(body.data.content).toHaveLength(0);
    });
});

test.describe('GET /api/trans/trans-data-inqlist/file-types — 파일 유형 목록', () => {

    test('조회 시 HTTP 200과 유형 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/file-types');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);

        // 각 유형은 code와 description을 가져야 함
        if (body.data.length > 0) {
            const first = body.data[0];
            expect(first).toHaveProperty('code');
            expect(first).toHaveProperty('description');
        }
    });
});

test.describe('GET /api/trans/trans-data-inqlist/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/trans-data-inqlist/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('인증 없이 파일 목록 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/trans/trans-data-inqlist/page', { params: { page: 1, size: 10 } });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('인증 없이 파일 유형 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/trans/trans-data-inqlist/file-types');
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
            const res = await ctx.get('/api/trans/trans-data-inqlist/export');
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
