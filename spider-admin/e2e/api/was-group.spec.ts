/**
 * WAS 그룹 API 계약 테스트 — /api/was/group
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-wg-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

test.describe('GET /api/was/group/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/group/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/was/group', {
            data: { wasGroupId: id, wasGroupName: 'SearchTest', wasGroupDesc: 'desc' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/was/group/page', {
                params: { page: 1, size: 10, wasGroupId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((g: { wasGroupId: string }) => g.wasGroupId === id);
            expect(match).toBeTruthy();
            expect(match.wasGroupId).toBe(id);
        } finally {
            await request.delete(`/api/was/group/${id}`);
        }
    });
});

test.describe('GET /api/was/group/:wasGroupId — 단건 조회', () => {

    test('존재하는 그룹 조회 시 HTTP 200과 그룹 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/group', {
            data: { wasGroupId: id, wasGroupName: 'DetailTest', wasGroupDesc: 'detail desc' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/was/group/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.wasGroupId).toBe(id);
            expect(body.data.wasGroupName).toBe('DetailTest');
        } finally {
            await request.delete(`/api/was/group/${id}`);
        }
    });

    test('존재하지 않는 그룹 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/group/no-such-group');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/was/group — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/was/group', {
                data: { wasGroupId: id, wasGroupName: 'CreateTest', wasGroupDesc: 'create desc' },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.wasGroupId).toBe(id);
        } finally {
            await request.delete(`/api/was/group/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/group', {
            data: { wasGroupId: id, wasGroupName: 'First', wasGroupDesc: 'first' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/was/group', {
                data: { wasGroupId: id, wasGroupName: 'Duplicate', wasGroupDesc: 'dup' },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/was/group/${id}`);
        }
    });
});

test.describe('PUT /api/was/group/:wasGroupId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/group', {
            data: { wasGroupId: id, wasGroupName: 'Before', wasGroupDesc: 'before' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/was/group/${id}`, {
                data: { wasGroupId: id, wasGroupName: 'After', wasGroupDesc: 'after' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.wasGroupName).toBe('After');
        } finally {
            await request.delete(`/api/was/group/${id}`);
        }
    });

    test('존재하지 않는 그룹 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/was/group/no-such-group', {
            data: { wasGroupId: 'no-such-group', wasGroupName: 'X', wasGroupDesc: 'x' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/was/group/:wasGroupId — 삭제', () => {

    test('존재하는 그룹 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/group', {
            data: { wasGroupId: id, wasGroupName: 'DeleteMe', wasGroupDesc: 'delete' },
        });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/was/group/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 그룹 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/was/group/no-such-group');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('GET /api/was/group/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/group/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
