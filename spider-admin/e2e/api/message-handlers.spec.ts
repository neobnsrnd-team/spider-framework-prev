/**
 * 전문처리핸들러 API 계약 테스트 — /api/message-handlers
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 필터)
 * - 배치 저장 (upsert 생성 → 목록 확인 → upsert 수정 → 배치 삭제)
 * - 엑셀 내보내기
 * - 인증 검증 (비인증 요청 401)
 */

import { test, expect, APIRequestContext } from '@playwright/test';

/** 시드된 기관 ID (e2e-seed.sql) */
const SEED_ORG_ID = 'E2EORG01';

/**
 * FWK_TRANSPORT에서 유효한 trxType을 동적으로 조회한다.
 * trx-types 옵션 API가 비어 있으면 테스트를 건너뛴다.
 */
async function fetchValidTrxType(request: APIRequestContext): Promise<string> {
    const res = await request.get('/api/transports/options/trx-types');
    expect(res.status()).toBe(200);
    const body = await res.json();
    const options: { trxType: string }[] = body.data ?? [];
    if (options.length === 0) {
        test.skip(true, 'FWK_TRANSPORT에 거래유형 데이터가 없어 테스트를 건너뜁니다');
    }
    return options[0].trxType;
}

// ─── 페이지네이션 조회 ─────────────────────────────────────────────

test.describe('GET /api/message-handlers — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-handlers', {
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

    test('orgId 필터 적용 시 해당 기관 결과만 반환해야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        // 테스트 데이터 생성
        const createRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'I',
                    operModeType: 'D',
                    handler: 'com.e2e.FilterTestHandler',
                    handlerDesc: 'filter-test',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });
        expect(createRes.status()).toBe(200);

        try {
            const res = await request.get('/api/message-handlers', {
                params: { page: 1, size: 100, orgId: SEED_ORG_ID },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            for (const item of body.data.content) {
                expect(item.orgId).toBe(SEED_ORG_ID);
            }
        } finally {
            await request.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [],
                    deletes: [{ orgId: SEED_ORG_ID, trxType, ioType: 'I', operModeType: 'D' }],
                },
            });
        }
    });

    test('ioType 필터 적용 시 해당 구분 결과만 반환해야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        const createRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'O',
                    operModeType: 'T',
                    handler: 'com.e2e.IoTypeFilterHandler',
                    handlerDesc: 'ioType-filter-test',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });
        expect(createRes.status()).toBe(200);

        try {
            const res = await request.get('/api/message-handlers', {
                params: { page: 1, size: 100, ioType: 'O' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            for (const item of body.data.content) {
                expect(item.ioType).toBe('O');
            }
        } finally {
            await request.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [],
                    deletes: [{ orgId: SEED_ORG_ID, trxType, ioType: 'O', operModeType: 'T' }],
                },
            });
        }
    });
});

// ─── 배치 저장 (Upsert + Delete) ───────────────────────────────────

test.describe('POST /api/message-handlers/batch — 배치 저장', () => {

    test('upsert로 핸들러를 생성한 뒤 목록에서 조회되어야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        const createRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'I',
                    operModeType: 'R',
                    handler: 'com.e2e.CreateTestHandler',
                    handlerDesc: 'create-test',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });
        expect(createRes.status()).toBe(200);
        const createBody = await createRes.json();
        expect(createBody.success).toBe(true);

        try {
            // 생성 확인
            const listRes = await request.get('/api/message-handlers', {
                params: { page: 1, size: 100, orgId: SEED_ORG_ID, trxType, ioType: 'I' },
            });
            expect(listRes.status()).toBe(200);
            const listBody = await listRes.json();

            const created = listBody.data.content.find(
                (h: { orgId: string; trxType: string; ioType: string; operModeType: string }) =>
                    h.orgId === SEED_ORG_ID && h.trxType === trxType && h.ioType === 'I' && h.operModeType === 'R',
            );
            expect(created).toBeTruthy();
            expect(created.handler).toBe('com.e2e.CreateTestHandler');
            expect(created.handlerDesc).toBe('create-test');
            expect(created.stopYn).toBe('N');
        } finally {
            await request.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [],
                    deletes: [{ orgId: SEED_ORG_ID, trxType, ioType: 'I', operModeType: 'R' }],
                },
            });
        }
    });

    test('upsert로 기존 핸들러를 수정하면 변경된 값이 반영되어야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        // 1. 생성
        const createRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'Q',
                    operModeType: 'D',
                    handler: 'com.e2e.BeforeUpdate',
                    handlerDesc: 'before-update',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });
        expect(createRes.status()).toBe(200);

        try {
            // 2. 수정 (같은 PK, 다른 handler/desc)
            const updateRes = await request.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [{
                        orgId: SEED_ORG_ID,
                        trxType,
                        ioType: 'Q',
                        operModeType: 'D',
                        handler: 'com.e2e.AfterUpdate',
                        handlerDesc: 'after-update',
                        stopYn: 'Y',
                    }],
                    deletes: [],
                },
            });
            expect(updateRes.status()).toBe(200);

            // 3. 수정 확인
            const listRes = await request.get('/api/message-handlers', {
                params: { page: 1, size: 100, orgId: SEED_ORG_ID, ioType: 'Q' },
            });
            expect(listRes.status()).toBe(200);
            const listBody = await listRes.json();

            const updated = listBody.data.content.find(
                (h: { orgId: string; trxType: string; ioType: string; operModeType: string }) =>
                    h.orgId === SEED_ORG_ID && h.trxType === trxType && h.ioType === 'Q' && h.operModeType === 'D',
            );
            expect(updated).toBeTruthy();
            expect(updated.handler).toBe('com.e2e.AfterUpdate');
            expect(updated.handlerDesc).toBe('after-update');
            expect(updated.stopYn).toBe('Y');
        } finally {
            await request.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [],
                    deletes: [{ orgId: SEED_ORG_ID, trxType, ioType: 'Q', operModeType: 'D' }],
                },
            });
        }
    });

    test('delete로 핸들러를 삭제하면 목록에서 사라져야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        // 생성
        const createRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'S',
                    operModeType: 'T',
                    handler: 'com.e2e.DeleteTestHandler',
                    handlerDesc: 'delete-test',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });
        expect(createRes.status()).toBe(200);

        // 삭제
        const deleteRes = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [],
                deletes: [{ orgId: SEED_ORG_ID, trxType, ioType: 'S', operModeType: 'T' }],
            },
        });
        expect(deleteRes.status()).toBe(200);
        const deleteBody = await deleteRes.json();
        expect(deleteBody.success).toBe(true);

        // 삭제 확인
        const listRes = await request.get('/api/message-handlers', {
            params: { page: 1, size: 100, orgId: SEED_ORG_ID, ioType: 'S' },
        });
        expect(listRes.status()).toBe(200);
        const listBody = await listRes.json();

        const deleted = listBody.data.content.find(
            (h: { orgId: string; trxType: string; ioType: string; operModeType: string }) =>
                h.orgId === SEED_ORG_ID && h.trxType === trxType && h.ioType === 'S' && h.operModeType === 'T',
        );
        expect(deleted).toBeUndefined();
    });

    test('동일 PK 조합이 중복된 upserts 요청 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        const res = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [
                    {
                        orgId: SEED_ORG_ID,
                        trxType,
                        ioType: 'I',
                        operModeType: 'D',
                        handler: 'com.e2e.Dup1',
                        handlerDesc: 'dup1',
                        stopYn: 'N',
                    },
                    {
                        orgId: SEED_ORG_ID,
                        trxType,
                        ioType: 'I',
                        operModeType: 'D',
                        handler: 'com.e2e.Dup2',
                        handlerDesc: 'dup2',
                        stopYn: 'N',
                    },
                ],
                deletes: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('유효하지 않은 ioType으로 요청 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        const res = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'Z',
                    operModeType: 'D',
                    handler: 'com.e2e.InvalidIo',
                    handlerDesc: 'invalid',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('유효하지 않은 operModeType으로 요청 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const trxType = await fetchValidTrxType(request);

        const res = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType,
                    ioType: 'I',
                    operModeType: 'Z',
                    handler: 'com.e2e.InvalidOperMode',
                    handlerDesc: 'invalid',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/message-handlers/batch', {
            params: { orgId: SEED_ORG_ID },
            data: {
                upserts: [{
                    orgId: SEED_ORG_ID,
                    trxType: '',
                    ioType: 'I',
                    operModeType: 'D',
                    handler: '',
                    handlerDesc: '',
                    stopYn: 'N',
                }],
                deletes: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 엑셀 내보내기 ─────────────────────────────────────────────────

test.describe('GET /api/message-handlers/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-handlers/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });

    test('orgId 필터 적용 시에도 엑셀 파일을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/message-handlers/export', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');
    });
});

// ─── 인증 검증 ─────────────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태로 목록 조회 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });

        try {
            const res = await ctx.get('/api/message-handlers', {
                params: { page: 1, size: 5 },
            });

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태로 배치 저장 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });

        try {
            const res = await ctx.post('/api/message-handlers/batch', {
                params: { orgId: SEED_ORG_ID },
                data: {
                    upserts: [{
                        orgId: SEED_ORG_ID,
                        trxType: '1',
                        ioType: 'I',
                        operModeType: 'D',
                        handler: 'com.e2e.Unauth',
                        handlerDesc: 'unauth',
                        stopYn: 'N',
                    }],
                    deletes: [],
                },
            });

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태로 엑셀 내보내기 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });

        try {
            const res = await ctx.get('/api/message-handlers/export');

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await ctx.dispose();
        }
    });
});
