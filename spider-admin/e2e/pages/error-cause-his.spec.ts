/**
 * 오류발생현황 — 목록·검색·정렬·엑셀·출력·상세 모달·권한 (11 tests)
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const API_PATH = '/api/error-histories/page';

const isListResponse = (url: string) => url.includes(API_PATH);

test.beforeEach(async ({ page }) => {
    const initSearch = page.waitForResponse(r => isListResponse(r.url()));
    await page.goto('/error-histories');
    await initSearch;
});

// ─── 목록 ──────────────────────────────────────────────────

test.describe('오류발생현황 목록', () => {
    test('[조회] 초기 페이지 로드 시 size=10으로 데이터가 조회되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto('/error-histories');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('10');

        await expect(page.locator('#limitRows')).toHaveValue('10');

        const rows = page.locator('#errorHisTableBody tr');
        expect(await rows.count()).toBeLessThanOrEqual(10);
    });

    test('[조회] 검색 조건 변경 시 API 요청에 page=1이 포함되어야 한다', async ({ page }) => {
        await page.locator('#searchErrorCode').fill('TEST_SEARCH');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('page')).toBe('1');
        expect(url.searchParams.get('errorCode')).toBe('TEST_SEARCH');
    });

    test('[조회] 고객ID 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#searchCustUserId').fill('test-user');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('custUserId')).toBe('test-user');
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('[조회] 컬럼 헤더를 클릭하면 해당 컬럼 기준으로 오름차순 정렬되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#errorHisTable thead th[data-sort="errorCode"]').click();
        const res1 = await res1Promise;

        const url1 = new URL(res1.request().url());
        expect(url1.searchParams.get('sortBy')).toBe('errorCode');
        expect(url1.searchParams.get('sortDirection')).toBe('ASC');

        await expect(page.locator('#errorHisTable thead th[data-sort="errorCode"]')).toHaveClass(/sort-asc/);
    });

    test('[조회] 정렬된 컬럼을 다시 클릭하면 내림차순으로 변경되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#errorHisTable thead th[data-sort="errorCode"]').click();
        await res1Promise;

        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#errorHisTable thead th[data-sort="errorCode"]').click();
        const res2 = await res2Promise;

        const url2 = new URL(res2.request().url());
        expect(url2.searchParams.get('sortDirection')).toBe('DESC');
        await expect(page.locator('#errorHisTable thead th[data-sort="errorCode"]')).toHaveClass(/sort-desc/);
    });

    test('[조회] 엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const [download] = await Promise.all([
            page.waitForEvent('download'),
            page.getByRole('button', { name: LABEL.EXCEL }).click(),
        ]);

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('[조회] 출력 버튼을 클릭하면 인쇄 동작이 실행되어야 한다', async ({ page }) => {
        await page.evaluate(() => {
            (window as any)._printCalled = false;
            (window as any)._openCalled = false;
            window.print = () => { (window as any)._printCalled = true; };
            const origOpen = window.open.bind(window);
            window.open = (...args: Parameters<typeof window.open>) => {
                (window as any)._openCalled = true;
                return origOpen(...args);
            };
        });

        await page.getByRole('button', { name: LABEL.PRINT }).click();
        await page.waitForTimeout(500);

        const triggered = await page.evaluate(() =>
            (window as any)._printCalled || (window as any)._openCalled,
        );
        expect(triggered).toBe(true);
    });
});

// ─── 상세 모달 ─────────────────────────────────────────────

test.describe('오류발생현황 상세 모달', () => {
    test('[조회] 데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page }) => {
        const rows = page.locator('#errorHisTableBody tr[onclick]');
        const count = await rows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // 모달 제목 확인
        await expect(page.locator('#errorHisModal .modal-title')).toBeVisible();

        // 상세 필드 렌더링 확인
        await expect(page.locator('#modalErrorCode')).toBeVisible();
        await expect(page.locator('#modalErrorSerNo')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });

    test('[조회] 모달에서 닫기 버튼을 클릭하면 모달이 닫혀야 한다', async ({ page }) => {
        const rows = page.locator('#errorHisTableBody tr[onclick]');
        const count = await rows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });
});

// ─── 권한 (쓰기 버튼 없음) ─────────────────────────────────
// 이 페이지는 조회 전용이므로 어떤 사용자에게도 쓰기 버튼이 없어야 한다

test.describe('오류발생현황 권한', () => {
    test('[권한] 오류발생현황 페이지에는 등록·저장·삭제 버튼이 존재해서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();
    });
});
