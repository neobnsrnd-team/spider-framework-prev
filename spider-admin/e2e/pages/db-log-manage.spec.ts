/**
 * 거래추적로그조회(DB) 페이지 UI 동작 테스트 — /message-instances
 *
 * FWK_MESSAGE_INSTANCE 시드 데이터 (e2e-seed.sql):
 *   E2E-TRX-TRACKING-001: e2e-admin / E2EORG01 / 응답 (Q+S 2건)
 *   E2E-TRX-TRACKING-002: e2e-admin / E2EORG02 / 오류 (Q 1건)
 */

import { test, expect } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const LIST_API = '/api/message-instances/page';
const DETAIL_API = '/api/message-instances/tracking/';
const PAGE_URL = '/message-instances';

const isListResponse = (url: string) => url.includes(LIST_API);

// ─── 공통 helper ──────────────────────────────────────────

async function searchByUserId(page: import('@playwright/test').Page, userId: string) {
    await page.locator('#searchUserId').fill(userId);
    const resPromise = page.waitForResponse(r => isListResponse(r.url()));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    return resPromise;
}

// ─── 목록·검색 ─────────────────────────────────────────────

test.describe('거래추적로그조회 목록·검색', () => {

    test.beforeEach(async ({ page }) => {
        const listPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        await listPromise;
    });

    test('[조회] 초기 페이지 로드 시 size=10으로 데이터가 조회되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('10');
        await expect(page.locator('#limitRows')).toHaveValue('10');
    });

    test('[조회] userId 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        const res = await searchByUserId(page, 'e2e-admin');

        const url = new URL(res.request().url());
        expect(url.searchParams.get('userId')).toBe('e2e-admin');
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('[조회] 거래추적번호 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#searchTrxTrackingNo').fill('E2E-TRX-TRACKING-001');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('trxTrackingNo')).toBe('E2E-TRX-TRACKING-001');
    });

    test('[조회] 기관ID 검색 조건이 API 요청에 반영되어야 한다', async ({ page }) => {
        await page.locator('#searchOrgId').fill('E2EORG01');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('orgId')).toBe('E2EORG01');
    });

    test('[조회] 검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page }) => {
        const res = await searchByUserId(page, 'e2e-admin');

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(body.data.totalElements).toBeGreaterThanOrEqual(1);

        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no]');
        expect(await rows.count()).toBeGreaterThanOrEqual(1);
    });

    test('[조회] 존재하지 않는 조건으로 검색 시 "조회된 데이터가 없습니다" 메시지가 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchTrxTrackingNo').fill('NON_EXIST_TRX_XYZ_99999');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await resPromise;

        await expect(page.locator('#messageInstanceTableBody')).toContainText('조회된 데이터가 없습니다');
    });

    test('[조회] 검색 조건을 변경하면 페이지가 1로 초기화되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await res1Promise;

        await page.locator('#searchUserId').fill('e2e-admin');
        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res2 = await res2Promise;

        const url = new URL(res2.request().url());
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('[조회] Enter 키 입력으로 검색이 실행되어야 한다', async ({ page }) => {
        await page.locator('#searchUserId').fill('e2e-admin');

        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#searchUserId').press('Enter');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('userId')).toBe('e2e-admin');
    });

    test('[조회] limitRows 변경 시 해당 size로 재조회되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#limitRows').selectOption('20');
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('size')).toBe('20');
    });
});

// ─── 정렬 ──────────────────────────────────────────────────

test.describe('거래추적로그조회 정렬', () => {

    test.beforeEach(async ({ page }) => {
        const listPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        await listPromise;
    });

    test('[조회] 컬럼 헤더를 클릭하면 해당 컬럼 기준으로 오름차순 정렬이 적용되어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#messageInstanceTable thead th[data-sort="trxDtime"]').click();
        const res = await resPromise;

        const url = new URL(res.request().url());
        expect(url.searchParams.get('sortBy')).toBe('trxDtime');
        expect(url.searchParams.get('sortDirection')).toBe('ASC');
    });

    test('[조회] 정렬된 컬럼을 다시 클릭하면 내림차순으로 변경되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#messageInstanceTable thead th[data-sort="trxDtime"]').click();
        await res1Promise;

        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.locator('#messageInstanceTable thead th[data-sort="trxDtime"]').click();
        const res2 = await res2Promise;

        const url = new URL(res2.request().url());
        expect(url.searchParams.get('sortDirection')).toBe('DESC');
    });
});

// 초기 로드 정렬 파라미터는 별도 describe로 분리 (beforeEach goto 중복 방지)
test.describe('거래추적로그조회 정렬 — 초기 상태', () => {

    test('[조회] 초기 로드 시 정렬 기준이 비어있어야 한다', async ({ page }) => {
        const resPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        const res = await resPromise;

        const url = new URL(res.request().url());
        const sortBy = url.searchParams.get('sortBy');
        // JS는 초기 sortBy를 빈 문자열('')로 전송함 (null 또는 '' 모두 허용)
        expect(sortBy === null || sortBy === '').toBe(true);
    });
});

// ─── 상세 모달 ─────────────────────────────────────────────

test.describe('거래추적로그조회 상세 모달', () => {

    test.beforeEach(async ({ page }) => {
        const listPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        await listPromise;

        await page.locator('#searchUserId').fill('e2e-admin');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;
    });

    test('[조회] 데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page }) => {
        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no]');
        if (await rows.count() === 0) { test.skip(); return; }

        const detailPromise = page.waitForResponse(r => r.url().includes(DETAIL_API));
        await rows.first().click();
        await detailPromise;

        await expect(page.locator('#messageInstanceDetailModal')).toBeVisible();
        await expect(page.locator('#messageInstanceDetailModal .modal-title')).toContainText('전문 송수신 로그');
    });

    test('[조회] 상세 모달에 거래추적번호가 올바르게 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchTrxTrackingNo').fill('E2E-TRX-TRACKING-001');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no="E2E-TRX-TRACKING-001"]');
        if (await rows.count() === 0) { test.skip(); return; }

        const detailPromise = page.waitForResponse(r => r.url().includes(DETAIL_API));
        await rows.first().click();
        await detailPromise;

        await expect(page.locator('#messageInstanceDetailModal')).toBeVisible();
        await expect(page.locator('#messageInstanceDetailModal .modal-body')).toContainText('E2E-TRX-TRACKING-001');
    });

    test('[조회] 동일 거래추적번호에 여러 전문이 있을 때 탭으로 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchTrxTrackingNo').fill('E2E-TRX-TRACKING-001');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no="E2E-TRX-TRACKING-001"]');
        if (await rows.count() === 0) { test.skip(); return; }

        const detailPromise = page.waitForResponse(r => r.url().includes(DETAIL_API));
        await rows.first().click();
        await detailPromise;

        // 시드: Q+S 2건 → 탭 2개
        const tabs = page.locator('#detailTabs .nav-link');
        await expect(tabs).toHaveCount(2);
    });

    test('[조회] 상세 모달에서 닫기 버튼을 클릭하면 모달이 닫혀야 한다', async ({ page }) => {
        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no]');
        if (await rows.count() === 0) { test.skip(); return; }

        const detailPromise = page.waitForResponse(r => r.url().includes(DETAIL_API));
        await rows.first().click();
        await detailPromise;

        await expect(page.locator('#messageInstanceDetailModal')).toBeVisible();
        await page.getByRole('button', { name: LABEL.CLOSE }).first().click();
        await expect(page.locator('#messageInstanceDetailModal')).not.toBeVisible();
    });

    test('[조회] 로그메시지 영역을 클릭하면 파싱결과 팝업이 열려야 한다', async ({ page }) => {
        test.setTimeout(30_000); // beforeEach 로딩 + 파싱 API 비동기 응답 시간 여유
        await page.locator('#searchTrxTrackingNo').fill('E2E-TRX-TRACKING-001');
        const searchPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchPromise;

        const rows = page.locator('#messageInstanceTableBody tr[data-trx-tracking-no="E2E-TRX-TRACKING-001"]');
        if (await rows.count() === 0) { test.skip(); return; }

        const detailPromise = page.waitForResponse(r => r.url().includes(DETAIL_API));
        await rows.first().click();
        await detailPromise;

        await expect(page.locator('#messageInstanceDetailModal')).toBeVisible();

        // 로그메시지 textarea 클릭 → 파싱(비동기 API) 또는 논파싱 팝업
        const logArea = page.locator('#messageInstanceDetailModal .log-message-area').first();
        await logArea.click();

        // 파싱 API 비동기 완료 후 모달이 열릴 때까지 대기 (parseResultModal 또는 rawResultModal)
        await page.locator('#parseResultModal.show').or(page.locator('#rawResultModal.show'))
            .waitFor({ state: 'visible', timeout: 8000 });
    });
});

// ─── 권한 ──────────────────────────────────────────────────

test.describe('거래추적로그조회 권한', () => {

    test.beforeEach(async ({ page }) => {
        const listPromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.goto(PAGE_URL);
        await listPromise;
    });

    test('[권한] 조회 전용 페이지이므로 등록·저장·삭제 버튼이 존재해서는 안 된다', async ({ page }) => {
        test.setTimeout(20_000);
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.DELETE })).not.toBeVisible();
    });

    test.describe('권한 — R 권한 사용자', () => {
        test.use({ storageState: 'e2e/.auth/session-readonly.json' });

        test('[권한] R 권한 사용자도 조회 기능은 정상 동작해야 한다', async ({ page }) => {
            test.setTimeout(30_000); // 메뉴 로딩 포함 초기화에 여유 부여
            const listPromise = page.waitForResponse(r => isListResponse(r.url()), { timeout: 25_000 });
            await page.goto(PAGE_URL);
            const res = await listPromise;
            expect(res.status()).toBe(200);
        });
    });
});
