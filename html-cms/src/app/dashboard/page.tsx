// src/app/dashboard/page.tsx
// 사용자 대시보드 — 페이지 목록 카드 그리드

import { existsSync } from 'fs';
import { join } from 'path';

import { getPageList } from '@/db/repository/page.repository';
import { canAccessCmsDashboard, canManageCmsPage, getCurrentUser } from '@/lib/current-user';
import { redirect } from 'next/navigation';
import { isPageExpired } from '@/lib/validators';
import { getApproveLabels } from '@/data/approve-config';
import DashboardClient from '@/components/dashboard/DashboardClient';
import type { ViewMode } from '@/db/types';

const PAGE_SIZE = 12;

const VIEW_MODE_VALUES: ViewMode[] = ['mobile', 'web', 'responsive'];

export const dynamic = 'force-dynamic';

export default async function DashboardPage({
    searchParams,
}: {
    searchParams: Promise<{ page?: string; search?: string; sortBy?: string; viewMode?: string }>;
}) {
    const { page: pageParam, search: searchParam, sortBy: sortByParam, viewMode: viewModeParam } = await searchParams;

    const currentUser = await getCurrentUser();
    if (!canAccessCmsDashboard(currentUser)) {
        redirect('/not-authorized');
    }

    const currentPage = Math.max(1, parseInt(pageParam ?? '1', 10));
    const search = searchParam ?? '';
    const sortBy = sortByParam === 'name' ? 'name' : 'date';
    const viewMode = VIEW_MODE_VALUES.includes(viewModeParam as ViewMode) ? (viewModeParam as ViewMode) : undefined;

    // Oracle 비활성화(ORACLE_DISABLED=true) 또는 DB 연결 실패 시 빈 목록으로 폴백
    const [pageListResult, approveLabels] = await Promise.all([
        getPageList({
            createUserId: currentUser.userId,
            page: currentPage,
            pageSize: PAGE_SIZE,
            search: search || undefined,
            sortBy,
            viewMode,
        }).catch(() => ({ list: [], totalCount: 0 })),
        getApproveLabels(),
    ]);
    const { list, totalCount } = pageListResult;

    const pages = list.map((p) => ({
        id: p.PAGE_ID,
        label: p.PAGE_NAME,
        viewMode: (p.VIEW_MODE ?? 'mobile') as ViewMode,
        thumbnail: p.THUMBNAIL ?? null,
        lastModifiedDtime: p.LAST_MODIFIED_DTIME ? new Date(p.LAST_MODIFIED_DTIME).toISOString() : null,
        approveState: p.APPROVE_STATE,
        beginningDate: p.BEGINNING_DATE ? new Date(p.BEGINNING_DATE).toLocaleDateString('en-CA') : null,
        expiredDate: p.EXPIRED_DATE ? new Date(p.EXPIRED_DATE).toLocaleDateString('en-CA') : null,
        rejectedReason: p.REJECTED_REASON ?? null,
        hasFile:
            p.PAGE_HTML != null ||
            !p.FILE_PATH ||
            existsSync(join(process.cwd(), 'public', p.FILE_PATH.replace(/^\//, ''))),
        isExpired: isPageExpired(p.IS_PUBLIC, p.EXPIRED_DATE),
        isPublic: p.IS_PUBLIC ?? 'Y',
    }));

    return (
        <DashboardClient
            userId={currentUser.userId}
            initialPages={pages}
            totalCount={totalCount}
            currentPage={currentPage}
            search={search}
            sortBy={sortBy}
            viewMode={viewMode ?? null}
            canWrite={canManageCmsPage(currentUser, currentUser.userId)}
            approveLabels={approveLabels}
        />
    );
}
