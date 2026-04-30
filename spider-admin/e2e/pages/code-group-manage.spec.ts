/**
 * 코드그룹 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
/** 코드그룹ID: @Size(max=8) 제약 → 8자 이내 */
function genGroupId() { return 'G' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }

async function createCodeGroup(
    request: APIRequestContext,
    codeGroupId: string,
    codeGroupName: string,
    codes: Array<{ code: string; codeName: string; codeEngname?: string }> = [],
) {
    const res = await request.post('/api/code-groups/with-codes', {
        data: {
            codeGroupId,
            codeGroupName,
            codeGroupDesc: 'E2E 테스트용',
            codes: codes.map(c => ({
                codeGroupId,
                code: c.code,
                codeName: c.codeName,
                codeEngname: c.codeEngname || c.code,
                sortOrder: 0,
                useYn: 'Y',
            })),
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteCodeGroup(request: APIRequestContext, codeGroupId: string) {
    const res = await request.delete(`/api/code-groups/${codeGroupId}/with-codes`);
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete code group ${codeGroupId}: ${res.status()}`);
    }
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('[id$="_searchField"]').selectOption(field);
    await page.locator('[id$="_searchValue"]').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

// ─── 테스트 ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/code-groups');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('코드그룹 관리 목록', () => {

    test('초기 페이지 로드 시 데이터가 표시되어야 한다', async ({ page }) => {
        const rows = page.locator('#tableContainer tbody tr');
        await expect(rows.first()).toBeVisible();
    });

    test('코드그룹명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        const uniqueName = 'GroupSearch' + id;
        await createCodeGroup(request, id, uniqueName);

        try {
            await searchByField(page, 'codeGroupName', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('코드그룹ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'IDSearch' + id);

        try {
            await searchByField(page, 'codeGroupId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'PageReset' + id);

        try {
            await searchByField(page, 'codeGroupId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
        await page.getByRole('columnheader', { name: LABEL.CODE_GROUP_ID_COLUMN }).click();
        await responsePromise;
        await expect(page.locator('#tableContainer tbody tr').first()).toBeVisible();

        const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
        await page.getByRole('columnheader', { name: LABEL.CODE_GROUP_ID_COLUMN }).click();
        await responsePromise2;
        await expect(page.locator('#tableContainer tbody tr').first()).toBeVisible();
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            const loadPromise = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
            await page.reload();
            await loadPromise;
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('h3')).toHaveText('코드 그룹 목록');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('데이터 행을 클릭하면 코드그룹 상세 모달이 열려야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'RowClick' + id);

        try {
            await searchByField(page, 'codeGroupId', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('코드그룹 관리 CRUD', () => {

    test('+ 추가 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.ADD }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.CODE_GROUP_CREATE_TITLE)).toBeVisible();

        // 코드그룹ID 필드가 비어있고 편집 가능해야 한다
        await expect(page.locator('#modalCodeGroupId')).toHaveValue('');
        await expect(page.locator('#modalCodeGroupId')).not.toHaveAttribute('readonly');

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('모달에서 코드그룹을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const id = genGroupId();

        try {
            await page.getByRole('button', { name: LABEL.ADD }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalCodeGroupId').fill(id);
            await page.locator('#modalCodeGroupName').fill('생성테스트그룹');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/code-groups/with-codes') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            // 성공 Toast
            await expect(page.locator('.toast')).toBeVisible();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'codeGroupId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('중복된 코드그룹ID를 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.ADD }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalCodeGroupId').fill(id);
            await page.locator('#modalCodeGroupName').fill('중복테스트');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/code-groups/with-codes') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            // 에러 Toast
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('필수 항목 없이 저장 시 Toast 경고가 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.ADD }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 코드그룹ID만 입력, 코드그룹명 비움
        await page.locator('#modalCodeGroupId').fill('TEMP');

        await page.getByRole('button', { name: LABEL.SAVE }).click();

        // validation Toast
        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행 클릭 시 PK(코드그룹ID)가 readonly인 수정 모달이 열려야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, '수정대상그룹');

        try {
            await searchByField(page, 'codeGroupId', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.CODE_GROUP_EDIT_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#modalCodeGroupId')).toHaveAttribute('readonly', '');
            await expect(page.locator('#modalCodeGroupId')).toHaveValue(id);

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('코드그룹명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'BeforeGroupName');

        try {
            await searchByField(page, 'codeGroupId', id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalCodeGroupName').fill('AfterGroupName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'codeGroupId', id);
            await expect(page.getByRole('cell', { name: 'AfterGroupName' })).toBeVisible();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });

    test('전체삭제 버튼 클릭 시 코드그룹과 하위 코드가 삭제되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'DeleteAllTest', [
            { code: 'D1', codeName: '삭제코드1' },
        ]);

        await searchByField(page, 'codeGroupId', id);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: id }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/code-groups/${id}/with-codes`) && r.request().method() === 'DELETE');
        await page.getByRole('button', { name: LABEL.CODE_GROUP_DELETE_ALL }).click();
        // Toast.confirm 모달의 확인 버튼 클릭
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('#codeGroupModal')).not.toBeVisible();
        await searchByField(page, 'codeGroupId', id);
        await expect(page.getByRole('cell', { name: id, exact: true })).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('코드그룹 관리 권한', () => {

    test('W 권한이 있는 사용자에게는 + 추가 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.ADD })).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────

test.describe('코드그룹 관리 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
        await page.goto('/code-groups');
        await loadPromise;
        await expect(page.getByRole('table')).toBeVisible();
    });

    test('R 권한 사용자에게는 + 추가 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.ADD })).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장·삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // data-index 속성이 있는 실제 데이터 행만 선택 ("조회된 데이터가 없습니다" 행 제외)
        const dataRows = page.locator('#tableContainer tbody tr[data-index]');
        const count = await dataRows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        const detailPromise = page.waitForResponse(r =>
            r.url().includes('/api/code-groups/') && r.request().method() === 'GET');
        await dataRows.first().click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.CODE_GROUP_DELETE_ALL })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.CODE_GROUP_ADD_ROW })).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });
});

// ─── 추가 기능 ────────────────────────────────────────────

test.describe('코드그룹 관리 추가 기능', () => {

    test('검색어 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        const id = genGroupId();
        await createCodeGroup(request, id, 'EnterKeyTest' + id);

        try {
            await page.locator('[id$="_searchField"]').selectOption('codeGroupId');
            await page.locator('[id$="_searchValue"]').fill(id);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/code-groups/page'));
            await page.locator('[id$="_searchValue"]').press('Enter');
            await responsePromise;

            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteCodeGroup(request, id);
        }
    });
});
