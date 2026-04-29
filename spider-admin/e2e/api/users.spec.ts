/**
 * 사용자 API 계약 테스트 — /api/users, /api/user-menus
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복/유효성 실패)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 중복 확인 (존재/미존재)
 * - 로그인 오류 횟수 초기화
 * - 엑셀 내보내기
 * - 사용자 메뉴 (현재 사용자, 사용자별, 역할별, 일괄 저장)
 * - 비인증 요청 접근 제어
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

const PREFIX = 'e2e-u-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function validUserData(userId: string) {
    return { userId, userName: 'TestUser-' + userId, password: 'Test1234!', roleId: 'ADMIN' };
}

async function createUser(request: APIRequestContext, userId: string) {
    const res = await request.post('/api/users', { data: validUserData(userId) });
    expect(res.status()).toBe(201);
}

async function deleteUser(request: APIRequestContext, userId: string) {
    await request.delete(`/api/users/${userId}`);
}

test.describe('/api/users/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/page', { params: { page: 1, size: 5 } });

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
        expect(data.content.length).toBeGreaterThan(0);
        expect(data.content.length).toBeLessThanOrEqual(5);
    });

    test('검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/page', {
            params: { page: 1, size: 10, searchField: 'userId', searchValue: ADMIN.userId },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const match = body.data.content.find((u: { userId: string }) => u.userId === ADMIN.userId);
        expect(match).toBeTruthy();
        expect(match.userId).toBe(ADMIN.userId);
    });
});

test.describe('/api/users/:userId — 단건 조회', () => {

    test('존재하는 사용자 조회 시 HTTP 200과 사용자 정보를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/users/${ADMIN.userId}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.userId).toBe(ADMIN.userId);
        expect(body.data.userName).toBe(ADMIN.userName);
    });

    test('존재하지 않는 사용자 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/nonexistent-user-zzz');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('POST /api/users — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/users', { data: validUserData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.userId).toBe(id);
            expect(body.data.userName).toBe('TestUser-' + id);
        } finally {
            await deleteUser(request, id);
        }
    });

    test('중복 사용자명으로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id1 = uniqueId();
        const id2 = uniqueId();
        await createUser(request, id1);

        try {
            const res = await request.post('/api/users', {
                data: { userId: id2, userName: 'TestUser-' + id1, password: 'Test1234!', roleId: 'ADMIN' },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteUser(request, id1);
            await deleteUser(request, id2);
        }
    });

    test('필수 항목 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/users', {
            data: { userId: uniqueId() },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/users/:userId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createUser(request, id);

        try {
            const res = await request.put(`/api/users/${id}`, {
                data: { userName: 'Updated', roleId: 'ADMIN' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.userName).toBe('Updated');
        } finally {
            await deleteUser(request, id);
        }
    });

    test('존재하지 않는 사용자 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/users/nonexistent-user-zzz', {
            data: { userName: 'Ghost', roleId: 'ADMIN' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/users/:userId — 삭제', () => {

    test('존재하는 사용자 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createUser(request, id);

        const res = await request.delete(`/api/users/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 사용자 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/users/nonexistent-user-zzz');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/users/:userId/error-count/reset — 로그인 오류 횟수 초기화', () => {

    test('존재하는 사용자 초기화 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createUser(request, id);

        try {
            const res = await request.put(`/api/users/${id}/error-count/reset`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteUser(request, id);
        }
    });
});

// ─── 중복 확인 ───────────────────────────────────────────

test.describe('/api/users/check — 중복 확인', () => {

    test('존재하는 사용자명 조회 시 true를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/check/username', {
            params: { userName: ADMIN.userName },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBe(true);
    });

    test('존재하지 않는 사용자명 조회 시 false를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/check/username', {
            params: { userName: 'zzz-no-such-user-12345' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBe(false);
    });
});

test.describe('/api/users/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/users/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

// ─── 사용자 메뉴 ─────────────────────────────────────────

test.describe('/api/user-menus — 현재 사용자 메뉴', () => {

    test('조회 시 HTTP 200과 메뉴 계층 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/user-menus');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);

        // 메뉴 항목 스키마 검증
        const first = body.data[0];
        expect(first).toHaveProperty('menuId');
        expect(first).toHaveProperty('menuName');
    });
});

test.describe('/api/user-menus/user/:userId — 사용자별 메뉴 할당', () => {

    test('조회 시 HTTP 200과 할당 목록 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/user-menus/user/${ADMIN.userId}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);
    });
});

test.describe('/api/user-menus/by-role/:roleId — 역할별 메뉴', () => {

    test('조회 시 HTTP 200과 메뉴 계층 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/user-menus/by-role/${ADMIN.roleId}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

test.describe('PUT /api/user-menus/:userId/batch — 메뉴 일괄 저장', () => {

    test('유효한 메뉴 목록으로 저장 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createUser(request, id);

        try {
            // 사용자의 현재 메뉴 조회
            const menuRes = await request.get(`/api/user-menus/user/${id}`);
            const menuBody = await menuRes.json();
            const currentMenus = menuBody.data ?? [];

            // 현재 메뉴로 일괄 저장 (변경 없이 저장 가능한지 검증)
            const menus = currentMenus.map((m: { menuId: string; authCode: string }) => ({
                menuId: m.menuId, authCode: m.authCode ?? 'R',
            }));

            const res = await request.put(`/api/user-menus/${id}/batch`, {
                data: { menus },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteUser(request, id);
        }
    });
});

// ─── 비인증 요청 ─────────────────────────────────────────

test.describe('비인증 요청 — 접근 제어', () => {

    test('비인증 상태에서 POST /api/users 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.post('/api/users', {
                data: validUserData('unauth-test'),
            });

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await ctx.dispose();
        }
    });

    test('비인증 상태에서 DELETE /api/users/x 요청 시 HTTP 401을 반환해야 한다', async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        const ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
        try {
            const res = await ctx.delete('/api/users/some-user');

            expect(res.status()).toBe(401);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await ctx.dispose();
        }
    });
});
