/**
 * 메뉴 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 * 초기 페이지 로드 시 데이터가 자동 조회되지 않으므로 각 테스트에서 명시적으로 조회한다.
 *
 * 주의:
 * - createMenu 서비스는 priorMenuId 필수 (null/빈값 → 400)
 * - 중복 체크 기준은 menuName (menuId가 아님)
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 기존 DB 메뉴 ID (priorMenuId 필수 제약 해소용) ─────────

let validParentMenuId: string | null = null;

test.beforeAll(async ({ request }) => {
    const res = await request.get('/api/menus/page', { params: { page: 1, size: 1 } });
    if (res.ok()) {
        const body = await res.json();
        validParentMenuId = body.data?.content?.[0]?.menuId ?? null;
    }
});

// ─── 헬퍼 함수 ───────────────────────────────────────────────

async function createMenu(request: APIRequestContext, menuId: string, menuName: string) {
    const res = await request.post('/api/menus', {
        data: {
            menuId,
            menuName,
            menuUrl: '/e2e-test/' + menuId,
            sortOrder: 0,
            displayYn: 'Y',
            useYn: 'Y',
            priorMenuId: validParentMenuId,
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteMenu(request: APIRequestContext, menuId: string) {
    const res = await request.delete(`/api/menus/${menuId}`);
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete menu ${menuId}: ${res.status()}`);
    }
}

async function searchByMenuId(page: Page, menuId: string) {
    await page.locator('#searchField').selectOption('menuId');
    await page.locator('#searchValue').fill(menuId);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/menus/page'));
    await page.locator('#btnSearch').click();
    await responsePromise;
}

async function loadMenus(page: Page) {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/menus/page'));
    await page.locator('#btnSearch').click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

// ─── 공통 setup ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/menus');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────────

test.describe('메뉴 목록', () => {

    test('조회 버튼 클릭 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        await loadMenus(page);

        const rows = page.locator('#menuTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('메뉴ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        await createMenu(request, id, 'SearchMenu' + id);

        try {
            await searchByMenuId(page, id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteMenu(request, id);
        }
    });

    test('메뉴명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        const uniqueName = 'MenuSearch' + id;
        await createMenu(request, id, uniqueName);

        try {
            await page.locator('#searchField').selectOption('menuName');
            await page.locator('#searchValue').fill(uniqueName);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/menus/page'));
            await page.locator('#btnSearch').click();
            await responsePromise;

            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteMenu(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        await createMenu(request, id, 'SortMenu' + id);

        try {
            await loadMenus(page);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/menus/page'));
            await page.getByRole('columnheader', { name: LABEL.MENU_ID_COLUMN }).click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/menus/page'));
            await page.getByRole('columnheader', { name: LABEL.MENU_ID_COLUMN }).click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteMenu(request, id);
        }
    });

    test('검색어 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        await createMenu(request, id, 'EnterKeyMenu' + id);

        try {
            await page.locator('#searchField').selectOption('menuId');
            await page.locator('#searchValue').fill(id);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/menus/page'));
            await page.locator('#searchValue').press('Enter');
            await responsePromise;

            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteMenu(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        await createMenu(request, id, 'PrintMenu' + id);

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await expect(page.getByRole('table')).toBeVisible();
            await loadMenus(page);

            const popupPromise = context.waitForEvent('page');
            await page.locator('#btnPrint').click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteMenu(request, id);
        }
    });

    test('데이터 행을 클릭하면 수정 모달이 열려야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const id = generateTestId('e2e-mls-');
        await createMenu(request, id, 'RowClickMenu' + id);

        try {
            await searchByMenuId(page, id);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/menus/${id}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;

            await expect(page.getByRole('dialog')).toBeVisible();
            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMenu(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────────

test.describe('메뉴 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.MENU_CREATE_TITLE)).toBeVisible();

        // 등록 모드: menuId 입력 가능, 삭제 버튼 숨김
        await expect(page.locator('#modalMenuId')).toHaveValue('');
        await expect(page.locator('#modalMenuId')).toBeEnabled();
        await expect(page.locator('#btnDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });

    test('모달에서 메뉴를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const testId = generateTestId('e2e-mcr-');

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalMenuId').fill(testId);
            await page.locator('#modalMenuName').fill('E2E생성메뉴' + testId);
            await page.locator('#modalMenuUrl').fill('/e2e-test/' + testId);
            await page.locator('#modalPriorMenuId').fill(validParentMenuId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/menus') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByMenuId(page, testId);
            await expect(page.getByRole('cell', { name: testId, exact: true })).toBeVisible();
        } finally {
            await deleteMenu(request, testId);
        }
    });

    test('중복된 menuName으로 생성할 경우 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const testId = generateTestId('e2e-mcr-');
        const dupId = generateTestId('e2e-mcr-');
        const sameName = 'DupTarget' + testId;
        await createMenu(request, testId, sameName);

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            // 다른 menuId, 같은 menuName → 409
            await page.locator('#modalMenuId').fill(dupId);
            await page.locator('#modalMenuName').fill(sameName);
            await page.locator('#modalMenuUrl').fill('/e2e-dup/' + dupId);
            await page.locator('#modalPriorMenuId').fill(validParentMenuId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/menus') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();
            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMenu(request, testId);
        }
    });

    test('필수 항목 없이 저장하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // menuId 비운 채로 저장 → 클라이언트 검증 Toast
        await page.getByRole('button', { name: LABEL.SAVE }).click();

        await expect(page.locator('.toast')).toBeVisible();
        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 PK(메뉴ID)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const testId = generateTestId('e2e-mcr-');
        await createMenu(request, testId, '수정대상메뉴' + testId);

        try {
            await searchByMenuId(page, testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/menus/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;

            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.MENU_EDIT_TITLE)).toBeVisible();

            // PK(메뉴ID)는 수정 불가
            await expect(page.locator('#modalMenuId')).toBeDisabled();
            await expect(page.locator('#modalMenuId')).toHaveValue(testId);

            // 삭제 버튼이 표시되어야 한다
            await expect(page.locator('#btnDelete')).toBeVisible();

            await page.locator('[data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteMenu(request, testId);
        }
    });

    test('메뉴명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const testId = generateTestId('e2e-mcr-');
        await createMenu(request, testId, '수정전메뉴명' + testId);

        try {
            await searchByMenuId(page, testId);
            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/menus/${testId}`) && r.request().method() === 'GET');
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalMenuName').fill('수정후메뉴명' + testId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/menus/${testId}`) && r.request().method() === 'PUT');
            await page.getByRole('button', { name: LABEL.SAVE }).click();
            await responsePromise;

            await expect(page.getByRole('dialog')).not.toBeVisible();
            await searchByMenuId(page, testId);
            await expect(page.getByRole('cell', { name: '수정후메뉴명' + testId })).toBeVisible();
        } finally {
            await deleteMenu(request, testId);
        }
    });

    test('메뉴를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        if (!validParentMenuId) { test.skip(); return; }
        const testId = generateTestId('e2e-mcr-');
        await createMenu(request, testId, '삭제대상메뉴' + testId);

        await searchByMenuId(page, testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/menus/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.getByRole('dialog')).toBeVisible();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/menus/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#btnDelete').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await page.locator('#spConfirmModal').waitFor({ state: 'hidden' });
        await expect(page.getByRole('dialog')).not.toBeVisible();
        await searchByMenuId(page, testId);
        await expect(page.getByRole('cell', { name: testId, exact: true })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});

// ─── 권한 ────────────────────────────────────────────────────

test.describe('메뉴 권한', () => {

    test('W 권한이 있는 사용자에게는 등록 버튼이 표시되어야 한다', async ({ page }) => {
        // e2e-admin은 MENU:W 권한 보유 (e2e-seed.sql: v3_menu_manage W)
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).toBeVisible();
    });
});
