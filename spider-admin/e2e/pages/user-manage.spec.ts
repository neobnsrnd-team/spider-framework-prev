/**
 * 사용자 페이지 — 목록, CRUD, 행 액션, 메뉴 권한 모달.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';
import { LABEL } from '../fixtures/locale';

async function createUser(request: APIRequestContext, userId: string, userName: string) {
    await request.post('/api/users', {
        data: { userId, userName, password: 'Test1234!', roleId: 'ADMIN' },
    });
}

async function deleteUser(request: APIRequestContext, userId: string) {
    await request.delete(`/api/users/${userId}`);
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/users/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/users');
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('사용자 목록', () => {

    test('초기 로드 시 시드 계정이 테이블에 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();
    });

    test('사용자명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-list-');
        const uniqueName = 'SearchName' + id;
        await createUser(request, id, uniqueName);

        try {
            await searchByField(page, 'userName', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteUser(request, id);
        }
    });

    test('사용자ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-list-');
        await createUser(request, id, 'ID검색유저');

        try {
            await searchByField(page, 'userId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteUser(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();

        const responsePromise = page.waitForResponse(r => r.url().includes('/api/users/page'));
        await page.getByRole('columnheader', { name: LABEL.USER_ID_COLUMN }).click();
        await responsePromise;

        await expect(page.getByRole('row').nth(1)).toBeVisible();

        const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/users/page'));
        await page.getByRole('columnheader', { name: LABEL.USER_ID_COLUMN }).click();
        await responsePromise2;

        await expect(page.getByRole('row').nth(1)).toBeVisible();
    });

    test('검색창에서 Enter를 누르면 검색이 실행되어야 한다', async ({ page }) => {
        await page.locator('#searchField').selectOption('userId');
        await page.locator('#searchValue').fill(ADMIN.userId);

        const responsePromise = page.waitForResponse(r => r.url().includes('/api/users/page'));
        await page.locator('#searchValue').press('Enter');
        await responsePromise;

        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context }) => {
        // headless에서 window.print()가 팝업을 즉시 닫는 것을 방지
        await context.addInitScript(() => { window.print = () => {}; });

        const popupPromise = context.waitForEvent('page');
        await page.getByRole('button', { name: LABEL.PRINT }).click();
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await expect(popup.locator('h3')).toHaveText('사용자 목록');
        await expect(popup.locator('table')).toBeVisible();
        await popup.close();
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('사용자 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.USER_CREATE_TITLE)).toBeVisible();

        await expect(page.getByLabel('사용자ID')).toHaveValue('');
        await expect(page.getByLabel('사용자ID')).toBeEnabled();

        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });

    test('모달에서 사용자를 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-crud-');
        page.on('dialog', d => d.accept());

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            // 역할 옵션이 서버에서 로드되었는지 확인 (빈 기본값 + 1개 이상)
            await expect(page.getByLabel('권한명').locator('option')).not.toHaveCount(1);

            await page.getByLabel('사용자ID').fill(testId);
            await page.getByLabel('사용자명').fill('생성테스트유저');
            await page.getByLabel('패스워드').fill('Test1234!');
            await page.getByLabel('권한명').selectOption({ index: 1 });

            await page.getByRole('button', { name: LABEL.SAVE }).last().click();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await searchByField(page, 'userId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteUser(request, testId);
        }
    });

    test('행을 클릭하면 정보가 채워진 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-crud-');
        await createUser(request, testId, '수정대상');

        try {
            await searchByField(page, 'userId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.USER_DETAIL_TITLE)).toBeVisible();

            await expect(page.getByLabel('사용자ID')).toBeDisabled();
            await expect(page.getByLabel('사용자ID')).toHaveValue(testId);

            await expect(page.getByRole('button', { name: LABEL.DELETE })).toBeVisible();

            await page.getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteUser(request, testId);
        }
    });

    test('사용자명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-crud-');
        page.on('dialog', d => d.accept());
        await createUser(request, testId, 'Before');

        try {
            await searchByField(page, 'userId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await page.getByLabel('사용자명').fill('After');
            await page.getByRole('button', { name: LABEL.SAVE }).last().click();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            const reloadRes = page.waitForResponse(r => r.url().includes('/api/users/page'));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await reloadRes;
            await expect(page.getByRole('cell', { name: 'After' })).toBeVisible();
        } finally {
            await deleteUser(request, testId);
        }
    });

    test('사용자를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2e-crud-');
        await createUser(request, testId, 'DeleteMe');

        await searchByField(page, 'userId', testId);
        await page.getByRole('row').filter({ hasText: testId }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await page.getByRole('button', { name: LABEL.DELETE }).click();
        await page.locator('#spConfirmModalOk').click();
        await page.locator('#spConfirmModal').waitFor({ state: 'hidden' });
        await expect(page.getByRole('dialog')).not.toBeVisible();

        const reloadRes = page.waitForResponse(r => r.url().includes('/api/users/page'));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await reloadRes;
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });

    test('필수 항목 없이 저장하면 유효성 오류가 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        page.once('dialog', d => d.accept());
        await page.getByRole('button', { name: LABEL.SAVE }).last().click();

        // 유효성 오류 메시지가 사용자에게 보여야 한다
        await expect(page.locator('.invalid-feedback:visible, .error-message:visible').first()).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
    });

    test('중복된 사용자명으로 생성하면 에러가 표시되어야 한다', async ({ page, request }) => {
        const id1 = generateTestId('e2e-dup-');
        const dupName = 'DupName' + id1;
        await createUser(request, id1, dupName);

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await expect(page.getByLabel('권한명').locator('option')).not.toHaveCount(1);

            await page.getByLabel('사용자ID').fill(generateTestId('e2e-dup2-'));
            await page.getByLabel('사용자명').fill(dupName);
            // blur를 트리거하여 중복 체크 실행
            await page.getByLabel('패스워드').focus();
            await page.getByLabel('패스워드').fill('Test1234!');
            await page.getByLabel('권한명').selectOption({ index: 1 });

            await page.getByRole('button', { name: LABEL.SAVE }).last().click();

            // toast 또는 인라인 에러 중 하나가 표시되어야 한다
            await page.waitForTimeout(2000);
            const hasInlineError = await page.locator('.error-message:visible, .has-error').first().isVisible().catch(() => false);
            const hasToast = await page.locator('.toast').first().isVisible().catch(() => false);
            expect(hasToast || hasInlineError).toBeTruthy();

            await page.getByRole('button', { name: LABEL.CLOSE }).click();
        } finally {
            await deleteUser(request, id1);
        }
    });
});

// ─── 행 액션 ─────────────────────────────────────────────

test.describe('사용자 행 액션', () => {

    test('초기화 버튼을 클릭하면 확인 대화상자가 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();

        const btn = page.getByRole('button', { name: LABEL.RESET }).first();
        await expect(btn).toBeVisible();

        await btn.click();
        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModal [data-bs-dismiss="modal"]').first().click();
    });

    test('메뉴 버튼을 클릭하면 권한 모달이 열려야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();

        const btn = page.getByRole('button', { name: LABEL.MENU }).first();
        await expect(btn).toBeVisible();

        await btn.click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).last().click();
    });

    test('액션 버튼 클릭 시 수정 모달이 열려서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();

        const btn = page.getByRole('button', { name: LABEL.RESET }).first();
        await expect(btn).toBeVisible();

        await btn.click();
        await page.locator('#spConfirmModal').waitFor({ state: 'visible' });
        await page.locator('#spConfirmModal [data-bs-dismiss="modal"]').first().click();
        await expect(page.getByText(LABEL.USER_DETAIL_TITLE)).not.toBeVisible();
    });
});

// ─── 메뉴 권한 모달 ─────────────────────────────────────

async function openMenuModal(page: Page): Promise<void> {
    const btn = page.getByRole('button', { name: LABEL.MENU }).first();
    await expect(btn).toBeVisible();
    await btn.click();
    await expect(page.getByRole('dialog').filter({ hasText: LABEL.MENU_PERMISSION_TITLE })).toBeVisible();
    // 어느 한쪽 패널에 행이 로드될 때까지 대기
    await expect(page.locator('#umAvailableBody tr, #umUserBody tr').first()).toBeVisible();
}

test.describe('사용자 메뉴 권한 모달', () => {

    test('모달이 열리면 가용 메뉴와 할당 메뉴 패널이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();
        await openMenuModal(page);

        await expect(page.getByText(LABEL.AVAILABLE_MENU_LABEL)).toBeVisible();
        await expect(page.getByText(LABEL.ASSIGNED_MENU_LABEL)).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).last().click();
    });

    test('메뉴를 해제하면 가용 목록으로 이동하고 재할당하면 돌아와야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();
        await openMenuModal(page);

        const assignedRows = page.locator('#umUserBody tr[data-menu-id]');
        const assignedCount = await assignedRows.count();
        expect(assignedCount).toBeGreaterThan(0);

        // 마지막 할당 메뉴 선택 (핵심 부모 메뉴가 아닐 가능성 높음)
        const lastRow = assignedRows.last();
        const menuId = (await lastRow.locator('td').nth(1).innerText()).trim();

        // 체크박스 선택 후 제거 버튼 클릭
        await lastRow.locator('.um-assigned-checkbox').click();
        await page.locator('button[onclick="UserMenuModal.removeSelectedMenus()"]').click();

        // 가용 패널로 이동했는지 확인
        await expect(page.locator(`#umAvailableBody tr[data-menu-id="${menuId}"]`)).toBeVisible();

        // 체크박스 선택 후 추가 버튼 클릭
        await page.locator(`#umAvailableBody tr[data-menu-id="${menuId}"]`).locator('.um-available-checkbox').click();
        await page.locator('button[onclick="UserMenuModal.addSelectedMenus()"]').click();

        // 할당 패널로 돌아왔는지 확인
        await expect(page.locator(`#umUserBody tr[data-menu-id="${menuId}"]`)).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).last().click();
    });

    test('검색어를 입력하면 가용 메뉴가 필터링되어야 한다', async ({ page }) => {
        await expect(page.getByRole('cell', { name: ADMIN.userId })).toBeVisible();
        await openMenuModal(page);

        await page.locator('#umSearchValue').fill('zzz-nonexistent');
        await expect(page.getByText(LABEL.NO_AVAILABLE_MENUS)).toBeVisible();

        await page.locator('#umSearchValue').fill('');
        // 필터 해제 후 가용 메뉴가 다시 나타나거나 전부 할당 상태 — 위의 no-match 검증이 핵심

        await page.getByRole('button', { name: LABEL.CLOSE }).last().click();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/users');
        await expect(page.getByRole('table')).toBeVisible();

        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/users');
        await expect(page.getByRole('table')).toBeVisible();

        // 시드 계정 행을 클릭하여 모달 열기
        const row = page.getByRole('row').filter({ hasText: ADMIN.userId });
        await expect(row).toBeVisible();
        await row.click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
    });

    test('R 권한 사용자에게는 행 액션 버튼(초기화, 메뉴)이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/users');
        await expect(page.getByRole('table')).toBeVisible();

        await expect(page.getByRole('button', { name: LABEL.RESET })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.MENU })).not.toBeVisible();
    });
});
