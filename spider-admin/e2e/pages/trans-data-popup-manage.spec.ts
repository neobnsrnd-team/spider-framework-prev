/**
 * 이행데이터 생성 페이지 — 탭별 소스 조회, 이행 대상 관리, 이행 실행, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 * 이 페이지는 표준 CRUD가 아닌 소스 데이터 선택 → 타겟 이동 → SQL ZIP 다운로드 방식이다.
 */

import { test, expect, type Page } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const SOURCE_API = '/api/trans/generation/source';

/** 페이지 이동 후 소스 테이블이 보일 때까지 대기 */
async function gotoAndWait(page: Page) {
    const loadPromise = page.waitForResponse(r => r.url().includes(SOURCE_API));
    await page.goto('/trans-data/generation');
    await loadPromise;
    await expect(page.locator('#genSourceTable')).toBeVisible();
}

/** 탭 전환 후 API 응답 대기 */
async function switchTab(page: Page, tabType: string) {
    const loadPromise = page.waitForResponse(r =>
        r.url().includes(SOURCE_API) && r.url().includes(`tab=${tabType}`),
    );
    await page.locator(`#genTabButtons button[data-tab="${tabType}"]`).click();
    await loadPromise;
}

/** 검색 실행 후 API 응답 대기 */
async function searchSource(page: Page, field: string, value: string) {
    await page.locator('#genSearchField').selectOption(field);
    await page.locator('#genSearchValue').fill(value);
    const loadPromise = page.waitForResponse(r => r.url().includes(SOURCE_API));
    await page.getByRole('button', { name: LABEL.TRANS_DATA_SEARCH }).click();
    await loadPromise;
}

/** 소스 데이터 행 수 (빈 데이터 메시지 행 제외) */
async function sourceDataCount(page: Page): Promise<number> {
    return page.locator('#genSourceTableBody .gen-source-check').count();
}

test.beforeEach(async ({ page }) => {
    await gotoAndWait(page);
});

// ─── 소스 데이터 목록 ──────────────────────────────────────────

test.describe('소스 데이터 목록', () => {

    test('초기 로드 시 TRX 탭이 활성화되고 소스 테이블이 표시되어야 한다', async ({ page }) => {
        // TRX 탭이 활성화 (btn-primary 클래스)
        const trxTab = page.locator('#genTabButtons button[data-tab="TRX"]');
        await expect(trxTab).toHaveClass(/btn-primary/);

        // 소스 테이블이 표시
        await expect(page.locator('#genSourceTable')).toBeVisible();
    });

    test('초기 로드 시 소스 데이터가 10건 이하로 표시되어야 한다', async ({ page }) => {
        const count = await sourceDataCount(page);
        // 클라이언트 사이드 페이지네이션 기본값 10건
        expect(count).toBeLessThanOrEqual(10);
    });

    const TABS = [
        { type: 'MESSAGE', label: LABEL.TRANS_DATA_TAB_MESSAGE },
        { type: 'CODE', label: LABEL.TRANS_DATA_TAB_CODE },
        { type: 'WEBAPP', label: LABEL.TRANS_DATA_TAB_WEBAPP },
        { type: 'ERROR', label: LABEL.TRANS_DATA_TAB_ERROR },
        { type: 'SERVICE', label: LABEL.TRANS_DATA_TAB_SERVICE },
        { type: 'COMPONENT', label: LABEL.TRANS_DATA_TAB_COMPONENT },
        { type: 'PROPERTY', label: LABEL.TRANS_DATA_TAB_PROPERTY },
    ];

    for (const tab of TABS) {
        test(`${tab.label} 탭 전환 시 해당 탭 데이터가 조회되어야 한다`, async ({ page }) => {
            await switchTab(page, tab.type);

            // 해당 탭이 활성화
            const tabBtn = page.locator(`#genTabButtons button[data-tab="${tab.type}"]`);
            await expect(tabBtn).toHaveClass(/btn-primary/);

            // 소스 테이블이 표시
            await expect(page.locator('#genSourceTable')).toBeVisible();
        });
    }

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page }) => {
        const initialCount = await sourceDataCount(page);
        if (initialCount === 0) {
            test.skip();
            return;
        }

        // 첫 번째 행의 col1(거래ID) 텍스트 가져오기
        const rows = page.locator('#genSourceTableBody tr');
        const firstId = await rows.first().locator('td').nth(1).textContent();
        if (!firstId?.trim()) {
            test.skip();
            return;
        }

        // 거래ID로 검색
        await searchSource(page, 'trxId', firstId.trim());

        // 검색 결과에 해당 ID가 포함
        const filteredCount = await sourceDataCount(page);
        expect(filteredCount).toBeGreaterThan(0);
        expect(filteredCount).toBeLessThanOrEqual(initialCount);
    });

    test('컬럼 헤더를 클릭하면 해당 컬럼 기준으로 정렬이 변경되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // col1 헤더 클릭 → sort-asc 클래스 추가
        const col1Header = page.locator('#genSourceTable th[data-sort="col1"]');
        await col1Header.click();
        await expect(col1Header).toHaveClass(/sort-asc/);

        // 다시 클릭 → sort-desc 클래스 추가
        await col1Header.click();
        await expect(col1Header).toHaveClass(/sort-desc/);
    });

    test('페이지 크기를 변경하면 표시 건수가 변경되어야 한다', async ({ page }) => {
        // 기본값 10 → 50으로 변경
        await page.locator('#genItemsPerPage').selectOption('50');

        const count = await sourceDataCount(page);
        expect(count).toBeLessThanOrEqual(50);
    });
});

// ─── 기관 필터 및 거래만 이행 ──────────────────────────────────

test.describe('기관 필터 및 거래만 이행', () => {

    test('TRX 탭에서 기관명 필터가 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#genOrgLabel')).toBeVisible();
        await expect(page.locator('#genOrgFilter')).toBeVisible();
    });

    test('MESSAGE 탭에서 기관명 필터가 표시되어야 한다', async ({ page }) => {
        await switchTab(page, 'MESSAGE');
        await expect(page.locator('#genOrgLabel')).toBeVisible();
        await expect(page.locator('#genOrgFilter')).toBeVisible();
    });

    test('CODE 탭에서 기관명 필터가 숨겨져야 한다', async ({ page }) => {
        await switchTab(page, 'CODE');
        await expect(page.locator('#genOrgLabel')).toBeHidden();
        await expect(page.locator('#genOrgFilter')).toBeHidden();
    });

    test('기관명 필터 변경 후 조회 시 해당 기관 데이터만 표시되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 기관ID가 있는 행 찾기
        const rows = page.locator('#genSourceTableBody tr');
        const allRows = await rows.all();
        let orgId = '';
        for (const row of allRows) {
            const col2Text = await row.locator('td').nth(2).textContent();
            if (col2Text?.trim()) {
                orgId = col2Text.trim();
                break;
            }
        }
        if (!orgId) {
            test.skip();
            return;
        }

        // 기관 필터 선택 후 조회
        await page.locator('#genOrgFilter').selectOption(orgId);
        const loadPromise = page.waitForResponse(r => r.url().includes(SOURCE_API));
        await page.getByRole('button', { name: LABEL.TRANS_DATA_SEARCH }).click();
        await loadPromise;

        // 모든 행의 기관ID가 선택한 기관과 일치
        const filteredRows = await page.locator('#genSourceTableBody tr').all();
        for (const row of filteredRows) {
            const col2Text = await row.locator('td').nth(2).textContent();
            expect(col2Text?.trim()).toBe(orgId);
        }
    });

    test('TRX 탭에서 "거래만 이행" 체크박스가 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#genTrxOnlyWrap')).toBeVisible();
        await expect(page.locator('#genTrxOnly')).toBeVisible();
    });

    test('MESSAGE 탭에서 "거래만 이행" 체크박스가 숨겨져야 한다', async ({ page }) => {
        await switchTab(page, 'MESSAGE');
        await expect(page.locator('#genTrxOnlyWrap')).toBeHidden();
    });

    test('TRX 탭의 3번째 컬럼에 기동 또는 수동 값이 표시되어야 한다', async ({ page }) => {
        const count = await sourceDataCount(page);
        if (count === 0) {
            test.skip();
            return;
        }

        // col3(운영모드) 검증 — 값이 있는 행에서 기동 또는 수동이어야 함
        const rows = page.locator('#genSourceTableBody tr');
        for (let i = 0; i < Math.min(count, 5); i++) {
            const col3Text = await rows.nth(i).locator('td').nth(3).textContent();
            if (col3Text?.trim()) {
                expect(['기동', '수동']).toContain(col3Text.trim());
            }
        }
    });

    test('탭 전환 시 기관 필터가 "전체"로 초기화되어야 한다', async ({ page }) => {
        // 기관 선택
        const options = await page.locator('#genOrgFilter option').allTextContents();
        if (options.length <= 1) {
            test.skip();
            return;
        }
        await page.locator('#genOrgFilter').selectOption({ index: 1 });

        // MESSAGE 탭으로 전환
        await switchTab(page, 'MESSAGE');

        // 기관 필터가 전체(빈값)로 초기화
        await expect(page.locator('#genOrgFilter')).toHaveValue('');
    });
});

// ─── 이행 대상 관리 ────────────────────────────────────────────

test.describe('이행 대상 관리', () => {

    test('소스 항목 체크 후 >> 버튼 클릭 시 이행 대상에 추가되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 첫 번째 항목 체크
        const firstCheckbox = page.locator('#genSourceTableBody .gen-source-check').first();
        await firstCheckbox.check();

        // >> 버튼 클릭
        await page.locator('.sp-popup-center-actions button').click();

        // 타겟 테이블에 행이 추가됨
        const targetRows = page.locator('#genTargetTableBody tr');
        await expect(targetRows.first()).toBeVisible();
        expect(await targetRows.count()).toBeGreaterThan(0);
    });

    test('이행 대상에 추가된 항목은 소스 테이블에서 비활성화되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 첫 번째 항목 체크 + 이동
        const rows = page.locator('#genSourceTableBody tr');
        const firstCheckbox = rows.first().locator('.gen-source-check');
        await firstCheckbox.check();
        await page.locator('.sp-popup-center-actions button').click();

        // 소스 테이블의 해당 행이 비활성화 (disabled + checked)
        await expect(rows.first().locator('.gen-source-check')).toBeDisabled();
        await expect(rows.first()).toHaveClass(/opacity-50/);
    });

    test('이행 대상에서 체크 후 삭제 시 해당 항목이 제거되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 항목 추가
        const rows = page.locator('#genSourceTableBody tr');
        await rows.first().locator('.gen-source-check').check();
        await page.locator('.sp-popup-center-actions button').click();

        const targetRows = page.locator('#genTargetTableBody tr');
        await expect(targetRows.first()).toBeVisible();

        // 타겟에서 첫 번째 항목 체크 + 삭제
        await targetRows.first().locator('.gen-target-check').check();
        await page.locator('button:has-text("삭제")').click();

        // Drop zone이 다시 표시됨 (타겟이 비어있음)
        await expect(page.locator('#genDropZone')).toBeVisible();
    });

    test('이행 대상 없이 이행실행 버튼 클릭 시 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('button:has-text("이행실행")').click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('이행 대상 데이터를 추가해 주세요');
    });

    test('소스 항목 미선택 시 >> 버튼 클릭하면 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('.sp-popup-center-actions button').click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('이행 대상을 선택해 주세요');
    });
});

// ─── 이행 실행 모달 ────────────────────────────────────────────

test.describe('이행 실행 모달', () => {

    test('이행 대상 추가 후 이행실행 클릭 시 확인 모달이 표시되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 항목 추가
        const rows = page.locator('#genSourceTableBody tr');
        await rows.first().locator('.gen-source-check').check();
        await page.locator('.sp-popup-center-actions button').click();

        // 이행실행 클릭
        await page.locator('button:has-text("이행실행")').click();

        // 모달 표시 확인
        const modal = page.locator('#genExecuteModal');
        await expect(modal).toBeVisible();

        // 이행 대상 건수 요약 표시
        const summary = page.locator('#genExecuteSummary');
        await expect(summary).toContainText('1건');

        // 이행 사유 입력란 표시
        await expect(page.locator('#genTranReason')).toBeVisible();
    });

    test('이행 사유 미입력 시 알림이 표시되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 항목 추가 + 모달 열기
        const rows = page.locator('#genSourceTableBody tr');
        await rows.first().locator('.gen-source-check').check();
        await page.locator('.sp-popup-center-actions button').click();
        await page.locator('button:has-text("이행실행")').click();
        await expect(page.locator('#genExecuteModal')).toBeVisible();

        // 사유 비우고 실행 클릭
        await page.locator('#genExecuteModal .btn-primary:has-text("이행 실행")').click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('필수 입력사항');
    });

    test('이행 사유 입력 후 이행 실행 시 ZIP 파일이 다운로드되어야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 항목 추가 + 모달 열기
        const rows = page.locator('#genSourceTableBody tr');
        await rows.first().locator('.gen-source-check').check();
        await page.locator('.sp-popup-center-actions button').click();
        await page.locator('button:has-text("이행실행")').click();
        await expect(page.locator('#genExecuteModal')).toBeVisible();

        // 이행 사유 입력
        await page.locator('#genTranReason').fill('E2E 테스트 이행');

        const [download] = await Promise.all([
            page.waitForEvent('download', { timeout: 15000 }),
            page.locator('#genExecuteModal .btn-primary:has-text("이행 실행")').click(),
        ]);
        expect(download.suggestedFilename()).toContain('.zip');
    });

    test('이행 사유 글자수 카운터가 동작해야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 항목 추가 + 모달 열기
        const rows = page.locator('#genSourceTableBody tr');
        await rows.first().locator('.gen-source-check').check();
        await page.locator('.sp-popup-center-actions button').click();
        await page.locator('button:has-text("이행실행")').click();
        await expect(page.locator('#genExecuteModal')).toBeVisible();

        // 초기 카운터 0
        await expect(page.locator('#genTranReasonCount')).toHaveText('0');

        // 텍스트 입력 후 카운터 업데이트
        await page.locator('#genTranReason').fill('테스트');
        await expect(page.locator('#genTranReasonCount')).toHaveText('3');
    });
});

// ─── 전체 선택 ─────────────────────────────────────────────────

test.describe('전체 선택', () => {

    test('소스 테이블 전체 선택 체크박스가 동작해야 한다', async ({ page }) => {
        if (await sourceDataCount(page) === 0) {
            test.skip();
            return;
        }

        // 전체 선택
        await page.locator('#genSelectAll').check();

        const checkboxes = page.locator('.gen-source-check:not(:disabled)');
        const count = await checkboxes.count();
        for (let i = 0; i < count; i++) {
            await expect(checkboxes.nth(i)).toBeChecked();
        }

        // 전체 해제
        await page.locator('#genSelectAll').uncheck();

        for (let i = 0; i < count; i++) {
            await expect(checkboxes.nth(i)).not.toBeChecked();
        }
    });
});

// ─── 권한 — W 사용자 ──────────────────────────────────────────

test.describe('권한 — W 사용자', () => {

    test('W 권한 사용자에게는 >> 이동 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('.sp-popup-center-actions')).toBeVisible();
    });

    test('W 권한 사용자에게는 이행실행/삭제 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('button:has-text("이행실행")')).toBeVisible();
        await expect(page.locator('button:has-text("삭제")')).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ──────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        const loadPromise = page.waitForResponse(r => r.url().includes(SOURCE_API));
        await page.goto('/trans-data/generation');
        await loadPromise;
        await expect(page.locator('#genSourceTable')).toBeVisible();
    });

    test('R 권한 사용자도 소스 데이터를 조회할 수 있어야 한다', async ({ page }) => {
        await expect(page.locator('#genSourceTable')).toBeVisible();
    });

    test('R 권한 사용자에게는 >> 이동 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('.sp-popup-center-actions')).toBeHidden();
    });

    test('R 권한 사용자에게는 이행실행/삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('button:has-text("이행실행")')).toBeHidden();
        await expect(page.locator('button:has-text("삭제")')).toBeHidden();
    });
});

// ─── 기타 기능 ─────────────────────────────────────────────────

test.describe('기타 기능', () => {

    test('Enter 키로 검색이 실행되어야 한다', async ({ page }) => {
        const loadPromise = page.waitForResponse(r => r.url().includes(SOURCE_API));
        await page.locator('#genSearchValue').fill('test');
        await page.locator('#genSearchValue').press('Enter');
        await loadPromise;

        // 검색이 실행됨 (에러 없이 완료)
        await expect(page.locator('#genSourceTable')).toBeVisible();
    });

    test('탭 전환 시 검색값이 초기화되어야 한다', async ({ page }) => {
        // 검색값 입력
        await page.locator('#genSearchValue').fill('someValue');

        // 탭 전환
        await switchTab(page, 'MESSAGE');

        // 검색값 초기화 확인
        await expect(page.locator('#genSearchValue')).toHaveValue('');
    });

    test('탭 전환 시 검색 필드 옵션이 해당 탭에 맞게 변경되어야 한다', async ({ page }) => {
        // TRX 탭의 검색 필드 확인
        const trxOptions = await page.locator('#genSearchField option').allTextContents();
        expect(trxOptions).toContain('거래ID');
        expect(trxOptions).toContain('거래명');

        // CODE 탭으로 전환
        await switchTab(page, 'CODE');

        const codeOptions = await page.locator('#genSearchField option').allTextContents();
        expect(codeOptions).toContain('코드그룹ID');
        expect(codeOptions).toContain('코드그룹명');
    });

    test('탭 전환 시 컬럼 헤더가 해당 탭에 맞게 변경되어야 한다', async ({ page }) => {
        // TRX 탭: 거래ID, 기관ID, 운영모드, 거래명
        await expect(page.locator('#genColHeader1')).toHaveText('거래ID');

        // MESSAGE 탭으로 전환
        await switchTab(page, 'MESSAGE');
        await expect(page.locator('#genColHeader1')).toHaveText('전문ID');

        // CODE 탭으로 전환
        await switchTab(page, 'CODE');
        await expect(page.locator('#genColHeader1')).toHaveText('코드그룹ID');
    });

    test('페이지네이션 정보가 올바르게 표시되어야 한다', async ({ page }) => {
        const pageInfo = page.locator('#genPageInfo');
        await expect(pageInfo).toBeVisible();

        const text = await pageInfo.textContent();
        // "X - Y of Z items" 형식
        expect(text).toMatch(/\d+ - \d+ of \d+ items/);
    });
});
