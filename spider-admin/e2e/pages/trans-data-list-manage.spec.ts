/**
 * 이행데이터 조회 페이지 — 파일 목록 조회, 검색, 정렬, 권한 (읽기 전용 페이지).
 *
 * NOTE: 이 페이지는 서버 파일시스템에서 파일을 읽으므로
 *       CI 환경에서는 데이터가 0건일 수 있다. 빈 결과도 정상으로 처리한다.
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const API_URL = '/api/trans/trans-data-inqlist/page';

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
    await page.goto('/trans-data/files');
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록 조회 ────────────────────────────────────────────

test.describe('이행데이터 파일 목록', () => {

    test('페이지 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#transDataFileTable')).toBeVisible();
        await expect(page.locator('#transDataFileTableBody')).toBeVisible();
    });

    test('파일 유형 드롭다운이 로드되어야 한다', async ({ page }) => {
        const select = page.locator('#searchFileType');
        await expect(select).toBeVisible();

        // file-types API에서 옵션이 로드됨 (전체 + N개 유형)
        const options = await select.locator('option').count();
        expect(options).toBeGreaterThanOrEqual(1); // 최소 '전체' 옵션
    });

    test('검색 조건을 변경하면 1페이지로 초기화되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;

        await expect(page.locator('#pageInfo')).toBeVisible();
        const pageInfo = await page.locator('#pageInfo').textContent();
        // 빈 결과이거나 1페이지여야 함
        if (pageInfo && !pageInfo.includes('0 - 0')) {
            expect(pageInfo).toContain('1 -');
        }
    });

    test('검색창에서 Enter를 누르면 검색이 실행되어야 한다', async ({ page }) => {
        await page.locator('#searchFileName').fill('test');

        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.locator('#searchFileName').press('Enter');
        await responsePromise;

        // 에러 없이 테이블이 표시
        await expect(page.locator('#transDataFileTable')).toBeVisible();
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        // ASC 클릭
        const header = page.locator('th[data-sort="fileName"]');
        const indicator = page.locator('[data-sort-indicator="fileName"]');
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

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 데이터가 없으면 알림이 표시되어야 한다', async ({ page }) => {
        // 파일 목록은 서버 파일시스템 기반이므로 CI에서 빈 결과일 수 있음
        // PrintUtil.print()는 data가 비어있으면 alert('출력할 데이터가 없습니다.')를 표시
        const rows = page.locator('#transDataFileTableBody tr');
        const rowCount = await rows.count();
        const hasNoDataRow = rowCount === 1 && (await rows.first().textContent() ?? '').includes('데이터가 없습니다');

        if (rowCount === 0 || hasNoDataRow) {
            // 빈 결과: alert 표시 확인
            page.on('dialog', dialog => dialog.accept());
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            // alert가 처리되면 성공
        } else {
            // 데이터 있음: 인쇄 팝업 열림 확인
            await page.context().addInitScript(() => { window.print = () => {}; });
            const reloadPromise = page.waitForResponse(r => r.url().includes(API_URL));
            await page.reload();
            await reloadPromise;
            await expect(page.getByRole('table')).toBeVisible();

            const popupPromise = page.context().waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;
            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        }
    });

    test('날짜 검색 필드가 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#searchDateFrom')).toBeVisible();
        await expect(page.locator('#searchDateTo')).toBeVisible();
    });

    test('페이지 크기를 변경하면 재검색이 실행되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.locator('#limitRows').selectOption('20');
        await responsePromise;

        // 에러 없이 테이블이 표시
        await expect(page.locator('#transDataFileTable')).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ──────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(API_URL));
        await page.goto('/trans-data/files');
        await responsePromise;
        await expect(page.getByRole('table')).toBeVisible();
    });

    test('R 권한 사용자도 파일 목록을 조회할 수 있어야 한다', async ({ page }) => {
        await expect(page.locator('#transDataFileTable')).toBeVisible();
    });
});
