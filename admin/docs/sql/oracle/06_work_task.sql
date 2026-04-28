-- =============================================================
-- 내 작업함 기능 — DDL 및 초기 데이터
-- 이슈: #224
-- 작성일: 2026-04-27
-- ⚠️  이 파일의 쿼리는 개발자가 DB에서 직접 수행해야 합니다.
-- ⚠️  FWK_WORK_GROUP, FWK_WORK_LIST 는 as-is 시스템 호환 테이블입니다.
--     이미 존재하는 경우 CREATE 구문을 실행하지 않습니다.
-- =============================================================


-- =============================================================
-- 1. FWK_WORK_GROUP — 작업 그룹 관리
--    GROUP_ID 형식: userId + LPAD(seq, 3, '0')  예) admin001
-- =============================================================

CREATE TABLE FWK_WORK_GROUP (
    GROUP_ID                VARCHAR2(20)    NOT NULL,
    GROUP_NAME              VARCHAR2(100)   NOT NULL,
    GROUP_DESC              VARCHAR2(200),
    LAST_UPDATE_DTIME       VARCHAR2(14),
    LAST_UPDATE_USER_ID     VARCHAR2(50),
    CONSTRAINT PK_FWK_WORK_GROUP PRIMARY KEY (GROUP_ID)
);

COMMENT ON TABLE  FWK_WORK_GROUP                        IS '내 작업함 — 작업 그룹';
COMMENT ON COLUMN FWK_WORK_GROUP.GROUP_ID               IS '그룹 ID (userId + 3자리 일련번호, 예: admin001)';
COMMENT ON COLUMN FWK_WORK_GROUP.GROUP_NAME             IS '그룹명';
COMMENT ON COLUMN FWK_WORK_GROUP.GROUP_DESC             IS '그룹 설명';
COMMENT ON COLUMN FWK_WORK_GROUP.LAST_UPDATE_DTIME      IS '최종 수정 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_WORK_GROUP.LAST_UPDATE_USER_ID    IS '최종 수정자 ID';


-- =============================================================
-- 2. FWK_WORK_LIST — 작업함 목록
--    작업 항목이 CRUD 될 때 자동 등록/수정되는 변경 이력 테이블
-- =============================================================

CREATE TABLE FWK_WORK_LIST (
    WORK_SEQ                NUMBER          NOT NULL,
    WORK_ID                 VARCHAR2(50)    NOT NULL,   -- 항목 코드 (Message, Trx, SQL_QUERY 등)
    WORK_DATA_PK            VARCHAR2(200)   NOT NULL,   -- 식별자 (해당 항목의 PK)
    WORK_NAME               VARCHAR2(200),              -- 작업내용
    CRUD_TYPE               VARCHAR2(1),                -- C=생성 U=수정 D=삭제
    LAST_UPDATE_DTIME       VARCHAR2(14),               -- 최종수정일시
    APPROVAL_SEQ            VARCHAR2(50),               -- 결재일련번호 (NULL=결재미신청)
    LAST_UPDATE_USER_ID     VARCHAR2(50),               -- 최종수정자ID
    DIST_YN                 VARCHAR2(1),                -- 운영반영여부 Y/N
    DIST_DTIME              VARCHAR2(14),               -- 운영반영일시
    GROUP_ID                VARCHAR2(20),               -- 그룹ID (FWK_WORK_GROUP.GROUP_ID)
    FIRST_INSERT_USER_ID    VARCHAR2(50),               -- 최초등록자ID
    FILE_NAME               VARCHAR2(200),              -- 이행스크립트 파일명
    CONSTRAINT PK_FWK_WORK_LIST PRIMARY KEY (WORK_SEQ)
);

COMMENT ON TABLE  FWK_WORK_LIST                         IS '내 작업함 — 작업 목록';
COMMENT ON COLUMN FWK_WORK_LIST.WORK_SEQ                IS '작업 일련번호 (PK, MAX+1 채번)';
COMMENT ON COLUMN FWK_WORK_LIST.WORK_ID                 IS '항목 유형 코드 (Message/Trx/SQL_QUERY 등)';
COMMENT ON COLUMN FWK_WORK_LIST.WORK_DATA_PK            IS '항목 식별자 (해당 관리 대상의 PK)';
COMMENT ON COLUMN FWK_WORK_LIST.WORK_NAME               IS '작업 내용 설명';
COMMENT ON COLUMN FWK_WORK_LIST.CRUD_TYPE               IS 'CRUD 유형: C=생성 U=수정 D=삭제';
COMMENT ON COLUMN FWK_WORK_LIST.LAST_UPDATE_DTIME       IS '최종 수정 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_WORK_LIST.APPROVAL_SEQ            IS '결재 일련번호 (NULL이면 결재 미신청)';
COMMENT ON COLUMN FWK_WORK_LIST.LAST_UPDATE_USER_ID     IS '최종 수정자 ID';
COMMENT ON COLUMN FWK_WORK_LIST.DIST_YN                 IS '운영 반영 여부 (Y/N)';
COMMENT ON COLUMN FWK_WORK_LIST.DIST_DTIME              IS '운영 반영 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_WORK_LIST.GROUP_ID                IS '소속 그룹 ID (FWK_WORK_GROUP.GROUP_ID)';
COMMENT ON COLUMN FWK_WORK_LIST.FIRST_INSERT_USER_ID    IS '최초 등록자 ID (권한이양 후에도 변경되지 않음)';
COMMENT ON COLUMN FWK_WORK_LIST.FILE_NAME               IS '이행스크립트 파일명';


-- =============================================================
-- 3. 인덱스
-- =============================================================

-- 사용자 그룹 검색용 (GROUP_ID LIKE userId||'%')
CREATE INDEX IDX_FWK_WORK_LIST_GROUP ON FWK_WORK_LIST (GROUP_ID);
-- 최초등록자 기준 검색용
CREATE INDEX IDX_FWK_WORK_LIST_USER  ON FWK_WORK_LIST (FIRST_INSERT_USER_ID);

-- =============================================================
-- 4. guest01 권한 부여
-- =============================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON FWK_WORK_GROUP  TO guest01;
GRANT SELECT, INSERT, UPDATE, DELETE ON FWK_WORK_LIST   TO guest01;
GRANT SELECT, INSERT, UPDATE         ON FWK_SETTLEMENT  TO guest01;


-- =============================================================
-- 5. FWK_MENU — 내 작업함 메뉴 항목 등록
-- =============================================================
-- ⚠️  PRIOR_MENU_ID, SORT_ORDER 는 실제 메뉴 구조에 맞게 조정 필요

INSERT INTO FWK_MENU (
    MENU_ID, MENU_NAME, MENU_URL, PRIOR_MENU_ID, SORT_ORDER,
    DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID
) VALUES (
    'v3_approval_workspace', '나의 작업함', '/my-work', 'v3_acl_manage', 1,
    'Y', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin'
);

COMMIT;


-- =============================================================
-- 6. GROUP_ID 컬럼 크기 변경 (VARCHAR2(20) → VARCHAR2(30))
-- =============================================================
-- ⚠️  USER_ID 최대 20자 + '001' = 23자 → ORA-12899 방지를 위해 여유있게 30자로 확장
-- ⚠️  개발자가 DB에서 직접 실행해야 합니다.

ALTER TABLE FWK_WORK_GROUP MODIFY (GROUP_ID VARCHAR2(30) NOT NULL);
ALTER TABLE FWK_WORK_LIST  MODIFY (GROUP_ID VARCHAR2(30));

COMMIT;
