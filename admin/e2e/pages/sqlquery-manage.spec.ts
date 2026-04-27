/**
 * SQL Query 관리 페이지 — 목록, CRUD, 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createSqlQuery(request: APIRequestContext, id: string, name: string) {
    const res = await request.post('/api/sql-queries', {
        data: {
            queryId: id,
            queryName: name,
            sqlGroupId: '',
            dbId: 'e2e-ds-001',
            sqlType: 'SELECT',
            execType: 'SYNC',
            cacheYn: 'N',
            timeOut: '30',
            resultType: 'MAP',
            useYn: 'Y',
            sqlQuery: 'SELECT 1 FROM DUAL',
            queryDesc: 'E2E 테스트용',
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteSqlQuery(request: APIRequestContext, id: string) {
    await request.delete(`/api/sql-queries/${id}`);
}

async function searchByField(page: Page, fieldId: string, value: string) {
    await page.locator(`#_searchContainer_${fieldId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/sql-queries/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await page.goto('/sql-queries');
    await page.waitForResponse(r => r.url().includes('/api/sql-queries/page'));
    await page.locator('#_searchContainer_queryId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('SQL Query 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#sqlQueryTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('queryId로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-list-');
        await createSqlQuery(request, id, 'SearchTest');

        try {
            await searchByField(page, 'queryId', id);
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteSqlQuery(request, id);
        }
    });

    test('queryName으로 검색하면 해당 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-nm-');
        const name = 'NameSearch' + Date.now().toString(36);
        await createSqlQuery(request, id, name);

        try {
            await searchByField(page, 'queryName', name);
            await expect(page.getByRole('cell', { name: name })).toBeVisible();
        } finally {
            await deleteSqlQuery(request, id);
        }
    });

    test('검색 조건이 없는 결과는 목록에 표시되어서는 안 된다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-miss-');
        await createSqlQuery(request, id, 'ShouldNotMatch');

        try {
            await searchByField(page, 'queryId', 'ZZZNOMATCH9999');
            const rows = page.locator('#sqlQueryTable tbody tr');
            const count = await rows.count();
            if (count === 1) {
                await expect(rows.first()).not.toContainText(id);
            } else {
                expect(count).toBe(0);
            }
        } finally {
            await deleteSqlQuery(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('SQL Query CRUD', () => {

    test('등록 후 목록에 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-cr-');
        const name = 'CreateTest ' + id;

        try {
            // 등록 버튼 클릭
            await page.locator('#btnAdd').click();
            await page.locator('#sqlQueryModal').waitFor({ state: 'visible' });

            // 폼 입력
            await page.locator('#sqModalQueryId').fill(id);
            await page.locator('#sqModalQueryName').fill(name);
            await page.locator('#sqModalDbId').fill('e2e-ds-001');
            await page.locator('#sqModalSqlType').selectOption('R');
            await page.locator('#sqModalExecType').selectOption('O');
            await page.locator('#sqModalTimeOut').fill('30');
            await page.locator('#sqModalResultType').fill('MAP');
            await page.locator('#sqModalCacheYn').selectOption('N');
            await page.locator('#sqModalUseYn').selectOption('Y');
            await page.locator('#sqModalSqlQuery').fill('SELECT 1 FROM DUAL');

            // 저장 클릭 및 응답 대기
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/sql-queries') && r.request().method() === 'POST'
            );
            await page.locator('#btnSqlQuerySave').click();
            await responsePromise;

            // 목록 재조회 후 확인
            await searchByField(page, 'queryId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteSqlQuery(request, id);
        }
    });

    test('수정 후 변경 내용이 반영되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-upd-');
        await createSqlQuery(request, id, 'UpdateBefore');

        try {
            await searchByField(page, 'queryId', id);
            await page.getByRole('cell', { name: id }).click();
            await page.locator('#sqlQueryModal').waitFor({ state: 'visible' });

            const updatedName = 'UpdatedName ' + Date.now().toString(36);
            await page.locator('#sqModalQueryName').fill(updatedName);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/sql-queries/${encodeURIComponent(id)}`) && r.request().method() === 'PUT'
            );
            await page.locator('#btnSqlQuerySave').click();
            await responsePromise;

            await searchByField(page, 'queryId', id);
            await expect(page.getByRole('cell', { name: updatedName })).toBeVisible();
        } finally {
            await deleteSqlQuery(request, id);
        }
    });

    test('삭제 후 목록에서 제거되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-sq-del-');
        await createSqlQuery(request, id, 'DeleteTarget');

        await searchByField(page, 'queryId', id);
        await page.getByRole('cell', { name: id }).click();
        await page.locator('#sqlQueryModal').waitFor({ state: 'visible' });

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/sql-queries/${encodeURIComponent(id)}`) && r.request().method() === 'DELETE'
        );

        await page.locator('#btnSqlQueryDelete').click();
        await page.locator('#spConfirmModalOk').click();

        await responsePromise;

        await searchByField(page, 'queryId', id);
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
        const rows = page.locator('#sqlQueryTable tbody tr');
        const count = await rows.count();
        if (count > 0) {
            await rows.first().click();
            await page.locator('#sqlQueryModal').waitFor({ state: 'visible' });
            await expect(page.locator('#btnSqlQuerySave')).not.toBeVisible();
            await expect(page.locator('#btnSqlQueryDelete')).not.toBeVisible();
        }
    });
});
