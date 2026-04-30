/**
 * 거래중지(TrxStop) API 계약 테스트 — /api/trx-stop
 *
 * 검증 범위:
 * - 페이지네이션 조회
 * - 엑셀 내보내기
 * - 거래중지 일괄 변경 (성공/미존재)
 * - 운영모드 일괄 변경 (성공/유효하지 않은 코드)
 * - 인증 검증
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

const PREFIX = 'e2e-ts-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

async function createTrx(request: APIRequestContext, id: string) {
    const res = await request.post('/api/trx', {
        data: { trxId: id, trxType: '1', retryTrxYn: 'N', maxRetryCount: 0 },
    });
    expect(res.status()).toBe(201);
}

async function deleteTrx(request: APIRequestContext, id: string) {
    await request.delete(`/api/trx/${id}`);
}

test.describe('GET /api/trx-stop/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trx-stop/page', { params: { page: 1, size: 10 } });

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
        await createTrx(request, id);

        try {
            const res = await request.get('/api/trx-stop/page', {
                params: { page: 1, size: 10, searchField: 'trxId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((t: { trxId: string }) => t.trxId === id);
            expect(match).toBeTruthy();
            expect(match.trxId).toBe(id);
        } finally {
            await deleteTrx(request, id);
        }
    });
});

test.describe('GET /api/trx-stop/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/trx-stop/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('PUT /api/trx-stop/batch — 거래중지 일괄 변경', () => {

    test('유효한 요청으로 거래를 중지하면 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createTrx(request, id);

        try {
            const res = await request.put('/api/trx-stop/batch', {
                data: { trxIds: [id], trxStopYn: 'Y', trxStopReason: 'E2E 테스트 중지' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            // 상태 복원 후 삭제
            await request.put('/api/trx-stop/batch', {
                data: { trxIds: [id], trxStopYn: 'N' },
            });
            await deleteTrx(request, id);
        }
    });

    test('이미 동일한 상태이면 변경 없이 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createTrx(request, id);

        try {
            // 이미 정상(N) 상태인 거래를 다시 정상으로 변경 요청
            const res = await request.put('/api/trx-stop/batch', {
                data: { trxIds: [id], trxStopYn: 'N' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deleteTrx(request, id);
        }
    });

    test('존재하지 않는 거래ID가 포함된 경우 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/trx-stop/batch', {
            data: { trxIds: ['NO-SUCH-TRX-99999'], trxStopYn: 'Y', trxStopReason: 'test' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/trx-stop/batch-oper-mode — 운영모드 일괄 변경', () => {

    test('유효한 코드(D)로 요청 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        try {
            const res = await request.put('/api/trx-stop/batch-oper-mode', {
                data: { operModeType: 'D' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            // 전체 초기화 (null로 복원)
            await request.put('/api/trx-stop/batch-oper-mode', {
                data: { operModeType: null },
            });
        }
    });

    test('null로 요청 시 전체 초기화를 수행하고 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/trx-stop/batch-oper-mode', {
            data: { operModeType: null },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('유효하지 않은 코드로 요청 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/trx-stop/batch-oper-mode', {
            data: { operModeType: 'X' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 사용자의 요청은 리다이렉트 또는 401을 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/trx-stop/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });
});
