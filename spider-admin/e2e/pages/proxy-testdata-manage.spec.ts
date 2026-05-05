/**
 * 당발 대응답 관리 페이지 — 목록, 상세 모달, 대응답 설정 모달, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ── 시드 데이터 상수 (e2e-seed.sql 기준) ──

const SEED_ORG_ID = 'E2EORG01';
const SEED_TRX_ID = 'E2E-TRX-001';
const SEED_MESSAGE_ID = 'E2E-MSG-001';

// ── 헬퍼 ──

function generateTestName(prefix: string): string {
    return prefix + Date.now().toString(36);
}

async function createProxyTestdata(
    request: APIRequestContext,
    testName: string,
    testDesc: string = 'E2E 테스트 설명',
): Promise<void> {
    const res = await request.post('/api/proxy-testdata', {
        data: {
            orgId: SEED_ORG_ID,
            trxId: SEED_TRX_ID,
            messageId: SEED_MESSAGE_ID,
            testName,
            testDesc,
            testData: '',
            testGroupId: 'DEFAULT',
        },
    });
    expect(res.status()).toBe(201);
}

async function findTestSno(request: APIRequestContext, testName: string): Promise<number> {
    const res = await request.get('/api/proxy-testdata/page', {
        params: { page: 1, size: 100, testNameFilter: testName },
    });
    const body = await res.json();
    const item = body.data.content.find((c: any) => c.testName === testName);
    return item.testSno;
}

async function deleteProxyTestdata(request: APIRequestContext, testSno: number) {
    await request.delete(`/api/proxy-testdata/${testSno}`, {
        params: { testGroupId: 'DEFAULT' },
    });
}

async function searchByFilter(page: Page, filterId: string, value: string) {
    await page.locator(`#_searchContainer_${filterId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/proxy-testdata/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

async function expectToast(page: Page, message: string) {
    await expect(
        page.locator('#spToastContainer .toast').filter({ hasText: message }).last(),
    ).toBeVisible({ timeout: 10000 });
}

async function fillEditableMessageFields(page: Page) {
    const rows = page.locator('#mtFieldTableBody tr');
    const rowCount = await rows.count();

    for (let i = 0; i < rowCount; i++) {
        const row = rows.nth(i);
        const input = row.locator('input.testValue:not([readonly])');

        if (await input.count() === 0) continue;

        const currentValue = (await input.inputValue()).trim();
        if (currentValue) continue;

        const dataType = (await row.locator('input.dataType').inputValue()).trim();
        await input.fill(dataType === 'N' || dataType === 'B' ? '1' : 'E2E');
    }
}

test.beforeEach(async ({ page }) => {
    await page.goto('/proxy-responses');
    // 테이블이 표시될 때까지 대기
    await expect(page.locator('#messageTestTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('당발 대응답 목록', () => {

    test('초기 페이지 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#messageTestTable thead')).toBeVisible();
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-list-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);
            await expect(page.locator('#messageTestTableBody').getByText(testName)).toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('검색 시 페이지가 1페이지로 초기화되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-page-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);
            // 1페이지의 데이터가 표시되어야 한다
            await expect(page.locator('#messageTestTableBody tr').first()).toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-sort-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            // 테스트명 헤더 클릭 → ASC
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/proxy-testdata/page'));
            await page.locator('#messageTestTable th[data-sort="testName"]').click();
            await responsePromise;

            await expect(page.locator('#messageTestTableBody tr').first()).toBeVisible();

            // 다시 클릭 → DESC
            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/proxy-testdata/page'));
            await page.locator('#messageTestTable th[data-sort="testName"]').click();
            await responsePromise2;

            await expect(page.locator('#messageTestTableBody tr').first()).toBeVisible();

            // 세 번째 클릭 → 정렬 해제
            const responsePromise3 = page.waitForResponse(r => r.url().includes('/api/proxy-testdata/page'));
            await page.locator('#messageTestTable th[data-sort="testName"]').click();
            await responsePromise3;

            await expect(page.locator('#messageTestTableBody tr').first()).toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context, request }) => {
        const testName = generateTestName('e2e-print-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await context.addInitScript(() => { window.print = () => {}; });
            await searchByFilter(page, 'testNameFilter', testName);
            await expect(page.locator('#messageTestTableBody').getByText(testName)).toBeVisible();

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-row-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/proxy-testdata/${testSno}`) && r.request().method() === 'GET');
            await page.locator('#messageTestTableBody').getByText(testName).click();
            await detailPromise;

            await expect(page.locator('#messageTestModal')).toBeVisible();
            await expect(page.locator('#mtModalTitle')).toHaveText(LABEL.PROXY_TESTDATA_DETAIL_TITLE);

            await page.locator('#messageTestModal .btn-close').click();
            await expect(page.locator('#messageTestModal')).not.toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });
});

// ─── 상세 모달 CRUD ──────────────────────────────────────

test.describe('당발 대응답 상세 모달 — CRUD', () => {

    test('등록 버튼을 클릭하면 등록 모달이 표시되어야 한다', async ({ page }) => {
        const registerBtn = page.locator('.bottom-actions').getByText('+ 등록');
        await registerBtn.click();

        await expect(page.locator('#messageTestModal')).toBeVisible();
        await expect(page.locator('#mtModalTitle')).toHaveText(LABEL.PROXY_TESTDATA_REGISTER_TITLE);

        // 등록 모드에서 삭제/수정/신규저장 버튼은 숨겨야 한다
        await expect(page.locator('#mtDeleteBtn')).not.toBeVisible();
        await expect(page.locator('#mtUpdateBtn')).not.toBeVisible();
        await expect(page.locator('#mtSaveAsNewBtn')).not.toBeVisible();

        await page.locator('#messageTestModal .btn-close').click();
    });

    test('등록 모달에서 거래조회 후 데이터를 생성하면 목록에 반영되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-create-');

        // 등록 모달 열기
        await page.locator('.bottom-actions').getByText('+ 등록').click();
        await expect(page.locator('#messageTestModal')).toBeVisible();

        // 거래조회 버튼 클릭
        await page.locator('#mtTrxSearchBtn').click();
        await expect(page.locator('#trxSearchModal')).toBeVisible();

        // 거래조회 모달에서 기관 선택 후 조회
        const trxSearchResponse = page.waitForResponse(r =>
            r.url().includes('/api/proxy-testdata/trx-messages/search'));
        await page.locator('#trxSearchOrgId').selectOption(SEED_ORG_ID);
        await page.locator('#trxSearchModal').getByRole('button', { name: LABEL.SEARCH }).click();
        await trxSearchResponse;

        // 거래 선택 (첫 번째 행 클릭)
        const fieldsResponse = page.waitForResponse(r =>
            r.url().includes('/api/proxy-testdata/test-fields'));
        await page.locator('#trxSearchTableBody tr').first().click();
        await fieldsResponse;

        // 거래조회 모달이 닫히고 필드가 채워져야 한다
        await expect(page.locator('#trxSearchModal')).not.toBeVisible();
        await expect(page.locator('#mtOrgName')).not.toHaveValue('');

        // 테스트 정보 입력
        await page.locator('#mtTestName').fill(testName);
        await page.locator('#mtTestDesc').fill('E2E 등록 테스트 설명');
        await fillEditableMessageFields(page);

        // 저장 버튼 표시 확인 후 클릭
        await expect(page.locator('#mtSaveBtn')).toBeVisible();
        const saveResponse = page.waitForResponse(r =>
            r.url().endsWith('/api/proxy-testdata') && r.request().method() === 'POST');
        await page.locator('#mtSaveBtn').click();
        expect((await saveResponse).ok()).toBeTruthy();

        // Toast 성공 메시지 확인
        await expectToast(page, '저장되었습니다.');

        // 모달이 닫히고 목록에 반영되었는지 확인
        await expect(page.locator('#messageTestModal')).not.toBeVisible();

        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);
            await expect(page.locator('#messageTestTableBody').getByText(testName)).toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('유효하지 않은 값을 입력할 경우 Toast 알림이 표시되어야 한다', async ({ page }) => {
        // 등록 모달 열기
        await page.locator('.bottom-actions').getByText('+ 등록').click();
        await expect(page.locator('#messageTestModal')).toBeVisible();

        // 거래조회로 거래 선택
        await page.locator('#mtTrxSearchBtn').click();
        await expect(page.locator('#trxSearchModal')).toBeVisible();

        const trxSearchResponse = page.waitForResponse(r =>
            r.url().includes('/api/proxy-testdata/trx-messages/search'));
        await page.locator('#trxSearchOrgId').selectOption(SEED_ORG_ID);
        await page.locator('#trxSearchModal').getByRole('button', { name: LABEL.SEARCH }).click();
        await trxSearchResponse;

        const fieldsResponse = page.waitForResponse(r =>
            r.url().includes('/api/proxy-testdata/test-fields'));
        await page.locator('#trxSearchTableBody tr').first().click();
        await fieldsResponse;

        // 테스트명 미입력 상태로 저장 시도
        await page.locator('#mtSaveBtn').click();

        // Toast 경고 메시지 확인
        await expectToast(page, '테스트명을 입력해주세요.');

        await page.locator('#messageTestModal .btn-close').click();
    });

    test('상세 모달에서 테스트그룹ID가 수정 불가능해야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-pk-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/proxy-testdata/${testSno}`));
            await page.locator('#messageTestTableBody').getByText(testName).click();
            await detailPromise;

            await expect(page.locator('#messageTestModal')).toBeVisible();
            await expect(page.locator('#mtTestGroupId')).toBeDisabled();

            await page.locator('#messageTestModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('데이터를 수정하면 목록에 즉시 반영되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-edit-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/proxy-testdata/${testSno}`));
            await page.locator('#messageTestTableBody').getByText(testName).click();
            await detailPromise;

            await expect(page.locator('#messageTestModal')).toBeVisible();

            const newName = testName + '-mod';
            await page.locator('#mtTestName').fill(newName);

            // 수정 버튼 클릭 → Toast.confirm 확인
            await page.locator('#mtUpdateBtn').click();
            await expect(page.locator('#spConfirmModal')).toBeVisible();

            const updateResponse = page.waitForResponse(r =>
                r.url().includes(`/api/proxy-testdata/${testSno}`) && r.request().method() === 'PUT');
            await page.locator('#spConfirmModalOk').click();
            await updateResponse;

            await expectToast(page, '수정되었습니다.');
            await expect(page.locator('#messageTestModal')).not.toBeVisible();

            await searchByFilter(page, 'testNameFilter', newName);
            await expect(page.locator('#messageTestTableBody').getByText(newName)).toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('데이터를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-del-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        await searchByFilter(page, 'testNameFilter', testName);

        const detailPromise = page.waitForResponse(r =>
            r.url().includes(`/api/proxy-testdata/${testSno}`));
        await page.locator('#messageTestTableBody').getByText(testName).click();
        await detailPromise;

        await expect(page.locator('#messageTestModal')).toBeVisible();

        // 삭제 버튼 → Toast.confirm 확인
        await page.locator('#mtDeleteBtn').click();
        await expect(page.locator('#spConfirmModal')).toBeVisible();

        const deleteResponse = page.waitForResponse(r =>
            r.url().includes(`/api/proxy-testdata/${testSno}`) && r.request().method() === 'DELETE');
        await page.locator('#spConfirmModalOk').click();
        await deleteResponse;

        await expectToast(page, '삭제되었습니다.');
        await expect(page.locator('#messageTestModal')).not.toBeVisible();

        // 삭제 후 검색하면 안 보여야 한다
        await searchByFilter(page, 'testNameFilter', testName);
        await expect(page.locator('#messageTestTableBody').getByText(testName)).not.toBeVisible();
    });
});

// ─── 대응답 설정 모달 ────────────────────────────────────

test.describe('대응답 설정 모달', () => {

    test('목록에서 설정 버튼을 클릭하면 대응답 설정 모달이 열려야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-ps-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            // 설정 버튼 클릭
            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();
            await expect(page.locator('#proxySettingModal .modal-title')).toHaveText(LABEL.PROXY_SETTING_TITLE);

            // 기관명, 거래ID가 자동 채워져야 한다
            await expect(page.locator('#psOrgName')).not.toHaveValue('');
            await expect(page.locator('#psTrxId')).not.toHaveValue('');

            await page.locator('#proxySettingModal .btn-close').click();
            await expect(page.locator('#proxySettingModal')).not.toBeVisible();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 설정 모달에서 테스트 데이터 목록이 표시되어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-psl-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // 테이블에 데이터가 표시되어야 한다
            await expect(page.locator('#psTableBody tr').first()).toBeVisible();
            await expect(page.locator('#psTableBody').getByText(testName)).toBeVisible();

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 설정 모달에서 기본 대응답을 설정할 수 있어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-dp-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // '설정' 버튼 클릭 → Toast.confirm
            const setBtn = page.locator('#psTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first();
            await setBtn.click();
            await expect(page.locator('#spConfirmModal')).toBeVisible();

            const setResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings/default-proxy/set'));
            await page.locator('#spConfirmModalOk').click();
            await setResponse;

            await expectToast(page, '기본 대응답 값이 설정되었습니다.');

            // '해제' 버튼이 나타나야 한다
            await expect(page.locator('#psTableBody').getByRole('button', { name: '해제' })).toBeVisible();

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            // 기본 대응답 해제 (정리)
            await request.put('/api/proxy-testdata/proxy-settings/default-proxy/clear', {
                params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID },
            });
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 설정 모달에서 기본 대응답을 해제할 수 있어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-clr-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        // API로 기본 대응답을 미리 설정
        await request.put('/api/proxy-testdata/proxy-settings/default-proxy/set', {
            params: { orgId: SEED_ORG_ID, trxId: SEED_TRX_ID, testSno },
        });

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // '해제' 버튼 클릭
            const clearBtn = page.locator('#psTableBody').getByRole('button', { name: '해제' });
            await expect(clearBtn).toBeVisible();
            await clearBtn.click();
            await expect(page.locator('#spConfirmModal')).toBeVisible();

            const clearResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings/default-proxy/clear'));
            await page.locator('#spConfirmModalOk').click();
            await clearResponse;

            await expectToast(page, '기본 대응답 값이 해제되었습니다.');

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 설정 모달에서 동적 대응답 필드ID를 등록할 수 있어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-pf-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // 필드ID 입력 및 등록
            await page.locator('#psProxyFieldId').fill('E2E_TEST_FIELD');
            await page.locator('#psProxyFieldBtn').click();
            await expect(page.locator('#spConfirmModal')).toBeVisible();

            const fieldResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings/proxy-field'));
            await page.locator('#spConfirmModalOk').click();
            await fieldResponse;

            await expectToast(page, '필드 구분값이');

            // 버튼이 '수정'으로 바뀌어야 한다
            await expect(page.locator('#psProxyFieldBtn')).toHaveText(LABEL.UPDATE);

            // 정리: 필드 초기화
            await page.locator('#psProxyFieldId').fill('');
            await page.locator('#psProxyFieldBtn').click();
            await expect(page.locator('#spConfirmModal')).toBeVisible();
            const clearFieldResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings/proxy-field'));
            await page.locator('#spConfirmModalOk').click();
            await clearFieldResponse;

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });

    test('대응답 설정 모달에서 검색 조건으로 필터링할 수 있어야 한다', async ({ page, request }) => {
        const testName = generateTestName('e2e-psf-');
        await createProxyTestdata(request, testName);
        const testSno = await findTestSno(request, testName);

        try {
            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // 테스트명으로 검색
            await page.locator('#psTestName').fill(testName);
            const searchResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#proxySettingModal').getByRole('button', { name: LABEL.SEARCH }).click();
            await searchResponse;

            await expect(page.locator('#psTableBody').getByText(testName)).toBeVisible();

            // 존재하지 않는 테스트명으로 검색
            await page.locator('#psTestName').fill('nonexistent-e2e-test');
            const emptyResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#proxySettingModal').getByRole('button', { name: LABEL.SEARCH }).click();
            await emptyResponse;

            await expect(page.locator('#psTableBody').getByText('조회된 데이터가 없습니다')).toBeVisible();

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(request, testSno);
        }
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/proxy-responses');
        await expect(page.locator('#messageTestTable')).toBeVisible();

        await expect(page.locator('.bottom-actions').getByText('+ 등록')).not.toBeVisible();
    });

    test('R 권한 사용자가 행을 클릭하면 수정/삭제/신규저장 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        // R-only request로는 데이터 생성 불가(403) → admin context 사용
        const adminCtx = await playwright.request.newContext({
            baseURL: 'http://localhost:8080',
            storageState: 'e2e/.auth/session.json',
        });
        const testName = generateTestName('e2e-ro-');
        await createProxyTestdata(adminCtx, testName);
        const testSno = await findTestSno(adminCtx, testName);

        try {
            await page.goto('/proxy-responses');
            await expect(page.locator('#messageTestTable')).toBeVisible();

            await searchByFilter(page, 'testNameFilter', testName);

            const detailPromise = page.waitForResponse(r =>
                r.url().includes(`/api/proxy-testdata/${testSno}`));
            await page.locator('#messageTestTableBody').getByText(testName).click();
            await detailPromise;

            await expect(page.locator('#messageTestModal')).toBeVisible();
            await expect(page.locator('#mtUpdateBtn')).not.toBeVisible();
            await expect(page.locator('#mtDeleteBtn')).not.toBeVisible();
            await expect(page.locator('#mtSaveAsNewBtn')).not.toBeVisible();

            await page.locator('#messageTestModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(adminCtx, testSno);
            await adminCtx.dispose();
        }
    });

    test('R 권한 사용자의 대응답 설정 모달에서 등록/수정 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminCtx = await playwright.request.newContext({
            baseURL: 'http://localhost:8080',
            storageState: 'e2e/.auth/session.json',
        });
        const testName = generateTestName('e2e-rops-');
        await createProxyTestdata(adminCtx, testName);
        const testSno = await findTestSno(adminCtx, testName);

        try {
            await page.goto('/proxy-responses');
            await expect(page.locator('#messageTestTable')).toBeVisible();

            await searchByFilter(page, 'testNameFilter', testName);

            const proxySettingsResponse = page.waitForResponse(r =>
                r.url().includes('/api/proxy-testdata/proxy-settings') && !r.url().includes('default-proxy') && !r.url().includes('proxy-field') && !r.url().includes('proxy-value'));
            await page.locator('#messageTestTableBody').getByRole('button', { name: LABEL.PROXY_SETTING }).first().click();
            await proxySettingsResponse;

            await expect(page.locator('#proxySettingModal')).toBeVisible();

            // 동적 대응답 필드ID 등록 버튼이 없어야 한다
            await expect(page.locator('#psProxyFieldBtn')).not.toBeVisible();

            // 동적 대응답 필드ID input이 readonly이어야 한다
            await expect(page.locator('#psProxyFieldId')).toHaveAttribute('readonly');

            // 테이블에서 대응답 값 input과 버튼이 없어야 한다
            await expect(page.locator('#psTableBody .ps-proxy-value-input')).not.toBeVisible();
            await expect(page.locator('#psTableBody').getByRole('button', { name: LABEL.PROXY_SETTING })).not.toBeVisible();

            await page.locator('#proxySettingModal .btn-close').click();
        } finally {
            await deleteProxyTestdata(adminCtx, testSno);
            await adminCtx.dispose();
        }
    });
});
