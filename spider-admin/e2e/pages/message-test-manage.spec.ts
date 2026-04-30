/**
 * 전문 테스트 페이지 — 페이지 로드, 거래 조회 모달, 결과 조회, 권한.
 *
 * 이 페이지는 표준 CRUD 목록이 아닌, 거래 선택 후 전문 필드를 조회하고
 * 시뮬레이션/테스트 케이스를 실행하는 테스트 실행 페이지이다.
 *
 * CI 환경에서는 거래(FWK_TRX_MESSAGE) 데이터가 없을 수 있으므로,
 * 거래 데이터 의존 테스트는 빈 결과를 정상으로 처리한다.
 */

import { test, expect, type Page } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const INSTANCE_API = '/api/message-test/instance-ids';
const TRX_PAGE_API = '/api/trx-messages/page';

/** 페이지 이동 + 인스턴스 ID 로드 완료 대기 + 이벤트 리스너 초기화 완료 대기 */
async function gotoAndWaitReady(page: Page) {
    const instancePromise = page.waitForResponse(
        r => r.url().includes(INSTANCE_API),
        { timeout: 15000 },
    );
    await page.goto('/message-tests');
    await expect(page.locator('#searchForm')).toBeVisible();
    await instancePromise;

    // 인스턴스 ID 옵션이 DOM에 반영될 때까지 대기
    await page.waitForFunction(
        () => {
            const sel = document.getElementById('instanceId');
            return sel && sel.querySelectorAll('option').length > 1;
        },
        { timeout: 10000 },
    );

    // setupEventListeners()가 setTimeout(100) 내에서 실행되므로
    // _initialized 플래그 설정 완료까지 대기
    await page.waitForFunction(
        () => {
            const el = document.getElementById('instanceId') as any;
            return el && el._initialized === true;
        },
        { timeout: 5000 },
    );
}

/** 거래 조회 모달을 열고, 목록 API 응답을 대기한 뒤 모달이 열린 상태를 반환 */
async function openTrxModalAndSearch(page: Page) {
    await page.locator('#btnTrxSearch').click();
    await expect(page.locator('#trxSearchModal')).toBeVisible();

    const trxPromise = page.waitForResponse(r => r.url().includes(TRX_PAGE_API));
    await page.locator('#btnModalSearch').click();
    await trxPromise;
}

// ─── 페이지 로드 ────────────────────────────────────────────────

test.describe('페이지 로드', () => {

    test.beforeEach(async ({ page }) => {
        await gotoAndWaitReady(page);
    });

    test('페이지가 로드되어야 한다', async ({ page }) => {
        await expect(page.locator('.page-title')).toContainText(LABEL.MSG_TEST_TITLE);
        await expect(page.locator('#searchForm')).toBeVisible();
    });

    test('인스턴스ID 목록이 로드되어야 한다', async ({ page }) => {
        const instanceSelect = page.locator('#instanceId');
        await expect(instanceSelect).toBeVisible();

        // "선택하세요" 기본 옵션 + E2E1 시드 데이터
        const options = await instanceSelect.locator('option').allTextContents();
        expect(options.length).toBeGreaterThanOrEqual(2);
        expect(options).toContain('E2E1');
    });

    test('거래조회 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnTrxSearch')).toBeVisible();
        await expect(page.locator('#btnTrxSearch')).toHaveText(LABEL.MSG_TEST_TRX_SEARCH);
    });

    test('조회 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnSearch')).toBeVisible();
        await expect(page.locator('#btnSearch')).toHaveText(LABEL.SEARCH);
    });

    test('결과 영역이 초기에는 숨겨져 있어야 한다', async ({ page }) => {
        await expect(page.locator('#resultSection')).toBeHidden();
    });

    test('기관명, 거래명, 거래ID 입력 필드가 읽기전용이어야 한다', async ({ page }) => {
        await expect(page.locator('#orgName')).toHaveAttribute('readonly', '');
        await expect(page.locator('#trxName')).toHaveAttribute('readonly', '');
        await expect(page.locator('#trxId')).toHaveAttribute('readonly', '');
    });
});

// ─── 거래 조회 모달 ────────────────────────────────────────────────

test.describe('거래 조회 모달', () => {

    test.beforeEach(async ({ page }) => {
        await gotoAndWaitReady(page);
    });

    test('거래조회 버튼 클릭 시 모달이 열려야 한다', async ({ page }) => {
        await page.locator('#btnTrxSearch').click();

        const modal = page.locator('#trxSearchModal');
        await expect(modal).toBeVisible();
        await expect(modal.locator('.modal-title')).toHaveText('거래 리스트');
    });

    test('모달에 기관ID 선택, 거래ID 입력, 거래명 입력 필드가 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnTrxSearch').click();
        await expect(page.locator('#trxSearchModal')).toBeVisible();

        await expect(page.locator('#modalOrgId')).toBeVisible();
        await expect(page.locator('#modalTrxId')).toBeVisible();
        await expect(page.locator('#modalTrxName')).toBeVisible();
    });

    test('모달에서 조회 클릭 시 결과가 표시되어야 한다', async ({ page }) => {
        await openTrxModalAndSearch(page);

        // 결과 테이블에 행이 있거나 "조회 결과가 없습니다" 메시지가 있어야 한다
        const rows = page.locator('#trxModalTableBody tr');
        await expect(rows.first()).toBeVisible();

        // 총 건수가 표시되어야 한다
        const totalCount = page.locator('#trxTotalCount');
        await expect(totalCount).toBeVisible();
    });

    test('모달 닫기 버튼 클릭 시 모달이 닫혀야 한다', async ({ page }) => {
        await page.locator('#btnTrxSearch').click();
        const modal = page.locator('#trxSearchModal');
        await expect(modal).toBeVisible();

        await modal.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(modal).not.toBeVisible();
    });

    test('거래 행 클릭 시 기관명, 거래ID, 거래명이 검색 폼에 채워져야 한다', async ({ page }) => {
        await openTrxModalAndSearch(page);

        // 데이터가 있는 경우에만 행 클릭 테스트
        const rows = page.locator('#trxModalTableBody tr');
        const firstRowText = await rows.first().textContent();
        if (firstRowText?.includes('조회 결과가 없습니다')) {
            test.skip();
            return;
        }

        // 첫 번째 행 클릭
        await rows.first().click();

        // 모달이 닫힘
        await expect(page.locator('#trxSearchModal')).not.toBeVisible();

        // 검색 폼에 값이 채워짐
        const trxIdValue = await page.locator('#trxId').inputValue();
        expect(trxIdValue).not.toBe('');
    });
});

// ─── 조회 기능 ────────────────────────────────────────────────

test.describe('조회 기능', () => {

    test.beforeEach(async ({ page }) => {
        await gotoAndWaitReady(page);
    });

    test('거래 미선택 상태에서 조회 클릭 시 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnSearch').click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('거래조회 버튼을 눌러 거래를 선택해주세요');
    });

    test('거래 선택 후 조회 시 결과 영역이 표시되어야 한다', async ({ page }) => {
        // 거래 모달 열기 및 조회
        await openTrxModalAndSearch(page);

        // 데이터 확인
        const rows = page.locator('#trxModalTableBody tr');
        const firstRowText = await rows.first().textContent();
        if (firstRowText?.includes('조회 결과가 없습니다')) {
            test.skip();
            return;
        }

        // 거래 선택
        await rows.first().click();
        await expect(page.locator('#trxSearchModal')).not.toBeVisible();

        // 조회 버튼 클릭 → 필드 API 응답 대기
        const fieldsPromise = page.waitForResponse(r => r.url().includes('/api/message-test/fields'));
        await page.locator('#btnSearch').click();
        await fieldsPromise;

        // 결과 영역이 표시되거나 필드가 없어서 숨겨진 상태를 허용
        // (거래에 따라 필드가 없을 수 있음)
        const resultSection = page.locator('#resultSection');
        const isVisible = await resultSection.isVisible();
        if (isVisible) {
            await expect(page.locator('#resultTable')).toBeVisible();
        }
    });
});

// ─── 테스트 케이스 저장 버튼 ────────────────────────────────────────

test.describe('테스트 케이스 저장 버튼', () => {

    test.beforeEach(async ({ page }) => {
        await gotoAndWaitReady(page);
    });

    test('W 권한 사용자에게는 결과 영역에 테스트 케이스 저장 버튼이 포함되어야 한다', async ({ page }) => {
        // e2e-admin은 TRX_TEST:W 보유
        // 결과 영역은 숨겨져 있지만, 버튼 자체는 DOM에 존재해야 한다
        const saveBtn = page.locator('#btnSaveTestCase');
        // th:if로 렌더링되므로 DOM에 존재
        await expect(saveBtn).toBeAttached();
    });

    test('결과 영역에 테스트 케이스 선택 버튼이 표시되어야 한다', async ({ page }) => {
        const loadBtn = page.locator('#btnLoadTestCase');
        await expect(loadBtn).toBeAttached();
    });

    test('결과 영역에 시뮬레이션 버튼이 표시되어야 한다', async ({ page }) => {
        const simBtn = page.locator('#btnSimulation');
        await expect(simBtn).toBeAttached();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        await page.goto('/message-tests');
        await expect(page.locator('#searchForm')).toBeVisible();
    });

    test('R 권한 사용자도 페이지에 접근할 수 있어야 한다', async ({ page }) => {
        await expect(page.locator('.page-title')).toContainText(LABEL.MSG_TEST_TITLE);
        await expect(page.locator('#searchForm')).toBeVisible();
    });

    test('R 권한 사용자에게도 거래조회 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnTrxSearch')).toBeVisible();
    });

    test('R 권한 사용자에게는 테스트 케이스 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // th:if 조건으로 TRX_TEST:W가 없으면 DOM에서 제거됨
        const saveBtn = page.locator('#btnSaveTestCase');
        await expect(saveBtn).toHaveCount(0);
    });

    test('R 권한 사용자에게도 테스트 케이스 선택 버튼은 표시되어야 한다', async ({ page }) => {
        const loadBtn = page.locator('#btnLoadTestCase');
        await expect(loadBtn).toBeAttached();
    });

    test('R 권한 사용자에게도 시뮬레이션 버튼은 표시되어야 한다', async ({ page }) => {
        const simBtn = page.locator('#btnSimulation');
        await expect(simBtn).toBeAttached();
    });
});
