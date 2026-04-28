/**
 * 배치 APP API 계약 테스트 — /api/batch/apps, /api/batch/exec
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 엑셀 내보내기
 * - ID 중복 체크
 * - 배치 수동 실행 (성공/필수값 누락)
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-ba-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }
let instanceSeq = 0;
function uniqueInstanceId() {
    return 'T' + ((Date.now() + instanceSeq++) % 46656).toString(36).padStart(3, '0').toUpperCase();
}

function buildCreateData(id: string, name: string) {
    return {
        batchAppId: id,
        batchAppName: name,
        batchAppFileName: id + '.jar',
        batchCycle: 'D',
        retryableYn: 'Y',
        perWasYn: 'N',
        importantType: '1',
    };
}

function buildWasInstanceData(id: string, name: string) {
    return {
        instanceId: id,
        instanceName: name,
        instanceDesc: 'E2E batch exec test instance',
        wasConfigId: 'E2E',
        ip: '127.0.0.1',
        port: '8080',
        instanceType: '1',
        operModeType: 'D',
    };
}

test.describe('/api/batch/apps/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/apps/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'SearchTest') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/batch/apps/page', {
                params: { page: 1, size: 10, searchField: 'batchAppId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((b: { batchAppId: string }) => b.batchAppId === id);
            expect(match).toBeTruthy();
            expect(match.batchAppId).toBe(id);
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });
});

test.describe('/api/batch/apps/:batchAppId — 단건 조회', () => {

    test('존재하는 배치 APP 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'DetailTest') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/batch/apps/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.batchApp.batchAppId).toBe(id);
            expect(body.data.batchApp.batchAppName).toBe('DetailTest');
            expect(body.data).toHaveProperty('allInstances');
            expect(body.data).toHaveProperty('assignedInstances');
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });

    test('존재하지 않는 배치 APP 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/apps/no-such-batch-app');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/batch/apps — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/batch/apps', { data: buildCreateData(id, 'CreateTest') });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.batchAppId).toBe(id);
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'First') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/batch/apps', { data: buildCreateData(id, 'Duplicate') });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/batch/apps', {
            data: { batchAppName: 'NoId' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PUT /api/batch/apps/:batchAppId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'Before') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/batch/apps/${id}`, {
                data: {
                    batchAppName: 'After',
                    batchAppFileName: id + '-updated.jar',
                    batchCycle: 'M',
                    retryableYn: 'N',
                    perWasYn: 'Y',
                    importantType: '2',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.batchAppName).toBe('After');
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });

    test('존재하지 않는 배치 APP 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/batch/apps/no-such-batch-app', {
            data: {
                batchAppName: 'X',
                batchAppFileName: 'x.jar',
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/batch/apps/:batchAppId — 삭제', () => {

    test('존재하는 배치 APP 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'DeleteMe') });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/batch/apps/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 배치 APP 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/batch/apps/no-such-batch-app');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('/api/batch/apps/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/apps/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('/api/batch/apps/check/id — ID 중복 체크', () => {

    test('존재하는 ID 조회 시 true를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/batch/apps', { data: buildCreateData(id, 'CheckTest') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/batch/apps/check/id', { params: { batchAppId: id } });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBe(true);
        } finally {
            await request.delete(`/api/batch/apps/${id}`);
        }
    });

    test('존재하지 않는 ID 조회 시 false를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/batch/apps/check/id', { params: { batchAppId: 'no-such-id' } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBe(false);
    });
});

// ─── 배치 수동 실행 ────────────────────────────────────────

/**
 * 배치 APP 생성 + 시드 WAS 인스턴스(E2E1) 할당 헬퍼.
 * E2E1 인스턴스는 e2e-seed.sql에서 사전 생성됨.
 */
async function createBatchAppWithInstance(
    request: import('@playwright/test').APIRequestContext,
    batchAppId: string,
    instanceId: string,
) {
    const instanceRes = await request.post('/api/was/instance', {
        data: buildWasInstanceData(instanceId, 'ExecTestInstance'),
    });
    expect(instanceRes.status(), await instanceRes.text()).toBe(201);

    const createRes = await request.post('/api/batch/apps', { data: buildCreateData(batchAppId, 'ExecTest') });
    expect(createRes.status()).toBe(201);

    const assignRes = await request.post(`/api/batch/apps/${batchAppId}/was/instance`, {
        params: { instanceId },
    });
    expect(assignRes.status(), await assignRes.text()).toBe(201);
}

async function cleanupBatchApp(request: import('@playwright/test').APIRequestContext, batchAppId: string) {
    await request.delete(`/api/batch/apps/${batchAppId}`);
}

async function cleanupWasInstance(request: import('@playwright/test').APIRequestContext, instanceId: string) {
    await request.delete(`/api/was/instance/${instanceId}`);
}

test.describe('POST /api/batch/exec — 배치 수동 실행', () => {

    test('유효한 데이터로 실행 시 실행 이력을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const instanceId = uniqueInstanceId();
        await createBatchAppWithInstance(request, id, instanceId);

        try {
            const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
            const res = await request.post('/api/batch/exec', {
                data: {
                    batchAppId: id,
                    batchDate: today,
                    instanceIds: [instanceId],
                },
            });

            // WAS TCP 연결 성공 시 201, 실패(CI 환경 등 WAS 미기동) 시 200
            expect([200, 201]).toContain(res.status());
            const body = await res.json();
            expect(typeof body.success).toBe('boolean');
            expect(Array.isArray(body.data)).toBe(true);
            expect(body.data.length).toBe(1);
            expect(body.data[0].batchAppId).toBe(id);
            expect(body.data[0].instanceId).toBe(instanceId);
            expect(body.data[0].batchDate).toBe(today);
            expect(body.data[0]).toHaveProperty('resRtCode');
        } finally {
            await cleanupBatchApp(request, id);
            await cleanupWasInstance(request, instanceId);
        }
    });

    test('파라미터를 포함하여 실행 시 실행 이력을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const instanceId = uniqueInstanceId();
        await createBatchAppWithInstance(request, id, instanceId);

        try {
            const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
            const res = await request.post('/api/batch/exec', {
                data: {
                    batchAppId: id,
                    parameters: 'key=value;date=20260101',
                    batchDate: today,
                    instanceIds: [instanceId],
                },
            });

            // WAS TCP 연결 성공 시 201, 실패(CI 환경 등 WAS 미기동) 시 200
            expect([200, 201]).toContain(res.status());
            const body = await res.json();
            expect(typeof body.success).toBe('boolean');
            expect(body.data.length).toBe(1);
        } finally {
            await cleanupBatchApp(request, id);
            await cleanupWasInstance(request, instanceId);
        }
    });

    test('batchAppId 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/batch/exec', {
            data: {
                batchDate: '20260312',
                instanceIds: ['E2E1'],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('batchDate 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/batch/exec', {
            data: {
                batchAppId: 'any-id',
                instanceIds: ['E2E1'],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('instanceIds 빈 배열 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/batch/exec', {
            data: {
                batchAppId: 'any-id',
                batchDate: '20260312',
                instanceIds: [],
            },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});
