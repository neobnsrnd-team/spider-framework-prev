-- =============================================================
-- Spider-Admin Oracle DDL — 테이블 생성 스크립트
-- =============================================================
-- 생성일: 2026-03-05
-- 원본: docs/sql/DDL/Oracle/ (DBeaver export)
-- 정리: 스키마 접두사 제거, 제약조건명 정규화, PK 인덱스 제거
-- 인덱스: 02_create_indexes.sql 참조
-- 초기 데이터: 03_insert_initial_data.sql 참조
-- 파티셔닝: FWK_BATCH_HIS_PARTITION.sql 참조 (Enterprise Edition)
-- 총 테이블: 62개
-- =============================================================


-- =============================================================
-- 1. 사용자/인증 (5 tables)
-- =============================================================

CREATE TABLE FWK_ROLE (
    ROLE_ID                    VARCHAR2(10)   NOT NULL,
    ROLE_NAME                  VARCHAR2(50)   NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    ROLE_DESC                  VARCHAR2(200),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    RANKING                    VARCHAR2(100),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_ROLE PRIMARY KEY (ROLE_ID)
);

CREATE TABLE FWK_USER (
    USER_ID                    VARCHAR2(20)   NOT NULL,
    USER_NAME                  VARCHAR2(50)   NOT NULL,
    PASSWORD                   VARCHAR2(50),
    ROLE_ID                    VARCHAR2(10),
    POSITION_NAME              VARCHAR2(100),
    ADDRESS                    VARCHAR2(200),
    CLASS_NAME                 VARCHAR2(100),
    EMAIL                      VARCHAR2(100),
    USER_STATE_CODE            VARCHAR2(1),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    ACCESS_IP                  VARCHAR2(15)   DEFAULT '*',
    USER_SSN                   VARCHAR2(13),
    PHONE                      VARCHAR2(13),
    REG_REQ_USER_NAME          VARCHAR2(10),
    TITLE_NAME                 VARCHAR2(10),
    EMP_NO                     VARCHAR2(10),
    BRANCH_NO                  VARCHAR2(10),
    BIZ_AUTH_CODE              VARCHAR2(100),
    LOGIN_FAIL_COUNT           NUMBER(1,0)    DEFAULT 0,
    LAST_PWD_UPDATE_DTIME      VARCHAR2(14),
    DEFAULT_PROJECT_ID         VARCHAR2(20),
    PA_USER                    VARCHAR2(128),
    PRE_PA_USER                VARCHAR2(128),
    PA_USER_SALT               VARCHAR2(50),
    PRE_PA_USER_SALT           VARCHAR2(50),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    PASSWD                     VARCHAR2(100),
    CONSTRAINT PK_FWK_USER PRIMARY KEY (USER_ID)
);

CREATE TABLE FWK_MENU (
    MENU_ID                    VARCHAR2(40)   NOT NULL,
    PRIOR_MENU_ID              VARCHAR2(40),
    SORT_ORDER                 NUMBER(3,0)    DEFAULT 0 NOT NULL,
    MENU_NAME                  VARCHAR2(100)  NOT NULL,
    MENU_URL                   VARCHAR2(200),
    MENU_IMAGE                 VARCHAR2(50),
    DISPLAY_YN                 VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    WEB_APP_ID                 VARCHAR2(70),
    MENU_ENG_NAME              VARCHAR2(200),
    CONSTRAINT PK_FWK_MENU PRIMARY KEY (MENU_ID)
);

CREATE TABLE FWK_USER_MENU (
    USER_ID                    VARCHAR2(20)   NOT NULL,
    MENU_ID                    VARCHAR2(40)   NOT NULL,
    AUTH_CODE                  VARCHAR2(1)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    FAVOR_MENU_ORDER           NUMBER(3,0)    DEFAULT 0 NOT NULL,
    CONSTRAINT PK_FWK_USER_MENU PRIMARY KEY (USER_ID, MENU_ID)
);

CREATE TABLE FWK_ROLE_MENU (
    ROLE_ID                    VARCHAR2(10)   NOT NULL,
    MENU_ID                    VARCHAR2(40)   NOT NULL,
    AUTH_CODE                  VARCHAR2(1)    NOT NULL,
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_ROLE_MENU PRIMARY KEY (ROLE_ID, MENU_ID)
);


-- =============================================================
-- 2. 접근 기록 (2 tables)
-- =============================================================

CREATE TABLE FWK_USER_ACCESS_HIS (
    USER_ID                    VARCHAR2(20)   NOT NULL,
    ACCESS_DTIME               VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    ACCESS_IP                  VARCHAR2(200),
    ACCESS_URL                 VARCHAR2(1000),
    INPUT_DATA                 VARCHAR2(4000),
    RESULT_MESSAGE             VARCHAR2(200)
);

CREATE TABLE FWK_ACCESS_USER (
    TRX_ID                     VARCHAR2(50)   NOT NULL,
    CUST_USER_ID               VARCHAR2(50)   NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    GUBUN_TYPE                 VARCHAR2(1)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_ACCESS_USER PRIMARY KEY (GUBUN_TYPE, TRX_ID, CUST_USER_ID)
);


-- =============================================================
-- 3. 코드/조직 (5 tables)
-- =============================================================

CREATE TABLE FWK_CODE_GROUP (
    CODE_GROUP_ID              VARCHAR2(8)    NOT NULL,
    CODE_GROUP_NAME            VARCHAR2(100)  NOT NULL,
    CODE_GROUP_DESC            VARCHAR2(200),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    BIZ_GROUP_ID               VARCHAR2(20),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_CODE_GROUP PRIMARY KEY (CODE_GROUP_ID)
);

CREATE TABLE FWK_CODE (
    CODE_GROUP_ID              VARCHAR2(8)    NOT NULL,
    CODE                       VARCHAR2(50)   NOT NULL,
    CODE_NAME                  VARCHAR2(100)  NOT NULL,
    CODE_DESC                  VARCHAR2(200),
    SORT_ORDER                 NUMBER(4,0)    DEFAULT 0 NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    CODE_ENGNAME               VARCHAR2(300),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_CODE PRIMARY KEY (CODE_GROUP_ID, CODE)
);

CREATE TABLE FWK_ORG (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    ORG_NAME                   VARCHAR2(50)   NOT NULL,
    ORG_DESC                   VARCHAR2(200),
    START_TIME                 VARCHAR2(6)    NOT NULL,
    END_TIME                   VARCHAR2(6)    NOT NULL,
    XML_ROOT_TAG               VARCHAR2(20),
    CONSTRAINT PK_FWK_ORG PRIMARY KEY (ORG_ID)
);

-- 기관별 코드 매핑 (원본 DDL 없음, schema-ci.sql 기준으로 Oracle 타입 변환)
CREATE TABLE FWK_ORG_CODE (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    CODE_GROUP_ID              VARCHAR2(8)    NOT NULL,
    CODE                       VARCHAR2(30)   NOT NULL,
    ORG_CODE                   VARCHAR2(30)   NOT NULL,
    PRIORITY                   VARCHAR2(3)    DEFAULT '1',
    CONSTRAINT PK_FWK_ORG_CODE PRIMARY KEY (ORG_ID, CODE_GROUP_ID, CODE, ORG_CODE)
);

CREATE TABLE FWK_BIZ_GROUP (
    BIZ_GROUP_ID               VARCHAR2(20)   NOT NULL,
    BIZ_GROUP_NAME             VARCHAR2(50),
    BIZ_GROUP_DESC             VARCHAR2(200),
    BIZ_L_GROUP_ID             VARCHAR2(20)   NOT NULL,
    BIZ_L_GROUP_NAME           VARCHAR2(50),
    BIZ_SUB_GROUP_ID           VARCHAR2(20),
    BIZ_SUB_GROUP_NAME         VARCHAR2(50),
    DEFAULT_WORK_SPACE_ID      VARCHAR2(20),
    "DEPTH"                    VARCHAR2(1),
    BIZ_DOMAIN                 VARCHAR2(10),
    KOR_PATH_TEXT              VARCHAR2(100),
    ENG_PKG_TEXT               VARCHAR2(50),
    CONSTRAINT PK_FWK_BIZ_GROUP PRIMARY KEY (BIZ_GROUP_ID)
);


-- =============================================================
-- 4. 거래/전문 (9 tables)
-- =============================================================

CREATE TABLE FWK_TRX (
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    OPER_MODE_TYPE             VARCHAR2(1),
    TRX_STOP_YN                VARCHAR2(1)    DEFAULT 'Y',
    TRX_NAME                   VARCHAR2(50),
    TRX_DESC                   VARCHAR2(200),
    TRX_TYPE                   VARCHAR2(1)    DEFAULT '1' NOT NULL,
    RETRY_TRX_YN               VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    MAX_RETRY_COUNT            NUMBER(3,0)    DEFAULT 0 NOT NULL,
    RETRY_MI_CYCLE             VARCHAR2(4),
    BIZ_GROUP_ID               VARCHAR2(10),
    BIZDAY_TRX_YN              VARCHAR2(1),
    BIZDAY_TRX_START_TIME      VARCHAR2(4),
    BIZDAY_TRX_END_TIME        VARCHAR2(4),
    SATURDAY_TRX_YN            VARCHAR2(1),
    SATURDAY_TRX_START_TIME    VARCHAR2(4),
    SATURDAY_TRX_END_TIME      VARCHAR2(4),
    HOLIDAY_TRX_YN             VARCHAR2(1),
    HOLIDAY_TRX_START_TIME     VARCHAR2(4),
    HOLIDAY_TRX_END_TIME       VARCHAR2(4),
    TRX_STOP_REASON            VARCHAR2(200),
    CONSTRAINT PK_FWK_TRX PRIMARY KEY (TRX_ID)
);

CREATE TABLE FWK_TRX_HISTORY (
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    OPER_MODE_TYPE             VARCHAR2(1),
    TRX_STOP_YN                VARCHAR2(1)    DEFAULT 'Y',
    TRX_NAME                   VARCHAR2(50),
    TRX_DESC                   VARCHAR2(200),
    TRX_TYPE                   VARCHAR2(1)    DEFAULT '1' NOT NULL,
    RETRY_TRX_YN               VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    MAX_RETRY_COUNT            NUMBER(3,0)    DEFAULT 0 NOT NULL,
    RETRY_MI_CYCLE             VARCHAR2(4),
    BIZ_GROUP_ID               VARCHAR2(10),
    BIZDAY_TRX_YN              VARCHAR2(1),
    BIZDAY_TRX_START_TIME      VARCHAR2(4),
    BIZDAY_TRX_END_TIME        VARCHAR2(4),
    SATURDAY_TRX_YN            VARCHAR2(1),
    SATURDAY_TRX_START_TIME    VARCHAR2(4),
    SATURDAY_TRX_END_TIME      VARCHAR2(4),
    HOLIDAY_TRX_YN             VARCHAR2(1),
    HOLIDAY_TRX_START_TIME     VARCHAR2(4),
    HOLIDAY_TRX_END_TIME       VARCHAR2(4),
    TRX_STOP_REASON            VARCHAR2(200),
    VERSION                    NUMBER(3,0)    NOT NULL,
    HISTORY_REASON             VARCHAR2(100),
    CONSTRAINT PK_FWK_TRX_HISTORY PRIMARY KEY (TRX_ID, VERSION)
);

CREATE TABLE FWK_TRX_MESSAGE (
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    IO_TYPE                    VARCHAR2(1)    NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    STD_MESSAGE_ID             VARCHAR2(50),
    RES_MESSAGE_ID             VARCHAR2(50),
    STD_RES_MESSAGE_ID         VARCHAR2(50),
    PROXY_RES_YN               VARCHAR2(1)    DEFAULT 'Y',
    PROXY_RES_DATA             RAW(2000),
    EXECUTE_SEQ                NUMBER(3,0)    DEFAULT 1,
    PROXY_RES_TYPE             VARCHAR2(1)    DEFAULT 'M',
    HEX_LOG_YN                 VARCHAR2(1),
    MULTI_RES_YN               VARCHAR2(1),
    RES_TYPE_FIELD_ID          VARCHAR2(50),
    MULTI_RES_TYPE             VARCHAR2(1)    DEFAULT '1',
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    TIMEOUT_SEC                NUMBER(3,0)    DEFAULT 210 NOT NULL,
    LEGACY_MESSAGE_ID          VARCHAR2(20),
    TARGET_SERVICE_URI         VARCHAR2(200),
    CONSTRAINT PK_FWK_TRX_MESSAGE PRIMARY KEY (TRX_ID, ORG_ID, IO_TYPE)
);

CREATE TABLE FWK_TRX_MESSAGE_HISTORY (
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    IO_TYPE                    VARCHAR2(1)    NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    STD_MESSAGE_ID             VARCHAR2(50),
    RES_MESSAGE_ID             VARCHAR2(50),
    STD_RES_MESSAGE_ID         VARCHAR2(50),
    PROXY_RES_YN               VARCHAR2(1)    DEFAULT 'Y',
    PROXY_RES_DATA             RAW(2000),
    EXECUTE_SEQ                NUMBER(3,0)    DEFAULT 1,
    PROXY_RES_TYPE             VARCHAR2(1)    DEFAULT 'M',
    HEX_LOG_YN                 VARCHAR2(1),
    MULTI_RES_YN               VARCHAR2(1),
    RES_TYPE_FIELD_ID          VARCHAR2(50),
    MULTI_RES_TYPE             VARCHAR2(1)    DEFAULT '1',
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    TIMEOUT_SEC                NUMBER(3,0)    DEFAULT 210 NOT NULL,
    LEGACY_MESSAGE_ID          VARCHAR2(20),
    VERSION                    NUMBER(3,0)    NOT NULL,
    CONSTRAINT PK_FWK_TRX_MESSAGE_HISTORY PRIMARY KEY (TRX_ID, ORG_ID, IO_TYPE, VERSION)
);

CREATE TABLE FWK_TRX_STOP_HISTORY (
    GUBUN_TYPE                 VARCHAR2(1)    NOT NULL,
    TRX_STOP_UPDATE_DTIME      VARCHAR2(14)   NOT NULL,
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    TRX_STOP_REASON            VARCHAR2(50),
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    TRX_STOP_YN                VARCHAR2(1),
    CONSTRAINT PK_FWK_TRX_STOP_HISTORY PRIMARY KEY (GUBUN_TYPE, TRX_STOP_UPDATE_DTIME, TRX_ID)
);

CREATE TABLE FWK_MESSAGE (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    MESSAGE_NAME               VARCHAR2(100)  NOT NULL,
    MESSAGE_DESC               VARCHAR2(200),
    MESSAGE_TYPE               VARCHAR2(1),
    PARENT_MESSAGE_ID          VARCHAR2(50),
    HEADER_YN                  VARCHAR2(1)    DEFAULT 'N',
    REQUEST_YN                 VARCHAR2(2)    DEFAULT NULL,
    TRX_TYPE                   VARCHAR2(1)    DEFAULT '1' NOT NULL,
    PRE_LOAD_YN                VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    LOG_LEVEL                  VARCHAR2(1),
    BIZ_DOMAIN                 VARCHAR2(10),
    VALIDATION_USE_YN          VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    LOCK_YN                    VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    CUR_VERSION                NUMBER(3,0),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    CONSTRAINT PK_FWK_MESSAGE PRIMARY KEY (ORG_ID, MESSAGE_ID)
);

CREATE TABLE FWK_MESSAGE_FIELD (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    MESSAGE_FIELD_ID           VARCHAR2(50)   NOT NULL,
    SORT_ORDER                 NUMBER(4,0)    DEFAULT 0 NOT NULL,
    DATA_TYPE                  VARCHAR2(1)    NOT NULL,
    DATA_LENGTH                NUMBER         NOT NULL,
    "SCALE"                    NUMBER(2,0),
    ALIGN                      VARCHAR2(1)    NOT NULL,
    FILLER                     VARCHAR2(20),
    FIELD_TYPE                 VARCHAR2(10),
    USE_MODE                   VARCHAR2(20),
    REQUIRED_YN                VARCHAR2(1)    DEFAULT 'Y',
    FIELD_TAG                  VARCHAR2(500),
    CODE_GROUP                 VARCHAR2(8),
    DEFAULT_VALUE              VARCHAR2(500),
    TEST_VALUE                 VARCHAR2(500),
    REMARK                     VARCHAR2(1)    DEFAULT NULL,
    LOG_YN                     VARCHAR2(1)    DEFAULT 'Y',
    CODE_MAPPING_YN            VARCHAR2(1)    DEFAULT 'N',
    MESSAGE_FIELD_NAME         VARCHAR2(100),
    MESSAGE_FIELD_DESC         VARCHAR2(200),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    VALIDATION_RULE_ID         VARCHAR2(20),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    FIELD_FORMAT               VARCHAR2(200),
    FIELD_FORMAT_DESC          VARCHAR2(500),
    FIELD_OPTION               VARCHAR2(200),
    FIELD_REPEAT_CNT           NUMBER(10,0)
);

CREATE TABLE FWK_MESSAGE_FIELD_HISTORY (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    MESSAGE_FIELD_ID           VARCHAR2(50)   NOT NULL,
    VERSION                    NUMBER(3,0)    NOT NULL,
    SORT_ORDER                 NUMBER(4,0)    DEFAULT 0 NOT NULL,
    DATA_TYPE                  VARCHAR2(1)    NOT NULL,
    DATA_LENGTH                NUMBER         NOT NULL,
    "SCALE"                    NUMBER(2,0),
    ALIGN                      VARCHAR2(1)    NOT NULL,
    FILLER                     VARCHAR2(20),
    FIELD_TYPE                 VARCHAR2(10),
    USE_MODE                   VARCHAR2(20),
    REQUIRED_YN                VARCHAR2(1)    DEFAULT 'Y',
    FIELD_TAG                  VARCHAR2(500),
    CODE_GROUP                 VARCHAR2(8),
    DEFAULT_VALUE              VARCHAR2(500),
    TEST_VALUE                 VARCHAR2(500),
    REMARK                     VARCHAR2(1)    DEFAULT NULL,
    LOG_YN                     VARCHAR2(1)    DEFAULT 'Y',
    CODE_MAPPING_YN            VARCHAR2(1)    DEFAULT 'N',
    MESSAGE_FIELD_NAME         VARCHAR2(100),
    MESSAGE_FIELD_DESC         VARCHAR2(200),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    VALIDATION_RULE_ID         VARCHAR2(20),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    CONSTRAINT PK_FWK_MESSAGE_FIELD_HISTORY PRIMARY KEY (ORG_ID, MESSAGE_ID, MESSAGE_FIELD_ID, VERSION)
);

CREATE TABLE FWK_MESSAGE_FIELD_MAPPING (
    TRG_ORG_ID                 VARCHAR2(10)   NOT NULL,
    TRG_MESSAGE_ID             VARCHAR2(50)   NOT NULL,
    SRC_ORG_ID                 VARCHAR2(10)   NOT NULL,
    TRG_MESSAGE_FIELD_ID       VARCHAR2(50)   NOT NULL,
    SRC_MESSAGE_ID             VARCHAR2(50)   NOT NULL,
    MAPPING_EXPR               VARCHAR2(500),
    SRC_PARENT_MESSAGE_FIELD_ID VARCHAR2(50),
    TRG_PARENT_MESSAGE_FIELD_ID VARCHAR2(50)
);


-- =============================================================
-- 5. 전문 테스트/인스턴스 (2 tables)
-- =============================================================

CREATE TABLE FWK_MESSAGE_TEST (
    TEST_SNO                   NUMBER(30,0)   NOT NULL,
    USER_ID                    VARCHAR2(20)   NOT NULL,
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    MESSAGE_ID                 VARCHAR2(30)   NOT NULL,
    HEADER_YN                  VARCHAR2(1)    NOT NULL,
    XML_YN                     VARCHAR2(1)    NOT NULL,
    TEST_NAME                  VARCHAR2(50)   NOT NULL,
    TEST_DESC                  VARCHAR2(500),
    TEST_DATA                  VARCHAR2(4000),
    TEST_DATA1                 VARCHAR2(4000),
    TEST_DATA2                 VARCHAR2(4000),
    TEST_DATA3                 VARCHAR2(4000),
    TEST_DATA4                 VARCHAR2(4000),
    TEST_GROUP_ID              VARCHAR2(20),
    TRX_ID                     VARCHAR2(50),
    DEFAULT_PROXY_YN           VARCHAR2(1),
    TEST_HEADER_DATA           CLOB,
    TEST_HEADER_USE_YN         VARCHAR2(1),
    PROXY_FIELD                VARCHAR2(50),
    PROXY_VALUE                VARCHAR2(100),
    LAST_UPDATE_DTIME          VARCHAR2(14),
    LAST_UPDATE_USERID         VARCHAR2(20),
    TEST_HEADER_DATA1          VARCHAR2(4000),
    CONSTRAINT PK_FWK_MESSAGE_TEST PRIMARY KEY (TEST_SNO)
);

CREATE TABLE FWK_MESSAGE_INSTANCE (
    MESSAGE_SNO                VARCHAR2(30)   NOT NULL,
    TRX_ID                     VARCHAR2(40)   NOT NULL,
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    IO_TYPE                    VARCHAR2(1)    NOT NULL,
    REQ_RES_TYPE               VARCHAR2(1)    NOT NULL,
    MESSAGE_ID                 VARCHAR2(50)   NOT NULL,
    TRX_TRACKING_NO            VARCHAR2(30)   NOT NULL,
    USER_ID                    VARCHAR2(20)   NOT NULL,
    LOG_DTIME                  VARCHAR2(17)   NOT NULL,
    LAST_LOG_DTIME             VARCHAR2(17)   NOT NULL,
    LAST_RT_CODE               VARCHAR2(40)   NOT NULL,
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    RETRY_TRX_YN               VARCHAR2(1)    NOT NULL,
    MESSAGE_DATA               VARCHAR2(4000),
    TRX_DTIME                  VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CHANNEL_TYPE               VARCHAR2(10),
    URI                        VARCHAR2(200),
    LOG_DATA                   CLOB,
    LOG_DATA2                  VARCHAR2(4000),
    LOG_DATA3                  VARCHAR2(4000),
    LOG_DATA4                  VARCHAR2(4000),
    TESTCASE_ID                VARCHAR2(50),
    TESTCASE_SNO               NUMBER(30,0),
    SUCCESS_YN                 VARCHAR2(1)
);


-- =============================================================
-- 6. Gateway/Interface (6 tables)
-- =============================================================

CREATE TABLE FWK_GATEWAY (
    GW_ID                      VARCHAR2(20)   NOT NULL,
    GW_NAME                    VARCHAR2(200),
    THREAD_COUNT               NUMBER(3,0),
    GW_PROPERTIES              VARCHAR2(500),
    GW_DESC                    VARCHAR2(1000),
    GW_APP_NAME                VARCHAR2(200)  DEFAULT NULL,
    IO_TYPE                    VARCHAR2(1)    DEFAULT 'O' NOT NULL,
    CONSTRAINT PK_FWK_GATEWAY PRIMARY KEY (GW_ID)
);

CREATE TABLE FWK_SYSTEM (
    GW_ID                      VARCHAR2(20)   NOT NULL,
    SYSTEM_ID                  VARCHAR2(20)   NOT NULL,
    OPER_MODE_TYPE             VARCHAR2(1),
    IP                         VARCHAR2(15),
    PORT                       VARCHAR2(5),
    STOP_YN                    VARCHAR2(1)    DEFAULT 'N',
    SYSTEM_DESC                VARCHAR2(1000),
    APPLIED_WAS_INSTANCE       VARCHAR2(100),
    CONSTRAINT PK_FWK_SYSTEM PRIMARY KEY (GW_ID, SYSTEM_ID)
);

CREATE TABLE FWK_MESSAGE_HANDLER (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    TRX_TYPE                   VARCHAR2(1)    NOT NULL,
    IO_TYPE                    VARCHAR2(1)    NOT NULL,
    HANDLER                    VARCHAR2(200),
    HANDLER_DESC               VARCHAR2(2000),
    STOP_YN                    VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    OPER_MODE_TYPE             VARCHAR2(1)    NOT NULL,
    CONSTRAINT PK_FWK_MESSAGE_HANDLER PRIMARY KEY (ORG_ID, TRX_TYPE, IO_TYPE, OPER_MODE_TYPE)
);

CREATE TABLE FWK_LISTENER_TRX_MESSAGE (
    GW_ID                      VARCHAR2(20)   NOT NULL,
    REQ_ID_CODE                VARCHAR2(40)   NOT NULL,
    TRX_ID                     VARCHAR2(40),
    ORG_ID                     VARCHAR2(10),
    IO_TYPE                    VARCHAR2(1),
    BIZ_APP_ID                 VARCHAR2(100),
    CONSTRAINT PK_FWK_LISTENER_TRX_MESSAGE PRIMARY KEY (GW_ID, REQ_ID_CODE)
);

CREATE TABLE FWK_LISTENER_CONNECTOR_MAPPING (
    LISTENER_GW_ID             VARCHAR2(20)   NOT NULL,
    LISTENER_SYSTEM_ID         VARCHAR2(20)   NOT NULL,
    IDENTIFIER                 VARCHAR2(100)  NOT NULL,
    CONNECTOR_GW_ID            VARCHAR2(20)   NOT NULL,
    CONNECTOR_SYSTEM_ID        VARCHAR2(20)   NOT NULL,
    DESCRIPTION                VARCHAR2(200),
    CONSTRAINT PK_FWK_LISTENER_CONN_MAPPING PRIMARY KEY (LISTENER_GW_ID, LISTENER_SYSTEM_ID, IDENTIFIER)
);

CREATE TABLE FWK_TRANSPORT (
    ORG_ID                     VARCHAR2(10)   NOT NULL,
    TRX_TYPE                   VARCHAR2(1)    NOT NULL,
    IO_TYPE                    VARCHAR2(1)    NOT NULL,
    REQ_RES_TYPE               VARCHAR2(1)    NOT NULL,
    GW_ID                      VARCHAR2(20),
    CONSTRAINT PK_FWK_TRANSPORT PRIMARY KEY (ORG_ID, TRX_TYPE, IO_TYPE, REQ_RES_TYPE)
);


-- =============================================================
-- 7. WAS (7 tables)
-- =============================================================

CREATE TABLE FWK_WAS_GROUP (
    WAS_GROUP_ID               VARCHAR2(20)   NOT NULL,
    WAS_GROUP_NAME             VARCHAR2(50),
    WAS_GROUP_DESC             VARCHAR2(200),
    CONSTRAINT PK_FWK_WAS_GROUP PRIMARY KEY (WAS_GROUP_ID)
);

CREATE TABLE FWK_WAS_INSTANCE (
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    INSTANCE_NAME              VARCHAR2(50),
    INSTANCE_DESC              VARCHAR2(200),
    WAS_CONFIG_ID              VARCHAR2(10),
    INSTANCE_TYPE              VARCHAR2(1),
    IP                         VARCHAR2(15),
    PORT                       VARCHAR2(5),
    OPER_MODE_TYPE             VARCHAR2(1),
    CONSTRAINT PK_FWK_WAS_INSTANCE PRIMARY KEY (INSTANCE_ID)
);

CREATE TABLE FWK_WAS_GROUP_INSTANCE (
    WAS_GROUP_ID               VARCHAR2(20)   NOT NULL,
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    CONSTRAINT PK_FWK_WAS_GROUP_INSTANCE PRIMARY KEY (WAS_GROUP_ID, INSTANCE_ID)
);

CREATE TABLE FWK_WAS_LISTENER (
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    GW_ID                      VARCHAR2(20)   NOT NULL,
    SYSTEM_ID                  VARCHAR2(20)   NOT NULL,
    WAS_INSTANCE_STATUS        VARCHAR2(5),
    ACTIVE_COUNT_IDLE          NUMBER(3,0),
    LAST_UPDATE_DTIME          VARCHAR2(20),
    CONSTRAINT PK_FWK_WAS_LISTENER PRIMARY KEY (INSTANCE_ID, GW_ID, SYSTEM_ID)
);

CREATE TABLE FWK_WAS_PROPERTY (
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    PROPERTY_GROUP_ID          VARCHAR2(20)   NOT NULL,
    PROPERTY_ID                VARCHAR2(50)   NOT NULL,
    PROPERTY_VALUE             VARCHAR2(1000) NOT NULL,
    PROPERTY_DESC              VARCHAR2(1000),
    CUR_VERSION                NUMBER,
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_WAS_PROPERTY PRIMARY KEY (INSTANCE_ID, PROPERTY_GROUP_ID, PROPERTY_ID)
);

CREATE TABLE FWK_WAS_PROPERTY_HISTORY (
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    PROPERTY_GROUP_ID          VARCHAR2(20)   NOT NULL,
    PROPERTY_ID                VARCHAR2(50)   NOT NULL,
    VERSION                    NUMBER(5,0)    NOT NULL,
    PROPERTY_VALUE             VARCHAR2(1000) NOT NULL,
    PROPERTY_DESC              VARCHAR2(1000),
    REASON                     VARCHAR2(2000),
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CONSTRAINT PK_FWK_WAS_PROPERTY_HISTORY PRIMARY KEY (INSTANCE_ID, PROPERTY_GROUP_ID, PROPERTY_ID, VERSION)
);

CREATE TABLE FWK_WAS_EXEC_BATCH (
    BATCH_APP_ID               VARCHAR2(50)   NOT NULL,
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_WAS_EXEC_BATCH PRIMARY KEY (BATCH_APP_ID, INSTANCE_ID)
);


-- =============================================================
-- 8. 배치 (2 tables)
-- =============================================================

CREATE TABLE FWK_BATCH_APP (
    BATCH_APP_ID               VARCHAR2(50)   NOT NULL,
    BATCH_APP_NAME             VARCHAR2(50),
    BATCH_APP_FILE_NAME        VARCHAR2(200)  NOT NULL,
    BATCH_APP_DESC             VARCHAR2(200),
    PRE_BATCH_APP_ID           VARCHAR2(50),
    BATCH_CYCLE                VARCHAR2(1)    NOT NULL,
    CRON_TEXT                  VARCHAR2(20),
    RETRYABLE_YN               VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    PER_WAS_YN                 VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    IMPORTANT_TYPE             VARCHAR2(1)    NOT NULL,
    PROPERTIES                 VARCHAR2(500),
    TRX_ID                     VARCHAR2(20),
    ORG_ID                     VARCHAR2(10),
    IO_TYPE                    VARCHAR2(1),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    SLA_SECONDS                NUMBER(10),
    CONSTRAINT PK_FWK_BATCH_APP PRIMARY KEY (BATCH_APP_ID)
);

-- 참고: 월별 파티셔닝이 필요한 경우 docs/sql/FWK_BATCH_HIS_PARTITION.sql 참조
-- (Oracle Enterprise Edition 파티셔닝 옵션 필요)
CREATE TABLE FWK_BATCH_HIS (
    BATCH_APP_ID               VARCHAR2(50)   NOT NULL,
    INSTANCE_ID                VARCHAR2(4)    NOT NULL,
    BATCH_DATE                 VARCHAR2(8)    NOT NULL,
    BATCH_EXECUTE_SEQ          NUMBER(3,0)    DEFAULT 1 NOT NULL,
    LOG_DTIME                  VARCHAR2(17)   DEFAULT TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3') NOT NULL,
    BATCH_END_DTIME            VARCHAR2(17)   DEFAULT NULL,
    RES_RT_CODE                VARCHAR2(40),
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    ERROR_CODE                 VARCHAR2(40),
    ERROR_REASON               VARCHAR2(4000),
    RECORD_COUNT               NUMBER(10,0),
    EXECUTE_COUNT              NUMBER(10,0),
    SUCCESS_COUNT              NUMBER(10,0),
    FAIL_COUNT                 NUMBER(10,0),
    CONSTRAINT PK_FWK_BATCH_HIS PRIMARY KEY (BATCH_APP_ID, INSTANCE_ID, BATCH_DATE, BATCH_EXECUTE_SEQ)
);


-- =============================================================
-- 9. 오류 (6 tables)
-- =============================================================

CREATE TABLE FWK_ERROR (
    ERROR_CODE                 VARCHAR2(40)   NOT NULL,
    TRX_ID                     VARCHAR2(40),
    ERROR_TITLE                VARCHAR2(200)  NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    ORG_ID                     VARCHAR2(10),
    ORG_ERROR_CODE             VARCHAR2(20),
    ERROR_LEVEL                VARCHAR2(1)    DEFAULT '1' NOT NULL,
    ERROR_HTTPCODE             VARCHAR2(20),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_ERROR PRIMARY KEY (ERROR_CODE)
);

CREATE TABLE FWK_ERROR_DESC (
    ERROR_CODE                 VARCHAR2(40)   NOT NULL,
    LOCALE_CODE                VARCHAR2(5)    NOT NULL,
    ERROR_TITLE                VARCHAR2(200),
    PB_ERROR_TITLE             VARCHAR2(400),
    ETC_ERROR_TITLE            VARCHAR2(400),
    ERROR_CAUSE_DESC           VARCHAR2(1600),
    ERROR_GUIDE_DESC           VARCHAR2(1600),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    HELP_PAGE_URL              VARCHAR2(200),
    IBS_ERROR_GUIDE_DESC       VARCHAR2(1600),
    CMS_ERROR_GUIDE_DESC       VARCHAR2(1600),
    ETC_ERROR_GUIDE_DESC       VARCHAR2(1600),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_ERROR_DESC PRIMARY KEY (ERROR_CODE, LOCALE_CODE)
);

CREATE TABLE FWK_ERROR_HIS (
    ERROR_CODE                 VARCHAR2(40)   NOT NULL,
    ERROR_SER_NO               VARCHAR2(28)   NOT NULL,
    CUST_USER_ID               VARCHAR2(20)   NOT NULL,
    ERROR_MESSAGE              VARCHAR2(500),
    ERROR_OCCUR_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    ERROR_URL                  VARCHAR2(300),
    ERROR_TRACE                VARCHAR2(4000),
    ERROR_INSTANCE_ID          VARCHAR2(10),
    CONSTRAINT PK_FWK_ERROR_HIS PRIMARY KEY (ERROR_CODE, ERROR_SER_NO)
);

CREATE TABLE FWK_HANDLE_APP (
    HANDLE_APP_ID              VARCHAR2(50)   NOT NULL,
    HANDLE_APP_NAME            VARCHAR2(50)   NOT NULL,
    HANDLE_APP_DESC            VARCHAR2(200)  NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    SYS_PARAM_VALUE            VARCHAR2(1000),
    PARAM_DESC                 VARCHAR2(2000),
    HANDLE_APP_FILE            VARCHAR2(100),
    CONSTRAINT PK_FWK_HANDLE_APP PRIMARY KEY (HANDLE_APP_ID)
);

CREATE TABLE FWK_ERROR_HANDLE_APP (
    ERROR_CODE                 VARCHAR2(40)   NOT NULL,
    HANDLE_APP_ID              VARCHAR2(50)   NOT NULL,
    USER_PARAM_VALUE           VARCHAR2(1000),
    CONSTRAINT PK_FWK_ERROR_HANDLE_APP PRIMARY KEY (ERROR_CODE, HANDLE_APP_ID)
);

CREATE TABLE FWK_ERROR_HANDLE_HIS (
    ERROR_CODE                 VARCHAR2(40)   NOT NULL,
    HANDLE_APP_ID              VARCHAR2(50)   NOT NULL,
    ERROR_SER_NO               VARCHAR2(28)   NOT NULL,
    ERROR_HANDLE_DTIME         VARCHAR2(14)   NOT NULL,
    ERROR_HANDLE_RT_CODE       VARCHAR2(8)    NOT NULL,
    ERROR_HANDLE_TEXT          VARCHAR2(2000),
    CONSTRAINT PK_FWK_ERROR_HANDLE_HIS PRIMARY KEY (ERROR_CODE, HANDLE_APP_ID, ERROR_SER_NO)
);


-- =============================================================
-- 10. 게시판 (5 tables)
-- =============================================================

CREATE TABLE FWK_BOARD (
    BOARD_ID                   VARCHAR2(20)   NOT NULL,
    BOARD_NAME                 VARCHAR2(50)   NOT NULL,
    BOARD_TYPE                 VARCHAR2(1)    NOT NULL,
    ADMIN_ID                   VARCHAR2(20),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_BOARD PRIMARY KEY (BOARD_ID)
);

CREATE TABLE FWK_BOARD_AUTH (
    USER_ID                    VARCHAR2(20)   NOT NULL,
    BOARD_ID                   VARCHAR2(20)   NOT NULL,
    AUTH_CODE                  VARCHAR2(1)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_BOARD_AUTH PRIMARY KEY (USER_ID, BOARD_ID)
);

CREATE TABLE FWK_BOARD_CATEGORY (
    BOARD_ID                   VARCHAR2(20)   NOT NULL,
    CATEGORY_SEQ               VARCHAR2(10)   NOT NULL,
    CATEGORY_NAME              VARCHAR2(50)   NOT NULL,
    CONSTRAINT PK_FWK_BOARD_CATEGORY PRIMARY KEY (BOARD_ID, CATEGORY_SEQ)
);

CREATE TABLE FWK_ARTICLE (
    ARTICLE_SEQ                NUMBER         NOT NULL,
    BOARD_ID                   VARCHAR2(20)   NOT NULL,
    CATEGORY_SEQ               VARCHAR2(20),
    TOP_YN                     VARCHAR2(1)    NOT NULL,
    REF_ARTICLE_SEQ            NUMBER,
    "POSITION"                 NUMBER         NOT NULL,
    STEP                       NUMBER         NOT NULL,
    TITLE                      VARCHAR2(500)  NOT NULL,
    WRITER_ID                  VARCHAR2(20)   NOT NULL,
    WRITER_NAME                VARCHAR2(50)   NOT NULL,
    READ_CNT                   NUMBER         NOT NULL,
    ATTATCH_FILE_PATH_1        VARCHAR2(500),
    DOWNLOAD_CNT_1             NUMBER,
    ATTATCH_FILE_PATH_2        VARCHAR2(500),
    DOWNLOAD_CNT_2             NUMBER,
    ATTATCH_FILE_PATH_3        VARCHAR2(500),
    DOWNLOAD_CNT_3             NUMBER,
    REGIST_DTIME               VARCHAR2(14)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONTENT                    CLOB
);

CREATE TABLE FWK_ARTICLE_USER (
    USER_ID                    VARCHAR2(20)   NOT NULL,
    BOARD_ID                   VARCHAR2(20)   NOT NULL,
    ARTICLE_SEQ                NUMBER         NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CONSTRAINT PK_FWK_ARTICLE_USER PRIMARY KEY (USER_ID, BOARD_ID, ARTICLE_SEQ)
);


-- =============================================================
-- 11. 프로퍼티/모니터링 (4 tables)
-- =============================================================

CREATE TABLE FWK_PROPERTY (
    PROPERTY_GROUP_ID          VARCHAR2(20)   NOT NULL,
    PROPERTY_ID                VARCHAR2(100)  NOT NULL,
    PROPERTY_NAME              VARCHAR2(100)  NOT NULL,
    PROPERTY_DESC              VARCHAR2(300)  NOT NULL,
    DATA_TYPE                  VARCHAR2(1),
    VALID_DATA                 VARCHAR2(1000),
    DEFAULT_VALUE              VARCHAR2(1000),
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CUR_VERSION                NUMBER,
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    CONSTRAINT PK_FWK_PROPERTY PRIMARY KEY (PROPERTY_GROUP_ID, PROPERTY_ID)
);

CREATE TABLE FWK_PROPERTY_HISTORY (
    PROPERTY_GROUP_ID          VARCHAR2(20)   NOT NULL,
    PROPERTY_ID                VARCHAR2(100)  NOT NULL,
    VERSION                    NUMBER(5,0)    NOT NULL,
    PROPERTY_NAME              VARCHAR2(100)  NOT NULL,
    PROPERTY_DESC              VARCHAR2(300)  NOT NULL,
    DATA_TYPE                  VARCHAR2(1),
    VALID_DATA                 VARCHAR2(1000),
    DEFAULT_VALUE              VARCHAR2(1000),
    REASON                     VARCHAR2(2000),
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CONSTRAINT PK_FWK_PROPERTY_HISTORY PRIMARY KEY (PROPERTY_GROUP_ID, PROPERTY_ID, VERSION)
);

CREATE TABLE FWK_MONITOR (
    MONITOR_ID                 VARCHAR2(20)   NOT NULL,
    MONITOR_NAME               VARCHAR2(50)   NOT NULL,
    MONITOR_QUERY              VARCHAR2(2000),
    ALERT_CONDITION            VARCHAR2(100),
    ALERT_MESSAGE              VARCHAR2(100),
    REFRESH_TERM               VARCHAR2(4)    NOT NULL,
    DETAIL_QUERY               VARCHAR2(2000),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_MONITOR PRIMARY KEY (MONITOR_ID)
);

CREATE TABLE FWK_VALIDATOR (
    VALIDATOR_ID               VARCHAR2(20)   NOT NULL,
    VALIDATOR_NAME             VARCHAR2(50)   NOT NULL,
    VALIDATOR_DESC             VARCHAR2(200),
    BIZ_DOMAIN                 VARCHAR2(10)   NOT NULL,
    JAVA_CLASS_NAME            VARCHAR2(100)  NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_VALIDATOR PRIMARY KEY (VALIDATOR_ID)
);


-- =============================================================
-- 12. 서비스/비즈니스 (11 tables)
-- =============================================================

CREATE TABLE FWK_SERVICE (
    SERVICE_ID                 VARCHAR2(30)   NOT NULL,
    SERVICE_NAME               VARCHAR2(100)  NOT NULL,
    SERVICE_DESC               VARCHAR2(300),
    CLASS_NAME                 VARCHAR2(100),
    METHOD_NAME                VARCHAR2(50),
    SERVICE_TYPE               VARCHAR2(1),
    PRE_PROCESS_APP_ID         VARCHAR2(100),
    POST_PROCESS_APP_ID        VARCHAR2(100),
    TIME_CHECK_YN              VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    START_TIME                 VARCHAR2(4)    DEFAULT '2400',
    END_TIME                   VARCHAR2(4)    DEFAULT '2400',
    BIZ_DAY_CHECK_YN           VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    TRX_ID                     VARCHAR2(40),
    ORG_ID                     VARCHAR2(10),
    IO_TYPE                    VARCHAR2(1),
    BIZ_GROUP_ID               VARCHAR2(20),
    WORK_SPACE_ID              VARCHAR2(20),
    LOGIN_ONLY_YN              VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    SECURE_SIGN_YN             VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    REQ_CHANNEL_CODE           VARCHAR2(1)    DEFAULT '3',
    SVC_CONF_1                 VARCHAR2(20),
    SVC_CONF_2                 VARCHAR2(20),
    BANK_STATUS_CHECK_YN       VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    BANK_CODE_FIELD            VARCHAR2(30),
    BIZDAY_SERVICE_YN          VARCHAR2(1),
    BIZDAY_SERVICE_START_TIME  VARCHAR2(4),
    BIZDAY_SERVICE_END_TIME    VARCHAR2(4),
    SATURDAY_SERVICE_YN        VARCHAR2(1),
    SATURDAY_SERVICE_START_TIME VARCHAR2(4),
    SATURDAY_SERVICE_END_TIME  VARCHAR2(4),
    HOLIDAY_SERVICE_YN         VARCHAR2(1),
    HOLIDAY_SERVICE_START_TIME VARCHAR2(4),
    HOLIDAY_SERVICE_END_TIME   VARCHAR2(4),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    CONSTRAINT PK_FWK_SERVICE PRIMARY KEY (SERVICE_ID)
);

CREATE TABLE FWK_SERVICE_RELATION (
    SERVICE_ID                 VARCHAR2(30)   NOT NULL,
    SERVICE_SEQ_NO             NUMBER(3,0)    DEFAULT 1 NOT NULL,
    COMPONENT_ID               VARCHAR2(40)   NOT NULL,
    POST_CONDITION             VARCHAR2(30),
    EXPRESSION                 VARCHAR2(100),
    RELATION_ETC1              VARCHAR2(500),
    RELATION_ETC2              VARCHAR2(500),
    RELATION_ETC3              VARCHAR2(500),
    RELATION_ETC4              VARCHAR2(500),
    LAST_UPDATE_DTIME          VARCHAR2(14),
    CONSTRAINT PK_FWK_SERVICE_RELATION PRIMARY KEY (SERVICE_ID, SERVICE_SEQ_NO)
);

CREATE TABLE FWK_COMPONENT (
    COMPONENT_ID               VARCHAR2(50)   NOT NULL,
    COMPONENT_NAME             VARCHAR2(100)  NOT NULL,
    COMPONENT_DESC             VARCHAR2(300),
    COMPONENT_TYPE             VARCHAR2(1)    NOT NULL,
    COMPONENT_CLASS_NAME       VARCHAR2(100)  NOT NULL,
    COMPONENT_METHOD_NAME      VARCHAR2(50)   NOT NULL,
    COMPONENT_CREATE_TYPE      VARCHAR2(1),
    BIZ_GROUP_ID               VARCHAR2(20),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    CONSTRAINT PK_FWK_COMPONENT PRIMARY KEY (COMPONENT_ID)
);

CREATE TABLE FWK_RELATION_PARAM (
    SERVICE_ID                 VARCHAR2(30)   NOT NULL,
    SERVICE_SEQ_NO             NUMBER(3,0)    DEFAULT 0 NOT NULL,
    COMPONENT_ID               VARCHAR2(40)   NOT NULL,
    PARAM_SEQ_NO               NUMBER(8,0)    NOT NULL,
    PARAM_VALUE                VARCHAR2(500),
    LAST_UPDATE_DTIME          VARCHAR2(14),
    CONSTRAINT PK_FWK_RELATION_PARAM PRIMARY KEY (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO)
);

CREATE TABLE FWK_CUST_MENU_APP (
    MENU_URL                   VARCHAR2(200)  NOT NULL,
    WEB_APP_ID                 VARCHAR2(100)  NOT NULL,
    MENU_NAME                  VARCHAR2(50),
    MENU_ID                    VARCHAR2(40)   NOT NULL,
    ASYNC_YN                   VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    START_TIME                 VARCHAR2(4)    DEFAULT '0000' NOT NULL,
    END_TIME                   VARCHAR2(4)    DEFAULT '2400' NOT NULL,
    TIME_CHECK_YN              VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    BIZ_DAY_CHECK_YN           VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    LOG_YN                     VARCHAR2(1)    DEFAULT 'N' NOT NULL,
    VALIDATION_MESSAGE_ID      VARCHAR2(50),
    LOGIN_ONLY_YN              VARCHAR2(1)    DEFAULT 'N',
    SECURE_SIGN_YN             VARCHAR2(1)    DEFAULT 'N',
    BANK_STATUS_CHECK_YN       VARCHAR2(1)    DEFAULT 'N',
    BANK_CODE_FIELD            VARCHAR2(30),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    ENCRIPTION_YN              VARCHAR2(1)    DEFAULT 'N',
    E_CHANNEL_CODE             VARCHAR2(1),
    BIZ_DOMAIN                 VARCHAR2(10),
    BIZ_GROUP_ID               VARCHAR2(20),
    APP_TYPE                   VARCHAR2(10),
    LAST_UPDATE_DTIME          VARCHAR2(14),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    STOP_REASON_KO             VARCHAR2(4000),
    STOP_REASON_EN             VARCHAR2(4000),
    URL_PATTERN                VARCHAR2(10),
    INPUT_TYPE                 VARCHAR2(1),
    WARNING_KO                 VARCHAR2(4000),
    WARNING_EN                 VARCHAR2(4000),
    CRM_LOG_TYPE1              VARCHAR2(2),
    CRM_LOG_TYPE2              VARCHAR2(2),
    CRM_LOG_TYPE3              VARCHAR2(2),
    HDAY_START_TIME            VARCHAR2(4)    DEFAULT '0000',
    HDAY_END_TIME              VARCHAR2(4)    DEFAULT '2400',
    ECRM_DESC                  VARCHAR2(400),
    VIEW_YN                    VARCHAR2(1)    DEFAULT 'Y',
    IN_OUT_USE_YN              VARCHAR2(1)    DEFAULT 'N',
    IN_OUT_MESSAGE_ID          VARCHAR2(50),
    WARNING_START_DTIME        VARCHAR2(14),
    WARNING_END_DTIME          VARCHAR2(14),
    STOP_REASON_START_DTIME    VARCHAR2(14),
    STOP_REASON_END_DTIME      VARCHAR2(14),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(50),
    PRIOR_MENU_ID              VARCHAR2(40),
    SORT_ORDER                 NUMBER,
    PAGE_YN                    VARCHAR2(1),
    TYPE_CHECK_YN              VARCHAR2(1),
    NOTIFY_NAME                VARCHAR2(500),
    ALTER_RESPONSE             CLOB,
    DISPLAY_YN                 VARCHAR2(1)    DEFAULT 'Y',
    CONSTRAINT PK_FWK_CUST_MENU_APP PRIMARY KEY (MENU_URL)
);

CREATE TABLE FWK_VALIDATION (
    VALIDATION_ID        VARCHAR2(20)   NOT NULL,
    VALIDATION_DESC      VARCHAR2(200),
    FIELD_EVENT_TEXT     VARCHAR2(4000),
    MASK_TEXT            VARCHAR2(200),
    CHAR_TYPE_TEXT       VARCHAR2(1000),
    MAX_VALUE_TEXT       VARCHAR2(200),
    MIN_VALUE_TEXT       VARCHAR2(200),
    SUBMIT_EVENT_TEXT    VARCHAR2(4000),
    JAVA_CLASS_NAME      VARCHAR2(100),
    LAST_UPDATE_DTIME    VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID  VARCHAR2(20),
    CONSTRAINT PK_FWK_VALIDATION PRIMARY KEY (VALIDATION_ID)
);

CREATE TABLE FWK_BIZ_APP (
    BIZ_APP_ID           VARCHAR2(100)  NOT NULL,
    BIZ_APP_NAME         VARCHAR2(100),
    BIZ_APP_DESC         VARCHAR2(300),
    DUP_CHECK_YN         VARCHAR2(1)    DEFAULT 'N',
    QUE_NAME             VARCHAR2(100),
    LOG_YN               VARCHAR2(1)    DEFAULT 'Y',
    LAST_UPDATE_DTIME    VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID  VARCHAR2(20),
    CONSTRAINT PK_FWK_BIZ_APP PRIMARY KEY (BIZ_APP_ID)
);

CREATE TABLE FWK_SQL_CONF (
    DB_ID                  VARCHAR2(50)   NOT NULL,
    DB_NAME                VARCHAR2(100),
    DB_DESC                VARCHAR2(300),
    JNDI_PROVIDER_URL      VARCHAR2(200),
    JNDI_CONTEXT_FACTORY   VARCHAR2(200),
    CONNECTION_URL         VARCHAR2(500),
    DRIVER_CLASS           VARCHAR2(200),
    DB_USER_ID             VARCHAR2(100),
    DB_PASSWORD            VARCHAR2(200),
    JNDI_YN                VARCHAR2(1)    DEFAULT 'N',
    JNDI_ID                VARCHAR2(100),
    LAST_UPDATE_DTIME      VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID    VARCHAR2(20),
    CONSTRAINT PK_FWK_SQL_CONF PRIMARY KEY (DB_ID)
);

CREATE TABLE FWK_SQL_GROUP (
    SQL_GROUP_ID               VARCHAR2(50)   NOT NULL,
    SQL_GROUP_NAME             VARCHAR2(100),
    INSTANCE_TYPE              VARCHAR2(10),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    CONSTRAINT PK_FWK_SQL_GROUP PRIMARY KEY (SQL_GROUP_ID)
);

CREATE TABLE FWK_SQL_QUERY (
    QUERY_ID                   VARCHAR2(50)   NOT NULL,
    QUERY_NAME                 VARCHAR2(200),
    SQL_GROUP_ID               VARCHAR2(50),
    DB_ID                      VARCHAR2(50),
    SQL_TYPE                   VARCHAR2(10),
    EXEC_TYPE                  VARCHAR2(10),
    CACHE_YN                   VARCHAR2(1)    DEFAULT 'N',
    TIME_OUT                   VARCHAR2(10),
    RESULT_TYPE                VARCHAR2(10),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y',
    SQL_QUERY                  CLOB,
    SQL_QUERY2                 CLOB,
    QUERY_DESC                 VARCHAR2(500),
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID        VARCHAR2(20),
    CONSTRAINT PK_FWK_SQL_QUERY PRIMARY KEY (QUERY_ID)
);

CREATE TABLE FWK_COMPONENT_PARAM (
    COMPONENT_ID          VARCHAR2(50)   NOT NULL,
    PARAM_SEQ_NO          NUMBER(8)      NOT NULL,
    PARAM_KEY             VARCHAR2(100)  NOT NULL,
    PARAM_DESC            VARCHAR2(300),
    DEFAULT_PARAM_VALUE   VARCHAR2(500),
    CONSTRAINT PK_FWK_COMPONENT_PARAM PRIMARY KEY (COMPONENT_ID, PARAM_SEQ_NO),
    CONSTRAINT FK_COMPONENT_PARAM_COMP FOREIGN KEY (COMPONENT_ID) REFERENCES FWK_COMPONENT (COMPONENT_ID)
);

CREATE TABLE FWK_SQL (
    SQL_GROUP_ID         VARCHAR2(50)   NOT NULL,
    SQL_ID               VARCHAR2(50)   NOT NULL,
    SQL_NAME             VARCHAR2(100),
    SQL_DESC             VARCHAR2(300),
    SQL_TEXT             CLOB,
    CRUD_TYPE            VARCHAR2(1),
    INPUT_TYPE           VARCHAR2(1),
    DB_ID                VARCHAR2(50),
    BIZ_GROUP_ID         VARCHAR2(20),
    TABLE_NAME           VARCHAR2(100),
    SQL_FILE_NAME        VARCHAR2(200),
    SQL_FILE_PATH        VARCHAR2(500),
    USE_YN               VARCHAR2(1)    DEFAULT 'Y',
    LAST_UPDATE_DTIME    VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID  VARCHAR2(20),
    CONSTRAINT PK_FWK_SQL PRIMARY KEY (SQL_GROUP_ID, SQL_ID)
);

CREATE TABLE FWK_SQL_INFO (
    SQL_GROUP_ID         VARCHAR2(50)   NOT NULL,
    SQL_ID               VARCHAR2(50)   NOT NULL,
    SQL_INFO_SEQ         NUMBER(5)      NOT NULL,
    INFO_TYPE            VARCHAR2(1),
    TABLE_NAME           VARCHAR2(100),
    COLUMN_NAME          VARCHAR2(100),
    OPERATOR_ID          VARCHAR2(20),
    PK_YN                VARCHAR2(1)    DEFAULT 'N',
    DEFAULT_WHEN_NULL    VARCHAR2(500),
    PREPEND_CONDITION    VARCHAR2(100),
    APPEND_CONDITION     VARCHAR2(100),
    PARAMETER_KEY        VARCHAR2(100),
    SQL_TEXT             VARCHAR2(4000),
    COLUMN_DESC          VARCHAR2(200),
    COLUMN_TYPE          VARCHAR2(20),
    LAST_UPDATE_DTIME    VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    LAST_UPDATE_USER_ID  VARCHAR2(20),
    CONSTRAINT PK_FWK_SQL_INFO PRIMARY KEY (SQL_GROUP_ID, SQL_ID, SQL_INFO_SEQ),
    CONSTRAINT FK_SQL_INFO_SQL FOREIGN KEY (SQL_GROUP_ID, SQL_ID) REFERENCES FWK_SQL (SQL_GROUP_ID, SQL_ID)
);


-- =============================================================
-- 13. 이행 데이터 (2 tables)
-- =============================================================

CREATE TABLE FWK_TRANS_DATA_TIMES (
    TRAN_SEQ                   VARCHAR2(14)   NOT NULL,
    TRAN_REASON                VARCHAR2(100),
    TRAN_RESULT                VARCHAR2(1)    NOT NULL,
    TRAN_TIME                  VARCHAR2(14)   NOT NULL,
    USER_ID                    VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_TRANS_DATA_TIMES PRIMARY KEY (TRAN_SEQ)
);

CREATE TABLE FWK_TRANS_DATA_HIS (
    TRAN_SEQ                   VARCHAR2(14)   NOT NULL,
    TRAN_ID                    VARCHAR2(50)   NOT NULL,
    TRAN_TYPE                  VARCHAR2(10)   NOT NULL,
    TRAN_NAME                  VARCHAR2(100),
    TRAN_RESULT                VARCHAR2(1)    NOT NULL,
    TRAN_FAIL_REASON           VARCHAR2(2000),
    TRAN_FAIL_SQL              VARCHAR2(2000),
    TRAN_TIME                  VARCHAR2(14)   NOT NULL,
    CONSTRAINT PK_FWK_TRANS_DATA_HIS PRIMARY KEY (TRAN_SEQ, TRAN_ID, TRAN_TYPE)
);


-- =============================================================
-- 14. 시스템 로그 (1 table)
-- =============================================================

CREATE TABLE FWK_LOG (
    LOG_SNO                    NUMBER(20,0)   NOT NULL,
    LOG_DTIME                  VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LOG_TRACE_NO               VARCHAR2(50),
    USER_ID                    VARCHAR2(20),
    CHANNEL_ID                 VARCHAR2(30),
    IO_TYPE                    VARCHAR2(1),
    REQ_RES_TYPE               VARCHAR2(1),
    RESULT_CODE                VARCHAR2(40),
    RESULT_MESSAGE             VARCHAR2(1000),
    LOG_DATA                   CLOB,
    CONSTRAINT PK_FWK_LOG PRIMARY KEY (LOG_SNO)
);


-- =============================================================
-- 15. 코드 템플릿 (1 table)
-- =============================================================

CREATE TABLE FWK_CODE_TEMPLATE (
    TEMPLATE_ID                VARCHAR2(50)   NOT NULL,
    TEMPLATE_NAME              VARCHAR2(100)  NOT NULL,
    TEMPLATE_TYPE              VARCHAR2(20)   NOT NULL,       -- JAVA, XML 등
    TEMPLATE_BODY              CLOB           NOT NULL,       -- Freemarker 템플릿 내용
    DESCRIPTION                VARCHAR2(500),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y'    NOT NULL,
    SORT_ORDER                 NUMBER(5,0)    DEFAULT 0,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_CODE_TEMPLATE PRIMARY KEY (TEMPLATE_ID),
    CONSTRAINT CHK_CODE_TEMPLATE_USE_YN CHECK (USE_YN IN ('Y', 'N'))
);


-- =============================================================
-- 16. Spider React Platform (2 table) — reactPlatform 프로젝트에서 사용
-- =============================================================

CREATE TABLE FWK_REACT_CODE_HIS (

    -- PK
    CODE_ID          VARCHAR2(36)   NOT NULL,

    -- Figma 원본 데이터
    FIGMA_URL        VARCHAR2(1000) NOT NULL,
    FIGMA_JSON       CLOB,

    -- 사용자 입력 (구조화)
    DOMAIN           VARCHAR2(100)  NOT NULL,
    BRAND            VARCHAR2(100)  NOT NULL,
    COMPONENT_NAME   VARCHAR2(200)  NOT NULL,
    TITLE            VARCHAR2(200),
    CATEGORY         VARCHAR2(30),
    DESCRIPTION      VARCHAR2(2000),

    -- 사용자 입력 (자유 텍스트)
    -- 최초 생성: Claude에게 전달하는 추가 요구사항
    -- 재생성:    REF_CODE_ID 가 존재하며, 변경 요청사항을 담음
    REQUIREMENTS     CLOB,

    -- AI 프롬프트 (감사·디버깅용)
    SYSTEM_PROMPT    CLOB,
    USER_PROMPT      CLOB,

    -- AI 생성 결과
    REACT_CODE       CLOB,
    FAIL_REASON      CLOB,
    STATUS           VARCHAR2(20)   DEFAULT 'GENERATED' NOT NULL,

    -- 재생성 체인
    -- REF_CODE_ID:  직계 부모 CODE_ID. NULL이면 최초 생성 레코드
    -- ROOT_CODE_ID: 체인 최상위 CODE_ID. NULL이면 최초 생성 레코드
    --               WHERE ROOT_CODE_ID = :id 로 체인 전체 조회 가능 (재귀 쿼리 불필요)
    REF_CODE_ID      VARCHAR2(36),
    ROOT_CODE_ID     VARCHAR2(36),

    -- 감사 정보
    CREATE_DTIME     VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    CREATE_USER_ID   VARCHAR2(20)   NOT NULL,
    APPROVAL_DTIME   VARCHAR2(14),
    APPROVAL_USER_ID VARCHAR2(20),

    CONSTRAINT PK_REACT_CODE_HIS    PRIMARY KEY (CODE_ID),
    CONSTRAINT CHK_REACT_GEN_STATUS CHECK (STATUS IN (
        'GENERATED', 'FAILED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'
    )),
    CONSTRAINT CHK_REACT_CATEGORY   CHECK (CATEGORY IN (
        'AUTH', 'MAIN', 'LIST', 'DETAIL', 'FORM',
        'MYPAGE', 'ADMIN', 'EVENT', 'ERROR'
    )),
    CONSTRAINT FK_REACT_REF         FOREIGN KEY (REF_CODE_ID)
        REFERENCES FWK_REACT_CODE_HIS(CODE_ID)
);

COMMENT ON TABLE  FWK_REACT_CODE_HIS                    IS 'Spider React Platform — React 코드 생성 이력';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.CODE_ID            IS '생성 이력 ID (UUID)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.FIGMA_URL          IS 'Figma 디자인 URL';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.FIGMA_JSON         IS 'Figma API 응답 JSON 원본';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.DOMAIN             IS '서비스 도메인 (예: 금융, 커머스)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.BRAND              IS '브랜드명';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.COMPONENT_NAME     IS '컴포넌트 기술 식별자 (예: LoginForm)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.TITLE              IS '화면 제목 (사람이 읽는 이름, 예: 로그인 폼)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.CATEGORY           IS '화면 목적 분류: AUTH/MAIN/LIST/DETAIL/FORM/MYPAGE/ADMIN/EVENT/ERROR';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.DESCRIPTION        IS '화면 설명';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.REQUIREMENTS       IS '사용자 추가 입력. 최초 생성 시 추가 요구사항, 재생성 시 변경 요청사항 (REF_CODE_ID 유무로 구분)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.SYSTEM_PROMPT      IS 'Claude에게 전달한 시스템 프롬프트 (감사·디버깅용)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.USER_PROMPT        IS 'Claude에게 전달한 유저 프롬프트 (감사·디버깅용)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.REACT_CODE         IS 'Claude가 생성한 React 코드';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.FAIL_REASON        IS '생성 실패 사유 (STATUS=FAILED 일 때)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.STATUS             IS '생성 상태: GENERATED/FAILED/PENDING_APPROVAL/APPROVED/REJECTED';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.REF_CODE_ID        IS '재생성 직계 부모 CODE_ID. NULL이면 최초 생성';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.ROOT_CODE_ID       IS '재생성 체인 최상위 CODE_ID. NULL이면 최초 생성. WHERE ROOT_CODE_ID=:id 로 체인 전체 조회';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.CREATE_DTIME       IS '생성 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.CREATE_USER_ID     IS '생성자 ID';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.APPROVAL_DTIME     IS '결재 일시 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_REACT_CODE_HIS.APPROVAL_USER_ID   IS '결재자 ID';

-- 카테고리 필터 검색용
CREATE INDEX IDX_REACT_CATEGORY ON FWK_REACT_CODE_HIS(CATEGORY);
-- 도메인·브랜드 검색용
CREATE INDEX IDX_REACT_DOMAIN   ON FWK_REACT_CODE_HIS(DOMAIN);
CREATE INDEX IDX_REACT_BRAND    ON FWK_REACT_CODE_HIS(BRAND);
-- 재생성 체인 전체 조회용 (WHERE ROOT_CODE_ID = :id)
CREATE INDEX IDX_REACT_CHAIN    ON FWK_REACT_CODE_HIS(ROOT_CODE_ID);

CREATE TABLE FWK_REACT_DEPLOY_HIS (
    DEPLOY_ID              VARCHAR2(36)   NOT NULL,
    CODE_ID                VARCHAR2(36)   NOT NULL,
    DEPLOY_MODE            VARCHAR2(10)   NOT NULL,
    DEPLOY_STATUS          VARCHAR2(10)   NOT NULL,
    FAIL_REASON            CLOB,
    PR_URL                 VARCHAR2(500),
    LAST_UPDATE_DTIME      VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID    VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_REACT_DEPLOY_HIS PRIMARY KEY (DEPLOY_ID),
    CONSTRAINT FK_REACT_DEPLOY_CODE FOREIGN KEY (CODE_ID)
        REFERENCES FWK_REACT_CODE_HIS (CODE_ID),
    CONSTRAINT CHK_REACT_DEPLOY_STATUS
        CHECK (DEPLOY_STATUS IN ('SUCCESS', 'FAILED')),
    CONSTRAINT CHK_REACT_DEPLOY_MODE
        CHECK (DEPLOY_MODE IN ('local', 'git-pr'))
);

-- CODE_ID 기반 배포 이력 조회 성능
CREATE INDEX IDX_REACT_DEPLOY_CODE ON FWK_REACT_DEPLOY_HIS (CODE_ID);
-- 최근 배포 현황 조회 성능
CREATE INDEX IDX_REACT_DEPLOY_DTIME ON FWK_REACT_DEPLOY_HIS (LAST_UPDATE_DTIME DESC);

-- =============================================================
-- 17. FWK_CODE_TEMPLATE (1 table)
-- =============================================================

CREATE TABLE D_SPIDERLINK.FWK_CODE_TEMPLATE (
    TEMPLATE_ID                VARCHAR2(50)   NOT NULL,
    TEMPLATE_NAME              VARCHAR2(100)  NOT NULL,
    TEMPLATE_TYPE              VARCHAR2(20)   NOT NULL,
    TEMPLATE_BODY              CLOB           NOT NULL,
    DESCRIPTION                VARCHAR2(500),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y'    NOT NULL,
    SORT_ORDER                 NUMBER(5,0)    DEFAULT 0,
    LAST_UPDATE_DTIME          VARCHAR2(14)   DEFAULT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_FWK_CODE_TEMPLATE PRIMARY KEY (TEMPLATE_ID),
    CONSTRAINT CHK_CODE_TEMPLATE_USE_YN CHECK (USE_YN IN ('Y', 'N'))
);

-- =============================================================
-- 18. CMS 페이지 관리 (3 tables)
-- =============================================================

CREATE TABLE SPW_CMS_PAGE (
    PAGE_ID                    VARCHAR2(100)  NOT NULL,
    PAGE_NAME                  VARCHAR2(200),
    VIEW_MODE                  VARCHAR2(20),
    PAGE_HTML                  CLOB,
    FILE_PATH                  VARCHAR2(500),
    FILE_PATH_BACK             VARCHAR2(500),
    OWNER_DEPT_CODE            VARCHAR2(50),
    CREATE_USER_ID             VARCHAR2(20),
    CREATE_USER_NAME           VARCHAR2(100),
    CREATE_DATE                DATE,
    LAST_MODIFIER_ID           VARCHAR2(20),
    LAST_MODIFIER_NAME         VARCHAR2(100),
    LAST_MODIFIED_DTIME        TIMESTAMP(6),
    APPROVER_ID                VARCHAR2(20),
    APPROVER_NAME              VARCHAR2(100),
    APPROVE_STATE              VARCHAR2(20)   DEFAULT 'WORK' NOT NULL,
    APPROVE_DATE               TIMESTAMP(6),
    REJECTED_REASON            VARCHAR2(2000),
    IS_PUBLIC                  VARCHAR2(1)    DEFAULT 'N',
    BEGINNING_DATE             DATE,
    EXPIRED_DATE               DATE,
    CONFIRM_DTIME              TIMESTAMP(6),
    PAGE_DESC                  VARCHAR2(500),
    PAGE_DESC_DETAIL           VARCHAR2(2000),
    TEMPLATE_ID                VARCHAR2(100),
    THUMBNAIL                  VARCHAR2(500),
    USER_GUIDE                 VARCHAR2(500),
    USE_YN                     VARCHAR2(1)    DEFAULT 'Y' NOT NULL,
    FILE_CRC_VALUE             VARCHAR2(64),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(100),
    AB_GROUP_ID                VARCHAR2(64),
    AB_WEIGHT                  NUMBER(5, 2),
    CONSTRAINT PK_SPW_CMS_PAGE PRIMARY KEY (PAGE_ID)
);

CREATE TABLE SPW_CMS_PAGE_HISTORY (
    PAGE_ID                    VARCHAR2(100)  NOT NULL,
    VERSION                    NUMBER(10)     NOT NULL,
    PAGE_NAME                  VARCHAR2(200),
    VIEW_MODE                  VARCHAR2(20),
    PAGE_HTML                  CLOB,
    FILE_PATH                  VARCHAR2(500),
    FILE_PATH_BACK             VARCHAR2(500),
    OWNER_DEPT_CODE            VARCHAR2(50),
    CREATE_USER_ID             VARCHAR2(20),
    CREATE_USER_NAME           VARCHAR2(100),
    CREATE_DATE                DATE,
    LAST_MODIFIER_ID           VARCHAR2(20),
    LAST_MODIFIER_NAME         VARCHAR2(100),
    LAST_MODIFIED_DTIME        TIMESTAMP(6),
    APPROVER_ID                VARCHAR2(20),
    APPROVER_NAME              VARCHAR2(100),
    APPROVE_STATE              VARCHAR2(20),
    APPROVE_DATE               TIMESTAMP(6),
    REJECTED_REASON            VARCHAR2(2000),
    IS_PUBLIC                  VARCHAR2(1),
    BEGINNING_DATE             DATE,
    EXPIRED_DATE               DATE,
    CONFIRM_DTIME              TIMESTAMP(6),
    PAGE_DESC                  VARCHAR2(500),
    PAGE_DESC_DETAIL           VARCHAR2(2000),
    TEMPLATE_ID                VARCHAR2(100),
    THUMBNAIL                  VARCHAR2(500),
    USER_GUIDE                 VARCHAR2(500),
    USE_YN                     VARCHAR2(1),
    FILE_CRC_VALUE             VARCHAR2(64),
    FINAL_APPROVAL_STATE       VARCHAR2(1),
    FINAL_APPROVAL_DTIME       VARCHAR2(14),
    FINAL_APPROVAL_USER_ID     VARCHAR2(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR2(100),
    CONSTRAINT PK_SPW_CMS_PAGE_HISTORY PRIMARY KEY (PAGE_ID, VERSION)
);

CREATE SEQUENCE SEQ_SPW_PAGE_VIEW_LOG START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE SPW_CMS_PAGE_VIEW_LOG (
    LOG_ID          NUMBER           NOT NULL,
    PAGE_ID         VARCHAR2(100)    NOT NULL,
    COMPONENT_ID    VARCHAR2(100),
    EVENT_TYPE      VARCHAR2(10)     NOT NULL,
    VIEW_DTIME      TIMESTAMP(6)     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_SPW_CMS_PAGE_VIEW_LOG PRIMARY KEY (LOG_ID),
    CONSTRAINT CHK_SPW_PAGE_VIEW_LOG_TYPE CHECK (EVENT_TYPE IN ('VIEW', 'CLICK'))
);

CREATE OR REPLACE TRIGGER TRG_SPW_PAGE_VIEW_LOG
    BEFORE INSERT ON SPW_CMS_PAGE_VIEW_LOG
    FOR EACH ROW
BEGIN
    IF :NEW.LOG_ID IS NULL THEN
        :NEW.LOG_ID := SEQ_SPW_PAGE_VIEW_LOG.NEXTVAL;
    END IF;
END;
/

CREATE INDEX IDX_SPW_PAGE_VIEW_LOG_PAGE ON SPW_CMS_PAGE_VIEW_LOG(PAGE_ID, EVENT_TYPE);
CREATE INDEX IDX_SPW_PAGE_VIEW_LOG_DTIME ON SPW_CMS_PAGE_VIEW_LOG(VIEW_DTIME);


-- =============================================================
-- bizApp — spider-link 전문 거래 이력 (FWK_MESSAGE_INSTANCE)
-- 추가일: 2026-04-23
-- 대상: D_SPIDERLINK 스키마
-- 주의: 개발자가 DB에서 직접 실행해야 한다
-- =============================================================

CREATE SEQUENCE FWK_MESSAGE_INSTANCE_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

COMMENT ON TABLE  FWK_MESSAGE_INSTANCE                IS 'spider-link 전문 거래 이력';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.MESSAGE_SNO    IS '일련번호 (FWK_MESSAGE_INSTANCE_SEQ)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.TRX_ID         IS '거래 ID (UUID, 요청·응답 쌍 식별)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.ORG_ID         IS '기관 ID (spring.application.name)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.IO_TYPE        IS 'I=인바운드 수신, O=아웃바운드 송신';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.REQ_RES_TYPE   IS 'REQ=요청, RES=응답';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.MESSAGE_ID     IS '커맨드명 (AUTH_LOGIN 등)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.TRX_TRACKING_NO IS '요청 추적 ID (JsonCommandRequest.requestId)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.LOG_DTIME      IS '기록 시각 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.LAST_LOG_DTIME IS '마지막 기록 시각';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.LAST_RT_CODE   IS 'SUCCESS / FAIL';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.INSTANCE_ID    IS '모듈명:포트 (예: biz-auth:19100)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.RETRY_TRX_YN   IS '재처리 여부 (항상 N)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.MESSAGE_DATA   IS '전문 데이터 (JSON)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.TRX_DTIME      IS '거래 발생 시각 (YYYYMMDDHH24MISS)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.CHANNEL_TYPE   IS '채널 구분 (TCP)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.URI            IS '커맨드명 (MESSAGE_ID와 동일)';
COMMENT ON COLUMN FWK_MESSAGE_INSTANCE.SUCCESS_YN     IS 'Y=성공, N=실패';

COMMIT;
