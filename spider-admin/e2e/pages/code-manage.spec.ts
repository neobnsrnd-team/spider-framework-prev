/**
 * 코드 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
/** 코드: @Size(max=50) 제약 → 여유 있음 */
function genCodeId() { return 'C' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0'); }

async function createCodeGroup(
    request: APIRequestContext,
    codeGroupId: string,
    codeGroupName: string,
) {
    const res = await request.post('/api/code-groups/with-codes', {
        data: { codeGroupId, codeGroupName, codeGroupDesc: 'E2E 테스트용', codes: [] },
    });
    expect(res.status()).toBe(201);
}

async function deleteCodeGroup(request: APIRequestContext, codeGroupId: string) {
    const res = await request.delete(`/api/code-groups/${codeGroupId}/with-codes`);
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete code group ${codeGroupId}: ${res.status()}`);
    }
}

async function createCode(
    request: APIRequestContext,
    codeGroupId: string,
    code: string,
    codeName: string,
) {
    const res = await request.post('/api/codes', {
        data: { codeGroupId, code, codeName, codeEngname: code, sortOrder: 0, useYn: 'Y' },
    });
    expect(res.status()).toBe(201);
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('[id$="_searchField"]').selectOption(field);
    await page.locator('[id$="_searchValue"]').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

// ─── 테스트 ──────────────────────────────────────────────

/** 각 테스트에서 사용할 코드그룹 — 테스트 전체에서 공유. */
const SHARED_GROUP = 'E2ECMGRP';
const SHARED_GROUP_NAME = 'E2E코드관리';

test.beforeAll(async ({ request }) => {
    // 이전 테스트 잔여 데이터 정리 후 공유 코드그룹 생성
    await deleteCodeGroup(request, SHARED_GROUP);
    await createCodeGroup(request, SHARED_GROUP, SHARED_GROUP_NAME);
});

test.afterAll(async ({ request }) => {
    // R-only storageState에서도 실행될 수 있어 403 무시
    await request.delete(`/api/code-groups/${SHARED_GROUP}/with-codes`);
});

test.beforeEach(async ({ page }) => {
    await page.goto('/codes');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('코드 관리 목록', () => {

    test('초기 페이지 로드 시 데이터가 표시되어야 한다', async ({ page }) => {
        const rows = page.locator('#tableContainer tbody tr');
        await expect(rows.first()).toBeVisible();
    });

    test('코드로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'SearchCode');

        try {
            await searchByField(page, 'code', codeId);
            await expect(page.getByRole('cell', { name: codeId, exact: true })).toBeVisible();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('코드명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        const uniqueName = 'SearchName' + codeId;
        await createCode(request, SHARED_GROUP, codeId, uniqueName);

        try {
            await searchByField(page, 'name', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'PageResetTest');

        try {
            await searchByField(page, 'code', codeId);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        const sortHeader = page.locator('th[data-sort="codeGroupId"]');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
        await sortHeader.click();
        await responsePromise;
        await expect(page.locator('#tableContainer tbody tr').first()).toBeVisible();

        const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
        await sortHeader.click();
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
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            const loadPromise = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
            await page.reload();
            await loadPromise;
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('h3')).toHaveText('코드 목록');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('데이터 행을 클릭하면 상세 모달이 열려야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'RowClickTest');

        try {
            await searchByField(page, 'code', codeId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: codeId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('코드 관리 CRUD', () => {

    test('+ 추가 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.ADD }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.CODE_CREATE_TITLE)).toBeVisible();

        // 코드그룹 드롭다운이 로드되어야 한다
        await expect(page.locator('#codeGroupId option').first()).toBeAttached();
        // 코드 필드가 비어있어야 한다
        await expect(page.locator('#code')).toHaveValue('');

        // 생성 모달에서 삭제 버튼은 보이지 않아야 한다
        const dialog = page.getByRole('dialog');
        await expect(dialog.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(dialog).not.toBeVisible();
    });

    test('모달에서 코드를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const codeId = genCodeId();

        try {
            await page.getByRole('button', { name: LABEL.ADD }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            // 코드그룹 드롭다운에서 공유 그룹 선택
            await page.locator('#codeGroupId').selectOption(SHARED_GROUP);
            await page.locator('#code').fill(codeId);
            await page.locator('#codeName').fill('생성테스트코드');
            await page.locator('#codeEngname').fill(codeId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/codes') && r.request().method() === 'POST');
            await page.locator('#btnSave').click();
            await responsePromise;

            // 성공 Toast
            await expect(page.locator('.toast')).toBeVisible();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'code', codeId);
            await expect(page.getByRole('cell', { name: codeId, exact: true })).toBeVisible();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('중복된 코드를 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'DupTest');

        try {
            await page.getByRole('button', { name: LABEL.ADD }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#codeGroupId').selectOption(SHARED_GROUP);
            await page.locator('#code').fill(codeId);
            await page.locator('#codeName').fill('중복테스트');
            await page.locator('#codeEngname').fill(codeId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/codes') && r.request().method() === 'POST');
            await page.locator('#btnSave').click();
            await responsePromise;

            // 에러 Toast가 표시되어야 한다
            await expect(page.locator('.toast')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('유효하지 않은 값을 입력할 경우 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.ADD }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 필수 항목 비우고 저장 클릭
        await page.locator('#btnSave').click();

        // validation Toast가 표시되어야 한다
        await expect(page.locator('.toast')).toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행 클릭 시 PK가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, '수정대상');

        try {
            await searchByField(page, 'code', codeId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: codeId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.CODE_EDIT_TITLE)).toBeVisible();

            // PK 필드는 수정 불가
            await expect(page.locator('#codeGroupId')).toBeDisabled();
            await expect(page.locator('#code')).toHaveAttribute('readonly', '');

            // 삭제 버튼 표시
            const dialog = page.getByRole('dialog');
            await expect(dialog.locator('#btnDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('코드명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'BeforeName');

        try {
            await searchByField(page, 'code', codeId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: codeId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#codeName').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'PUT');
            await page.locator('#btnSave').click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByField(page, 'code', codeId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });

    test('코드를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'DeleteMe');

        await searchByField(page, 'code', codeId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: codeId }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/codes/${SHARED_GROUP}/${codeId}`) && r.request().method() === 'DELETE');
        await page.locator('#btnDelete').click();
        // Toast.confirm 모달의 확인 버튼 클릭
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('#codeModal')).not.toBeVisible();
        await searchByField(page, 'code', codeId);
        await expect(page.getByRole('cell', { name: codeId, exact: true })).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('코드 관리 권한', () => {

    test('W 권한이 있는 사용자에게는 + 추가 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.ADD })).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────

test.describe('코드 관리 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
        await page.goto('/codes');
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

        await dataRows.first().click();
        await expect(page.getByRole('dialog')).toBeVisible({ timeout: 10_000 });

        await expect(page.locator('#btnSave')).not.toBeVisible();
        await expect(page.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });
});

// ─── 추가 기능 ────────────────────────────────────────────

test.describe('코드 관리 추가 기능', () => {

    test('검색어 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        const codeId = genCodeId();
        await createCode(request, SHARED_GROUP, codeId, 'EnterKeyTest');

        try {
            await page.locator('[id$="_searchField"]').selectOption('code');
            await page.locator('[id$="_searchValue"]').fill(codeId);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/codes/page-with-group'));
            await page.locator('[id$="_searchValue"]').press('Enter');
            await responsePromise;

            await expect(page.getByRole('cell', { name: codeId, exact: true })).toBeVisible();
        } finally {
            await request.delete(`/api/codes/${SHARED_GROUP}/${codeId}`);
        }
    });
});
