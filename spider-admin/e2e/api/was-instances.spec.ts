/**
 * WAS 인스턴스 API 계약 테스트 — /api/was/instance
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

let seq = 0;
const base = Date.now() % 900;
function uniqueId() {
    // instanceId는 최대 4자 — 타임스탬프 기반으로 실행간 충돌 방지
    return 'E' + String(base + seq++).toString(36).slice(-3).toUpperCase();
}

function buildCreateData(id: string, name: string) {
    return {
        instanceId: id,
        instanceName: name,
        instanceDesc: 'E2E 테스트용',
        ip: '10.0.0.1',
        port: '8080',
        instanceType: '1',
        operModeType: 'D',
    };
}

test.describe('/api/was/instance/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/instance/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/was/instance', { data: buildCreateData(id, 'SearchTest') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/was/instance/page', {
                params: { page: 1, size: 10, instanceName: 'SearchTest' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((i: { instanceId: string }) => i.instanceId === id);
            expect(match).toBeTruthy();
            expect(match.instanceId).toBe(id);
        } finally {
            await request.delete(`/api/was/instance/${id}`);
        }
    });
});

test.describe('/api/was/instance/:instanceId — 단건 조회', () => {

    test('존재하는 인스턴스 조회 시 HTTP 200과 인스턴스 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/instance', { data: buildCreateData(id, 'DetailTest') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/was/instance/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.instanceId).toBe(id);
            expect(body.data.instanceName).toBe('DetailTest');
        } finally {
            await request.delete(`/api/was/instance/${id}`);
        }
    });

    test('존재하지 않는 인스턴스 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/instance/ZZZZ');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/was/instance — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/was/instance', { data: buildCreateData(id, 'CreateTest') });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.instanceId).toBe(id);
        } finally {
            await request.delete(`/api/was/instance/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/instance', { data: buildCreateData(id, 'First') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/was/instance', { data: buildCreateData(id, 'Duplicate') });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/was/instance/${id}`);
        }
    });
});

test.describe('PUT /api/was/instance/:instanceId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/instance', { data: buildCreateData(id, 'Before') });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/was/instance/${id}`, {
                data: { ...buildCreateData(id, 'After'), ip: '10.0.0.2', port: '9090' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.instanceName).toBe('After');
            expect(body.data.ip).toBe('10.0.0.2');
        } finally {
            await request.delete(`/api/was/instance/${id}`);
        }
    });

    test('존재하지 않는 인스턴스 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/was/instance/ZZZZ', {
            data: buildCreateData('ZZZZ', 'NoSuch'),
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/was/instance/:instanceId — 삭제', () => {

    test('존재하는 인스턴스 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/was/instance', { data: buildCreateData(id, 'DeleteMe') });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/was/instance/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 인스턴스 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/was/instance/ZZZZ');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/was/instance/batch — 배치 저장', () => {

    test('C/U/D를 한 번에 전송하면 처리 건수를 반환해야 한다', async ({ request }) => {
        const id1 = uniqueId();
        const id2 = uniqueId();

        // 사전 생성: id2 (수정/삭제 대상)
        const setupRes = await request.post('/api/was/instance', { data: buildCreateData(id2, 'BatchSetup') });
        expect(setupRes.status()).toBe(201);

        try {
            const res = await request.post('/api/was/instance/batch', {
                data: [
                    { ...buildCreateData(id1, 'BatchCreate'), crud: 'C' },
                    { ...buildCreateData(id2, 'BatchUpdate'), ip: '10.0.0.2', crud: 'U' },
                ],
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBe(2);
        } finally {
            await request.delete(`/api/was/instance/${id1}`);
            await request.delete(`/api/was/instance/${id2}`);
        }
    });

    test('삭제(D) 액션이 정상 처리되어야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/was/instance', { data: buildCreateData(id, 'BatchDelete') });

        const res = await request.post('/api/was/instance/batch', {
            data: [{ ...buildCreateData(id, 'BatchDelete'), crud: 'D' }],
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data).toBe(1);

        // 삭제 확인
        const checkRes = await request.get(`/api/was/instance/${id}`);
        expect(checkRes.status()).toBe(404);
    });
});

test.describe('/api/was/instance/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/was/instance/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
