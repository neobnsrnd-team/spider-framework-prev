/**
 * 배치 APP 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createBatchApp(request: APIRequestContext, batchAppId: string, batchAppName: string) {
    const res = await request.post('/api/batch/apps', {
        data: {
            batchAppId,
            batchAppName,
            batchAppFileName: batchAppId + '.jar',
            batchCycle: 'D',
            retryableYn: 'Y',
            perWasYn: 'N',
            importantType: '1',
        },
    });
    expect(res.status()).toBe(201);
}

let wasInstanceSeq = 0;
function createWasInstanceId() {
    return 'T' + ((Date.now() + wasInstanceSeq++) % 46656).toString(36).padStart(3, '0').toUpperCase();
}

async function createWasInstance(request: APIRequestContext, instanceId: string, instanceName = 'ExecTestInstance') {
    const res = await request.post('/api/was/instance', {
        data: {
            instanceId,
            instanceName,
            instanceDesc: 'E2E batch exec test instance',
            wasConfigId: 'E2E',
            ip: '127.0.0.1',
            port: '8080',
            instanceType: '1',
            operModeType: 'D',
        },
    });
    expect(res.status(), await res.text()).toBe(201);
}

async function deleteBatchApp(request: APIRequestContext, batchAppId: string) {
    const res = await request.delete(`/api/batch/apps/${batchAppId}`);
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete batch app ${batchAppId}: ${res.status()}`);
    }
}

async function deleteWasInstance(request: APIRequestContext, instanceId: string) {
    const res = await request.delete(`/api/was/instance/${instanceId}`);
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete WAS instance ${instanceId}: ${res.status()}`);
    }
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/batch/apps/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/batches/apps');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('배치 APP 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#batchAppTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('배치 APP ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'SearchBatch');

        try {
            await searchByField(page, 'batchAppId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('배치 APP명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        const uniqueName = 'BatchSearch' + id;
        await createBatchApp(request, id, uniqueName);

        try {
            await searchByField(page, 'batchAppName', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'PageResetTest');

        try {
            await searchByField(page, 'batchAppId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'SortTest');

        try {
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/batch/apps/page'));
            await page.getByRole('columnheader', { name: LABEL.BATCH_APP_ID_COLUMN }).click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/batch/apps/page'));
            await page.getByRole('columnheader', { name: LABEL.BATCH_APP_ID_COLUMN }).click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'ExcelTest');

        try {
            await page.reload();
            await expect(page.getByRole('table')).toBeVisible();

            const downloadPromise = page.waitForEvent('download');
            await page.getByRole('button', { name: LABEL.EXCEL }).click();
            const download = await downloadPromise;

            expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'RowClickTest');

        try {
            await searchByField(page, 'batchAppId', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/batch/apps/${id}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteBatchApp(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('배치 APP CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.BATCH_APP_CREATE_TITLE)).toBeVisible();

        await expect(page.locator('#modalBatchAppId')).toHaveValue('');
        await expect(page.locator('#modalBatchAppId')).toBeEnabled();

        // 등록 모달에서는 삭제 버튼이 보이지 않아야 한다
        await expect(page.locator('#btnDeleteBatchApp')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });

    test('모달에서 배치 APP을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-bcrud-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalBatchAppId').fill(testId);
            await page.locator('#modalBatchAppName').fill('생성테스트배치');
            await page.locator('#modalBatchAppFileName').fill(testId + '.jar');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/batch/apps') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'batchAppId', testId);
            await expect(page.getByRole('cell', { name: testId, exact: true })).toBeVisible();
        } finally {
            await deleteBatchApp(request, testId);
        }
    });

    test('중복된 배치 APP을 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-bcrud-');
        await createBatchApp(request, testId, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalBatchAppId').fill(testId);
            await page.locator('#modalBatchAppName').fill('중복테스트');
            await page.locator('#modalBatchAppFileName').fill(testId + '.jar');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/batch/apps') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteBatchApp(request, testId);
        }
    });

    test('유효하지 않은 값을 입력할 경우 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 필수 항목 비우고 저장 클릭
        await page.getByRole('button', { name: LABEL.SAVE }).click();

        // validation Toast가 표시되어야 한다
        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(배치 APP ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-bcrud-');
        await createBatchApp(request, testId, '수정대상');

        try {
            await searchByField(page, 'batchAppId', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/batch/apps/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.BATCH_APP_EDIT_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#modalBatchAppId')).toBeDisabled();
            await expect(page.locator('#modalBatchAppId')).toHaveValue(testId);

            // 삭제 버튼 표시
            await expect(page.locator('#btnDeleteBatchApp')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteBatchApp(request, testId);
        }
    });

    test('배치 APP명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-bcrud-');
        await createBatchApp(request, testId, 'BeforeName');

        try {
            await searchByField(page, 'batchAppId', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/batch/apps/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalBatchAppName').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/batch/apps/${testId}`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'batchAppId', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteBatchApp(request, testId);
        }
    });

    test('배치 APP을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-bcrud-');
        await createBatchApp(request, testId, 'DeleteMe');

        await searchByField(page, 'batchAppId', testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/batch/apps/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/batch/apps/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#btnDeleteBatchApp').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('#batchAppModal')).not.toBeVisible({ timeout: 10_000 });
        await searchByField(page, 'batchAppId', testId);
        await expect(page.getByRole('cell', { name: testId, exact: true })).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('배치 APP 권한', () => {

    test('W 권한이 있는 사용자에게는 등록 버튼이 표시되어야 한다', async ({ page }) => {
        // e2e-admin은 BATCH_APP:W 권한 보유
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).toBeVisible();
    });
});

// ─── 추가 기능 ────────────────────────────────────────────

test.describe('배치 APP 추가 기능', () => {

    test('검색어 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'EnterKeyTest');

        try {
            await page.locator('#searchField').selectOption('batchAppId');
            await page.locator('#searchValue').fill(id);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/batch/apps/page'));
            await page.locator('#searchValue').press('Enter');
            await responsePromise;

            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('수동실행 버튼 클릭 시 실행 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-blist-');
        await createBatchApp(request, id, 'ExecTest');

        try {
            await searchByField(page, 'batchAppId', id);

            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/batch/apps/${id}`) && r.request().method() === 'GET');
            await page.locator('.btn-manual-run').first().click();
            await detailPromise;

            await expect(page.locator('#batchExecModal')).toBeVisible();
            await expect(page.locator('#execBatchAppId')).toContainText(id);

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
        }
    });
});

// ─── 배치 실행 모달 ──────────────────────────────────────────

test.describe('배치 실행 모달', () => {

    /** E2E1 인스턴스는 e2e-seed.sql에서 사전 생성됨 */
    async function assignInstance(request: APIRequestContext, batchAppId: string, instanceId: string) {
        const res = await request.post(`/api/batch/apps/${batchAppId}/was/instance`, {
            params: { instanceId },
        });
        expect(res.status(), await res.text()).toBe(201);
    }

    async function openExecModal(page: Page, request: APIRequestContext, batchAppId: string) {
        await searchByField(page, 'batchAppId', batchAppId);

        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/batch/apps/${batchAppId}`) && r.request().method() === 'GET');
        await page.locator('.btn-manual-run').first().click();
        await detailPromise;

        await expect(page.locator('#batchExecModal')).toBeVisible();
    }

    test('실행 모달에 배치프로그램 정보가 올바르게 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        await createBatchApp(request, id, 'ExecInfoTest');

        try {
            await openExecModal(page, request, id);

            await expect(page.locator('#execBatchAppId')).toContainText(id);
            await expect(page.locator('#execBatchAppName')).toContainText('ExecInfoTest');
            await expect(page.locator('#execBatchFileName')).toContainText(id + '.jar');

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('실행 모달 오픈 시 기준일이 오늘 날짜로 기본 설정되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        await createBatchApp(request, id, 'DateDefaultTest');

        try {
            await openExecModal(page, request, id);

            const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD
            await expect(page.locator('#execBatchDate')).toHaveValue(today);

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('WAS 인스턴스가 할당된 배치 APP의 실행 모달에 인스턴스 체크박스가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'InstanceCheckTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            const instanceCheckbox = page.locator('.instance-checkbox-item').filter({ hasText: instanceId });
            await expect(instanceCheckbox).toBeVisible();

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('WAS 인스턴스가 없는 배치 APP의 실행 모달에 빈 상태 메시지가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        await createBatchApp(request, id, 'NoInstanceTest');

        try {
            await openExecModal(page, request, id);

            await expect(page.locator('#execInstanceList .sp-empty-state')).toBeVisible();

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
        }
    });

    test('전체선택 체크박스를 클릭하면 모든 인스턴스가 선택되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'SelectAllTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            await page.locator('#execSelectAll').check();

            const checkboxes = page.locator('.instance-checkbox');
            const count = await checkboxes.count();
            expect(count).toBeGreaterThan(0);
            for (let i = 0; i < count; i++) {
                await expect(checkboxes.nth(i)).toBeChecked();
            }

            // 전체선택 해제
            await page.locator('#execSelectAll').uncheck();
            for (let i = 0; i < count; i++) {
                await expect(checkboxes.nth(i)).not.toBeChecked();
            }

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('기준일 미입력 시 실행하면 Toast 경고가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'NoDateTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            // 기준일 비우기
            await page.locator('#execBatchDate').fill('');

            await page.locator('#btnExecuteBatch').click();

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('인스턴스 미선택 시 실행하면 Toast 경고가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'NoInstanceSelectTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            // 인스턴스 선택하지 않고 실행
            await page.locator('#btnExecuteBatch').click();

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('인스턴스를 선택하고 실행하면 confirm 대화상자가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'ConfirmTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            // 인스턴스 선택
            await page.locator('#execSelectAll').check();

            await page.locator('#btnExecuteBatch').click();

            // confirm 모달에 배치 APP ID가 포함되어야 한다
            await expect(page.locator('#spConfirmModal')).toBeVisible();
            await expect(page.locator('#spConfirmModalMsg')).toContainText(id);
            await page.locator('#spConfirmModal [data-bs-dismiss="modal"]').first().click();

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('실행 확인 시 API 호출이 발생하고 모달이 닫혀야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'ExecSuccessTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            // 인스턴스 선택
            await page.locator('#execSelectAll').check();

            const execPromise = page.waitForResponse(r =>
                r.url().includes('/api/batch/exec') && r.request().method() === 'POST');
            await page.locator('#btnExecuteBatch').click();
            await page.locator('#spConfirmModalOk').click();
            const execResponse = await execPromise;

            // WAS TCP 연결 성공 시 201, 실패(CI 환경 등 WAS 미기동) 시 200
            expect([200, 201]).toContain(execResponse.status());

            const execBody = await execResponse.json();
            if (execBody.success) {
                // 성공 시: 모달이 닫혀야 한다
                await expect(page.locator('#batchExecModal')).not.toBeVisible();
            } else {
                // 실패 시(CI — WAS 미기동): 모달이 유지되어야 한다
                await expect(page.locator('#batchExecModal')).toBeVisible();
            }

            // 성공/실패 모두 Toast가 표시되어야 한다 (2개 동시 노출 가능하므로 first() 사용)
            await expect(page.locator('.toast').first()).toBeVisible();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });

    test('인스턴스 체크박스 카드를 클릭하면 선택/해제가 토글되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-bexec-');
        const instanceId = createWasInstanceId();
        await createBatchApp(request, id, 'ToggleTest');
        await createWasInstance(request, instanceId);
        await assignInstance(request, id, instanceId);

        try {
            await openExecModal(page, request, id);

            const item = page.locator('.instance-checkbox-item').filter({ hasText: instanceId });
            const checkbox = item.locator('input[type="checkbox"]');

            // 초기 상태: 미선택
            await expect(checkbox).not.toBeChecked();

            // 카드 클릭 → 선택
            await item.click();
            await expect(checkbox).toBeChecked();
            await expect(item).toHaveClass(/selected/);

            // 다시 클릭 → 해제
            await item.click();
            await expect(checkbox).not.toBeChecked();
            await expect(item).not.toHaveClass(/selected/);

            await page.locator('#batchExecModal [data-bs-dismiss="modal"]').click();
        } finally {
            await deleteBatchApp(request, id);
            await deleteWasInstance(request, instanceId);
        }
    });
});
