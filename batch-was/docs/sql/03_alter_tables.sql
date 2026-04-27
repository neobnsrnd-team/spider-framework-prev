-- =============================================================
-- batch-was DDL/DCL 변경 이력
-- 스키마 소유자(D_SPIDERLINK) 또는 권한 보유 계정으로 실행
-- ※ 쿼리 실행은 개발자가 DB에서 직접 수행해야 함
-- =============================================================


-- =============================================================
-- 1. Spring Batch 메타 테이블 접근 권한 부여
-- =============================================================
-- [발생 조건] DB_USERNAME ≠ DB_SCHEMA 구조에서 batch-was 기동 시
--   CURRENT_SCHEMA 전환만으로는 시퀀스 SELECT 권한이 자동 부여되지 않아
--   ORA-00942 발생 (Could not obtain sequence value)
-- [실행 주체] 스키마 소유자(DB_SCHEMA) 또는 DBA
-- ※ {DB_USERNAME}, {DB_SCHEMA}를 실제 값으로 치환 후 실행

-- BATCH_5_ 전용 시퀀스 권한 (기존 BATCH_* 시퀀스에는 영향 없음)
GRANT SELECT ON {DB_SCHEMA}.BATCH_5_JOB_SEQ            TO {DB_USERNAME};
GRANT SELECT ON {DB_SCHEMA}.BATCH_5_JOB_EXECUTION_SEQ  TO {DB_USERNAME};
GRANT SELECT ON {DB_SCHEMA}.BATCH_5_STEP_EXECUTION_SEQ TO {DB_USERNAME};

-- BATCH_5_ 전용 테이블 DML 권한
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_JOB_INSTANCE          TO {DB_USERNAME};
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_JOB_EXECUTION          TO {DB_USERNAME};
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_JOB_EXECUTION_PARAMS   TO {DB_USERNAME};
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_JOB_EXECUTION_CONTEXT  TO {DB_USERNAME};
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_STEP_EXECUTION          TO {DB_USERNAME};
GRANT SELECT, INSERT, UPDATE, DELETE ON {DB_SCHEMA}.BATCH_5_STEP_EXECUTION_CONTEXT  TO {DB_USERNAME};


-- =============================================================
-- 2. Spring Batch 시퀀스 동기화
-- =============================================================
-- [발생 조건] 시퀀스 NEXTVAL < 테이블 최대 ID 일 때
--   ORA-00001: unique constraint violated (BATCH_5_JOB_INSTANCE PK 충돌)
-- [참고] 신규 생성한 BATCH_5_* 시퀀스는 보통 불필요.
--   재기동 등으로 충돌 발생 시에만 실행

DECLARE
    v_max NUMBER;
    v_cur NUMBER;
    v_inc NUMBER;
BEGIN
    SELECT NVL(MAX(JOB_INSTANCE_ID), 0) INTO v_max FROM BATCH_5_JOB_INSTANCE;
    SELECT BATCH_5_JOB_SEQ.NEXTVAL INTO v_cur FROM DUAL;
    v_inc := v_max - v_cur + 100;
    IF v_inc > 0 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_JOB_SEQ INCREMENT BY ' || v_inc;
        SELECT BATCH_5_JOB_SEQ.NEXTVAL INTO v_cur FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_JOB_SEQ INCREMENT BY 1';
    END IF;
    DBMS_OUTPUT.PUT_LINE('BATCH_5_JOB_SEQ -> ' || v_cur);

    SELECT NVL(MAX(JOB_EXECUTION_ID), 0) INTO v_max FROM BATCH_5_JOB_EXECUTION;
    SELECT BATCH_5_JOB_EXECUTION_SEQ.NEXTVAL INTO v_cur FROM DUAL;
    v_inc := v_max - v_cur + 100;
    IF v_inc > 0 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_JOB_EXECUTION_SEQ INCREMENT BY ' || v_inc;
        SELECT BATCH_5_JOB_EXECUTION_SEQ.NEXTVAL INTO v_cur FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_JOB_EXECUTION_SEQ INCREMENT BY 1';
    END IF;
    DBMS_OUTPUT.PUT_LINE('BATCH_5_JOB_EXECUTION_SEQ -> ' || v_cur);

    SELECT NVL(MAX(STEP_EXECUTION_ID), 0) INTO v_max FROM BATCH_5_STEP_EXECUTION;
    SELECT BATCH_5_STEP_EXECUTION_SEQ.NEXTVAL INTO v_cur FROM DUAL;
    v_inc := v_max - v_cur + 100;
    IF v_inc > 0 THEN
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_STEP_EXECUTION_SEQ INCREMENT BY ' || v_inc;
        SELECT BATCH_5_STEP_EXECUTION_SEQ.NEXTVAL INTO v_cur FROM DUAL;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE BATCH_5_STEP_EXECUTION_SEQ INCREMENT BY 1';
    END IF;
    DBMS_OUTPUT.PUT_LINE('BATCH_5_STEP_EXECUTION_SEQ -> ' || v_cur);
END;
/

-- =============================================================
-- 3. POC_카드사용내역_백업 — 누락 컬럼 추가
-- =============================================================
-- [발생 경위] 01_create_tables.sql 초기 생성 시 원본 테이블(POC_카드사용내역)의
--   회차, 할부구분코드 컬럼이 누락됨.
--   Db2DbJob 아카이브 시 해당 컬럼도 복사해야 하므로 추가.

ALTER TABLE POC_카드사용내역_백업 ADD (
    회차          NUMBER(3),      -- 현재 할부 회차 (전체 할부개월 중 몇 번째)
    할부구분코드   VARCHAR2(2)     -- 할부 구분 코드 (예: 00=일시불, 01=할부)
);


-- ============================================================
-- #63: 고정 길이 파일 처리 Job 대상 테이블
-- 개발자가 직접 DB에서 실행해야 함
-- ============================================================
CREATE TABLE POC_고정길이거래 (
    ACCOUNT_NO      VARCHAR2(10)   NOT NULL,  -- 거래계좌번호
    TRX_DT          VARCHAR2(8)    NOT NULL,  -- 거래일자 (YYYYMMDD)
    TRX_TM          VARCHAR2(6)    NOT NULL,  -- 거래시각 (HHMMSS)
    AMOUNT          VARCHAR2(15),             -- 거래금액 (문자열, 우측정렬)
    TRX_TYPE_CODE   VARCHAR2(2),              -- 거래구분코드 (01=입금, 02=출금)
    MEMO            VARCHAR2(20),             -- 적요
    CONSTRAINT PK_POC_고정길이거래 PRIMARY KEY (ACCOUNT_NO, TRX_DT, TRX_TM)
);
