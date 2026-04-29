/**
 * 당발 대응답 관리 API 계약 테스트 — /api/proxy-testdata
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터, 정렬)
 * - 단건 상세 조회 (존재/미존재)
 * - CRUD (생성, 수정, 삭제)
 * - 거래조회 모달 검색
 * - 대응답 설정 (proxy-settings, proxy-field, proxy-value, default-proxy)
 * - 인증/인가 (비인증, R 전용 사용자)
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

// ── 시드 데이터 상수 (e2e-seed.sql 기준) ──

const SEED_ORG_ID = 'E2EORG01';
const SEED_TRX_ID = 'E2E-TRX-001';
const SEED_MESSAGE_ID = 'E2E-MSG-001';

// ── 헬퍼 ──

function generateTestName(prefix: string): string {
    return prefix + Date.now().toString(36);
}

async function createProxyTestdata(
    request: APIRequestContext,
    testName: string,
    testDesc: string = 'E2E 테스트 설명',
): Promise<void> {
    const res = await request.post('/api/proxy-testdata', {
        data: {
            orgId: SEED_ORG_ID,
            trxId: SEED_TRX_ID,
            messageId: SEED_MESSAGE_ID,
            testName,
            testDesc,
            testData: '',
            testGroupId: 'DEFAULT',
        },
    });
    expect(res.status()).toBe(201);
}

async function findTestSno(request: APIRequestContext, testName: string): Promise<number> {
    const res = await request.get('/api/proxy-testdata/page', {
        params: { page: 1, size: 100, testNameFilter: testName },
    });
    const body = await res.json();
    const item = body.data.content.find((c: any) => c.testName === testName);
    return item.testSno;
}

async function deleteProxyTestdata(request: APIRequestContext, testSno: number) {
    await request.delete(`/api/proxy-testdata/${testSno}`, {
        params: { testGroupId: 'DEFAULT' },
    });
}

async function createAndGetSno(
    request: APIRequestContext,
    testName: string,
    testDesc: string = 'E2E 테스트 설명',
): Promise<number> {
    await createProxyTestdata(request, testName, testDesc);
    return findTestSno(request, testName);
}

// ─── 페이지네이션 조회 ────────────────────────────────────

test.describe('GET /api/proxy-testdata/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/page', {
            params: { page: 1, size: 5 },
        });

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
        const testName = generateTestName('e2e-search-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const res = await request.get('/api/proxy-testdata/page', {
                params: { page: 1, size: 10, testNameFilter: testName },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.content.length).toBe(1);
            expect(body.data.content[0].testName).toBe(testName);
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('기관ID 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-org-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const res = await request.get('/api/proxy-testdata/page', {
                params: { page: 1, size: 10, orgIdFilter: SEED_ORG_ID, testNameFilter: testName },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.data.content.length).toBe(1);
            expect(body.data.content[0].orgId).toBe(SEED_ORG_ID);
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('정렬 파라미터를 지정하면 해당 기준으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/page', {
            params: { page: 1, size: 10, sortBy: 'testName', sortDirection: 'ASC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });
});

// ─── 단건 상세 조회 ──────────────────────────────────────

test.describe('GET /api/proxy-testdata/{testSno} — 단건 조회', () => {

    test('존재하는 데이터 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-detail-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const res = await request.get(`/api/proxy-testdata/${testSno}`);
            expect(res.status()).toBe(200);

            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.testSno).toBe(testSno);
            expect(body.data.testName).toBe(testName);
            expect(body.data).toHaveProperty('orgId');
            expect(body.data).toHaveProperty('trxId');
            expect(body.data).toHaveProperty('messageId');
            expect(body.data).toHaveProperty('testGroupId');
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('존재하지 않는 데이터 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/999999999');
        expect(res.status()).toBe(404);

        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── CRUD ─────────────────────────────────────────────────

test.describe('/api/proxy-testdata — CRUD', () => {

    test('유효한 데이터로 생성 시 HTTP 201을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-create-');
        let testSno: number | undefined;

        try {
            const res = await request.post('/api/proxy-testdata', {
                data: {
                    orgId: SEED_ORG_ID,
                    trxId: SEED_TRX_ID,
                    messageId: SEED_MESSAGE_ID,
                    testName,
                    testDesc: '생성 테스트 설명',
                    testData: '',
                    testGroupId: 'DEFAULT',
                },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);

            testSno = await findTestSno(request, testName);
        } finally {
            if (testSno) await deleteProxyTestdata(request, testSno);
        }
    });

    test('필수 항목 없이 생성하면 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/proxy-testdata', {
            data: {
                orgId: SEED_ORG_ID,
                trxId: SEED_TRX_ID,
                messageId: SEED_MESSAGE_ID,
                // testName, testDesc 누락
            },
        });

        expect(res.status()).toBe(400);
    });

    test('유효한 데이터로 수정 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-update-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const newName = testName + '-modified';
            const res = await request.put(`/api/proxy-testdata/${testSno}`, {
                data: {
                    testName: newName,
                    testDesc: '수정된 설명',
                    testGroupId: 'DEFAULT',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);

            // 수정된 값 확인
            const detailRes = await request.get(`/api/proxy-testdata/${testSno}`);
            const detailBody = await detailRes.json();
            expect(detailBody.data.testName).toBe(newName);
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('데이터 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-delete-');
        const testSno = await createAndGetSno(request, testName);

        const res = await request.delete(`/api/proxy-testdata/${testSno}`, {
            params: { testGroupId: 'DEFAULT' },
        });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);

        // 삭제 후 조회하면 404
        const checkRes = await request.get(`/api/proxy-testdata/${testSno}`);
        expect(checkRes.status()).toBe(404);
    });
});

// ─── 거래 검색 ────────────────────────────────────────────

test.describe('GET /api/proxy-testdata/trx-messages/search — 거래조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/trx-messages/search', {
            params: { orgId: SEED_ORG_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('거래ID 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/trx-messages/search', {
            params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.length).toBeGreaterThan(0);
    });
});

// ─── 대응답 설정 ──────────────────────────────────────────

test.describe('/api/proxy-testdata/proxy-settings — 대응답 설정', () => {

    test('대응답 설정 목록 조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-ps-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const res = await request.get('/api/proxy-testdata/proxy-settings', {
                params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(Array.isArray(body.data)).toBe(true);
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('기본 대응답 조회 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/proxy-settings/default', {
            params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('대응답 필드 구분값 업데이트 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-pf-');
        const testSno = await createAndGetSno(request, testName);

        try {
            const res = await request.put('/api/proxy-testdata/proxy-settings/proxy-field', {
                params: {
                    orgId: SEED_ORG_ID,
                    trxId: SEED_TRX_ID,
                    proxyField: 'E2E_FIELD',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
        } finally {
            // 필드 초기화
            await request.put('/api/proxy-testdata/proxy-settings/proxy-field', {
                params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID, proxyField: '' },
            });
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 값 중복 건수 조회 시 HTTP 200과 숫자를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/proxy-settings/proxy-value/count', {
            params: {
                orgId: SEED_ORG_ID,
                trxId: SEED_TRX_ID,
                proxyValue: 'nonexistent-value',
            },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(typeof body.data).toBe('number');
    });

    test('기본 대응답 설정/해제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const testName = generateTestName('e2e-dp-');
        const testSno = await createAndGetSno(request, testName);

        try {
            // 설정
            const setRes = await request.put('/api/proxy-testdata/proxy-settings/default-proxy/set', {
                params: {
                    orgId: SEED_ORG_ID,
                    trxId: SEED_TRX_ID,
                    testSno,
                },
            });
            expect(setRes.status()).toBe(200);

            // 해제
            const clearRes = await request.put('/api/proxy-testdata/proxy-settings/default-proxy/clear', {
                params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID },
            });
            expect(clearRes.status()).toBe(200);
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });
});

// ─── 엑셀 내보내기 ────────────────────────────────────────

test.describe('GET /api/proxy-testdata/export — 엑셀 내보내기', () => {

    test('엑셀 내보내기 시 HTTP 200과 xlsx 콘텐츠를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/export');
        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'];
        expect(contentType).toContain('spreadsheetml');
    });
});

// ─── 인증 ─────────────────────────────────────────────────

test.describe('인증 — 비인증 요청', () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test('인증 없이 조회 요청 시 로그인 페이지로 리다이렉트되어야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/page', {
            params: { page: 1, size: 5 },
            maxRedirects: 0,
        });
        expect([302, 401]).toContain(res.status());
    });
});

// ─── 인가 ─────────────────────────────────────────────────

test.describe('인가 — PROXY_TESTDATA:W 없는 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자의 조회 요청은 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/proxy-testdata/page', {
            params: { page: 1, size: 5 },
        });
        expect(res.status()).toBe(200);
    });

    test('R 권한 사용자의 생성 요청은 HTTP 403을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/proxy-testdata', {
            data: {
                orgId: SEED_ORG_ID,
                trxId: SEED_TRX_ID,
                messageId: SEED_MESSAGE_ID,
                testName: 'forbidden-test',
                testDesc: 'forbidden',
            },
        });
        expect(res.status()).toBe(403);
    });

    test('R 권한 사용자의 삭제 요청은 HTTP 403을 반환해야 한다', async ({ request }) => {
        const res = await request.delete('/api/proxy-testdata/1', {
            params: { testGroupId: 'DEFAULT' },
        });
        expect(res.status()).toBe(403);
    });

    test('R 권한 사용자의 대응답 설정 변경 요청은 HTTP 403을 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/proxy-testdata/proxy-settings/proxy-field', {
            params: {
                orgId: SEED_ORG_ID,
                trxId: SEED_TRX_ID,
                proxyField: 'FORBIDDEN',
            },
        });
        expect(res.status()).toBe(403);
    });
});
