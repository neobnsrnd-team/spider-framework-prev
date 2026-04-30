/**
 * 전문로그파싱 페이지 — 기관 선택, 전문 검색, 파싱 실행, 권한.
 *
 * 이 파일은 두 개의 URL을 모두 테스트한다:
 * - /message-parsing (전문로그파싱)
 * - /message-parsing/json (전문로그파싱 JSON)
 *
 * 두 URL은 동일한 템플릿을 사용하므로 동일한 UI 동작을 검증한다.
 * CI 환경에서는 기관/전문 데이터가 없을 수 있으므로 test.skip으로 처리한다.
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { LABEL } from '../fixtures/locale';

const ORGS_API = '/api/telegram/orgs';
const MESSAGES_API = '/api/telegram/messages';

/** 기관 목록 API 응답을 대기하며 페이지 이동 */
async function gotoAndWait(page: Page, url: string) {
    const orgsPromise = page.waitForResponse(
        r => r.url().includes(ORGS_API),
        { timeout: 15000 },
    );
    await page.goto(url);
    await orgsPromise;
}

/** 기관 목록 API로 데이터 존재 여부 확인 */
async function fetchOrgs(request: APIRequestContext): Promise<{ orgId: string; orgName: string }[]> {
    const res = await request.get(ORGS_API);
    const body = await res.json();
    return body.success ? body.data : [];
}

/** 커스텀 기관 드롭다운에서 첫 번째 기관 선택 */
async function selectFirstOrg(page: Page) {
    // 드롭다운 열기
    await page.locator('#orgSelectTrigger').click();
    await expect(page.locator('#orgSelectDropdown')).toBeVisible();

    // "기관선택" 기본 항목 이후의 첫 번째 실제 기관 선택
    const orgItems = page.locator('#orgSelectList li');
    const count = await orgItems.count();
    // 첫 번째는 "기관선택" 기본 항목, 두 번째부터 실제 기관
    if (count > 1) {
        await orgItems.nth(1).click();
    }
}

// ─── 전문로그파싱 (/message-parsing) ─────────────────────────────

test.describe('전문로그파싱 (/message-parsing)', () => {
    const PAGE_URL = '/message-parsing';

    test.beforeEach(async ({ page }) => {
        await gotoAndWait(page, PAGE_URL);
    });

    // ── 초기 로드 ────────────────────────────────────────────

    test('페이지 로드 시 검색 조건 영역과 전문 데이터 영역이 표시되어야 한다', async ({ page }) => {
        // 기관명 라벨 + 드롭다운
        await expect(page.locator('#orgSelectTrigger')).toBeVisible();

        // 전문명 입력 (readonly)
        await expect(page.locator('#messageIdInput')).toBeVisible();

        // 조회 버튼
        await expect(page.getByRole('button', { name: LABEL.SEARCH })).toBeVisible();

        // 전문 데이터 텍스트 영역
        await expect(page.locator('#telegramData')).toBeVisible();

        // 전문파싱 버튼
        await expect(page.getByRole('button', { name: LABEL.PARSE_BUTTON })).toBeVisible();
    });

    test('기관 드롭다운이 로드되어야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);

        // 드롭다운 열기
        await page.locator('#orgSelectTrigger').click();
        await expect(page.locator('#orgSelectDropdown')).toBeVisible();

        const orgItems = page.locator('#orgSelectList li');
        const itemCount = await orgItems.count();

        // 최소 "기관선택" 기본 항목 1개는 존재
        expect(itemCount).toBeGreaterThanOrEqual(1);

        if (orgs.length > 0) {
            // 기관 데이터가 있으면 기본 항목 + 기관 수만큼 항목이 있어야 한다
            expect(itemCount).toBe(orgs.length + 1);
        }

        // 드롭다운 닫기 (외부 클릭)
        await page.locator('#orgSelectTrigger').click();
    });

    // ── 기관 미선택 시 검증 ──────────────────────────────────

    test('기관 미선택 시 조회 버튼 클릭하면 알림이 표시되어야 한다', async ({ page }) => {
        // 기관 선택 안 한 상태에서 조회 버튼 클릭
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('기관명을 먼저 선택해주세요');
    });

    // ── 기관 선택 후 전문 검색 모달 ─────────────────────────

    test('기관 선택 후 조회 버튼 클릭 시 전문 검색 모달이 열려야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        await selectFirstOrg(page);

        // 조회 버튼 클릭 → 전문 검색 모달 열림 + 전문 목록 API 호출
        const messagesPromise = page.waitForResponse(r => r.url().includes(MESSAGES_API));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await messagesPromise;

        // 전문 검색 모달이 표시
        const modal = page.locator('#messageSearchModal');
        await expect(modal).toBeVisible();
        await expect(modal.locator('.modal-title')).toHaveText('전문 검색');

        // 검색 필드 (전문ID/전문명) 셀렉트가 표시
        await expect(page.locator('#messageSearchField')).toBeVisible();
        await expect(page.locator('#messageSearchValue')).toBeVisible();

        // 모달 닫기
        await modal.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(modal).not.toBeVisible();
    });

    test('전문 검색 모달에서 검색 필드를 전문ID/전문명으로 전환할 수 있어야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        await selectFirstOrg(page);

        const messagesPromise = page.waitForResponse(r => r.url().includes(MESSAGES_API));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await messagesPromise;

        const modal = page.locator('#messageSearchModal');
        await expect(modal).toBeVisible();

        // 전문ID 옵션 선택
        await page.locator('#messageSearchField').selectOption('messageId');
        await expect(page.locator('#messageSearchField')).toHaveValue('messageId');

        // 전문명 옵션 선택
        await page.locator('#messageSearchField').selectOption('messageName');
        await expect(page.locator('#messageSearchField')).toHaveValue('messageName');

        await modal.locator('[data-bs-dismiss="modal"]').first().click();
    });

    test('전문 검색 모달에서 전문을 선택하면 전문명이 입력 필드에 표시되어야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        await selectFirstOrg(page);

        const messagesPromise = page.waitForResponse(r => r.url().includes(MESSAGES_API));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await messagesPromise;

        const modal = page.locator('#messageSearchModal');
        await expect(modal).toBeVisible();

        // 전문 데이터가 있는지 확인
        const messageRows = modal.locator('#messageListBody tr');
        const rowCount = await messageRows.count();
        const firstRowText = await messageRows.first().textContent();

        if (rowCount === 0 || firstRowText?.includes('검색 결과가 없습니다') || firstRowText?.includes('조회 버튼을 눌러주세요')) {
            test.skip();
            return;
        }

        // 첫 번째 전문 행 클릭
        await messageRows.first().click();

        // 모달이 닫히고 전문명 입력 필드에 값이 설정
        await expect(modal).not.toBeVisible();
        await expect(page.locator('#messageIdInput')).not.toHaveValue('');
    });

    // ── 파싱 검증 ────────────────────────────────────────────

    test('기관과 전문 미선택 시 전문파싱 버튼 클릭하면 오류 메시지가 표시되어야 한다', async ({ page }) => {
        // 기관/전문 선택 없이 파싱 버튼 클릭
        await page.getByRole('button', { name: LABEL.PARSE_BUTTON }).click();

        // messageArea에 오류 메시지 표시
        await expect(page.locator('#messageArea')).toContainText('기관과 전문을 선택해주세요');
    });

    test('전문 데이터 미입력 시 파싱 버튼 클릭하면 알림이 표시되어야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        // 기관 선택
        await selectFirstOrg(page);

        // 전문 검색 모달 열어서 전문 선택
        const messagesPromise = page.waitForResponse(r => r.url().includes(MESSAGES_API));
        await page.getByRole('button', { name: LABEL.SEARCH }).click();
        await messagesPromise;

        const modal = page.locator('#messageSearchModal');
        await expect(modal).toBeVisible();

        const messageRows = modal.locator('#messageListBody tr');
        const rowCount = await messageRows.count();
        const firstRowText = await messageRows.first().textContent();

        if (rowCount === 0 || firstRowText?.includes('검색 결과가 없습니다') || firstRowText?.includes('조회 버튼을 눌러주세요')) {
            test.skip();
            return;
        }

        // 첫 번째 전문 선택
        await messageRows.first().click();
        await expect(modal).not.toBeVisible();

        // 전문 데이터 비우기 (기본적으로 비어있음)
        await page.locator('#telegramData').fill('');

        // 전문파싱 버튼 클릭
        await page.getByRole('button', { name: LABEL.PARSE_BUTTON }).click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('전문메시지를 입력해주세요');
    });

    // ── 기관 드롭다운 검색 필터 ──────────────────────────────

    test('기관 드롭다운에서 검색 필터가 동작해야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        // 드롭다운 열기
        await page.locator('#orgSelectTrigger').click();
        await expect(page.locator('#orgSelectDropdown')).toBeVisible();

        // 검색 입력란이 보이는지 확인
        await expect(page.locator('#orgSearchInput')).toBeVisible();

        // 존재하지 않는 키워드로 필터
        await page.locator('#orgSearchInput').fill('nonexistent-org-e2e-xyz');

        // "기관선택" 기본 항목만 남아야 한다 (실제 기관은 필터링되어 사라짐)
        const orgItems = page.locator('#orgSelectList li');
        const filteredCount = await orgItems.count();
        expect(filteredCount).toBe(1); // "기관선택" 기본 항목만
    });

    // ── 전문명 입력 필드 readonly 검증 ───────────────────────

    test('전문명 입력 필드는 읽기 전용이어야 한다', async ({ page }) => {
        const input = page.locator('#messageIdInput');
        await expect(input).toHaveAttribute('readonly', '');
    });
});

// ─── 전문로그파싱(JSON) (/message-parsing/json) ─────────────────

test.describe('전문로그파싱(JSON) (/message-parsing/json)', () => {
    const PAGE_URL = '/message-parsing/json';

    test.beforeEach(async ({ page }) => {
        await gotoAndWait(page, PAGE_URL);
    });

    test('페이지 로드 시 검색 조건 영역과 전문 데이터 영역이 표시되어야 한다', async ({ page }) => {
        // 소스(왼쪽) 패널
        await expect(page.locator('#orgSelectTrigger')).toBeVisible();
        await expect(page.locator('#messageIdInput')).toBeVisible();
        await expect(page.locator('#telegramData')).toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.PARSE_BUTTON })).toBeVisible();
        // 대상(오른쪽) 패널
        await expect(page.locator('#targetOrgSelectTrigger')).toBeVisible();
        await expect(page.locator('#jsonOutput')).toBeVisible();
    });

    test('기관 드롭다운이 로드되어야 한다', async ({ page }) => {
        await page.locator('#orgSelectTrigger').click();
        await expect(page.locator('#orgSelectDropdown')).toBeVisible();

        const orgItems = page.locator('#orgSelectList li');
        const itemCount = await orgItems.count();
        expect(itemCount).toBeGreaterThanOrEqual(1);

        await page.locator('#orgSelectTrigger').click();
    });

    test('기관 미선택 시 조회 버튼 클릭하면 알림이 표시되어야 한다', async ({ page }) => {
        // 소스(왼쪽) 패널의 조회 버튼 — openMessageSearchModal()
        await page.getByRole('button', { name: LABEL.SEARCH }).first().click();
        await expect(page.locator('.toast')).toBeVisible();
        await expect(page.locator('.toast-body span')).toContainText('기관명을 먼저 선택해주세요');
    });

    test('기관과 전문 미선택 시 전문파싱 버튼 클릭하면 오류 메시지가 표시되어야 한다', async ({ page }) => {
        await page.getByRole('button', { name: LABEL.PARSE_BUTTON }).click();
        await expect(page.locator('#messageArea')).toContainText('기관과 전문을 선택해주세요');
    });

    test('기관 선택 후 조회 버튼 클릭 시 전문 검색 모달이 열려야 한다', async ({ page, request }) => {
        const orgs = await fetchOrgs(request);
        if (orgs.length === 0) {
            test.skip();
            return;
        }

        await selectFirstOrg(page);

        const messagesPromise = page.waitForResponse(r => r.url().includes(MESSAGES_API));
        // 소스(왼쪽) 패널의 조회 버튼
        await page.getByRole('button', { name: LABEL.SEARCH }).first().click();
        await messagesPromise;

        const modal = page.locator('#messageSearchModal');
        await expect(modal).toBeVisible();
        await expect(modal.locator('.modal-title')).toHaveText('전문 검색');

        await modal.locator('[data-bs-dismiss="modal"]').first().click();
        await expect(modal).not.toBeVisible();
    });
});

// ─── 권한 — R 권한 사용자 ────────────────────────────────────────

test.describe('권한 — R 권한 사용자', () => {
    test.use({ storageState: 'e2e/.auth/session-readonly.json' });

    test('R 권한 사용자도 전문로그파싱 페이지에 접근할 수 있어야 한다', async ({ page }) => {
        await gotoAndWait(page, '/message-parsing');

        // READ-only 페이지이므로 모든 UI 요소가 동일하게 표시
        await expect(page.locator('#orgSelectTrigger')).toBeVisible();
        await expect(page.locator('#telegramData')).toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.PARSE_BUTTON })).toBeVisible();
    });

    test('R 권한 사용자도 전문로그파싱(JSON) 페이지에 접근할 수 있어야 한다', async ({ page }) => {
        await gotoAndWait(page, '/message-parsing/json');

        await expect(page.locator('#orgSelectTrigger')).toBeVisible();
        await expect(page.locator('#telegramData')).toBeVisible();
        await expect(page.getByRole('button', { name: LABEL.PARSE_BUTTON })).toBeVisible();
    });
});
