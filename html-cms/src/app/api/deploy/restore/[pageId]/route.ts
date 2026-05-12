// src/app/api/deploy/restore/[pageId]/route.ts
// 긴급차단 복구 API — SPW_CMS_PAGE_HISTORY 마지막 버전 HTML로 파일 재생성 + IS_PUBLIC='Y'
// 현재 페이지(SPW_CMS_PAGE)의 APPROVE_STATE·PAGE_HTML은 건드리지 않음
// 호출 주체: spider-admin (서버간 통신) → x-deploy-token 인증

import { readFile } from 'fs/promises';
import { timingSafeEqual } from 'crypto';
import path from 'path';

import { NextRequest } from 'next/server';

import { getServerList } from '@/db/repository/file-send.repository';
import { getPageById, getLatestHistory, setPagePublic } from '@/db/repository/page.repository';
import { errorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { buildServerUrl, sendToServer, buildDeployHtml } from '@/lib/deploy-utils';
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
        if (page.IS_PUBLIC !== 'N') {
            return errorResponse('긴급차단 상태가 아닌 페이지입니다.', 400);
        }

        // 마지막 승인 이력 조회 — HISTORY는 APPROVED 시에만 생성되므로 latest == last approved
        const history = await getLatestHistory(pageId);
        if (!history?.PAGE_HTML) {
            return errorResponse('복구할 승인 이력이 없습니다. 먼저 승인 후 배포하세요.', 400);
        }

        const pageName = page.PAGE_NAME ?? pageId;
        const html = await buildDeployHtml(history.PAGE_HTML as string, pageId, pageName);

        // 트래커 JS 읽기 (없어도 배포 진행)
        const trackerJsPath = path.join(process.cwd(), 'public', 'cms-tracker.js');
        let trackerJs: string | null = null;
        try {
            trackerJs = await readFile(trackerJsPath, 'utf8');
        } catch {
            console.warn('cms-tracker.js 파일을 찾을 수 없습니다. 트래커 없이 복구합니다.');
        }

        const servers = await getServerList('Y');
        if (servers.length === 0) {
            return errorResponse('활성화된 배포 서버가 없습니다.', 400);
        }

        // 각 서버에 HTML 파일 재생성
        const results = await Promise.all(
            servers.map(async (server) => {
                const serverUrl = buildServerUrl(server.INSTANCE_IP, server.INSTANCE_PORT, '/cms/api/deploy/receive');
                try {
                    await sendToServer(serverUrl, pageId, html, trackerJs);
                    return { instanceId: server.INSTANCE_ID, success: true as const };
                } catch (err: unknown) {
                    console.error(`파일 복구 실패 [${server.INSTANCE_ID}]:`, err);
                    return { instanceId: server.INSTANCE_ID, success: false as const, error: getErrorMessage(err) };
                }
            }),
        );

        const successCount = results.filter((r) => r.success).length;
        if (successCount === 0) {
            return errorResponse('모든 서버에 복구 파일 전송이 실패했습니다.', 500);
        }

        // 하나 이상 성공 시 IS_PUBLIC='Y' 복원 (호출자 식별: spider-admin 서버간 요청)
        await setPagePublic(pageId, 'Y', 'SYSTEM');

        return successResponse({
            pageId,
            historyVersion: history.VERSION,
            successCount,
            failCount: results.length - successCount,
            results,
            message: '복구가 완료되었습니다.',
        });
    } catch (err: unknown) {
        console.error('복구 처리 실패:', err);
        return errorResponse(getErrorMessage(err));
    }
}
