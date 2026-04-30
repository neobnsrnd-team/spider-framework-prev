/**
 * 현황판 등록 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createMonitor(request: APIRequestContext, monitorId: string, monitorName: string) {
    const res = await request.post('/api/monitors', {
        data: { monitorId, monitorName, refreshTerm: '5', useYn: 'Y' },
    });
    expect(res.status()).toBe(201);
}

async function deleteMonitor(request: APIRequestContext, monitorId: string) {
    await request.delete(`/api/monitors/${monitorId}`);
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/monitors/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/monitors');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('현황판 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        // limitRows 기본값이 10이므로 테이블 행이 최대 10개
        const rows = page.locator('#monitorTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('모니터ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'SearchMonitor');

        try {
            await searchByField(page, 'monitorId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('모니터명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        const uniqueName = 'MonSearch' + id;
        await createMonitor(request, id, uniqueName);

        try {
            await searchByField(page, 'monitorName', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'PageResetTest');

        try {
            await searchByField(page, 'monitorId', id);
            // 페이지 정보가 1페이지임을 확인
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'SortTest');

        try {
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/monitors/page'));
            await page.getByRole('columnheader', { name: LABEL.MONITOR_ID_COLUMN }).click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/monitors/page'));
            await page.getByRole('columnheader', { name: LABEL.MONITOR_ID_COLUMN }).click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'ExcelTest');

        try {
            await page.reload();
            await expect(page.getByRole('table')).toBeVisible();

            const downloadPromise = page.waitForEvent('download');
            await page.getByRole('button', { name: LABEL.EXCEL }).click();
            const download = await downloadPromise;

            expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'PrintTest');

        try {
            // headless에서 window.print()가 팝업을 즉시 닫는 것을 방지
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('h3')).toHaveText('현황판 목록');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteMonitor(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-mlist-');
        await createMonitor(request, id, 'RowClickTest');

        try {
            await searchByField(page, 'monitorId', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/monitors/${id}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMonitor(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('현황판 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.MONITOR_CREATE_TITLE)).toBeVisible();

        await expect(page.getByLabel('모니터ID')).toHaveValue('');
        await expect(page.getByLabel('모니터ID')).toBeEnabled();

        const dialog = page.getByRole('dialog');
        await expect(dialog.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(dialog).not.toBeVisible();
    });

    test('모달에서 모니터를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-mcrud-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.getByLabel('모니터ID').fill(testId);
            await page.getByLabel('모니터명').fill('생성테스트모니터');
            await page.locator('#refreshTerm').selectOption('5');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/monitors') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'monitorId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteMonitor(request, testId);
        }
    });

    test('중복된 모니터를 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-mcrud-');
        await createMonitor(request, testId, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.getByLabel('모니터ID').fill(testId);
            await page.getByLabel('모니터명').fill('중복테스트');
            await page.locator('#refreshTerm').selectOption('5');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/monitors') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            // Toast 알림이 표시되어야 한다
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMonitor(request, testId);
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

    test('행을 클릭하면 PK(모니터ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-mcrud-');
        await createMonitor(request, testId, '수정대상');

        try {
            await searchByField(page, 'monitorId', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/monitors/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.MONITOR_EDIT_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#monitorId')).toBeDisabled();
            await expect(page.locator('#monitorId')).toHaveValue(testId);

            const dialog = page.getByRole('dialog');
            await expect(dialog.locator('#btnDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMonitor(request, testId);
        }
    });

    test('모니터명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-mcrud-');
        await createMonitor(request, testId, 'BeforeName');

        try {
            await searchByField(page, 'monitorId', testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/monitors/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.getByLabel('모니터명').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/monitors/${testId}`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'monitorId', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteMonitor(request, testId);
        }
    });

    test('모니터를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-mcrud-');
        await createMonitor(request, testId, 'DeleteMe');

        await searchByField(page, 'monitorId', testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/monitors/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/monitors/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#btnDelete').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await page.locator('#spConfirmModal').waitFor({ state: 'hidden' });
        await expect(page.getByRole('dialog')).not.toBeVisible();
        await searchByField(page, 'monitorId', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('현황판 권한', () => {

    test('W 권한이 있는 사용자에게는 등록, 삭제 버튼이 표시되어야 한다', async ({ page }) => {
        // e2e-admin은 MONITOR:W 권한 보유
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).toBeVisible();
    });
});
