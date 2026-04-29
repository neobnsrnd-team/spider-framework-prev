/**
 * 거래중지이력 — 목록·검색·정렬·페이지네이션·엑셀·출력·권한
 *
 * 조회 전용 페이지 (TRX_STOP_HISTORY:R) — 등록·수정·삭제 버튼 없음
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const PAGE_URL = '/admin-histories/trx-stop-history';
const API_PATH = '/api/trx-stop-histories';

const isListResponse = (url: string) =>
    url.includes(API_PATH) && !url.includes('/trx/') && !url.includes('/export');

// ─── 공통 초기화 ──────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const initSearch = page.waitForResponse(r => isListResponse(r.url()));
    await page.goto(PAGE_URL);
    await initSearch;
});

// ─── 목록·검색 ────────────────────────────────────────────

test.describe('거래중지이력 목록', () => {
    test('[조회] 초기 페이지 로드 시 size=10으로 데이터가 조회되어야 한다', async ({ page }) => {
        // beforeEach에서 이미 goto 완료 — 조회 버튼으로 API 요청 유발 (중복 goto 제거)
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('10');

        await expect(page.locator('#limitRows')).toHaveValue('10');

        const rows = page.locator('#historyTableBody tr');
        expect(await rows.count()).toBeLessThanOrEqual(10);
    });

    test('[조회] 조회 버튼 클릭 시 API 요청에 page=0이 포함되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('page')).toBe('0');
    });

    test('[조회] 구분유형 T 필터로 조회하면 거래 타입 데이터가 조회되어야 한다', async ({ page }) => {
        await page.locator('#searchGubunType').selectOption('T');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('gubunType')).toBe('T');
        expect(url.searchParams.get('page')).toBe('0');

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);
    });

    test('[조회] 구분유형 S 필터로 조회하면 서비스 타입 데이터가 조회되어야 한다', async ({ page }) => {
        await page.locator('#searchGubunType').selectOption('S');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('gubunType')).toBe('S');

        const body = await res.json();
        expect(body.success).toBe(true);
        // E2E 시드 데이터로 S타입 5건 추가됨
        expect(body.data.totalElements).toBeGreaterThan(0);
    });

    test('[조회] 거래ID 부분 문자열로 검색하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page }) => {
        // E2E 시드 데이터 E2E-TRX-001 기준 부분 검색
        await page.locator('#searchTrxId').fill('E2E-TRX');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('trxId')).toBe('E2E-TRX');
        expect(url.searchParams.get('page')).toBe('0');

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThan(0);
    });

    test('[조회] 검색 후 currentPage가 1로 리셋되어야 한다', async ({ page }) => {
        // 2페이지로 이동 (size=10 기본값, 데이터가 충분하면 2페이지 존재)
        const page2Link = page.locator('#pagination a').filter({ hasText: '2' }).first();
        const page2Visible = await page2Link.isVisible().catch(() => false);

        if (page2Visible) {
            const page2Promise = page.waitForResponse(r => isListResponse(r.url()));
            await page2Link.click();
            await page2Promise;
        }

        // 조회 버튼 클릭 시 page=0으로 리셋되어야 한다
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await searchPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('page')).toBe('0');
    });

    test('[조회] 페이지 사이즈 변경 시 변경된 사이즈로 조회되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#limitRows').selectOption('20');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('20');
        expect(url.searchParams.get('page')).toBe('0');
    });

    test('[조회] 날짜 선택 후 조회 버튼 없이 페이지 이동 시 날짜 조건이 반영되어서는 안 된다', async ({ page }) => {
        // 날짜 입력 (조회 버튼 클릭하지 않음)
        await page.locator('#searchTrxStopDtime').fill('2026-03-01');

        // 2페이지 이동 — appliedSearch에는 날짜가 없어야 함
        const page2Link = page.locator('#pagination a').filter({ hasText: '2' }).first();
        if (!(await page2Link.isVisible().catch(() => false))) {
            test.skip();
            return;
        }

        const page2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page2Link.click();
        const res = await page2Promise;

        const url = new URL(res.request().url());
        // 조회 버튼을 누르지 않았으므로 startDtime/endDtime이 없어야 함
        expect(url.searchParams.get('startDtime')).toBeNull();
        expect(url.searchParams.get('endDtime')).toBeNull();
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('[조회] 날짜로 검색 후 날짜를 지워도 페이지 이동 시 이전 날짜 조건이 유지되어야 한다', async ({ page }) => {
        // 날짜 검색 후 조회
        await page.locator('#searchTrxStopDtime').fill('2026-03-01');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        // 날짜 초기화 (조회 버튼 누르지 않음)
        await page.locator('#searchTrxStopDtime').fill('');

        // 2페이지 이동 — appliedSearch에는 날짜가 여전히 있어야 함
        const page2Link = page.locator('#pagination a').filter({ hasText: '2' }).first();
        if (!(await page2Link.isVisible().catch(() => false))) {
            test.skip();
            return;
        }

        const page2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page2Link.click();
        const res = await page2Promise;

        const url = new URL(res.request().url());
        // 마지막 조회 버튼 기준 날짜 조건이 유지되어야 함
        expect(url.searchParams.get('startDtime')).toBe('20260301000000');
        expect(url.searchParams.get('endDtime')).toBe('20260301235959');
    });
});

// ─── 정렬 ─────────────────────────────────────────────────

test.describe('거래중지이력 정렬', () => {
    test('[조회] 컬럼 헤더를 클릭하면 해당 컬럼 기준으로 오름차순 정렬되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#historyTable thead th[data-sort]').first().click();
        const res1 = await res1Promise;

        const url1 = new URL(res1.request().url());
        expect(url1.searchParams.get('sortDirection')).toBe('ASC');

        await expect(page.locator('#historyTable thead th[data-sort].sort-asc').first()).toBeVisible();
    });

    test('[조회] 정렬된 컬럼을 다시 클릭하면 내림차순으로 변경되어야 한다', async ({ page }) => {
        const header = page.locator('#historyTable thead th[data-sort]').first();

        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await header.click();
        await res1Promise;

        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await header.click();
        const res2 = await res2Promise;

        const url2 = new URL(res2.request().url());
        expect(url2.searchParams.get('sortDirection')).toBe('DESC');

        await expect(page.locator('#historyTable thead th[data-sort].sort-desc').first()).toBeVisible();
    });

    test('[조회] 내림차순 컬럼을 한 번 더 클릭하면 정렬이 해제되어야 한다', async ({ page }) => {
        const header = page.locator('#historyTable thead th[data-sort]').first();

        // ASC
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await header.click();
        await res1Promise;

        // DESC
        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await header.click();
        await res2Promise;

        // 정렬 해제
        const res3Promise = page.waitForResponse(r => isListResponse(r.url()));
        await header.click();
        const res3 = await res3Promise;

        const url3 = new URL(res3.request().url());
        expect(url3.searchParams.get('sortBy')).toBeNull();
        expect(url3.searchParams.get('sortDirection')).toBeNull();
    });
});

// ─── 페이지네이션 ─────────────────────────────────────────

test.describe('거래중지이력 페이지네이션', () => {
    test('[조회] 페이지네이션 컨트롤이 화면에 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#pagination')).toBeVisible();
        await expect(page.locator('#pageInfo')).toBeVisible();
    });

    test('[조회] 다음 페이지로 이동 시 page 값이 증가되어야 한다', async ({ page }) => {
        // size=10 기본값으로 데이터가 10건 초과하면 2페이지 존재
        const page2Link = page.locator('#pagination a').filter({ hasText: '2' }).first();
        if (!(await page2Link.isVisible())) {
            test.skip();
            return;
        }

        const page2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page2Link.click();
        const page2Res = await page2Promise;

        const url2 = new URL(page2Res.request().url());
        expect(url2.searchParams.get('page')).toBe('1'); // 2페이지 = page=1 (0-based)
    });
});

// ─── 엑셀·출력 ────────────────────────────────────────────

test.describe('거래중지이력 엑셀·출력', () => {
    test('[조회] 엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const [download] = await Promise.all([
            page.waitForEvent('download'),
            page.getByRole('button', { name: LABEL.EXCEL }).click(),
        ]);

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('[조회] 출력 버튼을 클릭하면 인쇄 동작이 실행되어야 한다', async ({ page }) => {
        // PrintUtil.print()는 currentData가 비어있으면 early-return — 먼저 조회하여 데이터 로드
        const searchRes = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchRes;
        await page.waitForSelector('#historyTableBody tr td');

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
        await page.waitForFunction(() => (window as any)._printCalled || (window as any)._openCalled);

        const triggered = await page.evaluate(
            () => (window as any)._printCalled || (window as any)._openCalled,
        );
        expect(triggered).toBe(true);
    });
});

// ─── 권한 ─────────────────────────────────────────────────
// 거래중지이력은 조회 전용 — 모든 사용자에게 쓰기 버튼이 없어야 함

test.describe('거래중지이력 권한', () => {
    test('[권한] 거래중지이력 페이지에는 등록·저장·삭제 버튼이 존재해서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();
    });
});
