-- =============================================================
-- batch-was DDL — 테이블 및 시퀀스 생성
-- 스키마 소유자(D_SPIDERLINK) 또는 DDL 권한 보유 계정으로 실행
-- ※ 쿼리 실행은 개발자가 DB에서 직접 수행해야 함
-- =============================================================


-- =============================================================
-- 1. POC_카드사용내역_백업
-- =============================================================
-- Db2DbJob(db2db)이 POC_카드사용내역 → 이 테이블로 아카이브한다.

CREATE TABLE POC_카드사용내역_백업 (
    이용자          VARCHAR2(20)   NOT NULL,
    카드번호        VARCHAR2(20)   NOT NULL,
    이용일자        VARCHAR2(8)    NOT NULL,
    이용가맹점      VARCHAR2(100),
    이용금액        NUMBER(15),
    할부개월        NUMBER(3),
    회차            NUMBER(3),                              -- 현재 할부 회차
    할부구분코드    VARCHAR2(2),                            -- 할부 구분 코드
    승인여부        VARCHAR2(1),
    카드명          VARCHAR2(100),
    승인시각        VARCHAR2(6)    NOT NULL,
    결제예정일      VARCHAR2(6),
    승인번호        VARCHAR2(20),
    결제잔액        NUMBER(15),
    누적결제금액    NUMBER(15),
    결제상태코드    VARCHAR2(1),
    최종결제일자    VARCHAR2(8),
    -- 제약명은 ASCII로 작성 (한글 포함 시 30바이트 초과로 ORA-00972 발생)
    CONSTRAINT PK_POC_CARD_USAGE_BAK
        PRIMARY KEY (이용자, 카드번호, 이용일자, 승인시각)
);

COMMIT;


-- =============================================================
-- 2. Spring Batch 5.x 전용 메타 테이블 (prefix: BATCH_5_)
-- =============================================================
-- batch-was는 table-prefix=BATCH_5_ 를 사용해 기존 BATCH_* 테이블과 충돌을 피한다.
-- 출처: spring-batch-core-5.x schema-oracle10g.sql
-- 아래 테이블/시퀀스가 DB에 없을 때 실행

-- [시퀀스]
CREATE SEQUENCE BATCH_5_JOB_SEQ            MAXVALUE 9223372036854775807 NOCYCLE;
CREATE SEQUENCE BATCH_5_JOB_EXECUTION_SEQ  MAXVALUE 9223372036854775807 NOCYCLE;
CREATE SEQUENCE BATCH_5_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NOCYCLE;

-- [테이블]
CREATE TABLE BATCH_5_JOB_INSTANCE (
    JOB_INSTANCE_ID NUMBER(19)    NOT NULL PRIMARY KEY,
    VERSION         NUMBER(19),
    JOB_NAME        VARCHAR2(100) NOT NULL,
    JOB_KEY         VARCHAR2(32)  NOT NULL,
    CONSTRAINT BWAS_JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_5_JOB_EXECUTION (
    JOB_EXECUTION_ID NUMBER(19)    NOT NULL PRIMARY KEY,
    VERSION          NUMBER(19),
    JOB_INSTANCE_ID  NUMBER(19)    NOT NULL,
    CREATE_TIME      TIMESTAMP     NOT NULL,
    START_TIME       TIMESTAMP,
    END_TIME         TIMESTAMP,
    STATUS           VARCHAR2(10),
    EXIT_CODE        VARCHAR2(2500),
    EXIT_MESSAGE     VARCHAR2(2500),
    LAST_UPDATED     TIMESTAMP,
    CONSTRAINT BWAS_JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
        REFERENCES BATCH_5_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_5_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID NUMBER(19)    NOT NULL,
    PARAMETER_NAME   VARCHAR2(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR2(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR2(2500),
    IDENTIFYING      CHAR(1)       NOT NULL,
    CONSTRAINT BWAS_JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_5_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_5_STEP_EXECUTION (
    STEP_EXECUTION_ID  NUMBER(19)    NOT NULL PRIMARY KEY,
    VERSION            NUMBER(19)    NOT NULL,
    STEP_NAME          VARCHAR2(100) NOT NULL,
    JOB_EXECUTION_ID   NUMBER(19)    NOT NULL,
    CREATE_TIME        TIMESTAMP     NOT NULL,
    START_TIME         TIMESTAMP,
    END_TIME           TIMESTAMP,
    STATUS             VARCHAR2(10),
    COMMIT_COUNT       NUMBER(19),
    READ_COUNT         NUMBER(19),
    FILTER_COUNT       NUMBER(19),
    WRITE_COUNT        NUMBER(19),
    READ_SKIP_COUNT    NUMBER(19),
    WRITE_SKIP_COUNT   NUMBER(19),
    PROCESS_SKIP_COUNT NUMBER(19),
    ROLLBACK_COUNT     NUMBER(19),
    EXIT_CODE          VARCHAR2(2500),
    EXIT_MESSAGE       VARCHAR2(2500),
    LAST_UPDATED       TIMESTAMP,
    CONSTRAINT BWAS_JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_5_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_5_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID  NUMBER(19)     NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR2(2500) NOT NULL,
    SERIALIZED_CONTEXT CLOB,
    CONSTRAINT BWAS_STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
        REFERENCES BATCH_5_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_5_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID   NUMBER(19)     NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR2(2500) NOT NULL,
    SERIALIZED_CONTEXT CLOB,
    CONSTRAINT BWAS_JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_5_JOB_EXECUTION (JOB_EXECUTION_ID)
);
