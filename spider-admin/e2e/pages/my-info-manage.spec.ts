import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

const PAGE_URL = '/users/profile';
const API_URL = '/api/profile';

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

function isProfileResponse(url: string): boolean {
    return url.includes(API_URL);
}

async function navigateToProfile(page: Page) {
    const responsePromise = page.waitForResponse(
        (r) => isProfileResponse(r.url()) && r.request().method() === 'GET',
    );
    await page.goto(PAGE_URL);
    await responsePromise;
}

async function restoreProfile(
    request: APIRequestContext,
    original: Record<string, unknown>,
) {
    await request.put(API_URL, {
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
// 상세정보 조회
// ─────────────────────────────────────────────

test.describe('상세정보 조회', () => {
    test('페이지 로드 시 로그인한 사용자의 프로필 데이터가 표시되어야 한다', async ({ page }) => {
        await navigateToProfile(page);

        const userId = page.locator('#profileUserId');
        await expect(userId).toHaveValue(ADMIN.userId);

        const userName = page.locator('#profileUserName');
        await expect(userName).toHaveValue(ADMIN.userName);
    });

    test('읽기 전용 필드는 disabled 상태여야 한다', async ({ page }) => {
        await navigateToProfile(page);

        await expect(page.locator('#profilePositionName')).toBeDisabled();
        await expect(page.locator('#profileRoleName')).toBeDisabled();
        await expect(page.locator('#profileUserStateName')).toBeDisabled();
        await expect(page.locator('#profileLastUpdateDtime')).toBeDisabled();
        await expect(page.locator('#profileLastUpdateUserId')).toBeDisabled();
        await expect(page.locator('#profileAccessIp')).toBeDisabled();
    });

    test('수정 가능 필드는 편집 가능한 상태여야 한다', async ({ page }) => {
        await navigateToProfile(page);

        await expect(page.locator('#profileUserId')).toBeEditable();
        await expect(page.locator('#profileUserName')).toBeEditable();
        await expect(page.locator('#profilePhone')).toBeEditable();
        await expect(page.locator('#profileEmail')).toBeEditable();
        await expect(page.locator('#profileAddress')).toBeEditable();
        await expect(page.locator('#profileClassName')).toBeEnabled();
    });

    test('비밀번호 필드에 마스킹된 값이 표시되어야 한다', async ({ page }) => {
        await navigateToProfile(page);

        const passwordField = page.locator('#profilePassword');
        await expect(passwordField).toHaveAttribute('type', 'password');
        await expect(passwordField).not.toHaveValue('');
    });
});

// ─────────────────────────────────────────────
// 프로필 수정
// ─────────────────────────────────────────────

test.describe('프로필 수정', () => {
    test('사용자명을 수정하면 수정된 값이 반영되어야 한다', async ({
        page,
        request,
    }) => {
        await navigateToProfile(page);

        // 원본 백업
        const getRes = await request.get(API_URL);
        const original = (await getRes.json()).data;

        try {
            await page.locator('#profileUserName').fill('E2E수정테스트');
            await page.locator('#profileConfirmPassword').fill(ADMIN.password);

            const responsePromise = page.waitForResponse(
                (r) =>
                    isProfileResponse(r.url()) &&
                    r.request().method() === 'PUT',
            );
            await page.locator('#btnSaveProfile').click();
            await responsePromise;

            // Toast 성공 알림 확인
            await expect(page.locator('.toast')).toBeVisible();

            // 값 반영 확인
            await expect(page.locator('#profileUserName')).toHaveValue(
                'E2E수정테스트',
            );
        } finally {
            await restoreProfile(request, original);
        }
    });

    test('비밀번호 확인 없이 저장하면 에러 메시지가 표시되어야 한다', async ({
        page,
    }) => {
        await navigateToProfile(page);

        // 비밀번호 확인 비우고 저장 시도
        await page.locator('#profileConfirmPassword').fill('');
        await page.locator('#btnSaveProfile').click();

        // error-message가 visible 상태여야 한다
        await expect(page.locator('#confirmPasswordError')).toHaveClass(
            /visible/,
        );
    });

    test('유효하지 않은 이메일 형식을 입력하면 에러 메시지가 표시되어야 한다', async ({
        page,
    }) => {
        await navigateToProfile(page);

        await page.locator('#profileEmail').fill('invalid-email');
        await page.locator('#profileEmail').blur();

        await expect(page.locator('#emailError')).toHaveClass(/visible/);
    });

    test('이미 존재하는 사용자ID로 변경하면 에러 Toast가 표시되어야 한다', async ({
        page,
        request,
    }) => {
        await navigateToProfile(page);

        // 원본 백업
        const getRes = await request.get(API_URL);
        const original = (await getRes.json()).data;

        try {
            await page.locator('#profileUserId').fill('e2e-readonly');
            await page.locator('#profileConfirmPassword').fill(ADMIN.password);

            const responsePromise = page.waitForResponse(
                (r) =>
                    isProfileResponse(r.url()) &&
                    r.request().method() === 'PUT',
            );
            await page.locator('#btnSaveProfile').click();
            await responsePromise;

            // 에러 Toast
            await expect(
                page.locator('.toast.text-bg-danger'),
            ).toBeVisible();
        } finally {
            await restoreProfile(request, original);
        }
    });

    test('비밀번호 필드를 클릭하면 마스킹 값이 지워지고 새 비밀번호를 입력할 수 있어야 한다', async ({
        page,
    }) => {
        await navigateToProfile(page);

        const passwordField = page.locator('#profilePassword');

        // 클릭 전: 마스킹 값이 있어야 한다
        await expect(passwordField).not.toHaveValue('');

        // 포커스 → 값이 지워져야 한다
        await passwordField.focus();
        await expect(passwordField).toHaveValue('');

        // 새 비밀번호 입력 가능
        await passwordField.fill('NewPass1!');
        await expect(passwordField).toHaveValue('NewPass1!');
    });
});

// ─────────────────────────────────────────────
// 권한 — R 권한 사용자
// ─────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 저장 버튼이 표시되어서는 안 된다', async ({
        page,
    }) => {
        await navigateToProfile(page);

        await expect(page.locator('#btnSaveProfile')).not.toBeVisible();
    });
});
