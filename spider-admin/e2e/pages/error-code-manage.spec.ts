/**
 * 오류코드 관리 페이지 — 목록, CRUD, 핸들러 모달, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ────────────────────────────────────────

async function createError(request: APIRequestContext, errorCode: string, errorTitle: string) {
    const res = await request.post('/api/errors', {
        data: { errorCode, errorTitle, errorLevel: '1' },
    });
    expect(res.status()).toBe(201);
}

async function deleteError(request: APIRequestContext, errorCode: string) {
    await request.delete(`/api/errors/${errorCode}`);
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/errors/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

// ─── 공통 beforeEach ────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/error-codes');
    // autoLoad가 false이므로 조회 버튼 클릭하여 데이터 로드
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/errors/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('오류코드 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#errorTableBody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('오류코드로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-elist-');
        await createError(request, id, 'SearchTest');

        try {
            await searchByField(page, 'errorCode', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteError(request, id);
        }
    });

    test('오류제목으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-elist-');
        const uniqueTitle = 'ErrSearch' + id;
        await createError(request, id, uniqueTitle);

        try {
            await searchByField(page, 'errorTitle', uniqueTitle);
            await expect(page.getByRole('cell', { name: uniqueTitle })).toBeVisible();
        } finally {
            await deleteError(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-elist-');
        await createError(request, id, 'PageResetTest');

        try {
            await searchByField(page, 'errorCode', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteError(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-elist-');
        await createError(request, id, 'SortTest');

        try {
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/errors/page'));
            await page.getByRole('columnheader', { name: LABEL.ERROR_CODE_COLUMN }).click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/errors/page'));
            await page.getByRole('columnheader', { name: LABEL.ERROR_CODE_COLUMN }).click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteError(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-eprint-');
        await createError(request, id, 'PrintTest');

        try {
            // headless에서 window.print()가 팝업을 즉시 닫는 것을 방지
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            // autoLoad=false이므로 조회 클릭
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/errors/page'));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('h3')).toHaveText('오류코드 목록');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteError(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-elist-');
        await createError(request, id, 'RowClickTest');

        try {
            await searchByField(page, 'errorCode', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/errors/${id}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteError(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('오류코드 CRUD', () => {

    test('추가 버튼을 클릭하면 빈 등록 모달이 열려야 한다', async ({ page }) => {
        await page.locator('#btnAdd').click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.ERROR_CREATE_TITLE)).toBeVisible();

        await expect(page.locator('#errorCode')).toHaveValue('');
        await expect(page.locator('#errorCode')).toBeEnabled();

        const dialog = page.getByRole('dialog');
        await expect(dialog.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(dialog).not.toBeVisible();
    });

    test('모달에서 오류코드를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ecrud-');

        try {
            await page.locator('#btnAdd').click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#errorCode').fill(testId);
            await page.locator('#errorTitle').fill('생성테스트오류');
            await page.locator('#errorLevel').selectOption('1');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/errors') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'errorCode', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('중복된 오류코드를 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ecrud-');
        await createError(request, testId, 'DupTest');

        try {
            await page.locator('#btnAdd').click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#errorCode').fill(testId);
            await page.locator('#errorTitle').fill('중복테스트');
            await page.locator('#errorLevel').selectOption('1');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/errors') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('유효하지 않은 값을 입력할 경우 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnAdd').click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 필수 항목 비우고 저장 클릭
        await page.getByRole('button', { name: LABEL.SAVE }).click();

        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(오류코드)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ecrud-');
        await createError(request, testId, '수정대상');

        try {
            await searchByField(page, 'errorCode', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/errors/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.ERROR_EDIT_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#errorCode')).toBeDisabled();
            await expect(page.locator('#errorCode')).toHaveValue(testId);

            const dialog = page.getByRole('dialog');
            await expect(dialog.locator('#btnDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('오류제목을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ecrud-');
        await createError(request, testId, 'BeforeName');

        try {
            await searchByField(page, 'errorCode', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/errors/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#errorTitle').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/errors/${testId}`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'errorCode', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('오류코드를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ecrud-');
        await createError(request, testId, 'DeleteMe');

        await searchByField(page, 'errorCode', testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/errors/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        // Toast.confirm 모달의 확인 버튼 클릭
        const deleteResponsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/errors/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#btnDelete').click();
        // Toast.confirm 대화상자에서 확인 버튼 클릭
        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalOk').click();
        await deleteResponsePromise;

        await expect(page.locator('#errorModal')).not.toBeVisible();
        await searchByField(page, 'errorCode', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});

// ─── 핸들러 모달 ────────────────────────────────────────────────

test.describe('오류별 핸들러 모달', () => {

    test('핸들러 버튼을 클릭하면 핸들러 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ehdlr-');
        await createError(request, testId, 'HandlerTest');

        try {
            await searchByField(page, 'errorCode', testId);

            // 핸들러 버튼 클릭 (행 내 '핸들러' 버튼)
            const handlerPromise = page.waitForResponse(r =>
                r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId })
                .getByRole('button', { name: '핸들러' }).click();
            await handlerPromise;

            await expect(page.locator('#handlerModal')).toBeVisible();
            // 모달 본문에 오류코드와 제목이 표시되어야 한다
            await expect(page.locator('#handlerErrorInfo')).toContainText(testId);

            await page.locator('#handlerModal').getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('핸들러를 추가하고 저장하면 성공 Toast가 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ehdlr-');
        await createError(request, testId, 'HandlerSaveTest');

        try {
            await searchByField(page, 'errorCode', testId);

            const handlerPromise = page.waitForResponse(r =>
                r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId })
                .getByRole('button', { name: '핸들러' }).click();
            await handlerPromise;

            await expect(page.locator('#handlerModal')).toBeVisible();

            // 사용 가능한 핸들러가 있으면 추가
            const availableOptions = page.locator('#availableHandlers option');
            const optionCount = await availableOptions.count();

            if (optionCount > 0) {
                await page.locator('#availableHandlers').selectOption({ index: 0 });
                await page.locator('#handlerParam').fill('testParam');
                await page.locator('#handlerModal').getByRole('button', { name: LABEL.HANDLER_ADD }).click();

                // 할당 목록에 추가되었는지 확인
                const assignedOptions = page.locator('#assignedHandlers option');
                await expect(assignedOptions).not.toHaveCount(0);

                // 저장
                const savePromise = page.waitForResponse(r =>
                    r.url().includes(`/api/errors/${testId}/handle-apps`) && r.request().method() === 'PUT');
                await page.locator('#handlerModal').getByRole('button', { name: LABEL.SAVE_CHANGES }).click();
                await savePromise;

                await expect(page.locator('.toast')).toBeVisible();
            }
        } finally {
            await deleteError(request, testId);
        }
    });

    test('핸들러를 선택하지 않고 추가 버튼을 클릭하면 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ehdlr-');
        await createError(request, testId, 'HandlerValidTest');

        try {
            await searchByField(page, 'errorCode', testId);

            const handlerPromise = page.waitForResponse(r =>
                r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId })
                .getByRole('button', { name: '핸들러' }).click();
            await handlerPromise;

            await expect(page.locator('#handlerModal')).toBeVisible();

            // 선택 없이 추가 클릭
            await page.locator('#handlerModal').getByRole('button', { name: LABEL.HANDLER_ADD }).click();
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('#handlerModal').getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('핸들러를 선택하지 않고 제거 버튼을 클릭하면 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ehdlr-');
        await createError(request, testId, 'HandlerRemoveValidTest');

        try {
            await searchByField(page, 'errorCode', testId);

            const handlerPromise = page.waitForResponse(r =>
                r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId })
                .getByRole('button', { name: '핸들러' }).click();
            await handlerPromise;

            await expect(page.locator('#handlerModal')).toBeVisible();

            // 선택 없이 제거 클릭
            await page.locator('#handlerModal').getByRole('button', { name: LABEL.HANDLER_REMOVE }).click();
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('#handlerModal').getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteError(request, testId);
        }
    });

    test('할당된 핸들러를 제거하면 사용 가능 목록으로 돌아가야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-ehdlr-');
        await createError(request, testId, 'HandlerRemoveTest');

        try {
            await searchByField(page, 'errorCode', testId);

            const handlerPromise = page.waitForResponse(r =>
                r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId })
                .getByRole('button', { name: '핸들러' }).click();
            await handlerPromise;

            await expect(page.locator('#handlerModal')).toBeVisible();

            const availableOptions = page.locator('#availableHandlers option');
            const initialCount = await availableOptions.count();

            if (initialCount > 0) {
                // 핸들러 추가
                await page.locator('#availableHandlers').selectOption({ index: 0 });
                await page.locator('#handlerModal').getByRole('button', { name: LABEL.HANDLER_ADD }).click();

                // 할당 목록에서 선택 후 제거
                await page.locator('#assignedHandlers').selectOption({ index: 0 });
                await page.locator('#handlerModal').getByRole('button', { name: LABEL.HANDLER_REMOVE }).click();

                // 사용 가능 목록 수가 원래대로 복원되어야 한다
                await expect(availableOptions).toHaveCount(initialCount);
            }

            await page.locator('#handlerModal').getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteError(request, testId);
        }
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('오류코드 권한', () => {

    test('W 권한이 있는 사용자에게는 추가 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnAdd')).toBeVisible();
    });
});

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 추가 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/error-codes');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/errors/page'));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;
        await expect(page.getByRole('table')).toBeVisible();

        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장, 삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // beforeEach에서 이미 페이지 로드 + 검색 완료
        const firstRow = page.locator('#errorTableBody tr').first();
        await expect(firstRow).toBeVisible();

        const detailPromise = page.waitForResponse(r =>
            r.url().match(/\/api\/errors\/[^/]+$/) != null && r.request().method() === 'GET');
        await firstRow.click();
        await detailPromise;
        await expect(page.locator('#errorModal')).toBeVisible();

        await expect(page.locator('#btnSave')).not.toBeVisible();
        await expect(page.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('R 권한 사용자에게는 핸들러 모달의 추가, 제거, 변경사항저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // beforeEach에서 이미 페이지 로드 + 검색 완료
        const firstRow = page.locator('#errorTableBody tr').first();
        await expect(firstRow).toBeVisible();

        const handlerPromise = page.waitForResponse(r =>
            r.url().includes('/api/handle-apps') && r.request().method() === 'GET');
        await firstRow.getByRole('button', { name: '핸들러' }).click();
        await handlerPromise;

        await expect(page.locator('#handlerModal')).toBeVisible();

        // W 권한 전용 영역이 숨겨져야 한다
        await expect(page.locator('.sp-handler-actions')).not.toBeVisible();
        await expect(page.locator('#handlerModal').getByRole('button', { name: LABEL.SAVE_CHANGES })).not.toBeVisible();

        await page.locator('#handlerModal').getByRole('button', { name: LABEL.CLOSE }).click();
    });
});
