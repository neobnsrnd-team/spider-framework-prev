/**
 * 오류코드 API 계약 테스트 — /api/errors, /api/handle-apps
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 상세 조회 (존재/미존재)
 * - 생성 (성공/중복/필수항목 누락)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - 핸들러 목록 조회 / 핸들러 저장
 * - 비인증 요청
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

const PREFIX = 'e2e-err-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function validErrorData(errorCode: string) {
    return {
        errorCode,
        errorTitle: 'TestError-' + errorCode,
        errorLevel: '1',
    };
}

async function createError(request: APIRequestContext, errorCode: string) {
    const res = await request.post('/api/errors', { data: validErrorData(errorCode) });
    expect(res.status()).toBe(201);
}

async function deleteError(request: APIRequestContext, errorCode: string) {
    await request.delete(`/api/errors/${errorCode}`);
}

// ─── 페이지네이션 조회 ────────────────────────────────────────

test.describe('/api/errors/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/errors/page', { params: { page: 1, size: 5 } });

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

    test('검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.get('/api/errors/page', {
                params: { page: 1, size: 10, searchField: 'errorCode', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((e: { errorCode: string }) => e.errorCode === id);
            expect(match).toBeTruthy();
            expect(match.errorCode).toBe(id);
        } finally {
            await deleteError(request, id);
        }
    });

    test('오류제목으로 검색 시 일치하는 결과를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.get('/api/errors/page', {
                params: { page: 1, size: 10, searchField: 'errorTitle', searchValue: 'TestError-' + id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((e: { errorCode: string }) => e.errorCode === id);
            expect(match).toBeTruthy();
        } finally {
            await deleteError(request, id);
        }
    });
});

// ─── 단건 상세 조회 ────────────────────────────────────────

test.describe('/api/errors/:errorCode — 상세 조회', () => {

    test('존재하는 오류코드 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.get(`/api/errors/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.errorCode).toBe(id);
            expect(body.data.errorTitle).toBe('TestError-' + id);
            expect(body.data.errorLevel).toBe('1');
        } finally {
            await deleteError(request, id);
        }
    });

    test('존재하지 않는 오류코드 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/errors/no-such-error-code');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 생성 ────────────────────────────────────────

test.describe('POST /api/errors — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/errors', {
                data: validErrorData(id),
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.errorCode).toBe(id);
        } finally {
            await deleteError(request, id);
        }
    });

    test('다국어 정보를 포함하여 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/errors', {
                data: {
                    ...validErrorData(id),
                    koErrorTitle: '한국어 오류',
                    enErrorTitle: 'English Error',
                    koErrorCauseDesc: '한국어 원인',
                    enErrorCauseDesc: 'English cause',
                },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);

            // 상세 조회로 다국어 정보 확인
            const detailRes = await request.get(`/api/errors/${id}`);
            const detail = await detailRes.json();
            expect(detail.data.koErrorTitle).toBe('한국어 오류');
            expect(detail.data.enErrorTitle).toBe('English Error');
        } finally {
            await deleteError(request, id);
        }
    });

    test('중복 오류코드로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.post('/api/errors', {
                data: validErrorData(id),
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteError(request, id);
        }
    });

    test('필수 항목 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/errors', {
            data: { errorCode: uniqueId() },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 수정 ────────────────────────────────────────

test.describe('PUT /api/errors/:errorCode — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.put(`/api/errors/${id}`, {
                data: { errorTitle: 'UpdatedTitle', errorLevel: '2' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.errorTitle).toBe('UpdatedTitle');
        } finally {
            await deleteError(request, id);
        }
    });

    test('존재하지 않는 오류코드 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/errors/no-such-error-code', {
            data: { errorTitle: 'X', errorLevel: '1' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 삭제 ────────────────────────────────────────

test.describe('DELETE /api/errors/:errorCode — 삭제', () => {

    test('존재하는 오류코드 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        const res = await request.delete(`/api/errors/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 오류코드 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/errors/no-such-error-code');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('/api/errors/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/errors/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

// ─── 핸들러 ────────────────────────────────────────

test.describe('/api/errors/:errorCode/handle-apps — 핸들러 관리', () => {

    test('핸들러 목록 조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            const res = await request.get(`/api/errors/${id}/handle-apps`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(Array.isArray(body.data)).toBe(true);
        } finally {
            await deleteError(request, id);
        }
    });

    test('핸들러 저장 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createError(request, id);

        try {
            // 먼저 사용 가능한 핸들러 목록 조회
            const handleAppsRes = await request.get('/api/handle-apps');
            const handleApps = await handleAppsRes.json();

            if (handleApps.data && handleApps.data.length > 0) {
                const firstHandler = handleApps.data[0];

                const res = await request.put(`/api/errors/${id}/handle-apps`, {
                    data: [{ handleAppId: firstHandler.handleAppId, userParamValue: 'testParam' }],
                });

                expect(res.status()).toBe(200);
                const body = await res.json();
                expect(body.success).toBe(true);

                // 저장된 핸들러 확인
                const verifyRes = await request.get(`/api/errors/${id}/handle-apps`);
                const verifyBody = await verifyRes.json();
                expect(verifyBody.data.length).toBe(1);
                expect(verifyBody.data[0].handleAppId).toBe(firstHandler.handleAppId);
            }
        } finally {
            await deleteError(request, id);
        }
    });
});

// ─── 핸들러 앱 전체 목록 ────────────────────────────────────────

test.describe('/api/handle-apps — 핸들러 앱 목록', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/handle-apps');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 비인증 요청 ────────────────────────────────────────

test.describe('비인증 요청 — 접근 제어', () => {

    test('비인증 상태에서 GET /api/errors/page 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/errors/page', { params: { page: 1, size: 5 } });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 POST /api/errors 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/errors', { data: validErrorData('unauth-test') });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
