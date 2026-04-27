-- =============================================================
-- 내 작업함 기능 — DDL 및 초기 데이터
-- 이슈: #224
-- 작성일: 2026-04-27
-- ⚠️  이 파일의 쿼리는 개발자가 DB에서 직접 수행해야 합니다.
-- ⚠️  FWK_USER_MENU 는 ALTER 권한 없음 → 별도 매핑 테이블로 대체
-- =============================================================


-- =============================================================
-- 1. FWK_USER_WORK_GROUP — 사용자별 작업 그룹 관리
-- =============================================================

CREATE TABLE FWK_USER_WORK_GROUP (
    USER_ID                 VARCHAR2(20)    NOT NULL,
    GROUP_ID                VARCHAR2(20)    NOT NULL,
    GROUP_NAME              VARCHAR2(100)   NOT NULL,
    GROUP_ORDER             NUMBER(3, 0)    DEFAULT 0 NOT NULL,  -- 탭/목록 정렬 순서
    LAST_UPDATE_DTIME       VARCHAR2(14)    DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID     VARCHAR2(20)    NOT NULL,
    CONSTRAINT PK_FWK_USER_WORK_GROUP PRIMARY KEY (USER_ID, GROUP_ID)
);

COMMENT ON TABLE  FWK_USER_WORK_GROUP                       IS '내 작업함 — 사용자별 작업 그룹';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.USER_ID               IS '사용자 ID (FWK_USER.USER_ID 참조)';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.GROUP_ID              IS '그룹 ID (사용자 내 유니크)';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.GROUP_NAME            IS '그룹명';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.GROUP_ORDER           IS '그룹 정렬 순서';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.LAST_UPDATE_DTIME     IS '최종 수정 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_USER_WORK_GROUP.LAST_UPDATE_USER_ID   IS '최종 수정자 ID';


-- =============================================================
-- 2. FWK_USER_WORK_TASK — 사용자 작업함 항목 (FWK_USER_MENU ALTER 대체)
-- =============================================================
-- FWK_USER_MENU 를 읽기 전용으로 JOIN 하고,
-- 작업함 등록/그룹 분류/정렬은 이 테이블에서 관리한다.

CREATE TABLE FWK_USER_WORK_TASK (
    USER_ID                 VARCHAR2(20)    NOT NULL,
    MENU_ID                 VARCHAR2(40)    NOT NULL,
    WORK_GROUP_ID           VARCHAR2(20)    DEFAULT NULL,        -- NULL = 그룹 미지정 (전체 탭)
    TASK_ORDER              NUMBER(3, 0)    DEFAULT 0 NOT NULL,  -- 작업함 내 정렬 순서
    LAST_UPDATE_DTIME       VARCHAR2(14)    DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID     VARCHAR2(20)    NOT NULL,
    CONSTRAINT PK_FWK_USER_WORK_TASK PRIMARY KEY (USER_ID, MENU_ID)
);

COMMENT ON TABLE  FWK_USER_WORK_TASK                        IS '내 작업함 — 사용자별 작업 항목 (메뉴 매핑)';
COMMENT ON COLUMN FWK_USER_WORK_TASK.USER_ID                IS '사용자 ID (FWK_USER.USER_ID 참조)';
COMMENT ON COLUMN FWK_USER_WORK_TASK.MENU_ID                IS '메뉴 ID (FWK_MENU.MENU_ID 참조)';
COMMENT ON COLUMN FWK_USER_WORK_TASK.WORK_GROUP_ID          IS '작업 그룹 ID (FWK_USER_WORK_GROUP.GROUP_ID 참조), NULL = 미분류';
COMMENT ON COLUMN FWK_USER_WORK_TASK.TASK_ORDER             IS '작업함 내 정렬 순서';
COMMENT ON COLUMN FWK_USER_WORK_TASK.LAST_UPDATE_DTIME      IS '최종 수정 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_USER_WORK_TASK.LAST_UPDATE_USER_ID    IS '최종 수정자 ID';


-- =============================================================
-- 3. 인덱스 생성
-- =============================================================

-- 사용자별 그룹 목록 조회
CREATE INDEX IDX_USER_WORK_GROUP_USER ON FWK_USER_WORK_GROUP (USER_ID);

-- 사용자별 작업 목록 조회 + 그룹 필터링
CREATE INDEX IDX_USER_WORK_TASK_USER  ON FWK_USER_WORK_TASK (USER_ID);
CREATE INDEX IDX_USER_WORK_TASK_GROUP ON FWK_USER_WORK_TASK (USER_ID, WORK_GROUP_ID);


-- =============================================================
-- 4. FWK_MENU — 내 작업함 메뉴 항목 등록
-- =============================================================
-- ⚠️  PARENT_MENU_ID, MENU_ORDER 는 실제 메뉴 구조에 맞게 조정 필요

INSERT INTO FWK_MENU (
    MENU_ID, MENU_NAME, MENU_URL, PRIOR_MENU_ID, SORT_ORDER,
    DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID
) VALUES (
    'v3_approval_workspace', '나의 작업함', '/approval', 'v3_acl_manage', 1,
    'Y', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin'
);

COMMIT;