// GET /api/cms-admin/asset-categories
// FWK_CODE(CODE_GROUP_ID='CMS00001', USE_YN='Y')에 등록된 이미지 업무 카테고리 코드를
// SORT_ORDER 오름차순으로 그대로 반환한다. (spider-admin /cms-admin/asset-approvals 와 동일 정책)
//
// 화이트리스트·하드코딩 폴백을 두지 않으므로 운영에서 FWK_CODE 변경이 즉시 화면에 반영된다.
// Next.js 프레임워크 캐시는 dynamic = 'force-dynamic' 으로 비활성화한다.

import { errorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { CMS_ASSET_CATEGORY_GROUP_ID } from '@/lib/cms-asset-category';
import { getCodesByGroup } from '@/lib/codes';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export async function GET() {
    try {
        const categories = await getCodesByGroup(CMS_ASSET_CATEGORY_GROUP_ID);
        return successResponse({ categories });
    } catch (err) {
        return errorResponse(getErrorMessage(err), 500);
    }
}
