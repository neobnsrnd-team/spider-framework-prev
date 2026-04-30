/**
 * 거래중지 관리 페이지 — 목록 조회, 거래 중지/시작 토글, 운영모드 변경.
 *
 * 페이지 URL: /transactions/trx-stop
 * 목록 API: /api/trx-stop/page
 *
 * 모든 상태 변경 테스트는 독립 TRX를 생성하고 finally에서 복원/삭제한다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

async function createTrx(request: APIRequestContext, id: string) {
    const res = await request.post('/api/trx', {
        data: { trxId: id, trxType: '1', retryTrxYn: 'N', maxRetryCount: 0 },
    });
    expect(res.status()).toBe(201);
}

async function deleteTrx(request: APIRequestContext, id: string) {
    await request.delete(`/api/trx/${id}`);
}

async function restoreStop(request: APIRequestContext, id: string) {
    await request.put('/api/trx-stop/batch', {
        data: { trxIds: [id], trxStopYn: 'N' },
    });
}

async function searchByTrxId(page: Page, trxId: string) {
    await page.locator('#searchField').selectOption('trxId');
    await page.locator('#searchValue').fill(trxId);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/trx-stop/page'));
    await page.getByRole('button', { name: '조회' }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await Promise.all([
        page.waitForResponse(r => r.url().includes('/api/trx-stop/page')),
        page.goto('/transactions/trx-stop'),
    ]);
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('거래중지 목록', () => {

    test('초기 페이지 로드 시 테이블 컨테이너가 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#trxStopTable')).toBeVisible();
    });

    test('초기 로드 시 시드 데이터가 포함되어야 한다', async ({ page }) => {
        const rows = page.locator('#trxStopTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);
    });

    test('거래ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2ets-s-');
        await createTrx(request, id);

        try {
            await searchByTrxId(page, id);
            await expect(page.locator('#trxStopTableBody').getByText(id)).toBeVisible();
        } finally {
            await deleteTrx(request, id);
        }
    });

    test('존재하지 않는 ID로 검색하면 결과가 없어야 한다', async ({ page }) => {
        await searchByTrxId(page, 'ZZZNOMATCH-99999');

        const rows = page.locator('#trxStopTableBody tr');
        const count = await rows.count();
        if (count === 1) {
            await expect(rows.first()).not.toContainText('ZZZNOMATCH');
        } else {
            expect(count).toBe(0);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('조회 후 검색값을 수정해도 페이지 이동 시 기존 검색 조건으로 조회되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2ets-ap-');
        await createTrx(request, id);

        try {
            // 1. id로 조회 → appliedParams 에 id 저장됨
            await searchByTrxId(page, id);

            // 2. 검색 입력값을 변경 (조회 버튼 클릭 안 함)
            await page.locator('#searchValue').fill('MODIFIED_VALUE');

            // 3. limitRows 변경 → search()가 아닌 load() 호출
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/trx-stop/page'));
            await page.locator('#limitRows').selectOption('20');
            const response = await responsePromise;

            // 4. 요청 URL에 원래 id가 사용되어야 한다
            expect(response.url()).toContain(`searchValue=${id}`);
            expect(response.url()).not.toContain('MODIFIED_VALUE');
        } finally {
            await deleteTrx(request, id);
        }
    });

    test('조회 버튼을 클릭하면 첫 페이지부터 조회되어야 한다', async ({ page }) => {
        // 내부 상태를 page 2로 강제 설정
        await page.evaluate(() => { (window as any).TrxStopPage.currentPage = 2; });

        const responsePromise = page.waitForResponse(r => r.url().includes('/api/trx-stop/page'));
        await page.getByRole('button', { name: '조회' }).click();
        const response = await responsePromise;

        expect(response.url()).toContain('page=1');
    });
});

// ─── 거래 중지/시작 토글 ──────────────────────────────────

test.describe('거래 중지/시작 토글', () => {

    test.setTimeout(30_000);

    test('정상 거래에 중지를 요청하면 상태가 거래중지로 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2ets-t-');
        await createTrx(request, id);

        try {
            await searchByTrxId(page, id);

            const row = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await row.getByRole('button', { name: '중지' }).click();

            // 확인 모달 — "Yes" 클릭
            await page.locator('#trxStopConfirmModal').waitFor({ state: 'visible' });
            await page.locator('#trxStopConfirmModal').getByRole('button', { name: 'Yes' }).click();

            // 사유 입력 모달 — 사유 입력 후 "확인" 클릭
            await page.locator('#trxStopReasonModal').waitFor({ state: 'visible' });
            await page.locator('#trxStopReasonInput').fill('E2E 테스트 중지');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/trx-stop/batch') && r.request().method() === 'PUT'
            );
            await page.locator('#trxStopReasonModal').getByRole('button', { name: '확인' }).click();
            const res = await responsePromise;
            expect(res.status()).toBe(200);

            await searchByTrxId(page, id);

            const updatedRow = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await expect(updatedRow.getByRole('button', { name: '시작' })).toBeVisible();
        } finally {
            await restoreStop(request, id);
            await deleteTrx(request, id);
        }
    });

    test('중지 거래에 시작을 요청하면 상태가 거래중으로 복원되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2ets-r-');
        await createTrx(request, id);
        // 먼저 중지 상태로 설정
        await request.put('/api/trx-stop/batch', {
            data: { trxIds: [id], trxStopYn: 'Y', trxStopReason: 'E2E 초기 중지' },
        });
        // AuditUtil.now() 는 yyyyMMddHHmmss (초 단위) 이고 FWK_TRX_STOP_HISTORY PK 에 포함됨.
        // 위 API 호출과 아래 UI 액션이 같은 초에 실행되면 DuplicateKeyException(409) 발생.
        await page.waitForTimeout(1100);

        try {
            await searchByTrxId(page, id);

            const row = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await row.getByRole('button', { name: '시작' }).click();

            // 확인 모달 — "Yes" 클릭 (시작은 사유 입력 없음)
            await page.locator('#trxStopConfirmModal').waitFor({ state: 'visible' });

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/trx-stop/batch') && r.request().method() === 'PUT'
            );
            await page.locator('#trxStopConfirmModal').getByRole('button', { name: 'Yes' }).click();
            const res = await responsePromise;
            expect(res.status()).toBe(200);

            await searchByTrxId(page, id);

            const updatedRow = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await expect(updatedRow.getByRole('button', { name: '중지' })).toBeVisible();
        } finally {
            await restoreStop(request, id);
            await deleteTrx(request, id);
        }
    });

    test('확인 모달에서 No를 클릭하면 상태가 변경되어서는 안 된다', async ({ page, request }) => {
        const id = generateTestId('e2ets-n-');
        await createTrx(request, id);

        try {
            await searchByTrxId(page, id);

            const row = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await row.getByRole('button', { name: '중지' }).click();

            // 확인 모달 — "No" 클릭 (취소)
            await page.locator('#trxStopConfirmModal').waitFor({ state: 'visible' });
            await page.locator('#trxStopConfirmModal').getByRole('button', { name: 'No' }).click();
            await page.locator('#trxStopConfirmModal').waitFor({ state: 'hidden' });

            // 상태 변경 없음 — 아직 '중지' 버튼이 표시되어야 한다
            const updatedRow = page.locator('#trxStopTableBody tr').filter({ hasText: id });
            await expect(updatedRow.getByRole('button', { name: '중지' })).toBeVisible();
        } finally {
            await deleteTrx(request, id);
        }
    });
});

// ─── 운영모드 변경 ────────────────────────────────────────

test.describe('운영모드 변경', () => {

    test.setTimeout(30_000);

    test('운영모드변경 버튼 클릭 시 운영모드 변경 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: '운영모드변경' }).click();
        await expect(page.locator('#trxStopOperModeModal')).toBeVisible();

        // 닫기
        await page.locator('#trxStopOperModeModal').getByRole('button', { name: '닫기' }).click();
        await page.locator('#trxStopOperModeModal').waitFor({ state: 'hidden' });
    });

    test('운영모드 변경 모달에서 일괄수행을 클릭하면 API가 호출되어야 한다', async ({ page, request }) => {
        try {
            await page.getByRole('button', { name: '운영모드변경' }).click();
            await page.locator('#trxStopOperModeModal').waitFor({ state: 'visible' });

            // D(개발환경) 선택
            await page.locator('#trxStopOperModeModal select').selectOption('D');

            // '일괄수행' → TrxStopConfirmModal 열림 → Yes 클릭 → AJAX 실행
            await page.locator('#trxStopOperModeModal').getByRole('button', { name: '일괄수행' }).click();
            await page.locator('#trxStopConfirmModal').waitFor({ state: 'visible' });

            // AJAX 호출 및 성공 후 alert 핸들러를 Yes 클릭 전에 등록
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/trx-stop/batch-oper-mode') && r.request().method() === 'PUT'
            );
            page.once('dialog', dialog => dialog.accept());

            await page.locator('#trxStopConfirmModal').getByRole('button', { name: 'Yes' }).click();
            const res = await responsePromise;
            expect(res.status()).toBe(200);
        } finally {
            // 전체 복원 (운영환경 기본값으로) — 테스트 실패 시 request 컨텍스트 종료 방어
            try {
                await request.put('/api/trx-stop/batch-oper-mode', {
                    data: { operModeType: 'O' },
                });
            } catch {
                // cleanup is best-effort
            }
        }
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자도 거래중지 목록을 조회할 수 있어야 한다', async ({ page }) => {
        await expect(page.locator('#trxStopTable')).toBeVisible();
        const rows = page.locator('#trxStopTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);
    });

    test('R 권한 사용자도 엑셀을 다운로드할 수 있어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('R 권한 사용자가 거래 중지/시작을 요청하면 403이 반환되어야 한다', async ({ request }) => {
        const res = await request.put('/api/trx-stop/batch', {
            data: { trxIds: ['TEST_TRX'], trxStopYn: 'Y' },
        });
        expect(res.status()).toBe(403);
    });

    test('R 권한 사용자가 운영모드 일괄 변경을 요청하면 403이 반환되어야 한다', async ({ request }) => {
        const res = await request.put('/api/trx-stop/batch-oper-mode', {
            data: { operModeType: 'D' },
        });
        expect(res.status()).toBe(403);
    });
});
