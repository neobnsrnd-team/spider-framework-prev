/**
 * 게시판 권한 관리 페이지 -- 목록, CRUD.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 * 게시판: notice-board (e2e-seed.sql에 시드됨)
 * 사용자: e2e-admin (e2e-seed.sql에 시드됨)
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

// ─── Helper functions ────────────────────────────────────

/** 고유 ID 생성 (테스트 데이터 충돌 방지) */
function uniqueId(prefix: string): string {
    return prefix + Date.now().toString(36);
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

/** API로 테스트용 게시판을 생성한다. */
async function createBoard(
    request: APIRequestContext,
    boardId: string,
    boardName: string,
    boardType: string = 'N',
): Promise<void> {
    const res = await request.post('/api/boards', {
        data: { boardId, boardName, boardType },
    });
    expect(res.status()).toBe(201);
}

/** API로 테스트용 게시판을 삭제한다. 이미 없으면 무시한다. */
async function deleteBoard(request: APIRequestContext, boardId: string): Promise<void> {
    await request.delete(`/api/boards/${encodeURIComponent(boardId)}`);
}

/** 게시판 권한 관리 페이지로 이동하고 초기 데이터 로드를 대기한다. */
async function navigateToBoardAuthPage(page: Page): Promise<void> {
    const boardAuthPromise = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
    const boardsPromise = page.waitForResponse(r => /\/api\/boards(\?|$)/.test(r.url()));
    const usersPromise = page.waitForResponse(r => r.url().includes('/api/users/page'));
    await page.goto('/boards/auth');
    await Promise.all([boardAuthPromise, boardsPromise, usersPromise]);
    await expect(page.locator('#boardAuthTable')).toBeVisible();
}

// ─── Setup ───────────────────────────────────────────────

test.beforeEach(async ({ page }) => {
    await navigateToBoardAuthPage(page);
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('게시판 권한 목록', () => {

    test('초기 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#boardAuthTable')).toBeVisible();
        await expect(page.locator('#boardAuthTableBody')).toBeVisible();
        // e2e-admin의 시드 데이터가 있으므로 행이 1개 이상 존재해야 한다
        const rows = page.locator('#boardAuthTableBody tr');
        const count = await rows.count();
        expect(count).toBeGreaterThan(0);
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const boardId = uniqueId('e2e-srch-');
        const boardName = boardId + '-board';

        await createBoard(request, boardId, boardName);
        await createBoardAuth(request, ADMIN.userId, boardId, 'R');

        try {
            // 게시판 필터 드롭다운 새로고침을 위해 페이지 재진입
            await navigateToBoardAuthPage(page);

            // 게시판 필터로 검색
            await page.locator('#boardIdFilter').selectOption(boardId);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 검색 결과에 해당 권한이 표시되어야 한다
            await expect(page.locator('#boardAuthTableBody').getByText(boardId, { exact: true })).toBeVisible();

            // 존재하지 않는 사용자로 검색하면 결과가 없어야 한다
            await page.locator('#searchValue').fill('zzz-nonexistent-user-99999');
            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise2;

            await expect(page.locator('#boardAuthTableBody').getByText('조회된 데이터가 없습니다')).toBeVisible();
        } finally {
            await deleteBoardAuth(request, ADMIN.userId, boardId);
            await deleteBoard(request, boardId);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        const userIdTh = page.locator('th[data-sort="userId"]');

        // 클릭하여 ASC 정렬
        const responsePromise = page.waitForResponse(r =>
            r.url().includes('/api/board-auth/page') && r.url().includes('sortBy=userId'),
        );
        await userIdTh.click();
        await responsePromise;

        await expect(userIdTh).toHaveClass(/sort-asc/);

        // 다시 클릭하면 DESC로 변경
        const responsePromise2 = page.waitForResponse(r =>
            r.url().includes('sortDirection=DESC'),
        );
        await userIdTh.click();
        await responsePromise2;

        await expect(userIdTh).toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 CSV 파일이 다운로드되어야 한다', async ({ page }) => {
        // 시드 데이터가 있으므로 데이터가 존재한다
        const downloadPromise = page.waitForEvent('download');
        await page.locator('#btnExcel').click();
        const download = await downloadPromise;

        expect(download.suggestedFilename()).toMatch(/\.csv$/);
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, context }) => {
        // window.print()가 팝업을 즉시 닫는 것을 방지
        await context.addInitScript(() => { window.print = () => {}; });

        const popupPromise = context.waitForEvent('page');
        await page.locator('#btnPrint').click();
        const popup = await popupPromise;

        await popup.waitForLoadState('domcontentloaded');
        await expect(popup.locator('table')).toBeVisible();
        await popup.close();
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('게시판 권한 CRUD', () => {

    test('권한 부여 버튼을 클릭하면 등록 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnRegister').click();

        await expect(page.locator('#boardAuthModal')).toBeVisible();
        await expect(page.locator('#boardAuthModalTitle')).toHaveText('권한 부여');

        // 사용자/게시판 select가 활성화되어야 한다
        await expect(page.locator('#modalUserId')).toBeEnabled();
        await expect(page.locator('#modalBoardId')).toBeEnabled();

        // 삭제 버튼은 숨겨져야 한다
        await expect(page.locator('#btnDeleteAuth')).toHaveClass(/d-hidden/);

        // 닫기
        await page.locator('#boardAuthModal .modal-footer button[data-bs-dismiss="modal"]').click();
        await expect(page.locator('#boardAuthModal')).not.toBeVisible();
    });

    test('모달에서 권한을 부여하면 목록에 나타나야 한다', async ({ page, request }) => {
        const boardId = uniqueId('e2e-crd-');
        const boardName = boardId + '-board';

        await createBoard(request, boardId, boardName);

        page.on('dialog', d => d.accept());

        try {
            // 모달에 새 게시판이 보이도록 페이지 재진입
            await navigateToBoardAuthPage(page);

            await page.locator('#btnRegister').click();
            await expect(page.locator('#boardAuthModal')).toBeVisible();

            // 사용자 선택
            await page.locator('#modalUserId').selectOption(ADMIN.userId);
            // 게시판 선택
            await page.locator('#modalBoardId').selectOption(boardId);
            // 권한: W (기본 선택됨)

            // 저장
            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/board-auth') && r.request().method() === 'POST',
            );
            await page.locator('#boardAuthModal button:has-text("저장")').click();
            await responsePromise;

            // 모달이 닫히고 목록이 새로고침된다
            await expect(page.locator('#boardAuthModal')).not.toBeVisible();

            // 게시판 필터로 검색하여 확인
            await page.locator('#boardIdFilter').selectOption(boardId);
            const reloadRes = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await reloadRes;

            await expect(page.locator('#boardAuthTableBody').getByText(boardId, { exact: true })).toBeVisible();
        } finally {
            await deleteBoardAuth(request, ADMIN.userId, boardId);
            await deleteBoard(request, boardId);
        }
    });

    test('필수 항목 없이 저장하면 유효성 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('#btnRegister').click();
        await expect(page.locator('#boardAuthModal')).toBeVisible();

        // 사용자/게시판을 선택하지 않고 저장
        await page.locator('#boardAuthModal button:has-text("저장")').click();

        // 유효성 알림이 표시되어야 한다 (사용자를 선택하세요 또는 게시판을 선택하세요)
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('선택');

        await page.locator('#boardAuthModal .modal-footer button[data-bs-dismiss="modal"]').click();
    });

    test('행을 클릭하면 수정 모달이 열리고 권한을 변경할 수 있어야 한다', async ({ page, request }) => {
        const boardId = uniqueId('e2e-edt-');
        const boardName = boardId + '-board';

        await createBoard(request, boardId, boardName);
        await createBoardAuth(request, ADMIN.userId, boardId, 'W');

        page.on('dialog', d => d.accept());

        try {
            // 페이지 재진입 (게시판 드롭다운 갱신)
            await navigateToBoardAuthPage(page);

            // 게시판 필터로 검색
            await page.locator('#boardIdFilter').selectOption(boardId);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 행 클릭
            await page.locator(`#boardAuthTableBody tr[data-board-id="${boardId}"]`).click();

            // 수정 모달이 열려야 한다
            await expect(page.locator('#boardAuthModal')).toBeVisible();
            await expect(page.locator('#boardAuthModalTitle')).toHaveText('권한 수정');

            // 사용자/게시판 필드는 비활성화
            await expect(page.locator('#modalUserId')).toBeDisabled();
            await expect(page.locator('#modalBoardId')).toBeDisabled();

            // 삭제 버튼이 보여야 한다
            await expect(page.locator('#btnDeleteAuth')).not.toHaveClass(/d-hidden/);

            // 권한을 R로 변경
            await page.locator('input[name="modalAuthCode"][value="R"]').check();

            // 저장
            const updateRes = page.waitForResponse(r =>
                r.url().includes('/api/board-auth/') && r.request().method() === 'PUT',
            );
            await page.locator('#boardAuthModal button:has-text("저장")').click();
            await updateRes;

            await expect(page.locator('#boardAuthModal')).not.toBeVisible();

            // 목록 재조회하여 변경 확인 (R 배지가 보여야 한다)
            await page.locator('#boardIdFilter').selectOption(boardId);
            const reloadRes = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await reloadRes;

            const authCell = page.locator(`#boardAuthTableBody tr[data-board-id="${boardId}"] .auth-read`);
            await expect(authCell).toBeVisible();
        } finally {
            await deleteBoardAuth(request, ADMIN.userId, boardId);
            await deleteBoard(request, boardId);
        }
    });

    test('권한을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const boardId = uniqueId('e2e-del-');
        const boardName = boardId + '-board';

        await createBoard(request, boardId, boardName);
        await createBoardAuth(request, ADMIN.userId, boardId, 'R');

        try {
            // 페이지 재진입
            await navigateToBoardAuthPage(page);

            // 게시판 필터로 검색
            await page.locator('#boardIdFilter').selectOption(boardId);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 행 클릭
            await page.locator(`#boardAuthTableBody tr[data-board-id="${boardId}"]`).click();
            await expect(page.locator('#boardAuthModal')).toBeVisible();

            // 삭제
            const deleteRes = page.waitForResponse(r =>
                r.url().includes('/api/board-auth/') && r.request().method() === 'DELETE',
            );
            await page.locator('#btnDeleteAuth').click();
            await page.locator('#spConfirmModalOk').click();
            await deleteRes;

            await expect(page.locator('#boardAuthModal')).not.toBeVisible();

            // 재조회하여 삭제 확인
            const reloadRes = page.waitForResponse(r => r.url().includes('/api/board-auth/page'));
            await page.locator('button:has-text("조회")').click();
            await reloadRes;

            await expect(page.locator('#boardAuthTableBody').getByText(boardId, { exact: true })).not.toBeVisible();
        } finally {
            // cleanup: board auth는 이미 삭제됨, 게시판만 정리
            await deleteBoard(request, boardId);
        }
    });
});
