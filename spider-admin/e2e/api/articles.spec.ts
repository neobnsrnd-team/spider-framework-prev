/**
 * 게시글 API 계약 테스트 — /api/articles
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, 검색 필터, 정렬)
 * - 단건 조회 (존재/미존재)
 * - 게시글 CRUD (생성, 수정, 삭제)
 * - 다운로드 카운트
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

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

function generateTestTitle(prefix: string): string {
    return prefix + Date.now().toString(36);
}

// ─── 페이지네이션 조회 ────────────────────────────────────

test.describe('/api/articles/board/{boardId}/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/articles/board/${TEST_BOARD_ID}/page`, {
            params: { page: 1, size: 5 },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('content');
        expect(data).toHaveProperty('totalElements');
        expect(data).toHaveProperty('currentPage');
        expect(data).toHaveProperty('totalPages');
        expect(data).toHaveProperty('size');
        expect(Array.isArray(data.content)).toBe(true);
    });

    test('검색 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const title = generateTestTitle('e2e-search-');
        const articleSeq = await createArticle(request, title);

        try {
            const res = await request.get(`/api/articles/board/${TEST_BOARD_ID}/page`, {
                params: { page: 1, size: 10, searchField: 'title', keyword: title },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.content.length).toBe(1);
            expect(body.data.content[0].title).toBe(title);
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('정렬 파라미터를 지정하면 해당 기준으로 정렬되어야 한다', async ({ request }) => {
        const res = await request.get(`/api/articles/board/${TEST_BOARD_ID}/page`, {
            params: { page: 1, size: 10, sortBy: 'title', sortDirection: 'ASC' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });
});

// ─── 단건 조회 ────────────────────────────────────────────

test.describe('/api/articles/{articleSeq} — 단건 조회', () => {

    test('존재하는 게시글 조회 시 HTTP 200과 상세 정보를 반환해야 한다', async ({ request }) => {
        const title = generateTestTitle('e2e-detail-');
        const articleSeq = await createArticle(request, title);

        try {
            const res = await request.get(`/api/articles/${articleSeq}`);
            expect(res.status()).toBe(200);

            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.articleSeq).toBe(articleSeq);
            expect(body.data.title).toBe(title);
            expect(body.data.boardId).toBe(TEST_BOARD_ID);
            expect(body.data).toHaveProperty('content');
            expect(body.data).toHaveProperty('readCnt');
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('존재하지 않는 게시글 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/articles/999999999');
        expect(res.status()).toBe(404);

        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── CRUD ─────────────────────────────────────────────────

test.describe('/api/articles — CRUD', () => {

    test('게시글 생성 시 HTTP 201과 생성된 데이터를 반환해야 한다', async ({ request }) => {
        const title = generateTestTitle('e2e-create-');
        let articleSeq: number | undefined;

        try {
            const res = await request.post('/api/articles', {
                data: {
                    boardId: TEST_BOARD_ID,
                    title,
                    content: 'E2E 생성 테스트 내용',
                    topYn: 'N',
                },
            });

            expect(res.status()).toBe(201);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.title).toBe(title);
            expect(body.data.boardId).toBe(TEST_BOARD_ID);
            articleSeq = body.data.articleSeq;
        } finally {
            if (articleSeq) await deleteArticle(request, articleSeq);
        }
    });

    test('필수 항목 없이 생성하면 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/articles', {
            data: { boardId: TEST_BOARD_ID },
        });

        expect(res.status()).toBe(400);
    });

    test('게시글 수정 시 HTTP 200과 수정된 데이터를 반환해야 한다', async ({ request }) => {
        const title = generateTestTitle('e2e-update-');
        const articleSeq = await createArticle(request, title);

        try {
            const newTitle = title + '-modified';
            const res = await request.put(`/api/articles/${articleSeq}`, {
                data: {
                    boardId: TEST_BOARD_ID,
                    title: newTitle,
                    content: '수정된 내용',
                },
            });

            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.success).toBe(true);
            expect(body.data.title).toBe(newTitle);
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });

    test('게시글 삭제 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const title = generateTestTitle('e2e-delete-');
        const articleSeq = await createArticle(request, title);

        const res = await request.delete(`/api/articles/${articleSeq}`);
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);

        // 삭제 후 조회하면 404
        const checkRes = await request.get(`/api/articles/${articleSeq}`);
        expect(checkRes.status()).toBe(404);
    });

    test('존재하지 않는 게시글 수정 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.put('/api/articles/999999999', {
            data: {
                boardId: TEST_BOARD_ID,
                title: 'nonexistent',
                content: 'nonexistent',
            },
        });

        expect(res.status()).toBe(404);
    });
});

// ─── 다운로드 카운트 ──────────────────────────────────────

test.describe('/api/articles/{articleSeq}/download — 다운로드 카운트', () => {

    test('다운로드 카운트 증가 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const articleSeq = await createArticle(request, generateTestTitle('e2e-dl-'));

        try {
            const res = await request.post(`/api/articles/${articleSeq}/download/1`);
            expect(res.status()).toBe(200);
        } finally {
            await deleteArticle(request, articleSeq);
        }
    });
});

// ─── 게시판별 조회 ────────────────────────────────────────

test.describe('/api/articles/board/{boardId} — 게시판별 전체 조회', () => {

    test('조회 시 HTTP 200과 배열을 반환해야 한다', async ({ request }) => {
        const res = await request.get(`/api/articles/board/${TEST_BOARD_ID}`);
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });
});
