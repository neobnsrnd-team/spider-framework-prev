/**
 * 중지거래 접근허용자(AccessUser) API 계약 테스트.
 *
 * 복합 PK: (gubunType, trxId, custUserId)
 * 모든 테스트는 자체 데이터를 생성하고 정리한다.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
function genTrxId(): string {
    return 'E2ETRX' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0');
}
function genCustId(): string {
    return 'E2ECST' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0');
}

async function createAccessUser(
    request: APIRequestContext,
    trxId: string,
    custUserId: string,
    gubunType = 'T',
) {
    const res = await request.post('/api/access-users', {
        data: { gubunType, trxId, custUserId, useYn: 'Y' },
    });
    expect(res.status()).toBe(201);
}

async function deleteAccessUser(
    request: APIRequestContext,
    gubunType: string,
    trxId: string,
    custUserId: string,
) {
    await request.delete(
        `/api/access-users/${encodeURIComponent(gubunType)}/${encodeURIComponent(trxId)}/${encodeURIComponent(custUserId)}`,
    );
}

// ─── GET /api/access-users/page ──────────────────────────

test.describe('GET /api/access-users/page — 페이지네이션 조회', () => {

    test('기본 조회 시 PageResponse 구조를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/access-users/page');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('content');
        expect(body.data).toHaveProperty('totalElements');
        expect(Array.isArray(body.data.content)).toBe(true);
    });

    test('page/size 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/access-users/page?page=1&size=5');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
    });

    test('trxId 검색 조건이 올바르게 필터링되어야 한다', async ({ request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            const res = await request.get(`/api/access-users/page?trxId=${trxId}`);
            expect(res.ok()).toBeTruthy();

            const body = await res.json();
            expect(body.data.content.length).toBeGreaterThanOrEqual(1);
            expect(body.data.content.every((item: { trxId: string }) => item.trxId.includes(trxId))).toBe(true);
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('gubunType 검색 조건이 올바르게 필터링되어야 한다', async ({ request }) => {
        const res = await request.get('/api/access-users/page?gubunType=T');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(Array.isArray(body.data.content)).toBe(true);
        if (body.data.content.length > 0) {
            expect(body.data.content.every((item: { gubunType: string }) => item.gubunType === 'T')).toBe(true);
        }
    });

    test('sortBy/sortDirection 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/access-users/page?sortBy=trxId&sortDirection=DESC');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
    });
});

// ─── POST /api/access-users ──────────────────────────────

test.describe('POST /api/access-users — 생성', () => {

    test('생성 성공 시 HTTP 201과 생성된 데이터를 반환해야 한다', async ({ request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();

        try {
            const res = await request.post('/api/access-users', {
                data: { gubunType: 'T', trxId, custUserId, useYn: 'Y' },
            });
            expect(res.status()).toBe(201);

            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.trxId).toBe(trxId);
            expect(body.data.custUserId).toBe(custUserId);
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('복합 PK 중복 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            const res = await request.post('/api/access-users', {
                data: { gubunType: 'T', trxId, custUserId, useYn: 'Y' },
            });
            expect(res.status()).toBe(409);
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('필수 필드(trxId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/access-users', {
            data: { gubunType: 'T', custUserId: genCustId(), useYn: 'Y' },
        });
        expect(res.status()).toBe(400);
    });

    test('필수 필드(custUserId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/access-users', {
            data: { gubunType: 'T', trxId: genTrxId(), useYn: 'Y' },
        });
        expect(res.status()).toBe(400);
    });
});

// ─── PUT /api/access-users ───────────────────────────────

test.describe('PUT /api/access-users — 수정', () => {

    test('수정 성공 시 HTTP 200과 수정된 데이터를 반환해야 한다', async ({ request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            const res = await request.put('/api/access-users', {
                data: { gubunType: 'T', trxId, custUserId, useYn: 'N' },
            });
            expect(res.status()).toBe(200);

            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.useYn).toBe('N');
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('존재하지 않는 데이터 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/access-users', {
            data: { gubunType: 'T', trxId: 'NOTEXIST_TRX', custUserId: 'NOTEXIST_USR', useYn: 'N' },
        });
        expect(res.status()).toBe(404);
    });
});

// ─── DELETE /api/access-users/{gubunType}/{trxId}/{custUserId}

test.describe('DELETE /api/access-users/{gubunType}/{trxId}/{custUserId} — 삭제', () => {

    test('삭제 성공 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        const res = await request.delete(
            `/api/access-users/T/${encodeURIComponent(trxId)}/${encodeURIComponent(custUserId)}`,
        );
        expect(res.status()).toBe(200);

        // 삭제 후 조회 시 없어야 함
        const check = await request.get(`/api/access-users/page?trxId=${trxId}&custUserId=${custUserId}`);
        const body = await check.json();
        expect(body.data.content.length).toBe(0);
    });

    test('존재하지 않는 데이터 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/access-users/T/NOTEXIST_TRX/NOTEXIST_USR');
        expect(res.status()).toBe(404);
    });
});

// ─── 인증 검증 ───────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태에서 GET /api/access-users/page 요청 시 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/access-users/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });

    test('비인증 상태에서 POST /api/access-users 요청 시 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/access-users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ gubunType: 'T', trxId: 'X', custUserId: 'X', useYn: 'Y' }),
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });

    test('비인증 상태에서 DELETE /api/access-users/{...} 요청 시 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/access-users/T/X/X', {
            method: 'DELETE',
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
