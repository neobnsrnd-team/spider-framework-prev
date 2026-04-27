// ============================================================================
// SPW_CMS_PAGE + SPW_CMS_PAGE_HISTORY — Repository
// ============================================================================
// W-6 해결: VIEW_MODE 추가, HISTORY INSERT 타이밍 변경 (승인 시에만), SNAPSHOT_DTIME 제거
// 설계 원칙: PAGE는 최신 상태 조회용 (INSERT 1회 + UPDATE 반복)
//           PAGE_HISTORY는 승인(APPROVED) 시에만 VERSION INSERT

import oracledb from 'oracledb';
import { getConnection, withTransaction, clobBind } from '@/db/connection';
import type { CmsPage, CmsPageHistory, ApproveState, ViewMode, PageType } from '@/db/types';
import {
    PAGE_SELECT_BY_ID,
    PAGE_SELECT_LIST,
    PAGE_COUNT,
    PAGE_INSERT,
    PAGE_UPDATE,
    PAGE_REQUEST_APPROVAL,
    PAGE_UPDATE_APPROVE_STATE,
    PAGE_SOFT_DELETE,
    PAGE_HARD_DELETE,
    COMP_MAP_DELETE_BY_PAGE,
    PAGE_RESET_TO_WORK,
    PAGE_UPDATE_DATES,
    PAGE_SELECT_EXPIRED,
    PAGE_UPDATE_IS_PUBLIC,
    PAGE_EXPIRE,
    PAGE_UPDATE_DEPLOY,
    PAGE_ROLLBACK,
    PAGE_SELECT_AB_GROUP,
    PAGE_SELECT_HTML_BY_ID,
    PAGE_SELECT_TEMPLATE_LIST,
    PAGE_UPDATE_HTML,
    PAGE_UPDATE_AB_GROUP,
    PAGE_CLEAR_AB_GROUP,
    PAGE_CLEAR_PAGE_AB_GROUP,
    PAGE_PROMOTE_WINNER,
    PAGE_SET_WINNER,
} from '@/db/queries/page.sql';
import {
    PAGE_HISTORY_NEXT_VERSION,
    PAGE_HISTORY_INSERT,
    PAGE_HISTORY_SELECT_LATEST,
    PAGE_HISTORY_SELECT_BY_VERSION,
    PAGE_HISTORY_SELECT_LIST,
    PAGE_HISTORY_COUNT_BY_PAGE,
    PAGE_HISTORY_SELECT_VERSION_BY_FILE_PATH,
} from '@/db/queries/page-history.sql';
import { COMP_MAP_DELETE_BY_PAGE_VERSION, COMP_MAP_INSERT } from '@/db/queries/component-map.sql';
import { ASSET_MAP_INSERT, ASSET_MAP_DELETE_BY_PAGE_VERSION } from '@/db/queries/asset.sql';
import { readPageHtml } from '@/lib/page-file';

const OBJ = { outFormat: oracledb.OUT_FORMAT_OBJECT };

export interface CmsPageTemplateSummary {
    pageId: string;
    pageName: string;
    viewMode: string;
}

// ═══════════════════════════════════════════════
// 페이지 조회
// ═══════════════════════════════════════════════

/** 페이지 단건 조회 */
export async function getPageById(pageId: string): Promise<CmsPage | null> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<CmsPage>(PAGE_SELECT_BY_ID, { pageId }, OBJ);
        return result.rows?.[0] ?? null;
    } finally {
        await conn.close();
    }
}

/** 페이지 목록 조회 (페이지네이션 + 검색 + 정렬) */
export async function getPageList(
    options: {
        approveState?: ApproveState;
        excludeNewWork?: boolean; // 승인 이력 없는 순수 신규 WORK 제외 (관리자 대시보드용)
        createUserId?: string;
        createUserName?: string; // CREATE_USER_NAME 필터
        search?: string; // PAGE_NAME LIKE 검색어
        sortBy?: 'name' | 'date'; // 'name': 이름순, 'date'(기본): 최신 수정순
        viewMode?: ViewMode; // 뷰 모드 필터
        page?: number;
        pageSize?: number;
    } = {},
): Promise<{ list: CmsPage[]; totalCount: number }> {
    const page = options.page ?? 1;
    const pageSize = options.pageSize ?? 10;
    const startRow = (page - 1) * pageSize;
    const endRow = page * pageSize;

    const binds = {
        approveState: options.approveState ?? null,
        excludeNewWork: options.excludeNewWork ? 1 : 0,
        createUserId: options.createUserId ?? null,
        createUserName: options.createUserName ?? null,
        search: options.search ?? null,
        sortBy: options.sortBy === 'name' ? 'name' : 'date',
        viewMode: options.viewMode ?? null,
        startRow,
        endRow,
    };

    const conn = await getConnection();
    try {
        const [listResult, countResult] = await Promise.all([
            conn.execute<CmsPage>(PAGE_SELECT_LIST, binds, OBJ),
            conn.execute<{ TOTAL_COUNT: number }>(
                PAGE_COUNT,
                {
                    approveState: binds.approveState,
                    excludeNewWork: binds.excludeNewWork,
                    createUserId: binds.createUserId,
                    createUserName: binds.createUserName,
                    search: binds.search,
                    viewMode: binds.viewMode,
                },
                OBJ,
            ),
        ]);

        return {
            list: listResult.rows ?? [],
            totalCount: countResult.rows?.[0]?.TOTAL_COUNT ?? 0,
        };
    } finally {
        await conn.close();
    }
}

// ═══════════════════════════════════════════════
// PAGE_HTML (DB 직접 저장)
// ═══════════════════════════════════════════════

/** PAGE_HTML CLOB 단건 조회 (DB 직접 저장된 HTML) */
export async function getPageHtml(pageId: string): Promise<string | null> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<{ PAGE_HTML: string | null }>(PAGE_SELECT_HTML_BY_ID, { pageId }, OBJ);
        return result.rows?.[0]?.PAGE_HTML ?? null;
    } finally {
        await conn.close();
    }
}

/** 페이지 생성 모달용 템플릿 목록 조회 */
export async function getPageTemplateList(): Promise<CmsPageTemplateSummary[]> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<{ PAGE_ID: string; PAGE_NAME: string; VIEW_MODE: string }>(
            PAGE_SELECT_TEMPLATE_LIST,
            {},
            OBJ,
        );
        return (result.rows ?? []).map((row) => ({
            pageId: row.PAGE_ID,
            pageName: row.PAGE_NAME,
            viewMode: row.VIEW_MODE,
        }));
    } finally {
        await conn.close();
    }
}

/** PAGE_HTML CLOB 업데이트 (에디터 HTML → DB 직접 저장) */
export async function savePageHtml(
    pageId: string,
    html: string,
    lastModifierId: string,
    lastModifierName: string,
): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_UPDATE_HTML, {
            pageId,
            pageHtml: clobBind(html),
            lastModifierId,
            lastModifierName,
        });
    });
}

// ═══════════════════════════════════════════════
// 페이지 저장 (W-6.3: PAGE만 INSERT/UPDATE, HISTORY INSERT 제거)
// ═══════════════════════════════════════════════

/** 페이지 신규 생성 — PAGE INSERT만 (HISTORY는 승인 시에만 INSERT) */
export async function createPage(input: {
    pageId: string;
    pageName: string;
    viewMode?: ViewMode;
    pageType?: PageType;
    ownerDeptCode?: string;
    filePath?: string;
    pageHtml?: string;
    createUserId: string;
    createUserName: string;
    pageDesc?: string;
    pageDescDetail?: string;
    templateId?: string;
    thumbnail?: string;
    targetCd?: string;
}): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_INSERT, {
            pageId: input.pageId,
            pageName: input.pageName,
            viewMode: input.viewMode ?? null,
            pageType: input.pageType ?? null,
            ownerDeptCode: input.ownerDeptCode ?? null,
            filePath: input.filePath ?? null,
            pageHtml: clobBind(input.pageHtml ?? null),
            createUserId: input.createUserId,
            createUserName: input.createUserName,
            lastModifierId: input.createUserId,
            lastModifierName: input.createUserName,
            pageDesc: clobBind(input.pageDesc ?? null),
            pageDescDetail: clobBind(input.pageDescDetail ?? null),
            templateId: input.templateId ?? null,
            thumbnail: input.thumbnail ?? null,
            targetCd: input.targetCd ?? null,
        });
    });
}

/** 페이지 수정 — PAGE UPDATE만 (HISTORY는 승인 시에만 INSERT) */
export async function updatePage(input: {
    pageId: string;
    pageName?: string;
    viewMode?: ViewMode;
    pageDesc?: string;
    pageDescDetail?: string;
    filePath?: string;
    pageHtml?: string;
    thumbnail?: string;
    lastModifierId: string;
    lastModifierName: string;
}): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_UPDATE, {
            pageId: input.pageId,
            pageName: input.pageName ?? null,
            viewMode: input.viewMode ?? null,
            pageDesc: clobBind(input.pageDesc ?? null),
            pageDescDetail: clobBind(input.pageDescDetail ?? null),
            filePath: input.filePath ?? null,
            pageHtml: clobBind(input.pageHtml ?? null),
            thumbnail: input.thumbnail ?? null,
            lastModifierId: input.lastModifierId,
            lastModifierName: input.lastModifierName,
        });
    });
}

/** 승인 요청 — APPROVE_STATE를 PENDING으로 변경, 결재자와 노출 기간 지정 */
export async function requestApproval(
    pageId: string,
    approverId: string,
    approverName: string,
    beginningDate: string,
    expiredDate: string,
): Promise<void> {
    await withTransaction(async (conn) => {
        const result = await conn.execute(PAGE_REQUEST_APPROVAL, {
            pageId,
            approverId,
            approverName,
            beginningDate,
            expiredDate,
        });
        if ((result.rowsAffected ?? 0) === 0) {
            throw new Error('승인 요청할 수 없는 상태입니다.');
        }
    });
}

/** 결재 상태 변경 — APPROVED일 때만 HISTORY INSERT (W-6.3 핵심) */
export async function updateApproveState(input: {
    pageId: string;
    approveState: ApproveState;
    approverId?: string;
    approverName?: string;
    rejectedReason?: string;
    beginningDate?: string | null;
    lastModifierId: string;
}): Promise<{ version?: number }> {
    return await withTransaction(async (conn) => {
        // 1. 결재 상태 UPDATE — EXPIRED_DATE는 승인 요청 시 저장된 값 유지
        await conn.execute(PAGE_UPDATE_APPROVE_STATE, {
            pageId: input.pageId,
            approveState: input.approveState,
            approverId: input.approverId ?? null,
            approverName: input.approverName ?? null,
            rejectedReason: clobBind(input.rejectedReason ?? null),
            beginningDate: input.beginningDate ?? null,
            lastModifierId: input.lastModifierId,
        });

        // 2. 승인(APPROVED)일 때만 PAGE_HISTORY INSERT + COMP_PAGE_MAP 틀
        if (input.approveState === 'APPROVED') {
            const versionResult = await conn.execute<{ NEXT_VERSION: number }>(
                PAGE_HISTORY_NEXT_VERSION,
                { pageId: input.pageId },
                OBJ,
            );
            const nextVersion = versionResult.rows?.[0]?.NEXT_VERSION ?? 1;

            await conn.execute(PAGE_HISTORY_INSERT, {
                pageId: input.pageId,
                version: nextVersion,
            });

            // COMP_PAGE_MAP 매핑 생성 — HTML 파싱으로 컴포넌트 ID 추출 (#138)
            await conn.execute(COMP_MAP_DELETE_BY_PAGE_VERSION, {
                pageId: input.pageId,
                version: nextVersion,
            });

            // 페이지 HTML에서 컴포넌트/에셋 ID 추출 — DB 우선, FILE_PATH 폴백
            const page = await conn.execute<CmsPage>(PAGE_SELECT_BY_ID, { pageId: input.pageId }, OBJ);
            const pageRow = page.rows?.[0];
            let resolvedHtml = pageRow?.PAGE_HTML ?? null;
            if (!resolvedHtml && pageRow?.FILE_PATH) {
                resolvedHtml = await readPageHtml(pageRow.FILE_PATH);
            }

            if (resolvedHtml) {
                // (A) COMP_PAGE_MAP — data-component-id 추출 (기존 로직 유지)
                const compRegex = /data-component-id\s*=\s*["']([^"']+)["']/g;
                const componentIds = Array.from(resolvedHtml.matchAll(compRegex), (m) => m[1]);

                if (componentIds.length > 0) {
                    const binds = componentIds.map((componentId, i) => ({
                        pageId: input.pageId,
                        version: nextVersion,
                        sortOrder: i + 1,
                        componentId,
                        lastModifierId: input.lastModifierId,
                    }));
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    await (conn as any).executeMany(COMP_MAP_INSERT, binds);
                }

                // (B) ASSET_PAGE_MAP — UUID 파일명 패턴으로 에셋 ID 추출 (URL 경로 무관)
                const assetRegex = /([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})_[^"'\s)]+/gi;
                const candidateIds = [...new Set(Array.from(resolvedHtml.matchAll(assetRegex), (m) => m[1]))];

                // 실제 존재하는 에셋만 필터링 (FK 위반 방지)
                const assetIds: string[] = [];
                for (const id of candidateIds) {
                    const exists = await conn.execute<{ CNT: number }>(
                        `SELECT COUNT(*) AS CNT FROM SPW_CMS_ASSET WHERE ASSET_ID = :assetId AND USE_YN = 'Y'`,
                        { assetId: id },
                        { outFormat: oracledb.OUT_FORMAT_OBJECT },
                    );
                    if ((exists.rows?.[0]?.CNT ?? 0) > 0) {
                        assetIds.push(id);
                    }
                }

                // 기존 매핑 항상 초기화 (에셋 0개인 경우에도 이전 매핑 정리)
                await conn.execute(ASSET_MAP_DELETE_BY_PAGE_VERSION, {
                    pageId: input.pageId,
                    version: nextVersion,
                });

                for (const assetId of assetIds) {
                    await conn.execute(ASSET_MAP_INSERT, {
                        pageId: input.pageId,
                        version: nextVersion,
                        assetId,
                    });
                }
            }

            return { version: nextVersion };
        }

        return {};
    });
}

/**
 * 페이지 삭제 — 이슈 #26 삭제 정책:
 * - 미승인 (HISTORY 없음): PAGE + COMP_MAP 하드 삭제
 * - 승인됨 (HISTORY 있음): PAGE 소프트 삭제 (USE_YN='N'), HISTORY 보존
 * @returns 삭제 유형 ('hard' | 'soft')
 */
export async function deletePage(pageId: string, lastModifierId: string): Promise<{ deleteType: 'hard' | 'soft' }> {
    return await withTransaction(async (conn) => {
        // 승인 이력 존재 여부 확인
        const historyResult = await conn.execute<{ CNT: number }>(
            PAGE_HISTORY_COUNT_BY_PAGE,
            { pageId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT },
        );
        const hasHistory = (historyResult.rows?.[0]?.CNT ?? 0) > 0;

        if (hasHistory) {
            // 승인된 페이지: 소프트 삭제 (DB 보존, HISTORY 유지)
            // A/B 그룹 참여 중이면 먼저 해제
            await conn.execute(PAGE_CLEAR_PAGE_AB_GROUP, { pageId, lastModifierId });
            await conn.execute(PAGE_SOFT_DELETE, { pageId, lastModifierId });
            return { deleteType: 'soft' };
        } else {
            // 미승인 페이지: A/B 그룹 해제 → COMP_MAP + PAGE 하드 삭제
            await conn.execute(PAGE_CLEAR_PAGE_AB_GROUP, { pageId, lastModifierId });
            await conn.execute(COMP_MAP_DELETE_BY_PAGE, { pageId });
            await conn.execute(PAGE_HARD_DELETE, { pageId });
            return { deleteType: 'hard' };
        }
    });
}

/** 배포 완료 후 노출 시작일 및 CRC 값 갱신 — BEGINNING_DATE=오늘 */
export async function updatePageDeploy(pageId: string, fileCrcValue: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_UPDATE_DEPLOY, { pageId, fileCrcValue, lastModifierId });
    });
}

// ═══════════════════════════════════════════════
// 롤백
// ═══════════════════════════════════════════════

/**
 * 버전 롤백 — 지정 버전의 FILE_PATH를 PAGE에 덮어쓰고 APPROVE_STATE = 'WORK' 전환
 * - 이슈 #26 설계: HISTORY INSERT 없음, FILE_PATH UPDATE만 수행
 * - 롤백 후 재승인 절차 필요
 */
export async function updatePageRollback(pageId: string, version: number, lastModifierId: string): Promise<void> {
    const history = await getHistoryByVersion(pageId, version);
    if (!history) {
        throw new Error(`버전 ${version}에 해당하는 이력이 존재하지 않습니다.`);
    }
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_ROLLBACK, {
            pageId,
            pageHtml: clobBind(history.PAGE_HTML ?? null),
            filePath: history.FILE_PATH,
            lastModifierId,
        });
    });
}

/** FILE_PATH로 HISTORY VERSION 역조회 — 롤백 후 배포 fileId 결정용 */
export async function getHistoryVersionByFilePath(pageId: string, filePath: string): Promise<number | null> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<{ VERSION: number }>(
            PAGE_HISTORY_SELECT_VERSION_BY_FILE_PATH,
            { pageId, filePath },
            OBJ,
        );
        return result.rows?.[0]?.VERSION ?? null;
    } finally {
        await conn.close();
    }
}

// ═══════════════════════════════════════════════
// 이력 조회
// ═══════════════════════════════════════════════

/** 최신 이력 조회 */
export async function getLatestHistory(pageId: string): Promise<CmsPageHistory | null> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<CmsPageHistory>(PAGE_HISTORY_SELECT_LATEST, { pageId }, OBJ);
        return result.rows?.[0] ?? null;
    } finally {
        await conn.close();
    }
}

/** 특정 버전 이력 조회 */
export async function getHistoryByVersion(pageId: string, version: number): Promise<CmsPageHistory | null> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<CmsPageHistory>(PAGE_HISTORY_SELECT_BY_VERSION, { pageId, version }, OBJ);
        return result.rows?.[0] ?? null;
    } finally {
        await conn.close();
    }
}

/** 이력 목록 조회 (버전 역순, 요약 정보) — W-6.2: SNAPSHOT_DTIME → APPROVE_DATE */
export async function getHistoryList(
    pageId: string,
): Promise<
    Pick<
        CmsPageHistory,
        | 'PAGE_ID'
        | 'VERSION'
        | 'PAGE_NAME'
        | 'APPROVE_STATE'
        | 'LAST_MODIFIER_ID'
        | 'LAST_MODIFIER_NAME'
        | 'APPROVE_DATE'
    >[]
> {
    const conn = await getConnection();
    try {
        const result = await conn.execute(PAGE_HISTORY_SELECT_LIST, { pageId }, OBJ);
        return (result.rows ?? []) as Pick<
            CmsPageHistory,
            | 'PAGE_ID'
            | 'VERSION'
            | 'PAGE_NAME'
            | 'APPROVE_STATE'
            | 'LAST_MODIFIER_ID'
            | 'LAST_MODIFIER_NAME'
            | 'APPROVE_DATE'
        >[];
    } finally {
        await conn.close();
    }
}

// ═══════════════════════════════════════════════
// 상태 전환
// ═══════════════════════════════════════════════

/** 재수정 시 APPROVE_STATE → WORK 전환 (APPROVED/REJECTED만 대상, 그 외 무시) */
export async function resetApproveStateToWork(pageId: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_RESET_TO_WORK, { pageId, lastModifierId });
    });
}

/** 승인된 페이지 시작일/만료일 수정 — 관리자 날짜 관리 */
export async function updatePageDates(
    pageId: string,
    beginningDate: string | null,
    expiredDate: string | null,
    lastModifierId: string,
): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_UPDATE_DATES, {
            pageId,
            beginningDate: beginningDate ?? null,
            expiredDate: expiredDate ?? null,
            lastModifierId,
        });
    });
}

// ═══════════════════════════════════════════════
// 만료 관리
// ═══════════════════════════════════════════════

/** 만료 페이지 목록 조회 — EXPIRED_DATE 경과 + IS_PUBLIC='Y' + USE_YN='Y' */
export async function getExpiredPages(): Promise<CmsPage[]> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<CmsPage>(PAGE_SELECT_EXPIRED, {}, OBJ);
        return result.rows ?? [];
    } finally {
        await conn.close();
    }
}

/** IS_PUBLIC 단건 변경 — 관리자 긴급 차단/해제 */
export async function setPagePublic(pageId: string, isPublic: 'Y' | 'N', lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_UPDATE_IS_PUBLIC, { pageId, isPublic, lastModifierId });
    });
}

/** 만료 처리 단건 — IS_PUBLIC='N', FILE_PATH_BACK 기록 */
export async function expirePage(pageId: string, filePathBack: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_EXPIRE, { pageId, filePathBack, lastModifierId });
    });
}

// ═══════════════════════════════════════════════
// A/B 테스트
// ═══════════════════════════════════════════════

export interface AbGroupPage {
    PAGE_ID: string;
    PAGE_NAME: string;
    AB_WEIGHT: number | null;
    IS_PUBLIC: string;
}

/** A/B 그룹 내 페이지 목록 조회 */
export async function getAbGroup(groupId: string): Promise<AbGroupPage[]> {
    const conn = await getConnection();
    try {
        const result = await conn.execute<AbGroupPage>(PAGE_SELECT_AB_GROUP, { groupId }, OBJ);
        return result.rows ?? [];
    } finally {
        await conn.close();
    }
}

/**
 * 여러 페이지를 A/B 그룹에 일괄 설정 — 단일 트랜잭션으로 원자성 보장
 * - 이미 다른 그룹에 속한 페이지는 덮어쓰지 않음 (SQL WHERE 조건)
 * @returns 각 pageId별 업데이트 성공 여부 목록
 */
export async function setAbGroupForPages(
    pages: { pageId: string; weight: number }[],
    groupId: string,
    lastModifierId: string,
): Promise<{ pageId: string; updated: boolean }[]> {
    return await withTransaction(async (conn) => {
        const results: { pageId: string; updated: boolean }[] = [];
        for (const { pageId, weight } of pages) {
            const result = await conn.execute(PAGE_UPDATE_AB_GROUP, { pageId, groupId, weight, lastModifierId });
            results.push({ pageId, updated: (result.rowsAffected ?? 0) > 0 });
        }
        return results;
    });
}

/** 그룹 전체 해제 — 그룹 내 모든 페이지의 AB_GROUP_ID, AB_WEIGHT를 NULL로 */
export async function clearAbGroup(groupId: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_CLEAR_AB_GROUP, { groupId, lastModifierId });
    });
}

/** 단일 페이지 A/B 그룹 해제 */
export async function clearPageAbGroup(pageId: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_CLEAR_PAGE_AB_GROUP, { pageId, lastModifierId });
    });
}

/**
 * Winner 승격 — 단일 트랜잭션으로 처리
 * - 패배 페이지: AB_WEIGHT = 0 (AB_GROUP_ID 유지 — 이력 보존)
 * - Winner 페이지: AB_WEIGHT = 1 (단독 노출 고정)
 */
export async function promoteWinner(groupId: string, winnerPageId: string, lastModifierId: string): Promise<void> {
    await withTransaction(async (conn) => {
        await conn.execute(PAGE_PROMOTE_WINNER, { groupId, winnerPageId, lastModifierId });
        await conn.execute(PAGE_SET_WINNER, { winnerPageId, lastModifierId });
    });
}
