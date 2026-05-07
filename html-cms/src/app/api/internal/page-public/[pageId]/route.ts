/**
 * @file src/app/api/internal/page-public/[pageId]/route.ts
 * @description 내부 전용 — IS_PUBLIC 단건 조회 API.
 *              middleware.ts 에서 /deployed/*.html 접근 시 긴급차단 여부를 확인하는 용도로만 사용한다.
 *              x-internal-secret 헤더 검증으로 외부 직접 호출을 차단한다.
 * @returns {{ isPublic: 'Y' | 'N' }}
 */

import { NextRequest } from 'next/server';
import oracledb from 'oracledb';

import { getConnection } from '@/db/connection';
import { errorResponse, successResponse } from '@/lib/api-response';

const INTERNAL_SECRET = process.env.INTERNAL_API_SECRET ?? '';

export async function GET(req: NextRequest, { params }: { params: Promise<{ pageId: string }> }) {
    // 내부 호출 전용 — 시크릿 헤더 불일치 시 404로 위장하여 엔드포인트 존재 자체를 숨김
    const secret = req.headers.get('x-internal-secret') ?? '';
    if (!INTERNAL_SECRET || secret !== INTERNAL_SECRET) {
        return errorResponse('Not Found', 404);
    }

    const { pageId } = await params;

    // pageId 유효성 검사 — 영숫자·하이픈·언더스코어만 허용 (경로 트래버설 방지)
    if (!/^[a-zA-Z0-9_-]+$/.test(pageId)) {
        return errorResponse('유효하지 않은 pageId입니다.', 400);
    }

    const conn = await getConnection();
    try {
        const result = await conn.execute<{ IS_PUBLIC: string }>(
            `SELECT IS_PUBLIC FROM SPW_CMS_PAGE WHERE PAGE_ID = :pageId AND USE_YN = 'Y'`,
            { pageId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT },
        );
        const row = result.rows?.[0];
        // 페이지가 없는 경우 'Y'(정상) 반환 — fail-open: 조회 실패 시 접근 차단하지 않음
        return successResponse({ isPublic: row?.IS_PUBLIC ?? 'Y' });
    } finally {
        await conn.close();
    }
}
