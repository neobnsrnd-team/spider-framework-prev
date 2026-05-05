/**
 * Property DB API 계약 테스트 — /api/properties
 *
 * 검증 범위:
 * - 프로퍼티 그룹 페이지네이션 조회 (목록, 검색 필터)
 * - 프로퍼티 그룹 전체 목록 조회
 * - 프로퍼티 그룹 중복 확인
 * - 프로퍼티 그룹 생성 (성공/중복)
 * - 프로퍼티 그룹 삭제 (성공/미존재)
 * - 프로퍼티 일괄 저장 (생성/수정/삭제)
 * - 엑셀 내보내기
 */

import { test, expect } from '@playwright/test';

const PREFIX = 'e2e-prop-';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function propertyGroupData(groupId: string, groupName: string) {
    return {
        propertyGroupId: groupId,
        propertyGroupName: groupName,
        properties: [
            {
                propertyId: groupId + '-p1',
                propertyName: 'TestProp1',
                propertyDesc: 'Test property 1',
                dataType: 'C',
                validData: '',
                defaultValue: 'default1',
            },
        ],
    };
}

async function createPropertyGroup(request: import('@playwright/test').APIRequestContext, groupId: string, groupName: string = 'TestGroup') {
    const res = await request.post('/api/properties/groups', {
        data: propertyGroupData(groupId, groupName),
    });
    expect(res.status()).toBe(201);
}

async function deletePropertyGroup(request: import('@playwright/test').APIRequestContext, groupId: string) {
    await request.delete(`/api/properties/groups/${groupId}`);
}

test.describe('GET /api/properties/groups/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/properties/groups/page', { params: { page: 1, size: 5 } });

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
        await createPropertyGroup(request, id);

        try {
            const res = await request.get('/api/properties/groups/page', {
                params: { page: 1, size: 10, searchField: 'propertyGroupId', searchValue: id },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            const match = body.data.content.find((v: { propertyGroupId: string }) => v.propertyGroupId === id);
            expect(match).toBeTruthy();
            expect(match.propertyGroupId).toBe(id);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });
});

test.describe('GET /api/properties/groups — 전체 목록 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/properties/groups');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});

test.describe('GET /api/properties/groups/:groupId/exists — 중복 확인', () => {

    test('존재하는 그룹 ID 조회 시 true를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        try {
            const res = await request.get(`/api/properties/groups/${id}/exists`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBe(true);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });

    test('존재하지 않는 그룹 ID 조회 시 false를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/properties/groups/no-such-group/exists');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBe(false);
    });
});

test.describe('GET /api/properties/groups/:groupId/properties — 그룹별 프로퍼티 조회', () => {

    test('존재하는 그룹의 프로퍼티 목록을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        try {
            const res = await request.get(`/api/properties/groups/${id}/properties`);

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(Array.isArray(body.data)).toBe(true);
            expect(body.data.length).toBeGreaterThanOrEqual(1);
            expect(body.data[0].propertyGroupId).toBe(id);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });
});

test.describe('POST /api/properties/groups — 프로퍼티 그룹 생성', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        try {
            const res = await request.post('/api/properties/groups', {
                data: propertyGroupData(id, 'CreateTest'),
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });

    test('중복 ID로 생성 시 HTTP 409를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        try {
            const res = await request.post('/api/properties/groups', {
                data: propertyGroupData(id, 'DupTest'),
            });

            expect(res.status()).toBe(409);
            const body = await res.json();
            expect(body.success).toBe(false);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });
});

test.describe('POST /api/properties/save — 프로퍼티 일괄 저장', () => {

    test('프로퍼티 추가(C) 시 HTTP 200과 처리 건수를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        try {
            const res = await request.post('/api/properties/save', {
                data: [{
                    propertyGroupId: id,
                    propertyId: id + '-p2',
                    propertyName: 'NewProp',
                    propertyDesc: 'New property',
                    dataType: 'C',
                    validData: '',
                    defaultValue: 'val',
                    crud: 'C',
                }],
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBe(1);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });

    test('프로퍼티 수정(U) 시 HTTP 200과 처리 건수를 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        try {
            const res = await request.post('/api/properties/save', {
                data: [{
                    propertyGroupId: id,
                    propertyId: id + '-p1',
                    propertyName: 'UpdatedName',
                    propertyDesc: 'Updated desc',
                    dataType: 'N',
                    validData: '',
                    defaultValue: 'updated',
                    crud: 'U',
                }],
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data).toBe(1);
        } finally {
            await deletePropertyGroup(request, id);
        }
    });
});

test.describe('DELETE /api/properties/groups/:groupId — 프로퍼티 그룹 삭제', () => {

    test('존재하는 그룹 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();
        await createPropertyGroup(request, id);

        const res = await request.delete(`/api/properties/groups/${id}`);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('존재하지 않는 그룹 삭제 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/properties/groups/no-such-group');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('GET /api/properties/groups/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/properties/groups/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});
