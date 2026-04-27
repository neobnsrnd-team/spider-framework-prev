// src/app/api/assets/[assetId]/route.ts
// 에셋 단건 삭제 API

import { unlink } from 'fs/promises';

import { NextRequest } from 'next/server';

import { deleteAsset, getAssetById, hardDeleteAsset } from '@/db/repository/asset.repository';
import { errorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { canWriteCms, getCurrentUser } from '@/lib/current-user';
import { isValidDeployToken } from '@/lib/server-auth';

/**
 * DELETE /api/assets/:assetId — 에셋 삭제
 * - APPROVED: 논리 삭제 (USE_YN = 'N') — 페이지 참조 보존
 * - WORK/PENDING/REJECTED: 물리 삭제 (DB row 완전 제거)
 * - 모든 상태에서 물리 파일은 제거
 * - x-deploy-token 헤더 유효 시 세션 없이 서버 간 호출 허용 (Admin 백엔드용)
 */
export async function DELETE(req: NextRequest, { params }: { params: Promise<{ assetId: string }> }) {
    try {
        const { assetId } = await params;

        const asset = await getAssetById(assetId);
        if (!asset) {
            return errorResponse('에셋을 찾을 수 없습니다.', 404);
        }

        let userId: string;
        let userName: string;

        if (isValidDeployToken(req.headers.get('x-deploy-token'))) {
            // 서버 간 호출(Admin 백엔드): 세션 없이 시스템 계정으로 처리
            userId = 'SYSTEM';
            userName = 'SYSTEM';
        } else {
            const currentUser = await getCurrentUser();
            if (!canWriteCms(currentUser)) {
                return errorResponse('Permission denied.', 403);
            }
            userId = currentUser.userId;
            userName = currentUser.userName;
        }

        // 상태별 DB 삭제 처리
        if (asset.ASSET_STATE === 'APPROVED') {
            await deleteAsset(assetId, userId, userName);
        } else {
            await hardDeleteAsset(assetId);
        }

        // 물리 파일 삭제 (없어도 무시)
        if (asset.ASSET_PATH) {
            await unlink(asset.ASSET_PATH).catch(() => {});
        }

        return successResponse({ deleted: assetId, hardDeleted: asset.ASSET_STATE !== 'APPROVED' });
    } catch (err: unknown) {
        console.error('에셋 삭제 실패:', err);
        return errorResponse(getErrorMessage(err));
    }
}
