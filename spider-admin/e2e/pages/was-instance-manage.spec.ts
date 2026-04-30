/**
 * WAS 인스턴스 관리 페이지 — 목록, CRUD, 프로퍼티 섹션, 권한.
 *
 * 이 페이지는 인라인 편집 방식 (행 추가 → 인라인 입력 → 변경사항 저장)을 사용한다.
 * 삭제는 선택행 삭제 → 삭제 마킹 → 변경사항 저장 시 배치 API로 일괄 처리된다.
 * 검색 폼은 SearchForm.js 컴포넌트가 아닌 직접 HTML로 구성되어 있다.
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

// ─── 헬퍼 함수 ───────────────────────────────────────────────

function generateTestId(prefix: string): string {
    // instanceId는 최대 4자 — prefix 없이 짧은 ID 생성
    return prefix + Date.now().toString(36).slice(-3);
}

function buildCreateData(id: string, name: string) {
    return {
        instanceId: id,
        instanceName: name,
        instanceDesc: 'E2E 테스트용',
        ip: '10.0.0.1',
        port: '8080',
        instanceType: '1',
        operModeType: 'D',
    };
}

async function createInstance(request: APIRequestContext, id: string, name: string) {
    const res = await request.post('/api/was/instance', {
        data: buildCreateData(id, name),
    });
    expect(res.status()).toBe(201);
}

async function deleteInstance(request: APIRequestContext, id: string) {
    await request.delete(`/api/was/instance/${id}`);
}

async function waitForTableLoad(page: Page) {
    await page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
    await page.locator('#wasInstanceTableBody').waitFor({ state: 'visible' });
}

async function searchByName(page: Page, name: string) {
    await page.locator('#instanceName').fill(name);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
    await page.getByRole('button', { name: LABEL.SEARCH }).click();
    await responsePromise;
}

async function createWasProperty(request: APIRequestContext, instanceId: string, groupId: string, propertyId: string) {
    const res = await request.post('/api/was/property', {
        data: {
            instanceId,
            propertyGroupId: groupId,
            propertyId,
            propertyValue: 'test-value',
            propertyDesc: 'E2E 테스트',
        },
    });
    expect(res.status()).toBe(201);
}

async function deleteWasProperty(request: APIRequestContext, instanceId: string, groupId: string, propertyId: string) {
    await request.delete(`/api/was/property/${instanceId}/${groupId}/${propertyId}`);
}

// ─── 공통 setup ──────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await page.goto('/was-instances');
    await waitForTableLoad(page);
});

// ─── 목록 ────────────────────────────────────────────────────

test.describe('WAS 인스턴스 목록', () => {

    test('초기 페이지 로드 시 데이터가 10건 이하로 조회되어야 한다', async ({ page }) => {
        const rows = page.locator('#wasInstanceTableBody tr');
        const count = await rows.count();
        expect(count).toBeLessThanOrEqual(10);
    });

    test('리스트 헤더와 행 추가 버튼이 같은 줄에 표시되어야 한다', async ({ page }) => {
        const header = page.locator('.page-header').filter({ hasText: '리스트' });
        const headerBox = await header.boundingBox();
        const addBtn = page.getByRole('button', { name: '행 추가' });
        const addBtnBox = await addBtn.boundingBox();

        // 같은 줄: Y 좌표 차이가 작아야 한다
        expect(Math.abs(headerBox!.y - addBtnBox!.y)).toBeLessThan(30);
    });

    test('인스턴스명으로 검색하면 일치하는 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        const uniqueName = 'SearchWI' + id;
        await createInstance(request, id, uniqueName);

        try {
            await searchByName(page, uniqueName);
            await expect(page.getByRole('cell', { name: id, exact: true })).toBeVisible();
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('인스턴스 타입 필터를 적용하면 필터링된 결과가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'TypeFilter');

        try {
            await page.locator('#instanceTypeFilter').selectOption('1');
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
            await page.getByRole('button', { name: LABEL.SEARCH }).click();
            await responsePromise;

            await expect(page.locator('#wasInstanceTableBody tr').first()).toBeVisible();
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('컬럼 헤더를 클릭하면 오름차순→내림차순→해제 순으로 정렬이 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'SortTest');

        try {
            const sortHeader = page.locator('#wasInstanceTable thead th[data-sort="instanceId"]');

            // 1) 첫 클릭 → 오름차순 (sort-asc)
            const res1 = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
            await sortHeader.click();
            await res1;
            await expect(sortHeader).toHaveClass(/sort-asc/);

            // 2) 두 번째 클릭 → 내림차순 (sort-desc)
            const res2 = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
            await sortHeader.click();
            await res2;
            await expect(sortHeader).toHaveClass(/sort-desc/);

            // 3) 세 번째 클릭 → 정렬 해제
            const res3 = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
            await sortHeader.click();
            await res3;
            await expect(sortHeader).not.toHaveClass(/sort-asc/);
            await expect(sortHeader).not.toHaveClass(/sort-desc/);
        } finally {
            await deleteInstance(request, id);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────────

test.describe('WAS 인스턴스 CRUD', () => {

    test('행 추가 버튼을 클릭하면 편집 가능한 새 행이 추가되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: '행 추가' }).click();

        const firstRow = page.locator('#wasInstanceTableBody tr').first();
        await expect(firstRow).toBeVisible();
        await expect(firstRow).toHaveClass(/row-new/);
    });

    test('인스턴스를 API로 생성한 후 검색하면 목록에 나타나야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'CRUDCreate');

        try {
            await searchByName(page, 'CRUDCreate');
            await expect(page.getByRole('cell', { name: id })).toBeVisible();
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('인스턴스를 API로 삭제한 후 검색하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'CRUDDelete');

        await searchByName(page, 'CRUDDelete');
        await expect(page.getByRole('cell', { name: id })).toBeVisible();

        // 삭제
        await deleteInstance(request, id);

        // 재조회
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/was/instance/page'));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await responsePromise;
        await expect(page.getByRole('cell', { name: id })).not.toBeVisible();
    });

    test('선택행 삭제 시 기존 행이 삭제 마킹되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'DeleteMark');

        try {
            await searchByName(page, 'DeleteMark');

            // 체크박스 선택
            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });
            await row.locator('input[type="checkbox"]').check();

            // 선택행 삭제 클릭
            await page.getByRole('button', { name: '선택행 삭제' }).click();

            // 삭제 마킹 확인
            await expect(row).toHaveClass(/sp-row-crud-deleted/);
            await expect(row.locator('.badge')).toContainText('삭제');
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('변경사항 저장 시 배치 API가 호출되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'BatchSave');

        try {
            await searchByName(page, 'BatchSave');

            // 체크박스 선택 → 삭제 마킹
            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });
            await row.locator('input[type="checkbox"]').check();
            await page.getByRole('button', { name: '선택행 삭제' }).click();

            // 변경사항 저장 클릭 → Toast.confirm 모달이 열림
            await page.getByRole('button', { name: '변경사항 저장' }).click();

            // Toast.confirm은 Bootstrap 모달 — "확인" 버튼 클릭
            const batchPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/instance/batch') && r.request().method() === 'POST');
            await page.locator('#spConfirmModalOk').click();
            const batchRes = await batchPromise;

            expect(batchRes.status()).toBe(200);
        } finally {
            // 배치로 이미 삭제되었으므로 cleanup은 안전하게 시도
            await request.delete(`/api/was/instance/${id}`).catch(() => {});
        }
    });
});

// ─── 프로퍼티 섹션 ─────────────────────────────────────────────

test.describe('WAS 인스턴스 프로퍼티 섹션', () => {

    test('WAS 프로퍼티 버튼을 클릭하면 프로퍼티 섹션이 열려야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropOpen');

        try {
            await searchByName(page, 'PropOpen');

            // WAS 프로퍼티 버튼 클릭
            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();

            // 프로퍼티 섹션 활성화 확인
            await expect(page.locator('#propertySection')).toHaveClass(/active/);
            await expect(page.locator('#propertySectionTitle')).toContainText(id);
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티 섹션에서 페이지네이션 API가 호출되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropPage');

        try {
            await searchByName(page, 'PropPage');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });

            // /page API 호출 대기
            const pageApiPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/instance/') && r.url().includes('/page'));
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await pageApiPromise;

            await expect(page.locator('#propertySection')).toHaveClass(/active/);
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티가 있는 인스턴스의 프로퍼티 섹션에서 데이터가 표시되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropData');

        // 프로퍼티 생성 (FWK_PROPERTY에 이미 존재하는 그룹/키 필요 - 없으면 직접 생성)
        const groupId = 'TESTGRP';
        const propId = 'test.key.' + id;
        await createWasProperty(request, id, groupId, propId);

        try {
            await searchByName(page, 'PropData');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });

            const pageApiPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/instance/') && r.url().includes('/page'));
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await pageApiPromise;

            // 프로퍼티 테이블에 데이터 표시 확인
            await expect(page.locator('#propertySectionTableBody tr').first()).toBeVisible();
            await expect(page.locator('#propertySectionTableBody')).not.toContainText('등록된 프로퍼티가 없습니다');
        } finally {
            await deleteWasProperty(request, id, groupId, propId);
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티 섹션에서 닫기 버튼을 클릭하면 섹션이 닫혀야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropClose');

        try {
            await searchByName(page, 'PropClose');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await expect(page.locator('#propertySection')).toHaveClass(/active/);

            // 닫기 버튼 클릭
            await page.locator('.sp-property-section-footer').getByRole('button', { name: '닫기' }).click();
            await expect(page.locator('#propertySection')).not.toHaveClass(/active/);
        } finally {
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티 섹션에서 인라인 편집 시 CRUD 표시가 "수정"으로 변경되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropEdit');
        const groupId = 'TESTGRP';
        const propId = 'edit.key.' + id;
        await createWasProperty(request, id, groupId, propId);

        try {
            await searchByName(page, 'PropEdit');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });

            const pageApiPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/instance/') && r.url().includes('/page'));
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await pageApiPromise;

            // "설정된 값" 셀 클릭 → 인라인 편집
            const editableCell = page.locator('#propertySectionTableBody .sp-editable[data-field="propertyValue"]').first();
            await editableCell.click();

            // input 필드가 나타나야 함
            const input = editableCell.locator('input');
            await expect(input).toBeVisible();

            // 값 입력 후 blur
            await input.fill('new-value');
            await input.blur();

            // CRUD 컬럼에 "수정" 표시 확인
            const crudCell = page.locator('#propertySectionTableBody td.sp-prop-crud').first();
            await expect(crudCell).toContainText('수정');
        } finally {
            await deleteWasProperty(request, id, groupId, propId);
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티 섹션에서 변경사항 저장 시 배치 API가 호출되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropSave');
        const groupId = 'TESTGRP';
        const propId = 'save.key.' + id;
        await createWasProperty(request, id, groupId, propId);

        try {
            await searchByName(page, 'PropSave');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });

            const pageApiPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/instance/') && r.url().includes('/page'));
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await pageApiPromise;

            // 인라인 편집
            const editableCell = page.locator('#propertySectionTableBody .sp-editable[data-field="propertyValue"]').first();
            await editableCell.click();
            const input = editableCell.locator('input');
            await input.fill('batch-save-value');
            await input.blur();

            // 변경사항 저장 → batch API 호출 확인
            const batchPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/batch') && r.request().method() === 'POST');
            await page.locator('.sp-property-section-footer').getByRole('button', { name: '변경사항 저장' }).click();
            const batchRes = await batchPromise;

            expect(batchRes.status()).toBe(200);
        } finally {
            await deleteWasProperty(request, id, groupId, propId);
            await deleteInstance(request, id);
        }
    });

    test('프로퍼티 섹션에서 선택행 삭제 시 삭제 마킹되어야 한다', async ({ page, request }) => {
        const id = generateTestId('W');
        await createInstance(request, id, 'PropDel');
        const groupId = 'TESTGRP';
        const propId = 'del.key.' + id;
        await createWasProperty(request, id, groupId, propId);

        try {
            await searchByName(page, 'PropDel');

            const row = page.locator('#wasInstanceTableBody tr').filter({
                has: page.getByRole('cell', { name: id }),
            });

            const pageApiPromise = page.waitForResponse(r =>
                r.url().includes('/api/was/property/instance/') && r.url().includes('/page'));
            await row.getByRole('button', { name: 'WAS 프로퍼티' }).click();
            await pageApiPromise;

            // 프로퍼티 행 체크박스 선택
            const propRow = page.locator('#propertySectionTableBody tr').first();
            await propRow.locator('input[type="checkbox"]').check();

            // 프로퍼티 섹션 내의 "선택행삭제" 버튼 클릭 (공백 없음 — 인스턴스 "선택행 삭제"와 구분)
            await page.locator('#propertySection').getByRole('button', { name: '선택행삭제' }).click();

            // 삭제 마킹 확인
            await expect(propRow).toHaveClass(/sp-row-crud-deleted/);
            await expect(propRow.locator('td.sp-prop-crud')).toContainText('삭제');
        } finally {
            await deleteWasProperty(request, id, groupId, propId);
            await deleteInstance(request, id);
        }
    });
});

// ─── 권한 ────────────────────────────────────────────────────

test.describe('WAS 인스턴스 권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자에게는 행 추가, 선택행 삭제, 변경사항 저장 버튼이 표시되어서는 안 된다', async ({ page }) => {
        await page.goto('/was-instances');
        await page.waitForResponse(r => r.url().includes('/api/was/instance/page'));

        await expect(page.getByRole('button', { name: '행 추가' })).not.toBeVisible();
        await expect(page.getByRole('button', { name: '선택행 삭제' })).not.toBeVisible();
        await expect(page.getByRole('button', { name: '변경사항 저장' })).not.toBeVisible();
    });
});
