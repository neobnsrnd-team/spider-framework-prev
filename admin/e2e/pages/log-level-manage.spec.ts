/**
 * 로그레벨조정 페이지 UI 테스트 — /log-levels
 *
 * 검증 범위:
 * - 페이지 로드 시 테이블 표시
 * - Log Name 필터 (클라이언트 사이드)
 * - 새로고침 버튼
 * - 레벨 저장 (confirm → 성공 Toast)
 * - Additivity 저장 (confirm → 성공 Toast)
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

        // 첫 번째 행에 Log Name 값이 존재해야 한다
        await expect(rows.first().locator('td').first()).not.toBeEmpty();
    });

    test('Log Name 필터 입력 시 일치하는 행만 표시되어야 한다', async ({ page }) => {
        const filter = page.locator('#logNameFilter');
        await filter.fill('ROOT');

        // 필터 입력은 클라이언트 사이드이므로 API 대기 없이 즉시 반영
        const rows = page.locator('#logLevelTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        // 표시된 모든 행의 Log Name에 'ROOT'가 포함되어야 한다
        for (let i = 0; i < count; i++) {
            const cellText = await rows.nth(i).locator('td').first().textContent();
            expect(cellText?.toLowerCase()).toContain('root');
        }
    });

    test('필터를 지우면 전체 목록이 다시 표시되어야 한다', async ({ page }) => {
        const filter = page.locator('#logNameFilter');
        await filter.fill('ROOT');
        const filteredCount = await page.locator('#logLevelTableBody tr').count();

        await filter.clear();
        const totalCount = await page.locator('#logLevelTableBody tr').count();
        expect(totalCount).toBeGreaterThanOrEqual(filteredCount);
    });

    test('필터에 일치하는 결과가 없으면 "조회된 데이터가 없습니다" 메시지가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill('__NONEXISTENT_LOGGER_XYZ__');
        await expect(page.locator('#logLevelTableBody')).toContainText('조회된 데이터가 없습니다');
    });

    test('새로고침 버튼 클릭 시 API가 재호출되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level') && r.request().method() === 'GET');
        await page.locator('#btnRefresh').click();
        const response = await responsePromise;
        expect(response.status()).toBe(200);
    });
});

// ─── 레벨 저장 ────────────────────────────────────────────

test.describe('로그 레벨 저장', () => {

    test('레벨 저장 버튼 클릭 시 confirm 모달이 표시되어야 한다', async ({ page }) => {
        // TEST_LOGGER 행을 필터로 찾기
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.save-level-btn').click();

        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalOk').click();
    });

    test('confirm 후 레벨 저장 시 성공 Toast가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        // 현재 레벨과 다른 레벨 선택
        await row.locator('.level-select').selectOption('DEBUG');

        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/log-level/level') && r.request().method() === 'PATCH');
        await row.locator('.save-level-btn').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await expect(page.locator('.toast')).toBeVisible();
    });

    test('confirm 취소 시 API가 호출되지 않아야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
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
});

// ─── Additivity 저장 ──────────────────────────────────────

test.describe('Additivity 저장', () => {

    test('Additivity 저장 버튼 클릭 시 confirm 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
        const row = page.locator('#logLevelTableBody tr').first();
        await expect(row).toBeVisible();

        await row.locator('.save-additivity-btn').click();

        await expect(page.locator('#spConfirmModal')).toBeVisible();
        await page.locator('#spConfirmModalOk').click();
    });

    test('confirm 후 Additivity 저장 시 성공 Toast가 표시되어야 한다', async ({ page }) => {
        await page.locator('#logNameFilter').fill(TEST_LOGGER);
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

// ─── 권한 ────────────────────────────────────────────────

test.describe('로그레벨 권한', () => {

    test('W 권한이 있는 사용자에게는 저장 버튼이 표시되어야 한다', async ({ page }) => {
        // 기본 세션(e2e-admin)은 LOG_LEVEL:W 권한 보유
        await expect(page.locator('.save-level-btn').first()).toBeVisible();
        await expect(page.locator('.save-additivity-btn').first()).toBeVisible();
    });

    test('읽기 전용 사용자에게는 저장 버튼이 표시되지 않아야 한다', async ({ browser }) => {
        const context = await browser.newContext({
            storageState: 'e2e/.auth/readonly-session.json',
        });
        const page = await context.newPage();
        await page.goto('/log-levels');
        await expect(page.locator('#logLevelTableBody tr').first()).toBeVisible();

        await expect(page.locator('.save-level-btn')).toHaveCount(0);
        await expect(page.locator('.save-additivity-btn')).toHaveCount(0);

        await context.close();
    });
});
