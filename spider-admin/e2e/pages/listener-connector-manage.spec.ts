/**
 * 리스너 응답커넥터 맵핑 관리 페이지 — 목록, 모달 CRUD, 권한.
 *
 * 이 페이지는 모달 기반 CRUD 패턴을 사용한다.
 * 복합 PK: listenerGwId + listenerSystemId + identifier
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const BASE = '/api/interface-mnt/listener-connector-mappings';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

async function createMapping(
    request: APIRequestContext,
    listenerGwId: string,
    listenerSystemId: string,
    identifier: string,
    connectorGwId: string,
    connectorSystemId: string,
    description = '',
) {
    const res = await request.post(BASE, {
        data: { listenerGwId, listenerSystemId, identifier, connectorGwId, connectorSystemId, description },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteMapping(
    request: APIRequestContext,
    listenerGwId: string,
    listenerSystemId: string,
    identifier: string,
) {
    await request.delete(`${BASE}/${listenerGwId}/${listenerSystemId}/${identifier}`);
}

/** 고유 식별자 생성 */
function uniqueId(prefix: string) {
    return `${prefix}-${Date.now()}`;
}

/** 검색 실행 — 조회 버튼 클릭 */
async function doSearch(page: Page) {
    const responsePromise = page.waitForResponse(r =>
        r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
    await page.locator('button[onclick="ListenerConnectorMappingList.load(1)"]').click();
    await responsePromise;
}

// ─── 테스트 ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r =>
        r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
    await page.goto('/listener-trxs');
    await responsePromise;
    await expect(page.locator('#listenerConnectorMappingTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('리스너 커넥터 맵핑 목록', () => {

    test('초기 페이지 로드 시 데이터는 10건이 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#listenerConnectorMappingTableBody tr');
        await expect(rows).toHaveCount(10);
    });

    test('리스너 GATEWAY 필터로 검색하면 해당 데이터만 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchListenerGateway').selectOption('E2E-LIS-GW');
        await doSearch(page);

        const rows = page.locator('#listenerConnectorMappingTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        for (let i = 0; i < count; i++) {
            await expect(rows.nth(i)).toContainText('E2E-LIS-GW');
        }
    });

    test('응답커넥터 GATEWAY 필터로 검색하면 해당 데이터만 표시되어야 한다', async ({ page }) => {
        await page.locator('#searchConnectorGateway').selectOption('E2E-CON-GW');
        await doSearch(page);

        const rows = page.locator('#listenerConnectorMappingTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        for (let i = 0; i < count; i++) {
            await expect(rows.nth(i)).toContainText('E2E-CON-GW');
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page }) => {
        const sortHeader = page.locator('#listenerConnectorMappingTable thead th[data-sort="identifier"]');

        // 1) 첫 클릭 → 오름차순
        const res1 = page.waitForResponse(r => r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
        await sortHeader.click();
        await res1;
        await expect(sortHeader).toHaveClass(/sort-asc/);

        // 2) 두 번째 클릭 → 내림차순
        const res2 = page.waitForResponse(r => r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
        await sortHeader.click();
        await res2;
        await expect(sortHeader).toHaveClass(/sort-desc/);

        // 3) 세 번째 클릭 → 정렬 해제
        const res3 = page.waitForResponse(r => r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
        await sortHeader.click();
        await res3;
        await expect(sortHeader).not.toHaveClass(/sort-asc/);
        await expect(sortHeader).not.toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('.page-header-actions').getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context }) => {
        const popupPromise = context.waitForEvent('page');
        await page.locator('.page-header-actions').getByRole('button', { name: LABEL.PRINT }).click();
        const popup = await popupPromise;

        expect(popup).toBeTruthy();

        if (!popup.isClosed()) {
            await popup.waitForLoadState('domcontentloaded');
            await popup.close();
        }
    });

    test('리스트 라벨과 엑셀/출력 버튼이 같은 줄에 표시되어야 한다', async ({ page }) => {
        const header = page.locator('.page-header.mt-3').first();
        const title = header.locator('.page-title');
        const excelBtn = header.locator('.page-header-actions').getByRole('button', { name: LABEL.EXCEL });

        const titleBox = await title.boundingBox();
        const excelBox = await excelBtn.boundingBox();

        expect(titleBox).toBeTruthy();
        expect(excelBox).toBeTruthy();
        expect(Math.abs(titleBox!.y - excelBox!.y)).toBeLessThan(30);
    });
});

// ─── 모달 CRUD ───────────────────────────────────────────

test.describe('리스너 커넥터 맵핑 CRUD', () => {

    test('등록 버튼을 클릭하면 등록 모달이 열려야 한다', async ({ page }) => {
        await page.locator('.bottom-actions').getByRole('button', { name: LABEL.LCM_REGISTER }).click();
        await expect(page.locator('#mappingModal')).toBeVisible();
        await expect(page.locator('#mappingModalTitle')).toHaveText(LABEL.LCM_MODAL_TITLE_CREATE);
    });

    test('행을 클릭하면 수정 모달이 열려야 한다', async ({ page }) => {
        const firstRow = page.locator('#listenerConnectorMappingTableBody tr').first();
        await firstRow.click();

        await expect(page.locator('#mappingModal')).toBeVisible();
        await expect(page.locator('#mappingModalTitle')).toHaveText(LABEL.LCM_MODAL_TITLE_EDIT);

        // PK 필드는 disabled 상태여야 한다
        await expect(page.locator('#modalListenerGwId')).toBeDisabled();
        await expect(page.locator('#modalListenerSystemId')).toBeDisabled();
        await expect(page.locator('#modalIdentifier')).toBeDisabled();
    });

    test('모달에서 새 맵핑을 생성하고 조회되어야 한다', async ({ page, request }) => {
        const identifier = uniqueId('E2E-NEW');

        try {
            // 등록 모달 열기
            await page.locator('.bottom-actions').getByRole('button', { name: LABEL.LCM_REGISTER }).click();
            await expect(page.locator('#mappingModal')).toBeVisible();

            // 필수 필드 입력
            await page.locator('#modalListenerGwId').selectOption('E2E-LIS-GW');
            // 시스템 로드 대기
            await page.waitForResponse(r => r.url().includes('/api/gateways/E2E-LIS-GW'));
            await page.locator('#modalListenerSystemId').selectOption('LIS-SYS-01');

            await page.locator('#modalConnectorGwId').selectOption('E2E-CON-GW');
            await page.waitForResponse(r => r.url().includes('/api/gateways/E2E-CON-GW'));
            await page.locator('#modalConnectorSystemId').selectOption('CON-SYS-01');

            await page.locator('#modalIdentifier').fill(identifier);
            await page.locator('#modalDescription').fill('모달 생성 테스트');

            // 저장
            page.on('dialog', dialog => dialog.accept());
            const saveRes = page.waitForResponse(r =>
                r.url().includes('/listener-connector-mappings') && r.request().method() === 'POST' && !r.url().includes('batch'));
            await page.locator('#mappingModal').getByRole('button', { name: '저장' }).click();
            await saveRes;

            // 모달 닫힘 확인
            await expect(page.locator('#mappingModal')).not.toBeVisible();

            // 리스너 GW 필터로 검색 후 확인 (시드 10건 + 신규 1건이므로 pageSize를 늘린다)
            await page.locator('#searchListenerGateway').selectOption('E2E-LIS-GW');
            await page.locator('#mappingPageSize').selectOption('20');
            await doSearch(page);
            await expect(page.locator('#listenerConnectorMappingTableBody')).toContainText(identifier);
        } finally {
            await deleteMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', identifier);
        }
    });

    test('모달에서 맵핑을 수정하고 변경사항이 반영되어야 한다', async ({ page, request }) => {
        const identifier = uniqueId('E2E-UPD');
        await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', identifier, 'E2E-CON-GW', 'CON-SYS-01', 'BEFORE');

        try {
            // 필터 + 페이지 크기 확대 + 검색
            await page.locator('#searchListenerGateway').selectOption('E2E-LIS-GW');
            await page.locator('#mappingPageSize').selectOption('100');
            await doSearch(page);

            // 해당 행 클릭 → 수정 모달 열기
            const row = page.locator('#listenerConnectorMappingTableBody tr').filter({ hasText: identifier });
            await row.click();
            await expect(page.locator('#mappingModal')).toBeVisible();
            await expect(page.locator('#mappingModalTitle')).toHaveText(LABEL.LCM_MODAL_TITLE_EDIT);

            // 설명 변경
            await page.locator('#modalDescription').fill('AFTER');

            // 커넥터 시스템 변경
            await page.locator('#modalConnectorSystemId').selectOption('CON-SYS-02');

            // 저장
            page.on('dialog', dialog => dialog.accept());
            const saveRes = page.waitForResponse(r =>
                r.url().includes('/listener-connector-mappings/') && r.request().method() === 'PUT');
            await page.locator('#mappingModal').getByRole('button', { name: '저장' }).click();
            await saveRes;

            // 모달 닫힘 확인
            await expect(page.locator('#mappingModal')).not.toBeVisible();

            // API로 변경사항 확인
            const check = await request.get(`${BASE}/E2E-LIS-GW/LIS-SYS-01/${identifier}`);
            const body = await check.json();
            expect(body.data.description).toBe('AFTER');
            expect(body.data.connectorSystemId).toBe('CON-SYS-02');
        } finally {
            await deleteMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', identifier);
        }
    });

    test('모달에서 필수값 미입력 시 저장되어서는 안 된다', async ({ page }) => {
        // 등록 모달 열기
        await page.locator('.bottom-actions').getByRole('button', { name: LABEL.LCM_REGISTER }).click();
        await expect(page.locator('#mappingModal')).toBeVisible();

        // 필수값 비움 상태에서 저장 클릭
        await page.locator('#mappingModal').getByRole('button', { name: '저장' }).click();

        // toast에 '필수' 키워드가 포함되어야 한다
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('필수');

        // 모달이 여전히 열려 있어야 한다
        await expect(page.locator('#mappingModal')).toBeVisible();
    });

    test('모달에서 맵핑을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const identifier = uniqueId('E2E-DEL');
        await createMapping(request, 'E2E-LIS-GW', 'LIS-SYS-01', identifier, 'E2E-CON-GW', 'CON-SYS-01', '삭제 테스트');

        // 필터 + 페이지 크기 확대 + 검색
        await page.locator('#searchListenerGateway').selectOption('E2E-LIS-GW');
        await page.locator('#mappingPageSize').selectOption('100');
        await doSearch(page);

        // 해당 행 클릭 → 수정 모달 열기
        const row = page.locator('#listenerConnectorMappingTableBody tr').filter({
            hasText: identifier,
        });
        await row.click();
        await expect(page.locator('#mappingModal')).toBeVisible();

        // 삭제 버튼 클릭
        const deleteRes = page.waitForResponse(r =>
            r.url().includes('/listener-connector-mappings') && r.request().method() === 'DELETE');
        await page.locator('#mappingModal').getByRole('button', { name: '삭제' }).click();
        await page.locator('#spConfirmModalOk').click();
        await deleteRes;

        // 모달 닫힘 + 목록에서 사라짐 확인
        await expect(page.locator('#mappingModal')).not.toBeVisible();
        await doSearch(page);
        await expect(page.locator('#listenerConnectorMappingTableBody')).not.toContainText(identifier);
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/listener-trxs');
        await page.waitForResponse(r =>
            r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
        await expect(page.locator('#listenerConnectorMappingTable')).toBeVisible();

        // LISTENER_CONNECTOR_MAPPING:W 권한이 없으면 .bottom-actions가 렌더링되지 않아야 한다
        await expect(page.locator('.bottom-actions')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 상세 모달에서 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/listener-trxs');
        await page.waitForResponse(r =>
            r.url().includes('/listener-connector-mappings') && !r.url().includes('export'));
        await expect(page.locator('#listenerConnectorMappingTable')).toBeVisible();

        // 행 클릭 → 상세 API 응답 대기 → 모달 열기
        const firstRow = page.locator('#listenerConnectorMappingTableBody tr').first();
        const detailRes = page.waitForResponse(r =>
            r.url().includes('/listener-connector-mappings/') && !r.url().includes('export') && !r.url().includes('?'));
        await firstRow.click();
        await detailRes;
        await expect(page.locator('#mappingModal')).toBeVisible();

        // 저장/삭제 버튼이 없어야 한다
        await expect(page.locator('#modalSaveBtn')).not.toBeVisible();
        await expect(page.locator('#modalDeleteBtn')).not.toBeVisible();

        // 모든 폼 필드가 disabled 상태여야 한다
        await expect(page.locator('#modalConnectorGwId')).toBeDisabled();
        await expect(page.locator('#modalConnectorSystemId')).toBeDisabled();
        await expect(page.locator('#modalDescription')).toBeDisabled();
    });
});
