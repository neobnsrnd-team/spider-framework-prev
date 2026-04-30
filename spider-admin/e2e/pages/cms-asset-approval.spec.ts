/**
 * CMS 이미지 승인 관리 페이지 E2E — /cms-admin/asset-approvals
 *
 * 검증 범위:
 * - 페이지 로드 + 기본 PENDING 목록 렌더
 * - 프리뷰 모달 열림 (상세 API 호출)
 * - 반려 모달 open/close
 *
 * 주의: 실제 승인/반려 액션은 시드 데이터를 변경하므로 여기선 모달 open까지만 검증.
 */

import { test, expect } from '@playwright/test';

const PAGE_URL = '/cms-admin/asset-approvals';
const LIST_API = '/api/cms-admin/asset-approvals';

test.describe('CMS 이미지 승인 관리 화면', () => {

    test('페이지 로드 시 기본 PENDING 목록을 조회해야 한다', async ({ page }) => {
        const responsePromise = page.waitForResponse(r => r.url().includes(LIST_API));
        await page.goto(PAGE_URL);
        const listResponse = await responsePromise;
        expect(listResponse.status()).toBe(200);

        await expect(page.locator('#assetApprovalTable')).toBeVisible();
        await expect(page.locator('#filterAssetState')).toBeVisible();
        await expect(page.locator('#filterStartDate')).toBeVisible();
    });

    test('썸네일 클릭 시 상세 모달이 열리고 API 호출이 발생해야 한다', async ({ page }) => {
        await page.goto(PAGE_URL);
        await page.waitForResponse(r => r.url().includes(LIST_API));

        // 시드 데이터에 PENDING 이미지가 있을 때만 모달 테스트 수행
        const firstRow = page.locator('#assetApprovalTableBody tr').first();
        const hasData = await firstRow.locator('img.asset-thumb').count();
        if (hasData === 0) {
            test.skip(true, '시드에 PENDING asset이 없어 모달 테스트 스킵');
            return;
        }

        const detailResponse = page.waitForResponse(r => r.url().match(new RegExp(`${LIST_API}/[^/]+$`)) !== null);
        await firstRow.locator('img.asset-thumb').first().click();
        await detailResponse;

        await expect(page.locator('#assetPreviewModal')).toBeVisible();
    });

    test('반려 모달이 열리고 닫혀야 한다', async ({ page }) => {
        await page.goto(PAGE_URL);
        await page.waitForResponse(r => r.url().includes(LIST_API));

        const rejectBtn = page.locator('#assetApprovalTableBody button:has-text("반려")').first();
        const hasPending = await rejectBtn.count();
        if (hasPending === 0) {
            test.skip(true, '시드에 PENDING + CMS:W 권한이 없어 반려 모달 테스트 스킵');
            return;
        }

        await rejectBtn.click();
        await expect(page.locator('#assetRejectModal')).toBeVisible();
        await expect(page.locator('#rejectReason')).toBeVisible();

        await page.locator('#assetRejectModal button:has-text("취소")').click();
        await expect(page.locator('#assetRejectModal')).not.toBeVisible();
    });
});
