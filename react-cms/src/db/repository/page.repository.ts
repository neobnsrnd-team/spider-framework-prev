/**
 * @file page.repository.ts
 * @description SPW_CMS_PAGE 테이블 CRUD 리포지토리.
 * Oracle DB와 직접 통신하는 서버 사이드 전용 모듈입니다.
 *
 * react-cms는 기존 테이블 구조를 그대로 사용합니다.
 * PAGE_HTML(CLOB) 컬럼에 CMS 빌더 JSON을 저장합니다.
 *
 * @example
 * import { createPage, getPageById } from './page.repository';
 * await createPage({ pageId: uuid(), pageName: '메인', pageJson: JSON.stringify(page) });
 * const row = await getPageById('xxx-yyy');
 */
import oracledb from 'oracledb';
import { getConnection, withTransaction, clobBind } from '../connection';
import type { CmsPage, ViewMode } from '../types';
import type { CurrentUser } from '../../cms-admin/current-user';

const OBJ = { outFormat: oracledb.OUT_FORMAT_OBJECT };

// react-cms에서 저장한 페이지를 구분하는 타입 값
const PAGE_TYPE = 'REACT';

// ── SQL ──────────────────────────────────────────────────────────

const SQL_SELECT_BY_ID = `
  SELECT PAGE_ID, PAGE_NAME, PAGE_HTML, PAGE_DESC, PAGE_TYPE, VIEW_MODE, APPROVE_STATE,
         USE_YN, IS_PUBLIC, CREATE_USER_ID, CREATE_USER_NAME,
         LAST_MODIFIER_ID, LAST_MODIFIER_NAME, CREATE_DATE, LAST_MODIFIED_DTIME
  FROM   SPW_CMS_PAGE
  WHERE  PAGE_ID = :pageId
    AND  PAGE_TYPE = '${PAGE_TYPE}'
    AND  USE_YN = 'Y'
`;

const SQL_SELECT_LIST = `
  SELECT * FROM (
    SELECT PAGE_ID, PAGE_NAME, PAGE_TYPE, VIEW_MODE, APPROVE_STATE,
           USE_YN, IS_PUBLIC, CREATE_USER_ID, CREATE_USER_NAME,
           LAST_MODIFIER_ID, LAST_MODIFIER_NAME, CREATE_DATE, LAST_MODIFIED_DTIME,
           ROW_NUMBER() OVER (
             ORDER BY
               CASE WHEN :sortBy = 'name' THEN PAGE_NAME END ASC,
               CASE WHEN :sortBy != 'name' THEN LAST_MODIFIED_DTIME END DESC NULLS LAST
           ) AS RN
    FROM SPW_CMS_PAGE
    WHERE PAGE_TYPE = '${PAGE_TYPE}'
      AND USE_YN = 'Y'
      AND (:search IS NULL OR UPPER(PAGE_NAME) LIKE '%' || UPPER(:search) || '%')
      AND (:approveState IS NULL OR APPROVE_STATE = :approveState)
  )
  WHERE RN > :startRow AND RN <= :endRow
`;

const SQL_COUNT = `
  SELECT COUNT(*) AS TOTAL_COUNT
  FROM   SPW_CMS_PAGE
  WHERE  PAGE_TYPE = '${PAGE_TYPE}'
    AND  USE_YN = 'Y'
    AND  (:search IS NULL OR UPPER(PAGE_NAME) LIKE '%' || UPPER(:search) || '%')
    AND  (:approveState IS NULL OR APPROVE_STATE = :approveState)
`;

const SQL_INSERT = `
  INSERT INTO SPW_CMS_PAGE
    (PAGE_ID, PAGE_NAME, PAGE_HTML, PAGE_DESC, PAGE_TYPE, VIEW_MODE, APPROVE_STATE,
     USE_YN, IS_PUBLIC, CREATE_USER_ID, CREATE_USER_NAME,
     LAST_MODIFIER_ID, LAST_MODIFIER_NAME, CREATE_DATE, LAST_MODIFIED_DTIME)
  VALUES
    (:pageId, :pageName, :pageHtml, :pageDesc, '${PAGE_TYPE}', :viewMode, 'WORK',
     'Y', 'N', :createUserId, :createUserName,
     :lastModifierId, :lastModifierName, SYSTIMESTAMP, SYSTIMESTAMP)
`;

const SQL_UPDATE = `
  UPDATE SPW_CMS_PAGE
  SET    PAGE_NAME           = NVL(:pageName, PAGE_NAME),
         PAGE_HTML           = NVL(:pageHtml, PAGE_HTML),
         PAGE_DESC           = NVL(:pageDesc, PAGE_DESC),
         VIEW_MODE           = NVL(:viewMode, VIEW_MODE),
         LAST_MODIFIER_ID    = :lastModifierId,
         LAST_MODIFIER_NAME  = :lastModifierName,
         LAST_MODIFIED_DTIME = SYSTIMESTAMP
  WHERE  PAGE_ID = :pageId
`;

const SQL_DELETE = `
  DELETE FROM SPW_CMS_PAGE
  WHERE  PAGE_ID = :pageId
`;

// ── Repository Functions ─────────────────────────────────────────

/** 페이지 단건 조회 */
export async function getPageById(pageId: string): Promise<CmsPage | null> {
  const conn = await getConnection();
  try {
    const result = await conn.execute<CmsPage>(SQL_SELECT_BY_ID, { pageId }, OBJ);
    return result.rows?.[0] ?? null;
  } finally {
    await conn.close();
  }
}

/**
 * 페이지 목록 조회 (검색 + 정렬 + 페이지네이션)
 * @param options.search PAGE_NAME LIKE 검색어
 * @param options.sortBy 'name': 이름순, 'date'(기본): 최신 수정순
 * @param options.approveState 승인 상태 필터
 * @param options.page 페이지 번호 (1부터)
 * @param options.pageSize 페이지 크기 (기본 10)
 */
export async function listPages(
  options: {
    search?:       string;
    sortBy?:       'name' | 'date';
    approveState?: string;
    page?:         number;
    pageSize?:     number;
  } = {},
): Promise<{ list: CmsPage[]; totalCount: number }> {
  const page     = options.page     ?? 1;
  const pageSize = options.pageSize ?? 10;
  const startRow = (page - 1) * pageSize;
  const endRow   = page * pageSize;

  const binds = {
    search:       options.search       ?? null,
    sortBy:       options.sortBy === 'name' ? 'name' : 'date',
    approveState: options.approveState ?? null,
    startRow,
    endRow,
  };

  const conn = await getConnection();
  try {
    // 동일 커넥션에서 Promise.all은 실제로 병렬 실행되지 않으므로 순차 실행으로 명시
    const listResult  = await conn.execute<CmsPage>(SQL_SELECT_LIST, binds, OBJ);
    const countResult = await conn.execute<{ TOTAL_COUNT: number }>(
      SQL_COUNT,
      { search: binds.search, approveState: binds.approveState },
      OBJ,
    );

    return {
      list:       listResult.rows  ?? [],
      totalCount: countResult.rows?.[0]?.TOTAL_COUNT ?? 0,
    };
  } finally {
    await conn.close();
  }
}

/**
 * 페이지 신규 생성.
 * APPROVE_STATE는 항상 'WORK', USE_YN은 'Y', IS_PUBLIC은 'N'으로 초기화됩니다.
 * @param input.pageJson CMS 빌더 직렬화 JSON — PAGE_HTML 컬럼에 저장
 * @param input.pageCode 생성된 React JSX 코드 — PAGE_DESC 컬럼에 저장
 * @param input.user     인증된 현재 사용자 — CREATE_USER_ID/NAME에 기록됨
 */
export async function createPage(input: {
  pageId:    string;
  pageName:  string;
  pageJson?: string;
  pageCode?: string;
  viewMode?: ViewMode;
  user:      Pick<CurrentUser, 'userId' | 'userName'>;
}): Promise<void> {
  await withTransaction(async (conn) => {
    await conn.execute(SQL_INSERT, {
      pageId:           input.pageId,
      pageName:         input.pageName,
      pageHtml:         clobBind(input.pageJson ?? null),
      pageDesc:         clobBind(input.pageCode ?? null),
      viewMode:         input.viewMode ?? 'mobile',
      createUserId:     input.user.userId,
      createUserName:   input.user.userName,
      lastModifierId:   input.user.userId,
      lastModifierName: input.user.userName,
    });
  });
}

/**
 * 페이지 수정.
 * null이 아닌 필드만 UPDATE됩니다 (NVL 패턴).
 * @param input.pageJson CMS 빌더 직렬화 JSON — PAGE_HTML 컬럼에 저장
 * @param input.pageCode 생성된 React JSX 코드 — PAGE_DESC 컬럼에 저장
 * @param input.user     인증된 현재 사용자 — LAST_MODIFIER_ID/NAME에 기록됨
 */
export async function updatePage(input: {
  pageId:    string;
  pageName?: string;
  pageJson?: string;
  pageCode?: string;
  viewMode?: ViewMode;
  user:      Pick<CurrentUser, 'userId' | 'userName'>;
}): Promise<void> {
  await withTransaction(async (conn) => {
    await conn.execute(SQL_UPDATE, {
      pageId:           input.pageId,
      pageName:         input.pageName ?? null,
      pageHtml:         clobBind(input.pageJson ?? null),
      pageDesc:         clobBind(input.pageCode ?? null),
      viewMode:         input.viewMode ?? null,
      lastModifierId:   input.user.userId,
      lastModifierName: input.user.userName,
    });
  });
}

/** 페이지 삭제 (하드 삭제) */
export async function deletePage(pageId: string): Promise<void> {
  await withTransaction(async (conn) => {
    await conn.execute(SQL_DELETE, { pageId });
  });
}
