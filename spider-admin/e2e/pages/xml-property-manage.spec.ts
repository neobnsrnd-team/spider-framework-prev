/**
 * XML Property 관리 페이지 — 목록, CRUD, 권한.
 *
 * 이 페이지는 파일 기반 XML Property를 관리한다.
 * DB 기반이 아닌 파일 시스템 기반이므로 페이지네이션/검색 없이 파일 목록을 전체 조회한다.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

async function createFile(request: APIRequestContext, fileName: string, description?: string) {
    const res = await request.post('/api/xml-property/files', {
        data: { fileName, description: description ?? '' },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    return body.data.fileName as string; // returns full name with .properties.xml
}

async function deleteFile(request: APIRequestContext, fullFileName: string) {
    await request.delete(`/api/xml-property/files/${encodeURIComponent(fullFileName)}`);
}

function generateTestId(prefix: string): string {
    return prefix + Date.now().toString(36);
}

const DETAIL_MODAL = '#xmlPropDetailModal';

test.beforeEach(async ({ page }) => {
    await page.goto('/xml-properties');
    await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('XML Property 파일 목록', () => {

    test('초기 페이지 로드 시 파일 목록 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();
    });

    test('파일을 생성하면 목록에 표시되어야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xplist-');
        const fullName = await createFile(request, name, 'ListTest');

        try {
            // 페이지 새로고침하여 목록 갱신
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            await expect(page.locator(`text=${fullName}`)).toBeVisible();
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('Property 파일 컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xplist-');
        const fullName = await createFile(request, name, 'SortTest');

        try {
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            // 오름차순 정렬
            await page.getByRole('columnheader', { name: LABEL.XML_PROPERTY_FILE_COLUMN }).click();
            const sortIcon = page.locator('#sortIconFileName');
            await expect(sortIcon).toHaveText('↑');

            // 내림차순 정렬
            await page.getByRole('columnheader', { name: LABEL.XML_PROPERTY_FILE_COLUMN }).click();
            await expect(sortIcon).toHaveText('↓');

            // 정렬 해제
            await page.getByRole('columnheader', { name: LABEL.XML_PROPERTY_FILE_COLUMN }).click();
            await expect(sortIcon).toHaveText('⇅');
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('파일명을 클릭하면 상세 모달이 열려야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xplist-');
        const fullName = await createFile(request, name, 'SelectTest');

        try {
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await page.locator(`[role="button"][data-filename="${fullName}"]`).click();
            await responsePromise;

            await expect(page.locator(DETAIL_MODAL)).toBeVisible();
            await expect(page.locator(`${DETAIL_MODAL} .modal-title`)).toHaveText(fullName);
        } finally {
            await deleteFile(request, fullName);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('XML Property CRUD', () => {

    test('등록 버튼을 클릭하면 파일 등록 모달이 열려야 한다', async ({ page }) => {
        await page.locator('#btnAddFile').click();
        await expect(page.locator('#xmlPropAddFileModal')).toBeVisible();
        await expect(page.getByText(LABEL.XML_PROPERTY_CREATE_TITLE)).toBeVisible();

        // 필드가 비어있어야 한다
        await expect(page.locator('#xmlPropNewFileName')).toHaveValue('');
        await expect(page.locator('#xmlPropNewFileDesc')).toHaveValue('');

        await page.locator('#btnCancelAddFile').click();
        await expect(page.locator('#xmlPropAddFileModal')).not.toBeVisible();
    });

    test('모달에서 파일을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xpcrud-');
        const fullName = name + '.properties.xml';

        try {
            await page.locator('#btnAddFile').click();
            await expect(page.locator('#xmlPropAddFileModal')).toBeVisible();

            await page.locator('#xmlPropNewFileName').fill(name);
            await page.locator('#xmlPropNewFileDesc').fill('CRUD 생성 테스트');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files') && r.request().method() === 'POST');
            await page.locator('#btnConfirmAddFile').click();
            await responsePromise;

            await expect(page.locator('#xmlPropAddFileModal')).not.toBeVisible();

            // Toast 성공 메시지 확인
            await expect(page.locator('.toast')).toBeVisible();

            // 목록에 표시
            await expect(page.locator(`text=${fullName}`)).toBeVisible();
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('중복 파일을 생성하면 Toast 오류가 표시되어야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xpcrud-');
        const fullName = await createFile(request, name, 'DupTest');

        try {
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            await page.locator('#btnAddFile').click();
            await expect(page.locator('#xmlPropAddFileModal')).toBeVisible();

            await page.locator('#xmlPropNewFileName').fill(name);

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files') && r.request().method() === 'POST');
            await page.locator('#btnConfirmAddFile').click();
            await responsePromise;

            // Toast 오류 메시지가 표시되어야 한다
            await expect(page.locator('.toast')).toBeVisible();
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('상세 모달의 테이블이 모달 전체 너비를 차지해야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xpcrud-');
        const fullName = await createFile(request, name, 'WidthTest');

        try {
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            const detailPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await page.locator(`[role="button"][data-filename="${fullName}"]`).click();
            await detailPromise;
            await expect(page.locator(DETAIL_MODAL)).toBeVisible();

            // 테이블이 모달 본문 너비의 90% 이상을 차지해야 한다
            const modalBodyWidth = await page.locator(`${DETAIL_MODAL} .modal-body`).evaluate(el => el.clientWidth);
            const tableWidth = await page.locator(`${DETAIL_MODAL} .sp-data-grid`).evaluate(el => el.clientWidth);
            expect(tableWidth).toBeGreaterThan(modalBodyWidth * 0.9);
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('파일을 선택하고 항목을 추가하여 저장하면 반영되어야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xpcrud-');
        const fullName = await createFile(request, name, 'EntryTest');

        try {
            await page.goto('/xml-properties');
            await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();

            // 파일 선택 → 상세 모달 열기
            const detailPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await page.locator(`[role="button"][data-filename="${fullName}"]`).click();
            await detailPromise;
            await expect(page.locator(DETAIL_MODAL)).toBeVisible();

            // 항목 추가
            await page.locator('[data-table-add="entries"]').click();
            const newRow = page.locator(`${DETAIL_MODAL} tbody tr`).last();
            await newRow.locator('.entry-key').fill('testKey');
            await newRow.locator('.entry-value').fill('testValue');
            await newRow.locator('.entry-desc').fill('testDesc');

            // 저장
            const savePromise = page.waitForResponse(r =>
                r.url().includes('/entries') && r.request().method() === 'PUT');
            await page.locator(`${DETAIL_MODAL} [data-action="save"]`).click();
            await savePromise;

            // Toast 성공 메시지 + 모달 닫힘
            await expect(page.locator('.toast')).toBeVisible();
            await expect(page.locator(DETAIL_MODAL)).not.toBeVisible();

            // 다시 파일 선택하여 저장된 항목 확인
            const verifyPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await page.locator(`[role="button"][data-filename="${fullName}"]`).click();
            await verifyPromise;
            await expect(page.locator(DETAIL_MODAL)).toBeVisible();

            await expect(page.locator(`${DETAIL_MODAL} .entry-key`).first()).toHaveValue('testKey');
            await expect(page.locator(`${DETAIL_MODAL} .entry-value`).first()).toHaveValue('testValue');
        } finally {
            await deleteFile(request, fullName);
        }
    });

    test('파일 삭제 버튼을 클릭하면 삭제 확인 모달이 열리고 삭제 후 목록에서 사라져야 한다', async ({ page, request }) => {
        const name = generateTestId('e2e-xpcrud-');
        const fullName = await createFile(request, name, 'DeleteTest');

        await page.goto('/xml-properties');
        await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();
        await expect(page.locator(`text=${fullName}`)).toBeVisible();

        // 삭제 버튼 클릭
        const row = page.locator(`tr[data-filename="${fullName}"]`);
        await row.locator('.btn-table-delete').click();

        // 삭제 확인 모달
        await expect(page.locator('#xmlPropDeleteConfirmModal')).toBeVisible();
        await expect(page.locator('#xmlPropDeleteConfirmMsg')).toContainText(fullName);

        // 삭제 확인
        const deletePromise = page.waitForResponse(r =>
            r.url().includes('/api/xml-property/files/') && r.request().method() === 'DELETE');
        await page.locator('#btnConfirmDelete').click();
        await deletePromise;

        await expect(page.locator('#xmlPropDeleteConfirmModal')).not.toBeVisible();
        await expect(page.locator(`text=${fullName}`)).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });

    test('빈 파일명으로 등록 시 Toast 경고가 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnAddFile').click();
        await expect(page.locator('#xmlPropAddFileModal')).toBeVisible();

        // 파일명을 비우고 생성 시도
        await page.locator('#btnConfirmAddFile').click();

        // Toast 경고 메시지
        await expect(page.locator('.toast')).toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 등록 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await expect(page.locator('#xmlPropFileTableBody')).toBeVisible();
        await expect(page.locator('#btnAddFile')).not.toBeVisible();
    });

    test('R 권한 사용자에게는 파일 목록의 삭제 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const name = generateTestId('e2e-xpro-');
        const fullName = await createFile(adminRequest, name, 'RODeleteTest');

        try {
            const listPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files') && !r.url().includes('/files/') && r.request().method() === 'GET');
            await page.goto('/xml-properties');
            await listPromise;

            const row = page.locator(`tr[data-filename="${fullName}"]`);
            await expect(row).toBeVisible();
            await expect(row.locator('.btn-table-delete')).not.toBeVisible();
        } finally {
            await deleteFile(adminRequest, fullName);
            await adminRequest.dispose();
        }
    });

    test('R 권한 사용자가 파일을 선택하면 저장 버튼이 표시되어서는 안 된다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const name = generateTestId('e2e-xpro-');
        const fullName = await createFile(adminRequest, name, 'ROEntryTest');

        try {
            const listPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files') && !r.url().includes('/files/') && r.request().method() === 'GET');
            await page.goto('/xml-properties');
            await listPromise;

            const fileLink = page.locator(`[role="button"][data-filename="${fullName}"]`);
            await expect(fileLink).toBeVisible();

            const detailPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await fileLink.click();
            await detailPromise;

            await expect(page.locator(DETAIL_MODAL)).toBeVisible();
            // buttons: false → 저장/삭제 버튼 미렌더링
            await expect(page.locator(`${DETAIL_MODAL} [data-action="save"]`)).not.toBeVisible();
        } finally {
            await deleteFile(adminRequest, fullName);
            await adminRequest.dispose();
        }
    });

    test('R 권한 사용자가 파일을 선택하면 항목 입력 필드가 읽기전용이어야 한다', async ({ page, playwright }) => {
        const adminRequest = await playwright.request.newContext({
            storageState: 'e2e/.auth/session.json',
            baseURL: 'http://localhost:8080',
        });
        const name = generateTestId('e2e-xpro-');
        const fullName = await createFile(adminRequest, name, 'ROReadonlyTest');

        // 항목을 추가한 후 저장
        await adminRequest.put(`/api/xml-property/files/${encodeURIComponent(fullName)}/entries`, {
            data: {
                fileName: fullName,
                description: 'ReadonlyDesc',
                entries: [{ key: 'roKey', value: 'roValue', description: 'roDesc' }],
            },
        });

        try {
            const listPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files') && !r.url().includes('/files/') && r.request().method() === 'GET');
            await page.goto('/xml-properties');
            await listPromise;

            const fileLink = page.locator(`[role="button"][data-filename="${fullName}"]`);
            await expect(fileLink).toBeVisible();

            const detailPromise = page.waitForResponse(r =>
                r.url().includes('/api/xml-property/files/') && r.request().method() === 'GET');
            await fileLink.click();
            await detailPromise;

            await expect(page.locator(DETAIL_MODAL)).toBeVisible();

            // 입력 필드가 readonly여야 한다
            const keyInput = page.locator(`${DETAIL_MODAL} .entry-key`).first();
            await expect(keyInput).toHaveAttribute('readonly');

            // 행 추가 버튼이 없어야 한다
            await expect(page.locator(`${DETAIL_MODAL} [data-table-add="entries"]`)).not.toBeVisible();

            // 행 삭제 버튼이 없어야 한다
            await expect(page.locator(`${DETAIL_MODAL} [data-table-row-delete]`)).not.toBeVisible();
        } finally {
            await deleteFile(adminRequest, fullName);
            await adminRequest.dispose();
        }
    });
});
