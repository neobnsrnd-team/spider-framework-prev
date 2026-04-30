/**
 * 코드그룹 API 계약 테스트 — /api/code-groups
 *
 * 검증 범위:
 * - 전체 목록 조회
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 상세 조회 (존재/미존재)
 * - 생성 (성공/중복/유효성 실패)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 */

import { test, expect } from '@playwright/test';

let seq = 0;
/** 코드그룹ID: @Size(max=8) 제약 → 8자 이내 */
function uniqueId() { return 'G' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }

function buildCreateData(id: string, name: string) {
    return {
        codeGroupId: id,
        codeGroupName: name,
        codeGroupDesc: 'E2E 테스트용',
        codes: [],
    };
}

test.describe('/api/code-groups — 전체 목록 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/code-groups');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

test.describe('/api/code-groups/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/code-groups/page', { params: { page: 1, size: 5 } });

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
        // 병렬 워커 간 이름 충돌 방지 — id가 이미 유일하므로 이름에 포함
        const createRes = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'SearchGrp-' + id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/code-groups/page', {
                params: { page: 1, size: 10, searchField: 'codeGroupId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((g: { codeGroupId: string }) => g.codeGroupId === id);
            expect(match).toBeTruthy();
        } finally {
            await request.delete(`/api/code-groups/${id}/with-codes`);
        }
    });
});

test.describe('/api/code-groups/:id/with-codes — 상세 조회', () => {

    test('존재하는 코드그룹 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/code-groups/with-codes', {
            data: {
                ...buildCreateData(id, 'DetailGroup'),
                codes: [{ codeGroupId: id, code: 'C1', codeName: '코드1', codeEngname: 'Code1', sortOrder: 0, useYn: 'Y' }],
            },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/code-groups/${id}/with-codes`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.codeGroupId).toBe(id);
            expect(body.data.codeGroupName).toBe('DetailGroup');
            expect(Array.isArray(body.data.codes)).toBe(true);
            expect(body.data.codes.length).toBeGreaterThanOrEqual(1);
        } finally {
            await request.delete(`/api/code-groups/${id}/with-codes`);
        }
    });

    test('존재하지 않는 코드그룹 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/code-groups/NO_SUCH_GROUP/with-codes');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/code-groups/with-codes — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'CreateGroup') });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.codeGroupId).toBe(id);
        } finally {
            await request.delete(`/api/code-groups/${id}/with-codes`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'First') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'Duplicate') });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/code-groups/${id}/with-codes`);
        }
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/code-groups/with-codes', {
            data: { codeGroupDesc: 'NoId' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/code-groups/:id/with-codes — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'Before') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/code-groups/${id}/with-codes`, {
                data: { codeGroupName: 'After', codeGroupDesc: 'Updated', codes: [] },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.codeGroupName).toBe('After');
        } finally {
            await request.delete(`/api/code-groups/${id}/with-codes`);
        }
    });

    test('존재하지 않는 코드그룹 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/code-groups/NO_SUCH_GROUP/with-codes', {
            data: { codeGroupName: 'X', codes: [] },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/code-groups/:id/with-codes — 삭제', () => {

    test('존재하는 코드그룹 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/code-groups/with-codes', { data: buildCreateData(id, 'DeleteMe') });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/code-groups/${id}/with-codes`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 코드그룹 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/code-groups/NO_SUCH_GROUP/with-codes');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('/api/code-groups/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/code-groups/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
