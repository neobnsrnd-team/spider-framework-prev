// ============================================================================
// SPW_CMS_PAGE — 페이지 테이블 SQL 맵퍼
// ============================================================================

/** 페이지 단건 조회 */
export const PAGE_SELECT_BY_ID = `
  SELECT *
  FROM SPW_CMS_PAGE
  WHERE PAGE_ID = :pageId
    AND USE_YN = 'Y'
`;

/** 페이지 목록 조회 (결재상태 + 생성자 필터 + 검색 + 정렬, 페이지네이션) */
export const PAGE_SELECT_LIST = `
  SELECT *
  FROM (
    SELECT p.*, ROWNUM AS RN
    FROM (
      SELECT *
      FROM SPW_CMS_PAGE
      WHERE USE_YN = 'Y'
        AND (:approveState IS NULL OR APPROVE_STATE = :approveState)
        AND (:excludeNewWork = 0 OR NOT (APPROVE_STATE = 'WORK' AND APPROVE_DATE IS NULL))
        AND (:createUserId IS NULL OR CREATE_USER_ID = :createUserId)
        AND (:createUserName IS NULL OR CREATE_USER_NAME = :createUserName)
        AND (:search IS NULL OR PAGE_NAME LIKE '%' || :search || '%' OR CREATE_USER_NAME LIKE '%' || :search || '%')
        AND (:viewMode IS NULL OR VIEW_MODE = :viewMode)
      ORDER BY
        CASE WHEN :sortBy = 'name' THEN PAGE_NAME ELSE NULL END ASC NULLS LAST,
        CASE WHEN :sortBy = 'name' THEN NULL ELSE LAST_MODIFIED_DTIME END DESC NULLS LAST
    ) p
    WHERE ROWNUM <= :endRow
  )
  WHERE RN > :startRow
`;

/** 페이지 총 건수 조회 (페이지네이션용) */
export const PAGE_COUNT = `
  SELECT COUNT(*) AS TOTAL_COUNT
  FROM SPW_CMS_PAGE
  WHERE USE_YN = 'Y'
    AND (:approveState IS NULL OR APPROVE_STATE = :approveState)
    AND (:excludeNewWork = 0 OR NOT (APPROVE_STATE = 'WORK' AND APPROVE_DATE IS NULL))
    AND (:createUserId IS NULL OR CREATE_USER_ID = :createUserId)
    AND (:createUserName IS NULL OR CREATE_USER_NAME = :createUserName)
    AND (:search IS NULL OR PAGE_NAME LIKE '%' || :search || '%' OR CREATE_USER_NAME LIKE '%' || :search || '%')
    AND (:viewMode IS NULL OR VIEW_MODE = :viewMode)
`;

/** 페이지 신규 생성 (W-3: VIEW_MODE 바인딩 추가) */
export const PAGE_INSERT = `
  INSERT INTO SPW_CMS_PAGE (
    PAGE_ID, PAGE_NAME, VIEW_MODE, OWNER_DEPT_CODE, FILE_PATH, PAGE_HTML,
    CREATE_USER_ID, CREATE_USER_NAME,
    LAST_MODIFIER_ID, LAST_MODIFIER_NAME,
    APPROVE_STATE, PAGE_TYPE, PAGE_DESC, PAGE_DESC_DETAIL,
    TEMPLATE_ID, THUMBNAIL, TARGET_CD, USE_YN, IS_PUBLIC
  ) VALUES (
    :pageId, :pageName, NVL(:viewMode, 'mobile'), :ownerDeptCode, :filePath, :pageHtml,
    :createUserId, :createUserName,
    :lastModifierId, :lastModifierName,
    'WORK', NVL(:pageType, 'PAGE'), :pageDesc, :pageDescDetail,
    :templateId, :thumbnail, :targetCd, 'Y', 'Y'
  )
`;

/** 페이지 내용 수정 (에디터 저장 시, W-3: VIEW_MODE 추가) */
export const PAGE_UPDATE = `
  UPDATE SPW_CMS_PAGE
  SET PAGE_NAME = NVL(:pageName, PAGE_NAME),
      VIEW_MODE = NVL(:viewMode, VIEW_MODE),
      PAGE_DESC = :pageDesc,
      PAGE_DESC_DETAIL = :pageDescDetail,
      FILE_PATH = NVL(:filePath, FILE_PATH),
      PAGE_HTML = :pageHtml,
      THUMBNAIL = :thumbnail,
      LAST_MODIFIER_ID = :lastModifierId,
      LAST_MODIFIER_NAME = :lastModifierName
  WHERE PAGE_ID = :pageId
    AND USE_YN = 'Y'
`;

/** 승인 요청 — APPROVE_STATE를 PENDING으로, 결재자/노출 기간 지정, 요청 시각 기록 */
export const PAGE_REQUEST_APPROVAL = `
  UPDATE SPW_CMS_PAGE
  SET APPROVE_STATE = 'PENDING',
      APPROVER_ID   = :approverId,
      APPROVER_NAME = :approverName,
      CONFIRM_DTIME = SYSTIMESTAMP,
      BEGINNING_DATE = TO_DATE(:beginningDate, 'YYYY-MM-DD'),
      EXPIRED_DATE  = TO_DATE(:expiredDate, 'YYYY-MM-DD')
  WHERE PAGE_ID     = :pageId
    AND APPROVE_STATE IN ('WORK', 'REJECTED', 'APPROVED')
`;

/** 결재 상태 변경 — EXPIRED_DATE는 승인 요청 시 저장된 값 유지 */
export const PAGE_UPDATE_APPROVE_STATE = `
  UPDATE SPW_CMS_PAGE
  SET APPROVE_STATE = :approveState,
      APPROVER_ID = :approverId,
      APPROVER_NAME = :approverName,
      APPROVE_DATE = SYSTIMESTAMP,
      REJECTED_REASON = :rejectedReason,
      BEGINNING_DATE = TO_DATE(:beginningDate, 'YYYY-MM-DD'),
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** 페이지 시작일/만료일 수정 — 관리자 날짜 관리 (승인 이력 있는 페이지 대상) */
export const PAGE_UPDATE_DATES = `
  UPDATE SPW_CMS_PAGE
  SET BEGINNING_DATE = TO_DATE(:beginningDate, 'YYYY-MM-DD'),
      EXPIRED_DATE = TO_DATE(:expiredDate, 'YYYY-MM-DD'),
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** 재수정 시 APPROVE_STATE → WORK 전환 (APPROVED/REJECTED만 대상) */
export const PAGE_RESET_TO_WORK = `
  UPDATE SPW_CMS_PAGE
  SET APPROVE_STATE = 'WORK',
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
    AND APPROVE_STATE IN ('APPROVED', 'REJECTED')
`;

/** 만료 페이지 조회 — EXPIRED_DATE 경과 + 공개 + 사용 중인 페이지 */
export const PAGE_SELECT_EXPIRED = `
  SELECT *
  FROM SPW_CMS_PAGE
  WHERE EXPIRED_DATE < TRUNC(SYSDATE)
    AND IS_PUBLIC = 'Y'
    AND USE_YN = 'Y'
`;

/** IS_PUBLIC 단건 업데이트 — 관리자 긴급 차단/해제 */
export const PAGE_UPDATE_IS_PUBLIC = `
  UPDATE SPW_CMS_PAGE
  SET IS_PUBLIC = :isPublic,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** 만료 처리 — IS_PUBLIC='N', FILE_PATH_BACK 기록 (USE_YN은 유지 — 대시보드 노출) */
export const PAGE_EXPIRE = `
  UPDATE SPW_CMS_PAGE
  SET IS_PUBLIC = 'N',
      FILE_PATH_BACK = :filePathBack,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** 소프트 삭제 — 승인된 페이지 (USE_YN = 'N', HISTORY 보존) */
export const PAGE_SOFT_DELETE = `
  UPDATE SPW_CMS_PAGE
  SET USE_YN = 'N',
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** 하드 삭제 — 미승인 페이지 (레코드 물리적 삭제) */
export const PAGE_HARD_DELETE = `
  DELETE FROM SPW_CMS_PAGE
  WHERE PAGE_ID = :pageId
`;

/** COMP_PAGE_MAP 전체 삭제 (페이지 하드 삭제 시 연관 매핑 정리) */
export const COMP_MAP_DELETE_BY_PAGE = `
  DELETE FROM SPW_CMS_COMP_PAGE_MAP
  WHERE PAGE_ID = :pageId
`;

// ── A/B 테스트 ──

/** A/B 그룹 내 페이지 목록 조회 (가중치 포함, 활성 페이지만) */
export const PAGE_SELECT_AB_GROUP = `
  SELECT PAGE_ID, PAGE_NAME, AB_WEIGHT, IS_PUBLIC
  FROM SPW_CMS_PAGE
  WHERE AB_GROUP_ID = :groupId
    AND USE_YN = 'Y'
  ORDER BY AB_WEIGHT DESC NULLS LAST
`;

/** 페이지 A/B 그룹 설정 (가중치 포함) */
export const PAGE_UPDATE_AB_GROUP = `
  UPDATE SPW_CMS_PAGE
  SET AB_GROUP_ID     = :groupId,
      AB_WEIGHT       = :weight,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
    AND USE_YN = 'Y'
    AND (AB_GROUP_ID IS NULL OR AB_GROUP_ID = :groupId)
`;

/** 그룹 전체 해제 — AB_GROUP_ID 기준으로 그룹 내 모든 페이지 초기화 */
export const PAGE_CLEAR_AB_GROUP = `
  UPDATE SPW_CMS_PAGE
  SET AB_GROUP_ID      = NULL,
      AB_WEIGHT        = NULL,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE AB_GROUP_ID = :groupId
    AND USE_YN = 'Y'
`;

/** 단일 페이지 A/B 그룹 해제 */
export const PAGE_CLEAR_PAGE_AB_GROUP = `
  UPDATE SPW_CMS_PAGE
  SET AB_GROUP_ID      = NULL,
      AB_WEIGHT        = NULL,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

/** Winner 승격 — 패배 페이지 AB_WEIGHT = 0 (AB_GROUP_ID 유지, 이력 보존) */
export const PAGE_PROMOTE_WINNER = `
  UPDATE SPW_CMS_PAGE
  SET AB_WEIGHT        = 0,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE AB_GROUP_ID = :groupId
    AND PAGE_ID <> :winnerPageId
    AND USE_YN = 'Y'
`;

/** Winner 페이지 단독 노출 고정 — AB_WEIGHT = 1 */
export const PAGE_SET_WINNER = `
  UPDATE SPW_CMS_PAGE
  SET AB_WEIGHT        = 1,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :winnerPageId
    AND USE_YN = 'Y'
`;

/** 버전 롤백 — 지정 버전의 FILE_PATH를 PAGE에 덮어쓰고 APPROVE_STATE = 'WORK' 전환 */
export const PAGE_ROLLBACK = `
  UPDATE SPW_CMS_PAGE
  SET PAGE_HTML        = :pageHtml,
      FILE_PATH        = :filePath,
      APPROVE_STATE    = 'WORK',
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
    AND USE_YN  = 'Y'
`;

/** 배포 완료 후 무결성 값 갱신 — 시작일/만료일은 승인 시점에 이미 설정 */
export const PAGE_UPDATE_DEPLOY = `
  UPDATE SPW_CMS_PAGE
  SET FILE_CRC_VALUE  = :fileCrcValue,
      LAST_MODIFIER_ID = :lastModifierId
  WHERE PAGE_ID = :pageId
`;

// ── PAGE_HTML (DB 직접 저장) ──

/** PAGE_HTML 단건 조회 */
export const PAGE_SELECT_HTML_BY_ID = `
  SELECT PAGE_HTML
  FROM SPW_CMS_PAGE
  WHERE PAGE_ID = :pageId
    AND USE_YN = 'Y'
`;

/** 페이지 생성 모달용 템플릿 목록 조회 */
export const PAGE_SELECT_TEMPLATE_LIST = `
  SELECT PAGE_ID, PAGE_NAME, VIEW_MODE
  FROM SPW_CMS_PAGE
  WHERE PAGE_TYPE = 'TEMPLATE'
    AND USE_YN = 'Y'
  ORDER BY PAGE_NAME
`;

/** PAGE_HTML 업데이트 (에디터 HTML → DB CLOB 직접 저장) */
export const PAGE_UPDATE_HTML = `
  UPDATE SPW_CMS_PAGE
  SET PAGE_HTML = :pageHtml,
      LAST_MODIFIER_ID = :lastModifierId,
      LAST_MODIFIER_NAME = :lastModifierName
  WHERE PAGE_ID = :pageId
    AND USE_YN = 'Y'
`;
