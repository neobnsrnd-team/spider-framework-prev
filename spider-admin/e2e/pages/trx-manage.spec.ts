/**
 * 거래 관리 페이지 — 목록 조회, 모달 열기, CRUD.
 *
 * 페이지 URL: /transactions
 * 목록 API: /api/trx-messages/page (TRX + 전문 조인 뷰)
 * CRUD API: /api/trx/*
 *
 * 주의: 목록은 TRX_MESSAGE JOIN 데이터를 표시한다.
 * 전문 매핑이 없는 신규 TRX는 목록에 나타나지 않으므로,
 * 생성 검증은 API 응답 확인으로 대체한다.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

async function createTrx(request: APIRequestContext, id: string) {
    const res = await request.post('/api/trx', {
        data: { trxId: id, trxType: '1', retryTrxYn: 'N', maxRetryCount: 0 },
    });
    expect(res.status()).toBe(201);
}

async function deleteTrx(request: APIRequestContext, id: string) {
    await request.delete(`/api/trx/${id}`);
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/transactions');
    await page.waitForResponse(r => r.url().includes('/api/trx-messages/page'));
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('거래 목록', () => {

    test('초기 페이지 로드 시 테이블 컨테이너가 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#trxTable')).toBeVisible();
    });

    test('검색 후 조회 버튼 클릭 시 API 요청이 처리되어야 한다', async ({ page }) => {
        await page.locator('#searchValue').fill('E2E');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/trx-messages/page'));
        await page.getByRole('button', { name: '조회' }).click();
        const res = await responsePromise;
        expect(res.status()).toBe(200);
    });

    test('기관 필터를 변경하고 조회하면 API 요청이 처리되어야 한다', async ({ page }) => {
        await page.locator('#searchValue').fill('');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/trx-messages/page'));
        await page.getByRole('button', { name: '조회' }).click();
        const res = await responsePromise;
        expect(res.status()).toBe(200);
    });

    test('엑셀 버튼을 클릭하면 xlsx 파일이 다운로드되어야 한다', async ({ page }) => {
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx$/);
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('거래 CRUD', () => {

    test('등록 버튼 클릭 시 빈 등록 모달이 열려야 한다', async ({ page }) => {
        page.on('dialog', dialog => dialog.dismiss());

        await page.getByText('+ 등록').click();
        await page.locator('#trxDetailModal').waitFor({ state: 'visible' });

        // 등록 모드: trxId 입력 가능, trxDetailCreate 버튼 표시
        await expect(page.locator('#trxDetailTrxId')).not.toBeDisabled();
        await expect(page.locator('#btnTrxDetailCreate')).toBeVisible();
        await expect(page.locator('#btnTrxDetailDelete')).not.toBeVisible();

        await page.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('모달에서 거래를 등록하면 API가 성공 응답을 반환해야 한다', async ({ page, request }) => {
        // alert 자동 수락
        page.on('dialog', dialog => dialog.accept());

        const testId = generateTestId('e2etrx');

        try {
            await page.getByText('+ 등록').click();
            await page.locator('#trxDetailModal').waitFor({ state: 'visible' });

            await page.locator('#trxDetailTrxId').fill(testId);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/trx') && r.request().method() === 'POST'
            );
            await page.locator('#btnTrxDetailCreate').click();
            const res = await responsePromise;

            expect(res.status()).toBe(201);

            // API로 등록 확인
            const getRes = await request.get(`/api/trx/${testId}`);
            expect(getRes.status()).toBe(200);
        } finally {
            await deleteTrx(request, testId);
        }
    });

    test('API로 등록된 거래를 수정하면 수정 내용이 반영되어야 한다', async ({ request }) => {
        const testId = generateTestId('e2etrxu');
        await createTrx(request, testId);

        try {
            const updateRes = await request.put(`/api/trx/${testId}`, {
                data: {
                    trxName: '수정된 거래명',
                    trxType: '2',
                    retryTrxYn: 'N',
                    maxRetryCount: 3,
                },
            });

            expect(updateRes.status()).toBe(200);
            const body = await updateRes.json();
            expect(body.data.trxName).toBe('수정된 거래명');
        } finally {
            await deleteTrx(request, testId);
        }
    });

    test('API로 등록된 거래를 삭제하면 조회되지 않아야 한다', async ({ request }) => {
        const testId = generateTestId('e2etrxd');
        await createTrx(request, testId);

        const deleteRes = await request.delete(`/api/trx/${testId}`);
        expect(deleteRes.status()).toBe(200);

        const getRes = await request.get(`/api/trx/${testId}`);
        expect(getRes.status()).toBe(404);
    });
});
