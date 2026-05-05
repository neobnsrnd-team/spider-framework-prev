/**
 * Component API 계약 테스트 — /api/components
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재, PARAM 포함 확인)
 * - 생성 (성공/중복/유효성)
 * - 수정 (성공/미존재, PARAM 교체 확인)
 * - 삭제 (성공/미존재)
 * - 인증 검증
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-c-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function componentData(id: string, overrides: Record<string, unknown> = {}) {
    return {
        componentId: id,
        componentName: `테스트 컴포넌트 ${id}`,
        componentType: 'J',
        componentClassName: 'com.spider.e2e.TestComponent',
        componentMethodName: 'execute',
        useYn: 'Y',
        params: [],
        ...overrides,
    };
}

async function createComponent(request: Parameters<typeof test>[1] extends { request: infer R } ? R : never, id: string, overrides: Record<string, unknown> = {}) {
    const res = await request.post('/api/components', { data: componentData(id, overrides) });
    expect(res.status()).toBe(201);
    return id;
}

async function deleteComponent(request: Parameters<typeof test>[1] extends { request: infer R } ? R : never, id: string) {
    await request.delete(`/api/components/${encodeURIComponent(id)}`);
}

// ─── 페이지네이션 조회 ────────────────────────────────────────────────

test.describe('GET /api/components/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/components/page', { params: { page: 1, size: 5 } });

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

    test('목록 응답에는 params 필드가 포함되어서는 안 된다 (성능)', async ({ request }) => {
        const res = await request.get('/api/components/page', { params: { page: 1, size: 5 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        const content: Record<string, unknown>[] = body.data.content;
        if (content.length > 0) {
            // 목록은 params 없거나 null이어야 함
            expect(content[0].params == null || content[0].params === undefined).toBeTruthy();
        }
    });

    test('검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createComponent(request, id);

        try {
            const res = await request.get('/api/components/page', {
                params: { page: 1, size: 10, componentId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.content.length).toBeGreaterThanOrEqual(1);
            expect(body.data.content.some((c: { componentId: string }) => c.componentId === id)).toBe(true);
        } finally {
            await deleteComponent(request, id);
        }
    });
});

// ─── 단건 조회 ────────────────────────────────────────────────────────

test.describe('GET /api/components/:componentId — 단건 조회', () => {

    test('존재하는 컴포넌트 조회 시 HTTP 200과 params 포함 응답을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/components/e2e-cmp-001');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.componentId).toBe('e2e-cmp-001');
        expect(Array.isArray(body.data.params)).toBe(true);
        expect(body.data.params.length).toBeGreaterThan(0);
        // PARAM 필드 검증
        const param = body.data.params[0];
        expect(param).toHaveProperty('paramSeqNo');
        expect(param).toHaveProperty('paramKey');
    });

    test('존재하지 않는 컴포넌트 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/components/NOT-EXIST-COMPONENT');
        expect(res.status()).toBe(404);
    });
});

// ─── 생성 ─────────────────────────────────────────────────────────────

test.describe('POST /api/components — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201과 ComponentResponse를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const res = await request.post('/api/components', { data: componentData(id) });

        try {
            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.componentId).toBe(id);
            expect(Array.isArray(body.data.params)).toBe(true);
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('params 포함 생성 시 HTTP 201과 params 포함 응답을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const data = componentData(id, {
            params: [
                { paramSeqNo: 1, paramKey: 'key1', paramDesc: '설명1', defaultParamValue: 'val1' },
                { paramSeqNo: 2, paramKey: 'key2', paramDesc: '설명2', defaultParamValue: null },
            ],
        });

        const res = await request.post('/api/components', { data });

        try {
            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.params).toHaveLength(2);
            expect(body.data.params[0].paramKey).toBe('key1');
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/components', { data: componentData('e2e-cmp-001') });
        expect(res.status()).toBe(409);
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/components', {
            data: { componentName: '이름만있음' },
        });
        expect(res.status()).toBe(400);
    });
});

// ─── 수정 ─────────────────────────────────────────────────────────────

test.describe('PUT /api/components/:componentId — 수정', () => {

    test('존재하는 컴포넌트 수정 시 HTTP 200과 수정된 응답을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createComponent(request, id);

        try {
            const updateData = {
                componentName: '수정된 컴포넌트명',
                componentType: 'J',
                componentClassName: 'com.spider.e2e.UpdatedComponent',
                componentMethodName: 'process',
                useYn: 'Y',
                params: [],
            };
            const res = await request.put(`/api/components/${encodeURIComponent(id)}`, { data: updateData });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.componentName).toBe('수정된 컴포넌트명');
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('수정 시 params 교체가 정상 동작해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createComponent(request, id, {
            params: [{ paramSeqNo: 1, paramKey: 'oldKey', paramDesc: '구파라미터', defaultParamValue: null }],
        });

        try {
            const updateData = {
                componentName: `테스트 컴포넌트 ${id}`,
                componentType: 'J',
                componentClassName: 'com.spider.e2e.TestComponent',
                componentMethodName: 'execute',
                useYn: 'Y',
                params: [
                    { paramSeqNo: 1, paramKey: 'newKey1', paramDesc: '신파라미터1', defaultParamValue: 'v1' },
                    { paramSeqNo: 2, paramKey: 'newKey2', paramDesc: '신파라미터2', defaultParamValue: 'v2' },
                ],
            };
            const res = await request.put(`/api/components/${encodeURIComponent(id)}`, { data: updateData });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.data.params).toHaveLength(2);
            expect(body.data.params.some((p: { paramKey: string }) => p.paramKey === 'oldKey')).toBe(false);
            expect(body.data.params[0].paramKey).toBe('newKey1');
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('존재하지 않는 컴포넌트 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/components/NOT-EXIST-COMPONENT', {
            data: {
                componentName: '수정',
                componentType: 'J',
                componentClassName: 'com.test.C',
                componentMethodName: 'run',
                useYn: 'Y',
                params: [],
            },
        });
        expect(res.status()).toBe(404);
    });
});

// ─── 삭제 ─────────────────────────────────────────────────────────────

test.describe('DELETE /api/components/:componentId — 삭제', () => {

    test('존재하는 컴포넌트 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createComponent(request, id);

        const res = await request.delete(`/api/components/${encodeURIComponent(id)}`);
        expect(res.status()).toBe(200);

        // 삭제 후 조회하면 404여야 한다
        const getRes = await request.get(`/api/components/${encodeURIComponent(id)}`);
        expect(getRes.status()).toBe(404);
    });

    test('params가 있는 컴포넌트 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createComponent(request, id, {
            params: [{ paramSeqNo: 1, paramKey: 'k1', paramDesc: null, defaultParamValue: null }],
        });

        const res = await request.delete(`/api/components/${encodeURIComponent(id)}`);
        expect(res.status()).toBe(200);
    });

    test('존재하지 않는 컴포넌트 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/components/NOT-EXIST-COMPONENT');
        expect(res.status()).toBe(404);
    });
});

// ─── 인증 검증 ────────────────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test('비인증 목록 조회 시 HTTP 401을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/components/page');
        expect(res.status()).toBe(401);
    });

    test('비인증 단건 조회 시 HTTP 401을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/components/e2e-cmp-001');
        expect(res.status()).toBe(401);
    });

    test('비인증 생성 요청 시 HTTP 401을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/components', { data: componentData(uniqueId()) });
        expect(res.status()).toBe(401);
    });
});
