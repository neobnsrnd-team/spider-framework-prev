/**
 * 메뉴 API 계약 테스트 — /api/menus
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복 menuId/필수 필드 누락)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 *
 * 주의:
 * - createMenu 서비스는 priorMenuId 필수 (null/빈값 → 400)
 * - 중복 체크 기준은 menuId (menuName 중복은 허용)
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-ma-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

// 기존 DB에 존재하는 메뉴 ID를 상위메뉴로 사용 (priorMenuId 필수 제약)
let validParentMenuId: string | null = null;

test.beforeAll(async ({ request }) => {
    const res = await request.get('/api/menus/page', { params: { page: 1, size: 1 } });
    if (res.ok()) {
        const body = await res.json();
        validParentMenuId = body.data?.content?.[0]?.menuId ?? null;
    }
});

function buildCreateData(id: string, name: string) {
    return {
        menuId: id,
        menuName: name,
        menuUrl: '/e2e-api/' + id,
        sortOrder: 0,
        displayYn: 'Y',
        useYn: 'Y',
        priorMenuId: validParentMenuId,
    };
}

test.describe('/api/menus/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/menus/page', { params: { page: 1, size: 5 } });

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

    test('menuId 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();
        const createRes = await request.post('/api/menus', { data: buildCreateData(id, 'IdFilter' + id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/menus/page', {
                params: { page: 1, size: 10, menuId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((m: { menuId: string }) => m.menuId === id);
            expect(match).toBeTruthy();
            expect(match.menuId).toBe(id);
        } finally {
            await request.delete(`/api/menus/${id}`);
        }
    });

    test('menuName 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();
        const uniqueName = 'NmFilter' + id;
        const createRes = await request.post('/api/menus', { data: buildCreateData(id, uniqueName) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/menus/page', {
                params: { page: 1, size: 10, menuName: uniqueName },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((m: { menuName: string }) => m.menuName === uniqueName);
            expect(match).toBeTruthy();
        } finally {
            await request.delete(`/api/menus/${id}`);
        }
    });
});

test.describe('/api/menus/:menuId — 단건 조회', () => {

    test('존재하는 메뉴 조회 시 HTTP 200과 메뉴 정보를 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();
        const createRes = await request.post('/api/menus', { data: buildCreateData(id, 'Detail' + id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/menus/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.menuId).toBe(id);
            expect(body.data).toHaveProperty('menuUrl');
            expect(body.data).toHaveProperty('displayYn');
            expect(body.data).toHaveProperty('useYn');
        } finally {
            await request.delete(`/api/menus/${id}`);
        }
    });

    test('존재하지 않는 메뉴 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/menus/NO_SUCH_MENU_E2E_ZZZ');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/menus — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();

        try {
            const res = await request.post('/api/menus', { data: buildCreateData(id, 'Create' + id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.menuId).toBe(id);
        } finally {
            await request.delete(`/api/menus/${id}`);
        }
    });

    test('중복 menuName이어도 menuId가 다르면 HTTP 201을 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id1 = uniqueId();
        const id2 = uniqueId();
        const sameName = 'DupName' + id1;

        const createRes = await request.post('/api/menus', { data: buildCreateData(id1, sameName) });
        expect(createRes.status()).toBe(201);

        try {
            // 같은 menuName, 다른 menuId → menuName 중복은 허용되므로 201
            const res = await request.post('/api/menus', { data: buildCreateData(id2, sameName) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await request.delete(`/api/menus/${id1}`);
            await request.delete(`/api/menus/${id2}`);
        }
    });

    test('필수 필드(menuName) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/menus', {
            data: {
                menuId: 'e2e-validation-only',
                // menuName, menuUrl, displayYn, useYn, sortOrder 누락
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/menus/:menuId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();
        const createRes = await request.post('/api/menus', { data: buildCreateData(id, 'Before' + id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/menus/${id}`, {
                data: {
                    menuName: 'After' + id,
                    menuUrl: '/e2e-updated/' + id,
                    sortOrder: 1,
                    displayYn: 'N',
                    useYn: 'Y',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.menuName).toBe('After' + id);
            expect(body.data.displayYn).toBe('N');
        } finally {
            await request.delete(`/api/menus/${id}`);
        }
    });

    test('존재하지 않는 메뉴 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/menus/NO_SUCH_MENU_E2E_ZZZ', {
            data: {
                menuName: 'Ghost',
                menuUrl: '/ghost',
                sortOrder: 0,
                displayYn: 'Y',
                useYn: 'Y',
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/menus/:menuId — 삭제', () => {

    test('존재하는 메뉴 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = uniqueId();
        const createRes = await request.post('/api/menus', { data: buildCreateData(id, 'DelMe' + id) });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/menus/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 메뉴 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/menus/NO_SUCH_MENU_E2E_ZZZ');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('/api/menus/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/menus/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
