/**
 * 전문 관리(Message) API 계약 테스트 — /api/messages
 *
 * 검증 범위:
 * - 전문 유형 목록 조회
 * - 검증 규칙 목록 조회
 * - 헤더 전문 목록 조회
 * - 페이지네이션 조회 (목록, 검색 필터, 기관 필터)
 * - 단건 조회 (존재/미존재)
 * - 상세 조회 (존재/미존재)
 * - 생성 (성공/중복/필수항목 누락)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - 비인증 요청
 *
 * PK: orgId + messageId (복합키)
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

/** E2E 시드 데이터에 존재하는 기관 ID */
const SEED_ORG_ID = 'E2EORG01';

const PREFIX = 'e2e-msg-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function validMessageData(orgId: string, messageId: string) {
    return {
        orgId,
        messageId,
        messageName: 'TestMsg-' + messageId,
        messageDesc: 'E2E test message',
        messageType: '',
        headerYn: 'N',
        requestYn: 'N',
        preLoadYn: 'N',
        lockYn: 'N',
    };
}

async function createMessage(request: APIRequestContext, orgId: string, messageId: string) {
    const res = await request.post('/api/messages', { data: validMessageData(orgId, messageId) });
    expect(res.status()).toBe(201);
}

async function deleteMessage(request: APIRequestContext, orgId: string, messageId: string) {
    await request.delete(`/api/messages/${messageId}`, { params: { orgId } });
}

// ─── 전문 유형 목록 ────────────────────────────────────────

test.describe('GET /api/messages/types — 전문 유형 목록', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/types');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 검증 규칙 목록 ────────────────────────────────────────

test.describe('GET /api/messages/validation-rules — 검증 규칙 목록', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/validation-rules');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 헤더 전문 목록 ────────────────────────────────────────

test.describe('GET /api/messages/headers — 헤더 전문 목록', () => {

    test('기관 ID로 조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/headers', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 페이지네이션 조회 ────────────────────────────────────────

test.describe('GET /api/messages/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/page', { params: { page: 1, size: 5 } });

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
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.get('/api/messages/page', {
                params: { page: 1, size: 10, searchField: 'messageId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((m: { messageId: string }) => m.messageId === id);
            expect(match).toBeTruthy();
            expect(match.messageId).toBe(id);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('기관 ID 필터 적용 시 해당 기관의 전문만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/page', {
            params: { page: 1, size: 100, orgIdFilter: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const content = body.data.content as { orgId: string }[];
        for (const item of content) {
            expect(item.orgId).toBe(SEED_ORG_ID);
        }
    });

    test('전문명으로 검색 시 일치하는 결과를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.get('/api/messages/page', {
                params: { page: 1, size: 10, searchField: 'messageName', searchValue: 'TestMsg-' + id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((m: { messageId: string }) => m.messageId === id);
            expect(match).toBeTruthy();
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });
});

// ─── 단건 조회 ────────────────────────────────────────

test.describe('GET /api/messages/:messageId — 단건 조회', () => {

    test('존재하는 전문 조회 시 HTTP 200과 전문 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.get(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID, includeFields: false },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.orgId).toBe(SEED_ORG_ID);
            expect(body.data.messageId).toBe(id);
            expect(body.data.messageName).toBe('TestMsg-' + id);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('includeFields=true 조회 시 fields 배열을 포함해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.get(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID, includeFields: true },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.messageId).toBe(id);
            expect(Array.isArray(body.data.fields)).toBe(true);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('존재하지 않는 전문 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/no-such-message', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 상세 조회 ────────────────────────────────────────

test.describe('GET /api/messages/:messageId/detail — 상세 조회', () => {

    test('존재하는 전문 상세 조회 시 HTTP 200과 필드 목록을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.get(`/api/messages/${id}/detail`, {
                params: { orgId: SEED_ORG_ID },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.orgId).toBe(SEED_ORG_ID);
            expect(body.data.messageId).toBe(id);
            expect(Array.isArray(body.data.fields)).toBe(true);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('존재하지 않는 전문 상세 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/no-such-message/detail', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 생성 ────────────────────────────────────────

test.describe('POST /api/messages — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/messages', {
                data: validMessageData(SEED_ORG_ID, id),
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.orgId).toBe(SEED_ORG_ID);
            expect(body.data.messageId).toBe(id);
            expect(body.data.messageName).toBe('TestMsg-' + id);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('중복 복합키(orgId+messageId)로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.post('/api/messages', {
                data: validMessageData(SEED_ORG_ID, id),
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('필수 항목(orgId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/messages', {
            data: { messageId: uniqueId(), messageName: 'NoOrg' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 항목(messageId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/messages', {
            data: { orgId: SEED_ORG_ID, messageName: 'NoMsgId' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 수정 ────────────────────────────────────────

test.describe('PUT /api/messages/:messageId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        try {
            const res = await request.put(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
                data: {
                    messageName: 'UpdatedName',
                    messageDesc: 'Updated desc',
                    headerYn: 'N',
                    preLoadYn: 'N',
                    lockYn: 'N',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.messageName).toBe('UpdatedName');
            expect(body.data.messageDesc).toBe('Updated desc');
        } finally {
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });

    test('존재하지 않는 전문 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/messages/no-such-message', {
            params: { orgId: SEED_ORG_ID },
            data: { messageName: 'X' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 삭제 ────────────────────────────────────────

test.describe('DELETE /api/messages/:messageId — 삭제', () => {

    test('존재하는 전문 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createMessage(request, SEED_ORG_ID, id);

        const res = await request.delete(`/api/messages/${id}`, {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 전문 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/messages/no-such-message', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── CRUD 라이프사이클 ────────────────────────────────────────

test.describe('POST → GET → PUT → DELETE — CRUD 라이프사이클', () => {

    test('전문 생성 후 조회, 수정, 삭제가 순차적으로 성공해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            // 1. 생성
            const createRes = await request.post('/api/messages', {
                data: validMessageData(SEED_ORG_ID, id),
            });
            expect(createRes.status()).toBe(201);

            // 2. 단건 조회 확인
            const getRes = await request.get(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
            });
            expect(getRes.status()).toBe(200);
            const getBody = await getRes.json();
            expect(getBody.data.messageId).toBe(id);
            expect(getBody.data.messageName).toBe('TestMsg-' + id);

            // 3. 수정
            const updateRes = await request.put(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
                data: {
                    messageName: 'CycleUpdated',
                    messageDesc: 'cycle desc',
                    headerYn: 'N',
                    preLoadYn: 'N',
                    lockYn: 'N',
                },
            });
            expect(updateRes.status()).toBe(200);
            const updateBody = await updateRes.json();
            expect(updateBody.data.messageName).toBe('CycleUpdated');

            // 4. 수정 확인
            const verifyRes = await request.get(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
            });
            expect(verifyRes.status()).toBe(200);
            const verifyBody = await verifyRes.json();
            expect(verifyBody.data.messageName).toBe('CycleUpdated');

            // 5. 삭제
            const deleteRes = await request.delete(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
            });
            expect(deleteRes.status()).toBe(200);

            // 6. 삭제 확인
            const gone = await request.get(`/api/messages/${id}`, {
                params: { orgId: SEED_ORG_ID },
            });
            expect(gone.status()).toBe(404);
        } finally {
            // 안전망: 테스트 실패 시에도 정리
            await deleteMessage(request, SEED_ORG_ID, id);
        }
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('GET /api/messages/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/messages/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

// ─── 비인증 요청 ────────────────────────────────────────

test.describe('비인증 요청 — 접근 제어', () => {

    test('비인증 상태에서 GET /api/messages/page 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.get('/api/messages/page', { params: { page: 1, size: 5 } });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 POST /api/messages 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/messages', {
                data: validMessageData(SEED_ORG_ID, 'unauth-test'),
            });
            expect(res.status()).toBe(401);
        } finally {
            await ctx.dispose();
        }
    });
});
