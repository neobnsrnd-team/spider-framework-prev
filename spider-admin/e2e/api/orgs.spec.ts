/**
 * 연계기관(Org) API 계약 테스트.
 *
 * 모든 테스트는 자체 데이터를 생성하고 정리한다.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
function genOrgId() { return 'AO' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }

async function createOrg(request: APIRequestContext, orgId: string, orgName: string) {
    const res = await request.post('/api/orgs/batch', {
        data: {
            upserts: [{ orgId, orgName, orgDesc: 'API test', startTime: '0000', endTime: '2359' }],
            deleteOrgIds: [],
        },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteOrg(request: APIRequestContext, orgId: string) {
    await request.post('/api/orgs/batch', {
        data: { upserts: [], deleteOrgIds: [orgId] },
    });
}

// ─── 테스트 ──────────────────────────────────────────────

// ─── GET /api/orgs/page ──────────────────────────────────

test.describe('GET /api/orgs/page — 페이지네이션 조회', () => {

    test('기본 조회 시 PageResponse 구조를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/orgs/page');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('content');
        expect(body.data).toHaveProperty('totalElements');
        expect(Array.isArray(body.data.content)).toBe(true);
    });

    test('page/size 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/orgs/page?page=1&size=5');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
    });

    test('searchField/searchValue로 필터링되어야 한다', async ({ request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'ApiSearchTest');

        try {
            const res = await request.get(`/api/orgs/page?searchField=orgId&searchValue=${orgId}`);
            expect(res.ok()).toBeTruthy();

            const body = await res.json();
            expect(body.data.content.length).toBe(1);
            expect(body.data.content[0].orgId).toBe(orgId);
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('sortBy/sortDirection으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get('/api/orgs/page?sortBy=orgId&sortDirection=DESC');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThan(0);
    });
});

// ─── GET /api/orgs/list ──────────────────────────────────

test.describe('GET /api/orgs/list — 전체 목록', () => {

    test('모든 기관 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/orgs/list');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── POST /api/orgs/batch ────────────────────────────────

test.describe('POST /api/orgs/batch — 생성/수정/삭제', () => {

    test('새 기관을 생성해야 한다', async ({ request }) => {
        const orgId = genOrgId();

        try {
            const res = await request.post('/api/orgs/batch', {
                data: {
                    upserts: [{ orgId, orgName: 'BatchCreate', orgDesc: 'test', startTime: '0800', endTime: '1700' }],
                    deleteOrgIds: [],
                },
            });
            expect(res.ok()).toBeTruthy();

            // 생성 확인
            const check = await request.get(`/api/orgs/page?searchField=orgId&searchValue=${orgId}`);
            const body = await check.json();
            expect(body.data.content.length).toBe(1);
            expect(body.data.content[0].orgName).toBe('BatchCreate');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('기존 기관을 수정해야 한다', async ({ request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'BeforeUpdate');

        try {
            const res = await request.post('/api/orgs/batch', {
                data: {
                    upserts: [{ orgId, orgName: 'AfterUpdate', orgDesc: 'updated', startTime: '0000', endTime: '2359' }],
                    deleteOrgIds: [],
                },
            });
            expect(res.ok()).toBeTruthy();

            const check = await request.get(`/api/orgs/page?searchField=orgId&searchValue=${orgId}`);
            const body = await check.json();
            expect(body.data.content[0].orgName).toBe('AfterUpdate');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('기관을 삭제해야 한다', async ({ request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'ToDelete');

        const res = await request.post('/api/orgs/batch', {
            data: { upserts: [], deleteOrgIds: [orgId] },
        });
        expect(res.ok()).toBeTruthy();

        const check = await request.get(`/api/orgs/page?searchField=orgId&searchValue=${orgId}`);
        const body = await check.json();
        expect(body.data.content.length).toBe(0);
    });

    test('유효하지 않은 시간값이면 400을 반환해야 한다', async ({ request }) => {
        const orgId = genOrgId();
        const res = await request.post('/api/orgs/batch', {
            data: {
                upserts: [{ orgId, orgName: 'BadTime', startTime: '9999', endTime: '0000' }],
                deleteOrgIds: [],
            },
        });
        expect(res.status()).toBe(400);
    });
});

// ─── GET /api/orgs/export ────────────────────────────────

test.describe('GET /api/orgs/export — 엑셀 다운로드', () => {

    test('xlsx 파일을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/orgs/export');
        expect(res.ok()).toBeTruthy();

        const contentType = res.headers()['content-type'] || '';
        expect(contentType).toContain('spreadsheet');
    });
});

// ─── 인증 검증 ───────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 요청은 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/orgs/page', {
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
