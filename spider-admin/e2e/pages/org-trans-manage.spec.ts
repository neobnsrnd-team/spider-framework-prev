/**
 * 기관통신 Gateway 맵핑 관리 페이지 — 목록, 인라인 CRUD, 권한.
 *
 * 이 페이지는 인라인 편집 + batch 저장 패턴을 사용한다.
 * 복합 PK: orgId + trxType + ioType + reqResType
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

async function createTransport(
    request: APIRequestContext,
    orgId: string,
    trxType: string,
    ioType: string,
    reqResType: string,
    gwId: string,
) {
    const res = await request.post(`/api/transports/batch?orgId=${orgId}`, {
        data: {
            upserts: [{ orgId, trxType, ioType, reqResType, gwId }],
            deletes: [],
        },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteTransport(
    request: APIRequestContext,
    orgId: string,
    trxType: string,
    ioType: string,
    reqResType: string,
) {
    await request.post(`/api/transports/batch?orgId=${orgId}`, {
        data: {
            upserts: [],
            deletes: [{ orgId, trxType, ioType, reqResType }],
        },
    });
}

/** 검색 실행 — 조회 버튼 클릭 */
async function doSearch(page: Page) {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/transports') && !r.url().includes('export') && !r.url().includes('options'));
    await page.locator('button[onclick="GatewayMappingList.load(1)"]').click();
    await responsePromise;
}

/** 메인 페이지 하단 액션 버튼 — .bottom-actions 내 */
function mainActionBtn(page: Page, name: string) {
    return page.locator('.bottom-actions').getByRole('button', { name });
}

// ─── 테스트 ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r =>
        r.url().includes('/api/transports') && !r.url().includes('export') && !r.url().includes('options'));
    await page.goto('/gw-systems');
    await responsePromise;
    await expect(page.locator('#gatewayMappingTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('Gateway 맵핑 목록', () => {

    test('초기 페이지 로드 시 데이터는 10건이 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#gatewayMappingTableBody tr');
        await expect(rows).toHaveCount(10);
    });

    test('기관 필터로 검색하면 해당 기관 데이터만 표시되어야 한다', async ({ page }) => {
        await page.locator('#mappingOrgId').selectOption('E2EORG02');
        await doSearch(page);

        const rows = page.locator('#gatewayMappingTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        // 모든 행이 E2EORG02를 포함해야 한다
        for (let i = 0; i < count; i++) {
            await expect(rows.nth(i)).toContainText('E2EORG02');
        }
    });

    test('어댑터/리스너 필터로 검색하면 해당 유형만 표시되어야 한다', async ({ page }) => {
        await page.locator('#mappingIoType').selectOption('I');
        await doSearch(page);

        const rows = page.locator('#gatewayMappingTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);

        for (let i = 0; i < count; i++) {
            const select = rows.nth(i).locator('select[data-field="ioType"]');
            await expect(select).toHaveValue('I');
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page }) => {
        await page.locator('#mappingOrgId').selectOption('E2EORG01');
        await doSearch(page);
        await expect(page.locator('#pageInfo')).toContainText('1 -');
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page }) => {
        const sortHeader = page.locator('#gatewayMappingTable thead th[data-sort="orgId"]');

        // 1) 첫 클릭 → 오름차순
        const res1 = page.waitForResponse(r => r.url().includes('/api/transports') && !r.url().includes('export'));
        await sortHeader.click();
        await res1;
        await expect(sortHeader).toHaveClass(/sort-asc/);

        // 2) 두 번째 클릭 → 내림차순
        const res2 = page.waitForResponse(r => r.url().includes('/api/transports') && !r.url().includes('export'));
        await sortHeader.click();
        await res2;
        await expect(sortHeader).toHaveClass(/sort-desc/);

        // 3) 세 번째 클릭 → 정렬 해제
        const res3 = page.waitForResponse(r => r.url().includes('/api/transports') && !r.url().includes('export'));
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

        // 팝업이 열렸다는 것 자체가 출력 기능 동작 확인
        expect(popup).toBeTruthy();

        // 팝업이 이미 닫혔을 수 있으므로 안전하게 처리
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

// ─── 인라인 CRUD ────────────────────────────────────────

test.describe('Gateway 맵핑 CRUD', () => {

    test('행 추가 버튼을 클릭하면 새 행이 테이블에 추가되어야 한다', async ({ page }) => {
        const beforeCount = await page.locator('#gatewayMappingTableBody tr').count();
        await mainActionBtn(page, LABEL.GW_MAPPING_ADD_ROW).click();
        const afterCount = await page.locator('#gatewayMappingTableBody tr').count();
        expect(afterCount).toBe(beforeCount + 1);
    });

    test('인라인으로 데이터를 수정하고 저장하면 반영되어야 한다', async ({ page, request }) => {
        const orgId = 'E2EORG02';
        const trxType = '3';
        const ioType = 'O';
        const reqResType = 'S';
        const gwId = 'E2E-EDIT-GW';

        await createTransport(request, orgId, trxType, ioType, reqResType, gwId);

        try {
            // 기관 필터로 검색
            await page.locator('#mappingOrgId').selectOption(orgId);
            await doSearch(page);

            // gwId가 E2E-EDIT-GW인 행 찾기
            const row = page.locator(`#gatewayMappingTableBody tr`).filter({ has: page.locator(`input[value="${gwId}"]`) });
            await expect(row).toBeVisible();

            // gwId 수정
            const gwInput = row.locator('input[data-field="gwId"]');
            await gwInput.fill('E2E-EDITED');

            page.on('dialog', dialog => dialog.accept());

            const saveRes = page.waitForResponse(r =>
                r.url().includes('/api/transports/batch') && r.request().method() === 'POST');
            await mainActionBtn(page, LABEL.GW_MAPPING_SAVE).click();
            await saveRes;

            // 재조회 후 수정값 확인
            await doSearch(page);
            await expect(page.locator('#gatewayMappingTableBody').locator(`input[value="E2E-EDITED"]`)).toBeVisible();
        } finally {
            // 수정된 값으로 삭제 (gwId는 PK가 아니므로 동일 키로 삭제)
            await deleteTransport(request, orgId, trxType, ioType, reqResType);
        }
    });

    test('선택 삭제 후 저장하면 테이블에서 사라져야 한다', async ({ page, request }) => {
        const orgId = 'E2EORG02';
        const trxType = '4';
        const ioType = 'I';
        const reqResType = 'Q';

        await createTransport(request, orgId, trxType, ioType, reqResType, 'E2E-DEL-GW');

        // 기관 필터로 검색
        await page.locator('#mappingOrgId').selectOption(orgId);
        await doSearch(page);

        // 해당 행의 체크박스 선택
        const row = page.locator('#gatewayMappingTableBody tr').filter({
            has: page.locator('input[value="E2E-DEL-GW"]'),
        });
        await row.locator('input[type="checkbox"]').check();

        // 선택 삭제 — CRUD 컬럼에 "삭제" 표시되어야 한다
        await mainActionBtn(page, LABEL.GW_MAPPING_DELETE_SELECTED).click();
        await expect(row).toContainText(LABEL.CRUD_DELETE);
        await expect(row).toHaveClass(/sp-row-deleted/);

        page.on('dialog', dialog => dialog.accept());

        // 변경사항 저장
        const saveRes = page.waitForResponse(r =>
            r.url().includes('/api/transports/batch') && r.request().method() === 'POST');
        await mainActionBtn(page, LABEL.GW_MAPPING_SAVE).click();
        await saveRes;

        // 재조회 후 사라졌는지 확인
        await doSearch(page);
        await expect(page.locator('#gatewayMappingTableBody').locator('input[value="E2E-DEL-GW"]')).not.toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 행 추가/삭제/저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/gw-systems');
        await page.waitForResponse(r =>
            r.url().includes('/api/transports') && !r.url().includes('export') && !r.url().includes('options'));
        await expect(page.locator('#gatewayMappingTable')).toBeVisible();

        // GATEWAY_MAPPING:W 권한이 없으면 .bottom-actions 자체가 렌더링되지 않아야 한다
        await expect(page.locator('.bottom-actions')).not.toBeVisible();
    });
});
