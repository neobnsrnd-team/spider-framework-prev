import { test, expect, type Page } from '@playwright/test';

const PAGE_URL = '/was-gateway-statuses';
const API_PAGE = '**/api/was/gateway-status/page**';
const API_TEST = '**/api/was/gateway-status/**/test';

// ── 인라인 헬퍼 ───────────────────────────────────────────────────────────────

async function gotoAndWaitForList(page: Page) {
    const [res] = await Promise.all([
        page.waitForResponse(API_PAGE),
        page.goto(PAGE_URL),
    ]);
    expect(res.status()).toBe(200);
}

async function clickSearchAndWait(page: Page) {
    const [res] = await Promise.all([
        page.waitForResponse(API_PAGE),
        page.click('#gwStatusSearchBtn'),
    ]);
    return res;
}

async function getTableRowCount(page: Page): Promise<number> {
    const noDataText = '조회된 데이터가 없습니다.';
    const tbody = page.locator('#gwStatusTableBody');
    const firstCell = tbody.locator('tr:first-child td:first-child');
    const text = await firstCell.textContent();
    if (text && text.trim() === noDataText) return 0;
    return await tbody.locator('tr').count();
}

// ── 목록 조회 ─────────────────────────────────────────────────────────────────

test.describe('목록 조회', () => {
    test('초기 페이지 로드 시 목록 API가 호출되고 테이블이 렌더링되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        await expect(page.locator('#gwStatusTable')).toBeVisible();
        await expect(page.locator('#gwStatusTableBody')).toBeVisible();
    });

    test('초기 페이지 로드 시 최대 10건이 조회되어야 한다', async ({ page }) => {
        const [res] = await Promise.all([
            page.waitForResponse(API_PAGE),
            page.goto(PAGE_URL),
        ]);
        const body = await res.json();

        expect(body.data.size).toBe(10);
        const rowCount = await getTableRowCount(page);
        expect(rowCount).toBeLessThanOrEqual(10);
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 하고 페이지는 1페이지로 초기화되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        await page.selectOption('#gwStatusInstanceId', { index: 0 });
        const res = await clickSearchAndWait(page);

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.data.currentPage).toBe(1);
    });

    test('존재하지 않는 검색 조건이면 빈 테이블이 표시되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const res = await page.request.get('/api/was/gateway-status/page', {
            params: { instanceId: 'NOTEXIST-INSTANCE' },
        });
        const body = await res.json();
        expect(body.data.totalElements).toBe(0);
    });

    test('컬럼 헤더를 클릭하면 sort-asc 클래스가 적용되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const header = page.locator('#gwStatusTable thead th[data-sort="instanceName"]');
        await header.click();

        await expect(header).toHaveClass(/sort-asc/);
    });

    test('같은 컬럼 헤더를 다시 클릭하면 sort-desc 클래스로 변경되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const header = page.locator('#gwStatusTable thead th[data-sort="instanceName"]');
        await header.click();
        await header.click();

        await expect(header).toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 파일이 다운로드되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const [download] = await Promise.all([
            page.waitForEvent('download'),
            page.click('#btnExcel'),
        ]);

        expect(download.suggestedFilename()).toContain('WasGatewayStatus');
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 표시되어야 한다', async ({ page, context }) => {
        await context.addInitScript(() => { window.print = () => {}; });
        await gotoAndWaitForList(page);

        const popupPromise = context.waitForEvent('page');
        await page.click('#btnPrint');
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await popup.close();
    });
});

// ── 연결 테스트 ───────────────────────────────────────────────────────────────

test.describe('연결 테스트', () => {
    test('데이터 행을 클릭하면 연결 테스트 모달이 열려야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const rowCount = await getTableRowCount(page);
        if (rowCount === 0) {
            test.skip(true, '테이블에 행이 없어 모달 테스트를 건너뜁니다');
            return;
        }

        await page.locator('#gwStatusTableBody tr:first-child').click();
        await expect(page.locator('#gwStatusModal')).toHaveClass(/show/);
    });

    test('모달의 닫기 버튼을 클릭하면 모달이 닫혀야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const rowCount = await getTableRowCount(page);
        if (rowCount === 0) {
            test.skip(true, '테이블에 행이 없어 모달 테스트를 건너뜁니다');
            return;
        }

        await page.evaluate(() => (window as any).WasGatewayStatus.openTestModal(0));
        await expect(page.locator('#gwStatusModal')).toHaveClass(/show/);

        await page.locator('#gwStatusModal button[data-bs-dismiss="modal"]').first().click();
        await expect(page.locator('#gwStatusModal')).not.toHaveClass(/show/);
    });

    test('연결 테스트를 실행하면 결과가 표시되어야 한다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const rowCount = await getTableRowCount(page);
        if (rowCount === 0) {
            test.skip(true, '테이블에 행이 없어 테스트를 건너뜁니다');
            return;
        }

        await page.evaluate(() => (window as any).WasGatewayStatus.openTestModal(0));
        await expect(page.locator('#gwStatusModal')).toHaveClass(/show/);

        const [res] = await Promise.all([
            page.waitForResponse(API_TEST),
            page.click('#gwStatusTestRun'),
        ]);

        expect(res.status()).toBe(200);
        const result = page.locator('#gwStatusModalResult');
        await expect(result).not.toContainText('테스트를 실행하면 결과가 표시됩니다');
    });
});

// ── 권한 — R 권한 사용자 ──────────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자도 목록을 조회할 수 있어야 한다', async ({ page }) => {
        const [res] = await Promise.all([
            page.waitForResponse(API_PAGE),
            page.goto(PAGE_URL),
        ]);

        expect(res.status()).toBe(200);
        await expect(page.locator('#gwStatusTable')).toBeVisible();
    });

    test('R 권한 사용자에게는 테스트 실행 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await gotoAndWaitForList(page);

        const rowCount = await getTableRowCount(page);
        if (rowCount === 0) {
            test.skip(true, '테이블에 행이 없어 권한 테스트를 건너뜁니다');
            return;
        }

        await page.evaluate(() => (window as any).WasGatewayStatus.openTestModal(0));
        await expect(page.locator('#gwStatusModal')).toHaveClass(/show/);

        await expect(page.locator('#gwStatusTestRun')).not.toBeVisible();
    });
});
