import crypto from 'crypto';
import { mkdir, unlink, writeFile } from 'fs/promises';
import { dirname, join } from 'path';

import { NextRequest } from 'next/server';

import { createAsset } from '@/db/repository/asset.repository';
import { contentBuilderErrorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { normalizeCmsAssetCategory } from '@/lib/codes';
import { canAccessCmsEdit, getCurrentUser } from '@/lib/current-user';
import { ASSET_BASE_URL, ASSET_UPLOAD_DIR, SERVER_MODE } from '@/lib/env';
import { isValidDeployToken } from '@/lib/server-auth';

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

    try {
        const deployTokenValid = isValidDeployToken(req.headers.get('x-deploy-token'));

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
        const assetName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');

        const filename = `${assetId}_${assetName}`;
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
