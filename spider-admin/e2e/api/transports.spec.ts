/**
 * 기관통신 Gateway 맵핑(Transport) API 계약 테스트.
 *
 * 모든 테스트는 자체 데이터를 생성하고 정리한다.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

async function createTransport(
    request: APIRequestContext,
    orgId: string,
    trxType: string,
    ioType: string,
    reqResType: string,
    gwId: string,
) {
    const res = await request.post(`/api/transports/batch?orgId=${orgId}`, {
        data: {
            upserts: [{ orgId, trxType, ioType, reqResType, gwId }],
            deletes: [],
        },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteTransport(
    request: APIRequestContext,
    orgId: string,
    trxType: string,
    ioType: string,
    reqResType: string,
) {
    await request.post(`/api/transports/batch?orgId=${orgId}`, {
        data: {
            upserts: [],
            deletes: [{ orgId, trxType, ioType, reqResType }],
        },
    });
}

// ─── GET /api/transports ────────────────────────────────

test.describe('GET /api/transports — 페이지네이션 조회', () => {

    test('기본 조회 시 PageResponse 구조를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/transports');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('content');
        expect(body.data).toHaveProperty('totalElements');
        expect(Array.isArray(body.data.content)).toBe(true);
    });

    test('page/size 파라미터가 정상 동작해야 한다', async ({ request }) => {
        const res = await request.get('/api/transports?page=1&size=5');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeLessThanOrEqual(5);
    });

    test('orgId로 필터링되어야 한다', async ({ request }) => {
        const res = await request.get('/api/transports?orgId=E2EORG01');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThan(0);
        body.data.content.forEach((item: { orgId: string }) => {
            expect(item.orgId).toBe('E2EORG01');
        });
    });

    test('ioType으로 필터링되어야 한다', async ({ request }) => {
        const res = await request.get('/api/transports?ioType=O');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        body.data.content.forEach((item: { ioType: string }) => {
            expect(item.ioType).toBe('O');
        });
    });

    test('reqResType으로 필터링되어야 한다', async ({ request }) => {
        const res = await request.get('/api/transports?reqResType=Q');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        body.data.content.forEach((item: { reqResType: string }) => {
            expect(item.reqResType).toBe('Q');
        });
    });

    test('sortBy/sortDirection으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get('/api/transports?sortBy=orgId&sortDirection=DESC');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.data.content.length).toBeGreaterThan(0);
    });
});

// ─── POST /api/transports/batch ─────────────────────────

test.describe('POST /api/transports/batch — 생성/수정/삭제', () => {

    test('새 맵핑을 생성해야 한다', async ({ request }) => {
        // trxType은 FWK_TRANSPORT에 이미 존재하는 값만 유효 (isValidTrxType 체크)
        const orgId = 'E2EORG02';
        const trxType = '3';
        const ioType = 'O';
        const reqResType = 'Q';

        try {
            await createTransport(request, orgId, trxType, ioType, reqResType, 'E2E-NEW-GW');

            const check = await request.get(`/api/transports?orgId=${orgId}&size=100`);
            const body = await check.json();
            const found = body.data.content.find(
                (r: { trxType: string; ioType: string; reqResType: string }) =>
                    r.trxType === trxType && r.ioType === ioType && r.reqResType === reqResType,
            );
            expect(found).toBeTruthy();
            expect(found.gwId).toBe('E2E-NEW-GW');
        } finally {
            await deleteTransport(request, orgId, trxType, ioType, reqResType);
        }
    });

    test('기존 맵핑의 gwId를 수정해야 한다', async ({ request }) => {
        const orgId = 'E2EORG02';
        const trxType = '4';
        const ioType = 'O';
        const reqResType = 'Q';

        await createTransport(request, orgId, trxType, ioType, reqResType, 'GW-BEFORE');

        try {
            await createTransport(request, orgId, trxType, ioType, reqResType, 'GW-AFTER');

            const check = await request.get(`/api/transports?orgId=${orgId}&size=100`);
            const body = await check.json();
            const found = body.data.content.find(
                (r: { trxType: string; ioType: string; reqResType: string }) =>
                    r.trxType === trxType && r.ioType === ioType && r.reqResType === reqResType,
            );
            expect(found.gwId).toBe('GW-AFTER');
        } finally {
            await deleteTransport(request, orgId, trxType, ioType, reqResType);
        }
    });

    test('맵핑을 삭제해야 한다', async ({ request }) => {
        const orgId = 'E2EORG02';
        const trxType = '3';
        const ioType = 'I';
        const reqResType = 'Q';

        await createTransport(request, orgId, trxType, ioType, reqResType, 'GW-DELETE');

        const res = await request.post(`/api/transports/batch?orgId=${orgId}`, {
            data: {
                upserts: [],
                deletes: [{ orgId, trxType, ioType, reqResType }],
            },
        });
        expect(res.ok()).toBeTruthy();

        const check = await request.get(`/api/transports?orgId=${orgId}&size=100`);
        const body = await check.json();
        const found = body.data.content.find(
            (r: { trxType: string; ioType: string; reqResType: string }) =>
                r.trxType === trxType && r.ioType === ioType && r.reqResType === reqResType,
        );
        expect(found).toBeFalsy();
    });
});

// ─── GET /api/transports/export ─────────────────────────

test.describe('GET /api/transports/export — 엑셀 다운로드', () => {

    test('xlsx 파일을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/transports/export');
        expect(res.ok()).toBeTruthy();

        const contentType = res.headers()['content-type'] || '';
        expect(contentType).toContain('spreadsheet');
    });
});

// ─── GET /api/transports/options/trx-types ──────────────

test.describe('GET /api/transports/options/trx-types — 거래유형 옵션', () => {

    test('거래유형 옵션 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/transports/options/trx-types');
        expect(res.ok()).toBeTruthy();

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

// ─── 인증 검증 ──────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 요청은 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/transports', {
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
