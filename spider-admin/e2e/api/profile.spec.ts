import { test, expect, APIRequestContext } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

const BASE_URL = '/api/profile';

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

/** 원본 프로필을 복원한다 (테스트 후 정리용). */
async function restoreProfile(
    request: APIRequestContext,
    original: Record<string, unknown>,
) {
    await request.put(BASE_URL, {
        data: {
            userId: original.userId,
            userName: original.userName,
            confirmPassword: ADMIN.password,
            email: original.email,
            phone: original.phone,
            address: original.address,
            className: original.className,
        },
    });
}

// ─────────────────────────────────────────────
// GET /api/profile — 프로필 조회
// ─────────────────────────────────────────────

test.describe('GET /api/profile — 프로필 조회', () => {
    test('로그인한 사용자의 프로필 정보를 조회할 수 있어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL);
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data).toBeTruthy();
        expect(body.data.userId).toBe(ADMIN.userId);
        expect(body.data.userName).toBe(ADMIN.userName);
    });

    test('응답에 읽기 전용 필드가 포함되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL);
        const { data } = await res.json();

        // 읽기 전용 필드 존재 여부
        expect(data).toHaveProperty('userId');
        expect(data).toHaveProperty('roleId');
        expect(data).toHaveProperty('roleName');
        expect(data).toHaveProperty('userStateCode');
        expect(data).toHaveProperty('userStateName');
        expect(data).toHaveProperty('lastUpdateDtime');
        expect(data).toHaveProperty('lastUpdateUserId');
    });

    test('응답에 수정 가능 필드가 포함되어야 한다', async ({ request }) => {
        const res = await request.get(BASE_URL);
        const { data } = await res.json();

        expect(data).toHaveProperty('userName');
        expect(data).toHaveProperty('email');
        expect(data).toHaveProperty('phone');
        expect(data).toHaveProperty('address');
        expect(data).toHaveProperty('className');
    });
});

// ─────────────────────────────────────────────
// PUT /api/profile — 프로필 수정
// ─────────────────────────────────────────────

test.describe('PUT /api/profile — 프로필 수정', () => {
    test('유효한 데이터로 프로필을 수정할 수 있어야 한다', async ({ request }) => {
        // 원본 조회
        const getRes = await request.get(BASE_URL);
        const original = (await getRes.json()).data;

        try {
            const res = await request.put(BASE_URL, {
                data: {
                    userId: ADMIN.userId,
                    userName: 'E2E 수정테스트',
                    confirmPassword: ADMIN.password,
                    email: 'e2e-update@test.com',
                    phone: '01099998888',
                    address: '서울시 테스트구',
                    className: original.className,
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.userName).toBe('E2E 수정테스트');
            expect(body.data.email).toBe('e2e-update@test.com');
            expect(body.data.phone).toBe('01099998888');
            expect(body.data.address).toBe('서울시 테스트구');
        } finally {
            await restoreProfile(request, original);
        }
    });

    test('사용자명이 비어 있으면 400 에러가 반환되어야 한다', async ({ request }) => {
        const res = await request.put(BASE_URL, {
            data: {
                userId: ADMIN.userId,
                userName: '',
                confirmPassword: ADMIN.password,
            },
        });

        expect(res.status()).toBe(400);
    });

    test('사용자ID가 비어 있으면 400 에러가 반환되어야 한다', async ({ request }) => {
        const res = await request.put(BASE_URL, {
            data: {
                userId: '',
                userName: ADMIN.userName,
                confirmPassword: ADMIN.password,
            },
        });

        expect(res.status()).toBe(400);
    });

    test('이미 존재하는 사용자ID로 변경하면 409 에러가 반환되어야 한다', async ({ request }) => {
        const res = await request.put(BASE_URL, {
            data: {
                userId: 'e2e-readonly',
                userName: ADMIN.userName,
                confirmPassword: ADMIN.password,
            },
        });

        expect(res.status()).toBe(409);
    });

    test('유효하지 않은 이메일 형식이면 400 에러가 반환되어야 한다', async ({ request }) => {
        const res = await request.put(BASE_URL, {
            data: {
                userId: ADMIN.userId,
                userName: ADMIN.userName,
                confirmPassword: ADMIN.password,
                email: 'invalid-email',
            },
        });

        expect(res.status()).toBe(400);
    });

    test('새 비밀번호가 복잡성 규칙에 맞지 않으면 에러가 반환되어야 한다', async ({ request }) => {
        const res = await request.put(BASE_URL, {
            data: {
                userId: ADMIN.userId,
                userName: ADMIN.userName,
                newPassword: '1234',
                confirmPassword: ADMIN.password,
            },
        });

        // 400 (validation) or 서비스에서 InvalidInputException
        expect(res.ok()).toBe(false);
    });
});

// ─────────────────────────────────────────────
// 인증 검증 — 비인증 요청
// ─────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {
    let ctx: APIRequestContext;

    test.beforeAll(async ({ playwright }) => {
        const baseURL =
            test.info().project.use.baseURL ?? 'http://localhost:8080';
        ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
    });

    test.afterAll(async () => {
        await ctx.dispose();
    });

    test('인증되지 않은 요청으로 프로필을 조회하면 401이 반환되어야 한다', async () => {
        const res = await ctx.get(BASE_URL);
        expect(res.status()).toBe(401);
    });

    test('인증되지 않은 요청으로 프로필을 수정하면 401이 반환되어야 한다', async () => {
        const res = await ctx.put(BASE_URL, {
            data: {
                userId: ADMIN.userId,
                userName: ADMIN.userName,
                confirmPassword: ADMIN.password,
            },
        });
        expect(res.status()).toBe(401);
    });
});
