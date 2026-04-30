/**
 * 배치 수행 내역 페이지 — 목록 조회 (읽기 전용 페이지).
 *
 * 시드 데이터: e2e/docker/e2e-seed.sql (e2e-batch-his / E2E1 / 15건)
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const SEED_BATCH_APP_ID = 'e2e-batch-his';
const SEED_BATCH_DATE = '2026-03-10';  // input[type=date] 형식

const API_URL = '/api/batch/history/page';

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
    await page.goto('/batches/history');
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
    // 페이지 초기화 시 오늘 날짜가 자동 설정되므로 비워준다
    await page.locator('#searchBatchDate').fill('');
});

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('배치 수행 내역 목록', () => {

    test('초기 페이지 로드 시 데이터는 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#batchHisTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('배치 APP ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        await expect(page.getByRole('cell', { name: SEED_BATCH_APP_ID }).first()).toBeVisible();
    });

    test('기준일로 검색하면 해당 날짜의 결과만 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
        await page.locator('#searchBatchDate').fill(SEED_BATCH_DATE);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        const rows = page.locator('#batchHisTableBody tr');
        await expect(rows.first()).toBeVisible();
        const count = await rows.count();
        // 20260310에는 4건의 시드 데이터
        expect(count).toBe(4);
    });

    test('상태코드로 검색하면 해당 상태의 결과만 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
        await page.locator('#searchResRtCode').selectOption('-1');

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        const rows = page.locator('#batchHisTableBody tr');
        await expect(rows.first()).toBeVisible();
        // -1(오류) 상태는 1건
        const count = await rows.count();
        expect(count).toBe(1);
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page }) => {
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        await expect(page.locator('#pageInfo')).toContainText('1 -');
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        // 먼저 시드 데이터로 검색
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        // ASC 클릭
        const header = page.locator('th[data-sort="batchDate"]');
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise;
        await expect(header).toHaveClass(/sort-asc/);

        // DESC 클릭
        const responsePromise2 = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise2;
        await expect(header).toHaveClass(/sort-desc/);

        // 해제 클릭
        const responsePromise3 = page.waitForResponse(r => r.url().includes(API_URL));
        await header.click();
        await responsePromise3;
        await expect(header).not.toHaveClass(/sort-asc|sort-desc/);
    });

    test('검색창에서 Enter를 누르면 검색이 실행되어야 한다', async ({ page }) => {
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.locator('#searchBatchAppId').press('Enter');
        await responsePromise;

        await expect(page.getByRole('cell', { name: SEED_BATCH_APP_ID }).first()).toBeVisible();
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        // 시드 데이터 검색
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
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
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        await context.addInitScript(() => { window.print = () => {}; });
        const reloadPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.reload();
        await reloadPromise;
        await expect(page.getByRole('table')).toBeVisible();

        // reload 후 오늘 날짜가 다시 설정되므로 비워준다
        await page.locator('#searchBatchDate').fill('');
        // 다시 검색
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
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

    test('조회 버튼 클릭 전에 입력한 검색 조건은 페이지네이션에 영향을 주지 않아야 한다', async ({ page }) => {
        // 먼저 시드 데이터로 검색
        await page.locator('#searchBatchAppId').fill(SEED_BATCH_APP_ID);
        const searchPromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        // 검색 조건 변경하지만 조회 버튼 클릭 안함
        await page.locator('#searchBatchAppId').fill('no-such-batch-app');

        // 페이지 이동이 가능한 경우에만 테스트 (2페이지 이상일 때)
        const pageInfo = await page.locator('#pageInfo').textContent();
        if (pageInfo && !pageInfo.includes('of 0')) {
            // 새로고침 버튼으로 현재 조건 재조회
            const refreshPromise = page.waitForResponse(r => r.url().includes(API_URL));
            await page.locator('#btnRefresh').click();
            await refreshPromise;

            // 기존 검색 조건(e2e-batch-his)으로 조회되어야 함
            await expect(page.getByRole('cell', { name: SEED_BATCH_APP_ID }).first()).toBeVisible();
        }
    });
});
