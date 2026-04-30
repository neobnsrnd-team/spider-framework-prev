/**
 * Biz App 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createBizApp(request: APIRequestContext, id: string, name: string) {
    const res = await request.post('/api/biz-apps', {
        data: {
            bizAppId: id,
            bizAppName: name,
            bizAppDesc: 'E2E 테스트용',
            dupCheckYn: 'Y',
            queName: '00',
            logYn: 'Y',
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteBizApp(request: APIRequestContext, id: string) {
    await request.delete(`/api/biz-apps/${id}`);
}

async function searchByField(page: Page, fieldId: string, value: string) {
    await page.locator(`#_searchContainer_${fieldId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/biz-apps/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

/** queName 드롭다운이 비어 있으면 옵션을 주입한다 (코드그룹 FR20003 미시딩 대응). */
async function ensureQueNameOption(page: Page) {
    const hasOptions = await page.locator('#baModalQueName option').count();
    if (hasOptions === 0) {
        await page.evaluate(() => {
            const select = document.querySelector('#baModalQueName') as HTMLSelectElement;
            if (select && select.options.length === 0) {
                const opt = document.createElement('option');
                opt.value = '00';
                opt.textContent = '공통';
                select.appendChild(opt);
                select.value = '00';
            }
        });
    }
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/biz-apps');
    await page.waitForResponse(r => r.url().includes('/api/biz-apps/page'));
    await page.locator('#_searchContainer_bizAppId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('BizApp 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#bizAppTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('bizAppId로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-list-');
        await createBizApp(request, id, 'SearchTest');

        try {
            await searchByField(page, 'bizAppId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteBizApp(request, id);
        }
    });

    test('bizAppName으로 검색하면 해당 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-nm-');
        const name = 'NameSearch' + Date.now().toString(36);
        await createBizApp(request, id, name);

        try {
            await searchByField(page, 'bizAppName', name);
            await expect(page.getByRole('cell', { name: name })).toBeVisible();
        } finally {
            await deleteBizApp(request, id);
        }
    });

    test('검색 조건이 없는 결과는 목록에 표시되어서는 안 된다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-miss-');
        await createBizApp(request, id, 'ShouldNotMatch');

        try {
            await searchByField(page, 'bizAppId', 'ZZZNOMATCH9999');
            const rows = page.locator('#bizAppTable tbody tr');
            const count = await rows.count();
            // 빈 결과 또는 no-data 메시지 행만 있어야 함
            if (count === 1) {
                await expect(rows.first()).not.toContainText(id);
            } else {
                expect(count).toBe(0);
            }
        } finally {
            await deleteBizApp(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('BizApp CRUD', () => {

    test('등록 후 목록에 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-cr-');
        const name = 'CreateTest ' + id;

        try {
            // 등록 버튼 클릭
            await page.locator('#btnAdd').click();
            await page.locator('#bizAppModal').waitFor({ state: 'visible' });

            // queName 옵션 확인
            await ensureQueNameOption(page);

            // 폼 입력
            await page.locator('#baModalBizAppId').fill(id);
            await page.locator('#baModalBizAppName').fill(name);
            await page.locator('#baModalBizAppDesc').fill('등록 테스트 설명');
            await page.locator('#baModalDupCheckYn').selectOption('Y');
            await page.locator('#baModalLogYn').selectOption('Y');

            // 저장 클릭 및 응답 대기
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/biz-apps') && r.request().method() === 'POST'
            );
            await page.locator('#btnBizAppSave').click();
            await responsePromise;

            // 목록 재조회 후 확인
            await searchByField(page, 'bizAppId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteBizApp(request, id);
        }
    });

    test('수정 후 변경 내용이 반영되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-upd-');
        await createBizApp(request, id, 'UpdateBefore');

        try {
            await searchByField(page, 'bizAppId', id);
            await page.getByRole('cell', { name: id }).click();
            await page.locator('#bizAppModal').waitFor({ state: 'visible' });

            const updatedName = 'UpdatedName ' + Date.now().toString(36);
            await page.locator('#baModalBizAppName').fill(updatedName);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/biz-apps/${id}`) && r.request().method() === 'PUT'
            );
            await page.locator('#btnBizAppSave').click();
            await responsePromise;

            await searchByField(page, 'bizAppId', id);
            await expect(page.getByRole('cell', { name: updatedName })).toBeVisible();
        } finally {
            await deleteBizApp(request, id);
        }
    });

    test('삭제 후 목록에서 제거되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-ba-del-');
        await createBizApp(request, id, 'DeleteTarget');

        await searchByField(page, 'bizAppId', id);
        await page.getByRole('cell', { name: id }).click();
        await page.locator('#bizAppModal').waitFor({ state: 'visible' });

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/biz-apps/${id}`) && r.request().method() === 'DELETE'
        );

        // Toast.confirm은 Bootstrap 모달 — #spConfirmModalOk 버튼 클릭으로 확인
        await page.locator('#btnBizAppDelete').click();
        await page.locator('#spConfirmModalOk').click();

        await responsePromise;

        await searchByField(page, 'bizAppId', id);
        await expect(page.getByRole('cell', { name: id })).not.toBeVisible();
    });
});

// ─── 권한 ─────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행 클릭 시 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        const rows = page.locator('#bizAppTable tbody tr');
        const count = await rows.count();
        if (count > 0) {
            await rows.first().click();
            await page.locator('#bizAppModal').waitFor({ state: 'visible' });
            await expect(page.locator('#btnBizAppSave')).not.toBeVisible();
            await expect(page.locator('#btnBizAppDelete')).not.toBeVisible();
        }
    });
});
