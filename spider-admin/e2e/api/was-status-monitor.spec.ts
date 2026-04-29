import { test, expect } from '@playwright/test';

const BASE_URL = '/api/was/gateway-status';

// 시드 데이터 (e2e-seed.sql 참조)
const SEED_INSTANCE_ID = 'E2E1';
const SEED_GW_ID = 'E2E-LIS-GW';
const SEED_SYSTEM_ID = 'MON-SYS-STOP'; // STOP_YN='Y' — 소켓 연결 없이 200 반환

test.describe('GET /api/was/gateway-status/page — 페이지 조회', () => {
    test('정상 조회 시 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toMatchObject({
            content: expect.any(Array),
            currentPage: expect.any(Number),
            totalPages: expect.any(Number),
            totalElements: expect.any(Number),
            size: expect.any(Number),
        });
    });

    test('instanceId 필터를 적용하면 해당 조건으로 조회되어야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { instanceId: SEED_INSTANCE_ID, page: '1', size: '20' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.content).toBeInstanceOf(Array);
    });

    test('존재하지 않는 instanceId 필터 시 빈 목록을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`, {
            params: { instanceId: 'NOTEXIST-INSTANCE' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.content).toHaveLength(0);
        expect(body.data.totalElements).toBe(0);
    });
});

test.describe('GET /api/was/gateway-status/options — 옵션 조회', () => {
    test('200과 instances/gateways/operModes 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/options`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toMatchObject({
            instances: expect.any(Array),
            gateways: expect.any(Array),
            operModes: expect.any(Array),
        });

        const instances: unknown[] = body.data.instances;
        if (instances.length > 0) {
            expect(instances[0]).toMatchObject({
                value: expect.any(String),
                label: expect.any(String),
            });
        }
    });
});

test.describe('GET /api/was/gateway-status/export — WAS 상태 엑셀 내보내기', () => {
    test('200과 xlsx Content-Type 및 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export`);

        expect(res.status()).toBe(200);
        expect(res.headers()['content-type']).toContain(
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        );
        expect(res.headers()['content-disposition']).toContain('attachment');
    });
});

test.describe('GET /api/was/gateway-status/export-monitor — 엑셀 내보내기', () => {
    test('200과 xlsx Content-Type 및 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/export-monitor`);

        expect(res.status()).toBe(200);
        expect(res.headers()['content-type']).toContain(
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        );
        expect(res.headers()['content-disposition']).toContain('attachment');
    });
});

test.describe('POST /api/was/gateway-status/{instanceId}/{gwId}/{systemId}/test — 연결 테스트', () => {
    test('W 권한 사용자는 200과 connected 필드를 포함한 응답을 받아야 한다', async ({ request }) => {
        // STOP_YN='Y' 시스템 → 소켓 연결 없이 connected=false 반환 (200)
        const res = await request.post(
            `${BASE_URL}/${SEED_INSTANCE_ID}/${SEED_GW_ID}/${SEED_SYSTEM_ID}/test`,
        );

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toMatchObject({
            connected: expect.any(Boolean),
            message: expect.any(String),
            checkedAt: expect.any(String),
        });
        expect(body.data.connected).toBe(false);
        expect(body.data.message).toBe('정지 상태의 시스템입니다.');
    });

    test('시스템을 찾을 수 없으면 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post(
            `${BASE_URL}/${SEED_INSTANCE_ID}/${SEED_GW_ID}/NOTEXIST-SYS/test`,
        );

        expect(res.status()).toBe(404);
    });

    test('인스턴스를 찾을 수 없으면 404를 반환해야 한다', async ({ request }) => {
        const res = await request.post(
            `${BASE_URL}/NOTX/${SEED_GW_ID}/${SEED_SYSTEM_ID}/test`,
        );

        expect(res.status()).toBe(404);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {
    let unauthCtx: import('@playwright/test').APIRequestContext;

    test.beforeAll(async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        unauthCtx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
    });

    test.afterAll(async () => {
        await unauthCtx.dispose();
    });

    test('비인증 요청은 401을 반환해야 한다', async () => {
        const res = await unauthCtx.get(`${BASE_URL}/page`);

        expect(res.status()).toBe(401);
    });
});

test.describe('권한 검증 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자는 목록 조회 시 200을 받아야 한다', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/page`);

        expect(res.status()).toBe(200);
    });

    test('R 권한 사용자가 연결 테스트를 실행하면 403을 받아야 한다', async ({ request }) => {
        const res = await request.post(
            `${BASE_URL}/${SEED_INSTANCE_ID}/${SEED_GW_ID}/${SEED_SYSTEM_ID}/test`,
        );

        expect(res.status()).toBe(403);
    });
});
