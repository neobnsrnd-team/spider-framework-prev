/**
 * Biz App API 계약 테스트 — /api/biz-apps
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터)
 * - 단건 조회 (존재/미존재)
 * - 생성 (성공/중복)
 * - 수정 (성공/미존재)
 * - 삭제 (성공/미존재)
 * - 인증 검증
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-ba-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function bizAppData(id: string, overrides: Record<string, string> = {}) {
    return {
        bizAppId: id,
        bizAppName: overrides.bizAppName ?? 'TestBizApp',
        bizAppDesc: overrides.bizAppDesc ?? 'E2E 테스트용 Biz App',
        dupCheckYn: overrides.dupCheckYn ?? 'Y',
        queName: overrides.queName ?? '00',
        logYn: overrides.logYn ?? 'Y',
        ...overrides,
    };
}

test.describe('GET /api/biz-apps/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/biz-apps/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/biz-apps', { data: bizAppData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/biz-apps/page', {
                params: { page: 1, size: 10, bizAppId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((a: { bizAppId: string }) => a.bizAppId === id);
            expect(match).toBeTruthy();
            expect(match.bizAppId).toBe(id);
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });

    test('dupCheckYn 필터 적용 시 해당 조건에 맞는 결과를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id, { dupCheckYn: 'N' }) });

        try {
            const res = await request.get('/api/biz-apps/page', {
                params: { page: 1, size: 10, bizAppId: id, dupCheckYn: 'N' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            const match = body.data.content.find((a: { bizAppId: string }) => a.bizAppId === id);
            expect(match).toBeTruthy();
            expect(match.dupCheckYn).toBe('N');
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });
});

test.describe('GET /api/biz-apps/:bizAppId — 단건 조회', () => {

    test('존재하는 Biz App 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });

        try {
            const res = await request.get(`/api/biz-apps/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.bizAppId).toBe(id);
            expect(body.data).toHaveProperty('bizAppName');
            expect(body.data).toHaveProperty('dupCheckYn');
            expect(body.data).toHaveProperty('queName');
            expect(body.data).toHaveProperty('logYn');
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });

    test('존재하지 않는 Biz App 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/biz-apps/NOT-EXIST-9999');
        expect(res.status()).toBe(404);
    });
});

test.describe('POST /api/biz-apps — 등록', () => {

    test('유효한 요청으로 등록 시 HTTP 201과 생성된 Biz App을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/biz-apps', { data: bizAppData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.bizAppId).toBe(id);
            expect(body.data.dupCheckYn).toBe('Y');
            expect(body.data.logYn).toBe('Y');
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });

    test('중복 ID 등록 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });

        try {
            const res = await request.post('/api/biz-apps', { data: bizAppData(id) });
            expect(res.status()).toBe(409);
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/biz-apps', {
            data: { bizAppName: '이름만있음' },
        });
        expect(res.status()).toBe(400);
    });
});

test.describe('PUT /api/biz-apps/:bizAppId — 수정', () => {

    test('존재하는 Biz App 수정 시 HTTP 200과 수정된 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });

        try {
            const res = await request.put(`/api/biz-apps/${id}`, {
                data: {
                    bizAppName: '수정된 App 명',
                    bizAppDesc: '수정된 설명',
                    dupCheckYn: 'N',
                    queName: '00',
                    logYn: 'N',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.bizAppName).toBe('수정된 App 명');
            expect(body.data.dupCheckYn).toBe('N');
            expect(body.data.logYn).toBe('N');
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });

    test('존재하지 않는 Biz App 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/biz-apps/NOT-EXIST-9999', {
            data: {
                bizAppName: '수정',
                dupCheckYn: 'N',
                queName: '00',
                logYn: 'N',
            },
        });
        expect(res.status()).toBe(404);
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });

        try {
            const res = await request.put(`/api/biz-apps/${id}`, { data: {} });
            expect(res.status()).toBe(400);
        } finally {
            await request.delete(`/api/biz-apps/${id}`);
        }
    });
});

test.describe('DELETE /api/biz-apps/:bizAppId — 삭제', () => {

    test('존재하는 Biz App 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });

        const res = await request.delete(`/api/biz-apps/${id}`);
        expect(res.status()).toBe(200);
    });

    test('삭제 후 단건 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/biz-apps', { data: bizAppData(id) });
        await request.delete(`/api/biz-apps/${id}`);

        const res = await request.get(`/api/biz-apps/${id}`);
        expect(res.status()).toBe(404);
    });

    test('존재하지 않는 Biz App 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/biz-apps/NOT-EXIST-9999');
        expect(res.status()).toBe(404);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태에서 목록 조회 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/biz-apps/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });

    test('비인증 상태에서 등록 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/biz-apps', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bizAppData('test')),
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
