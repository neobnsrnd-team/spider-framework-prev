/**
 * 관리자 작업이력 로그 — 목록·검색·정렬·엑셀·출력·상세 모달·권한
 *
 * FWK_USER_ACCESS_HIS 시드 데이터(e2e-seed.sql)를 기반으로 동작한다.
 * 이 페이지는 조회 전용이며, 검색 조건 없이도 전체 데이터가 조회된다.
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const LIST_API = '/api/admin-action-logs';

const isListResponse = (url: string) =>
    url.includes(LIST_API) && !url.includes('/export');

// ─── 공통 setup ──────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const listPromise = page.waitForResponse(r => isListResponse(r.url()));
    await page.goto('/admin-histories');
    await listPromise;
});

// ─── 목록 ──────────────────────────────────────────────────

test.describe('관리자 작업이력 로그 목록', () => {
    test('[조회] 초기 페이지 로드 시 size=10으로 데이터가 조회되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto('/admin-histories');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('10');

        await expect(page.locator('#_searchContainer_limitRows')).toHaveValue('10');

        const rows = page.locator('#logTableBody tr');
        expect(await rows.count()).toBeLessThanOrEqual(10);
    });

    test('[조회] 사용자ID 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#_searchContainer_userId').fill('e2e-admin');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#_searchContainer_searchBtn').click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('userId')).toBe('e2e-admin');
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('[조회] 접근 IP 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#_searchContainer_accessIp').fill('127.0.0.1');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#_searchContainer_searchBtn').click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('accessIp')).toBe('127.0.0.1');
    });

    test('[조회] 접근 URL 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#_searchContainer_accessUrl').fill('/api/users');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#_searchContainer_searchBtn').click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('accessUrl')).toBe('/api/users');
    });

    test('[조회] 존재하지 않는 사용자ID로 검색 시 데이터가 없어야 한다', async ({ page }) => {
        await page.locator('#_searchContainer_userId').fill('NON_EXIST_USER_XYZ_99999');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#_searchContainer_searchBtn').click();
        await resPromise;

        const rows = page.locator('#logTableBody tr');
        const count = await rows.count();
        if (count > 0) {
            await expect(rows.first()).not.toHaveAttribute('onclick');
        }
    });
});

// ─── 정렬 ──────────────────────────────────────────────────

test.describe('관리자 작업이력 로그 정렬', () => {
    test('[조회] 컬럼 헤더를 클릭하면 해당 컬럼 기준으로 오름차순 정렬되어야 한다', async ({
        page,
    }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#logTable thead th[data-sort="userId"]').click();
        const res1 = await res1Promise;

        const url1 = new URL(res1.request().url());
        expect(url1.searchParams.get('sortBy')).toBe('userId');
        expect(url1.searchParams.get('sortDirection')).toBe('ASC');

        await expect(page.locator('#logTable thead th[data-sort="userId"]')).toHaveClass(/sort-asc/);
    });

    test('[조회] 정렬된 컬럼을 다시 클릭하면 내림차순으로 변경되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#logTable thead th[data-sort="userId"]').click();
        await res1Promise;

        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#logTable thead th[data-sort="userId"]').click();
        const res2 = await res2Promise;

        const url2 = new URL(res2.request().url());
        expect(url2.searchParams.get('sortDirection')).toBe('DESC');
        await expect(page.locator('#logTable thead th[data-sort="userId"]')).toHaveClass(/sort-desc/);
    });

    test('[조회] 초기 로드 시 기본 정렬이 적용되지 않아야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto('/admin-histories');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('sortBy')).toBeNull();
    });
});

// ─── 엑셀·출력 ─────────────────────────────────────────────

test.describe('관리자 작업이력 로그 엑셀·출력', () => {
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
            window.print = () => {
                (window as any)._printCalled = true;
            };
            const origOpen = window.open.bind(window);
            window.open = (...args: Parameters<typeof window.open>) => {
                (window as any)._openCalled = true;
                return origOpen(...args);
            };
        });

        await page.getByRole('button', { name: LABEL.PRINT }).click();
        await page.waitForTimeout(500);

        const triggered = await page.evaluate(
            () => (window as any)._printCalled || (window as any)._openCalled,
        );
        expect(triggered).toBe(true);
    });
});

// ─── 상세 모달 ─────────────────────────────────────────────

test.describe('관리자 작업이력 로그 상세 모달', () => {
    test('[조회] 데이터 행을 클릭하면 상세 모달이 열리고 필드가 표시되어야 한다', async ({
        page,
    }) => {
        const rows = page.locator('#logTableBody tr[onclick]');
        const count = await rows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.locator('#adminActionLogDetailModal')).toBeVisible();

        await expect(page.locator('#adminActionLogDetailModal .modal-title')).toContainText(
            LABEL.ADMIN_ACTION_LOG_DETAIL_TITLE,
        );

        await expect(page.locator('#detailUserId')).toBeVisible();
        await expect(page.locator('#detailAccessDtime')).toBeVisible();
        await expect(page.locator('#detailAccessIp')).toBeVisible();
        await expect(page.locator('#detailAccessUrl')).toBeVisible();
        await expect(page.locator('#detailResultMessage')).toBeVisible();
        await expect(page.locator('#detailInputData')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.locator('#adminActionLogDetailModal')).not.toBeVisible();
    });

    test('[조회] 시드 데이터 행 클릭 시 모달에 올바른 데이터가 표시되어야 한다', async ({
        page,
    }) => {
        // e2e-admin 시드 데이터가 조회되도록 검색
        await page.locator('#_searchContainer_userId').fill('e2e-admin');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#_searchContainer_searchBtn').click();
        await searchPromise;

        const rows = page.locator('#logTableBody tr[onclick]');
        const count = await rows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.locator('#adminActionLogDetailModal')).toBeVisible();

        await expect(page.locator('#detailUserId')).toHaveValue('e2e-admin');

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.locator('#adminActionLogDetailModal')).not.toBeVisible();
    });

    test('[조회] 모달에서 닫기 버튼을 클릭하면 모달이 닫혀야 한다', async ({ page }) => {
        const rows = page.locator('#logTableBody tr[onclick]');
        const count = await rows.count();

        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.locator('#adminActionLogDetailModal')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.locator('#adminActionLogDetailModal')).not.toBeVisible();
    });
});

// ─── 권한 (쓰기 버튼 없음) ─────────────────────────────────
// 이 페이지는 조회 전용이므로 등록·저장·삭제 버튼이 존재해서는 안 된다

test.describe('관리자 작업이력 로그 권한', () => {
    test('[권한] 관리자 작업이력 로그 페이지에는 등록·저장·삭제 버튼이 존재해서는 안 된다', async ({
        page,
    }) => {
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();
    });
});
