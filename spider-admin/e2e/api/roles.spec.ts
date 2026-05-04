/**
 * 역할 API 계약 테스트 — /api/roles, /api/role-menus
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 전체 목록 조회
 * - 엑셀 내보내기
 * - 일괄 처리 — 생성 (성공 / 중복 roleName → 409 / 필수 필드 누락 → 400)
 * - 일괄 처리 — 수정 (성공 / 미존재 roleId → 404 / 중복 roleName → 409 / 필수 필드 누락 → 400)
 * - 일괄 처리 — 삭제 (성공 / 미존재 roleId → 404)
 * - 역할별 메뉴 권한 조회 (성공 / 미존재 roleId → 404)
 * - 역할별 메뉴 권한 수정 (성공 / 미존재 roleId → 404 / 필수 필드 누락 → 400)
 * - 비인증 요청 → 302
 * - 인가 — ROLE:W 없는 사용자 batch → 403
 *
 * 주의:
 * - CRUD 엔드포인트는 POST /api/roles/batch (단건 POST/PUT/DELETE 없음)
 * - 중복 체크 기준은 roleName (roleId가 아님)
 * - batch API 성공 응답은 HTTP 200
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

let seq = 0;
// roleId 최대 10자 제약: 'r'(1) + base36 timestamp 마지막 8자(8) + seq 1자리(1) = 10자
function uniqueId() { return 'r' + Date.now().toString(36).slice(-8) + (seq++ % 10); }

function buildBatchCreate(roleId: string, roleName: string) {
    return {
        newRoles: [{ roleId, roleName, useYn: 'Y', roleDesc: 'E2E 테스트', ranking: '99' }],
        updatedRoles: [],
        deletedRoleIds: [],
    };
}

async function createRole(request: APIRequestContext, roleId: string, roleName: string) {
    const res = await request.post('/api/roles/batch', { data: buildBatchCreate(roleId, roleName) });
    expect(res.status()).toBe(200);
}

async function deleteRole(request: APIRequestContext, roleId: string) {
    const res = await request.post('/api/roles/batch', {
        data: { newRoles: [], updatedRoles: [], deletedRoleIds: [roleId] },
    });
    if (!res.ok() && res.status() !== 404) {
        console.warn(`deleteRole failed: ${roleId} status=${res.status()}`);
    }
}

// ─── 페이지네이션 조회 ────────────────────────────────────────

test.describe('/api/roles/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/roles/page', { params: { page: 1, size: 5 } });

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

    test('roleId 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'IdFilter' + id);

        try {
            const res = await request.get('/api/roles/page', {
                params: { page: 1, size: 10, roleId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((r: { roleId: string }) => r.roleId === id);
            expect(match).toBeTruthy();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('roleName 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const uniqueName = 'NmFilter' + id;
        await createRole(request, id, uniqueName);

        try {
            const res = await request.get('/api/roles/page', {
                params: { page: 1, size: 10, roleName: uniqueName },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((r: { roleName: string }) => r.roleName === uniqueName);
            expect(match).toBeTruthy();
        } finally {
            await deleteRole(request, id);
        }
    });
});

// ─── 전체 목록 조회 ──────────────────────────────────────────

test.describe('GET /api/roles — 전체 목록 조회', () => {

    test('조회 시 HTTP 200과 역할 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/roles');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 엑셀 내보내기 ───────────────────────────────────────────

test.describe('GET /api/roles/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/roles/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

// ─── 일괄 처리 — 생성 ────────────────────────────────────────

test.describe('POST /api/roles/batch — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/roles/batch', {
                data: buildBatchCreate(id, 'Create' + id),
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteRole(request, id);
        }
    });

    test('중복 roleName으로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id1 = uniqueId();
        const id2 = uniqueId();
        const sameName = 'DupName' + id1;
        await createRole(request, id1, sameName);

        try {
            // 다른 roleId, 같은 roleName → 409
            const res = await request.post('/api/roles/batch', {
                data: buildBatchCreate(id2, sameName),
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteRole(request, id1);
        }
    });

    test('필수 필드(roleName) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/roles/batch', {
            data: {
                newRoles: [{ roleId: 'e2e-val-only' /* roleName 누락 */ }],
                updatedRoles: [],
                deletedRoleIds: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 일괄 처리 — 수정 ────────────────────────────────────────

test.describe('POST /api/roles/batch — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'Before' + id);

        try {
            const res = await request.post('/api/roles/batch', {
                data: {
                    newRoles: [],
                    updatedRoles: [{
                        roleId: id,
                        roleName: 'After' + id,
                        useYn: 'N',
                        roleDesc: '수정된 설명',
                        ranking: '50',
                    }],
                    deletedRoleIds: [],
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            // 수정 내용 반영 확인
            const pageRes = await request.get('/api/roles/page', {
                params: { page: 1, size: 10, roleId: id },
            });
            const pageBody = await pageRes.json();
            const updated = pageBody.data.content.find((r: { roleId: string }) => r.roleId === id);
            expect(updated?.roleName).toBe('After' + id);
            expect(updated?.useYn).toBe('N');
        } finally {
            await deleteRole(request, id);
        }
    });

    test('존재하지 않는 roleId 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/roles/batch', {
            data: {
                newRoles: [],
                updatedRoles: [{ roleId: 'no-exist-z', roleName: 'AnyName', useYn: 'Y', roleDesc: '', ranking: '99' }],
                deletedRoleIds: [],
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('중복 roleName으로 수정 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id1 = uniqueId();
        const id2 = uniqueId();
        const nameForId1 = 'UpdOrig' + id1;
        await createRole(request, id1, nameForId1);
        await createRole(request, id2, 'UpdOther' + id2);

        try {
            // id2의 roleName을 id1과 동일하게 변경 → 409
            const res = await request.post('/api/roles/batch', {
                data: {
                    newRoles: [],
                    updatedRoles: [{ roleId: id2, roleName: nameForId1, useYn: 'Y', roleDesc: '', ranking: '99' }],
                    deletedRoleIds: [],
                },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteRole(request, id1);
            await deleteRole(request, id2);
        }
    });

    test('필수 필드(roleName) 누락으로 수정 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'ValBefore' + id);

        try {
            const res = await request.post('/api/roles/batch', {
                data: {
                    newRoles: [],
                    updatedRoles: [{ roleId: id /* roleName 누락 */ }],
                    deletedRoleIds: [],
                },
            });

            expect(res.status()).toBe(400);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteRole(request, id);
        }
    });
});

// ─── 일괄 처리 — 삭제 ────────────────────────────────────────

test.describe('POST /api/roles/batch — 삭제', () => {

    test('존재하는 역할 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'DelMe' + id);

        const res = await request.post('/api/roles/batch', {
            data: { newRoles: [], updatedRoles: [], deletedRoleIds: [id] },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        // 삭제 테스트이므로 cleanup 불필요
    });

    test('존재하지 않는 역할 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/roles/batch', {
            data: { newRoles: [], updatedRoles: [], deletedRoleIds: ['NO_SUCH_ROLE_E2E_ZZZ'] },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 역할별 메뉴 권한 조회 ───────────────────────────────────

test.describe('GET /api/role-menus/:roleId — 역할별 메뉴 권한 조회', () => {

    test('존재하는 역할의 메뉴 권한 조회 시 HTTP 200과 스키마를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'MenuPerm' + id);

        try {
            const res = await request.get(`/api/role-menus/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toHaveProperty('roleId');
            expect(body.data).toHaveProperty('roleName');
            expect(body.data).toHaveProperty('allMenusWithPermissions');
            expect(Array.isArray(body.data.allMenusWithPermissions)).toBe(true);
        } finally {
            await deleteRole(request, id);
        }
    });

    test('존재하지 않는 역할의 메뉴 권한 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/role-menus/NO_SUCH_ROLE_E2E_ZZZ');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── 역할별 메뉴 권한 수정 ───────────────────────────────────

test.describe('POST /api/role-menus/:roleId — 역할별 메뉴 권한 수정', () => {

    test('유효한 데이터로 권한 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'MenuUpd' + id);

        try {
            // 빈 목록으로 권한 초기화 (성공 케이스)
            const res = await request.post(`/api/role-menus/${id}`, {
                data: { menuPermissions: [] },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteRole(request, id);
        }
    });

    test('존재하지 않는 roleId로 권한 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/role-menus/NO_SUCH_ROLE_E2E_ZZZ', {
            data: { menuPermissions: [] },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('필수 필드(menuPermissions) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createRole(request, id, 'MenuVal' + id);

        try {
            // menuPermissions @NotNull 위반 → 400
            const res = await request.post(`/api/role-menus/${id}`, {
                data: {},
            });

            expect(res.status()).toBe(400);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteRole(request, id);
        }
    });
});

// ─── 비인증 요청 ─────────────────────────────────────────────

test.describe('인증 — 비인증 요청', () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test('인증 없이 조회 요청 시 로그인 페이지로 리다이렉트되어야 한다', async ({ request }) => {
        const res = await request.get('/api/roles/page', {
            params: { page: 1, size: 5 },
            maxRedirects: 0,
        });

        // Spring Security: form login redirect → 302, or AJAX 401
        expect([302, 401]).toContain(res.status());
    });
});

// ─── 인가 — ROLE:W 없는 사용자 ───────────────────────────────

test.describe('인가 — ROLE:W 없는 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('ROLE:W 없는 사용자의 batch 요청은 HTTP 403을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const res = await request.post('/api/roles/batch', {
            data: buildBatchCreate(id, 'Forbidden' + id),
        });

        expect(res.status()).toBe(403);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});
