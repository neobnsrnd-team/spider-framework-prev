/**
 * Validation API 계약 테스트 — /api/validations
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

const PREFIX = 'e2e-vld-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function validationData(id: string, overrides: Record<string, string> = {}) {
    return {
        validationId: id,
        validationDesc: overrides.validationDesc ?? '테스트 Validation',
        javaClassName: overrides.javaClassName ?? 'com.test.Validation',
        ...overrides,
    };
}

test.describe('GET /api/validations/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validations/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/validations', { data: validationData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/validations/page', {
                params: { page: 1, size: 10, validationId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((v: { validationId: string }) => v.validationId === id);
            expect(match).toBeTruthy();
            expect(match.validationId).toBe(id);
        } finally {
            await request.delete(`/api/validations/${id}`);
        }
    });
});

test.describe('GET /api/validations/:validationId — 단건 조회', () => {

    test('존재하는 Validation 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validations', { data: validationData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get(`/api/validations/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validationId).toBe(id);
            expect(body.data.validationDesc).toBe('테스트 Validation');
        } finally {
            await request.delete(`/api/validations/${id}`);
        }
    });

    test('존재하지 않는 Validation 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validations/no-such-validation');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/validations — 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/validations', { data: validationData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validationId).toBe(id);
        } finally {
            await request.delete(`/api/validations/${id}`);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validations', { data: validationData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.post('/api/validations', { data: validationData(id) });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await request.delete(`/api/validations/${id}`);
        }
    });
});

test.describe('PUT /api/validations/:validationId — 수정', () => {

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validations', { data: validationData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.put(`/api/validations/${id}`, {
                data: {
                    validationDesc: '수정된 설명',
                    javaClassName: 'com.test.Updated',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.validationDesc).toBe('수정된 설명');
            expect(body.data.javaClassName).toBe('com.test.Updated');
        } finally {
            await request.delete(`/api/validations/${id}`);
        }
    });

    test('존재하지 않는 Validation 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/validations/no-such-validation', {
            data: { validationDesc: '수정' },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('DELETE /api/validations/:validationId — 삭제', () => {

    test('존재하는 Validation 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        const createRes = await request.post('/api/validations', { data: validationData(id) });
        expect(createRes.status()).toBe(201);

        const res = await request.delete(`/api/validations/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 Validation 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/validations/no-such-validation');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('GET /api/validations/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/validations/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태로 목록 조회 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/validations/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });
});
