// src/app/api/builder/save/route.ts

import { NextRequest, NextResponse } from 'next/server';

import { createPage, getPageById, resetApproveStateToWork, updatePage } from '@/db/repository/page.repository';
import { contentBuilderErrorResponse, getErrorMessage, successResponse } from '@/lib/api-response';
import { nextApi } from '@/lib/api-url';
import { canAccessCmsEdit, canManageCmsPage, getCurrentUser } from '@/lib/current-user';
import { isValidBankId, isPageExpired } from '@/lib/validators';
import { isTemplateCompatibleViewMode } from '@/lib/view-mode';

class PageNotFoundError extends Error {}

async function savePage(
    bank: string,
    html: string,
    pageName?: string,
    viewMode?: string,
    thumbnail?: string,
    templateId?: string,
    allowCreate = false,
): Promise<void> {
    const currentUser = await getCurrentUser();
    if (!canAccessCmsEdit(currentUser)) {
        throw new Error('권한이 없습니다.');
    }
    const { userId, userName } = currentUser;

    const existing = allowCreate ? null : await getPageById(bank);
    if (!existing && !allowCreate) {
        throw new PageNotFoundError('페이지를 찾을 수 없습니다.');
    }
    if (existing && isPageExpired(existing.IS_PUBLIC, existing.EXPIRED_DATE)) {
        throw new Error('만료된 페이지는 수정할 수 없습니다.');
    }
    if (existing && !canManageCmsPage(currentUser, existing.CREATE_USER_ID)) {
        throw new Error('권한이 없습니다.');
    }

    if (existing) {
        await updatePage({
            pageId: bank,
            pageName,
            viewMode: viewMode as 'mobile' | 'web' | 'responsive' | undefined,
            pageHtml: html,
            thumbnail,
            lastModifierId: userId,
            lastModifierName: userName,
        });

        await resetApproveStateToWork(bank, userId);
        return;
    }

    let pageHtml = html;
    if (templateId) {
        const template = await getPageById(templateId);
        if (!template || template.PAGE_TYPE !== 'TEMPLATE') {
            throw new Error('유효하지 않은 템플릿입니다.');
        }
        if (!isTemplateCompatibleViewMode(viewMode, template.VIEW_MODE)) {
            throw new Error('선택한 레이아웃과 템플릿의 레이아웃이 일치하지 않습니다.');
        }
        pageHtml = template.PAGE_HTML ?? '';
    }

    await createPage({
        pageId: bank,
        pageName: pageName ?? bank,
        viewMode: (viewMode as 'mobile' | 'web' | 'responsive') ?? 'mobile',
        pageHtml,
        thumbnail,
        createUserId: userId,
        createUserName: userName,
        templateId,
    });
}

export async function POST(req: NextRequest) {
    try {
        const body = await req.json();
        const { html, pageName, viewMode, thumbnail, templateId } = body;

        const hasValidBank = isValidBankId(body.bank);
        const bank = hasValidBank ? body.bank : crypto.randomUUID();

        if (html === undefined || html === null) {
            return contentBuilderErrorResponse('HTML 콘텐츠가 없습니다.');
        }

        await savePage(
            bank,
            html,
            typeof pageName === 'string' ? pageName : undefined,
            typeof viewMode === 'string' ? viewMode : undefined,
            typeof thumbnail === 'string' ? thumbnail : undefined,
            typeof templateId === 'string' && templateId !== 'blank' ? templateId : undefined,
            !hasValidBank,
        );

        return successResponse({ pageId: bank, editorUrl: nextApi(`/edit?bank=${encodeURIComponent(bank)}`) });
    } catch (err: unknown) {
        if (err instanceof PageNotFoundError) {
            return NextResponse.json({ ok: false, error: err.message, errorCode: 'PAGE_NOT_FOUND' });
        }

        console.error('페이지 저장 실패:', err);
        return contentBuilderErrorResponse(getErrorMessage(err));
    }
}
