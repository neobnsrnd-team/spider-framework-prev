/**
 * 거래 Validator 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createValidator(request: APIRequestContext, id: string, name: string) {
    const res = await request.post('/api/validators', {
        data: {
            validatorId: id,
            validatorName: name,
            bizDomain: '00',
            javaClassName: 'com.test.' + id.replace(/[^a-zA-Z0-9]/g, ''),
            useYn: 'Y',
        },
    });
    expect(res.status()).toBe(201);
}

/** bizDomain 드롭다운이 비어 있으면 옵션을 주입한다 (코드그룹 FR20003 미시딩 대응). */
async function ensureBizDomainOption(page: Page) {
    const hasOptions = await page.locator('#vModalBizDomain option').count();
    if (hasOptions === 0) {
        await page.evaluate(() => {
            const select = document.querySelector('#vModalBizDomain') as HTMLSelectElement;
            if (select && select.options.length === 0) {
                const opt = document.createElement('option');
                opt.value = '00';
                opt.textContent = '공통';
                select.appendChild(opt);
                select.value = '00';
            }
        });
    }
}

async function deleteValidator(request: APIRequestContext, id: string) {
    await request.delete(`/api/validators/${id}`);
}

async function searchByField(page: Page, fieldId: string, value: string) {
    await page.locator(`#_searchContainer_${fieldId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/validators/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/validators');
    await page.waitForResponse(r => r.url().includes('/api/validators/page'));
    await page.locator('#_searchContainer_validatorId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('Validator 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#validatorTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('Validator ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vlist-');
        await createValidator(request, id, 'SearchTest');

        try {
            await searchByField(page, 'validatorId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteValidator(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vlist-');
        await createValidator(request, id, 'PageResetTest');

        try {
            await searchByField(page, 'validatorId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteValidator(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vlist-');
        await createValidator(request, id, 'SortTest');

        try {
            const sortHeader = page.locator('#validatorTable thead th[data-sort="validatorId"]');

            // 1) 첫 클릭 → 오름차순 (sort-asc)
            const res1 = page.waitForResponse(r => r.url().includes('/api/validators/page'));
            await sortHeader.click();
            await res1;
            await expect(sortHeader).toHaveClass(/sort-asc/);

            // 2) 두 번째 클릭 → 내림차순 (sort-desc)
            const res2 = page.waitForResponse(r => r.url().includes('/api/validators/page'));
            await sortHeader.click();
            await res2;
            await expect(sortHeader).toHaveClass(/sort-desc/);

            // 3) 세 번째 클릭 → 정렬 해제
            const res3 = page.waitForResponse(r => r.url().includes('/api/validators/page'));
            await sortHeader.click();
            await res3;
            await expect(sortHeader).not.toHaveClass(/sort-asc/);
            await expect(sortHeader).not.toHaveClass(/sort-desc/);
        } finally {
            await deleteValidator(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-vlist-');
        await createValidator(request, id, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await page.waitForResponse(r => r.url().includes('/api/validators/page'));

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteValidator(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-vlist-');
        await createValidator(request, id, 'RowClickTest');

        try {
            await searchByField(page, 'validatorId', id);
            await page.getByRole('row').filter({ hasText: id }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.VALIDATOR_DETAIL_TITLE)).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidator(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('Validator CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.VALIDATOR_CREATE_TITLE)).toBeVisible();

        // PK 필드 편집 가능
        await expect(page.locator('#vModalValidatorId')).toBeEditable();

        // 삭제 버튼 숨김
        await expect(page.locator('#btnValidatorDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('모달에서 Validator를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vcrud-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vModalValidatorId').fill(testId);
            await page.locator('#vModalValidatorName').fill('생성테스트');
            await page.locator('#vModalJavaClassName').fill('com.test.Create');
            await ensureBizDomainOption(page);

            const responsePromise = page.waitForResponse(r =>
                r.url().endsWith('/api/validators') && r.request().method() === 'POST');
            await page.locator('#btnValidatorSave').click();
            await responsePromise;
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'validatorId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteValidator(request, testId);
        }
    });

    test('중복된 Validator를 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vcrud-');
        await createValidator(request, testId, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vModalValidatorId').fill(testId);
            await page.locator('#vModalValidatorName').fill('중복테스트');
            await page.locator('#vModalJavaClassName').fill('com.test.Dup');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/validators') && r.request().method() === 'POST');
            await page.locator('#btnValidatorSave').click();
            await responsePromise;

            // Toast 에러 알림이 표시되어야 한다
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidator(request, testId);
        }
    });

    test('유효하지 않은 값을 입력할 경우 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 필수 항목(Validator명) 비우고 ID만 입력 후 저장
        await page.locator('#vModalValidatorId').fill('temp-id');

        await page.locator('#btnValidatorSave').click();

        // validation Toast가 표시되어야 한다
        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(Validator ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vcrud-');
        await createValidator(request, testId, '수정대상');

        try {
            await searchByField(page, 'validatorId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.VALIDATOR_DETAIL_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#vModalValidatorId')).toBeDisabled();
            await expect(page.locator('#vModalValidatorId')).toHaveValue(testId);

            // 삭제 버튼 표시
            await expect(page.locator('#btnValidatorDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidator(request, testId);
        }
    });

    test('Validator명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vcrud-');
        await createValidator(request, testId, 'BeforeName');

        try {
            await searchByField(page, 'validatorId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#vModalValidatorName').fill('AfterName');
            await ensureBizDomainOption(page);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/validators/${testId}`) && r.request().method() === 'PUT');
            await page.locator('#btnValidatorSave').click();
            await responsePromise;
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'validatorId', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteValidator(request, testId);
        }
    });

    test('Validator를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-vcrud-');
        await createValidator(request, testId, 'DeleteMe');

        await searchByField(page, 'validatorId', testId);
        await page.getByRole('row').filter({ hasText: testId }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.locator('#btnValidatorDelete').click();

        // Toast.confirm 모달에서 확인 클릭
        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModal .btn-primary').click();

        await page.waitForResponse(r =>
            r.url().includes(`/api/validators/${testId}`) && r.request().method() === 'DELETE');

        await searchByField(page, 'validatorId', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('Validator 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/validators');
        await page.waitForResponse(r => r.url().includes('/api/validators/page'));

        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const testId = generateTestId('e2e-vperm-');
        await createValidator(adminRequest, testId, 'PermTest');

        try {
            await searchByField(page, 'validatorId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await expect(page.locator('#btnValidatorSave')).not.toBeVisible();
            await expect(page.locator('#btnValidatorDelete')).not.toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteValidator(adminRequest, testId);
            await adminRequest.dispose();
        }
    });
});
