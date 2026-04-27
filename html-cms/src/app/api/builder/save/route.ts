// src/app/api/builder/save/route.ts

import { NextRequest } from 'next/server';

import { updatePage, createPage, getPageById, resetApproveStateToWork } from '@/db/repository/page.repository';
import { canAccessCmsEdit, canManageCmsPage, getCurrentUser } from '@/lib/current-user';
import { isValidBankId, isPageExpired } from '@/lib/validators';
import { successResponse, contentBuilderErrorResponse, getErrorMessage } from '@/lib/api-response';
import { nextApi } from '@/lib/api-url';

// 페이지 저장: DB PAGE_HTML에 HTML 직접 저장
async function savePage(
    bank: string,
    html: string,
    pageName?: string,
    viewMode?: string,
    thumbnail?: string,
    templateId?: string,
    skipExistingLookup = false,
): Promise<void> {
    const currentUser = await getCurrentUser();
    if (!canAccessCmsEdit(currentUser)) {
        throw new Error('권한이 없습니다.');
    }
    const { userId, userName } = currentUser;

    // 1. 기존 페이지 확인 + 만료 체크
    const existing = skipExistingLookup ? null : await getPageById(bank);
    if (existing && isPageExpired(existing.IS_PUBLIC, existing.EXPIRED_DATE)) {
        throw new Error('만료된 페이지는 수정할 수 없습니다.');
    }
    if (existing && !canManageCmsPage(currentUser, existing.CREATE_USER_ID)) {
        throw new Error('권한이 없습니다.');
    }

    // 2. DB 저장 (PAGE_HTML CLOB에 직접 기록)
    if (existing) {
        await updatePage({
            pageId: bank,
            pageName: pageName,
            viewMode: viewMode as 'mobile' | 'web' | 'responsive' | undefined,
            pageHtml: html,
            thumbnail,
            lastModifierId: userId,
            lastModifierName: userName,
        });

        // 3. 승인/반려 상태이면 WORK로 전환 (재승인 플로우)
        await resetApproveStateToWork(bank, userId);
    } else {
        let pageHtml = html;
        if (templateId) {
            const template = await getPageById(templateId);
            if (!template || template.PAGE_TYPE !== 'TEMPLATE') {
                throw new Error('유효하지 않은 템플릿입니다.');
            }
            const sameViewMode =
                !viewMode || template.VIEW_MODE === viewMode || (viewMode === 'web' && template.VIEW_MODE === 'PC');
            if (!sameViewMode) {
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
}

export async function POST(req: NextRequest) {
    try {
        const body = await req.json();
        const { html, pageName, viewMode, thumbnail, templateId } = body;

        // bank 미전달 또는 유효하지 않으면 서버에서 UUID 생성 (신규 페이지)
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
        console.error('페이지 저장 실패:', err);
        return contentBuilderErrorResponse(getErrorMessage(err));
    }
}
