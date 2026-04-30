/**
 * 리스너 응답커넥터 맵핑 API 계약 테스트.
 *
 * 모든 테스트는 자체 데이터를 생성하고 정리한다.
 * 복합 PK: listenerGwId + listenerSystemId + identifier
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

const BASE = '/api/interface-mnt/listener-connector-mappings';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

async function createMapping(
    request: APIRequestContext,
    listenerGwId: string,
    listenerSystemId: string,
    identifier: string,
    connectorGwId: string,
    connectorSystemId: string,
    description = '',
) {
    const res = await request.post(BASE, {
        data: { listenerGwId, listenerSystemId, identifier, connectorGwId, connectorSystemId, description },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteMapping(
    request: APIRequestContext,
    listenerGwId: string,
    listenerSystemId: string,
    identifier: string,
) {
    await request.delete(`${BASE}/${listenerGwId}/${listenerSystemId}/${identifier}`);
}

/** 고유 식별자 생성 */
function uniqueId(prefix: string) {
    return `${prefix}-${Date.now()}`;
}

// ─── GET /api/interface-mnt/listener-connector-mappings ──

test.describe('GET — 페이지네이션 조회', () => {

    test('기본 조회 시 PageResponse 구조를 반환해야 한다', async ({ request }) => {
        const res = await request.get(BASE);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('content');
        expect(body.data).toHaveProperty('totalElements');
        expect(Array.isArray(body.data.content)).toBe(true);
    });

    test('page/size 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}?page=1&size=5`);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
    });

    test('listenerGwId로 필터링되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}?listenerGwId=E2E-LIS-GW`);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThan(0);
        body.data.content.forEach((item: { listenerGwId: string }) => {
            expect(item.listenerGwId).toBe('E2E-LIS-GW');
        });
    });

    test('connectorGwId로 필터링되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}?connectorGwId=E2E-CON-GW`);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        body.data.content.forEach((item: { connectorGwId: string }) => {
            expect(item.connectorGwId).toBe('E2E-CON-GW');
        });
    });

    test('sortBy/sortDirection으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}?sortBy=identifier&sortDirection=DESC`);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThan(0);
    });
});

// ─── GET (상세) ──────────────────────────────────────────

test.describe('GET /{pk} — 상세 조회', () => {

    test('복합 PK로 단건 조회해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}/E2E-LIS-GW/LIS-SYS-01/E2E-ID-01`);
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.listenerGwId).toBe('E2E-LIS-GW');
        expect(body.data.listenerSystemId).toBe('LIS-SYS-01');
        expect(body.data.identifier).toBe('E2E-ID-01');
    });
});

// ─── POST / PUT / DELETE ─────────────────────────────────

test.describe('CRUD — 생성/수정/삭제', () => {

    test('새 맵핑을 생성해야 한다', async ({ request }) => {
        const id = uniqueId('E2E-C');
        try {
            await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id, 'E2E-CON-GW', 'CON-SYS-01', '생성 테스트');

            const check = await request.get(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${id}`);
            const body = await check.json();
            expect(body.data.identifier).toBe(id);
            expect(body.data.description).toBe('생성 테스트');
        } finally {
            await deleteMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id);
        }
    });

    test('기존 맵핑의 설명을 수정해야 한다', async ({ request }) => {
        const id = uniqueId('E2E-U');
        await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id, 'E2E-CON-GW', 'CON-SYS-01', 'BEFORE');

        try {
            const res = await request.put(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${id}`, {
                data: {
                    listenerGwId: 'E2E-LIS-GW',
                    listenerSystemId: 'LIS-SYS-01',
                    identifier: id,
                    connectorGwId: 'E2E-CON-GW',
                    connectorSystemId: 'CON-SYS-02',
                    description: 'AFTER',
                },
            });
            expect(res.ok()).toBeTruthy();

            const check = await request.get(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${id}`);
            const body = await check.json();
            expect(body.data.connectorSystemId).toBe('CON-SYS-02');
            expect(body.data.description).toBe('AFTER');
        } finally {
            await deleteMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id);
        }
    });

    test('맵핑을 삭제해야 한다', async ({ request }) => {
        const id = uniqueId('E2E-D');
        await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id, 'E2E-CON-GW', 'CON-SYS-01');

        const res = await request.delete(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${id}`);
        expect(res.ok()).toBeTruthy();

        const check = await request.get(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${id}`);
        expect(check.ok()).toBeFalsy();
    });
});

// ─── 유효성 검증 ─────────────────────────────────────────

test.describe('유효성 검증', () => {

    test('중복 PK로 생성하면 실패해야 한다', async ({ request }) => {
        const id = uniqueId('E2E-DUP');
        await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id, 'E2E-CON-GW', 'CON-SYS-01');

        try {
            const res = await request.post(BASE, {
                data: {
                    listenerGwId: 'E2E-LIS-GW',
                    listenerSystemId: 'LIS-SYS-01',
                    identifier: id,
                    connectorGwId: 'E2E-CON-GW',
                    connectorSystemId: 'CON-SYS-01',
                },
            });
            expect(res.ok()).toBeFalsy();

            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', id);
        }
    });

    test('존재하지 않는 PK를 조회하면 실패해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}/NON-EXIST-GW/NON-EXIST-SYS/NON-EXIST-ID`);
        expect(res.ok()).toBeFalsy();
    });

    test('필수 필드 누락 시 생성이 실패해야 한다', async ({ request }) => {
        const res = await request.post(BASE, {
            data: {
                listenerGwId: 'E2E-LIS-GW',
                // listenerSystemId 누락
                identifier: 'E2E-MISSING',
                connectorGwId: 'E2E-CON-GW',
                connectorSystemId: 'CON-SYS-01',
            },
        });
        expect(res.ok()).toBeFalsy();
    });
});

// ─── R-only 권한 API 검증 ────────────────────────────────

test.describe('R-only 권한 — 쓰기 API 거부', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자의 POST 요청은 거부되어야 한다', async ({ request }) => {
        const res = await request.post(BASE, {
            data: {
                listenerGwId: 'E2E-LIS-GW',
                listenerSystemId: 'LIS-SYS-01',
                identifier: 'E2E-READONLY-POST',
                connectorGwId: 'E2E-CON-GW',
                connectorSystemId: 'CON-SYS-01',
            },
        });
        expect(res.status()).toBe(403);
    });

    test('R 권한 사용자의 PUT 요청은 거부되어야 한다', async ({ request }) => {
        const res = await request.put(`${BASE}/E2E-LIS-GW/LIS-SYS-01/E2E-ID-01`, {
            data: {
                listenerGwId: 'E2E-LIS-GW',
                listenerSystemId: 'LIS-SYS-01',
                identifier: 'E2E-ID-01',
                connectorGwId: 'E2E-CON-GW',
                connectorSystemId: 'CON-SYS-01',
                description: 'hacked',
            },
        });
        expect(res.status()).toBe(403);
    });

    test('R 권한 사용자의 DELETE 요청은 거부되어야 한다', async ({ request }) => {
        const res = await request.delete(`${BASE}/E2E-LIS-GW/LIS-SYS-01/E2E-ID-01`);
        expect(res.status()).toBe(403);
    });
});

// ─── GET /export ─────────────────────────────────────────

test.describe('GET /export — 엑셀 다운로드', () => {

    test('xlsx 파일을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE}/export`);
        expect(res.ok()).toBeTruthy();

        const contentType = res.headers()['content-type'] || '';
        expect(contentType).toContain('spreadsheet');
    });
});

// ─── 인증 검증 ──────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 요청은 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch(`http://localhost:8080${BASE}`, {
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
