/**
 * 전문로그파싱 API 계약 테스트 — /api/telegram
 *
 * 검증 범위:
 * - 기관 목록 조회 (드롭다운용)
 * - 전문 검색 (필터)
 * - 전문 파싱 (유효성 검증, 존재하지 않는 전문)
 * - JSON 변환 (유효성 검증, 존재하지 않는 전문)
 * - 비인증 요청
 *
 * 참고: 이 도메인은 READ 전용 (모든 엔드포인트가 MESSAGE_PARSING:R 권한)
 */

import { test, expect } from '@playwright/test';

// ─── GET /api/telegram/orgs — 기관 목록 조회 ────────────────────────

test.describe('GET /api/telegram/orgs — 기관 목록 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/telegram/orgs');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── GET /api/telegram/messages — 전문 검색 ─────────────────────────

test.describe('GET /api/telegram/messages — 전문 검색', () => {

    test('파라미터 없이 조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/telegram/messages');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('searchField와 keyword로 검색 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/telegram/messages', {
            params: { searchField: 'messageId', keyword: 'nonexistent-e2e-test' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('orgId 필터로 검색 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/telegram/messages', {
            params: { orgId: 'nonexistent-org' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── POST /api/telegram/parse — 전문 파싱 ───────────────────────────

test.describe('POST /api/telegram/parse — 전문 파싱', () => {

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/parse', {
            data: {},
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('orgId 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/parse', {
            data: { messageId: 'MSG001', rawString: 'test-raw-data' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('messageId 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/parse', {
            data: { orgId: 'ORG001', rawString: 'test-raw-data' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('rawString 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/parse', {
            data: { orgId: 'ORG001', messageId: 'MSG001' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('존재하지 않는 전문 메타데이터로 파싱 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/parse', {
            data: {
                orgId: 'nonexistent-org-e2e',
                messageId: 'nonexistent-msg-e2e',
                rawString: 'some-raw-data-for-parsing',
            },
        });

        // 전문 메타데이터가 DB에 없으므로 404 (MessageNotFoundException)
        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── POST /api/telegram/to-json — JSON 변환 ────────────────────────

test.describe('POST /api/telegram/to-json — JSON 변환', () => {

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/to-json', {
            data: {},
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('orgId 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/to-json', {
            data: { messageId: 'MSG001', rawString: 'test-raw-data' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('messageId 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/to-json', {
            data: { orgId: 'ORG001', rawString: 'test-raw-data' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('rawString 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/to-json', {
            data: { orgId: 'ORG001', messageId: 'MSG001' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('존재하지 않는 전문 메타데이터로 변환 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/telegram/to-json', {
            data: {
                orgId: 'nonexistent-org-e2e',
                messageId: 'nonexistent-msg-e2e',
                rawString: 'some-raw-data-for-parsing',
            },
        });

        // 전문 메타데이터가 DB에 없으므로 404 (MessageNotFoundException)
        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 비인증 요청 ────────────────────────────────────────────────────

test.describe('비인증 요청 — 접근 제어', () => {

    test('비인증 상태에서 GET /api/telegram/orgs 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/telegram/orgs');
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 GET /api/telegram/messages 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/telegram/messages');
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 POST /api/telegram/parse 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/telegram/parse', {
                data: { orgId: 'ORG001', messageId: 'MSG001', rawString: 'test' },
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 POST /api/telegram/to-json 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/telegram/to-json', {
                data: { orgId: 'ORG001', messageId: 'MSG001', rawString: 'test' },
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
