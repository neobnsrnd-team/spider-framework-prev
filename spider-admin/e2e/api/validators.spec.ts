/**
 * Validator API 계약 테스트 — /api/validators
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

const PREFIX = 'e2e-v-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function validatorData(id: string, overrides: Record<string, string> = {}) {
    return {
        validatorId: id,
        validatorName: overrides.validatorName ?? 'TestValidator',
        bizDomain: overrides.bizDomain ?? '00',
        javaClassName: overrides.javaClassName ?? 'com.test.Validator',
        useYn: overrides.useYn ?? 'Y',
        ...overrides,
    };
}

test.describe('GET /api/validators/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validators/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/validators', { data: validatorData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/validators/page', {
                params: { page: 1, size: 10, validatorId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((v: { validatorId: string }) => v.validatorId === id);
            expect(match).toBeTruthy();
            expect(match.validatorId).toBe(id);
        } finally {
            await request.delete(`/api/validators/${id}`);
        }
    });
});

test.describe('GET /api/validators/:validatorId — 단건 조회', () => {

    test('존재하는 Validator 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validators', { data: validatorData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/validators/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validatorId).toBe(id);
            expect(body.data.validatorName).toBe('TestValidator');
        } finally {
            await request.delete(`/api/validators/${id}`);
        }
    });

    test('존재하지 않는 Validator 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validators/no-such-validator');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/validators — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/validators', { data: validatorData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validatorId).toBe(id);
        } finally {
            await request.delete(`/api/validators/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validators', { data: validatorData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/validators', { data: validatorData(id) });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/validators/${id}`);
        }
    });
});

test.describe('PUT /api/validators/:validatorId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validators', { data: validatorData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/validators/${id}`, {
                data: {
                    validatorName: 'UpdatedName',
                    bizDomain: '00',
                    javaClassName: 'com.test.Updated',
                    useYn: 'Y',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validatorName).toBe('UpdatedName');
            expect(body.data.javaClassName).toBe('com.test.Updated');
        } finally {
            await request.delete(`/api/validators/${id}`);
        }
    });

    test('존재하지 않는 Validator 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/validators/no-such-validator', {
            data: {
                validatorName: 'X',
                bizDomain: '00',
                javaClassName: 'com.test.X',
                useYn: 'Y',
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/validators/:validatorId — 삭제', () => {

    test('존재하는 Validator 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validators', { data: validatorData(id) });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/validators/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 Validator 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/validators/no-such-validator');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('GET /api/validators/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validators/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
