/**
 * 이행데이터 반영 페이지 — 목록 조회, 검색, 정렬, 모달, 권한 (읽기 전용 페이지).
 *
 * 시드 데이터: e2e/docker/e2e-seed.sql (FWK_TRANS_DATA_TIMES 12건, FWK_TRANS_DATA_HIS 15건)
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const SEED_USER_ID = 'e2e-admin';

const API_URL = '/api/trans/page';

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
    await page.goto('/trans-data');
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('이행데이터 반영 목록', () => {

    test('초기 페이지 로드 시 데이터는 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#transDataTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('사용자ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchUserId').fill(SEED_USER_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        const rows = page.locator('#transDataTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);
    });

    test('이행결과(실패)로 검색하면 해당 결과만 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        await page.locator('#searchTranResult').selectOption('F');

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        const rows = page.locator('#transDataTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page }) => {
        await page.locator('#searchUserId').fill(SEED_USER_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        await expect(page.locator('#pageInfo')).toContainText('1 -');
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        // 시드 데이터로 검색
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        // ASC 클릭
        const header = page.locator('th[data-sort="tranTime"]');
        const indicator = page.locator('[data-sort-indicator="tranTime"]');
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise;
        await expect(indicator).toHaveText('▲');

        // DESC 클릭
        const responsePromise2 = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise2;
        await expect(indicator).toHaveText('▼');

        // 해제 클릭
        const responsePromise3 = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise3;
        await expect(indicator).toHaveText('');
    });

    test('검색창에서 Enter를 누르면 검색이 실행되어야 한다', async ({ page }) => {
        await page.locator('#searchUserId').fill(SEED_USER_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.locator('#searchUserId').press('Enter');
        await responsePromise;

        const rows = page.locator('#transDataTableBody tr');
        await expect(rows.first()).toBeVisible();
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        // 시드 데이터 검색
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context }) => {
        // 시드 데이터 검색
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        await context.addInitScript(() => { window.print = () => {}; });
        const reloadPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.reload();
        await reloadPromise;
        await expect(page.getByRole('table')).toBeVisible();

        // 다시 검색
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        const searchPromise2 = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise2;

        const popupPromise = context.waitForEvent('page');
        await page.getByRole('button', { name: LABEL.PRINT }).click();
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await expect(popup.locator('table')).toBeVisible();
        await popup.close();
    });

    test('행 클릭 시 상세 모달이 표시되어야 한다', async ({ page }) => {
        // 시드 데이터 검색
        await page.locator('#searchUserId').fill(SEED_USER_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        // 첫 번째 행 클릭
        const firstRow = page.locator('#transDataTableBody tr').first();
        await expect(firstRow).toBeVisible();

        const detailPromise = page.waitForResponse(r => r.url().includes('/details'));
        await firstRow.click();
        await detailPromise;

        // 모달 표시 확인
        const modal = page.locator('#transDataModal');
        await expect(modal).toBeVisible();

        // 이행순번 필드 확인
        await expect(page.locator('#modalTranSeq')).toBeVisible();
        const tranSeqValue = await page.locator('#modalTranSeq').inputValue();
        expect(tranSeqValue).toBeTruthy();

        // 모달 닫기
        await modal.locator('button:has-text("닫기")').click();
    });
});

// ─── 권한 — R 권한 사용자 ──────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.goto('/trans-data');
        await responsePromise;
        await expect(page.getByRole('table')).toBeVisible();
    });

    test('R 권한 사용자에게는 Upload 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnUpload')).toBeHidden();
    });

    test('R 권한 사용자에게는 데이터 이행 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnTransData')).toBeHidden();
    });
});
