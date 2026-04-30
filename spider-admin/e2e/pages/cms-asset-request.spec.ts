/**
 * CMS 이미지 승인 요청 페이지 E2E — /cms-admin/asset-requests
 *
 * 검증 범위:
 * - 페이지 로드 + 테이블 렌더
 * - 상태 필터 동작
 * - 초기화 버튼
 *
 * 로드는 e2e-admin(ADMIN 역할, CMS:W 보유) 계정 기반.
 */

import { test, expect } from '@playwright/test';

const PAGE_URL = '/cms-admin/asset-requests';
const LIST_API = '/api/cms-admin/asset-requests';

test.describe('CMS 이미지 승인 요청 화면', () => {

    test('페이지가 정상 로드되고 테이블이 렌더링되어야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(LIST_API));
        await page.goto(PAGE_URL);
        const listResponse = await responsePromise;
        expect(listResponse.status()).toBe(200);

        await expect(page.locator('#assetRequestTable')).toBeVisible();
        await expect(page.locator('#filterAssetState')).toBeVisible();
        await expect(page.locator('#filterSearch')).toBeVisible();
    });

    test('승인상태 필터 변경 후 조회 시 API가 필터와 함께 호출되어야 한다', async ({ page }) => {
        await page.goto(PAGE_URL);
        await page.waitForResponse(r => r.url().includes(LIST_API));

        await page.locator('#filterAssetState').selectOption('WORK');

        const filterResponse = page.waitForResponse(r => r.url().includes(LIST_API) && r.url().includes('assetState=WORK'));
        await page.getByRole('button', { name: /조회/ }).click();
        const res = await filterResponse;
        expect(res.status()).toBe(200);
    });

    test('초기화 버튼 클릭 시 필터가 비워져야 한다', async ({ page }) => {
        await page.goto(PAGE_URL);
        await page.waitForResponse(r => r.url().includes(LIST_API));

        await page.locator('#filterAssetState').selectOption('APPROVED');
        await page.locator('#filterSearch').fill('something');

        const resetResponse = page.waitForResponse(r => r.url().includes(LIST_API));
        await page.getByRole('button', { name: /초기화/ }).click();
        await resetResponse;

        await expect(page.locator('#filterAssetState')).toHaveValue('');
        await expect(page.locator('#filterSearch')).toHaveValue('');
    });
});
