-- =====================================================
-- FWK_BATCH_HIS 월별 RANGE 파티셔닝 DDL
-- =====================================================
-- 작성일: 2026-01-21
-- 목적: 배치 실행 이력 테이블에 월별 파티셔닝 적용
-- 파티션 키: BATCH_DATE (YYYYMMDD 형식)
-- =====================================================

-- =====================================================
-- 1. 기존 테이블 백업 (선택사항)
-- =====================================================
-- CREATE TABLE FWK_BATCH_HIS_BACKUP AS SELECT * FROM FWK_BATCH_HIS;

-- =====================================================
-- 2. 기존 테이블 이름 변경
-- =====================================================
-- ALTER TABLE FWK_BATCH_HIS RENAME TO FWK_BATCH_HIS_OLD;

-- =====================================================
-- 3. 파티셔닝 적용 테이블 생성
-- =====================================================
-- 주의: Oracle Enterprise Edition 또는 파티셔닝 옵션이 필요합니다.

CREATE TABLE FWK_BATCH_HIS (
    BATCH_APP_ID        VARCHAR2(50)   NOT NULL,
    INSTANCE_ID         VARCHAR2(50)   NOT NULL,
    BATCH_DATE          VARCHAR2(8)    NOT NULL,  -- YYYYMMDD (파티션 키)
    BATCH_EXECUTE_SEQ   NUMBER(10)     NOT NULL,
    LOG_DTIME           VARCHAR2(17),              -- YYYYMMDDHH24MISSFF3
    BATCH_END_DTIME     VARCHAR2(17),
    RES_RT_CODE         VARCHAR2(10),
    LAST_UPDATE_USER_ID VARCHAR2(50),
    ERROR_CODE          VARCHAR2(50),
    ERROR_REASON        VARCHAR2(4000),
    RECORD_COUNT        NUMBER(10),
    EXECUTE_COUNT       NUMBER(10),
    SUCCESS_COUNT       NUMBER(10),
    FAIL_COUNT          NUMBER(10),
    CONSTRAINT PK_FWK_BATCH_HIS PRIMARY KEY (BATCH_APP_ID, INSTANCE_ID, BATCH_DATE, BATCH_EXECUTE_SEQ)
        USING INDEX LOCAL  -- LOCAL 인덱스 (파티션별 인덱스)
)
PARTITION BY RANGE (BATCH_DATE)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(
    -- 초기 파티션 (2024년 1월부터 시작)
    PARTITION P_202401 VALUES LESS THAN ('20240201'),
    PARTITION P_202402 VALUES LESS THAN ('20240301'),
    PARTITION P_202403 VALUES LESS THAN ('20240401'),
    PARTITION P_202404 VALUES LESS THAN ('20240501'),
    PARTITION P_202405 VALUES LESS THAN ('20240601'),
    PARTITION P_202406 VALUES LESS THAN ('20240701'),
    PARTITION P_202407 VALUES LESS THAN ('20240801'),
    PARTITION P_202408 VALUES LESS THAN ('20240901'),
    PARTITION P_202409 VALUES LESS THAN ('20241001'),
    PARTITION P_202410 VALUES LESS THAN ('20241101'),
    PARTITION P_202411 VALUES LESS THAN ('20241201'),
    PARTITION P_202412 VALUES LESS THAN ('20250101'),
    PARTITION P_202501 VALUES LESS THAN ('20250201'),
    PARTITION P_202502 VALUES LESS THAN ('20250301'),
    PARTITION P_202503 VALUES LESS THAN ('20250401'),
    PARTITION P_202504 VALUES LESS THAN ('20250501'),
    PARTITION P_202505 VALUES LESS THAN ('20250601'),
    PARTITION P_202506 VALUES LESS THAN ('20250701'),
    PARTITION P_202507 VALUES LESS THAN ('20250801'),
    PARTITION P_202508 VALUES LESS THAN ('20250901'),
    PARTITION P_202509 VALUES LESS THAN ('20251001'),
    PARTITION P_202510 VALUES LESS THAN ('20251101'),
    PARTITION P_202511 VALUES LESS THAN ('20251201'),
    PARTITION P_202512 VALUES LESS THAN ('20260101'),
    PARTITION P_202601 VALUES LESS THAN ('20260201')
    -- INTERVAL 설정으로 이후 파티션 자동 생성
);

-- =====================================================
-- 4. 데이터 마이그레이션 (기존 데이터가 있는 경우)
-- =====================================================
-- INSERT INTO FWK_BATCH_HIS SELECT * FROM FWK_BATCH_HIS_OLD;
-- COMMIT;

-- =====================================================
-- 5. 추가 LOCAL 인덱스 생성 (조회 성능 최적화)
-- =====================================================
-- LOCAL 인덱스: 파티션별로 분리되어 관리됨
-- 참고: BATCH_DATE, BATCH_APP_ID는 PK에 포함되어 있으나, PK 인덱스 컬럼 순서상
--       단독 조회 시 활용이 어려울 수 있어 별도 인덱스 생성을 고려할 수 있습니다.
--       단, 파티션 프루닝이 적용되므로 BATCH_DATE 단독 인덱스는 불필요할 수 있습니다.

-- CREATE INDEX IDX_BATCH_HIS_BATCHDATE ON FWK_BATCH_HIS (BATCH_DATE) LOCAL;  -- PK에 포함, 파티션 프루닝으로 대체
CREATE INDEX IDX_BATCH_HIS_LOGDTIME ON FWK_BATCH_HIS (LOG_DTIME) LOCAL;
-- CREATE INDEX IDX_BATCH_HIS_APPID ON FWK_BATCH_HIS (BATCH_APP_ID) LOCAL;    -- PK 선두 컬럼, 필요 시 활성화
CREATE INDEX IDX_BATCH_HIS_RESRTCODE ON FWK_BATCH_HIS (RES_RT_CODE) LOCAL;

-- =====================================================
-- 6. 통계 수집
-- =====================================================
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'FWK_BATCH_HIS');

-- =====================================================
-- 7. 파티션 확인
-- =====================================================
SELECT partition_name, high_value, num_rows
FROM user_tab_partitions
WHERE table_name = 'FWK_BATCH_HIS'
ORDER BY partition_name;

-- =====================================================
-- 8. 오래된 파티션 삭제 예시 (수동 실행 시)
-- =====================================================
-- 특정 파티션 삭제 (예: 2024년 1월 파티션)
-- ALTER TABLE FWK_BATCH_HIS DROP PARTITION P_202401;

-- =====================================================
-- 9. 파티션 삭제 프로시저 (선택사항)
-- =====================================================
-- 주의: 이 프로시저는 직접 파티션을 DROP하므로 신중하게 사용하세요.
-- 주의: 파티션 명명 규칙이 'P_YYYYMM' 형식에 의존합니다.
--       명명 규칙 변경 시 프로시저 수정이 필요합니다.

/*
CREATE OR REPLACE PROCEDURE SP_DROP_OLD_BATCH_HIS_PARTITIONS(
    p_retention_months IN NUMBER DEFAULT 3
) AS
    v_cutoff_date VARCHAR2(8);
BEGIN
    -- 기준일 계산 (현재 월 - 보관월수)
    v_cutoff_date := TO_CHAR(ADD_MONTHS(SYSDATE, -p_retention_months), 'YYYYMM') || '01';

    -- 삭제 대상 파티션 조회 및 삭제
    FOR rec IN (
        SELECT partition_name
        FROM user_tab_partitions
        WHERE table_name = 'FWK_BATCH_HIS'
          AND partition_name LIKE 'P_%'
          AND SUBSTR(partition_name, 3) < SUBSTR(v_cutoff_date, 1, 6)
        ORDER BY partition_name
    ) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'ALTER TABLE FWK_BATCH_HIS DROP PARTITION ' || rec.partition_name;
            DBMS_OUTPUT.PUT_LINE('Dropped partition: ' || rec.partition_name);
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Failed to drop: ' || rec.partition_name || ' - ' || SQLERRM);
        END;
    END LOOP;

    COMMIT;
END;
/
*/

-- =====================================================
-- 참고: 파티션 DROP vs DELETE 성능 비교
-- =====================================================
-- DELETE: 행 단위 삭제, UNDO 로그 생성, 느림
-- DROP PARTITION: 세그먼트 단위 삭제, 즉시, 빠름
--
-- 대용량 데이터 삭제 시 파티션 DROP이 훨씬 효율적입니다.
-- 단, 파티션 DROP은 복구가 어려우므로 백업 후 실행하세요.
-- =====================================================
