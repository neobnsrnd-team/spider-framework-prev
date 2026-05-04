/**
 * 중지거래 접근허용자 관리 페이지 — 목록, 엑셀·출력, 인라인 CRUD, 기타, 국제화, 권한.
 *
 * UI 패턴: 인라인 행 추가/편집 + 배치 저장 (모달 없음)
 * - 신규 행: '행 추가' 버튼 → 테이블 하단에 입력 행 추가 → '변경사항 저장'
 * - 기존 행: PK 필드(구분유형, 거래서비스ID, 접근허용 사용자ID) 클릭 → 수정불가 alert
 *           useYn 필드만 select로 수정 가능 → '변경사항 저장'
 * - 삭제: 체크박스 선택 → '선택행 삭제' → 삭제 배지 표시 → '변경사항 저장'
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext, type Response } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const PAGE_URL = '/stop-transaction-accessors';
const API_PAGE = (r: Response) => r.url().includes('/api/access-users/page');

// ─── 인라인 헬퍼 ─────────────────────────────────────────

let seq = 0;
function genTrxId(): string {
    return 'E2ETRX' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0');
}
function genCustId(): string {
    return 'E2ECST' + Date.now().toString(36).slice(-5) + String(seq++).padStart(2, '0');
}

async function createAccessUser(
    request: APIRequestContext,
    trxId: string,
    custUserId: string,
    gubunType = 'T',
    useYn = 'Y',
) {
    const res = await request.post('/api/access-users', {
        data: { gubunType, trxId, custUserId, useYn },
    });
    expect(res.status()).toBe(201);
}

async function deleteAccessUser(
    request: APIRequestContext,
    gubunType: string,
    trxId: string,
    custUserId: string,
) {
    await request.delete(
        `/api/access-users/${encodeURIComponent(gubunType)}/${encodeURIComponent(trxId)}/${encodeURIComponent(custUserId)}`,
    );
}

async function searchByTrxId(page: Page, trxId: string) {
    await page.locator('#searchTrxId').fill(trxId);
    const responsePromise = page.waitForResponse(API_PAGE);
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function bottomBtn(page: Page, name: string) {
    return page.locator('.bottom-actions').getByRole('button', { name });
}

// ─── 공통 setup ──────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(API_PAGE);
    await page.goto(PAGE_URL);
    await responsePromise;
    await expect(page.locator('#accessorTable')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('접근허용자 목록', () => {

    test('초기 페이지 로드 시 데이터는 20건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#accessorTableBody tr');
        await expect(rows.first()).toBeVisible();
        expect(await rows.count()).toBeLessThanOrEqual(20);
    });

    test('거래서비스ID 검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await searchByTrxId(page, trxId);
            await expect(page.locator('#accessorTableBody').getByText(trxId)).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('구분유형 검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId, 'S');

        try {
            await page.locator('#searchGubunType').selectOption('S');
            const responsePromise = page.waitForResponse(API_PAGE);
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;

            await expect(page.locator('#accessorTableBody').getByText('서비스').first()).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'S', trxId, custUserId);
        }
    });

    test('접근허용 사용자ID 검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await page.locator('#searchCustUserId').fill(custUserId);
            const responsePromise = page.waitForResponse(API_PAGE);
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;

            await expect(page.locator('#accessorTableBody').getByText(custUserId)).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('검색 조건을 변경하면 페이지가 1페이지로 초기화되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(API_PAGE);
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        const res = await responsePromise;
        const url = new URL(res.url());
        expect(url.searchParams.get('page')).toBe('1');
    });

    test('컬럼 헤더를 클릭하면 해당 컬럼 기준으로 정렬이 변경되어야 한다', async ({ page }) => {
        const trxHeader = page.locator('#accessorTable thead th[data-sort="trxId"]');
        await expect(trxHeader).toBeVisible();

        const res1 = page.waitForResponse(API_PAGE);
        await trxHeader.click();
        const url1 = new URL((await res1).url());
        expect(url1.searchParams.get('sortBy')).toBe('trxId');
        expect(url1.searchParams.get('sortDirection')).toBe('ASC');

        const res2 = page.waitForResponse(API_PAGE);
        await trxHeader.click();
        const url2 = new URL((await res2).url());
        expect(url2.searchParams.get('sortDirection')).toBe('DESC');
    });

    test('거래서비스ID 입력 후 Enter 키를 누르면 조회되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await page.locator('#searchTrxId').fill(trxId);
            const responsePromise = page.waitForResponse(API_PAGE);
            await page.locator('#searchTrxId').press('Enter');
            await responsePromise;

            await expect(page.locator('#accessorTableBody').getByText(trxId)).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });
});

// ─── 엑셀·출력 ───────────────────────────────────────────

test.describe('접근허용자 엑셀·출력', () => {

    test('[조회] 엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('[조회] 출력 버튼을 클릭하면 인쇄 동작이 실행되어야 한다', async ({ page }) => {
        await page.addInitScript(() => {
            (window as any)._printCalled = false;
            window.print = () => { (window as any)._printCalled = true; };
        });

        const responsePromise = page.waitForResponse(API_PAGE);
        await page.goto(PAGE_URL);
        await responsePromise;

        await page.getByRole('button', { name: LABEL.PRINT }).click();

        await page.waitForFunction(
            () => (window as any)._printCalled === true,
            { timeout: 3000 },
        );
        expect(await page.evaluate(() => (window as any)._printCalled)).toBe(true);
    });
});

// ─── 생성 ────────────────────────────────────────────────

test.describe('접근허용자 생성', () => {

    test("'행 추가' 버튼을 클릭하면 테이블 하단에 새로운 입력 행이 추가되어야 한다", async ({ page }) => {
        const beforeCount = await page.locator('#accessorTableBody tr').count();

        await bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW).click();

        const afterCount = await page.locator('#accessorTableBody tr').count();
        expect(afterCount).toBe(beforeCount + 1);

        const newRow = page.locator('#accessorTableBody tr[data-is-new="true"]');
        await expect(newRow).toBeVisible();
        await expect(newRow.getByText(LABEL.CRUD_CREATE)).toBeVisible();
    });

    test('신규 행에서 필수 필드를 입력하고 변경사항 저장을 클릭하면 데이터가 저장되고 목록에 반영되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();

        try {
            await bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW).click();

            const newRow = page.locator('#accessorTableBody tr[data-is-new="true"]');
            await newRow.locator('[data-field="gubunType"]').selectOption('T');
            await newRow.locator('[data-field="trxId"]').fill(trxId);
            await newRow.locator('[data-field="custUserId"]').fill(custUserId);
            await newRow.locator('[data-field="useYn"]').selectOption('Y');

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/access-users') && r.request().method() === 'POST');
            await bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            await searchByTrxId(page, trxId);
            await expect(page.locator('#accessorTableBody').getByText(trxId)).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('중복된 복합 PK 데이터를 저장하면 오류 알림이 표시되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW).click();

            const newRow = page.locator('#accessorTableBody tr[data-is-new="true"]');
            await newRow.locator('[data-field="gubunType"]').selectOption('T');
            await newRow.locator('[data-field="trxId"]').fill(trxId);
            await newRow.locator('[data-field="custUserId"]').fill(custUserId);
            await newRow.locator('[data-field="useYn"]').selectOption('Y');

            await bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();

            await expect(page.locator('.toast')).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('신규 행에서 필수 필드를 비우고 저장하면 유효성 검사 알림이 표시되어야 한다', async ({ page }) => {
        await bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW).click();

        const newRow = page.locator('#accessorTableBody tr[data-is-new="true"]');
        // trxId 비워둔 채 저장
        await newRow.locator('[data-field="custUserId"]').fill('SOMEUSER');

        await bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES).click();

        await expect(page.locator('.toast')).toBeVisible();
    });
});

// ─── 수정 ────────────────────────────────────────────────

test.describe('접근허용자 수정', () => {

    test('기존 행의 PK 필드(구분유형)를 클릭하면 수정 불가 알림이 표시되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await searchByTrxId(page, trxId);

            const row = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
            // 구분유형 셀 (PK 클릭)
            await row.locator('td').nth(2).click();

            await expect(page.locator('.toast')).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('기존 행에서 사용여부(useYn)만 변경 가능해야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        try {
            await searchByTrxId(page, trxId);

            const row = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
            const useYnSelect = row.locator('select[data-field="useYn"]');
            await expect(useYnSelect).toBeVisible();
            await expect(useYnSelect).toBeEnabled();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test('사용여부를 변경하고 변경사항 저장을 클릭하면 변경 내용이 반영되어야 한다', async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId, 'T', 'Y');

        try {
            await searchByTrxId(page, trxId);

            const row = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
            await row.locator('select[data-field="useYn"]').selectOption('N');

            const responsePromise = page.waitForResponse(r => r.url().includes('/api/access-users') && r.request().method() === 'PUT');
            await bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES).click();
            await page.locator('#spConfirmModalOk').click();
            await responsePromise;

            // 저장 후 재조회하여 반영 확인
            await searchByTrxId(page, trxId);
            const updatedRow = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
            await expect(updatedRow.locator('select[data-field="useYn"]')).toHaveValue('N');
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });
});

// ─── 삭제 ────────────────────────────────────────────────

test.describe('접근허용자 삭제', () => {

    test("체크박스 선택 후 '선택행 삭제'를 클릭하면 해당 행에 삭제 배지가 표시되어야 한다", async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        page.on('dialog', d => d.accept());

        try {
            await searchByTrxId(page, trxId);

            const row = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
            await row.locator('.row-checkbox').check();
            await bottomBtn(page, LABEL.ACCESS_USER_DELETE_SELECTED).click();

            await expect(row.getByText(LABEL.CRUD_DELETE)).toBeVisible();
        } finally {
            await deleteAccessUser(request, 'T', trxId, custUserId);
        }
    });

    test("'변경사항 저장'을 클릭하면 삭제 예정 항목이 실제 삭제되어야 한다", async ({ page, request }) => {
        const trxId = genTrxId();
        const custUserId = genCustId();
        await createAccessUser(request, trxId, custUserId);

        page.on('dialog', d => d.accept());

        await searchByTrxId(page, trxId);

        const row = page.locator('#accessorTableBody tr').filter({ hasText: trxId });
        await row.locator('.row-checkbox').check();
        await bottomBtn(page, LABEL.ACCESS_USER_DELETE_SELECTED).click();

        const responsePromise = page.waitForResponse(r => r.url().includes('/api/access-users') && r.request().method() === 'DELETE');
        await bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES).click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await searchByTrxId(page, trxId);
        await expect(page.locator('#accessorTableBody').getByText(trxId)).not.toBeVisible();
    });
});

// ─── 기타 기능 ───────────────────────────────────────────

test.describe('접근허용자 기타 기능', () => {

    test("신규 추가 행을 체크 후 '선택행 삭제'를 클릭하면 DB 요청 없이 즉시 행이 제거되어야 한다", async ({ page }) => {
        page.on('dialog', d => d.accept());

        await bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW).click();
        const newRow = page.locator('#accessorTableBody tr[data-is-new="true"]');
        await expect(newRow).toBeVisible();

        await newRow.locator('.row-checkbox').check();

        // API 호출 없이 즉시 DOM에서 제거
        const apiCalled = { value: false };
        page.on('request', req => {
            if (req.url().includes('/api/access-users') && req.method() === 'DELETE') {
                apiCalled.value = true;
            }
        });

        await bottomBtn(page, LABEL.ACCESS_USER_DELETE_SELECTED).click();
        await expect(newRow).not.toBeVisible();
        expect(apiCalled.value).toBe(false);
    });

    test("'Reload' 버튼을 클릭하면 WAS 인스턴스 선택 모달이 표시되어야 한다", async ({ page }) => {
        await bottomBtn(page, LABEL.ACCESS_USER_RELOAD).click();
        await expect(page.getByText(LABEL.ACCESS_USER_RELOAD_MODAL_TITLE)).toBeVisible();
    });
});

// ─── 국제화 ──────────────────────────────────────────────

test.describe('국제화', () => {
    test('언어 변경 버튼을 클릭하면 데이터를 제외한 모든 UI 텍스트가 해당 언어로 변경되어야 한다', async ({ page: _page }) => {
        // TODO: 언어 변경 기능 미구현 — 기능 구현 후 테스트 활성화
        test.skip(true, '언어 변경 기능이 아직 구현되지 않았습니다');
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자도 목록을 조회할 수 있어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(API_PAGE);
        await page.goto(PAGE_URL);
        await responsePromise;
        await expect(page.locator('#accessorTable')).toBeVisible();
    });

    test("R 권한 사용자에게는 '행 추가' 버튼이 표시되어서는 안 된다", async ({ page }) => {
        await expect(bottomBtn(page, LABEL.ACCESS_USER_ADD_ROW)).not.toBeVisible();
    });

    test("R 권한 사용자에게는 '선택행 삭제' 버튼이 표시되어서는 안 된다", async ({ page }) => {
        await expect(bottomBtn(page, LABEL.ACCESS_USER_DELETE_SELECTED)).not.toBeVisible();
    });

    test("R 권한 사용자에게는 '변경사항 저장' 버튼이 표시되어서는 안 된다", async ({ page }) => {
        await expect(bottomBtn(page, LABEL.ACCESS_USER_SAVE_CHANGES)).not.toBeVisible();
    });
});
