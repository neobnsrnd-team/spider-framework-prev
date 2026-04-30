/**
 * 거래(TRX) API 계약 테스트 — /api/trx
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 등록 (성공/중복)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - 인증 검증
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-trx-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function trxData(id: string, overrides: Record<string, unknown> = {}) {
    return {
        trxId: id,
        trxType: '1',
        retryTrxYn: 'N',
        maxRetryCount: 0,
        ...overrides,
    };
}

test.describe('GET /api/trx/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trx/page', { params: { page: 1, size: 10 } });

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
        const createRes = await request.post('/api/trx', { data: trxData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/trx/page', {
                params: { page: 1, size: 10, searchField: 'trxId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((t: { trxId: string }) => t.trxId === id);
            expect(match).toBeTruthy();
            expect(match.trxId).toBe(id);
        } finally {
            await request.delete(`/api/trx/${id}`);
        }
    });
});

test.describe('GET /api/trx/{trxId} — 단건 조회', () => {

    test('존재하는 거래 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/trx', { data: trxData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/trx/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.trxId).toBe(id);
            expect(body.data.trxType).toBe('1');
        } finally {
            await request.delete(`/api/trx/${id}`);
        }
    });

    test('존재하지 않는 거래 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trx/no-such-trx-id');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/trx — 등록', () => {

    test('유효한 데이터로 등록 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/trx', { data: trxData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.trxId).toBe(id);
        } finally {
            await request.delete(`/api/trx/${id}`);
        }
    });

    test('중복 ID로 등록 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/trx', { data: trxData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/trx', { data: trxData(id) });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/trx/${id}`);
        }
    });
});

test.describe('PUT /api/trx/{trxId} — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200과 수정된 내용을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/trx', { data: trxData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/trx/${id}`, {
                data: {
                    trxName: '수정된거래명',
                    trxType: '2',
                    retryTrxYn: 'N',
                    maxRetryCount: 3,
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.trxId).toBe(id);
            expect(body.data.trxName).toBe('수정된거래명');
        } finally {
            await request.delete(`/api/trx/${id}`);
        }
    });

    test('존재하지 않는 거래 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/trx/no-such-trx-id', {
            data: { trxName: 'X', trxType: '1', retryTrxYn: 'N', maxRetryCount: 0 },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/trx/{trxId} — 삭제', () => {

    test('존재하는 거래 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/trx', { data: trxData(id) });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/trx/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 거래 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/trx/no-such-trx-id');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('GET /api/trx/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trx/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 사용자의 요청은 리다이렉트 또는 401을 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/trx/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });
});
