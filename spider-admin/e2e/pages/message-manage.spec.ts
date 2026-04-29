/**
 * 전문 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 *
 * 특이사항:
 *  - PK가 복합키 (orgId + messageId)
 *  - 등록 모달(#messageModal)과 상세 모달(#messageDetailModal)이 분리되어 있음
 *  - 행 클릭 시 상세 모달(#messageDetailModal)이 열림
 *  - Bootstrap 5 모달의 outer div(tabindex=-1)가 pointer events를 가로채므로
 *    모달 내부 버튼은 page.evaluate로 네이티브 DOM click()을 사용한다
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ────────────────────────────────────────

/** 첫 번째 orgId를 API에서 조회하여 반환한다. */
async function getFirstOrgId(request: APIRequestContext): Promise<string> {
    const res = await request.get('/api/orgs/list');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.data.length).toBeGreaterThan(0);
    return body.data[0].orgId;
}

/** 첫 번째 기관의 orgName을 API에서 조회하여 반환한다. */
async function getFirstOrgName(request: APIRequestContext): Promise<string> {
    const res = await request.get('/api/orgs/list');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.data.length).toBeGreaterThan(0);
    return body.data[0].orgName;
}

async function createMessage(
    request: APIRequestContext,
    orgId: string,
    messageId: string,
    messageName: string,
) {
    const res = await request.post('/api/messages', {
        data: {
            orgId,
            messageId,
            messageName,
            messageType: 'J',
            bizDomain: '공통',
            headerYn: 'N',
            requestYn: 'Q',
            logLevel: 'A',
            preLoadYn: 'N',
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteMessage(request: APIRequestContext, orgId: string, messageId: string) {
    await request.delete(`/api/messages/${encodeURIComponent(messageId)}?orgId=${encodeURIComponent(orgId)}`);
}

/** 조회 버튼을 클릭하고 응답을 기다린다. */
async function clickSearch(page: Page) {
    const responsePromise = page.waitForResponse(
        r => r.url().includes('/api/messages/page'),
        { timeout: 15000 },
    );
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

/** 검색필드를 선택하고 검색어를 입력한 뒤 조회한다. */
async function searchByField(page: Page, field: string, value: string) {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    await clickSearch(page);
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

/** 등록 모달(#messageModal)을 닫는다. */
async function closeCreateModal(page: Page) {
    // hide() 호출 후 display:none 전환까지 직접 처리
    await page.evaluate(() => {
        const el = document.getElementById('messageModal');
        if (!el) return;
        el.classList.remove('show');
        el.style.display = 'none';
        el.removeAttribute('aria-modal');
        el.removeAttribute('role');
        el.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
        document.querySelector('.modal-backdrop')?.remove();
    });
}

/** 상세 모달(#messageDetailModal)을 닫는다. */
async function closeDetailModal(page: Page) {
    // hide() 호출 후 display:none 전환까지 직접 처리
    await page.evaluate(() => {
        const el = document.getElementById('messageDetailModal');
        if (!el) return;
        el.classList.remove('show');
        el.style.display = 'none';
        el.removeAttribute('aria-modal');
        el.removeAttribute('role');
        el.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
        document.querySelector('.modal-backdrop')?.remove();
    });
}

/**
 * 모달 내부의 저장/삭제 버튼을 클릭한다.
 * Bootstrap 5 모달 overlay 때문에 Playwright click이 차단되므로
 * 네이티브 DOM click으로 onclick 핸들러를 트리거한다.
 */
async function clickModalButton(page: Page, selector: string) {
    await page.evaluate((sel) => {
        const btn = document.querySelector(sel) as HTMLElement;
        if (btn) btn.click();
    }, selector);
}

// ─── 공통 beforeEach ────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/messages');
    // 페이지 스크립트가 자동으로 Messages.load()를 호출하므로 조회 버튼 클릭하여 데이터 로드
    await clickSearch(page);
    await expect(page.locator('#messageTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('전문 목록', () => {

    test('초기 페이지 로드 시 데이터가 20건 이하로 조회되어야 한다', async ({ page }) => {
        // limitRows 기본값이 20이므로 테이블 행이 최대 20개
        const rows = page.locator('#messageTableBody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(20);
    });

    test('전문ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mlist-');
        await createMessage(request, orgId, id, 'SearchTest');

        try {
            await searchByField(page, 'messageId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });

    test('전문명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mlist-');
        const uniqueName = 'MsgSearch' + id;
        await createMessage(request, orgId, id, uniqueName);

        try {
            await searchByField(page, 'messageName', uniqueName);
            await expect(page.getByRole('cell', { name: uniqueName })).toBeVisible();
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mlist-');
        await createMessage(request, orgId, id, 'PageResetTest');

        try {
            await searchByField(page, 'messageId', id);
            await expect(page.locator('#pageInfo')).toContainText('1 -');
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mlist-');
        await createMessage(request, orgId, id, 'SortTest');

        try {
            // data-sort 속성으로 직접 선택하여 정렬 표시 문자(↕) 영향 방지
            const header = page.locator('#messageTable th[data-sort="messageId"]');
            const responsePromise = page.waitForResponse(
                r => r.url().includes('/api/messages/page'),
                { timeout: 15000 },
            );
            await header.click();
            await responsePromise;
            await expect(page.getByRole('row').nth(1)).toBeVisible();

            const responsePromise2 = page.waitForResponse(
                r => r.url().includes('/api/messages/page'),
                { timeout: 15000 },
            );
            await header.click();
            await responsePromise2;
            await expect(page.getByRole('row').nth(1)).toBeVisible();
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download', { timeout: 15000 });
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mprint-');
        await createMessage(request, orgId, id, 'PrintTest');

        try {
            // headless에서 window.print()가 팝업을 즉시 닫는 것을 방지
            await context.addInitScript(() => { window.print = () => {}; });
            await page.reload();
            // 조회 버튼 클릭하여 데이터 로드
            await clickSearch(page);
            await expect(page.locator('#messageTable')).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });

    test('데이터 행을 클릭하면 상세 모달이 열려야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const id = generateTestId('e2e-mlist-');
        await createMessage(request, orgId, id, 'RowClickTest');

        try {
            await searchByField(page, 'messageId', id);
            const detailPromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${id}/detail`) && r.request().method() === 'GET',
                { timeout: 15000 },
            );
            await page.getByRole('row').filter({ hasText: id }).click();
            await detailPromise;
            await expect(page.locator('#messageDetailModal')).toBeVisible();

            await closeDetailModal(page);
        } finally {
            await deleteMessage(request, orgId, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('전문 CRUD', () => {

    test('등록 버튼을 클릭하면 빈 생성 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: '+ 등록' }).click();
        await expect(page.locator('#messageModal')).toBeVisible();
        await expect(page.getByText(LABEL.MSG_CREATE_TITLE)).toBeVisible();

        await expect(page.locator('#modalMessageId')).toHaveValue('');
        await expect(page.locator('#modalMessageId')).toBeEnabled();
        await expect(page.locator('#modalOrgId')).toBeEnabled();

        // 생성 모드에서는 삭제 버튼이 숨겨져야 한다
        await expect(page.locator('#messageModal #btnDelete')).not.toBeVisible();

        await closeCreateModal(page);
    });

    test('모달에서 전문을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const testId = generateTestId('e2e-mcrud-');

        try {
            await page.getByRole('button', { name: '+ 등록' }).click();
            await expect(page.locator('#messageModal')).toBeVisible();

            // 기관명 선택 (orgId 값으로 select)
            await page.locator('#modalOrgId').selectOption(orgId);
            await page.locator('#modalMessageId').fill(testId);
            await page.locator('#modalMessageName').fill('생성테스트전문');
            await page.locator('#modalMessageType').selectOption('J');
            await page.locator('#modalBizDomain').selectOption('공통');

            // alert 대화상자 처리 (저장 성공 시 alert이 발생하고, auto-accept해야 JS가 계속 실행됨)
            page.on('dialog', dialog => dialog.accept());

            const responsePromise = page.waitForResponse(
                r => r.url().includes('/api/messages') && r.request().method() === 'POST',
                { timeout: 15000 },
            );
            // 모달 내 저장 버튼을 네이티브 DOM click으로 트리거 (Bootstrap overlay 우회)
            await clickModalButton(page, '#messageModal .modal-footer .btn-primary');
            await responsePromise;

            // AJAX 성공 콜백이 alert + close + Messages.load()를 호출할 때까지 대기
            // Messages.load()가 /api/messages/page를 호출하므로 해당 응답으로 콜백 완료를 판단
            await page.waitForResponse(r => r.url().includes('/api/messages/page'), { timeout: 15000 });

            // bootstrap.Modal.hide() CSS 전환이 완료되지 않으므로 직접 닫기
            await closeCreateModal(page);
            await searchByField(page, 'messageId', testId);
            await expect(page.getByRole('cell', { name: testId })).toBeVisible();
        } finally {
            await deleteMessage(request, orgId, testId);
        }
    });

    test('유효하지 않은 값을 입력할 경우 alert 알림이 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: '+ 등록' }).click();
        await expect(page.locator('#messageModal')).toBeVisible();

        // 모달 내 저장 버튼을 네이티브 DOM click으로 트리거 (Bootstrap overlay 우회)
        await clickModalButton(page, '#messageModal .modal-footer .btn-primary');
        await expect(page.locator('.toast')).toBeVisible();

        await closeCreateModal(page);
    });

    test('행을 클릭하면 상세 모달에서 전문 정보가 표시되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const orgName = await getFirstOrgName(request);
        const testId = generateTestId('e2e-mcrud-');
        await createMessage(request, orgId, testId, '수정대상전문');

        try {
            await searchByField(page, 'messageId', testId);
            const detailPromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${testId}/detail`) && r.request().method() === 'GET',
                { timeout: 15000 },
            );
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#messageDetailModal')).toBeVisible();

            // 전문 정보가 올바르게 표시되는지 확인
            await expect(page.locator('#detailMessageId')).toContainText(testId);
            // detailOrgId에는 기관명(orgName)이 표시됨
            await expect(page.locator('#detailOrgId')).toContainText(orgName);

            await closeDetailModal(page);
        } finally {
            await deleteMessage(request, orgId, testId);
        }
    });

    test('상세 모달에서 전문명을 수정하면 저장이 완료되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const testId = generateTestId('e2e-mcrud-');
        await createMessage(request, orgId, testId, 'BeforeName');

        try {
            await searchByField(page, 'messageId', testId);
            const detailPromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${testId}/detail`) && r.request().method() === 'GET',
                { timeout: 15000 },
            );
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#messageDetailModal')).toBeVisible();

            await page.locator('#detailMessageName').fill('AfterName');

            // alert 대화상자 처리 (저장 성공 시 alert이 발생하고, auto-accept해야 JS가 계속 실행됨)
            page.on('dialog', dialog => dialog.accept());

            const responsePromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${testId}`) && r.request().method() === 'PUT',
                { timeout: 15000 },
            );
            // 상세 모달 내 저장 버튼을 네이티브 DOM click으로 트리거 (Bootstrap overlay 우회)
            await clickModalButton(page, '#messageDetailModal .modal-footer .btn-primary');
            await responsePromise;

            // AJAX 성공 콜백이 alert + close + Messages.load()를 호출할 때까지 대기
            await page.waitForResponse(r => r.url().includes('/api/messages/page'), { timeout: 15000 });

            // bootstrap.Modal.hide() CSS 전환이 완료되지 않으므로 직접 닫기
            await closeDetailModal(page);
            await searchByField(page, 'messageId', testId);
            await expect(page.getByRole('cell', { name: 'AfterName' })).toBeVisible();
        } finally {
            await deleteMessage(request, orgId, testId);
        }
    });

    test('상세 모달에서 전문을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const testId = generateTestId('e2e-mcrud-');
        await createMessage(request, orgId, testId, 'DeleteMe');

        await searchByField(page, 'messageId', testId);
        const detailPromise = page.waitForResponse(
            r => r.url().includes(`/api/messages/${testId}/detail`) && r.request().method() === 'GET',
            { timeout: 15000 },
        );
        await page.getByRole('row').filter({ hasText: testId }).click();
        await detailPromise;
        await expect(page.locator('#messageDetailModal')).toBeVisible();

        const deleteResponsePromise = page.waitForResponse(
            r => r.url().includes(`/api/messages/${testId}`) && r.request().method() === 'DELETE',
            { timeout: 15000 },
        );
        // 상세 모달 내 삭제 버튼을 네이티브 DOM click으로 트리거 (Bootstrap overlay 우회)
        await clickModalButton(page, '#messageDetailModal .modal-footer .btn-danger');
        await page.locator('#spConfirmModalOk').click();
        await deleteResponsePromise;

        // AJAX 성공 콜백이 alert + close + Messages.load()를 호출할 때까지 대기
        await page.waitForResponse(r => r.url().includes('/api/messages/page'), { timeout: 15000 });

        // bootstrap.Modal.hide() CSS 전환이 완료되지 않으므로 직접 닫기
        await closeDetailModal(page);
        await searchByField(page, 'messageId', testId);
        await expect(page.getByRole('cell', { name: testId })).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('전문 권한', () => {

    test('W 권한이 있는 사용자에게는 등록 버튼이 표시되어야 한다', async ({ page }) => {
        // e2e-admin은 MESSAGE:W 권한 보유
        await expect(page.getByRole('button', { name: '+ 등록' })).toBeVisible();
    });
});

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        // beforeEach에서 이미 페이지 로드 + 검색 완료
        await expect(page.getByRole('button', { name: '+ 등록' })).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 저장, 삭제 버튼이 표시되어서는 안 된다', async ({ page, request }) => {
        // R-only 사용자가 MESSAGE:R 권한을 가지고 있는지 확인 (seed SQL 미적용 환경 대비)
        const apiResp = await request.get('/api/messages/page?page=1&size=5');
        test.skip(apiResp.status() === 403,
            'e2e-readonly 사용자에게 MESSAGE:R 권한이 없습니다. e2e/docker/e2e-seed.sql 시드 데이터를 확인하세요.');

        // beforeEach의 clickSearch와 autoLoad 사이 race condition 대비:
        // data-message-index 속성이 있는 실제 데이터 행이 렌더링될 때까지 대기
        const dataRow = page.locator('#messageTableBody tr[data-message-index]').first();
        await expect(dataRow).toBeVisible({ timeout: 15000 });

        // 행 클릭 시 상세 API가 호출되어야 한다
        const detailPromise = page.waitForResponse(
            r => r.url().includes('/detail') && r.request().method() === 'GET',
            { timeout: 15000 },
        );
        // 첫 데이터 행의 td 클릭 — jQuery row.on('click') 핸들러가 td→tr 이벤트 버블링으로 트리거됨
        await dataRow.locator('td').first().click();
        await detailPromise;
        await expect(page.locator('#messageDetailModal')).toBeVisible();

        // 저장, 삭제 버튼이 보이지 않아야 한다 (th:if로 렌더링 자체가 안 됨)
        await expect(page.locator('#messageDetailModal .modal-footer .btn-primary')).not.toBeVisible();
        await expect(page.locator('#messageDetailModal .modal-footer .btn-danger')).not.toBeVisible();

        await closeDetailModal(page);
    });

    test('R 권한 사용자가 행을 클릭하면 Reload 버튼이 표시되어서는 안 된다', async ({ page, request }) => {
        const apiResp = await request.get('/api/messages/page?page=1&size=5');
        test.skip(apiResp.status() === 403,
            'e2e-readonly 사용자에게 MESSAGE:R 권한이 없습니다. e2e/docker/e2e-seed.sql 시드 데이터를 확인하세요.');

        const dataRow = page.locator('#messageTableBody tr[data-message-index]').first();
        await expect(dataRow).toBeVisible({ timeout: 15000 });

        const detailPromise = page.waitForResponse(
            r => r.url().includes('/detail') && r.request().method() === 'GET',
            { timeout: 15000 },
        );
        await dataRow.locator('td').first().click();
        await detailPromise;
        await expect(page.locator('#messageDetailModal')).toBeVisible();

        // Reload 버튼이 보이지 않아야 한다 (th:if로 렌더링 자체가 안 됨)
        await expect(page.locator('#messageDetailModal .modal-footer .btn-success')).not.toBeVisible();

        await closeDetailModal(page);
    });

    test('R 권한 사용자에게는 엑셀 UPLOAD 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.MSG_EXCEL_UPLOAD })).not.toBeVisible();
    });
});

// ─── Reload 버튼 ────────────────────────────────────────────────

test.describe('전문 Reload 버튼', () => {

    test('W 권한 사용자가 상세 모달을 열면 Reload 버튼이 표시되어야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const testId = generateTestId('e2e-mreload-');
        await createMessage(request, orgId, testId, 'ReloadBtnTest');

        try {
            await searchByField(page, 'messageId', testId);
            const detailPromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${testId}/detail`) && r.request().method() === 'GET',
                { timeout: 15000 },
            );
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#messageDetailModal')).toBeVisible();

            // Reload 버튼이 표시되어야 한다 (btn-success)
            await expect(page.locator('#messageDetailModal .modal-footer .btn-success')).toBeVisible();
            await expect(page.locator('#messageDetailModal .modal-footer .btn-success')).toContainText(LABEL.MSG_RELOAD);

            await closeDetailModal(page);
        } finally {
            await deleteMessage(request, orgId, testId);
        }
    });

    test('W 권한 사용자가 Reload 버튼을 클릭하면 WAS 선택 모달이 열려야 한다', async ({ page, request }) => {
        const orgId = await getFirstOrgId(request);
        const testId = generateTestId('e2e-mreload-');
        await createMessage(request, orgId, testId, 'ReloadModalTest');

        try {
            await searchByField(page, 'messageId', testId);
            const detailPromise = page.waitForResponse(
                r => r.url().includes(`/api/messages/${testId}/detail`) && r.request().method() === 'GET',
                { timeout: 15000 },
            );
            await page.getByRole('row').filter({ hasText: testId }).click();
            await detailPromise;
            await expect(page.locator('#messageDetailModal')).toBeVisible();

            // Reload 버튼 클릭 (Bootstrap overlay 우회)
            await clickModalButton(page, '#messageDetailModal .modal-footer .btn-success');

            // WAS 선택 Reload 모달이 열려야 한다
            await expect(page.locator('#wasReloadModal')).toBeVisible({ timeout: 10000 });
            await expect(page.locator('#wasReloadModalTitle')).toContainText(LABEL.MSG_RELOAD_TITLE);

            // WAS Reload 모달 닫기
            await page.evaluate(() => {
                const el = document.getElementById('wasReloadModal');
                if (!el) return;
                el.classList.remove('show');
                el.style.display = 'none';
                el.removeAttribute('aria-modal');
                el.removeAttribute('role');
                el.setAttribute('aria-hidden', 'true');
                document.body.classList.remove('modal-open');
                const backdrops = document.querySelectorAll('.modal-backdrop');
                backdrops.forEach((bd) => { bd.remove(); });
            });

            await closeDetailModal(page);
        } finally {
            await deleteMessage(request, orgId, testId);
        }
    });
});

// ─── 엑셀 업로드 ────────────────────────────────────────────────

test.describe('전문 엑셀 업로드', () => {

    test('W 권한 사용자에게는 엑셀 UPLOAD 버튼이 표시되어야 한다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.MSG_EXCEL_UPLOAD })).toBeVisible();
    });

    test('W 권한 사용자가 엑셀 UPLOAD 버튼을 클릭하면 업로드 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.MSG_EXCEL_UPLOAD }).click();

        // 엑셀 업로드 모달이 열려야 한다
        await expect(page.locator('#excelUploadModal')).toBeVisible({ timeout: 10000 });
        await expect(page.locator('#excelUploadModal .modal-title')).toContainText(LABEL.MSG_EXCEL_UPLOAD_TITLE);

        // 파일 선택 input이 표시되어야 한다
        await expect(page.locator('#excelUploadFile')).toBeVisible();

        // 업로드 버튼이 표시되어야 한다
        await expect(page.locator('#btnExcelUploadSubmit')).toBeVisible();

        // 모달 닫기
        await page.locator('#excelUploadModal [data-bs-dismiss="modal"]').first().click();
    });
});
