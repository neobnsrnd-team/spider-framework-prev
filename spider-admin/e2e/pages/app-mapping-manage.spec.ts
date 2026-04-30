/**
 * 요청처리 App 맵핑관리 — 목록·검색·정렬·CRUD·거래조회 모달·권한 (21 tests)
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── Helpers (inlined) ───────────────────────────────────────

async function fetchFirstGatewayId(request: APIRequestContext): Promise<string | null> {
    const res = await request.get('/api/gateways/page', { params: { page: 1, size: 1 } });
    if (!res.ok()) return null;
    const body = await res.json();
    const content: Array<{ gwId: string }> = body.data?.content ?? [];
    return content[0]?.gwId ?? null;
}

async function createAppMapping(
    request: APIRequestContext,
    gwId: string,
    reqIdCode: string,
    extras: { orgId?: string; trxId?: string; bizAppId?: string } = {},
) {
    await request.post('/api/interface-mnt/app-mappings', {
        data: { gwId, reqIdCode, ...extras },
    });
}

async function deleteAppMapping(request: APIRequestContext, gwId: string, reqIdCode: string) {
    await request.delete(
        `/api/interface-mnt/app-mappings/${encodeURIComponent(gwId)}/${encodeURIComponent(reqIdCode)}`,
    );
}

function generateReqIdCode(prefix: string = 'E2E-'): string {
    return (prefix + Date.now().toString(36)).toUpperCase().slice(0, 20);
}

// ─── Shared constants ────────────────────────────────────────

const API_PATH = '/api/interface-mnt/app-mappings';

const isListResponse = (url: string) =>
    url.includes(API_PATH) && !url.includes('export') && !url.includes(API_PATH + '/');

// ─── Shared setup ────────────────────────────────────────────

let gwId: string;

test.beforeAll(async ({ request }) => {
    const id = await fetchFirstGatewayId(request);
    if (!id) test.skip(true, '사용 가능한 게이트웨이가 없어 테스트를 건너뜁니다.');
    gwId = id!;
});

test.beforeEach(async ({ page }) => {
    const responsePromise = page.waitForResponse(r => isListResponse(r.url()));
    await page.goto('/transports');
    await responsePromise;
    await expect(page.getByRole('table')).toBeVisible();
});

// ─── 목록·검색·정렬 ─────────────────────────────────────────

test.describe('목록', () => {
    test('초기 페이지 로드 시 데이터는 10건이 조회되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => isListResponse(r.url()));
        await page.reload();
        const response = await responsePromise;
        const url = new URL(response.url());
        expect(url.searchParams.get('size')).toBe('10');
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되고 페이지는 1로 초기화되어야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('SEARCH-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const responsePromise = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            const response = await responsePromise;

            const url = new URL(response.url());
            expect(url.searchParams.get('page')).toBe('1');
            expect(url.searchParams.get('reqIdCode')).toBe(reqIdCode);
            await expect(page.getByRole('cell', { name: reqIdCode })).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('컬럼 헤더를 클릭하면 해당 컬럼 기준으로 정렬이 변경되어야 한다', async ({ page }) => {
        const res1Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('columnheader', { name: '전문식별자' }).click();
        const res1 = await res1Promise;
        const url1 = new URL(res1.url());
        expect(url1.searchParams.get('sortBy')).toBe('reqIdCode');
        expect(url1.searchParams.get('sortDirection')).toBe('ASC');

        const res2Promise = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('columnheader', { name: '전문식별자' }).click();
        const res2 = await res2Promise;
        const url2 = new URL(res2.url());
        expect(url2.searchParams.get('sortDirection')).toBe('DESC');
    });

    test('엑셀 버튼을 클릭하면 엑셀 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.getByRole('button', { name: LABEL.EXCEL }).click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 표시되어야 한다', async ({ page, context, request }) => {
        // 출력에는 데이터가 필요하므로 임시 매핑 생성
        const reqIdCode = generateReqIdCode('PRINT-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            // 데이터 로드 대기
            const responsePromise = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;

            const popupPromise = context.waitForEvent('page');
            await page.getByRole('button', { name: LABEL.PRINT }).click();
            const popup = await popupPromise;

            expect(popup).toBeTruthy();

            if (!popup.isClosed()) {
                await popup.waitForLoadState('domcontentloaded');
                await popup.close();
            }
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('DETAIL-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const searchRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await searchRes;

            await page.getByRole('row').filter({ hasText: reqIdCode }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.APP_MAPPING_DETAIL_TITLE)).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
            if (await page.getByRole('dialog').isVisible()) {
                await page.getByRole('button', { name: LABEL.CLOSE }).click();
            }
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────────

test.describe('CRUD', () => {
    // ─── 등록 ─────────────────────────────────────────────────

    test('등록 버튼을 클릭하면 빈 등록 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await expect(page.getByText(LABEL.APP_MAPPING_CREATE_TITLE)).toBeVisible();

        await expect(page.locator('#modalGwId')).toBeEnabled();
        await expect(page.locator('#modalReqIdCode')).toHaveValue('');
        await expect(page.locator('#modalReqIdCode')).toBeEnabled();
        await expect(page.locator('#appMappingDeleteBtn')).not.toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(page.getByRole('dialog')).not.toBeVisible();
    });

    test('데이터를 생성하면 목록에 즉시 반영되어야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('CREATE-');
        page.on('dialog', d => d.accept());

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalGwId').selectOption({ value: gwId });
            await page.locator('#modalReqIdCode').fill(reqIdCode);
            await page.locator('#modalBizAppId').fill('e2e-biz-app');

            await page.getByRole('button', { name: LABEL.SAVE }).last().click();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const searchRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await searchRes;

            await expect(page.getByRole('cell', { name: reqIdCode })).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    test('중복된 데이터를 생성할 경우 오류 Toast가 표시되어야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('DUP-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            await page.getByRole('button', { name: LABEL.REGISTER }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalGwId').selectOption({ value: gwId });
            await page.locator('#modalReqIdCode').fill(reqIdCode);

            await page.getByRole('button', { name: LABEL.SAVE }).last().click();

            await expect(page.locator('#spToastContainer .toast.text-bg-danger')).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
            if (await page.getByRole('dialog').isVisible()) {
                await page.getByRole('button', { name: LABEL.CLOSE }).click();
            }
        }
    });

    test('필수 항목(게이트웨이·전문식별자) 없이 저장하면 오류 Toast가 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        // gwId, reqIdCode 미입력 상태로 저장 시도 → Toast.error() 호출
        await page.getByRole('button', { name: LABEL.SAVE }).last().click();
        await expect(page.locator('#spToastContainer .toast.text-bg-danger')).toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
    });

    // ─── 수정 ─────────────────────────────────────────────────

    test('행을 클릭하면 정보가 채워진 수정 모달이 열려야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('EDIT-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const searchRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await searchRes;

            await page.getByRole('row').filter({ hasText: reqIdCode }).click();
            await expect(page.getByRole('dialog')).toBeVisible();
            await expect(page.getByText(LABEL.APP_MAPPING_DETAIL_TITLE)).toBeVisible();
            await expect(page.locator('#appMappingDeleteBtn')).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
            if (await page.getByRole('dialog').isVisible()) {
                await page.getByRole('button', { name: LABEL.CLOSE }).click();
            }
        }
    });

    test('PK(게이트웨이·전문식별자)에 해당하는 필드는 수정할 수 없어야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('PK-');
        await createAppMapping(request, gwId, reqIdCode);

        try {
            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const searchRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await searchRes;

            await page.getByRole('row').filter({ hasText: reqIdCode }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await expect(page.locator('#modalGwId')).toBeDisabled();
            await expect(page.locator('#modalReqIdCode')).toBeDisabled();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
            if (await page.getByRole('dialog').isVisible()) {
                await page.getByRole('button', { name: LABEL.CLOSE }).click();
            }
        }
    });

    test('데이터를 수정하면 목록에 즉시 반영되어야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('UPD-');
        await createAppMapping(request, gwId, reqIdCode, { bizAppId: 'before-app' });
        page.on('dialog', d => d.accept());

        try {
            await page.locator('#searchReqIdCode').fill(reqIdCode);
            const searchRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await searchRes;

            await page.getByRole('row').filter({ hasText: reqIdCode }).click();
            await expect(page.getByRole('dialog')).toBeVisible();

            await page.locator('#modalBizAppId').fill('after-app');
            await page.getByRole('button', { name: LABEL.SAVE }).last().click();
            await expect(page.getByRole('dialog')).not.toBeVisible();

            const reloadRes = page.waitForResponse(r => isListResponse(r.url()));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await reloadRes;

            await expect(page.getByRole('cell', { name: 'after-app' })).toBeVisible();
        } finally {
            await deleteAppMapping(request, gwId, reqIdCode);
        }
    });

    // ─── 삭제 ─────────────────────────────────────────────────

    test('데이터를 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const reqIdCode = generateReqIdCode('DEL-');
        await createAppMapping(request, gwId, reqIdCode);

        await page.locator('#searchReqIdCode').fill(reqIdCode);
        const searchRes = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await searchRes;

        await page.getByRole('row').filter({ hasText: reqIdCode }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.locator('#appMappingDeleteBtn').click();
        await page.locator('#spConfirmModalOk').click();
        await page.locator('#spConfirmModal').waitFor({ state: 'hidden' });
        await expect(page.getByRole('dialog')).not.toBeVisible();

        const reloadRes = page.waitForResponse(r => isListResponse(r.url()));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await reloadRes;

        await expect(page.getByRole('cell', { name: reqIdCode })).not.toBeVisible();
        // 삭제 성공이므로 cleanup 불필요
    });
});

// ─── 거래조회 모달 ───────────────────────────────────────────

test.describe('거래조회 모달', () => {
    test('등록 모달에서 거래 조회 버튼 클릭 시 거래조회 모달이 열려야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.getByRole('button', { name: '조회' }).nth(1).click();

        const trxModal = page.locator('#appMappingTrxModal');
        await expect(trxModal).toBeVisible();
        await expect(page.locator('#appMappingTrxTableBody')).toBeVisible();

        await trxModal.getByRole('button', { name: LABEL.CLOSE }).click();
        await expect(trxModal).not.toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
    });

    test('거래조회 모달에서 거래 행 클릭 시 거래명이 등록 모달에 반영되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.REGISTER }).click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await page.getByRole('button', { name: '조회' }).nth(1).click();
        await expect(page.locator('#appMappingTrxModal')).toBeVisible();

        const trxRows = page.locator('#appMappingTrxTableBody tr[data-trx-id]');
        const count = await trxRows.count();

        if (count > 0) {
            const firstRow = trxRows.first();
            const trxName = (await firstRow.locator('td').nth(1).textContent())?.trim() ?? '';

            await firstRow.click();

            await expect(page.locator('#appMappingTrxModal')).not.toBeVisible();
            await expect(page.locator('#modalTrxName')).toHaveValue(trxName);
        } else {
            test.skip(true, '조회된 거래 데이터가 없어 선택 테스트를 건너뜁니다.');
        }

        if (await page.getByRole('dialog').isVisible()) {
            await page.getByRole('button', { name: LABEL.CLOSE }).click();
        }
    });
});

// ─── 권한 — 비인증 사용자 ────────────────────────────────────

test.describe('권한 — 비인증 사용자', () => {
    let ctx: APIRequestContext;

    test.beforeAll(async ({ playwright }) => {
        const baseURL = test.info().project.use.baseURL ?? 'http://localhost:8080';
        ctx = await playwright.request.newContext({
            baseURL,
            storageState: { cookies: [], origins: [] },
        });
    });

    test.afterAll(async () => {
        await ctx.dispose();
    });

    test('비인증 사용자의 등록 요청은 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.post(API_PATH, { data: { gwId: 'ANY', reqIdCode: 'ANY' } });
        expect(res.status()).toBe(401);
    });

    test('비인증 사용자의 수정 요청은 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.put(`${API_PATH}/ANY/ANY`, { data: { gwId: 'ANY', reqIdCode: 'ANY' } });
        expect(res.status()).toBe(401);
    });

    test('비인증 사용자의 삭제 요청은 HTTP 401을 반환해야 한다', async () => {
        const res = await ctx.delete(`${API_PATH}/ANY/ANY`);
        expect(res.status()).toBe(401);
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test.beforeEach(async ({ page }) => {
        // readonly 계정은 게이트웨이·기관 API 권한이 없어 페이지 로드 시 alert()이 발생할 수 있음
        page.on('dialog', d => d.accept());
        await page.goto('/transports');
        await expect(page.getByRole('table')).toBeVisible();
    });

    test('[권한] R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.getByRole('button', { name: LABEL.REGISTER })).not.toBeVisible();
    });

    test('[권한] R 권한 사용자가 행을 클릭하면 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        const rows = page.locator('#appMappingTableBody tr[data-gw-id]');
        const count = await rows.count();

        // 조회된 데이터가 없으면 모달 검증 불가 — 건너뜀
        if (count === 0) {
            test.skip();
            return;
        }

        await rows.first().click();
        await expect(page.getByRole('dialog')).toBeVisible();

        await expect(page.getByRole('button', { name: LABEL.SAVE })).not.toBeVisible();
        await expect(page.locator('#appMappingDeleteBtn')).not.toBeVisible();

        await page.getByRole('button', { name: LABEL.CLOSE }).click();
    });

    test('[권한] R 권한 사용자의 등록 API 요청은 HTTP 403을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/interface-mnt/app-mappings', {
            data: { gwId: 'ANY', reqIdCode: 'ANY' },
        });
        // 인증은 되어 있으나 쓰기 권한 없음 → 403
        expect(res.status()).toBe(403);
    });
});
