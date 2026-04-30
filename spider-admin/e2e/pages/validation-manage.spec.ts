/**
 * Validation 컴포넌트 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createValidation(request: APIRequestContext, id: string, desc: string) {
    const res = await request.post('/api/validations', {
        data: {
            validationId: id,
            validationDesc: desc,
            javaClassName: 'com.test.' + id.replace(/[^a-zA-Z0-9]/g, ''),
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteValidation(request: APIRequestContext, id: string) {
    await request.delete(`/api/validations/${id}`);
}

async function searchByField(page: Page, fieldId: string, value: string) {
    await page.locator(`#_searchContainer_${fieldId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/validations/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/validation');
    await page.waitForResponse(r => r.url().includes('/api/validations/page'));
    await page.locator('#_searchContainer_validationId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('Validation 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#validationTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('Validation ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vldlist-');
        await createValidation(request, id, '목록검색테스트');

        try {
            await searchByField(page, 'validationId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteValidation(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vldlist-');
        await createValidation(request, id, '페이지초기화테스트');

        try {
            await searchByField(page, 'validationId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteValidation(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vldlist-');
        await createValidation(request, id, '정렬테스트');

        try {
            const sortHeader = page.locator('#validationTable thead th[data-sort="validationId"]');

            const res1 = page.waitForResponse(r => r.url().includes('/api/validations/page'));
            await sortHeader.click();
            await res1;
            await expect(sortHeader).toHaveClass(/sort-asc/);

            const res2 = page.waitForResponse(r => r.url().includes('/api/validations/page'));
            await sortHeader.click();
            await res2;
            await expect(sortHeader).toHaveClass(/sort-desc/);

            const res3 = page.waitForResponse(r => r.url().includes('/api/validations/page'));
            await sortHeader.click();
            await res3;
            await expect(sortHeader).not.toHaveClass(/sort-asc/);
            await expect(sortHeader).not.toHaveClass(/sort-desc/);
        } finally {
            await deleteValidation(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-vldlist-');
        await createValidation(request, id, '출력테스트');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await page.waitForResponse(r => r.url().includes('/api/validations/page'));

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteValidation(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vldlist-');
        await createValidation(request, id, '행클릭테스트');

        try {
            await searchByField(page, 'validationId', id);
            await page.getByRole('row').filter({ hasText: id }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.VALIDATION_DETAIL_TITLE)).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidation(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('Validation CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.VALIDATION_CREATE_TITLE)).toBeVisible();

        // PK 필드 편집 가능
        await expect(page.locator('#vldModalValidationId')).toBeEditable();

        // 삭제 버튼 숨김
        await expect(page.locator('#btnValidationDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('모달에서 Validation을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vldcrud-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vldModalValidationId').fill(testId);
            await page.locator('#vldModalValidationDesc').fill('생성테스트');

            const responsePromise = page.waitForResponse(r =>
                r.url().endsWith('/api/validations') && r.request().method() === 'POST');
            await page.locator('#btnValidationSave').click();
            await responsePromise;
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'validationId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteValidation(request, testId);
        }
    });

    test('중복된 Validation을 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vldcrud-');
        await createValidation(request, testId, '중복테스트');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vldModalValidationId').fill(testId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/validations') && r.request().method() === 'POST');
            await page.locator('#btnValidationSave').click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidation(request, testId);
        }
    });

    test('필수 항목(Validation ID)을 비우고 저장 시 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // ID 입력 없이 저장
        await page.locator('#btnValidationSave').click();

        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(Validation ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vldcrud-');
        await createValidation(request, testId, '수정대상');

        try {
            await searchByField(page, 'validationId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.VALIDATION_DETAIL_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#vldModalValidationId')).toBeDisabled();
            await expect(page.locator('#vldModalValidationId')).toHaveValue(testId);

            // 삭제 버튼 표시
            await expect(page.locator('#btnValidationDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidation(request, testId);
        }
    });

    test('설명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vldcrud-');
        await createValidation(request, testId, '수정전설명');

        try {
            await searchByField(page, 'validationId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vldModalValidationDesc').fill('수정후설명');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/validations/${testId}`) && r.request().method() === 'PUT');
            await page.locator('#btnValidationSave').click();
            await responsePromise;
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'validationId', testId);
            await expect(page.getByRole('cell', { name: '수정후설명' })).toBeVisible();
        } finally {
            await deleteValidation(request, testId);
        }
    });

    test('Validation을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vldcrud-');
        await createValidation(request, testId, '삭제대상');

        await searchByField(page, 'validationId', testId);
        await page.getByRole('row').filter({ hasText: testId }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.locator('#btnValidationDelete').click();

        await expect(page.locator('#spConfirmModal')).toBeVisible();
        const deleteResponse = page.waitForResponse(r =>
            r.url().includes(`/api/validations/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#spConfirmModal .btn-primary').click();
        await deleteResponse;

        await searchByField(page, 'validationId', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('Validation 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/validation');
        await page.waitForResponse(r => r.url().includes('/api/validations/page'));

        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const testId = generateTestId('e2e-vldperm-');
        await createValidation(adminRequest, testId, '권한테스트');

        try {
            await searchByField(page, 'validationId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await expect(page.locator('#btnValidationSave')).not.toBeVisible();
            await expect(page.locator('#btnValidationDelete')).not.toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidation(adminRequest, testId);
            await adminRequest.dispose();
        }
    });
});
