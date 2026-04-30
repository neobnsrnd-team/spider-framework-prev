/**
 * SQL Query API 계약 테스트 — /api/sql-queries
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

const PREFIX = 'e2e-sq-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function sqlQueryData(id: string, overrides: Record<string, string> = {}) {
    return {
        queryId: id,
        queryName: overrides.queryName ?? 'TestSqlQuery',
        sqlGroupId: overrides.sqlGroupId ?? '',
        dbId: overrides.dbId ?? 'e2e-ds-001',
        sqlType: overrides.sqlType ?? 'SELECT',
        execType: overrides.execType ?? 'SYNC',
        cacheYn: overrides.cacheYn ?? 'N',
        timeOut: overrides.timeOut ?? '30',
        resultType: overrides.resultType ?? 'MAP',
        useYn: overrides.useYn ?? 'Y',
        sqlQuery: overrides.sqlQuery ?? 'SELECT 1 FROM DUAL',
        queryDesc: overrides.queryDesc ?? 'E2E 테스트용 SQL Query',
        ...overrides,
    };
}

test.describe('GET /api/sql-queries/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/sql-queries/page', { params: { page: 1, size: 5 } });

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
        const createRes = await request.post('/api/sql-queries', { data: sqlQueryData(id) });
        expect(createRes.status()).toBe(201);

        try {
            const res = await request.get('/api/sql-queries/page', {
                params: { page: 1, size: 10, queryId: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((a: { queryId: string }) => a.queryId === id);
            expect(match).toBeTruthy();
            expect(match.queryId).toBe(id);
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });

    test('useYn 필터 적용 시 해당 조건에 맞는 결과를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id, { useYn: 'N' }) });

        try {
            const res = await request.get('/api/sql-queries/page', {
                params: { page: 1, size: 10, queryId: id, useYn: 'N' },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            const match = body.data.content.find((a: { queryId: string }) => a.queryId === id);
            expect(match).toBeTruthy();
            expect(match.useYn).toBe('N');
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });
});

test.describe('GET /api/sql-queries/:queryId — 단건 조회', () => {

    test('존재하는 SQL Query 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });

        try {
            const res = await request.get(`/api/sql-queries/${id}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.queryId).toBe(id);
            expect(body.data).toHaveProperty('queryName');
            expect(body.data).toHaveProperty('useYn');
            expect(body.data).toHaveProperty('sqlQuery');
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });

    test('존재하지 않는 SQL Query 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/sql-queries/NOT-EXIST-9999');
        expect(res.status()).toBe(404);
    });
});

test.describe('POST /api/sql-queries — 등록', () => {

    test('유효한 요청으로 등록 시 HTTP 201과 생성된 SQL Query를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/sql-queries', { data: sqlQueryData(id) });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.queryId).toBe(id);
            expect(body.data.useYn).toBe('Y');
            expect(body.data.sqlQuery).toBe('SELECT 1 FROM DUAL');
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });

    test('중복 ID 등록 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });

        try {
            const res = await request.post('/api/sql-queries', { data: sqlQueryData(id) });
            expect(res.status()).toBe(409);
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/sql-queries', {
            data: { queryName: '이름만있음' },
        });
        expect(res.status()).toBe(400);
    });
});

test.describe('PUT /api/sql-queries/:queryId — 수정', () => {

    test('존재하는 SQL Query 수정 시 HTTP 200과 수정된 정보를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });

        try {
            const res = await request.put(`/api/sql-queries/${id}`, {
                data: {
                    queryName: '수정된 쿼리 명',
                    sqlGroupId: '',
                    dbId: 'e2e-ds-002',
                    sqlType: 'INSERT',
                    execType: 'ASYNC',
                    cacheYn: 'Y',
                    timeOut: '60',
                    resultType: 'LIST',
                    useYn: 'N',
                    sqlQuery: 'INSERT INTO T VALUES(1)',
                    queryDesc: '수정된 설명',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.queryName).toBe('수정된 쿼리 명');
            expect(body.data.useYn).toBe('N');
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });

    test('존재하지 않는 SQL Query 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/sql-queries/NOT-EXIST-9999', {
            data: {
                queryName: '수정',
                useYn: 'N',
            },
        });
        expect(res.status()).toBe(404);
    });

    test('필수 필드 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });

        try {
            const res = await request.put(`/api/sql-queries/${id}`, { data: {} });
            expect(res.status()).toBe(400);
        } finally {
            await request.delete(`/api/sql-queries/${id}`);
        }
    });
});

test.describe('DELETE /api/sql-queries/:queryId — 삭제', () => {

    test('존재하는 SQL Query 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });

        const res = await request.delete(`/api/sql-queries/${id}`);
        expect(res.status()).toBe(200);
    });

    test('삭제 후 단건 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await request.post('/api/sql-queries', { data: sqlQueryData(id) });
        await request.delete(`/api/sql-queries/${id}`);

        const res = await request.get(`/api/sql-queries/${id}`);
        expect(res.status()).toBe(404);
    });

    test('존재하지 않는 SQL Query 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/sql-queries/NOT-EXIST-9999');
        expect(res.status()).toBe(404);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태에서 목록 조회 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/sql-queries/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });

    test('비인증 상태에서 등록 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/sql-queries', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sqlQueryData('test')),
            redirect: 'manual',
        });
        expect([302, 401]).toContain(res.status);
    });
});
