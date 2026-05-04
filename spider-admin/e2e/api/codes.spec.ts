/**
 * 코드 API 계약 테스트 — /api/codes
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복/유효성 실패)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - 코드 수 조회
 */

import { test, expect } from '@playwright/test';

let seq = 0;
/** 코드그룹ID: @Size(max=8) 제약 → 8자 이내 */
function genGid() { return 'G' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }
/** 코드: @Size(max=50) 제약 → 여유 있음 */
function genCid() { return 'C' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }

/** 코드 테스트에 필요한 코드그룹을 생성하고 정리한다. */
async function withCodeGroup(request: import('@playwright/test').APIRequestContext, fn: (gid: string) => Promise<void>) {
    const gid = genGid();
    const res = await request.post('/api/code-groups/with-codes', {
        data: { codeGroupId: gid, codeGroupName: 'Test' + gid, codes: [] },
    });
    // 201(생성) 또는 409(이미 존재) 모두 허용
    expect([201, 409]).toContain(res.status());
    try {
        await fn(gid);
    } finally {
        await request.delete(`/api/code-groups/${gid}/with-codes`);
    }
}

test.describe('/api/codes/page-with-group — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/codes/page-with-group', { params: { page: 1, size: 5 } });

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
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'SearchTest', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            const res = await request.get('/api/codes/page-with-group', {
                params: { page: 1, size: 10, searchField: 'code', searchValue: cd },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((c: { code: string }) => c.code === cd);
            expect(match).toBeTruthy();
        });
    });
});

test.describe('/api/codes/:codeGroupId/:code — 단건 조회', () => {

    test('존재하는 코드 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'DetailTest', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            const res = await request.get(`/api/codes/${gid}/${cd}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.code).toBe(cd);
            expect(body.data.codeName).toBe('DetailTest');
        });
    });

    test('존재하지 않는 코드 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/codes/NO_GROUP/NO_CODE');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/codes — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            const res = await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'CreateTest', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.code).toBe(cd);
        });
    });

    test('중복 코드 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            const createRes = await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'First', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });
            expect(createRes.status()).toBe(201);

            const res = await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'Duplicate', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        });
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/codes', {
            data: { codeName: 'NoGroupId' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/codes/:codeGroupId/:code — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'Before', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            const res = await request.put(`/api/codes/${gid}/${cd}`, {
                data: { codeName: 'After', codeEngname: cd + '-upd', sortOrder: 1, useYn: 'N' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.codeName).toBe('After');
        });
    });

    test('존재하지 않는 코드 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/codes/NO_GROUP/NO_CODE', {
            data: { codeName: 'X', codeEngname: 'X' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/codes/:codeGroupId/:code — 삭제', () => {

    test('존재하는 코드 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            const cd = genCid();
            await request.post('/api/codes', {
                data: { codeGroupId: gid, code: cd, codeName: 'DeleteMe', codeEngname: cd, sortOrder: 0, useYn: 'Y' },
            });

            const res = await request.delete(`/api/codes/${gid}/${cd}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        });
    });

    test('존재하지 않는 코드 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/codes/NO_GROUP/NO_CODE');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('/api/codes/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/codes/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('/api/codes/count/:codeGroupId — 코드 수 조회', () => {

    test('코드가 있는 그룹의 코드 수를 반환해야 한다', async ({ request }) => {
        await withCodeGroup(request, async (gid) => {
            await request.post('/api/codes', {
                data: { codeGroupId: gid, code: 'C1', codeName: 'One', codeEngname: 'C1', sortOrder: 0, useYn: 'Y' },
            });

            const res = await request.get(`/api/codes/count/${gid}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBeGreaterThanOrEqual(1);
        });
    });
});
