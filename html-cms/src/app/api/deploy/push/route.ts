// src/app/api/deploy/push/route.ts
import { timingSafeEqual } from 'crypto';
import { readFile, access } from 'fs/promises';
import path from 'path';

import { NextRequest } from 'next/server';
import oracledb from 'oracledb';

import { getConnection } from '@/db/connection';
import { PAGE_SELECT_BY_ID } from '@/db/queries/page.sql';
import { upsertFileSend, getServerList } from '@/db/repository/file-send.repository';
import { updatePageDeploy, getLatestHistory, getHistoryVersionByFilePath } from '@/db/repository/page.repository';
import type { CmsPage } from '@/db/types';
import { canWriteCms, getCurrentUser } from '@/lib/current-user';
import { errorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { DEPLOY_SECRET } from '@/lib/env';
import { sendToServer, buildServerUrl, buildDeployHtml, calcCrc } from '@/lib/deploy-utils';

const OBJ = { outFormat: oracledb.OUT_FORMAT_OBJECT };

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

export async function POST(req: NextRequest) {
    try {
        // 인증 — x-deploy-token(서버 간 호출) 또는 세션 권한(브라우저 UI) 중 하나 통과
        const tokenValid = isValidToken(req.headers.get('x-deploy-token'));
        let userId: string;

        if (tokenValid) {
            // 서버 간 호출: 요청 바디에서 userId 수신
            const body = (await req.json()) as { pageId?: string; userId?: string };
            if (!body.userId || typeof body.userId !== 'string') {
                return errorResponse('서버 간 호출 시 userId가 필요합니다.', 400);
            }
            userId = body.userId;

            const { pageId } = body;
            return await processDeploy(pageId, userId);
        }

        // 브라우저 UI 호출: 세션 권한 체크
        const currentUser = await getCurrentUser();
        if (!canWriteCms(currentUser)) {
            return errorResponse('권한이 없습니다.', 401);
        }
        userId = currentUser.userId;

        const { pageId } = (await req.json()) as { pageId?: string };
        return await processDeploy(pageId, userId);
    } catch (err: unknown) {
        console.error('배포 요청 처리 실패:', err);
        return errorResponse(getErrorMessage(err));
    }
}

async function processDeploy(pageId: string | undefined, userId: string) {
    if (!pageId || typeof pageId !== 'string') {
        return errorResponse('pageId가 필요합니다.', 400);
    }

    // 1. 페이지 조회 — APPROVED 상태 확인
    const conn = await getConnection();
    let page: CmsPage | null = null;
    try {
        const result = await conn.execute<CmsPage>(PAGE_SELECT_BY_ID, { pageId }, OBJ);
        page = result.rows?.[0] ?? null;
    } finally {
        await conn.close();
    }

    if (!page) {
        return errorResponse('페이지를 찾을 수 없습니다.', 404);
    }
    if (page.APPROVE_STATE !== 'APPROVED') {
        return errorResponse('승인된 페이지만 배포할 수 있습니다.', 400);
    }

    // 2. HTML 읽기 — DB PAGE_HTML 우선, FILE_PATH 폴백
    let rawHtml = page.PAGE_HTML ?? null;
    if (!rawHtml && page.FILE_PATH) {
        const normalizedFilePath = page.FILE_PATH.replace(/^\//, '');
        if (normalizedFilePath.includes('..') || path.isAbsolute(normalizedFilePath)) {
            return errorResponse('유효하지 않은 파일 경로입니다.', 400);
        }
        const absolutePath = path.join(process.cwd(), 'public', normalizedFilePath);
        try {
            await access(absolutePath);
        } catch {
            return errorResponse(
                '페이지 HTML이 DB에 없고, 파일도 서버에 존재하지 않습니다. 에디터에서 저장 후 다시 시도해 주세요.',
                400,
            );
        }
        rawHtml = await readFile(absolutePath, 'utf8');
    }

    if (!rawHtml) {
        return errorResponse('배포할 HTML 콘텐츠가 없습니다.', 400);
    }

    // 프래그먼트 → 완전한 HTML 문서 조립 (CSS 인라인 + 경로 치환 + 트래커 포함)
    const pageName = page.PAGE_NAME ?? pageId;
    const html = await buildDeployHtml(rawHtml, pageId, pageName);
    const crcValue = calcCrc(html);

    // 트래커 JS 파일 읽기 (운영 서버로 함께 Push)
    const trackerJsPath = path.join(process.cwd(), 'public', 'cms-tracker.js');
    let trackerJs: string | null = null;
    try {
        trackerJs = await readFile(trackerJsPath, 'utf8');
    } catch {
        console.warn('cms-tracker.js 파일을 찾을 수 없습니다. 트래커 없이 배포합니다.');
    }

    // 3. ALIVE_YN='Y' 서버 목록 조회
    const servers = await getServerList('Y');
    if (servers.length === 0) {
        return errorResponse('활성화된 배포 서버가 없습니다.', 400);
    }

    // 4. 현재 FILE_PATH에 해당하는 HISTORY VERSION 조회 (롤백 대응)
    const historyVersion = page.FILE_PATH ? await getHistoryVersionByFilePath(pageId, page.FILE_PATH) : null;
    const latestHistory = await getLatestHistory(pageId);
    const version = historyVersion ?? latestHistory?.VERSION ?? 1;
    const fileId = `${pageId}_v${version}.html`;

    // 5. 각 서버에 병렬 전송 + 이력 기록
    const results = await Promise.all(
        servers.map(async (server) => {
            const serverUrl = buildServerUrl(server.INSTANCE_IP, server.INSTANCE_PORT, '/cms/api/deploy/receive');
            try {
                await sendToServer(serverUrl, pageId, html, trackerJs);
                await upsertFileSend({
                    instanceId: server.INSTANCE_ID,
                    fileId,
                    fileSize: Buffer.byteLength(html, 'utf8'),
                    fileCrcValue: crcValue,
                    lastModifierId: userId,
                });
                return { instanceId: server.INSTANCE_ID, success: true as const };
            } catch (err: unknown) {
                console.error(`서버 전송 실패 [${server.INSTANCE_ID}]:`, err);
                return { instanceId: server.INSTANCE_ID, success: false as const, error: getErrorMessage(err) };
            }
        }),
    );

    // 6. 하나 이상 성공 시 페이지 배포 기록 갱신
    const successCount = results.filter((r) => r.success).length;
    if (successCount > 0) {
        await updatePageDeploy(pageId, crcValue, userId);
    }

    return successResponse({
        fileId,
        crcValue,
        successCount,
        failCount: results.length - successCount,
        results,
    });
}
