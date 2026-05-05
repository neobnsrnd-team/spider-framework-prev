/**
 * 이행데이터 생성 API 계약 테스트 — /api/trans/generation
 *
 * 검증 범위:
 * - 소스 데이터 조회 (탭별 조회, 검색 필터)
 * - 이행 실행 (성공, 유효성 실패)
 * - SQL ZIP 다운로드 (성공, 유효성 실패)
 * - 인증 검증
 *
 * 시드 데이터: e2e/docker/e2e-seed.sql (E2E-TRX-*, E2ECG*, E2E-ERR-* 등)
 */

import { test, expect } from '@playwright/test';

const SEED_TRX_ID = 'E2E-TRX-001';
const SEED_ORG_ID = 'E2EORG01';
const SEED_CODE_GROUP_ID = 'E2ECG01';

// ─── GET /api/trans/generation/source — 소스 데이터 조회 ─────────

test.describe('GET /api/trans/generation/source — 소스 데이터 조회', () => {

    const TABS = ['TRX', 'MESSAGE', 'CODE', 'WEBAPP', 'SERVICE', 'ERROR', 'COMPONENT', 'PROPERTY'];

    for (const tab of TABS) {
        test(`${tab} 탭 조회 시 HTTP 200과 배열을 반환해야 한다`, async ({ request }) => {
            const res = await request.get('/api/trans/generation/source', {
                params: { tab },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(Array.isArray(body.data)).toBe(true);
            expect(body.data.length).toBeGreaterThan(0);

            const item = body.data[0];
            expect(item).toHaveProperty('id');
            expect(item).toHaveProperty('name');
            expect(item).toHaveProperty('col1');
        });
    }

    test('TRX 탭에서 검색 필터 적용 시 일치하는 결과를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source', {
            params: { tab: 'TRX', searchField: 'trxId', searchValue: SEED_TRX_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);

        const match = body.data.find((d: { col1: string }) =>
            d.col1.toUpperCase().includes(SEED_TRX_ID.toUpperCase()),
        );
        expect(match).toBeTruthy();
    });

    test('CODE 탭에서 코드그룹ID 검색 시 일치하는 결과를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source', {
            params: { tab: 'CODE', searchField: 'codeGroupId', searchValue: SEED_CODE_GROUP_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);
    });

    test('TRX 탭에서 orgId 필터 적용 시 해당 기관의 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source', {
            params: { tab: 'TRX', orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);

        for (const item of body.data) {
            expect(item.col2).toBe(SEED_ORG_ID);
        }
    });

    test('MESSAGE 탭에서 orgId 필터 적용 시 해당 기관의 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source', {
            params: { tab: 'MESSAGE', orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);

        for (const item of body.data) {
            expect(item.col2).toBe(SEED_ORG_ID);
        }
    });

    test('잘못된 탭 유형 조회 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source', {
            params: { tab: 'INVALID_TAB' },
        });

        expect(res.status()).toBe(400);
    });

    test('tab 파라미터 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trans/generation/source');

        expect(res.status()).toBe(400);
    });
});

// ─── POST /api/trans/generation/execute — 이행 실행 (DB 저장) ────

test.describe('POST /api/trans/generation/execute — 이행 실행', () => {

    test('유효한 이행 요청 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const sourceRes = await request.get('/api/trans/generation/source', {
            params: { tab: 'CODE' },
        });
        const sourceBody = await sourceRes.json();
        const item = sourceBody.data[0];

        const res = await request.post('/api/trans/generation/execute', {
            data: {
                tranReason: 'E2E 테스트 이행',
                items: [{
                    tranId: item.id,
                    tranName: item.name,
                    tranType: 'CODE',
                }],
            },
        });

        expect(res.status()).toBe(201);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('tranSeq');
        expect(body.data).toHaveProperty('tranResult', 'S');
        expect(body.data).toHaveProperty('totalCount', 1);
    });

    test('items가 빈 배열이면 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/trans/generation/execute', {
            data: {
                tranReason: 'E2E 빈 목록 테스트',
                items: [],
            },
        });

        expect(res.status()).toBe(400);
    });

    test('items 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/trans/generation/execute', {
            data: {
                tranReason: 'E2E 누락 테스트',
            },
        });

        expect(res.status()).toBe(400);
    });
});

// ─── POST /api/trans/generation/download — SQL ZIP 다운로드 ──────

test.describe('POST /api/trans/generation/download — SQL ZIP 다운로드', () => {

    test('유효한 요청 시 ZIP 파일을 반환해야 한다', async ({ request }) => {
        const sourceRes = await request.get('/api/trans/generation/source', {
            params: { tab: 'CODE' },
        });
        const sourceBody = await sourceRes.json();
        const item = sourceBody.data[0];

        const res = await request.post('/api/trans/generation/download', {
            data: {
                tranReason: 'E2E 다운로드 테스트',
                items: [{
                    tranId: item.id,
                    tranName: item.name,
                    tranType: 'CODE',
                }],
            },
        });

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'];
        expect(contentType).toContain('application/octet-stream');

        const disposition = res.headers()['content-disposition'];
        expect(disposition).toContain('attachment');
        expect(disposition).toContain('.zip');

        const body = await res.body();
        expect(body.length).toBeGreaterThan(0);
    });

    test('items가 빈 배열이면 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/trans/generation/download', {
            data: {
                tranReason: 'E2E 빈 목록',
                items: [],
            },
        });

        expect(res.status()).toBe(400);
    });

    test('trxOnly 옵션을 포함한 요청 시에도 ZIP 파일을 반환해야 한다', async ({ request }) => {
        const sourceRes = await request.get('/api/trans/generation/source', {
            params: { tab: 'TRX' },
        });
        const sourceBody = await sourceRes.json();
        const item = sourceBody.data[0];

        const res = await request.post('/api/trans/generation/download', {
            data: {
                tranReason: 'E2E trxOnly 테스트',
                trxOnly: true,
                items: [{
                    tranId: item.id,
                    tranName: item.name,
                    tranType: 'TRX',
                }],
            },
        });

        expect(res.status()).toBe(200);
        expect(res.headers()['content-type']).toContain('application/octet-stream');

        const body = await res.body();
        expect(body.length).toBeGreaterThan(0);
    });

    test('여러 탭 유형의 항목을 포함할 수 있어야 한다', async ({ request }) => {
        const codeRes = await request.get('/api/trans/generation/source', { params: { tab: 'CODE' } });
        const errorRes = await request.get('/api/trans/generation/source', { params: { tab: 'ERROR' } });

        const codeData = (await codeRes.json()).data;
        const errorData = (await errorRes.json()).data;

        const res = await request.post('/api/trans/generation/download', {
            data: {
                tranReason: 'E2E 복합 다운로드',
                items: [
                    { tranId: codeData[0].id, tranName: codeData[0].name, tranType: 'CODE' },
                    { tranId: errorData[0].id, tranName: errorData[0].name, tranType: 'ERROR' },
                ],
            },
        });

        expect(res.status()).toBe(200);
        expect(res.headers()['content-type']).toContain('application/octet-stream');
    });
});

// ─── 인증 검증 ──────────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 사용자의 source 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/trans/generation/source', {
                params: { tab: 'TRX' },
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 사용자의 execute 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/trans/generation/execute', {
                data: { tranReason: 'test', items: [{ tranId: 'x', tranName: 'x', tranType: 'TRX' }] },
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 사용자의 download 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/trans/generation/download', {
                data: { tranReason: 'test', items: [{ tranId: 'x', tranName: 'x', tranType: 'TRX' }] },
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
