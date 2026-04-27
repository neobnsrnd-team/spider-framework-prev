import crypto, { timingSafeEqual } from 'crypto';
import { mkdir, unlink, writeFile } from 'fs/promises';
import { dirname, join } from 'path';

import { NextRequest } from 'next/server';

import { createAsset } from '@/db/repository/asset.repository';
import { contentBuilderErrorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { normalizeCmsAssetCategory } from '@/lib/codes';
import { canAccessCmsEdit, getCurrentUser } from '@/lib/current-user';
import { ASSET_BASE_URL, ASSET_UPLOAD_DIR, DEPLOY_SECRET, SERVER_MODE } from '@/lib/env';

/**
 * Admin 백엔드 등 서버 간 호출 시 x-deploy-token 헤더로 인증한다.
 * 타이밍 공격 방지를 위해 timingSafeEqual로 비교한다.
 */
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
    if (SERVER_MODE === 'operation') {
        return contentBuilderErrorResponse('이미지 업로드는 관리자 서버에서만 가능합니다.');
    }

    const formData = await req.formData();
    const file = formData.get('file') as File | null;

    if (!file) {
        return contentBuilderErrorResponse('File is required.');
    }

    const bodyUserId = formData.get('userId')?.toString() || null;
    const bodyUserName = formData.get('userName')?.toString() || null;
    const businessCategoryInput = formData.get('businessCategory')?.toString() || null;
    const assetDesc = formData.get('assetDesc')?.toString() || null;
    // 사용자가 직접 입력한 표시명. Admin 서버에서 항상 채워서 전달하므로 없을 경우 원본 파일명 폴백
    const assetNameInput = formData.get('assetName')?.toString().trim() || null;

    try {
        const deployTokenValid = isValidToken(req.headers.get('x-deploy-token'));

        let userId: string;
        let userName: string;

        if (deployTokenValid) {
            // 서버 간 호출(Admin 백엔드): 세션 쿠키 없이 form data의 userId/userName을 직접 사용
            if (!bodyUserId) {
                return contentBuilderErrorResponse('서버 간 호출 시 userId가 필요합니다.');
            }
            userId = bodyUserId;
            userName = bodyUserName ?? bodyUserId;
        } else {
            // 브라우저 세션 호출: 기존 인증 흐름 유지
            const currentUser = await getCurrentUser();
            if (!canAccessCmsEdit(currentUser)) {
                return contentBuilderErrorResponse('Permission denied.');
            }
            userId = bodyUserId ?? currentUser.userId;
            userName = bodyUserId ? (bodyUserName ?? bodyUserId) : currentUser.userName;
        }
        const businessCategory = await normalizeCmsAssetCategory(businessCategoryInput);

        const buffer = Buffer.from(await file.arrayBuffer());
        const assetId = crypto.randomUUID();

        console.log('[upload route] assetNameInput=%s, file.name=%s', assetNameInput, file.name);
        // 표시명(DB 저장): 사용자 입력명 우선, 없으면 원본 파일명 그대로 사용 (한글 허용)
        const assetName = assetNameInput || file.name;

        // 파일시스템 저장명: 영문·숫자·점·하이픈만 허용하여 OS 경로 안전성 확보
        const safeFilename = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
        const filename = `${assetId}_${safeFilename}`;
        const filepath = join(ASSET_UPLOAD_DIR, filename);
        await mkdir(dirname(filepath), { recursive: true });
        await writeFile(filepath, buffer);
        const assetUrl = `${ASSET_BASE_URL}/${filename}`;

        try {
            await createAsset({
                assetId,
                assetName,
                businessCategory,
                mimeType: file.type || 'application/octet-stream',
                fileSize: buffer.length,
                assetPath: filepath,
                assetUrl,
                assetDesc: assetDesc ?? undefined,
                createUserId: userId,
                createUserName: userName,
            });
        } catch (dbErr: unknown) {
            await unlink(filepath).catch(() => {});
            throw dbErr;
        }

        return successResponse({ url: assetUrl, assetId }, 201);
    } catch (err: unknown) {
        if (err instanceof Error && err.message === '유효하지 않은 이미지 카테고리입니다.') {
            return contentBuilderErrorResponse(err.message);
        }
        console.error('File upload failed:', err);
        return contentBuilderErrorResponse(getErrorMessage(err));
    }
}
