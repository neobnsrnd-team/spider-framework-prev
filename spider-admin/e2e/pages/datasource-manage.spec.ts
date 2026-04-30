/**
 * 데이터소스 관리 페이지 E2E 테스트 — /datasources
 *
 * 검증 범위:
 * - 목록/검색 (DB ID, DB 명, JNDI 필터, 정렬, 엑셀, 출력, 행 클릭)
 * - CRUD (등록 모달, DB ID 읽기 전용, 저장, 삭제, JNDI 토글)
 * - 권한 (R 권한 사용자: 등록 버튼 미표시, 저장/삭제 버튼 미표시)
 *
 * 주의:
 * - DB ID는 FWK_SQL_CONF PK — 중복 불가.
 * - 삭제는 Toast.confirm (#spConfirmModal) 사용.
 * - JNDI 여부 토글 시 dsDirectSection / dsJndiSection 표시 전환.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 헬퍼 ────────────────────────────────────────────────────────────────────

let seq = 0;
function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36) + (seq++);
}

async function createDataSource(request: APIRequestContext, id: string, name: string) {
    const res = await request.post('/api/datasources', {
        data: {
            dbId: id,
            dbName: name,
            dbUserId: 'testuser',
            dbPassword: 'testpass123',
            jndiYn: 'N',
            connectionUrl: 'jdbc:oracle:thin:@localhost:1521:XE',
            driverClass: 'oracle.jdbc.OracleDriver',
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteDataSource(request: APIRequestContext, id: string) {
    await request.delete(`/api/datasources/${id}`);
}

async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#_searchContainer_searchField').selectOption(field);
    await page.locator('#_searchContainer_searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/datasources/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

// ─── 초기화 ──────────────────────────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/datasources');
    await page.waitForResponse(r => r.url().includes('/api/datasources/page'));
    await page.locator('#_searchContainer_searchValue').waitFor({ state: 'visible' });
});

// ─── 목록/검색 ───────────────────────────────────────────────────────────────

test.describe('데이터소스 목록 및 검색', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#datasourceTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('DB ID로 검색하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eDsl');
        await createDataSource(request, id, 'SearchTest');

        try {
            await searchByField(page, 'dbId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteDataSource(request, id);
        }
    });

    test('DB 명으로 검색하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eDsl');
        const name = 'NameSearch' + id;
        await createDataSource(request, id, name);

        try {
            await searchByField(page, 'dbName', name);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteDataSource(request, id);
        }
    });

    test('JNDI 필터를 적용하면 해당 조건에 맞는 데이터만 조회되어야 한다', async ({ page }) => {
        await page.locator('#_searchContainer_jndiYnFilter').selectOption('N');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/datasources/page'));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        // 조회된 행은 모두 직접접속이어야 한다
        const cells = page.locator('#datasourceTable tbody td:nth-child(5)');
        const count = await cells.count();
        if (count > 0) {
            for (let i = 0; i < count; i++) {
                await expect(cells.nth(i)).toContainText('직접접속');
            }
        }
    });

    test('검색 조건을 변경하면 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eDsl');
        await createDataSource(request, id, 'PageReset');

        try {
            await searchByField(page, 'dbId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteDataSource(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eDsl');
        await createDataSource(request, id, 'SortTest');

        try {
            const sortHeader = page.locator('#datasourceTable thead th[data-sort="dbId"]');

            // 1) 첫 클릭 → 오름차순
            const res1 = page.waitForResponse(r => r.url().includes('/api/datasources/page'));
            await sortHeader.click();
            await res1;
            await expect(sortHeader).toHaveClass(/sort-asc/);

            // 2) 두 번째 클릭 → 내림차순
            const res2 = page.waitForResponse(r => r.url().includes('/api/datasources/page'));
            await sortHeader.click();
            await res2;
            await expect(sortHeader).toHaveClass(/sort-desc/);
        } finally {
            await deleteDataSource(request, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const id = generateTestId('e2eDsl');
        await createDataSource(request, id, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await page.waitForResponse(r => r.url().includes('/api/datasources/page'));

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteDataSource(request, id);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('e2eDsl');
        await createDataSource(request, id, 'RowClickTest');

        try {
            await searchByField(page, 'dbId', id);
            await page.getByRole('row').filter({ hasText: id }).click();

            await expect(page.locator('#datasourceModal')).toBeVisible();
            await expect(page.locator('#datasourceModalLabel')).toContainText(LABEL.DS_DETAIL_TITLE);

            await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
            await expect(page.locator('#datasourceModal')).not.toBeVisible();
        } finally {
            await deleteDataSource(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────────────────────────

test.describe('데이터소스 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.locator('#btnAdd').click();

        await expect(page.locator('#datasourceModal')).toBeVisible();
        await expect(page.locator('#datasourceModalLabel')).toContainText(LABEL.DS_CREATE_TITLE);

        // DB ID 필드는 편집 가능해야 한다
        await expect(page.locator('#dsModalDbId')).toBeEditable();
        await expect(page.locator('#dsModalDbId')).toHaveValue('');

        // 삭제 버튼은 숨겨져야 한다
        await expect(page.locator('#btnDsDelete')).not.toBeVisible();

        await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
        await expect(page.locator('#datasourceModal')).not.toBeVisible();
    });

    test('새 데이터소스를 등록하면 목록에 즉시 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2eDsc');

        try {
            await page.locator('#btnAdd').click();
            await expect(page.locator('#datasourceModal')).toBeVisible();

            await page.locator('#dsModalDbId').fill(testId);
            await page.locator('#dsModalDbName').fill('E2E 등록 테스트');
            await page.locator('#dsModalDbUserId').fill('testuser');
            await page.locator('#dsModalDbPassword').fill('testpass');
            await page.locator('#dsModalConnectionUrl').fill('jdbc:oracle:thin:@localhost:1521:XE');
            await page.locator('#dsModalDriverClass').fill('oracle.jdbc.OracleDriver');

            const responsePromise = page.waitForResponse(r =>
                r.url().endsWith('/api/datasources') && r.request().method() === 'POST');
            await page.locator('#btnDsSave').click();
            await responsePromise;
            await expect(page.locator('#datasourceModal')).not.toBeVisible();

            await searchByField(page, 'dbId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteDataSource(request, testId);
        }
    });

    test('필수 항목(DB ID)을 입력하지 않으면 Toast 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnAdd').click();
        await expect(page.locator('#datasourceModal')).toBeVisible();

        // DB ID를 비우고 저장 시도
        await page.locator('#btnDsSave').click();

        // Toast 경고가 표시되어야 한다
        await expect(page.locator('.toast')).toBeVisible();

        // 모달은 닫히지 않아야 한다
        await expect(page.locator('#datasourceModal')).toBeVisible();

        await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
    });

    test('행을 클릭하면 DB ID(PK)가 비활성화된 수정 모달이 열려야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2eDsc');
        await createDataSource(request, testId, '수정대상');

        try {
            await searchByField(page, 'dbId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();

            await expect(page.locator('#datasourceModal')).toBeVisible();
            await expect(page.locator('#datasourceModalLabel')).toContainText(LABEL.DS_DETAIL_TITLE);

            // DB ID는 비활성화되어야 한다
            await expect(page.locator('#dsModalDbId')).toBeDisabled();
            await expect(page.locator('#dsModalDbId')).toHaveValue(testId);

            // 저장/삭제 버튼이 표시되어야 한다
            await expect(page.locator('#btnDsSave')).toBeVisible();
            await expect(page.locator('#btnDsDelete')).toBeVisible();

            await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteDataSource(request, testId);
        }
    });

    test('DB 명을 수정하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2eDsc');
        await createDataSource(request, testId, 'BeforeName');

        try {
            await searchByField(page, 'dbId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.locator('#datasourceModal')).toBeVisible();

            await page.locator('#dsModalDbName').fill('AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/datasources/${testId}`) && r.request().method() === 'PUT');
            await page.locator('#btnDsSave').click();
            await responsePromise;
            await expect(page.locator('#datasourceModal')).not.toBeVisible();

            await searchByField(page, 'dbId', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteDataSource(request, testId);
        }
    });

    test('JNDI 여부를 Y로 변경하면 JNDI 필드가 표시되고 접속 URL 섹션이 숨겨져야 한다', async ({ page }) => {
        await page.locator('#btnAdd').click();
        await expect(page.locator('#datasourceModal')).toBeVisible();

        // 기본값 N: 직접접속 섹션 표시, JNDI 섹션 숨김
        await expect(page.locator('#dsDirectSection')).toBeVisible();
        await expect(page.locator('#dsJndiSection')).not.toBeVisible();

        // JNDI 여부 Y로 변경
        await page.locator('#dsModalJndiYn').selectOption('Y');

        // 직접접속 섹션 숨김, JNDI 섹션 표시
        await expect(page.locator('#dsDirectSection')).not.toBeVisible();
        await expect(page.locator('#dsJndiSection')).toBeVisible();

        await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
    });

    test('데이터소스를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testId = generateTestId('e2eDsc');
        await createDataSource(request, testId, 'DeleteMe');

        await searchByField(page, 'dbId', testId);
        await page.getByRole('row').filter({ hasText: testId }).click();
        await expect(page.locator('#datasourceModal')).toBeVisible();

        await page.locator('#btnDsDelete').click();

        // Toast.confirm 모달에서 확인 클릭
        await expect(page.locator('#spConfirmModal')).toBeVisible();
        const deleteRes = page.waitForResponse(r =>
            r.url().includes(`/api/datasources/${testId}`) && r.request().method() === 'DELETE');
        await page.locator('#spConfirmModal .btn-primary').click();
        await deleteRes;

        await searchByField(page, 'dbId', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/datasources');
        await page.waitForResponse(r => r.url().includes('/api/datasources/page'));

        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const testId = generateTestId('e2eDsp');
        await createDataSource(adminRequest, testId, 'PermTest');

        try {
            await page.goto('/datasources');
            await page.waitForResponse(r => r.url().includes('/api/datasources/page'));
            await page.locator('#_searchContainer_searchValue').waitFor({ state: 'visible' });

            await searchByField(page, 'dbId', testId);
            await page.getByRole('row').filter({ hasText: testId }).click();
            await expect(page.locator('#datasourceModal')).toBeVisible();

            await expect(page.locator('#btnDsSave')).not.toBeVisible();
            await expect(page.locator('#btnDsDelete')).not.toBeVisible();

            await page.locator('#datasourceModal [data-bs-dismiss="modal"]').first().click();
        } finally {
            await deleteDataSource(adminRequest, testId);
            await adminRequest.dispose();
        }
    });
});
