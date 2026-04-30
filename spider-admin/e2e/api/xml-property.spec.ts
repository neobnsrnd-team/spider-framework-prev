/**
 * XML Property API 계약 테스트 — /api/xml-property
 *
 * 검증 범위:
 * - 파일 목록 조회
 * - 파일 상세 조회 (존재/미존재)
 * - 파일 생성 (성공/중복)
 * - 항목 일괄 저장 (성공/미존재)
 * - 파일 삭제 (성공/미존재)
 * - 인증 검증
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-xp-';
let seq = 0;
function uniqueName() { return PREFIX + Date.now().toString(36) + (seq++); }

async function createFile(request: import('@playwright/test').APIRequestContext, fileName: string, description?: string) {
    const res = await request.post('/api/xml-property/files', {
        data: { fileName, description: description ?? '' },
    });
    expect(res.status()).toBe(201);
    return res;
}

async function deleteFile(request: import('@playwright/test').APIRequestContext, fileName: string) {
    // fileName에 .properties.xml이 자동 추가되므로 그대로 전달
    await request.delete(`/api/xml-property/files/${encodeURIComponent(fileName)}`);
}

test.describe('GET /api/xml-property/files — 파일 목록 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/xml-property/files');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('파일을 생성한 후 목록에 포함되어야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'ListTest');

        try {
            const res = await request.get('/api/xml-property/files');

            expect(res.status()).toBe(200);
            const body = await res.json();
            const match = body.data.find((f: { fileName: string }) => f.fileName === fullName);
            expect(match).toBeTruthy();
            expect(match.fileName).toBe(fullName);
        } finally {
            await deleteFile(request, fullName);
        }
    });
});

test.describe('GET /api/xml-property/files/:fileName — 파일 상세 조회', () => {

    test('존재하는 파일 조회 시 HTTP 200과 파일 상세 정보를 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'DetailTest');

        try {
            const res = await request.get(`/api/xml-property/files/${encodeURIComponent(fullName)}`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.fileName).toBe(fullName);
            expect(body.data).toHaveProperty('entries');
            expect(Array.isArray(body.data.entries)).toBe(true);
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('존재하지 않는 파일 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/xml-property/files/no-such-file.properties.xml');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('POST /api/xml-property/files — 파일 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';

        try {
            const res = await request.post('/api/xml-property/files', {
                data: { fileName: name, description: 'CreateTest' },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.fileName).toBe(fullName);
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('중복 파일명으로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'First');

        try {
            const res = await request.post('/api/xml-property/files', {
                data: { fileName: name, description: 'Duplicate' },
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('파일명 없이 생성 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/xml-property/files', {
            data: { fileName: '', description: 'NoName' },
        });

        expect(res.status()).toBe(400);
    });
});

test.describe('PUT /api/xml-property/files/:fileName/entries — 항목 일괄 저장', () => {

    test('유효한 항목 데이터로 저장 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'SaveTest');

        try {
            const res = await request.put(`/api/xml-property/files/${encodeURIComponent(fullName)}/entries`, {
                data: {
                    fileName: fullName,
                    description: 'Updated description',
                    entries: [
                        { key: 'key1', value: 'value1', description: 'desc1' },
                        { key: 'key2', value: 'value2', description: 'desc2' },
                    ],
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.entries).toHaveLength(2);
            expect(body.data.entries[0].key).toBe('key1');
            expect(body.data.entries[0].value).toBe('value1');
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('존재하지 않는 파일에 항목 저장 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/xml-property/files/no-such-file.properties.xml/entries', {
            data: {
                fileName: 'no-such-file.properties.xml',
                description: '',
                entries: [{ key: 'k', value: 'v', description: '' }],
            },
        });

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('중복 key가 포함된 항목 저장 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'DupKeyTest');

        try {
            const res = await request.put(`/api/xml-property/files/${encodeURIComponent(fullName)}/entries`, {
                data: {
                    fileName: fullName,
                    description: '',
                    entries: [
                        { key: 'sameKey', value: 'v1', description: '' },
                        { key: 'sameKey', value: 'v2', description: '' },
                    ],
                },
            });

            expect(res.status()).toBe(400);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deleteFile(request, fullName);
        }
    });
});

test.describe('DELETE /api/xml-property/files/:fileName — 파일 삭제', () => {

    test('존재하는 파일 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const name = uniqueName();
        const fullName = name + '.properties.xml';
        await createFile(request, name, 'DeleteMe');

        const res = await request.delete(`/api/xml-property/files/${encodeURIComponent(fullName)}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 파일 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/xml-property/files/no-such-file.properties.xml');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 요청 시 401 또는 302(로그인 리다이렉트)를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/xml-property/files', {
            headers: { Cookie: '' },
        });

        // Spring Security: 비인증 API 요청은 401, 폼로그인은 302
        expect([401, 302]).toContain(res.status());
    });
});
