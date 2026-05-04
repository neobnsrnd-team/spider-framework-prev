/**
 * 연계기관 관리 페이지 — 목록, 인라인 CRUD, Gateway/Handler 모달, 권한.
 *
 * 이 페이지는 모달 CRUD가 아닌 인라인 편집 + batch 저장 패턴을 사용한다.
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 *
 * NOTE: 이 페이지는 click-to-edit 패턴을 사용한다.
 * 모든 editable-field는 초기에 readonly이며, 클릭해야 편집 가능해진다.
 */

import { test, expect, type Page, type Locator, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
function genOrgId() { return 'E2O' + Date.now().toString(36).slice(-4) + String(seq++).padStart(2, '0'); }

async function createOrg(
    request: APIRequestContext,
    orgId: string,
    orgName: string,
) {
    const res = await request.post('/api/orgs/batch', {
        data: {
            upserts: [{ orgId, orgName, orgDesc: 'E2E 테스트용', startTime: '0000', endTime: '2359' }],
            deleteOrgIds: [],
        },
    });
    expect(res.ok()).toBeTruthy();
}

async function deleteOrg(request: APIRequestContext, orgId: string) {
    await request.post('/api/orgs/batch', {
        data: { upserts: [], deleteOrgIds: [orgId] },
    });
}

/** 메인 페이지 검색 — 모달 내 조회 버튼과 구별하기 위해 onclick으로 특정 */
async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#orgSearchField').selectOption(field);
    await page.locator('#orgSearchKeyword').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/orgs/page'));
    await page.locator('button[onclick="InterfaceMnt.Orgs.search()"]').click();
    await responsePromise;
}

/** 메인 페이지 하단 액션 버튼 — 모달 내 동명 버튼과 구별 */
function mainActionBtn(page: Page, name: string) {
    return page.locator('.bottom-actions').getByRole('button', { name });
}

/** click-to-edit 필드에 값을 입력 (클릭 → 편집 가능 → fill) */
async function clickAndFill(field: Locator, value: string) {
    await field.click();
    await field.fill(value);
}

// ─── 테스트 ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/orgs/page'));
    await page.goto('/orgs');
    await responsePromise;
    await expect(page.locator('#orgTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('연계기관 목록', () => {

    test('초기 페이지 로드 시 데이터가 표시되어야 한다', async ({ page }) => {
        const rows = page.locator('#orgTableBody tr');
        await expect(rows.first()).toBeVisible();
    });

    test('기관ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'SearchOrg');

        try {
            await searchByField(page, 'orgId', orgId);
            await expect(page.locator('#orgTableBody').getByRole('textbox').first()).toHaveValue(orgId);
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('기관명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        const uniqueName = 'OrgSearch' + orgId;
        await createOrg(request, orgId, uniqueName);

        try {
            await searchByField(page, 'orgName', uniqueName);
            await expect(page.locator('#orgTableBody input.editable-field').nth(1)).toHaveValue(uniqueName);
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('검색 조건을 변경하면 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'PageResetTest');

        try {
            await searchByField(page, 'orgId', orgId);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page }) => {
        const sortHeader = page.locator('#orgTable thead th[data-sort="orgId"]');

        // 1) 첫 클릭 → 오름차순 (sort-asc)
        const res1 = page.waitForResponse(r => r.url().includes('/api/orgs/page'));
        await sortHeader.click();
        await res1;
        await expect(sortHeader).toHaveClass(/sort-asc/);

        // 2) 두 번째 클릭 → 내림차순 (sort-desc)
        const res2 = page.waitForResponse(r => r.url().includes('/api/orgs/page'));
        await sortHeader.click();
        await res2;
        await expect(sortHeader).toHaveClass(/sort-desc/);

        // 3) 세 번째 클릭 → 정렬 해제 (sort-asc, sort-desc 둘 다 없어야 함)
        const res3 = page.waitForResponse(r => r.url().includes('/api/orgs/page'));
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

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'PrintTest');

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            await page.waitForResponse(r => r.url().includes('/api/orgs/page'));
            await expect(page.locator('#orgTable')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.locator('.page-header-actions').getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('리스트 라벨과 엑셀/출력 버튼이 같은 줄에 표시되어야 한다', async ({ page }) => {
        // 메인 페이지의 "리스트" page-header는 .mt-3 클래스를 가짐 (모달 내 것과 구별)
        const header = page.locator('.page-header.mt-3').first();
        const title = header.locator('.page-title');
        const excelBtn = header.locator('.page-header-actions').getByRole('button', { name: LABEL.EXCEL });

        const titleBox = await title.boundingBox();
        const excelBox = await excelBtn.boundingBox();

        // 같은 줄: Y 좌표 차이가 작아야 한다
        expect(titleBox).toBeTruthy();
        expect(excelBox).toBeTruthy();
        expect(Math.abs(titleBox!.y - excelBox!.y)).toBeLessThan(30);
    });
});

// ─── 인라인 CRUD ─────────────────────────────────────────

test.describe('연계기관 CRUD', () => {

    test('행 추가 버튼을 클릭하면 새 행이 테이블에 추가되어야 한다', async ({ page }) => {
        const beforeCount = await page.locator('#orgTableBody tr').count();
        await mainActionBtn(page, LABEL.ORG_ADD_ROW).click();
        const afterCount = await page.locator('#orgTableBody tr').count();
        expect(afterCount).toBe(beforeCount + 1);

        // 새 행의 기관ID 필드가 클릭 후 편집 가능해야 한다
        const firstRow = page.locator('#orgTableBody tr').first();
        const orgIdField = firstRow.locator('input.editable-field').first();
        await orgIdField.click();
        await expect(orgIdField).not.toHaveAttribute('readonly');
    });

    test('데이터를 생성하면 저장 후 테이블에 즉시 반영되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();

        try {
            // 행 추가
            await mainActionBtn(page, LABEL.ORG_ADD_ROW).click();
            const newRow = page.locator('#orgTableBody tr').first();

            // 데이터 입력 (click-to-edit)
            const fields = newRow.locator('input.editable-field');
            await clickAndFill(fields.nth(0), orgId);
            await clickAndFill(fields.nth(1), '생성테스트기관');
            await clickAndFill(fields.nth(2), 'E2E 생성 테스트');
            await clickAndFill(fields.nth(3), '0900');
            await clickAndFill(fields.nth(4), '1800');

            // 저장
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/orgs/batch') && r.request().method() === 'POST');
            await mainActionBtn(page, LABEL.ORG_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            // 목록 새로고침 후 확인
            await searchByField(page, 'orgId', orgId);
            await expect(page.locator('#orgTableBody').getByRole('textbox').first()).toHaveValue(orgId);
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('중복된 기관ID를 생성할 경우 upsert로 처리되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'DupTarget');

        try {
            // 행 추가 후 동일 ID 입력
            await mainActionBtn(page, LABEL.ORG_ADD_ROW).click();
            const newRow = page.locator('#orgTableBody tr').first();
            const fields = newRow.locator('input.editable-field');
            await clickAndFill(fields.nth(0), orgId);
            await clickAndFill(fields.nth(1), '중복테스트');
            await clickAndFill(fields.nth(3), '0000');
            await clickAndFill(fields.nth(4), '2359');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/orgs/batch') && r.request().method() === 'POST');
            await mainActionBtn(page, LABEL.ORG_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            // batch API는 upsert이므로 중복 ID는 업데이트로 처리된다
            await searchByField(page, 'orgId', orgId);
            await expect(page.locator('#orgTableBody input.editable-field').nth(1)).toHaveValue('중복테스트');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('유효하지 않은 시간값을 입력할 경우 예외 알림이 표시되어야 한다', async ({ page }) => {
        await mainActionBtn(page, LABEL.ORG_ADD_ROW).click();
        const newRow = page.locator('#orgTableBody tr').first();
        const fields = newRow.locator('input.editable-field');
        await clickAndFill(fields.nth(0), 'INVALID01');
        await clickAndFill(fields.nth(1), '유효성테스트');
        await clickAndFill(fields.nth(3), '9999'); // 잘못된 시간

        await mainActionBtn(page, LABEL.ORG_SAVE_CHANGES).click();
        await expect(page.locator('.toast').first()).toBeVisible();
        await expect(page.locator('.toast').first().locator('.toast-body span')).toContainText('시간');
    });

    test('기존 행의 기관ID(PK)는 수정할 수 없어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'PKReadonly');

        try {
            await searchByField(page, 'orgId', orgId);
            const row = page.locator('#orgTableBody tr').first();
            // 기존 행의 첫 번째 input(기관ID)은 클릭해도 readonly 유지
            const orgIdField = row.locator('input.editable-field').first();
            await expect(orgIdField).toHaveAttribute('data-pk');
            await orgIdField.click();
            await expect(orgIdField).toHaveAttribute('readonly');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('데이터를 수정하면 저장 후 테이블에 즉시 반영되어야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'BeforeName');

        try {
            await searchByField(page, 'orgId', orgId);
            const row = page.locator('#orgTableBody tr').first();

            // 기관명 수정 (click-to-edit)
            await clickAndFill(row.locator('input.editable-field').nth(1), 'AfterName');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/orgs/batch') && r.request().method() === 'POST');
            await mainActionBtn(page, LABEL.ORG_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            // 새로고침 후 확인
            await searchByField(page, 'orgId', orgId);
            await expect(page.locator('#orgTableBody input.editable-field').nth(1)).toHaveValue('AfterName');
        } finally {
            await deleteOrg(request, orgId);
        }
    });

    test('데이터를 삭제하면 저장 후 테이블에서 사라져야 한다', async ({ page, request }) => {
        const orgId = genOrgId();
        await createOrg(request, orgId, 'DeleteMe');

        await searchByField(page, 'orgId', orgId);

        // 체크박스 선택
        const row = page.locator('#orgTableBody tr').first();
        await row.locator('input.row-check').check();

        // 선택행 삭제
        await mainActionBtn(page, LABEL.ORG_DELETE_SELECTED).click();
        await page.locator('#spConfirmModalOk').click();

        // 변경사항 저장
        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/orgs/batch') && r.request().method() === 'POST');
        await mainActionBtn(page, LABEL.ORG_SAVE_CHANGES).click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        // 검색 후 결과 없어야 함
        await searchByField(page, 'orgId', orgId);
        await expect(page.locator('#orgTableBody')).toContainText('조회된 데이터가 없습니다');
    });
});

// ─── Gateway 모달 ────────────────────────────────────────

test.describe('Gateway 모달', () => {

    test('GATEWAY 버튼 클릭 시 모달이 열리고 데이터가 로드되어야 한다', async ({ page }) => {
        // 시드 데이터(E2EORG01)가 있는 행에서 GATEWAY 버튼 클릭
        await searchByField(page, 'orgId', 'E2EORG01');
        const row = page.locator('#orgTableBody tr').first();
        await row.locator('.gateway-btn').click();

        // 모달이 열려야 한다
        await expect(page.locator('#gatewayModal')).toBeVisible();
        // alert 없이 데이터가 로드되어야 한다 (Bug #3 검증)
        await expect(page.locator('#gatewayTableBody')).toBeVisible();

        // 모달 닫기
        await page.locator('#gatewayModal [data-bs-dismiss="modal"]').first().click();
    });
});

// ─── 핸들러 모달 ─────────────────────────────────────────

test.describe('핸들러 모달', () => {

    test('핸들러 버튼 클릭 시 모달이 열리고 데이터가 로드되어야 한다', async ({ page }) => {
        await searchByField(page, 'orgId', 'E2EORG01');
        const row = page.locator('#orgTableBody tr').first();
        await row.locator('.handler-btn').click();

        // 모달이 열려야 한다
        await expect(page.locator('#handlerModal')).toBeVisible();
        // alert 없이 데이터가 로드되어야 한다 (Bug #4 검증)
        await expect(page.locator('#handlerTableBody')).toBeVisible();

        // 모달 닫기
        await page.locator('#handlerModal [data-bs-dismiss="modal"]').first().click();
    });

    test('핸들러 모달 테이블이 모달 내에서 잘리지 않아야 한다', async ({ page }) => {
        await searchByField(page, 'orgId', 'E2EORG01');
        const row = page.locator('#orgTableBody tr').first();
        await row.locator('.handler-btn').click();
        await expect(page.locator('#handlerModal')).toBeVisible();

        // 모달 body 내의 테이블이 모달 너비를 초과하지 않아야 한다 (Bug #5 검증)
        const modalBody = page.locator('#handlerModal .modal-body');
        const modalBox = await modalBody.boundingBox();
        const tableContainer = page.locator('#handlerModal .table-responsive');
        const tableBox = await tableContainer.boundingBox();

        if (modalBox && tableBox) {
            // 테이블이 모달 body 너비를 크게 초과하지 않아야 한다
            expect(tableBox.width).toBeLessThanOrEqual(modalBox.width + 30);
        }

        await page.locator('#handlerModal [data-bs-dismiss="modal"]').first().click();
    });

    test('거래유형 API가 에러 없이 호출되어야 한다', async ({ page }) => {
        // Bug #6 검증: trxType API 호출이 성공하는지 확인
        // (실제 옵션 개수는 시드 데이터에 의존하므로 API 성공 여부만 검증)
        const trxTypeApiPromise = page.waitForResponse(
            r => r.url().includes('/transports/options/trx-types'),
            { timeout: 10000 },
        );
        await searchByField(page, 'orgId', 'E2EORG01');
        const row = page.locator('#orgTableBody tr').first();
        await row.locator('.handler-btn').click();
        await expect(page.locator('#handlerModal')).toBeVisible();

        const trxTypeRes = await trxTypeApiPromise;
        expect(trxTypeRes.ok()).toBeTruthy();

        // 거래유형 셀렉트가 존재해야 한다
        await expect(page.locator('#hdTrxType')).toBeVisible();

        await page.locator('#handlerModal [data-bs-dismiss="modal"]').first().click();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 행 추가/삭제/저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/orgs');
        await page.waitForResponse(r => r.url().includes('/api/orgs/page'));
        await expect(page.locator('#orgTable')).toBeVisible();

        // ORG:W 권한이 없으면 .bottom-actions 자체가 렌더링되지 않아야 한다
        await expect(page.locator('.bottom-actions')).not.toBeVisible();
    });
});
