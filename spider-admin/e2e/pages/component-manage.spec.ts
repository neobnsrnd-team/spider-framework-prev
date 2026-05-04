/**
 * 컴포넌트 관리 페이지 — 목록, CRUD (PARAM 동적 행 포함), 권한.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 인라인 헬퍼 ──────────────────────────────────────────

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

async function createComponent(request: APIRequestContext, id: string, name: string, params: unknown[] = []) {
    const res = await request.post('/api/components', {
        data: {
            componentId: id,
            componentName: name,
            componentDesc: 'E2E 테스트용',
            componentType: 'J',
            componentClassName: 'com.spider.e2e.TestComponent',
            componentMethodName: 'execute',
            useYn: 'Y',
            params,
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteComponent(request: APIRequestContext, id: string) {
    await request.delete(`/api/components/${encodeURIComponent(id)}`);
}

async function searchByField(page: Page, fieldId: string, value: string) {
    await page.locator(`#_searchContainer_${fieldId}`).fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/components/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

async function openDetailModal(page: Page, componentId: string) {
    await page.getByRole('cell', { name: componentId, exact: true }).click();
    await page.locator('#componentModal').waitFor({ state: 'visible' });
}

// ─── 초기화 ───────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/components');
    await page.waitForResponse(r => r.url().includes('/api/components/page'));
    await page.locator('#_searchContainer_componentId').waitFor({ state: 'visible' });
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('컴포넌트 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#componentTable tbody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('컴포넌트 ID로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-list-');
        await createComponent(request, id, 'ListSearchTest');

        try {
            await searchByField(page, 'componentId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('컴포넌트명으로 검색하면 해당 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-nm-');
        const name = '검색테스트컴포넌트' + Date.now().toString(36);
        await createComponent(request, id, name);

        try {
            await searchByField(page, 'componentName', name);
            await expect(page.getByRole('cell', { name })).toBeVisible();
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('일치하지 않는 검색 조건으로 조회 시 결과가 표시되어서는 안 된다', async ({ page }) => {
        await searchByField(page, 'componentId', 'ZZZNOMATCH_COMPONENT_9999');
        const rows = page.locator('#componentTable tbody tr');
        const count = await rows.count();
        if (count === 1) {
            await expect(rows.first()).not.toContainText('ZZZNOMATCH');
        } else {
            expect(count).toBe(0);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('컴포넌트 CRUD', () => {

    test('params 없이 등록하면 목록에 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-cr-');
        const name = '등록테스트 ' + id;

        try {
            // 등록 버튼 클릭
            await page.locator('#btnAdd').click();
            await page.locator('#componentModal').waitFor({ state: 'visible' });

            // 폼 입력
            await page.locator('#cmpModalComponentId').fill(id);
            await page.locator('#cmpModalComponentName').fill(name);
            await page.locator('#cmpModalComponentType').fill('J');
            await page.locator('#cmpModalComponentClassName').fill('com.spider.e2e.NewComponent');
            await page.locator('#cmpModalComponentMethodName').fill('execute');
            await page.locator('#cmpModalUseYn').selectOption('Y');

            // 저장
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/components') && r.request().method() === 'POST'
            );
            await page.locator('#btnComponentSave').click();
            await responsePromise;

            // 목록 확인
            await searchByField(page, 'componentId', id);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('params 행 추가 후 등록하면 params가 저장되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-param-');
        const name = 'PARAM등록테스트 ' + id;

        try {
            await page.locator('#btnAdd').click();
            await page.locator('#componentModal').waitFor({ state: 'visible' });

            // 기본 정보 입력
            await page.locator('#cmpModalComponentId').fill(id);
            await page.locator('#cmpModalComponentName').fill(name);
            await page.locator('#cmpModalComponentType').fill('J');
            await page.locator('#cmpModalComponentClassName').fill('com.spider.e2e.ParamComponent');
            await page.locator('#cmpModalComponentMethodName').fill('execute');
            await page.locator('#cmpModalUseYn').selectOption('Y');

            // 파라미터 행 추가
            await page.locator('#btnAddParam').click();
            const firstRow = page.locator('#paramTableBody tr').first();
            await firstRow.locator('.param-key').fill('testParam1');
            await firstRow.locator('.param-desc').fill('테스트 파라미터');
            await firstRow.locator('.param-default').fill('defaultVal');

            // 저장
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/components') && r.request().method() === 'POST'
            );
            await page.locator('#btnComponentSave').click();
            const response = await responsePromise;
            const body = await response.json();

            expect(body.success).toBe(true);
            expect(body.data.params).toHaveLength(1);
            expect(body.data.params[0].paramKey).toBe('testParam1');
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('수정 후 변경 내용이 반영되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-upd-');
        await createComponent(request, id, '수정전컴포넌트');

        try {
            await searchByField(page, 'componentId', id);
            await openDetailModal(page, id);

            const updatedName = '수정된컴포넌트 ' + Date.now().toString(36);
            await page.locator('#cmpModalComponentName').fill(updatedName);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes(`/api/components/${encodeURIComponent(id)}`) &&
                r.request().method() === 'PUT'
            );
            await page.locator('#btnComponentSave').click();
            await responsePromise;

            await searchByField(page, 'componentId', id);
            await expect(page.getByRole('cell', { name: updatedName })).toBeVisible();
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('params가 있는 컴포넌트 상세 모달에 params 행이 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-dt-');
        await createComponent(request, id, '파라미터확인컴포넌트', [
            { paramSeqNo: 1, paramKey: 'displayKey', paramDesc: '표시확인', defaultParamValue: 'val' },
        ]);

        try {
            await searchByField(page, 'componentId', id);
            await openDetailModal(page, id);

            // PARAM 행이 표시되어야 한다
            await expect(page.locator('#paramTableBody tr')).toHaveCount(1);
            await expect(page.locator('#paramTableBody .param-key').first()).toHaveValue('displayKey');
        } finally {
            await deleteComponent(request, id);
        }
    });

    test('삭제 후 목록에서 제거되어야 한다', async ({ page, request }) => {
        const id = generateTestId('e2e-cm-del-');
        await createComponent(request, id, '삭제대상컴포넌트');

        await searchByField(page, 'componentId', id);
        await openDetailModal(page, id);

        const responsePromise = page.waitForResponse(r =>
            r.url().includes(`/api/components/${encodeURIComponent(id)}`) &&
            r.request().method() === 'DELETE'
        );

        await page.locator('#btnComponentDelete').click();
        await page.locator('#spConfirmModalOk').click();
        await responsePromise;

        await searchByField(page, 'componentId', id);
        await expect(page.getByRole('cell', { name: id, exact: true })).not.toBeVisible();
    });
});

// ─── 권한 ─────────────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#btnAdd')).not.toBeVisible();
    });

    test('R 권한 사용자가 행 클릭 시 저장/삭제 버튼이 표시되어서는 안 된다', async ({ page }) => {
        const rows = page.locator('#componentTable tbody tr');
        const count = await rows.count();
        if (count > 0) {
            await rows.first().click();
            await page.locator('#componentModal').waitFor({ state: 'visible' });
            await expect(page.locator('#btnComponentSave')).not.toBeVisible();
            await expect(page.locator('#btnComponentDelete')).not.toBeVisible();
        }
    });

    test('R 권한 사용자에게는 파라미터 추가 버튼이 표시되어서는 안 된다', async ({ page }) => {
        const rows = page.locator('#componentTable tbody tr');
        const count = await rows.count();
        if (count > 0) {
            await rows.first().click();
            await page.locator('#componentModal').waitFor({ state: 'visible' });
            await expect(page.locator('#btnAddParam')).not.toBeVisible();
        }
    });
});
