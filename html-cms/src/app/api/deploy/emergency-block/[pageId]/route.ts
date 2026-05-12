// src/app/api/deploy/emergency-block/[pageId]/route.ts
// 긴급차단 API — IS_PUBLIC='N' 업데이트 + 배포된 HTML 파일 삭제
// 호출 주체: spider-admin (서버간 통신) → x-deploy-token 인증

import { timingSafeEqual } from 'crypto';

import { NextRequest } from 'next/server';

import { getServerList } from '@/db/repository/file-send.repository';
import { getPageById, setPagePublic } from '@/db/repository/page.repository';
import { errorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { buildServerUrl, deleteFromServer } from '@/lib/deploy-utils';
import { DEPLOY_SECRET } from '@/lib/env';

/** 타이밍 공격 방지 토큰 비교 */
function isValidToken(token: string | null): boolean {
    if (!DEPLOY_SECRET || !token) return false;
    try {
        const expected = Buffer.from(DEPLOY_SECRET, 'utf8');
        const received = Buffer.from(token, 'utf8');
        if (expected.length !== received.length) return false;
        return timingSafeEqual(expected, received);
    } catch {
        return false;
    }
}

export async function POST(req: NextRequest, { params }: { params: Promise<{ pageId: string }> }) {
    // spider-admin → html-cms 서버간 호출: x-deploy-token으로 인증
    if (!isValidToken(req.headers.get('x-deploy-token'))) {
        return errorResponse('인증 토큰이 유효하지 않습니다.', 401);
    }

    try {
        const { pageId } = await params;
        if (!pageId || typeof pageId !== 'string') {
            return errorResponse('pageId가 필요합니다.', 400);
        }

        const page = await getPageById(pageId);
        if (!page) {
            return errorResponse('페이지를 찾을 수 없습니다.', 404);
        }
        if (page.IS_PUBLIC === 'N') {
            return errorResponse('이미 긴급차단된 페이지입니다.', 400);
        }

        // DB IS_PUBLIC='N' 업데이트 (호출자 식별: spider-admin 서버간 요청)
        await setPagePublic(pageId, 'N', 'SYSTEM');

        // 활성 서버의 배포 파일 삭제
        const servers = await getServerList('Y');
        const results = await Promise.all(
            servers.map(async (server) => {
                const serverUrl = buildServerUrl(server.INSTANCE_IP, server.INSTANCE_PORT, '/cms/api/deploy/receive');
                try {
                    await deleteFromServer(serverUrl, pageId);
                    return { instanceId: server.INSTANCE_ID, success: true as const };
                } catch (err: unknown) {
                    console.error(`파일 삭제 실패 [${server.INSTANCE_ID}]:`, err);
                    return { instanceId: server.INSTANCE_ID, success: false as const, error: getErrorMessage(err) };
                }
            }),
        );

        return successResponse({ pageId, results, message: '긴급차단이 완료되었습니다.' });
    } catch (err: unknown) {
        console.error('긴급차단 처리 실패:', err);
        return errorResponse(getErrorMessage(err));
    }
}
