// src/lib/codes.ts
// FWK_CODE common lookup helpers for server components and route handlers.

import 'server-only';

import oracledb from 'oracledb';

import { getConnection } from '@/db/connection';

import { CMS_ASSET_CATEGORY_GROUP_ID, CMS_ASSET_DEFAULT_CATEGORY } from '@/lib/cms-asset-category';

export interface CodeItem {
    code: string;
    codeName: string;
    sortOrder: number;
}

export { CMS_ASSET_CATEGORY_GROUP_ID, CMS_ASSET_DEFAULT_CATEGORY };

/**
 * 공통 코드 그룹의 활성(USE_YN='Y') 코드를 SORT_ORDER 오름차순으로 조회.
 * outFormat 은 OBJECT 로 받아 컬럼명으로 매핑 — 다른 repository 와 동일 패턴.
 * (이전 구현은 outFormat 만 OBJECT(4002) 인데 배열 destructuring 으로 매핑하려 해
 *  TypeError 가 catch 로 묻혀 항상 빈 배열을 반환하는 사일런트 버그가 있었음)
 */
export async function getCodesByGroup(codeGroupId: string): Promise<CodeItem[]> {
    let connection;
    try {
        connection = await getConnection();
        const result = await connection.execute<{ CODE: string; CODE_NAME: string; SORT_ORDER: number }>(
            `SELECT CODE, CODE_NAME, SORT_ORDER
               FROM FWK_CODE
              WHERE CODE_GROUP_ID = :codeGroupId
                AND USE_YN = 'Y'
              ORDER BY SORT_ORDER ASC`,
            { codeGroupId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT },
        );
        return (result.rows ?? []).map((row) => ({
            code: row.CODE,
            codeName: row.CODE_NAME,
            sortOrder: row.SORT_ORDER,
        }));
    } catch (err) {
        // 사일런트 실패 방지 — DB·SQL 오류는 서버 로그에 남겨 운영에서 추적 가능하게 함
        console.error('[getCodesByGroup] FWK_CODE 조회 실패', { codeGroupId, err });
        return [];
    } finally {
        if (connection) {
            try {
                await connection.close();
            } catch {
                // Ignore connection close failures.
            }
        }
    }
}

export async function getCmsAssetCategoryCodes(): Promise<CodeItem[]> {
    return getCodesByGroup(CMS_ASSET_CATEGORY_GROUP_ID);
}

export async function normalizeCmsAssetCategory(category?: string | null): Promise<string> {
    // Admin이 코드 목록 기반으로 선택한 값을 전송하므로 CMS에서 DB 재검증은 불필요하다.
    // DB 불안정 시 getCodesByGroup이 빈 배열을 반환해 COMMON을 포함한 모든 카테고리가 거부되는 문제 방지.
    return category?.trim() || CMS_ASSET_DEFAULT_CATEGORY;
}
