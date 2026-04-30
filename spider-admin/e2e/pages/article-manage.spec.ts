/**
 * 게시글 관리 페이지 — 목록, CRUD, 정렬, 엑셀, 출력.
 *
 * 모든 테스트는 API로 자체 데이터를 생성한다. 기존 DB 상태에 의존하지 않는다.
 * 게시판 ID: notice-board (e2e-seed.sql에 e2e-admin WRITE 권한 시드됨)
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const TEST_BOARD_ID = 'notice-board';

async function createArticle(
    request: APIRequestContext,
    title: string,
    content: string = 'E2E 테스트 내용입니다.',
): Promise<number> {
    const res = await request.post('/api/articles', {
        data: { boardId: TEST_BOARD_ID, title, content, topYn: 'N' },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    return body.data.articleSeq;
}

async function deleteArticle(request: APIRequestContext, articleSeq: number) {
    await request.delete(`/api/articles/${articleSeq}`);
}

async function navigateToArticlePage(page: Page, boardId: string = TEST_BOARD_ID) {
    await page.goto(`/articles/${boardId}`);
    await page.waitForResponse(r => r.url().includes(`/api/articles/board/${boardId}/page`));
    await expect(page.locator('#articleTable')).toBeVisible();
}

function generateTestTitle(prefix: string): string {
    return prefix + Date.now().toString(36);
}

test.beforeEach(async ({ page }) => {
    await navigateToArticlePage(page);
});

// ─── 목록 ────────────────────────────────────────────────

test.describe('게시글 목록', () => {

    test('초기 로드 시 테이블이 표시되어야 한다', async ({ page }) => {
        await expect(page.locator('#articleTable')).toBeVisible();
        await expect(page.locator('#articleTableBody')).toBeVisible();
    });

    test('초기 페이지 로드 시 데이터는 최대 10건이 조회되어야 한다', async ({ page, request }) => {
        // 11건의 테스트 데이터 생성
        const articleSeqs: number[] = [];
        for (let i = 0; i < 11; i++) {
            const seq = await createArticle(request, generateTestTitle(`e2e-paging-${i}-`));
            articleSeqs.push(seq);
        }

        try {
            // 페이지 새로고침
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.goto(`/articles/${TEST_BOARD_ID}`);
            await responsePromise;

            // 테이블 행 수 확인 (헤더 제외)
            const rows = page.locator('#articleTableBody tr');
            const count = await rows.count();
            expect(count).toBeLessThanOrEqual(10);
            expect(count).toBeGreaterThan(0);
        } finally {
            for (const seq of articleSeqs) {
                await deleteArticle(request, seq);
            }
        }
    });

    test('데이터 행을 클릭하면 상세 조회 모달이 열려야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-view-');
        const articleSeq = await createArticle(request, title);

        try {
            // 검색하여 해당 게시글 찾기
            await page.locator('#searchField2').selectOption('title');
            await page.locator('#searchValue2').fill(title);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 행 클릭
            const detailResponse = page.waitForResponse(r => r.url().includes(`/api/articles/${articleSeq}`));
            await page.locator('#articleTableBody tr').first().click();
            await detailResponse;

            // 모달 표시 확인
            await expect(page.locator('#articleModal')).toBeVisible();
            await expect(page.locator('#modalTitle')).toHaveText(LABEL.ARTICLE_DETAIL_TITLE);

            // 읽기 뷰에서 제목 확인
            await expect(page.locator('#readTitle')).toContainText(title);

            // 닫기
            await page.locator('#articleModal .modal-footer button[data-bs-dismiss="modal"]').click();
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('검색 조건을 변경하면 해당 조건에 맞는 데이터가 조회되어야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-srch-');
        const articleSeq = await createArticle(request, title);

        try {
            await page.locator('#searchField2').selectOption('title');
            await page.locator('#searchValue2').fill(title);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 검색 결과에 해당 게시글이 표시되어야 함
            await expect(page.locator('#articleTableBody').getByText(title)).toBeVisible();

            // 존재하지 않는 검색어로 검색
            await page.locator('#searchValue2').fill('zzz-nonexistent-article-99999');
            const responsePromise2 = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise2;

            await expect(page.locator('#articleTableBody').getByText(LABEL.NO_DATA)).toBeVisible();
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('컬럼 헤더를 클릭하면 정렬이 변경되어야 한다', async ({ page }) => {
        // 제목 컬럼 클릭하여 정렬
        const titleTh = page.locator('th[data-sort="title"]');
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/') && r.url().includes('sortBy=title'));
        await titleTh.click();
        await responsePromise;

        // sort-asc 클래스 확인
        await expect(titleTh).toHaveClass(/sort-asc/);

        // 다시 클릭하면 DESC로 변경
        const responsePromise2 = page.waitForResponse(r => r.url().includes('sortDirection=DESC'));
        await titleTh.click();
        await responsePromise2;

        await expect(titleTh).toHaveClass(/sort-desc/);
    });

    test('엑셀 버튼을 클릭하면 CSV 파일이 다운로드되어야 한다', async ({ page, request }) => {
        // 데이터가 있어야 다운로드 가능
        const articleSeq = await createArticle(request, generateTestTitle('e2e-excel-'));

        try {
            // 데이터 리로드
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.goto(`/articles/${TEST_BOARD_ID}`);
            await responsePromise;

            const downloadPromise = page.waitForEvent('download');
            await page.locator('#btnExcel').click();
            const download = await downloadPromise;

            expect(download.suggestedFilename()).toMatch(/\.csv$/);
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('출력 버튼을 클릭하면 인쇄 팝업이 열려야 한다', async ({ page, request, context }) => {
        const articleSeq = await createArticle(request, generateTestTitle('e2e-print-'));

        try {
            // 데이터 리로드
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.goto(`/articles/${TEST_BOARD_ID}`);
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
            await deleteArticle(request, articleSeq);
        }
    });
});

// ─── CRUD ────────────────────────────────────────────────

test.describe('게시글 CRUD', () => {

    test('새 글 작성 버튼을 클릭하면 등록 모달이 표시되어야 한다', async ({ page }) => {
        await page.locator('button:has-text("새 글 작성")').click();

        await expect(page.locator('#articleModal')).toBeVisible();
        await expect(page.locator('#modalTitle')).toHaveText(LABEL.ARTICLE_CREATE_TITLE);

        // 빈 폼 확인
        await expect(page.locator('#modalTitle2')).toHaveValue('');
        await expect(page.locator('#modalContent')).toHaveValue('');

        // 삭제 버튼은 숨겨져야 함
        await expect(page.locator('#btnDelete')).not.toBeVisible();
        // 저장 버튼은 표시되어야 함
        await expect(page.locator('#btnSave')).toBeVisible();

        await page.locator('#articleModal .modal-footer button[data-bs-dismiss="modal"]').click();
        await expect(page.locator('#articleModal')).not.toBeVisible();
    });

    test('모달에서 게시글을 생성하면 목록에 나타나야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-crud-');
        let createdArticleSeq: number | undefined;

        page.on('dialog', d => d.accept());

        try {
            await page.locator('button:has-text("새 글 작성")').click();
            await expect(page.locator('#articleModal')).toBeVisible();

            await page.locator('#modalTitle2').fill(title);
            await page.locator('#modalContent').fill('E2E 생성 테스트 내용');

            const responsePromise = page.waitForResponse(r =>
                r.url().includes('/api/articles') && r.request().method() === 'POST'
            );
            await page.locator('#btnSave').click();
            const response = await responsePromise;
            const body = await response.json();
            createdArticleSeq = body.data?.articleSeq;

            await expect(page.locator('#articleModal')).not.toBeVisible();

            // 검색하여 목록에 나타나는지 확인
            await page.locator('#searchField2').selectOption('title');
            await page.locator('#searchValue2').fill(title);
            const reloadRes = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await reloadRes;

            await expect(page.locator('#articleTableBody').getByText(title)).toBeVisible();
        } finally {
            if (createdArticleSeq) await deleteArticle(request, createdArticleSeq);
        }
    });

    test('제목 없이 저장하면 유효성 알림이 표시되어야 한다', async ({ page }) => {
        await page.locator('button:has-text("새 글 작성")').click();
        await expect(page.locator('#articleModal')).toBeVisible();

        // 제목, 내용 비우고 저장
        await page.locator('#btnSave').click();

        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('제목');

        await page.locator('#articleModal .modal-footer button[data-bs-dismiss="modal"]').click();
    });

    test('행을 클릭하고 수정 모드로 전환하면 편집할 수 있어야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-edit-');
        const articleSeq = await createArticle(request, title);

        page.on('dialog', d => d.accept());

        try {
            // 검색
            await page.locator('#searchField2').selectOption('title');
            await page.locator('#searchValue2').fill(title);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            // 행 클릭하여 상세 모달 열기
            const detailResponse = page.waitForResponse(r => r.url().includes(`/api/articles/${articleSeq}`));
            await page.locator('#articleTableBody tr').first().click();
            await detailResponse;

            await expect(page.locator('#articleModal')).toBeVisible();
            await expect(page.locator('#modalTitle')).toHaveText(LABEL.ARTICLE_DETAIL_TITLE);

            // 수정 버튼 클릭
            await page.locator('#btnEdit').click();
            await expect(page.locator('#modalTitle')).toHaveText(LABEL.ARTICLE_EDIT_TITLE);

            // 제목 수정
            const newTitle = title + '-modified';
            await page.locator('#modalTitle2').fill(newTitle);

            // 저장
            const updateResponse = page.waitForResponse(r =>
                r.url().includes(`/api/articles/${articleSeq}`) && r.request().method() === 'PUT'
            );
            await page.locator('#btnSave').click();
            await updateResponse;

            await expect(page.locator('#articleModal')).not.toBeVisible();

            // 수정된 제목으로 검색
            await page.locator('#searchValue2').fill(newTitle);
            const reloadRes = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await reloadRes;

            await expect(page.locator('#articleTableBody').getByText(newTitle)).toBeVisible();
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('게시글을 삭제하면 목록에서 사라져야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-del-');
        const articleSeq = await createArticle(request, title);

        // 검색
        await page.locator('#searchField2').selectOption('title');
        await page.locator('#searchValue2').fill(title);
        const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
        await page.locator('button:has-text("조회")').click();
        await responsePromise;

        // 행 클릭
        const detailResponse = page.waitForResponse(r => r.url().includes(`/api/articles/${articleSeq}`));
        await page.locator('#articleTableBody tr').first().click();
        await detailResponse;

        await expect(page.locator('#articleModal')).toBeVisible();

        // 수정 모드 진입 (삭제 버튼은 수정 모드에서 보임)
        await page.locator('#btnEdit').click();

        // 삭제
        const deleteResponse = page.waitForResponse(r =>
            r.url().includes(`/api/articles/${articleSeq}`) && r.request().method() === 'DELETE'
        );
        await page.locator('#btnDelete').click();
        await page.locator('#spConfirmModalOk').click();
        await deleteResponse;

        await expect(page.locator('#articleModal')).not.toBeVisible();

        // 재검색하여 삭제 확인
        const reloadRes = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
        await page.locator('button:has-text("조회")').click();
        await reloadRes;

        await expect(page.locator('#articleTableBody').getByText(title)).not.toBeVisible();
    });
});

// ─── 권한 ────────────────────────────────────────────────

test.describe('게시글 권한', () => {

    test('WRITE 권한이 있으면 새 글 작성 버튼이 표시되어야 한다', async ({ page }) => {
        // e2e-admin은 notice-board에 W 권한이 있음
        await expect(page.locator('button:has-text("새 글 작성")')).toBeVisible();
    });

    test('상세 모달에서 수정 버튼이 표시되어야 한다', async ({ page, request }) => {
        const title = generateTestTitle('e2e-perm-');
        const articleSeq = await createArticle(request, title);

        try {
            await page.locator('#searchField2').selectOption('title');
            await page.locator('#searchValue2').fill(title);
            const responsePromise = page.waitForResponse(r => r.url().includes('/api/articles/board/'));
            await page.locator('button:has-text("조회")').click();
            await responsePromise;

            const detailResponse = page.waitForResponse(r => r.url().includes(`/api/articles/${articleSeq}`));
            await page.locator('#articleTableBody tr').first().click();
            await detailResponse;

            await expect(page.locator('#articleModal')).toBeVisible();
            await expect(page.locator('#btnEdit')).toBeVisible();

            await page.locator('#articleModal .modal-footer button[data-bs-dismiss="modal"]').click();
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });
});
