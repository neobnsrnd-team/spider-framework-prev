-- =============================================================
-- CI 전용 H2 인메모리 스키마 (Oracle 호환 모드)
-- Oracle DDL(docs/sql/oracle/01_create_tables.sql) 기반 H2 변환
-- 총 테이블: 60개
-- =============================================================
-- 타입 변환: VARCHAR2→VARCHAR, NUMBER→NUMERIC, RAW→VARBINARY
-- DEFAULT TO_CHAR(SYSDATE,...) 제거 (애플리케이션에서 AuditUtil로 처리)
-- =============================================================


-- =============================================================
-- DROP (역순)
-- =============================================================
DROP TABLE IF EXISTS FWK_TRANS_DATA_HIS;
DROP TABLE IF EXISTS FWK_TRANS_DATA_TIMES;
DROP TABLE IF EXISTS FWK_CUST_MENU_APP;
DROP TABLE IF EXISTS FWK_RELATION_PARAM;
DROP TABLE IF EXISTS FWK_COMPONENT;
DROP TABLE IF EXISTS FWK_SERVICE_RELATION;
DROP TABLE IF EXISTS FWK_SERVICE;
DROP TABLE IF EXISTS FWK_VALIDATOR;
DROP TABLE IF EXISTS FWK_MONITOR;
DROP TABLE IF EXISTS FWK_PROPERTY_HISTORY;
DROP TABLE IF EXISTS FWK_PROPERTY;
DROP TABLE IF EXISTS FWK_ARTICLE_USER;
DROP TABLE IF EXISTS FWK_ARTICLE;
DROP TABLE IF EXISTS FWK_BOARD_CATEGORY;
DROP TABLE IF EXISTS FWK_BOARD_AUTH;
DROP TABLE IF EXISTS FWK_BOARD;
DROP TABLE IF EXISTS FWK_ERROR_HANDLE_HIS;
DROP TABLE IF EXISTS FWK_ERROR_HANDLE_APP;
DROP TABLE IF EXISTS FWK_HANDLE_APP;
DROP TABLE IF EXISTS FWK_ERROR_HIS;
DROP TABLE IF EXISTS FWK_ERROR_DESC;
DROP TABLE IF EXISTS FWK_ERROR;
DROP TABLE IF EXISTS FWK_BATCH_HIS;
DROP TABLE IF EXISTS FWK_BATCH_APP;
DROP TABLE IF EXISTS FWK_WAS_EXEC_BATCH;
DROP TABLE IF EXISTS FWK_WAS_PROPERTY_HISTORY;
DROP TABLE IF EXISTS FWK_WAS_PROPERTY;
DROP TABLE IF EXISTS FWK_WAS_LISTENER;
DROP TABLE IF EXISTS FWK_WAS_GROUP_INSTANCE;
DROP TABLE IF EXISTS FWK_WAS_INSTANCE;
DROP TABLE IF EXISTS FWK_WAS_GROUP;
DROP TABLE IF EXISTS FWK_TRANSPORT;
DROP TABLE IF EXISTS FWK_LISTENER_CONNECTOR_MAPPING;
DROP TABLE IF EXISTS FWK_LISTENER_TRX_MESSAGE;
DROP TABLE IF EXISTS FWK_MESSAGE_HANDLER;
DROP TABLE IF EXISTS FWK_SYSTEM;
DROP TABLE IF EXISTS FWK_GATEWAY;
DROP TABLE IF EXISTS FWK_MESSAGE_INSTANCE;
DROP TABLE IF EXISTS FWK_MESSAGE_TEST;
DROP TABLE IF EXISTS FWK_MESSAGE_FIELD_MAPPING;
DROP TABLE IF EXISTS FWK_MESSAGE_FIELD_HISTORY;
DROP TABLE IF EXISTS FWK_MESSAGE_FIELD;
DROP TABLE IF EXISTS FWK_MESSAGE;
DROP TABLE IF EXISTS FWK_TRX_STOP_HISTORY;
DROP TABLE IF EXISTS FWK_TRX_MESSAGE_HISTORY;
DROP TABLE IF EXISTS FWK_TRX_MESSAGE;
DROP TABLE IF EXISTS FWK_TRX_HISTORY;
DROP TABLE IF EXISTS FWK_TRX;
DROP TABLE IF EXISTS FWK_BIZ_GROUP;
DROP TABLE IF EXISTS FWK_ORG_CODE;
DROP TABLE IF EXISTS FWK_ORG;
DROP TABLE IF EXISTS FWK_CODE;
DROP TABLE IF EXISTS FWK_CODE_GROUP;
DROP TABLE IF EXISTS FWK_ACCESS_USER;
DROP TABLE IF EXISTS FWK_USER_ACCESS_HIS;
DROP TABLE IF EXISTS FWK_ROLE_MENU;
DROP TABLE IF EXISTS FWK_USER_MENU;
DROP TABLE IF EXISTS FWK_MENU;
DROP TABLE IF EXISTS FWK_USER;
DROP TABLE IF EXISTS FWK_ROLE;


-- =============================================================
-- 1. 사용자/인증 (5 tables)
-- =============================================================

CREATE TABLE FWK_ROLE (
    ROLE_ID                    VARCHAR(10)    NOT NULL,
    ROLE_NAME                  VARCHAR(50)    NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    ROLE_DESC                  VARCHAR(200),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    RANKING                    VARCHAR(100),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_ROLE PRIMARY KEY (ROLE_ID)
);

CREATE TABLE FWK_USER (
    USER_ID                    VARCHAR(20)    NOT NULL,
    USER_NAME                  VARCHAR(50)    NOT NULL,
    PASSWORD                   VARCHAR(50),
    ROLE_ID                    VARCHAR(10),
    POSITION_NAME              VARCHAR(100),
    ADDRESS                    VARCHAR(200),
    CLASS_NAME                 VARCHAR(100),
    EMAIL                      VARCHAR(100),
    USER_STATE_CODE            VARCHAR(1),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    ACCESS_IP                  VARCHAR(15)    DEFAULT '*',
    USER_SSN                   VARCHAR(13),
    PHONE                      VARCHAR(13),
    REG_REQ_USER_NAME          VARCHAR(10),
    TITLE_NAME                 VARCHAR(10),
    EMP_NO                     VARCHAR(10),
    BRANCH_NO                  VARCHAR(10),
    BIZ_AUTH_CODE              VARCHAR(100),
    LOGIN_FAIL_COUNT           NUMERIC(1,0)   DEFAULT 0,
    LAST_PWD_UPDATE_DTIME      VARCHAR(14),
    DEFAULT_PROJECT_ID         VARCHAR(20),
    PA_USER                    VARCHAR(128),
    PRE_PA_USER                VARCHAR(128),
    PA_USER_SALT               VARCHAR(50),
    PRE_PA_USER_SALT           VARCHAR(50),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    PASSWD                     VARCHAR(100),
    CONSTRAINT PK_FWK_USER PRIMARY KEY (USER_ID)
);

CREATE TABLE FWK_MENU (
    MENU_ID                    VARCHAR(40)    NOT NULL,
    PRIOR_MENU_ID              VARCHAR(40)    NOT NULL,
    SORT_ORDER                 NUMERIC(3,0)   DEFAULT 0 NOT NULL,
    MENU_NAME                  VARCHAR(100)   NOT NULL,
    MENU_URL                   VARCHAR(200)   NOT NULL,
    MENU_IMAGE                 VARCHAR(50),
    DISPLAY_YN                 VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20),
    WEB_APP_ID                 VARCHAR(70),
    MENU_ENG_NAME              VARCHAR(200),
    CONSTRAINT PK_FWK_MENU PRIMARY KEY (MENU_ID)
);

CREATE TABLE FWK_USER_MENU (
    USER_ID                    VARCHAR(20)    NOT NULL,
    MENU_ID                    VARCHAR(40)    NOT NULL,
    AUTH_CODE                  VARCHAR(1)     NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    FAVOR_MENU_ORDER           NUMERIC(3,0)   DEFAULT 0 NOT NULL,
    CONSTRAINT PK_FWK_USER_MENU PRIMARY KEY (USER_ID, MENU_ID)
);

CREATE TABLE FWK_ROLE_MENU (
    ROLE_ID                    VARCHAR(10)    NOT NULL,
    MENU_ID                    VARCHAR(40)    NOT NULL,
    AUTH_CODE                  VARCHAR(1)     NOT NULL,
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_ROLE_MENU PRIMARY KEY (ROLE_ID, MENU_ID)
);


-- =============================================================
-- 2. 접근 기록 (2 tables)
-- =============================================================

CREATE TABLE FWK_USER_ACCESS_HIS (
    USER_ID                    VARCHAR(20)    NOT NULL,
    ACCESS_DTIME               VARCHAR(14)    NOT NULL,
    ACCESS_IP                  VARCHAR(200),
    ACCESS_URL                 VARCHAR(1000),
    INPUT_DATA                 VARCHAR(4000),
    RESULT_MESSAGE             VARCHAR(200)
);

CREATE TABLE FWK_ACCESS_USER (
    TRX_ID                     VARCHAR(50)    NOT NULL,
    CUST_USER_ID               VARCHAR(50)    NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    GUBUN_TYPE                 VARCHAR(1)     NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_ACCESS_USER PRIMARY KEY (GUBUN_TYPE, TRX_ID, CUST_USER_ID)
);


-- =============================================================
-- 3. 코드/조직 (5 tables)
-- =============================================================

CREATE TABLE FWK_CODE_GROUP (
    CODE_GROUP_ID              VARCHAR(8)     NOT NULL,
    CODE_GROUP_NAME            VARCHAR(100)   NOT NULL,
    CODE_GROUP_DESC            VARCHAR(200),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    BIZ_GROUP_ID               VARCHAR(20),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_CODE_GROUP PRIMARY KEY (CODE_GROUP_ID)
);

CREATE TABLE FWK_CODE (
    CODE_GROUP_ID              VARCHAR(8)     NOT NULL,
    CODE                       VARCHAR(50)    NOT NULL,
    CODE_NAME                  VARCHAR(100)   NOT NULL,
    CODE_DESC                  VARCHAR(200),
    SORT_ORDER                 NUMERIC(4,0)   DEFAULT 0 NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    CODE_ENGNAME               VARCHAR(300),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_CODE PRIMARY KEY (CODE_GROUP_ID, CODE)
);

CREATE TABLE FWK_ORG (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    ORG_NAME                   VARCHAR(50)    NOT NULL,
    ORG_DESC                   VARCHAR(200),
    START_TIME                 VARCHAR(6)     NOT NULL,
    END_TIME                   VARCHAR(6)     NOT NULL,
    XML_ROOT_TAG               VARCHAR(20),
    CONSTRAINT PK_FWK_ORG PRIMARY KEY (ORG_ID)
);

CREATE TABLE FWK_ORG_CODE (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    CODE_GROUP_ID              VARCHAR(8)     NOT NULL,
    CODE                       VARCHAR(30)    NOT NULL,
    ORG_CODE                   VARCHAR(30)    NOT NULL,
    PRIORITY                   VARCHAR(3)     DEFAULT '1',
    CONSTRAINT PK_FWK_ORG_CODE PRIMARY KEY (ORG_ID, CODE_GROUP_ID, CODE, ORG_CODE)
);

CREATE TABLE FWK_BIZ_GROUP (
    BIZ_GROUP_ID               VARCHAR(20)    NOT NULL,
    BIZ_GROUP_NAME             VARCHAR(50),
    BIZ_GROUP_DESC             VARCHAR(200),
    BIZ_L_GROUP_ID             VARCHAR(20)    NOT NULL,
    BIZ_L_GROUP_NAME           VARCHAR(50),
    BIZ_SUB_GROUP_ID           VARCHAR(20),
    BIZ_SUB_GROUP_NAME         VARCHAR(50),
    DEFAULT_WORK_SPACE_ID      VARCHAR(20),
    "DEPTH"                    VARCHAR(1),
    BIZ_DOMAIN                 VARCHAR(10),
    KOR_PATH_TEXT              VARCHAR(100),
    ENG_PKG_TEXT               VARCHAR(50),
    CONSTRAINT PK_FWK_BIZ_GROUP PRIMARY KEY (BIZ_GROUP_ID)
);


-- =============================================================
-- 4. 거래/전문 (9 tables)
-- =============================================================

CREATE TABLE FWK_TRX (
    TRX_ID                     VARCHAR(40)    NOT NULL,
    OPER_MODE_TYPE             VARCHAR(1),
    TRX_STOP_YN                VARCHAR(1)     DEFAULT 'Y',
    TRX_NAME                   VARCHAR(50),
    TRX_DESC                   VARCHAR(200),
    TRX_TYPE                   VARCHAR(1)     DEFAULT '1' NOT NULL,
    RETRY_TRX_YN               VARCHAR(1)     DEFAULT 'N' NOT NULL,
    MAX_RETRY_COUNT            NUMERIC(3,0)   DEFAULT 0 NOT NULL,
    RETRY_MI_CYCLE             VARCHAR(4),
    BIZ_GROUP_ID               VARCHAR(10),
    BIZDAY_TRX_YN              VARCHAR(1),
    BIZDAY_TRX_START_TIME      VARCHAR(4),
    BIZDAY_TRX_END_TIME        VARCHAR(4),
    SATURDAY_TRX_YN            VARCHAR(1),
    SATURDAY_TRX_START_TIME    VARCHAR(4),
    SATURDAY_TRX_END_TIME      VARCHAR(4),
    HOLIDAY_TRX_YN             VARCHAR(1),
    HOLIDAY_TRX_START_TIME     VARCHAR(4),
    HOLIDAY_TRX_END_TIME       VARCHAR(4),
    TRX_STOP_REASON            VARCHAR(200),
    CONSTRAINT PK_FWK_TRX PRIMARY KEY (TRX_ID)
);

CREATE TABLE FWK_TRX_HISTORY (
    TRX_ID                     VARCHAR(40)    NOT NULL,
    OPER_MODE_TYPE             VARCHAR(1),
    TRX_STOP_YN                VARCHAR(1)     DEFAULT 'Y',
    TRX_NAME                   VARCHAR(50),
    TRX_DESC                   VARCHAR(200),
    TRX_TYPE                   VARCHAR(1)     DEFAULT '1' NOT NULL,
    RETRY_TRX_YN               VARCHAR(1)     DEFAULT 'N' NOT NULL,
    MAX_RETRY_COUNT            NUMERIC(3,0)   DEFAULT 0 NOT NULL,
    RETRY_MI_CYCLE             VARCHAR(4),
    BIZ_GROUP_ID               VARCHAR(10),
    BIZDAY_TRX_YN              VARCHAR(1),
    BIZDAY_TRX_START_TIME      VARCHAR(4),
    BIZDAY_TRX_END_TIME        VARCHAR(4),
    SATURDAY_TRX_YN            VARCHAR(1),
    SATURDAY_TRX_START_TIME    VARCHAR(4),
    SATURDAY_TRX_END_TIME      VARCHAR(4),
    HOLIDAY_TRX_YN             VARCHAR(1),
    HOLIDAY_TRX_START_TIME     VARCHAR(4),
    HOLIDAY_TRX_END_TIME       VARCHAR(4),
    TRX_STOP_REASON            VARCHAR(200),
    VERSION                    NUMERIC(3,0)   NOT NULL,
    HISTORY_REASON             VARCHAR(100),
    CONSTRAINT PK_FWK_TRX_HISTORY PRIMARY KEY (TRX_ID, VERSION)
);

CREATE TABLE FWK_TRX_MESSAGE (
    TRX_ID                     VARCHAR(40)    NOT NULL,
    ORG_ID                     VARCHAR(10)    NOT NULL,
    IO_TYPE                    VARCHAR(1)     NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    STD_MESSAGE_ID             VARCHAR(50),
    RES_MESSAGE_ID             VARCHAR(50),
    STD_RES_MESSAGE_ID         VARCHAR(50),
    PROXY_RES_YN               VARCHAR(1)     DEFAULT 'Y',
    PROXY_RES_DATA             VARBINARY(2000),
    EXECUTE_SEQ                NUMERIC(3,0)   DEFAULT 1,
    PROXY_RES_TYPE             VARCHAR(1)     DEFAULT 'M',
    HEX_LOG_YN                 VARCHAR(1),
    MULTI_RES_YN               VARCHAR(1),
    RES_TYPE_FIELD_ID          VARCHAR(50),
    MULTI_RES_TYPE             VARCHAR(1)     DEFAULT '1',
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20),
    TIMEOUT_SEC                NUMERIC(3,0)   DEFAULT 210 NOT NULL,
    LEGACY_MESSAGE_ID          VARCHAR(20),
    TARGET_SERVICE_URI         VARCHAR(200),
    CONSTRAINT PK_FWK_TRX_MESSAGE PRIMARY KEY (TRX_ID, ORG_ID, IO_TYPE)
);

CREATE TABLE FWK_TRX_MESSAGE_HISTORY (
    TRX_ID                     VARCHAR(40)    NOT NULL,
    ORG_ID                     VARCHAR(10)    NOT NULL,
    IO_TYPE                    VARCHAR(1)     NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    STD_MESSAGE_ID             VARCHAR(50),
    RES_MESSAGE_ID             VARCHAR(50),
    STD_RES_MESSAGE_ID         VARCHAR(50),
    PROXY_RES_YN               VARCHAR(1)     DEFAULT 'Y',
    PROXY_RES_DATA             VARBINARY(2000),
    EXECUTE_SEQ                NUMERIC(3,0)   DEFAULT 1,
    PROXY_RES_TYPE             VARCHAR(1)     DEFAULT 'M',
    HEX_LOG_YN                 VARCHAR(1),
    MULTI_RES_YN               VARCHAR(1),
    RES_TYPE_FIELD_ID          VARCHAR(50),
    MULTI_RES_TYPE             VARCHAR(1)     DEFAULT '1',
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20),
    TIMEOUT_SEC                NUMERIC(3,0)   DEFAULT 210 NOT NULL,
    LEGACY_MESSAGE_ID          VARCHAR(20),
    VERSION                    NUMERIC(3,0)   NOT NULL,
    CONSTRAINT PK_FWK_TRX_MESSAGE_HISTORY PRIMARY KEY (TRX_ID, ORG_ID, IO_TYPE, VERSION)
);

CREATE TABLE FWK_TRX_STOP_HISTORY (
    GUBUN_TYPE                 VARCHAR(1)     NOT NULL,
    TRX_STOP_UPDATE_DTIME      VARCHAR(14)    NOT NULL,
    TRX_ID                     VARCHAR(40)    NOT NULL,
    TRX_STOP_REASON            VARCHAR(50),
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    TRX_STOP_YN                VARCHAR(1),
    CONSTRAINT PK_FWK_TRX_STOP_HISTORY PRIMARY KEY (GUBUN_TYPE, TRX_STOP_UPDATE_DTIME, TRX_ID)
);

CREATE TABLE FWK_MESSAGE (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    MESSAGE_NAME               VARCHAR(100)   NOT NULL,
    MESSAGE_DESC               VARCHAR(200),
    MESSAGE_TYPE               VARCHAR(1),
    PARENT_MESSAGE_ID          VARCHAR(50),
    HEADER_YN                  VARCHAR(1)     DEFAULT 'N',
    REQUEST_YN                 VARCHAR(2)     DEFAULT NULL,
    TRX_TYPE                   VARCHAR(1)     DEFAULT '1' NOT NULL,
    PRE_LOAD_YN                VARCHAR(1)     DEFAULT 'N' NOT NULL,
    LOG_LEVEL                  VARCHAR(1),
    BIZ_DOMAIN                 VARCHAR(10),
    VALIDATION_USE_YN          VARCHAR(1)     DEFAULT 'N' NOT NULL,
    LOCK_YN                    VARCHAR(1)     DEFAULT 'N' NOT NULL,
    CUR_VERSION                NUMERIC(3,0),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20),
    CONSTRAINT PK_FWK_MESSAGE PRIMARY KEY (ORG_ID, MESSAGE_ID)
);

CREATE TABLE FWK_MESSAGE_FIELD (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    MESSAGE_FIELD_ID           VARCHAR(50)    NOT NULL,
    SORT_ORDER                 NUMERIC(4,0)   DEFAULT 0 NOT NULL,
    DATA_TYPE                  VARCHAR(1)     NOT NULL,
    DATA_LENGTH                NUMERIC        NOT NULL,
    "SCALE"                    NUMERIC(2,0),
    ALIGN                      VARCHAR(1)     NOT NULL,
    FILLER                     VARCHAR(20),
    FIELD_TYPE                 VARCHAR(10),
    USE_MODE                   VARCHAR(20),
    REQUIRED_YN                VARCHAR(1)     DEFAULT 'Y',
    FIELD_TAG                  VARCHAR(500),
    CODE_GROUP                 VARCHAR(8),
    DEFAULT_VALUE              VARCHAR(500),
    TEST_VALUE                 VARCHAR(500),
    REMARK                     VARCHAR(1)     DEFAULT NULL,
    LOG_YN                     VARCHAR(1)     DEFAULT 'Y',
    CODE_MAPPING_YN            VARCHAR(1)     DEFAULT 'N',
    MESSAGE_FIELD_NAME         VARCHAR(100),
    MESSAGE_FIELD_DESC         VARCHAR(200),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    VALIDATION_RULE_ID         VARCHAR(20),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    FIELD_FORMAT               VARCHAR(200),
    FIELD_FORMAT_DESC          VARCHAR(500),
    FIELD_OPTION               VARCHAR(200),
    FIELD_REPEAT_CNT           NUMERIC(10,0)
);

CREATE TABLE FWK_MESSAGE_FIELD_HISTORY (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    MESSAGE_FIELD_ID           VARCHAR(50)    NOT NULL,
    VERSION                    NUMERIC(3,0)   NOT NULL,
    SORT_ORDER                 NUMERIC(4,0)   DEFAULT 0 NOT NULL,
    DATA_TYPE                  VARCHAR(1)     NOT NULL,
    DATA_LENGTH                NUMERIC        NOT NULL,
    "SCALE"                    NUMERIC(2,0),
    ALIGN                      VARCHAR(1)     NOT NULL,
    FILLER                     VARCHAR(20),
    FIELD_TYPE                 VARCHAR(10),
    USE_MODE                   VARCHAR(20),
    REQUIRED_YN                VARCHAR(1)     DEFAULT 'Y',
    FIELD_TAG                  VARCHAR(500),
    CODE_GROUP                 VARCHAR(8),
    DEFAULT_VALUE              VARCHAR(500),
    TEST_VALUE                 VARCHAR(500),
    REMARK                     VARCHAR(1)     DEFAULT NULL,
    LOG_YN                     VARCHAR(1)     DEFAULT 'Y',
    CODE_MAPPING_YN            VARCHAR(1)     DEFAULT 'N',
    MESSAGE_FIELD_NAME         VARCHAR(100),
    MESSAGE_FIELD_DESC         VARCHAR(200),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    VALIDATION_RULE_ID         VARCHAR(20),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    CONSTRAINT PK_FWK_MESSAGE_FIELD_HISTORY PRIMARY KEY (ORG_ID, MESSAGE_ID, MESSAGE_FIELD_ID, VERSION)
);

CREATE TABLE FWK_MESSAGE_FIELD_MAPPING (
    TRG_ORG_ID                 VARCHAR(10)    NOT NULL,
    TRG_MESSAGE_ID             VARCHAR(50)    NOT NULL,
    SRC_ORG_ID                 VARCHAR(10)    NOT NULL,
    TRG_MESSAGE_FIELD_ID       VARCHAR(50)    NOT NULL,
    SRC_MESSAGE_ID             VARCHAR(50)    NOT NULL,
    MAPPING_EXPR               VARCHAR(500),
    SRC_PARENT_MESSAGE_FIELD_ID VARCHAR(50),
    TRG_PARENT_MESSAGE_FIELD_ID VARCHAR(50)
);


-- =============================================================
-- 5. 전문 테스트/인스턴스 (2 tables)
-- =============================================================

CREATE TABLE FWK_MESSAGE_TEST (
    TEST_SNO                   NUMERIC(30,0)  NOT NULL,
    USER_ID                    VARCHAR(20)    NOT NULL,
    ORG_ID                     VARCHAR(10)    NOT NULL,
    MESSAGE_ID                 VARCHAR(30)    NOT NULL,
    HEADER_YN                  VARCHAR(1)     NOT NULL,
    XML_YN                     VARCHAR(1)     NOT NULL,
    TEST_NAME                  VARCHAR(50)    NOT NULL,
    TEST_DESC                  VARCHAR(500),
    TEST_DATA                  VARCHAR(4000),
    TEST_DATA1                 VARCHAR(4000),
    TEST_DATA2                 VARCHAR(4000),
    TEST_DATA3                 VARCHAR(4000),
    TEST_DATA4                 VARCHAR(4000),
    TEST_GROUP_ID              VARCHAR(20),
    TRX_ID                     VARCHAR(50),
    DEFAULT_PROXY_YN           VARCHAR(1),
    TEST_HEADER_DATA           CLOB,
    TEST_HEADER_USE_YN         VARCHAR(1),
    PROXY_FIELD                VARCHAR(50),
    PROXY_VALUE                VARCHAR(100),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USERID         VARCHAR(20),
    TEST_HEADER_DATA1          VARCHAR(4000),
    CONSTRAINT PK_FWK_MESSAGE_TEST PRIMARY KEY (TEST_SNO)
);

CREATE TABLE FWK_MESSAGE_INSTANCE (
    MESSAGE_SNO                VARCHAR(30)    NOT NULL,
    TRX_ID                     VARCHAR(40)    NOT NULL,
    ORG_ID                     VARCHAR(10)    NOT NULL,
    IO_TYPE                    VARCHAR(1)     NOT NULL,
    REQ_RES_TYPE               VARCHAR(1)     NOT NULL,
    MESSAGE_ID                 VARCHAR(50)    NOT NULL,
    TRX_TRACKING_NO            VARCHAR(30)    NOT NULL,
    USER_ID                    VARCHAR(20)    NOT NULL,
    LOG_DTIME                  VARCHAR(17)    NOT NULL,
    LAST_LOG_DTIME             VARCHAR(17)    NOT NULL,
    LAST_RT_CODE               VARCHAR(40)    NOT NULL,
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    RETRY_TRX_YN               VARCHAR(1)     NOT NULL,
    MESSAGE_DATA               VARCHAR(4000),
    TRX_DTIME                  VARCHAR(14)    NOT NULL,
    CHANNEL_TYPE               VARCHAR(10),
    URI                        VARCHAR(200),
    LOG_DATA                   CLOB,
    LOG_DATA2                  VARCHAR(4000),
    LOG_DATA3                  VARCHAR(4000),
    LOG_DATA4                  VARCHAR(4000),
    TESTCASE_ID                VARCHAR(50),
    TESTCASE_SNO               NUMERIC(30,0),
    SUCCESS_YN                 VARCHAR(1)
);


-- =============================================================
-- 6. Gateway/Interface (6 tables)
-- =============================================================

CREATE TABLE FWK_GATEWAY (
    GW_ID                      VARCHAR(20)    NOT NULL,
    GW_NAME                    VARCHAR(200),
    THREAD_COUNT               NUMERIC(3,0),
    GW_PROPERTIES              VARCHAR(500),
    GW_DESC                    VARCHAR(1000),
    GW_APP_NAME                VARCHAR(200)   DEFAULT NULL,
    IO_TYPE                    VARCHAR(1)     DEFAULT 'O' NOT NULL,
    CONSTRAINT PK_FWK_GATEWAY PRIMARY KEY (GW_ID)
);

CREATE TABLE FWK_SYSTEM (
    GW_ID                      VARCHAR(20)    NOT NULL,
    SYSTEM_ID                  VARCHAR(20)    NOT NULL,
    OPER_MODE_TYPE             VARCHAR(1),
    IP                         VARCHAR(15),
    PORT                       VARCHAR(5),
    STOP_YN                    VARCHAR(1)     DEFAULT 'N',
    SYSTEM_DESC                VARCHAR(1000),
    APPLIED_WAS_INSTANCE       VARCHAR(100),
    CONSTRAINT PK_FWK_SYSTEM PRIMARY KEY (GW_ID, SYSTEM_ID)
);

CREATE TABLE FWK_MESSAGE_HANDLER (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    TRX_TYPE                   VARCHAR(1)     NOT NULL,
    IO_TYPE                    VARCHAR(1)     NOT NULL,
    HANDLER                    VARCHAR(200),
    HANDLER_DESC               VARCHAR(2000),
    STOP_YN                    VARCHAR(1)     DEFAULT 'N' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    OPER_MODE_TYPE             VARCHAR(1)     NOT NULL,
    CONSTRAINT PK_FWK_MESSAGE_HANDLER PRIMARY KEY (ORG_ID, TRX_TYPE, IO_TYPE, OPER_MODE_TYPE)
);

CREATE TABLE FWK_LISTENER_TRX_MESSAGE (
    GW_ID                      VARCHAR(20)    NOT NULL,
    REQ_ID_CODE                VARCHAR(40)    NOT NULL,
    TRX_ID                     VARCHAR(40),
    ORG_ID                     VARCHAR(10),
    IO_TYPE                    VARCHAR(1),
    BIZ_APP_ID                 VARCHAR(100),
    CONSTRAINT PK_FWK_LISTENER_TRX_MESSAGE PRIMARY KEY (GW_ID, REQ_ID_CODE)
);

CREATE TABLE FWK_LISTENER_CONNECTOR_MAPPING (
    LISTENER_GW_ID             VARCHAR(20)    NOT NULL,
    LISTENER_SYSTEM_ID         VARCHAR(20)    NOT NULL,
    IDENTIFIER                 VARCHAR(100)   NOT NULL,
    CONNECTOR_GW_ID            VARCHAR(20)    NOT NULL,
    CONNECTOR_SYSTEM_ID        VARCHAR(20)    NOT NULL,
    DESCRIPTION                VARCHAR(200),
    CONSTRAINT PK_FWK_LISTENER_CONN_MAPPING PRIMARY KEY (LISTENER_GW_ID, LISTENER_SYSTEM_ID, IDENTIFIER)
);

CREATE TABLE FWK_TRANSPORT (
    ORG_ID                     VARCHAR(10)    NOT NULL,
    TRX_TYPE                   VARCHAR(1)     NOT NULL,
    IO_TYPE                    VARCHAR(1)     NOT NULL,
    REQ_RES_TYPE               VARCHAR(1)     NOT NULL,
    GW_ID                      VARCHAR(20),
    CONSTRAINT PK_FWK_TRANSPORT PRIMARY KEY (ORG_ID, TRX_TYPE, IO_TYPE, REQ_RES_TYPE)
);


-- =============================================================
-- 7. WAS (7 tables)
-- =============================================================

CREATE TABLE FWK_WAS_GROUP (
    WAS_GROUP_ID               VARCHAR(20)    NOT NULL,
    WAS_GROUP_NAME             VARCHAR(50),
    WAS_GROUP_DESC             VARCHAR(200),
    CONSTRAINT PK_FWK_WAS_GROUP PRIMARY KEY (WAS_GROUP_ID)
);

CREATE TABLE FWK_WAS_INSTANCE (
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    INSTANCE_NAME              VARCHAR(50),
    INSTANCE_DESC              VARCHAR(200),
    WAS_CONFIG_ID              VARCHAR(10),
    INSTANCE_TYPE              VARCHAR(1),
    IP                         VARCHAR(15),
    PORT                       VARCHAR(5),
    OPER_MODE_TYPE             VARCHAR(1),
    CONSTRAINT PK_FWK_WAS_INSTANCE PRIMARY KEY (INSTANCE_ID)
);

CREATE TABLE FWK_WAS_GROUP_INSTANCE (
    WAS_GROUP_ID               VARCHAR(20)    NOT NULL,
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    CONSTRAINT PK_FWK_WAS_GROUP_INSTANCE PRIMARY KEY (WAS_GROUP_ID, INSTANCE_ID)
);

CREATE TABLE FWK_WAS_LISTENER (
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    GW_ID                      VARCHAR(20)    NOT NULL,
    SYSTEM_ID                  VARCHAR(20)    NOT NULL,
    WAS_INSTANCE_STATUS        VARCHAR(5),
    ACTIVE_COUNT_IDLE          NUMERIC(3,0),
    LAST_UPDATE_DTIME          VARCHAR(20),
    CONSTRAINT PK_FWK_WAS_LISTENER PRIMARY KEY (INSTANCE_ID, GW_ID, SYSTEM_ID)
);

CREATE TABLE FWK_WAS_PROPERTY (
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    PROPERTY_GROUP_ID          VARCHAR(20)    NOT NULL,
    PROPERTY_ID                VARCHAR(50)    NOT NULL,
    PROPERTY_VALUE             VARCHAR(1000)  NOT NULL,
    PROPERTY_DESC              VARCHAR(1000),
    CUR_VERSION                NUMERIC,
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_WAS_PROPERTY PRIMARY KEY (INSTANCE_ID, PROPERTY_GROUP_ID, PROPERTY_ID)
);

CREATE TABLE FWK_WAS_PROPERTY_HISTORY (
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    PROPERTY_GROUP_ID          VARCHAR(20)    NOT NULL,
    PROPERTY_ID                VARCHAR(50)    NOT NULL,
    VERSION                    NUMERIC(5,0)   NOT NULL,
    PROPERTY_VALUE             VARCHAR(1000)  NOT NULL,
    PROPERTY_DESC              VARCHAR(1000),
    REASON                     VARCHAR(2000),
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    CONSTRAINT PK_FWK_WAS_PROPERTY_HISTORY PRIMARY KEY (INSTANCE_ID, PROPERTY_GROUP_ID, PROPERTY_ID, VERSION)
);

CREATE TABLE FWK_WAS_EXEC_BATCH (
    BATCH_APP_ID               VARCHAR(50)    NOT NULL,
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_WAS_EXEC_BATCH PRIMARY KEY (BATCH_APP_ID, INSTANCE_ID)
);


-- =============================================================
-- 8. 배치 (2 tables)
-- =============================================================

CREATE TABLE FWK_BATCH_APP (
    BATCH_APP_ID               VARCHAR(50)    NOT NULL,
    BATCH_APP_NAME             VARCHAR(50),
    BATCH_APP_FILE_NAME        VARCHAR(200)   NOT NULL,
    BATCH_APP_DESC             VARCHAR(200),
    PRE_BATCH_APP_ID           VARCHAR(50),
    BATCH_CYCLE                VARCHAR(1)     NOT NULL,
    CRON_TEXT                  VARCHAR(20),
    RETRYABLE_YN               VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    PER_WAS_YN                 VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    IMPORTANT_TYPE             VARCHAR(1)     NOT NULL,
    PROPERTIES                 VARCHAR(500),
    TRX_ID                     VARCHAR(20),
    ORG_ID                     VARCHAR(10),
    IO_TYPE                    VARCHAR(1),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_BATCH_APP PRIMARY KEY (BATCH_APP_ID)
);

CREATE TABLE FWK_BATCH_HIS (
    BATCH_APP_ID               VARCHAR(50)    NOT NULL,
    INSTANCE_ID                VARCHAR(4)     NOT NULL,
    BATCH_DATE                 VARCHAR(8)     NOT NULL,
    BATCH_EXECUTE_SEQ          NUMERIC(3,0)   DEFAULT 1 NOT NULL,
    LOG_DTIME                  VARCHAR(17)    NOT NULL,
    BATCH_END_DTIME            VARCHAR(17)    DEFAULT NULL,
    RES_RT_CODE                VARCHAR(40),
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    ERROR_CODE                 VARCHAR(40),
    ERROR_REASON               VARCHAR(4000),
    RECORD_COUNT               NUMERIC(10,0),
    EXECUTE_COUNT              NUMERIC(10,0),
    SUCCESS_COUNT              NUMERIC(10,0),
    FAIL_COUNT                 NUMERIC(10,0),
    CONSTRAINT PK_FWK_BATCH_HIS PRIMARY KEY (BATCH_APP_ID, INSTANCE_ID, BATCH_DATE, BATCH_EXECUTE_SEQ)
);


-- =============================================================
-- 9. 오류 (6 tables)
-- =============================================================

CREATE TABLE FWK_ERROR (
    ERROR_CODE                 VARCHAR(40)    NOT NULL,
    TRX_ID                     VARCHAR(40),
    ERROR_TITLE                VARCHAR(200)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    ORG_ID                     VARCHAR(10),
    ORG_ERROR_CODE             VARCHAR(20),
    ERROR_LEVEL                VARCHAR(1)     DEFAULT '1' NOT NULL,
    ERROR_HTTPCODE             VARCHAR(20),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_ERROR PRIMARY KEY (ERROR_CODE)
);

CREATE TABLE FWK_ERROR_DESC (
    ERROR_CODE                 VARCHAR(40)    NOT NULL,
    LOCALE_CODE                VARCHAR(5)     NOT NULL,
    ERROR_TITLE                VARCHAR(200),
    PB_ERROR_TITLE             VARCHAR(400),
    ETC_ERROR_TITLE            VARCHAR(400),
    ERROR_CAUSE_DESC           VARCHAR(1600),
    ERROR_GUIDE_DESC           VARCHAR(1600),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    HELP_PAGE_URL              VARCHAR(200),
    IBS_ERROR_GUIDE_DESC       VARCHAR(1600),
    CMS_ERROR_GUIDE_DESC       VARCHAR(1600),
    ETC_ERROR_GUIDE_DESC       VARCHAR(1600),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_ERROR_DESC PRIMARY KEY (ERROR_CODE, LOCALE_CODE)
);

CREATE TABLE FWK_ERROR_HIS (
    ERROR_CODE                 VARCHAR(40)    NOT NULL,
    ERROR_SER_NO               VARCHAR(28)    NOT NULL,
    CUST_USER_ID               VARCHAR(20)    NOT NULL,
    ERROR_MESSAGE              VARCHAR(500),
    ERROR_OCCUR_DTIME          VARCHAR(14)    NOT NULL,
    ERROR_URL                  VARCHAR(300),
    ERROR_TRACE                VARCHAR(4000),
    ERROR_INSTANCE_ID          VARCHAR(10),
    CONSTRAINT PK_FWK_ERROR_HIS PRIMARY KEY (ERROR_CODE, ERROR_SER_NO)
);

CREATE TABLE FWK_HANDLE_APP (
    HANDLE_APP_ID              VARCHAR(50)    NOT NULL,
    HANDLE_APP_NAME            VARCHAR(50)    NOT NULL,
    HANDLE_APP_DESC            VARCHAR(200)   NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    SYS_PARAM_VALUE            VARCHAR(1000),
    PARAM_DESC                 VARCHAR(2000),
    HANDLE_APP_FILE            VARCHAR(100),
    CONSTRAINT PK_FWK_HANDLE_APP PRIMARY KEY (HANDLE_APP_ID)
);

CREATE TABLE FWK_ERROR_HANDLE_APP (
    ERROR_CODE                 VARCHAR(40)    NOT NULL,
    HANDLE_APP_ID              VARCHAR(50)    NOT NULL,
    USER_PARAM_VALUE           VARCHAR(1000),
    CONSTRAINT PK_FWK_ERROR_HANDLE_APP PRIMARY KEY (ERROR_CODE, HANDLE_APP_ID)
);

CREATE TABLE FWK_ERROR_HANDLE_HIS (
    ERROR_CODE                 VARCHAR(40)    NOT NULL,
    HANDLE_APP_ID              VARCHAR(50)    NOT NULL,
    ERROR_SER_NO               VARCHAR(28)    NOT NULL,
    ERROR_HANDLE_DTIME         VARCHAR(14)    NOT NULL,
    ERROR_HANDLE_RT_CODE       VARCHAR(8)     NOT NULL,
    ERROR_HANDLE_TEXT          VARCHAR(2000),
    CONSTRAINT PK_FWK_ERROR_HANDLE_HIS PRIMARY KEY (ERROR_CODE, HANDLE_APP_ID, ERROR_SER_NO)
);


-- =============================================================
-- 10. 게시판 (5 tables)
-- =============================================================

CREATE TABLE FWK_BOARD (
    BOARD_ID                   VARCHAR(20)    NOT NULL,
    BOARD_NAME                 VARCHAR(50)    NOT NULL,
    BOARD_TYPE                 VARCHAR(1)     NOT NULL,
    ADMIN_ID                   VARCHAR(20),
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_BOARD PRIMARY KEY (BOARD_ID)
);

CREATE TABLE FWK_BOARD_AUTH (
    USER_ID                    VARCHAR(20)    NOT NULL,
    BOARD_ID                   VARCHAR(20)    NOT NULL,
    AUTH_CODE                  VARCHAR(1)     NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_BOARD_AUTH PRIMARY KEY (USER_ID, BOARD_ID)
);

CREATE TABLE FWK_BOARD_CATEGORY (
    BOARD_ID                   VARCHAR(20)    NOT NULL,
    CATEGORY_SEQ               VARCHAR(10)    NOT NULL,
    CATEGORY_NAME              VARCHAR(50)    NOT NULL,
    CONSTRAINT PK_FWK_BOARD_CATEGORY PRIMARY KEY (BOARD_ID, CATEGORY_SEQ)
);

CREATE TABLE FWK_ARTICLE (
    ARTICLE_SEQ                NUMERIC        NOT NULL,
    BOARD_ID                   VARCHAR(20)    NOT NULL,
    CATEGORY_SEQ               VARCHAR(20),
    TOP_YN                     VARCHAR(1)     NOT NULL,
    REF_ARTICLE_SEQ            NUMERIC,
    "POSITION"                 NUMERIC        NOT NULL,
    STEP                       NUMERIC        NOT NULL,
    TITLE                      VARCHAR(500)   NOT NULL,
    WRITER_ID                  VARCHAR(20)    NOT NULL,
    WRITER_NAME                VARCHAR(50)    NOT NULL,
    READ_CNT                   NUMERIC        NOT NULL,
    ATTATCH_FILE_PATH_1        VARCHAR(500),
    DOWNLOAD_CNT_1             NUMERIC,
    ATTATCH_FILE_PATH_2        VARCHAR(500),
    DOWNLOAD_CNT_2             NUMERIC,
    ATTATCH_FILE_PATH_3        VARCHAR(500),
    DOWNLOAD_CNT_3             NUMERIC,
    REGIST_DTIME               VARCHAR(14)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONTENT                    CLOB
);

CREATE TABLE FWK_ARTICLE_USER (
    USER_ID                    VARCHAR(20)    NOT NULL,
    BOARD_ID                   VARCHAR(20)    NOT NULL,
    ARTICLE_SEQ                NUMERIC        NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    CONSTRAINT PK_FWK_ARTICLE_USER PRIMARY KEY (USER_ID, BOARD_ID, ARTICLE_SEQ)
);


-- =============================================================
-- 11. 프로퍼티/모니터링 (4 tables)
-- =============================================================

CREATE TABLE FWK_PROPERTY (
    PROPERTY_GROUP_ID          VARCHAR(20)    NOT NULL,
    PROPERTY_ID                VARCHAR(100)   NOT NULL,
    PROPERTY_NAME              VARCHAR(100)   NOT NULL,
    PROPERTY_DESC              VARCHAR(300)   NOT NULL,
    DATA_TYPE                  VARCHAR(1),
    VALID_DATA                 VARCHAR(1000),
    DEFAULT_VALUE              VARCHAR(1000),
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    CUR_VERSION                NUMERIC,
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    CONSTRAINT PK_FWK_PROPERTY PRIMARY KEY (PROPERTY_GROUP_ID, PROPERTY_ID)
);

CREATE TABLE FWK_PROPERTY_HISTORY (
    PROPERTY_GROUP_ID          VARCHAR(20)    NOT NULL,
    PROPERTY_ID                VARCHAR(100)   NOT NULL,
    VERSION                    NUMERIC(5,0)   NOT NULL,
    PROPERTY_NAME              VARCHAR(100)   NOT NULL,
    PROPERTY_DESC              VARCHAR(300)   NOT NULL,
    DATA_TYPE                  VARCHAR(1),
    VALID_DATA                 VARCHAR(1000),
    DEFAULT_VALUE              VARCHAR(1000),
    REASON                     VARCHAR(2000),
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    CONSTRAINT PK_FWK_PROPERTY_HISTORY PRIMARY KEY (PROPERTY_GROUP_ID, PROPERTY_ID, VERSION)
);

CREATE TABLE FWK_MONITOR (
    MONITOR_ID                 VARCHAR(20)    NOT NULL,
    MONITOR_NAME               VARCHAR(50)    NOT NULL,
    MONITOR_QUERY              VARCHAR(2000),
    ALERT_CONDITION            VARCHAR(100),
    ALERT_MESSAGE              VARCHAR(100),
    REFRESH_TERM               VARCHAR(4)     NOT NULL,
    DETAIL_QUERY               VARCHAR(2000),
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_MONITOR PRIMARY KEY (MONITOR_ID)
);

CREATE TABLE FWK_VALIDATOR (
    VALIDATOR_ID               VARCHAR(20)    NOT NULL,
    VALIDATOR_NAME             VARCHAR(50)    NOT NULL,
    VALIDATOR_DESC             VARCHAR(200),
    BIZ_DOMAIN                 VARCHAR(10)    NOT NULL,
    JAVA_CLASS_NAME            VARCHAR(100)   NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    LAST_UPDATE_DTIME          VARCHAR(14)    NOT NULL,
    LAST_UPDATE_USER_ID        VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_VALIDATOR PRIMARY KEY (VALIDATOR_ID)
);


-- =============================================================
-- 12. 서비스/비즈니스 (5 tables)
-- =============================================================

CREATE TABLE FWK_SERVICE (
    SERVICE_ID                 VARCHAR(30)    NOT NULL,
    SERVICE_NAME               VARCHAR(100)   NOT NULL,
    SERVICE_DESC               VARCHAR(300),
    CLASS_NAME                 VARCHAR(100),
    METHOD_NAME                VARCHAR(50),
    SERVICE_TYPE               VARCHAR(1),
    PRE_PROCESS_APP_ID         VARCHAR(100),
    POST_PROCESS_APP_ID        VARCHAR(100),
    TIME_CHECK_YN              VARCHAR(1)     DEFAULT 'N' NOT NULL,
    START_TIME                 VARCHAR(4)     DEFAULT '2400',
    END_TIME                   VARCHAR(4)     DEFAULT '2400',
    BIZ_DAY_CHECK_YN           VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    USE_YN                     VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    TRX_ID                     VARCHAR(40),
    ORG_ID                     VARCHAR(10),
    IO_TYPE                    VARCHAR(1),
    BIZ_GROUP_ID               VARCHAR(20),
    WORK_SPACE_ID              VARCHAR(20),
    LOGIN_ONLY_YN              VARCHAR(1)     DEFAULT 'Y' NOT NULL,
    SECURE_SIGN_YN             VARCHAR(1)     DEFAULT 'N' NOT NULL,
    REQ_CHANNEL_CODE           VARCHAR(1)     DEFAULT '3',
    SVC_CONF_1                 VARCHAR(20),
    SVC_CONF_2                 VARCHAR(20),
    BANK_STATUS_CHECK_YN       VARCHAR(1)     DEFAULT 'N' NOT NULL,
    BANK_CODE_FIELD            VARCHAR(30),
    BIZDAY_SERVICE_YN          VARCHAR(1),
    BIZDAY_SERVICE_START_TIME  VARCHAR(4),
    BIZDAY_SERVICE_END_TIME    VARCHAR(4),
    SATURDAY_SERVICE_YN        VARCHAR(1),
    SATURDAY_SERVICE_START_TIME VARCHAR(4),
    SATURDAY_SERVICE_END_TIME  VARCHAR(4),
    HOLIDAY_SERVICE_YN         VARCHAR(1),
    HOLIDAY_SERVICE_START_TIME VARCHAR(4),
    HOLIDAY_SERVICE_END_TIME   VARCHAR(4),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    CONSTRAINT PK_FWK_SERVICE PRIMARY KEY (SERVICE_ID)
);

CREATE TABLE FWK_SERVICE_RELATION (
    SERVICE_ID                 VARCHAR(30)    NOT NULL,
    SERVICE_SEQ_NO             NUMERIC(3,0)   DEFAULT 1 NOT NULL,
    COMPONENT_ID               VARCHAR(40)    NOT NULL,
    POST_CONDITION             VARCHAR(30),
    EXPRESSION                 VARCHAR(100),
    RELATION_ETC1              VARCHAR(500),
    RELATION_ETC2              VARCHAR(500),
    RELATION_ETC3              VARCHAR(500),
    RELATION_ETC4              VARCHAR(500),
    LAST_UPDATE_DTIME          VARCHAR(14),
    CONSTRAINT PK_FWK_SERVICE_RELATION PRIMARY KEY (SERVICE_ID, SERVICE_SEQ_NO)
);

CREATE TABLE FWK_COMPONENT (
    COMPONENT_ID               VARCHAR(50)    NOT NULL,
    COMPONENT_NAME             VARCHAR(100)   NOT NULL,
    COMPONENT_DESC             VARCHAR(300),
    COMPONENT_TYPE             VARCHAR(1)     NOT NULL,
    COMPONENT_CLASS_NAME       VARCHAR(100)   NOT NULL,
    COMPONENT_METHOD_NAME      VARCHAR(50)    NOT NULL,
    COMPONENT_CREATE_TYPE      VARCHAR(1),
    BIZ_GROUP_ID               VARCHAR(20),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    CONSTRAINT PK_FWK_COMPONENT PRIMARY KEY (COMPONENT_ID)
);

CREATE TABLE FWK_RELATION_PARAM (
    SERVICE_ID                 VARCHAR(30)    NOT NULL,
    SERVICE_SEQ_NO             NUMERIC(3,0)   DEFAULT 0 NOT NULL,
    COMPONENT_ID               VARCHAR(40)    NOT NULL,
    PARAM_SEQ_NO               NUMERIC(8,0)   NOT NULL,
    PARAM_VALUE                VARCHAR(500),
    LAST_UPDATE_DTIME          VARCHAR(14),
    CONSTRAINT PK_FWK_RELATION_PARAM PRIMARY KEY (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO)
);

CREATE TABLE FWK_CUST_MENU_APP (
    MENU_URL                   VARCHAR(200)   NOT NULL,
    WEB_APP_ID                 VARCHAR(100)   NOT NULL,
    MENU_NAME                  VARCHAR(50),
    MENU_ID                    VARCHAR(40)    NOT NULL,
    ASYNC_YN                   VARCHAR(1)     DEFAULT 'N' NOT NULL,
    START_TIME                 VARCHAR(4)     DEFAULT '0000' NOT NULL,
    END_TIME                   VARCHAR(4)     DEFAULT '2400' NOT NULL,
    TIME_CHECK_YN              VARCHAR(1)     DEFAULT 'N' NOT NULL,
    BIZ_DAY_CHECK_YN           VARCHAR(1)     DEFAULT 'N' NOT NULL,
    LOG_YN                     VARCHAR(1)     DEFAULT 'N' NOT NULL,
    VALIDATION_MESSAGE_ID      VARCHAR(50),
    LOGIN_ONLY_YN              VARCHAR(1)     DEFAULT 'N',
    SECURE_SIGN_YN             VARCHAR(1)     DEFAULT 'N',
    BANK_STATUS_CHECK_YN       VARCHAR(1)     DEFAULT 'N',
    BANK_CODE_FIELD            VARCHAR(30),
    USE_YN                     VARCHAR(1)     DEFAULT 'Y',
    ENCRIPTION_YN              VARCHAR(1)     DEFAULT 'N',
    E_CHANNEL_CODE             VARCHAR(1),
    BIZ_DOMAIN                 VARCHAR(10),
    BIZ_GROUP_ID               VARCHAR(20),
    APP_TYPE                   VARCHAR(10),
    LAST_UPDATE_DTIME          VARCHAR(14),
    LAST_UPDATE_USER_ID        VARCHAR(20),
    STOP_REASON_KO             VARCHAR(4000),
    STOP_REASON_EN             VARCHAR(4000),
    URL_PATTERN                VARCHAR(10),
    INPUT_TYPE                 VARCHAR(1),
    WARNING_KO                 VARCHAR(4000),
    WARNING_EN                 VARCHAR(4000),
    CRM_LOG_TYPE1              VARCHAR(2),
    CRM_LOG_TYPE2              VARCHAR(2),
    CRM_LOG_TYPE3              VARCHAR(2),
    HDAY_START_TIME            VARCHAR(4)     DEFAULT '0000',
    HDAY_END_TIME              VARCHAR(4)     DEFAULT '2400',
    ECRM_DESC                  VARCHAR(400),
    VIEW_YN                    VARCHAR(1)     DEFAULT 'Y',
    IN_OUT_USE_YN              VARCHAR(1)     DEFAULT 'N',
    IN_OUT_MESSAGE_ID          VARCHAR(50),
    WARNING_START_DTIME        VARCHAR(14),
    WARNING_END_DTIME          VARCHAR(14),
    STOP_REASON_START_DTIME    VARCHAR(14),
    STOP_REASON_END_DTIME      VARCHAR(14),
    FINAL_APPROVAL_STATE       VARCHAR(1),
    FINAL_APPROVAL_DTIME       VARCHAR(14),
    FINAL_APPROVAL_USER_ID     VARCHAR(20),
    FINAL_APPROVAL_USER_NAME   VARCHAR(50),
    PRIOR_MENU_ID              VARCHAR(40),
    SORT_ORDER                 NUMERIC,
    PAGE_YN                    VARCHAR(1),
    TYPE_CHECK_YN              VARCHAR(1),
    NOTIFY_NAME                VARCHAR(500),
    ALTER_RESPONSE             CLOB,
    DISPLAY_YN                 VARCHAR(1)     DEFAULT 'Y',
    CONSTRAINT PK_FWK_CUST_MENU_APP PRIMARY KEY (MENU_URL)
);


-- =============================================================
-- 13. 이행 데이터 (2 tables)
-- =============================================================

CREATE TABLE FWK_TRANS_DATA_TIMES (
    TRAN_SEQ                   VARCHAR(14)    NOT NULL,
    TRAN_REASON                VARCHAR(100),
    TRAN_RESULT                VARCHAR(1)     NOT NULL,
    TRAN_TIME                  VARCHAR(14)    NOT NULL,
    USER_ID                    VARCHAR(20)    NOT NULL,
    CONSTRAINT PK_FWK_TRANS_DATA_TIMES PRIMARY KEY (TRAN_SEQ)
);

CREATE TABLE FWK_TRANS_DATA_HIS (
    TRAN_SEQ                   VARCHAR(14)    NOT NULL,
    TRAN_ID                    VARCHAR(50)    NOT NULL,
    TRAN_TYPE                  VARCHAR(10)    NOT NULL,
    TRAN_NAME                  VARCHAR(100),
    TRAN_RESULT                VARCHAR(1)     NOT NULL,
    TRAN_FAIL_REASON           VARCHAR(2000),
    TRAN_FAIL_SQL              VARCHAR(2000),
    TRAN_TIME                  VARCHAR(14)    NOT NULL,
    CONSTRAINT PK_FWK_TRANS_DATA_HIS PRIMARY KEY (TRAN_SEQ, TRAN_ID, TRAN_TYPE)
);
