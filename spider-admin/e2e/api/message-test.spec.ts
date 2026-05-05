/**
 * 전문 테스트(MessageTest) API 계약 테스트 — /api/message-test
 *
 * 검증 범위:
 * - 내 테스트 케이스 목록 조회
 * - 인스턴스 ID 목록 조회
 * - CRUD 라이프사이클 (생성 → 거래별 조회 → 수정 → 삭제)
 * - 거래별 테스트 케이스 조회 (검색 필터 포함)
 * - 유효성 검증 (필수 필드 누락)
 * - 존재하지 않는 리소스 처리
 * - 인증 검증 (비인증 요청)
 */

import { test, expect } from '@playwright/test';

// ─── GET /api/message-test — 내 테스트 케이스 목록 ─────────────────

test.describe('GET /api/message-test — 내 테스트 케이스 목록 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-test');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── GET /api/message-test/instance-ids — 인스턴스 ID 목록 ────────

test.describe('GET /api/message-test/instance-ids — 인스턴스 ID 목록 조회', () => {

    test('조회 시 HTTP 200과 문자열 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-test/instance-ids');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('E2E 시드 인스턴스(E2E1)가 목록에 포함되어야 한다', async ({ request }) => {
        const res = await request.get('/api/message-test/instance-ids');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data).toContain('E2E1');
    });
});

// ─── CRUD 라이프사이클 ─────────────────────────────────────────────

test.describe('POST → GET /by-trx → PUT → DELETE — CRUD 라이프사이클', () => {

    test('테스트 케이스를 생성하고 조회, 수정, 삭제할 수 있어야 한다', async ({ request }) => {
        // --- CREATE ---
        const createRes = await request.post('/api/message-test', {
            data: {
                orgId: 'E2EORG01',
                messageId: 'E2E-MSG-001',
                trxId: 'E2E-TRX-001',
                testName: 'E2E-CRUD-테스트',
                testDesc: 'E2E CRUD 라이프사이클 테스트',
                testData: 'field1=value1;field2=value2;',
                headerYn: 'N',
                xmlYn: 'N',
            },
        });

        expect(createRes.status()).toBe(201);
        const createBody = await createRes.json();
        expect(createBody.success).toBe(true);

        const created = createBody.data;
        expect(created.testSno).toBeTruthy();
        expect(created.testName).toBe('E2E-CRUD-테스트');
        expect(created.orgId).toBe('E2EORG01');
        expect(created.messageId).toBe('E2E-MSG-001');
        expect(created.trxId).toBe('E2E-TRX-001');

        const testSno = created.testSno;

        try {
            // --- GET /by-trx (조회) ---
            // Note: by-trx 엔드포인트는 내부적으로 MESSAGE_ID 기준으로 조회
            const byTrxRes = await request.get(`/api/message-test/by-trx/${created.messageId}`);

            expect(byTrxRes.status()).toBe(200);
            const byTrxBody = await byTrxRes.json();
            expect(byTrxBody.success).toBe(true);
            expect(Array.isArray(byTrxBody.data)).toBe(true);

            const found = byTrxBody.data.find(
                (item: { testSno: number }) => item.testSno === testSno
            );
            expect(found).toBeTruthy();
            expect(found.testName).toBe('E2E-CRUD-테스트');

            // --- GET /api/message-test (내 목록에도 포함 확인) ---
            const myListRes = await request.get('/api/message-test');
            expect(myListRes.status()).toBe(200);
            const myListBody = await myListRes.json();
            const myFound = myListBody.data.find(
                (item: { testSno: number }) => item.testSno === testSno
            );
            expect(myFound).toBeTruthy();

            // --- UPDATE ---
            const updateRes = await request.put(`/api/message-test/${testSno}`, {
                data: {
                    testSno,
                    orgId: 'E2EORG01',
                    messageId: 'E2E-MSG-001',
                    trxId: 'E2E-TRX-001',
                    testName: 'E2E-CRUD-수정됨',
                    testDesc: '수정된 설명',
                    testData: 'field1=updated;',
                    headerYn: 'N',
                    xmlYn: 'N',
                },
            });

            expect(updateRes.status()).toBe(200);
            const updateBody = await updateRes.json();
            expect(updateBody.success).toBe(true);
            expect(updateBody.data.testName).toBe('E2E-CRUD-수정됨');
            expect(updateBody.data.testDesc).toBe('수정된 설명');

        } finally {
            // --- DELETE ---
            const deleteRes = await request.delete(`/api/message-test/${testSno}`);
            expect(deleteRes.status()).toBe(200);
            const deleteBody = await deleteRes.json();
            expect(deleteBody.success).toBe(true);
        }
    });
});

// ─── GET /api/message-test/by-trx — 거래별 조회 및 검색 ──────────

test.describe('GET /api/message-test/by-trx/:trxId — 거래별 조회', () => {

    test('존재하지 않는 거래 ID 조회 시 HTTP 200과 빈 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-test/by-trx/NONEXISTENT-TRX-E2E');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
        expect(body.data).toHaveLength(0);
    });

    test('검색 필터로 조회 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        // 테스트 케이스 생성
        const createRes = await request.post('/api/message-test', {
            data: {
                orgId: 'E2EORG01',
                messageId: 'E2E-MSG-001',
                trxId: 'E2E-TRX-001',
                testName: 'E2E-검색필터-테스트',
                testDesc: '검색 필터 검증용',
                testData: 'searchKey=searchValue;',
                headerYn: 'N',
                xmlYn: 'N',
            },
        });
        expect(createRes.status()).toBe(201);
        const testSno = (await createRes.json()).data.testSno;

        try {
            // testName 필터 검색
            const searchRes = await request.get(`/api/message-test/by-trx/E2E-MSG-001`, {
                params: { testName: 'E2E-검색필터' },
            });

            expect(searchRes.status()).toBe(200);
            const body = await searchRes.json();
            expect(body.success).toBe(true);
            expect(Array.isArray(body.data)).toBe(true);

            const match = body.data.find(
                (item: { testSno: number }) => item.testSno === testSno
            );
            expect(match).toBeTruthy();
            expect(match.testName).toContain('E2E-검색필터');
        } finally {
            await request.delete(`/api/message-test/${testSno}`);
        }
    });
});

// ─── POST /api/message-test — 유효성 검증 ─────────────────────────

test.describe('POST /api/message-test — 유효성 검증', () => {

    test('필수 필드(orgId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-test', {
            data: {
                messageId: 'E2E-MSG-001',
                testName: 'MissingOrgId',
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 필드(messageId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-test', {
            data: {
                orgId: 'E2EORG01',
                testName: 'MissingMessageId',
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 필드(testName) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-test', {
            data: {
                orgId: 'E2EORG01',
                messageId: 'E2E-MSG-001',
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('빈 요청 본문 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-test', {
            data: {},
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── PUT/DELETE — 존재하지 않는 리소스 ─────────────────────────────

test.describe('PUT/DELETE — 존재하지 않는 리소스 처리', () => {

    test('존재하지 않는 testSno 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/message-test/999999999', {
            data: {
                testSno: 999999999,
                orgId: 'E2EORG01',
                messageId: 'E2E-MSG-001',
                testName: 'NotExist',
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('존재하지 않는 testSno 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/message-test/999999999');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── GET /api/message-test/fields — 메시지 필드 조회 ──────────────

test.describe('GET /api/message-test/fields — 메시지 필드 조회', () => {

    test('존재하지 않는 trxId로 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-test/fields', {
            params: { trxId: 'NONEXISTENT-TRX-E2E', ioType: 'I' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── POST /api/message-test/simulate — 시뮬레이션 ─────────────────

test.describe('POST /api/message-test/simulate — 시뮬레이션', () => {

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-test/simulate', {
            data: {},
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('유효한 요청 시 HTTP 200과 시뮬레이션 응답을 반환해야 한다', async ({ request }) => {
        // 시뮬레이션은 실제 WAS 인스턴스 연결을 시도하므로 success=false가 될 수 있지만,
        // API 자체는 200을 반환하고 내부 success 필드로 결과를 전달한다.
        const res = await request.post('/api/message-test/simulate', {
            data: {
                orgId: 'E2EORG01',
                trxId: 'E2E-TRX-001',
                instanceId: 'E2E1',
                fieldData: {},
            },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        // 내부 시뮬레이션 결과 (WAS 미기동 환경에서는 success=false)
        const data = body.data;
        expect(data).toHaveProperty('success');
        expect(data).toHaveProperty('request');
    });
});

// ─── 인증 검증 ────────────────────────────────────────────────────

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

    test('인증 없이 내 테스트 케이스 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get('/api/message-test');
        expect(res.status()).toBe(401);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('인증 없이 인스턴스 ID 목록 조회 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.get('/api/message-test/instance-ids');
        expect(res.status()).toBe(401);
    });

    test('인증 없이 테스트 케이스 생성 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.post('/api/message-test', {
            data: { orgId: 'E2EORG01', messageId: 'E2E-MSG-001', testName: 'Unauth' },
        });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 테스트 케이스 수정 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.put('/api/message-test/1', {
            data: { testSno: 1, orgId: 'E2EORG01', messageId: 'E2E-MSG-001', testName: 'Unauth' },
        });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 테스트 케이스 삭제 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.delete('/api/message-test/1');
        expect(res.status()).toBe(401);
    });

    test('인증 없이 시뮬레이션 요청 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.post('/api/message-test/simulate', {
            data: { orgId: 'E2EORG01', trxId: 'E2E-TRX-001', instanceId: 'E2E1' },
        });
        expect(res.status()).toBe(401);
    });
});
