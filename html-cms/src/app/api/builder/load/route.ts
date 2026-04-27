// src/app/api/builder/load/route.ts

import { NextRequest, NextResponse } from 'next/server';

import { getPageById } from '@/db/repository/page.repository';
import { isValidBankId } from '@/lib/validators';
import { readPageHtml } from '@/lib/page-file';
import { contentBuilderErrorResponse, getErrorMessage } from '@/lib/api-response';
import { canAccessCmsEdit, canManageCmsPage, getCurrentUser } from '@/lib/current-user';

function durationMs(start: number) {
    return Math.round((performance.now() - start) * 10) / 10;
}

// DB PAGE_HTML 우선 → FILE_PATH 폴백 → PAGE_DESC 폴백
async function loadPage(bank: string): Promise<{
    html: string;
    updated: string | null;
    pageMissing?: boolean;
    fileNotFound?: boolean;
    pageName?: string;
    viewMode?: string;
    createUserId?: string | null;
    timing: {
        dbMs: number;
        fileMs: number;
    };
}> {
    const dbStart = performance.now();
    const page = await getPageById(bank);
    const dbMs = durationMs(dbStart);
    if (!page) {
        return { html: '', updated: null, pageMissing: true, createUserId: null, timing: { dbMs, fileMs: 0 } };
    }

    let html = '';
    let fileNotFound = false;
    let fileMs = 0;

    // DB PAGE_HTML 우선 (getPageById의 SELECT *에 이미 포함)
    if (page.PAGE_HTML != null) {
        html = page.PAGE_HTML;
    } else if (page.FILE_PATH) {
        // FILE_PATH 폴백 (기존 데이터 호환)
        const fileStart = performance.now();
        const content = await readPageHtml(page.FILE_PATH);
        fileMs = durationMs(fileStart);
        if (content !== null) {
            html = content;
        } else {
            fileNotFound = true;
        }
    } else {
        // 마이그레이션 이전 데이터: PAGE_DESC 폴백
        html = page.PAGE_DESC ?? '';
    }

    const updated = page.LAST_MODIFIED_DTIME ? new Date(page.LAST_MODIFIED_DTIME).toISOString() : null;

    return {
        html,
        updated,
        fileNotFound,
        pageName: page.PAGE_NAME,
        viewMode: page.VIEW_MODE ?? 'mobile',
        createUserId: page.CREATE_USER_ID ?? null,
        timing: { dbMs, fileMs },
    };
}

export async function POST(req: NextRequest) {
    const totalStart = performance.now();
    try {
        const authStart = performance.now();
        const currentUser = await getCurrentUser();
        const authMs = durationMs(authStart);
        if (!canAccessCmsEdit(currentUser)) {
            return contentBuilderErrorResponse('Permission denied.');
        }

        const parseStart = performance.now();
        const body = await req.json().catch(() => ({}));
        const bank = isValidBankId(body.bank) ? body.bank : 'ibk';
        const parseMs = durationMs(parseStart);

        const loadStart = performance.now();
        const { html, updated, pageMissing, fileNotFound, pageName, viewMode, createUserId, timing } = await loadPage(bank);
        const loadMs = durationMs(loadStart);
        const totalMs = durationMs(totalStart);

        if (createUserId && !canManageCmsPage(currentUser, createUserId)) {
            return contentBuilderErrorResponse('Permission denied.');
        }

        return NextResponse.json(
            {
                ok: true,
                html,
                updated,
                pageMissing,
                fileNotFound,
                pageName,
                viewMode,
                _timing: {
                    totalMs,
                    authMs,
                    parseMs,
                    loadMs,
                    dbMs: timing.dbMs,
                    fileMs: timing.fileMs,
                    htmlBytes: new TextEncoder().encode(html).length,
                },
            },
            {
                headers: {
                    'Server-Timing': `auth;dur=${authMs}, parse;dur=${parseMs}, load;dur=${loadMs}, total;dur=${totalMs}`,
                },
            },
        );
    } catch (err: unknown) {
        console.error('페이지 로드 실패:', err);
        return contentBuilderErrorResponse(getErrorMessage(err));
    }
}
