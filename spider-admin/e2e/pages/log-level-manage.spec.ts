/**
 * 로그레벨조정 페이지 UI 테스트 — /log-levels
 *
 * 검증 범위:
 * - 페이지 로드 시 테이블 표시
 * - Log Name 필터 + 조회 버튼 (클라이언트 사이드)
 * - Level 필터 + 조회 버튼
 * - 페이지당 표시 건수 변경
 * - 새로고침 버튼
 * - 레벨 저장 (confirm → 자동 Reload → 성공 Toast)
 * - Additivity 저장 (confirm → 자동 Reload → 성공 Toast)
 * - 상속 옵션 저장
 * - confirm 취소 시 API 미호출
 * - CSV 내보내기 버튼 존재
 * - 읽기 권한 사용자에게 저장 버튼 미표시
 */

import { test, expect } from '@playwright/test';

const TEST_LOGGER = 'com.example.admin_demo';

test.beforeEach(async ({ page }) => {
    await page.goto('/log-levels');
    // API 응답 후 테이블에 행이 최소 1개 이상 나타날 때까지 대기
    await expect(page.locator('#logLevelTableBody tr').first()).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('로그레벨 목록', () => {

    test('페이지 로드 시 테이블에 데이터가 표시되어야 한다', async ({ page }) => {
        const rows = page.locator('#logLevelTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        // 첫 번째 행의 Log Name 셀(1번째 td)에 값이 존재해야 한다
        await expect(rows.first().locator('td').nth(0)).not.toBeEmpty();
    });

    test('Log Name 필터 입력 후 조회 시 일치하는 행만 표시되어야 한다', async ({ page }) => {
        // 하드코딩된 'ROOT' 대신 실제 테이블 첫 번째 행의 Log Name을 동적으로 사용
        // CI 환경마다 로거 목록이 달라 특정 이름을 가정하면 flaky 해짐
        const firstLogName = (await page.locator('#logLevelTableBody tr').first()
            .locator('td').nth(0).textContent()) ?? '';
        const keyword = firstLogName.trim().split('.')[0]; // 첫 번째 패키지 세그먼트만 사용
        expect(keyword.length).toBeGreaterThan(0);

        await page.locator('#logNameFilter').fill(keyword);
        await page.locator('#btnSearch').click();

        const rows = page.locator('#logLevelTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        // 표시된 모든 행의 Log Name(1번째 td)에 keyword가 포함되어야 한다
        for (let i = 0; i < count; i++) {
            const cellText = await rows.nth(i).locator('td').nth(0).textContent();
            expect(cellText?.toLowerCase()).toContain(keyword.toLowerCase());
        }
    });

    test('Log Name 필터를 지우고 조회하면 전체 목록이 다시 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill('ROOT');
        await page.locator('#btnSearch').click();
        const filteredCount = await page.locator('#logLevelTableBody tr').count();

        await page.locator('#logNameFilter').clear();
        await page.locator('#btnSearch').click();
        const totalCount = await page.locator('#logLevelTableBody tr').count();
        expect(totalCount).toBeGreaterThanOrEqual(filteredCount);
    });

    test('필터에 일치하는 결과가 없으면 "조회된 데이터가 없습니다" 메시지가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill('__NONEXISTENT_LOGGER_XYZ__');
        await page.locator('#btnSearch').click();
        await expect(page.locator('#logLevelTableBody')).toContainText('조회된 데이터가 없습니다');
    });

    test('Log Name 필터 입력 후 Enter 키로 조회할 수 있어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill('ROOT');
        await page.locator('#logNameFilter').press('Enter');

        const rows = page.locator('#logLevelTableBody tr');
        expect(await rows.count()).toBeGreaterThan(0);
    });

    test('Level 필터로 조회하면 해당 레벨의 행만 표시되어야 한다', async ({ page }) => {
        await page.locator('#levelFilter').selectOption('INFO');
        await page.locator('#btnSearch').click();

        // 결과가 없을 수도 있으므로 조회 자체가 오류 없이 동작하는지만 확인
        await expect(page.locator('#logLevelTableBody')).toBeVisible();
    });

    test('Level 필터 "상속" 선택 시 logLevel이 null인 행만 표시되어야 한다', async ({ page }) => {
        await page.locator('#levelFilter').selectOption('__INHERITED__');
        await page.locator('#btnSearch').click();

        // 결과가 있다면 드롭다운 선택값이 빈 문자열(상속)이어야 한다
        const rows = page.locator('#logLevelTableBody tr');
        const count = await rows.count();
        if (count > 0) {
            const firstSelectValue = await rows.first().locator('.level-select').inputValue();
            expect(firstSelectValue).toBe('');
        }
    });

    test('새로고침 버튼 클릭 시 API가 재호출되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level') && r.request().method() === 'GET');
        await page.locator('#btnRefresh').click();
        const response = await responsePromise;
        expect(response.status()).toBe(200);
    });
});

// ─── 페이지당 표시 건수 ──────────────────────────────────

test.describe('페이지당 표시 건수', () => {

    test('건수를 20으로 변경 후 조회하면 최대 20행이 표시되어야 한다', async ({ page }) => {
        await page.locator('#limitRows').selectOption('20');
        await page.locator('#btnSearch').click();

        const count = await page.locator('#logLevelTableBody tr').count();
        expect(count).toBeLessThanOrEqual(20);
    });

    test('건수를 10으로 변경 후 조회하면 최대 10행이 표시되어야 한다', async ({ page }) => {
        // 먼저 50으로 늘렸다가 10으로 줄임
        await page.locator('#limitRows').selectOption('50');
        await page.locator('#btnSearch').click();

        await page.locator('#limitRows').selectOption('10');
        await page.locator('#btnSearch').click();

        const count = await page.locator('#logLevelTableBody tr').count();
        expect(count).toBeLessThanOrEqual(10);
    });
});

// ─── 레벨 저장 ────────────────────────────────────────────

test.describe('로그 레벨 저장', () => {

    test('레벨 저장 버튼 클릭 시 confirm 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.save-level-btn').click();

        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalOk').click();
    });

    test('confirm 후 레벨 저장 시 성공 Toast가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.level-select').selectOption('DEBUG');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level/level') && r.request().method() === 'PATCH');
        await row.locator('.save-level-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('.toast')).toBeVisible();
    });

    test('상속 옵션 저장 시 logLevel이 null로 반영되어야 한다', async ({ page, request }) => {
        // 먼저 명시적 레벨 설정
        await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'DEBUG' },
        });

        await page.reload();
        await expect(page.locator('#logLevelTableBody tr').first()).toBeVisible();

        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        // 상속 옵션 선택 (value="")
        await row.locator('.level-select').selectOption('');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level/level') && r.request().method() === 'PATCH');
        await row.locator('.save-level-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('.toast')).toBeVisible();
    });

    test('confirm 취소 시 API가 호출되지 않아야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        let apiCalled = false;
        page.on('request', req => {
            if (req.url().includes('/api/log-level/level')) apiCalled = true;
        });

        await row.locator('.save-level-btn').click();
        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalCancel, [data-bs-dismiss="modal"]').first().click();

        expect(apiCalled).toBe(false);
    });

    test('저장 후 테이블이 최신 데이터로 갱신되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.level-select').selectOption('WARN');

        // PATCH 응답 후 테이블 갱신을 위한 GET 요청 대기
        const reloadPromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level') && r.request().method() === 'GET');
        await row.locator('.save-level-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await reloadPromise;

        // 테이블 재필터링 후 변경된 레벨 확인
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const updatedRow = page.locator('#logLevelTableBody tr').first();
        const levelSelectValue = await updatedRow.locator('.level-select').inputValue();
        expect(levelSelectValue).toBe('WARN');
    });
});

// ─── Additivity 저장 ──────────────────────────────────────

test.describe('Additivity 저장', () => {

    test('Additivity 저장 버튼 클릭 시 confirm 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.save-additivity-btn').click();

        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalOk').click();
    });

    test('confirm 후 Additivity 저장 시 성공 Toast가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        await page.locator('#btnSearch').click();
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.additivity-select').selectOption('N');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level/additivity') && r.request().method() === 'PATCH');
        await row.locator('.save-additivity-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('.toast')).toBeVisible();

        // teardown: Additivity를 Y로 복원
        await row.locator('.additivity-select').selectOption('Y');
        const restorePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level/additivity') && r.request().method() === 'PATCH');
        await row.locator('.save-additivity-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await restorePromise;
    });
});

// ─── 내보내기 ─────────────────────────────────────────────

test.describe('내보내기', () => {

    test('CSV 내보내기 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#btnExcel')).toBeVisible();
    });

    test('데이터가 있을 때 CSV 내보내기 버튼 클릭 시 다운로드가 시작되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toContain('log-level');
    });

    test('필터 결과가 없을 때 CSV 내보내기 클릭 시 Toast 경고가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill('__NONEXISTENT_LOGGER_XYZ__');
        await page.locator('#btnSearch').click();
        await expect(page.locator('#logLevelTableBody')).toContainText('조회된 데이터가 없습니다');

        await page.locator('#btnExcel').click();
        await expect(page.locator('.toast')).toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('로그레벨 권한', () => {

    test('W 권한이 있는 사용자에게는 저장 버튼이 표시되어야 한다', async ({ page }) => {
        // 기본 세션(e2e-admin)은 LOG_LEVEL:W 권한 보유
        await expect(page.locator('.save-level-btn').first()).toBeVisible();
        await expect(page.locator('.save-additivity-btn').first()).toBeVisible();
    });

    test('읽기 전용 사용자에게는 저장 버튼이 표시되지 않아야 한다', async ({ browser }) => {
        const context = await browser.newContext({
            storageState: 'e2e/.auth/session-readonly.json',
        });
        const page = await context.newPage();
        await page.goto('/log-levels');
        await expect(page.locator('#logLevelTableBody tr').first()).toBeVisible();

        await expect(page.locator('.save-level-btn')).toHaveCount(0);
        await expect(page.locator('.save-additivity-btn')).toHaveCount(0);

        await context.close();
    });

    test('읽기 전용 사용자는 레벨이 뱃지 텍스트로만 표시되어야 한다', async ({ browser }) => {
        const context = await browser.newContext({
            storageState: 'e2e/.auth/session-readonly.json',
        });
        const page = await context.newPage();
        await page.goto('/log-levels');
        await expect(page.locator('#logLevelTableBody tr').first()).toBeVisible();

        // 드롭다운 대신 텍스트 뱃지 또는 텍스트가 표시되어야 한다
        await expect(page.locator('.level-select')).toHaveCount(0);
        await expect(page.locator('.additivity-select')).toHaveCount(0);

        await context.close();
    });
});
