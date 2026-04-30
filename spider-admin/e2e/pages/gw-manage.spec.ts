/**
 * Gateway 관리 페이지 E2E 테스트 — /gateways
 *
 * 검증 범위:
 * - 목록/검색 (G/W 명, GATEWAY ID, ioType 필터, 컬럼 정렬)
 * - 조회 (엑셀 다운로드, 출력 팝업, 행 클릭 → 모달, 관련기관 버튼)
 * - CRUD (등록 모달, 저장, PK readonly, SYSTEM 행 추가/삭제)
 * - 권한 (R 권한 사용자: 저장 시도 → API 403)
 *
 * 주의:
 * - Gateway 삭제 API가 없으므로 생성된 테스트 데이터는 DB에 누적된다.
 *   GW ID는 ^[A-Za-z0-9_]+$ 패턴이므로 하이픈 사용 불가.
 * - saveChanges(), deleteSelected() 는 브라우저 confirm() 을 사용한다.
 * - gw-manage 페이지는 sec:authorize 없이 모든 버튼이 표시된다.
 *   권한 제어는 API 레벨에서만 이루어진다.
 * - 시드 데이터: E2E-LIS-GW (리스너), E2E-CON-GW (어댑터)
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 헬퍼 ────────────────────────────────────────────────────────────────────

async function createGateway(
    request: APIRequestContext,
    gwId: string,
    gwName: string,
    ioType: 'I' | 'O' = 'O',
) {
    const res = await request.post('/api/gateways/with-systems', {
        data: {
            gateway: {
                gwId,
                gwName,
                threadCount: 1,
                ioType,
                gwDesc: 'E2E 테스트',
                gwAppName: 'com.test.Gateway',
            },
        },
    });
    expect(res.status()).toBe(200);
}

async function searchByGwName(page: Page, value: string) {
    await page.locator('#gatewaySearchField').selectOption('gwName');
    await page.locator('#gatewaySearchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

async function searchByGwId(page: Page, value: string) {
    await page.locator('#gatewaySearchField').selectOption('gwId');
    await page.locator('#gatewaySearchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

// GW ID 패턴: ^[A-Za-z0-9_]+$ — 하이픈 불가
function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

// ─── 초기화 ──────────────────────────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
    await page.goto('/gateways');
    await responsePromise;
    await page.locator('#gatewaySearchValue').waitFor({ state: 'visible' });
});

// ─── 목록/검색 ───────────────────────────────────────────────────────────────

test.describe('Gateway 목록 및 검색', () => {

    test('초기 페이지 로드 시 데이터가 20건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#gatewayTableBody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(20);
    });

    test('G/W 명으로 검색하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eGwl');
        const name = 'GWSearch' + id;
        await createGateway(request, id, name);

        await searchByGwName(page, name);
        await expect(page.getByRole('cell', { name: id })).toBeVisible();
    });

    test('GATEWAY ID로 검색하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eGwl');
        await createGateway(request, id, 'IDSearchTest');

        await searchByGwId(page, id);
        await expect(page.getByRole('cell', { name: id })).toBeVisible();
    });

    test('어댑터/리스너 필터를 변경하면 해당 조건에 맞는 데이터만 조회되어야 한다', async ({ page }) => {
        await page.locator('#gatewayIoType').selectOption('I');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        // 조회된 행은 모두 리스너(수신)여야 한다
        const cells = page.locator('#gatewayTableBody td:nth-child(4)');
        const count = await cells.count();
        if (count > 0) {
            for (let i = 0; i < count; i++) {
                await expect(cells.nth(i)).toContainText('리스너(수신)');
            }
        }
    });

    test('검색 조건을 변경하면 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eGwl');
        await createGateway(request, id, 'PageReset');

        await searchByGwName(page, 'PageReset');
        await expect(page.locator('.page-item.active .page-link')).toContainText('1');
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eGwl');
        await createGateway(request, id, 'SortTest');

        const sortHeader = page.locator('#gatewayTable thead th[data-sort="gwId"]');

        // 1) 첫 클릭 → 오름차순
        const res1 = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
        await sortHeader.click();
        await res1;
        await expect(sortHeader).toHaveClass(/sort-asc/);

        // 2) 두 번째 클릭 → 내림차순
        const res2 = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
        await sortHeader.click();
        await res2;
        await expect(sortHeader).toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 표시되어야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2eGwl');
        await createGateway(request, id, 'PrintTest');

        await context.addInitScript(() => { window.print = () => {}; });
        await page.reload();
        await page.waitForResponse(r => r.url().includes('/api/gateways/page'));

        await searchByGwName(page, 'PrintTest');

        const popupPromise = context.waitForEvent('page');
        await page.getByRole('button', { name: LABEL.PRINT }).click();
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await expect(popup.locator('table')).toBeVisible();
        await popup.close();
    });

    test('데이터 행을 클릭하면 Gateway 관리 모달이 열려야 한다', async ({ page }) => {
        await searchByGwId(page, 'E2E-LIS-GW');

        const detailPromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/E2E-LIS-GW') && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: 'E2E-LIS-GW' }).first().click();
        await detailPromise;

        await expect(page.locator('#gatewayManageModal')).toBeVisible();
        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
        await expect(page.locator('#gatewayManageModal')).not.toBeVisible();
    });

    test('관련기관 버튼을 클릭하면 관련기관 모달이 열려야 한다', async ({ page }) => {
        await searchByGwId(page, 'E2E-LIS-GW');

        const row = page.getByRole('row').filter({ hasText: 'E2E-LIS-GW' }).first();
        const transportsPromise = page.waitForResponse(r =>
            r.url().includes('/api/transports') && r.url().includes('E2E-LIS-GW'));
        await row.getByRole('button', { name: LABEL.GW_ORG_VIEW }).click();
        await transportsPromise;

        await expect(page.locator('#gatewayOrgsModal')).toBeVisible();
        await page.locator('#gatewayOrgsModal .modal-footer [data-bs-dismiss="modal"]').click();
        await expect(page.locator('#gatewayOrgsModal')).not.toBeVisible();
    });
});

// ─── CRUD ────────────────────────────────────────────────────────────────────

test.describe('Gateway CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        // GW ID 필드는 편집 가능해야 한다
        await expect(page.locator('#gwManageId')).not.toHaveAttribute('readonly');
        await expect(page.locator('#gwManageId')).toHaveValue('');

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
        await expect(page.locator('#gatewayManageModal')).not.toBeVisible();
    });

    test('새 Gateway를 등록하면 목록에 즉시 반영되어야 한다', async ({ page }) => {
        const testId = generateTestId('e2eGwc');

        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        await page.locator('#gwManageId').fill(testId);
        await page.locator('#gwManageName').fill('GW_Create_Test');
        await page.locator('#gwManageIoType').selectOption('O');
        await page.locator('#gwManageThread').fill('2');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/with-systems') && r.request().method() === 'POST');
        await page.getByRole('button', { name: LABEL.GW_MANAGE_SAVE }).click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('#gatewayManageModal')).not.toBeVisible();
        await searchByGwId(page, testId);
        await expect(page.getByRole('cell', { name: testId })).toBeVisible();
    });

    test('필수 항목을 입력하지 않으면 저장이 되지 않아야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        // 아무것도 입력하지 않고 저장 → validateGatewayForm toast 발생
        await page.getByRole('button', { name: LABEL.GW_MANAGE_SAVE }).click();

        // 모달이 닫히지 않아야 한다
        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 GW ID(PK)가 읽기 전용인 수정 모달이 열려야 한다', async ({ page }) => {
        await searchByGwId(page, 'E2E-LIS-GW');

        const detailPromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/E2E-LIS-GW') && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: 'E2E-LIS-GW' }).first().click();
        await detailPromise;

        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        // GW ID는 readonly 여야 한다
        await expect(page.locator('#gwManageId')).toHaveAttribute('readonly');
        await expect(page.locator('#gwManageId')).toHaveValue('E2E-LIS-GW');

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
    });

    test('기존 Gateway를 수정하면 목록에 즉시 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2eGwc');
        await createGateway(request, testId, 'BeforeName');

        await searchByGwId(page, testId);
        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/gateways/${testId}`) && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: testId }).first().click();
        await detailPromise;

        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        await page.locator('#gwManageName').fill('AfterName');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/with-systems') && r.request().method() === 'POST');
        await page.getByRole('button', { name: LABEL.GW_MANAGE_SAVE }).click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('#gatewayManageModal')).not.toBeVisible();
        await searchByGwName(page, 'AfterName');
        await expect(page.getByRole('cell', { name: testId })).toBeVisible();
    });

    test('Gateway SYSTEM 행 추가 버튼을 클릭하면 SYSTEM 테이블에 새 행이 추가되어야 한다', async ({ page }) => {
        await searchByGwId(page, 'E2E-LIS-GW');

        const detailPromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/E2E-LIS-GW') && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: 'E2E-LIS-GW' }).first().click();
        await detailPromise;

        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        const initialRows = await page.locator('#gatewaySystemTableBody tr').count();

        await page.getByRole('button', { name: LABEL.GW_MANAGE_ADD_ROW }).click();

        const newRows = await page.locator('#gatewaySystemTableBody tr').count();
        expect(newRows).toBeGreaterThan(initialRows);

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
    });

    test('SYSTEM 신규 행 선택 후 선택행 삭제를 클릭하면 행이 제거되어야 한다', async ({ page }) => {
        await searchByGwId(page, 'E2E-LIS-GW');

        const detailPromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/E2E-LIS-GW') && r.request().method() === 'GET');
        await page.getByRole('row').filter({ hasText: 'E2E-LIS-GW' }).first().click();
        await detailPromise;

        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        // 신규 행 추가
        await page.getByRole('button', { name: LABEL.GW_MANAGE_ADD_ROW }).click();

        const rowsBefore = await page.locator('#gatewaySystemTableBody tr').count();

        // 첫 번째 행이 신규 행 (addRow는 tbody에 prepend)
        await page.locator('#gatewaySystemTableBody tr').first().locator('input[type="checkbox"]').check();

        await page.getByRole('button', { name: LABEL.GW_MANAGE_DELETE_SELECTED }).click();
        await page.locator('#spConfirmModalOk').click();

        const rowsAfter = await page.locator('#gatewaySystemTableBody tr').count();
        expect(rowsAfter).toBeLessThan(rowsBefore);

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────────────────────
//
// gw-manage 페이지는 sec:authorize 없이 버튼을 렌더링한다.
// 권한 제어는 API 레벨에서만 이루어지므로, R 권한 사용자가
// 저장을 시도하면 API가 403을 반환하고 실패 알림이 표시된다.

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자도 Gateway 목록을 조회할 수 있어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
        await page.goto('/gateways');
        await responsePromise;

        // 목록이 정상 로드되어야 한다
        await expect(page.locator('#gatewayTableBody')).toBeVisible();
    });

    test('R 권한 사용자가 저장을 시도하면 실패 알림이 표시되어야 한다', async ({ page }) => {
        const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/gateways/page'));
        await page.goto('/gateways');
        await responsePromise2;

        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.locator('#gatewayManageModal')).toBeVisible();

        await page.locator('#gwManageId').fill('e2eGwRO_test');
        await page.locator('#gwManageName').fill('R Only Test');
        await page.locator('#gwManageThread').fill('1');
        await page.locator('#gwManageIoType').selectOption('O');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/gateways/with-systems') && r.request().method() === 'POST');
        await page.getByRole('button', { name: LABEL.GW_MANAGE_SAVE }).click();
        await page.locator('#spConfirmModalOk').click();
        const response = await responsePromise;

        expect(response.status()).toBe(403);

        await page.locator('#gatewayManageModal [data-bs-dismiss="modal"]').first().click();
    });
});
