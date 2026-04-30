/**
 * CMS 이미지 승인 API 계약 테스트 — /api/cms-admin/asset-requests, /api/cms-admin/asset-approvals
 *
 * 검증 범위:
 * - 승인 요청 목록 조회 (PageResponse 스키마)
 * - 승인 관리 목록 조회 (기본 PENDING 필터)
 * - 이미지 상세 조회 (모달 프리뷰용)
 * - 상태 전이: 승인 요청 / 승인 / 반려 (멱등 race → 409 InvalidStateException)
 * - 인증 검증 (비인증 → 401)
 *
 * 시드 asset (e2e-seed.sql):
 *   E2E-ASSET-WORK     — WORK
 *   E2E-ASSET-PENDING  — PENDING
 *   E2E-ASSET-APPROVED — APPROVED
 *   E2E-ASSET-REJECTED — REJECTED
 */

import { test, expect } from '@playwright/test';

const REQUESTS_URL = '/api/cms-admin/asset-requests';
const APPROVALS_URL = '/api/cms-admin/asset-approvals';

const ASSET_WORK = 'E2E-ASSET-WORK';
const ASSET_PENDING = 'E2E-ASSET-PENDING';
const ASSET_APPROVED = 'E2E-ASSET-APPROVED';

// ─── 승인 요청 목록 조회 ──────────────────────────────

test.describe('GET /api/cms-admin/asset-requests — 내 이미지 목록', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(REQUESTS_URL, { params: { page: 1, size: 10 } });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('content');
        expect(data).toHaveProperty('totalElements');
        expect(data).toHaveProperty('currentPage');
        expect(data).toHaveProperty('totalPages');
        expect(Array.isArray(data.content)).toBe(true);
    });

    test('assetState 필터가 적용되어야 한다', async ({ request }) => {
        const res = await request.get(REQUESTS_URL, { params: { page: 1, size: 100, assetState: 'APPROVED' } });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        const states: string[] = body.data.content.map((a: { assetState: string }) => a.assetState);
        if (states.length > 0) {
            expect(states.every(s => s === 'APPROVED')).toBe(true);
        }
    });
});

// ─── 승인 관리 목록 조회 ──────────────────────────────

test.describe('GET /api/cms-admin/asset-approvals — 승인 대기 목록', () => {

    test('필터 없이 조회 시 WORK를 제외한 전체(PENDING/APPROVED/REJECTED)를 반환해야 한다', async ({ request }) => {
        const res = await request.get(APPROVALS_URL, { params: { page: 1, size: 100 } });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        const states: string[] = body.data.content.map((a: { assetState: string }) => a.assetState);
        if (states.length > 0) {
            expect(states.every(s => s !== 'WORK')).toBe(true);
        }
    });

    test('assetState=REJECTED 지정 시 해당 상태만 반환해야 한다', async ({ request }) => {
        const res = await request.get(APPROVALS_URL, { params: { page: 1, size: 100, assetState: 'REJECTED' } });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        const states: string[] = body.data.content.map((a: { assetState: string }) => a.assetState);
        if (states.length > 0) {
            expect(states.every(s => s === 'REJECTED')).toBe(true);
        }
    });
});

// ─── 이미지 상세 조회 ────────────────────────────────

test.describe('GET /api/cms-admin/asset-approvals/{assetId} — 상세', () => {

    test('존재하는 assetId는 200과 상세 데이터를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${APPROVALS_URL}/${ASSET_PENDING}`);
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toHaveProperty('assetId', ASSET_PENDING);
        expect(body.data).toHaveProperty('assetName');
        expect(body.data).toHaveProperty('assetState');
    });

    test('존재하지 않는 assetId는 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${APPROVALS_URL}/NON_EXIST_ASSET_XYZ`);
        expect(res.status()).toBe(404);
    });
});

// ─── 상태 전이 ────────────────────────────────────────

test.describe('상태 전이 API', () => {

    test('이미 APPROVED인 asset에 대한 승인 요청은 409를 반환해야 한다', async ({ request }) => {
        // APPROVED 상태는 WORK → PENDING 전이를 허용하지 않음
        const res = await request.post(`${REQUESTS_URL}/${ASSET_APPROVED}/request`);
        expect(res.status()).toBe(409);
    });

    test('존재하지 않는 asset 승인 요청은 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post(`${REQUESTS_URL}/NON_EXIST_ASSET_XYZ/request`);
        expect(res.status()).toBe(404);
    });

    test('WORK 상태가 아닌 asset 승인은 409를 반환해야 한다', async ({ request }) => {
        // WORK 상태 asset을 바로 승인하려 하면 PENDING이 아니므로 409
        const res = await request.post(`${APPROVALS_URL}/${ASSET_WORK}/approve`);
        expect(res.status()).toBe(409);
    });

    test('PENDING 상태가 아닌 asset 반려는 409를 반환해야 한다', async ({ request }) => {
        const res = await request.post(`${APPROVALS_URL}/${ASSET_APPROVED}/reject`, {
            data: { rejectedReason: '테스트' },
        });
        expect(res.status()).toBe(409);
    });
});

// ─── 인증 검증 ────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {
    let ctx: import('@playwright/test').APIRequestContext;

    test.beforeAll(async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
    });

    test.afterAll(async () => {
        await ctx.dispose();
    });

    test('인증 없이 승인 요청 목록 조회 시 401', async () => {
        const res = await ctx.get(REQUESTS_URL);
        expect(res.status()).toBe(401);
    });

    test('인증 없이 승인 관리 목록 조회 시 401', async () => {
        const res = await ctx.get(APPROVALS_URL);
        expect(res.status()).toBe(401);
    });
});
