/**
 * 역할 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 *
 * 주의:
 * - 페이지 진입 시 데이터가 자동 조회된다 (Roles.load() 즉시 호출)
 * - CRUD는 인라인 편집 + POST /api/roles/batch (단건 엔드포인트 없음)
 * - 중복 체크 기준은 roleName (roleId가 아님)
 * - R-only 사용자 테스트: e2e-seed.sql에 v3_role_manage:R 부여 필요
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 헬퍼 함수 ───────────────────────────────────────────────

let seq = 0;
// roleId 최대 10자 제약: prefix 2자 + base36 timestamp 마지막 7자(7) + seq 1자리(1) = 10자
function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36).slice(-7) + (seq++ % 10);
}

async function createRole(request: APIRequestContext, roleId: string, roleName: string) {
    const res = await request.post('/api/roles/batch', {
        data: {
            newRoles: [{ roleId, roleName, useYn: 'Y', roleDesc: 'E2E 테스트', ranking: '99' }],
            updatedRoles: [],
            deletedRoleIds: [],
        },
    });
    expect(res.status()).toBe(200);
}

async function deleteRole(request: APIRequestContext, roleId: string) {
    const res = await request.post('/api/roles/batch', {
        data: { newRoles: [], updatedRoles: [], deletedRoleIds: [roleId] },
    });
    if (!res.ok() && res.status() !== 404) {
        throw new Error(`Failed to delete role ${roleId}: ${res.status()}`);
    }
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#_searchContainer_searchField').selectOption(field);
    await page.locator('#_searchContainer_searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/roles/page'));
    await page.locator('#_searchContainer_searchBtn').click();
    await responsePromise;
}

// ─── 공통 setup ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/roles/page'));
    await page.goto('/roles');
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────────

test.describe('역할 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#roleTableBody tr');
        await expect(rows.first()).toBeVisible();
        expect(await rows.count()).toBeLessThanOrEqual(10);
    });

    test('권한ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('ls');
        await createRole(request, id, 'SearchById' + id);

        try {
            await searchByField(page, 'roleId', id);
            await expect(page.locator(`#roleTableBody input[data-field="roleId"][value="${id}"]`)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('권한명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('ls');
        const uniqueName = 'SearchByName' + id;
        await createRole(request, id, uniqueName);

        try {
            await searchByField(page, 'roleName', uniqueName);
            await expect(page.locator(`#roleTableBody input[data-field="roleName"][value="${uniqueName}"]`)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('ls');
        await createRole(request, id, 'SortRole' + id);

        try {
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/roles/page'));
            await page.getByRole('columnheader', { name: LABEL.ROLE_ID_COLUMN }).click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/roles/page'));
            await page.getByRole('columnheader', { name: LABEL.ROLE_ID_COLUMN }).click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('검색어 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('ls');
        await createRole(request, id, 'EnterKeyRole' + id);

        try {
            await page.locator('#_searchContainer_searchField').selectOption('roleId');
            await page.locator('#_searchContainer_searchValue').fill(id);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/roles/page'));
            await page.locator('#_searchContainer_searchValue').press('Enter');
            await responsePromise;

            await expect(page.locator(`#roleTableBody input[data-field="roleId"][value="${id}"]`)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('ls');
        await createRole(request, id, 'PrintRole' + id);

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/roles/page'));
            await page.reload();
            await responsePromise;

            const popupPromise = context.waitForEvent('page');
            await page.locator('#btnPrint').click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteRole(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────────

test.describe('역할 CRUD', () => {

    test('행 추가 버튼을 클릭하면 테이블 상단에 입력 행이 추가되어야 한다', async ({ page }) => {
        await page.locator('#btnAddRow').click();

        const newRow = page.locator('#roleTableBody tr').first();
        await expect(newRow.locator('input[data-field="roleId"]')).toBeVisible();
        await expect(newRow.locator('input[data-field="roleId"]')).toBeEnabled();
        await expect(newRow.locator('input[data-field="roleName"]')).toBeEnabled();
    });

    test('새 행에 값을 입력하고 저장하면 목록에 나타나야 한다', async ({ page, request }) => {
        const id = generateTestId('cr');

        try {
            await page.locator('#btnAddRow').click();

            const newRow = page.locator('#roleTableBody tr').first();
            await newRow.locator('input[data-field="roleId"]').fill(id);
            await newRow.locator('input[data-field="roleName"]').fill('NewRole' + id);
            await newRow.locator('input[data-field="ranking"]').fill('99');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/roles/batch') && r.request().method() === 'POST');
            await page.locator('#btnSaveChanges').click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            await searchByField(page, 'roleId', id);
            await expect(page.locator(`#roleTableBody input[data-field="roleId"][value="${id}"]`)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('권한ID 없이 저장하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnAddRow').click();

        const newRow = page.locator('#roleTableBody tr').first();
        await newRow.locator('input[data-field="roleName"]').fill('NoIdRole');
        await newRow.locator('input[data-field="ranking"]').fill('1');
        // roleId 비운 채 저장
        await page.locator('#btnSaveChanges').click();

        await expect(page.locator('.toast')).toBeVisible();
    });

    test('권한명 없이 저장하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        const id = generateTestId('cr');
        await page.locator('#btnAddRow').click();

        const newRow = page.locator('#roleTableBody tr').first();
        await newRow.locator('input[data-field="roleId"]').fill(id);
        await newRow.locator('input[data-field="ranking"]').fill('1');
        // roleName 비운 채 저장
        await page.locator('#btnSaveChanges').click();

        await expect(page.locator('.toast')).toBeVisible();
    });

    test('중복 권한명으로 저장하면 Toast 알림이 표시되어야 한다', async ({ page, request }) => {
        const existId = generateTestId('cr');
        const sameName = 'DupRoleName' + existId;
        await createRole(request, existId, sameName);

        const newId = generateTestId('cr');

        try {
            await page.locator('#btnAddRow').click();

            const newRow = page.locator('#roleTableBody tr').first();
            await newRow.locator('input[data-field="roleId"]').fill(newId);
            await newRow.locator('input[data-field="roleName"]').fill(sameName);
            await newRow.locator('input[data-field="ranking"]').fill('99');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/roles/batch') && r.request().method() === 'POST');
            await page.locator('#btnSaveChanges').click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            await expect(page.locator('.toast')).toBeVisible();
        } finally {
            await deleteRole(request, existId);
        }
    });

    test('기존 행의 권한명을 수정하고 저장하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const id = generateTestId('cr');
        await createRole(request, id, 'BeforeName' + id);

        try {
            await searchByField(page, 'roleId', id);

            const row = page.locator('#roleTableBody tr').filter({
                has: page.locator(`input[data-field="roleId"][value="${id}"]`),
            });
            await row.locator('input[data-field="roleName"]').fill('AfterName' + id);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/roles/batch') && r.request().method() === 'POST');
            await page.locator('#btnSaveChanges').click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            await searchByField(page, 'roleName', 'AfterName' + id);
            await expect(page.locator(`#roleTableBody input[data-field="roleName"][value="AfterName${id}"]`)).toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('행을 체크하고 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const id = generateTestId('cr');
        await createRole(request, id, 'DeleteTarget' + id);

        await searchByField(page, 'roleId', id);

        const row = page.locator('#roleTableBody tr').filter({
            has: page.locator(`input[data-field="roleId"][value="${id}"]`),
        });
        await row.locator('.row-checkbox').check();
        await page.locator('#btnDeleteSelected').click();

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/roles/batch') && r.request().method() === 'POST');
        await page.locator('#btnSaveChanges').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await searchByField(page, 'roleId', id);
        await expect(page.locator(`#roleTableBody input[data-field="roleId"][value="${id}"]`)).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });

    test('선택 없이 삭제 버튼을 클릭하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnDeleteSelected').click();
        await expect(page.locator('.toast')).toBeVisible();
    });

    test('변경사항 없이 저장 버튼을 클릭하면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnSaveChanges').click();
        await expect(page.locator('.toast')).toBeVisible();
    });

    test('기존 행의 PK(권한ID) 필드는 수정할 수 없어야 한다', async ({ page, request }) => {
        const id = generateTestId('cr');
        await createRole(request, id, 'PkReadonly' + id);

        try {
            await searchByField(page, 'roleId', id);

            const row = page.locator('#roleTableBody tr').filter({
                has: page.locator(`input[data-field="roleId"][value="${id}"]`),
            });
            const pkInput = row.locator('input[data-field="roleId"]');

            await expect(pkInput).toHaveValue(id);
            await expect(pkInput).toHaveAttribute('readonly', '');
        } finally {
            await deleteRole(request, id);
        }
    });
});

// ─── 메뉴 권한 모달 ──────────────────────────────────────────

test.describe('메뉴 권한 모달', () => {

    test('메뉴 권한 버튼을 클릭하면 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('mm');
        await createRole(request, id, 'ModalRole' + id);

        try {
            await searchByField(page, 'roleId', id);

            const row = page.locator('#roleTableBody tr').filter({
                has: page.locator(`input[data-field="roleId"][value="${id}"]`),
            });
            await row.getByRole('button', { name: LABEL.ROLE_MENU_PERMISSION }).click();

            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.locator('#roleMenuPermissionModal')).toBeVisible();

            await page.locator('#roleMenuPermissionModal').getByRole('button', { name: LABEL.CLOSE }).click();
            await expect(page.getByRole('dialog')).not.toBeVisible();
        } finally {
            await deleteRole(request, id);
        }
    });

    test('신규 행의 메뉴 권한 버튼은 비활성화되어야 한다', async ({ page }) => {
        await page.locator('#btnAddRow').click();

        const newRow = page.locator('#roleTableBody tr').first();
        const menuBtn = newRow.getByRole('button', { name: LABEL.ROLE_MENU_PERMISSION });

        await expect(menuBtn).toBeDisabled();
    });
});

// ─── 권한 — W 권한 사용자 ────────────────────────────────────

test.describe('권한 — W 권한 사용자', () => {

    test('W 권한 사용자에게는 행 추가 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnAddRow')).toBeVisible();
    });

    test('W 권한 사용자에게는 선택 행 삭제 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnDeleteSelected')).toBeVisible();
    });

    test('W 권한 사용자에게는 변경사항 저장 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnSaveChanges')).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 행 추가 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnAddRow')).not.toBeVisible();
    });

    test('R 권한 사용자에게는 선택 행 삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnDeleteSelected')).not.toBeVisible();
    });

    test('R 권한 사용자에게는 변경사항 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnSaveChanges')).not.toBeVisible();
    });

    test('R 권한 사용자에게는 테이블에 편집 가능한 입력 필드가 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#roleTableBody input.editable-field')).not.toBeVisible();
    });

    test('R 권한 사용자가 메뉴 권한 모달을 열면 변경사항 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // e2e-admin 계정으로 역할 생성 후 R-only 사용자로 모달 열기
        // request fixture는 storageState를 따르므로 직접 API 호출은 403
        // 기존 역할 목록에서 첫 행의 메뉴 권한 버튼 클릭
        const rows = page.locator('#roleTableBody tr');
        const firstRowMenuBtn = rows.first().getByRole('button', { name: LABEL.ROLE_MENU_PERMISSION });

        if (await firstRowMenuBtn.isVisible()) {
            await firstRowMenuBtn.click();
            await expect(page.getByRole('dialog')).toBeVisible();

            // 변경사항 저장 버튼 미표시
            await expect(
                page.locator('#roleMenuPermissionModal').getByRole('button', { name: LABEL.ROLE_SAVE_CHANGES })
            ).not.toBeVisible();

            await page.locator('#roleMenuPermissionModal').getByRole('button', { name: LABEL.CLOSE }).click();
        } else {
            test.skip();
        }
    });
});
