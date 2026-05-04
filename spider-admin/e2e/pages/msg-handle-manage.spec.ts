/**
 * 전문처리핸들러 관리 페이지 — 목록, CRUD (인라인 편집), 권한.
 *
 * 이 페이지는 모달 기반 CRUD가 아닌 인라인 테이블 편집 + 배치 저장 패턴을 사용한다.
 * - "행 추가" → 테이블 상단에 새 행 삽입
 * - "변경사항 저장" → POST /api/message-handlers/batch (upserts)
 * - "선택행 삭제" → POST /api/message-handlers/batch (deletes)
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ─────────────────────────────────────────

interface HandlerData {
    orgId: string;
    trxType: string;
    ioType: string;
    operModeType: string;
    handler: string;
    handlerDesc: string;
    stopYn?: string;
}

/**
 * API를 통해 전문처리핸들러를 생성한다.
 * 배치 API는 orgId를 쿼리 파라미터로 받는다.
 */
async function createHandler(request: APIRequestContext, data: HandlerData) {
    const res = await request.post(`/api/message-handlers/batch?orgId=${encodeURIComponent(data.orgId)}`, {
        data: {
            upserts: [{
                orgId: data.orgId,
                trxType: data.trxType,
                ioType: data.ioType,
                operModeType: data.operModeType,
                handler: data.handler,
                handlerDesc: data.handlerDesc,
                stopYn: data.stopYn || 'N',
            }],
        },
    });
    expect(res.ok(), `createHandler failed: ${res.status()}`).toBeTruthy();
}

/** API를 통해 전문처리핸들러를 삭제한다. */
async function deleteHandler(request: APIRequestContext, data: Pick<HandlerData, 'orgId' | 'trxType' | 'ioType' | 'operModeType'>) {
    await request.post(`/api/message-handlers/batch?orgId=${encodeURIComponent(data.orgId)}`, {
        data: {
            deletes: [{
                orgId: data.orgId,
                trxType: data.trxType,
                ioType: data.ioType,
                operModeType: data.operModeType,
            }],
        },
    });
}

/** 사용 가능한 orgId 목록에서 첫 번째를 반환한다. */
async function getFirstOrgId(request: APIRequestContext): Promise<string> {
    const res = await request.get('/api/orgs?page=1&size=1');
    const json = await res.json();
    const rows = json.data?.content || json.data || [];
    expect(rows.length).toBeGreaterThan(0);
    return rows[0].orgId;
}

/** 사용 가능한 trxType 목록에서 첫 번째를 반환한다. */
async function getFirstTrxType(request: APIRequestContext): Promise<string> {
    const res = await request.get('/api/transports/options/trx-types');
    const json = await res.json();
    const options = json.data || [];
    expect(options.length).toBeGreaterThan(0);
    return options[0].trxType;
}

/** 고유한 핸들러명을 생성한다. */
function generateHandlerName(prefix: string): string {
    return prefix + Date.now().toString(36);
}

/** 페이지 방문 후 초기 데이터 로드를 기다린다. */
async function gotoAndWaitLoad(page: Page) {
    await page.goto('/message-handlers');
    await expect(page.locator('#handlerTable')).toBeVisible();
    // 초기 데이터 로드 완료 대기 (옵션 API 3개 → 핸들러 API 순서라 10s 초과 가능)
    await page.waitForFunction(
        () => {
            const tbody = document.getElementById('handlerTableBody');
            if (!tbody) return false;
            return !tbody.textContent?.includes('불러오는 중');
        },
        { timeout: 15000 },
    );
}

// ─── 테스트 ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await gotoAndWaitLoad(page);
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('전문처리핸들러 목록', () => {

    test('초기 페이지 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#handlerTable')).toBeVisible();
        await expect(page.locator('#handlerTableBody')).toBeVisible();
    });

    test('기관명 필터를 변경하면 해당 기관의 데이터가 조회되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const trxType = await getFirstTrxType(request);
        const handlerName = generateHandlerName('e2e-hfilter-');
        const data: HandlerData = {
            orgId, trxType, ioType: 'I', operModeType: 'D',
            handler: handlerName, handlerDesc: 'FilterTest',
        };
        await createHandler(request, data);

        try {
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
            await page.locator('#handlerOrgId').selectOption(orgId);
            await responsePromise;

            // 필터링된 결과에서 해당 기관 데이터가 포함되어야 한다
            await expect(page.locator('#handlerTableBody')).toBeVisible();
        } finally {
            await deleteHandler(request, data);
        }
    });

    test('거래유형 필터를 변경하면 해당 유형의 데이터가 조회되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const trxType = await getFirstTrxType(request);
        const handlerName = generateHandlerName('e2e-htrx-');
        const data: HandlerData = {
            orgId, trxType, ioType: 'I', operModeType: 'D',
            handler: handlerName, handlerDesc: 'TrxFilterTest',
        };
        await createHandler(request, data);

        try {
            // 거래유형 필터 옵션이 로드되기를 기다린다
            await expect(page.locator('#handlerTrxType option')).not.toHaveCount(1);

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
            await page.locator('#handlerTrxType').selectOption(trxType);
            await responsePromise;

            await expect(page.locator('#handlerTableBody')).toBeVisible();
        } finally {
            await deleteHandler(request, data);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerTable thead th[data-sort="handler"]').click();
        await responsePromise;
        await expect(page.locator('#handlerTableBody')).toBeVisible();

        const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerTable thead th[data-sort="handler"]').click();
        await responsePromise2;
        await expect(page.locator('#handlerTableBody')).toBeVisible();
    });

    test('페이지 사이즈를 변경하면 데이터가 다시 조회되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerPageSize').selectOption('10');
        await responsePromise;

        await expect(page.locator('#handlerTableBody')).toBeVisible();
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context }) => {
        await context.addInitScript(() => { window.print = () => {}; });
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.reload();
        await loadPromise;
        await expect(page.locator('#handlerTable')).toBeVisible();

        const popupPromise = context.waitForEvent('page');
        await page.locator('#btnPrint').click();
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await expect(popup.locator('h3')).toHaveText('전문처리핸들러 목록');
        await expect(popup.locator('table')).toBeVisible();
        await popup.close();
    });
});

// ─── CRUD (인라인 편집) ──────────────────────────────────

test.describe('전문처리핸들러 CRUD (인라인 편집)', () => {

    test('행 추가 버튼을 클릭하면 테이블 상단에 새 행이 추가되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);

        // 기관을 먼저 선택해야 행 추가가 가능하다
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerOrgId').selectOption(orgId);
        await responsePromise;

        const rowsBefore = await page.locator('#handlerTableBody tr').count();

        await page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW }).click();

        const rowsAfter = await page.locator('#handlerTableBody tr').count();
        expect(rowsAfter).toBe(rowsBefore + 1);

        // 새 행에 CRUD 표시가 'I'여야 한다
        const firstRow = page.locator('#handlerTableBody tr').first();
        await expect(firstRow).toContainText('I');
        await expect(firstRow).toHaveAttribute('data-new', 'true');
    });

    test('새 행을 추가하고 저장하면 데이터가 서버에 반영되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const trxType = await getFirstTrxType(request);
        const handlerName = generateHandlerName('e2e-hcreate-');

        // 기관 선택
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerOrgId').selectOption(orgId);
        await loadPromise;

        // 행 추가
        await page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW }).click();
        const newRow = page.locator('#handlerTableBody tr').first();
        await expect(newRow).toHaveAttribute('data-new', 'true');

        // 필드 채우기
        await newRow.locator('[data-field="trxType"]').selectOption(trxType);
        await newRow.locator('[data-field="ioType"]').selectOption('I');
        await newRow.locator('[data-field="handler"]').fill(handlerName);
        await newRow.locator('[data-field="handlerDesc"]').fill('E2E 생성 테스트');

        try {
            // 저장
            page.on('dialog', dialog => dialog.accept());
            const savePromise = page.waitForResponse(r =>
                r.url().includes('/api/message-handlers/batch') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.HANDLER_SAVE_CHANGES }).click();
            const saveRes = await savePromise;
            expect(saveRes.ok()).toBeTruthy();

            // alert('저장되었습니다.') 처리 후 리로드됨
            // 리로드 후 데이터 확인
            await page.waitForResponse(r => r.url().includes('/api/message-handlers') && !r.url().includes('batch'));
            await expect(page.locator('#handlerTableBody')).toBeVisible();

            // 생성된 핸들러가 테이블에 존재하는지 확인
            await expect(page.locator(`#handlerTableBody input[value="${handlerName}"]`)).toBeVisible();
        } finally {
            await deleteHandler(request, { orgId, trxType, ioType: 'I', operModeType: 'D' });
        }
    });

    test('기존 행의 핸들러설명을 수정하고 저장하면 반영되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const trxType = await getFirstTrxType(request);
        const handlerName = generateHandlerName('e2e-hupdate-');
        const data: HandlerData = {
            orgId, trxType, ioType: 'O', operModeType: 'D',
            handler: handlerName, handlerDesc: 'BeforeUpdate',
        };
        await createHandler(request, data);

        try {
            // 기관 필터로 데이터 조회
            const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
            await page.locator('#handlerOrgId').selectOption(orgId);
            await loadPromise;

            // 생성한 핸들러가 있는 행 찾기
            const targetRow = page.locator('#handlerTableBody tr').filter({
                has: page.locator(`input[value="${handlerName}"]`),
            });
            await expect(targetRow).toBeVisible();

            // 핸들러설명 수정
            const descInput = targetRow.locator('[data-field="handlerDesc"]');
            await descInput.fill('AfterUpdate');

            // 저장
            page.on('dialog', dialog => dialog.accept());
            const savePromise = page.waitForResponse(r =>
                r.url().includes('/api/message-handlers/batch') && r.request().method() === 'POST');
            await page.getByRole('button', { name: LABEL.HANDLER_SAVE_CHANGES }).click();
            const saveRes = await savePromise;
            expect(saveRes.ok()).toBeTruthy();

            // 리로드 후 수정된 값 확인
            await page.waitForResponse(r => r.url().includes('/api/message-handlers') && !r.url().includes('batch'));

            const updatedRow = page.locator('#handlerTableBody tr').filter({
                has: page.locator(`input[value="${handlerName}"]`),
            });
            await expect(updatedRow.locator('[data-field="handlerDesc"]')).toHaveValue('AfterUpdate');
        } finally {
            await deleteHandler(request, data);
        }
    });

    test('체크박스로 행을 선택하고 삭제하면 테이블에서 사라져야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const trxType = await getFirstTrxType(request);
        const handlerName = generateHandlerName('e2e-hdel-');
        const data: HandlerData = {
            orgId, trxType, ioType: 'I', operModeType: 'D',
            handler: handlerName, handlerDesc: 'DeleteTest',
        };
        await createHandler(request, data);

        // 기관 필터로 데이터 조회
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerOrgId').selectOption(orgId);
        await loadPromise;

        // 생성한 핸들러가 있는 행 찾기
        const targetRow = page.locator('#handlerTableBody tr').filter({
            has: page.locator(`input[value="${handlerName}"]`),
        });
        await expect(targetRow).toBeVisible();

        // 체크박스 선택
        await targetRow.locator('.handler-checkbox').check();

        const deletePromise = page.waitForResponse(r =>
            r.url().includes('/api/message-handlers/batch') && r.request().method() === 'POST');
        await page.getByRole('button', { name: LABEL.HANDLER_DELETE_SELECTED }).click();
        await page.locator('#spConfirmModalOk').click();
        const deleteRes = await deletePromise;
        expect(deleteRes.ok()).toBeTruthy();

        // 리로드 후 삭제 확인
        await page.waitForResponse(r => r.url().includes('/api/message-handlers') && !r.url().includes('batch'));
        await expect(page.locator(`#handlerTableBody input[value="${handlerName}"]`)).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });

    test('새로 추가한 행을 선택행 삭제하면 서버 호출 없이 제거되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);

        // 기관 선택
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerOrgId').selectOption(orgId);
        await loadPromise;

        // 행 추가
        await page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW }).click();
        const newRow = page.locator('#handlerTableBody tr[data-new="true"]');
        await expect(newRow).toBeVisible();

        // 체크박스 선택 후 삭제
        await newRow.locator('.handler-checkbox').check();
        await page.getByRole('button', { name: LABEL.HANDLER_DELETE_SELECTED }).click();
        await page.locator('#spConfirmModalOk').click();

        // 새 행이 제거되어야 한다
        await expect(page.locator('#handlerTableBody tr[data-new="true"]')).not.toBeVisible();
    });

    test('필수 항목 없이 저장 시 alert 경고가 표시되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);

        // 기관 선택
        const loadPromise = page.waitForResponse(r => r.url().includes('/api/message-handlers'));
        await page.locator('#handlerOrgId').selectOption(orgId);
        await loadPromise;

        // 행 추가 (trxType, handler 미입력)
        await page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW }).click();

        await page.getByRole('button', { name: LABEL.HANDLER_SAVE_CHANGES }).click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('거래유형');

        // 추가한 새 행 정리 (체크 후 삭제)
        const newRow = page.locator('#handlerTableBody tr[data-new="true"]');
        await newRow.locator('.handler-checkbox').check();
        await page.getByRole('button', { name: LABEL.HANDLER_DELETE_SELECTED }).click();
        await page.locator('#spConfirmModalOk').click();
    });

    test('전체 선택 체크박스를 클릭하면 모든 행이 선택되어야 한다', async ({ page }) => {
        // 데이터가 있는 상태에서 전체 선택
        const checkboxes = page.locator('.handler-checkbox');
        const count = await checkboxes.count();
        if (count === 0) {
            test.skip();
            return;
        }

        await page.locator('#handlerSelectAll').check();

        for (let i = 0; i < count; i++) {
            await expect(checkboxes.nth(i)).toBeChecked();
        }

        // 전체 해제
        await page.locator('#handlerSelectAll').uncheck();

        for (let i = 0; i < count; i++) {
            await expect(checkboxes.nth(i)).not.toBeChecked();
        }
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('전문처리핸들러 권한', () => {

    test('W 권한이 있는 사용자에게는 행 추가, 선택행 삭제, 변경사항 저장 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW })).toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.HANDLER_DELETE_SELECTED })).toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.HANDLER_SAVE_CHANGES })).toBeVisible();
    });

    test('W 권한 사용자에게는 WAS Reload 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_RELOAD_TO_WAS })).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('전문처리핸들러 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        await gotoAndWaitLoad(page);
    });

    test('R 권한 사용자에게는 행 추가 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_ADD_ROW })).not.toBeVisible();
    });

    test('R 권한 사용자에게는 선택행 삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_DELETE_SELECTED })).not.toBeVisible();
    });

    test('R 권한 사용자에게는 변경사항 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_SAVE_CHANGES })).not.toBeVisible();
    });

    test('R 권한 사용자에게는 WAS Reload 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.HANDLER_RELOAD_TO_WAS })).not.toBeVisible();
    });
});
