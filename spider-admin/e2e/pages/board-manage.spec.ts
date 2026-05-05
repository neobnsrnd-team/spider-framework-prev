/**
 * 게시판 관리 페이지 — 목록, CRUD, 정렬, 엑셀, 출력.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 * e2e-admin 계정에 BOARD_MANAGEMENT:W 권한이 시드되어 있다 (e2e-seed.sql).
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';
import { ADMIN } from '../fixtures/test-accounts';

// ─── 헬퍼 ────────────────────────────────────────────────

/** 고유한 게시판 ID를 생성한다 (영문/숫자/하이픈, 최대 20자). */
function generateBoardId(prefix: string): string {
    return (prefix + Date.now().toString(36)).slice(0, 20);
}

/** API로 테스트 게시판을 생성한다. */
async function createBoard(
    request: APIRequestContext,
    boardId: string,
    boardName: string,
    boardType: string = 'N',
): Promise<void> {
    const res = await request.post('/api/boards', {
        data: { boardId, boardName, boardType, adminId: null },
    });
    expect(res.status()).toBe(201);
}

/** API로 테스트 게시판을 삭제한다 (이미 없으면 무시). */
async function deleteBoard(request: APIRequestContext, boardId: string): Promise<void> {
    await request.delete(`/api/boards/${encodeURIComponent(boardId)}`);
}

/** API로 게시판 권한을 생성한다. */
async function createBoardAuth(
    request: APIRequestContext,
    userId: string,
    boardId: string,
    authCode: string,
): Promise<void> {
    const res = await request.post('/api/board-auth', {
        data: { userId, boardId, authCode },
    });
    expect(res.status()).toBe(201);
}

/** API로 게시판 권한을 삭제한다. 이미 없으면 무시한다. */
async function deleteBoardAuth(
    request: APIRequestContext,
    userId: string,
    boardId: string,
): Promise<void> {
    await request.delete(`/api/board-auth/${encodeURIComponent(userId)}/${encodeURIComponent(boardId)}`);
}

/** 게시판 관리 페이지로 이동하고 첫 API 로드를 대기한다. */
async function navigateToBoardManagePage(page: Page): Promise<void> {
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/boards/page'));
    await page.goto('/boards');
    await responsePromise;
    await expect(page.locator('#boardTable')).toBeVisible();
}

/** 검색 필드/값으로 게시판을 검색하고 결과를 대기한다. */
async function searchBoard(page: Page, field: string, value: string): Promise<void> {
    await page.locator('#searchField').selectOption(field);
    await page.locator('#searchValue').fill(value);
    const responsePromise = page.waitForResponse(r => r.url().includes('/api/boards/page'));
    await page.locator('button:has-text("조회")').click();
    await responsePromise;
}

// ─── 설정 ────────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await navigateToBoardManagePage(page);
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('게시판 목록', () => {

    test('초기 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#boardTable')).toBeVisible();
        await expect(page.locator('#boardTableBody')).toBeVisible();
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const boardId = generateBoardId('e2e-srch-');
        const boardName = 'SearchTest' + Date.now().toString(36);
        await createBoard(request, boardId, boardName);

        try {
            // 게시판 ID로 검색
            await searchBoard(page, 'boardId', boardId);
            await expect(page.locator('#boardTableBody').getByText(boardId)).toBeVisible();

            // 게시판명으로 검색
            await searchBoard(page, 'boardName', boardName);
            await expect(page.locator('#boardTableBody').getByText(boardName)).toBeVisible();

            // 존재하지 않는 값으로 검색
            await searchBoard(page, 'boardId', 'zzz-nonexistent-99999');
            await expect(page.locator('#boardTableBody').getByText(LABEL.NO_DATA)).toBeVisible();
        } finally {
            await deleteBoard(request, boardId);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        const boardIdTh = page.locator('th[data-sort="boardId"]');

        // 클릭하여 ASC 정렬
        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/boards/page') && r.url().includes('sortBy=boardId'),
        );
        await boardIdTh.click();
        await responsePromise;
        await expect(boardIdTh).toHaveClass(/sort-asc/);

        // 다시 클릭하여 DESC 정렬
        const responsePromise2 = page.waitForResponse(r =>
            r.url().includes('sortDirection=DESC'),
        );
        await boardIdTh.click();
        await responsePromise2;
        await expect(boardIdTh).toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 CSV 파일이 다운로드되어야 한다', async ({ page, request }) => {
        const boardId = generateBoardId('e2e-xls-');
        await createBoard(request, boardId, 'ExcelTest');

        try {
            // 데이터 리로드
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/boards/page'));
            await page.goto('/boards');
            await responsePromise;

            const downloadPromise = page.waitForEvent('download');
            await page.locator('#btnExcel').click();
            const download = await downloadPromise;

            expect(download.suggestedFilename()).toMatch(/\.csv$/);
        } finally {
            await deleteBoard(request, boardId);
        }
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, request, context }) => {
        const boardId = generateBoardId('e2e-prt-');
        await createBoard(request, boardId, 'PrintTest');

        try {
            // 데이터 리로드
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/boards/page'));
            await page.goto('/boards');
            await responsePromise;

            // window.print()가 팝업을 즉시 닫는 것을 방지
            await context.addInitScript(() => { window.print = () => {}; });

            const popupPromise = context.waitForEvent('page');
            await page.locator('#btnPrint').click();
            const popup = await popupPromise;

            await popup.waitForLoadState('domcontentloaded');
            await expect(popup.locator('table')).toBeVisible();
            await popup.close();
        } finally {
            await deleteBoard(request, boardId);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('게시판 CRUD', () => {

    test('등록 버튼을 클릭하면 등록 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnRegister').click();

        await expect(page.locator('#boardMgmtModal')).toBeVisible();
        await expect(page.locator('#boardMgmtModalTitle')).toHaveText('게시판 등록');

        // 빈 폼 확인
        await expect(page.locator('#modalBoardId')).toHaveValue('');
        await expect(page.locator('#modalBoardName')).toHaveValue('');
        await expect(page.locator('#modalBoardType')).toHaveValue('');
        // 생성 모드: boardId 편집 가능
        await expect(page.locator('#modalBoardId')).toBeEnabled();
        // 생성 모드: 삭제 버튼 숨김
        await expect(page.locator('#btnDeleteBoard')).not.toBeVisible();
        // 카테고리 섹션 숨김
        await expect(page.locator('#categorySectionWrapper')).not.toBeVisible();

        // 닫기
        await page.locator('#boardMgmtModal .modal-footer button[data-bs-dismiss="modal"]').click();
        await expect(page.locator('#boardMgmtModal')).not.toBeVisible();
    });

    test('모달에서 게시판을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const boardId = generateBoardId('e2e-crt-');
        const boardName = 'CreateTest' + Date.now().toString(36);

        page.on('dialog', d => d.accept());

        try {
            await page.locator('#btnRegister').click();
            await expect(page.locator('#boardMgmtModal')).toBeVisible();

            await page.locator('#modalBoardId').fill(boardId);
            await page.locator('#modalBoardName').fill(boardName);
            await page.locator('#modalBoardType').selectOption('N');

            // 저장
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/boards') && r.request().method() === 'POST',
            );
            await page.locator('#boardMgmtModal .btn-primary:has-text("저장")').click();
            const response = await responsePromise;
            expect(response.status()).toBe(201);

            // 모달 닫힘 확인
            await expect(page.locator('#boardMgmtModal')).not.toBeVisible();

            // 목록에서 검색하여 확인
            await searchBoard(page, 'boardId', boardId);
            await expect(page.locator('#boardTableBody').getByText(boardId)).toBeVisible();
            await expect(page.locator('#boardTableBody').getByText(boardName)).toBeVisible();
        } finally {
            await deleteBoard(request, boardId);
        }
    });

    test('필수 항목 없이 저장하면 유효성 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnRegister').click();
        await expect(page.locator('#boardMgmtModal')).toBeVisible();

        // 필수 값(게시판 ID, 게시판명, 유형) 비우고 저장
        await page.locator('#boardMgmtModal .btn-primary:has-text("저장")').click();

        // 게시판 ID 필수 알림
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('게시판 ID');

        await page.locator('#boardMgmtModal .modal-footer button[data-bs-dismiss="modal"]').click();
    });

    test('행을 클릭하면 수정 모달이 열리고 편집할 수 있어야 한다', async ({ page, request }) => {
        const boardId = generateBoardId('e2e-edt-');
        const boardName = 'EditBefore';
        await createBoard(request, boardId, boardName, 'N');
        await createBoardAuth(request, ADMIN.userId, boardId, 'W');

        page.on('dialog', d => d.accept());

        try {
            // 검색하여 해당 게시판 찾기
            await searchBoard(page, 'boardId', boardId);

            // 행 클릭하여 수정 모달 열기 (board-auth 확인 + detail 로드)
            const detailResponse = page.waitForResponse(r =>
                r.url().includes(`/api/boards/${boardId}`) && !r.url().includes('/page'),
            );
            await page.locator('#boardTableBody tr').first().click();
            await detailResponse;

            await expect(page.locator('#boardMgmtModal')).toBeVisible();
            await expect(page.locator('#boardMgmtModalTitle')).toHaveText('게시판 수정');

            // boardId는 수정 모드에서 비활성화
            await expect(page.locator('#modalBoardId')).toBeDisabled();
            await expect(page.locator('#modalBoardId')).toHaveValue(boardId);

            // 삭제 버튼 표시
            await expect(page.locator('#btnDeleteBoard')).toBeVisible();

            // 카테고리 섹션 표시
            await expect(page.locator('#categorySectionWrapper')).toBeVisible();

            // 게시판명 수정
            const newName = 'EditAfter';
            await page.locator('#modalBoardName').fill(newName);

            // 저장
            const updateResponse = page.waitForResponse(r =>
                r.url().includes(`/api/boards/${boardId}`) && r.request().method() === 'PUT',
            );
            await page.locator('#boardMgmtModal .btn-primary:has-text("저장")').click();
            await updateResponse;

            await expect(page.locator('#boardMgmtModal')).not.toBeVisible();

            // 재검색하여 수정 확인
            await searchBoard(page, 'boardId', boardId);
            await expect(page.locator('#boardTableBody').getByText(newName)).toBeVisible();
        } finally {
            await deleteBoardAuth(request, ADMIN.userId, boardId);
            await deleteBoard(request, boardId);
        }
    });

    test('게시판을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const boardId = generateBoardId('e2e-del-');
        const boardName = 'DeleteMe';
        await createBoard(request, boardId, boardName);
        await createBoardAuth(request, ADMIN.userId, boardId, 'W');

        // 검색
        await searchBoard(page, 'boardId', boardId);
        await expect(page.locator('#boardTableBody').getByText(boardId)).toBeVisible();

        // 행 클릭하여 수정 모달 열기
        const detailResponse = page.waitForResponse(r =>
            r.url().includes(`/api/boards/${boardId}`) && !r.url().includes('/page'),
        );
        await page.locator('#boardTableBody tr').first().click();
        await detailResponse;

        await expect(page.locator('#boardMgmtModal')).toBeVisible();
        await expect(page.locator('#btnDeleteBoard')).toBeVisible();

        // 삭제
        const deleteResponse = page.waitForResponse(r =>
            r.url().includes(`/api/boards/${boardId}`) && r.request().method() === 'DELETE',
        );
        await page.locator('#btnDeleteBoard').click();
        await page.locator('#spConfirmModalOk').click();
        await deleteResponse;

        await expect(page.locator('#boardMgmtModal')).not.toBeVisible();

        // 재검색하여 삭제 확인
        await searchBoard(page, 'boardId', boardId);
        await expect(page.locator('#boardTableBody').getByText(boardId)).not.toBeVisible();
        // 삭제 테스트이므로 cleanup 불필요
    });
});
