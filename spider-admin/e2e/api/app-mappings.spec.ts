/**
 * 요청처리 App 맵핑 API 계약 테스트 — /api/interface-mnt/app-mappings
 *
 * 검증 범위:
 * - 페이지네이션 조회 (성공 / 검색 필터)
 * - 단건 조회 (성공 / 미존재)
 * - 등록 (성공 / 중복 / 필수값 누락)
 * - 수정 (성공 / 미존재)
 * - 삭제 (성공 / 미존재)
 * - 엑셀 내보내기
 * - 인증 검증 (비인증 요청)
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

async function fetchFirstGatewayId(request: APIRequestContext): Promise<string | null> {
    const res = await request.get('/api/gateways/page', { params: { page: 1, size: 1 } });
    if (!res.ok()) return null;
    const body = await res.json();
    const content: Array<{ gwId: string }> = body.data?.content ?? [];
    return content[0]?.gwId ?? null;
}

async function createAppMapping(
    request: APIRequestContext,
    gwId: string,
    reqIdCode: string,
    extras: { orgId?: string; trxId?: string; bizAppId?: string } = {},
) {
    await request.post('/api/interface-mnt/app-mappings', {
        data: { gwId, reqIdCode, ...extras },
    });
}

async function deleteAppMapping(request: APIRequestContext, gwId: string, reqIdCode: string) {
    await request.delete(
        `/api/interface-mnt/app-mappings/${encodeURIComponent(gwId)}/${encodeURIComponent(reqIdCode)}`,
    );
}

function generateReqIdCode(prefix: string = 'E2E-'): string {
    return (prefix + Date.now().toString(36)).toUpperCase().slice(0, 20);
}

const BASE_URL = '/api/interface-mnt/app-mappings';

// ─── 사전 조건: 게이트웨이 ID 확보 ─────────────────────────

let gwId: string;

test.beforeAll(async ({ request }) => {
    const id = await fetchFirstGatewayId(request);
    if (!id) test.skip(true, '사용 가능한 게이트웨이가 없어 테스트를 건너뜁니다.');
    gwId = id!;
});

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('GET /api/interface-mnt/app-mappings — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL, { params: { page: 1, size: 10 } });

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

    test('검색 필터 적용 시 해당 조건에 맞는 결과만 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('FILTER-');
        await createAppMapping(request, gwId, reqIdCode, { bizAppId: 'filter-app' });

        try {
            const res = await request.get(BASE_URL, {
                params: { page: 1, size: 10, gwId, reqIdCode },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            const match = body.data.content.find(
                (m: { reqIdCode: string }) => m.reqIdCode === reqIdCode,
            );
            expect(match).toBeTruthy();
            expect(match.gwId).toBe(gwId);
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });
});

// ─── 단건 조회 ────────────────────────────────────────────

test.describe('GET /api/interface-mnt/app-mappings/{gwId}/{reqIdCode} — 단건 조회', () => {

    test('존재하는 매핑 조회 시 HTTP 200과 매핑 정보를 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('GET-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            const res = await request.get(
                `${BASE_URL}/${encodeURIComponent(gwId)}/${encodeURIComponent(reqIdCode)}`,
            );

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.gwId).toBe(gwId);
            expect(body.data.reqIdCode).toBe(reqIdCode);
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('존재하지 않는 매핑 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/NON-EXIST-GW/NON-EXIST-CODE`);

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 등록 ─────────────────────────────────────────────────

test.describe('POST /api/interface-mnt/app-mappings — 등록', () => {

    test('유효한 데이터로 등록 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('CREATE-');

        try {
            const res = await request.post(BASE_URL, {
                data: { gwId, reqIdCode },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('중복된 PK로 등록 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('DUP-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            const res = await request.post(BASE_URL, {
                data: { gwId, reqIdCode },
            });

            expect(res.status()).toBe(400);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('필수 필드(gwId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post(BASE_URL, {
            data: { reqIdCode: generateReqIdCode('NOVAL-') },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 필드(reqIdCode) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post(BASE_URL, {
            data: { gwId },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 수정 ─────────────────────────────────────────────────

test.describe('PUT /api/interface-mnt/app-mappings/{gwId}/{reqIdCode} — 수정', () => {

    test('존재하는 매핑 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('UPD-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            const res = await request.put(
                `${BASE_URL}/${encodeURIComponent(gwId)}/${encodeURIComponent(reqIdCode)}`,
                { data: { gwId, reqIdCode, bizAppId: 'updated-app' } },
            );

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('존재하지 않는 매핑 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put(
            `${BASE_URL}/NON-EXIST-GW/NON-EXIST-CODE`,
            { data: { gwId: 'NON-EXIST-GW', reqIdCode: 'NON-EXIST-CODE' } },
        );

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 삭제 ─────────────────────────────────────────────────

test.describe('DELETE /api/interface-mnt/app-mappings/{gwId}/{reqIdCode} — 삭제', () => {

    test('존재하는 매핑 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const reqIdCode = generateReqIdCode('DEL-');
        await createAppMapping(request, gwId, reqIdCode);

        const res = await request.delete(
            `${BASE_URL}/${encodeURIComponent(gwId)}/${encodeURIComponent(reqIdCode)}`,
        );

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 매핑 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete(`${BASE_URL}/NON-EXIST-GW/NON-EXIST-CODE`);

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('GET /api/interface-mnt/app-mappings/export — 엑셀 내보내기', () => {

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
        const res = await ctx.get(BASE_URL, { params: { page: 1, size: 10 } });
        expect(res.status()).toBe(401);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('인증 없이 등록 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.post(BASE_URL, { data: { gwId: 'ANY', reqIdCode: 'ANY' } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 수정 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.put(`${BASE_URL}/ANY/ANY`, { data: { gwId: 'ANY', reqIdCode: 'ANY' } });
        expect(res.status()).toBe(401);
    });

    test('인증 없이 삭제 시 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.delete(`${BASE_URL}/ANY/ANY`);
        expect(res.status()).toBe(401);
    });
});
