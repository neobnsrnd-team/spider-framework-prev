/**
 * WAS 그룹 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createWasGroup(request: APIRequestContext, wasGroupId: string, wasGroupName: string) {
    const res = await request.post('/api/was/group', {
        data: { wasGroupId, wasGroupName, wasGroupDesc: wasGroupName + ' desc' },
    });
    expect(res.status()).toBe(201);
}

async function deleteWasGroup(request: APIRequestContext, wasGroupId: string) {
    await request.delete(`/api/was/group/${wasGroupId}`);
}

async function searchByGroupId(page: Page, value: string) {
    await page.locator('#groupId').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/was/group/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/was-groups');
    await page.waitForResponse(r => r.url().includes('/api/was/group/page'));
    await page.locator('#groupId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('WAS 그룹 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#wasGroupTableBody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('리스트 헤더와 엑셀/출력 버튼이 같은 줄에 표시되어야 한다', async ({ page }) => {
        const header = page.locator('.page-header').filter({ hasText: '리스트' });
        const headerBox = await header.boundingBox();
        const excelBtn = page.getByRole('button', { name: LABEL.EXCEL });
        const excelBox = await excelBtn.boundingBox();

        // 같은 줄: Y 좌표 차이가 작아야 한다
        expect(Math.abs(headerBox!.y - excelBox!.y)).toBeLessThan(30);
    });

    test('검색 라벨 텍스트가 줄바꿈 없이 한 줄로 표시되어야 한다', async ({ page }) => {
        const label = page.locator('.sp-search-field-group label').first();
        const box = await label.boundingBox();
        // 한 줄 라벨의 높이는 보통 20px 미만
        expect(box!.height).toBeLessThan(30);
    });

    test('WAS 그룹ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-wgl-');
        await createWasGroup(request, id, 'SearchGroup');

        try {
            await searchByGroupId(page, id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteWasGroup(request, id);
        }
    });

    test('WAS 그룹명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-wgl-');
        const uniqueName = 'WGSearch' + id;
        await createWasGroup(request, id, uniqueName);

        try {
            await page.locator('#groupName').fill(uniqueName);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/was/group/page'));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;

            await expect(page.getByRole('cell', { name: uniqueName, exact: true })).toBeVisible();
        } finally {
            await deleteWasGroup(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-wgl-');
        await createWasGroup(request, id, 'SortTest');

        try {
            const sortHeader = page.locator('#wasGroupTable thead th[data-sort="wasGroupId"]');

            // 1) 첫 클릭 → 오름차순 (sort-asc)
            const res1 = page.waitForResponse(r => r.url().includes('/api/was/group/page'));
            await sortHeader.click();
            await res1;
            await expect(sortHeader).toHaveClass(/sort-asc/);

            // 2) 두 번째 클릭 → 내림차순 (sort-desc)
            const res2 = page.waitForResponse(r => r.url().includes('/api/was/group/page'));
            await sortHeader.click();
            await res2;
            await expect(sortHeader).toHaveClass(/sort-desc/);

            // 3) 세 번째 클릭 → 정렬 해제 (sort-asc, sort-desc 둘 다 없어야 함)
            const res3 = page.waitForResponse(r => r.url().includes('/api/was/group/page'));
            await sortHeader.click();
            await res3;
            await expect(sortHeader).not.toHaveClass(/sort-asc/);
            await expect(sortHeader).not.toHaveClass(/sort-desc/);
        } finally {
            await deleteWasGroup(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2e-wgl-');
        await createWasGroup(request, id, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await page.waitForResponse(r => r.url().includes('/api/was/group/page'));

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteWasGroup(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-wgl-');
        await createWasGroup(request, id, 'RowClickTest');

        try {
            await searchByGroupId(page, id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/was/group/${id}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteWasGroup(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('WAS 그룹 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#wasGroupModal')).toBeVisible();

        await expect(page.locator('#modalGroupId')).toHaveValue('');
        await expect(page.locator('#modalGroupId')).not.toHaveAttribute('readonly');

        await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
        await expect(page.locator('#wasGroupModal')).not.toBeVisible();
    });

    test('모달에서 그룹을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-wgc-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            await page.locator('#modalGroupId').fill(testId);
            await page.locator('#modalGroupName').fill('생성테스트그룹');
            await page.locator('#modalGroupDesc').fill('생성 테스트 설명');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/was/group') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('#wasGroupModal')).not.toBeVisible();
            await searchByGroupId(page, testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteWasGroup(request, testId);
        }
    });

    test('중복된 그룹을 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-wgc-');
        await createWasGroup(request, testId, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            await page.locator('#modalGroupId').fill(testId);
            await page.locator('#modalGroupName').fill('중복테스트');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/was/group') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteWasGroup(request, testId);
        }
    });

    test('필수 항목을 비우고 저장하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#wasGroupModal')).toBeVisible();

        // 필수 항목 비우고 저장 클릭
        await page.getByRole('button', { name: LABEL.SAVE }).click();

        // validation Toast가 표시되어야 한다
        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(WAS 그룹 ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-wgc-');
        await createWasGroup(request, testId, '수정대상');

        try {
            await searchByGroupId(page, testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#modalGroupId')).toHaveAttribute('readonly');
            await expect(page.locator('#modalGroupId')).toHaveValue(testId);

            await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteWasGroup(request, testId);
        }
    });

    test('그룹명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-wgc-');
        await createWasGroup(request, testId, 'BeforeName');

        try {
            await searchByGroupId(page, testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            await page.locator('#modalGroupName').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('#wasGroupModal')).not.toBeVisible();
            await searchByGroupId(page, testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteWasGroup(request, testId);
        }
    });

    test('그룹을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-wgc-');
        await createWasGroup(request, testId, 'DeleteMe');

        await searchByGroupId(page, testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.locator('#wasGroupModal')).toBeVisible();

        // 삭제 버튼 클릭 → Toast.confirm 모달이 열림
        await page.locator('#wasGroupModal .btn-danger').click();
        await expect(page.locator('#spConfirmModal')).toBeVisible();

        // 확인 버튼 클릭
        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#spConfirmModal .btn-primary').click();
        await responsePromise;

        await expect(page.locator('#wasGroupModal')).not.toBeVisible();
        await searchByGroupId(page, testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/was-groups');
        await page.waitForResponse(r => r.url().includes('/api/was/group/page'));

        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const testId = generateTestId('e2e-wgro-');
        await createWasGroup(adminRequest, testId, 'ReadonlyTest');

        try {
            await searchByGroupId(page, testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/was/group/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#wasGroupModal')).toBeVisible();

            // 저장/삭제 버튼이 없어야 한다
            await expect(page.locator('#wasGroupModal').getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
            await expect(page.locator('#wasGroupModal').getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();

            await page.locator('#wasGroupModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteWasGroup(adminRequest, testId);
            await adminRequest.dispose();
        }
    });
});
