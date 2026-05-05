/**
 * 모니터(현황판) API 계약 테스트 — /api/monitors
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - 집계 (전체/활성)
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-m-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

test.describe('/api/monitors/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/monitors/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/monitors', {
            data: { monitorId: id, monitorName: 'SearchTest', refreshTerm: '5', useYn: 'Y' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/monitors/page', {
                params: { page: 1, size: 10, searchField: 'monitorId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((m: { monitorId: string }) => m.monitorId === id);
            expect(match).toBeTruthy();
            expect(match.monitorId).toBe(id);
        } finally {
            await request.delete(`/api/monitors/${id}`);
        }
    });
});

test.describe('/api/monitors/:monitorId — 단건 조회', () => {

    test('존재하는 모니터 조회 시 HTTP 200과 모니터 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/monitors', {
            data: { monitorId: id, monitorName: 'DetailTest', refreshTerm: '10', useYn: 'Y' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/monitors/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.monitorId).toBe(id);
            expect(body.data.monitorName).toBe('DetailTest');
            expect(body.data.refreshTerm).toBe('10');
        } finally {
            await request.delete(`/api/monitors/${id}`);
        }
    });

    test('존재하지 않는 모니터 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/monitors/no-such-monitor');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/monitors — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/monitors', {
                data: { monitorId: id, monitorName: 'CreateTest', refreshTerm: '5', useYn: 'Y' },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.monitorId).toBe(id);
        } finally {
            await request.delete(`/api/monitors/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/monitors', {
            data: { monitorId: id, monitorName: 'First', refreshTerm: '5', useYn: 'Y' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/monitors', {
                data: { monitorId: id, monitorName: 'Duplicate', refreshTerm: '5', useYn: 'Y' },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/monitors/${id}`);
        }
    });
});

test.describe('PUT /api/monitors/:monitorId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/monitors', {
            data: { monitorId: id, monitorName: 'Before', refreshTerm: '5', useYn: 'Y' },
        });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/monitors/${id}`, {
                data: { monitorId: id, monitorName: 'After', refreshTerm: '10', useYn: 'Y' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.monitorName).toBe('After');
            expect(body.data.refreshTerm).toBe('10');
        } finally {
            await request.delete(`/api/monitors/${id}`);
        }
    });

    test('존재하지 않는 모니터 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/monitors/no-such-monitor', {
            data: { monitorId: 'no-such-monitor', monitorName: 'X', refreshTerm: '5', useYn: 'Y' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/monitors/:monitorId — 삭제', () => {

    test('존재하는 모니터 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/monitors', {
            data: { monitorId: id, monitorName: 'DeleteMe', refreshTerm: '5', useYn: 'Y' },
        });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/monitors/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 모니터 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/monitors/no-such-monitor');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('/api/monitors/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/monitors/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('/api/monitors/count — 집계', () => {

    test('전체 모니터 수 조회 시 HTTP 200과 숫자를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/monitors/count/total');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(typeof body.data).toBe('number');
    });

    test('활성 모니터 수 조회 시 HTTP 200과 숫자를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/monitors/count/active');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(typeof body.data).toBe('number');
    });
});
