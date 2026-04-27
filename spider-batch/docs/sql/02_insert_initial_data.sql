-- =============================================================
-- batch-was 연동을 위한 Admin DB 초기 데이터
-- Admin DB(D_SPIDERLINK 스키마)에서 실행
-- ※ 쿼리 실행은 개발자가 DB에서 직접 수행해야 함
-- =============================================================

-- 1. Batch WAS 인스턴스 등록
--    FWK_WAS_INSTANCE: Admin이 배치 실행 요청을 보낼 대상 WAS
INSERT INTO FWK_WAS_INSTANCE (
    INSTANCE_ID, INSTANCE_NAME, INSTANCE_DESC,
    INSTANCE_TYPE, IP, PORT, OPER_MODE_TYPE
) VALUES (
    'BT01',                    -- BT(Batch) + 01 / application.yml의 batch.was.instance-id와 일치
    'Batch WAS',
    'Spring Batch 실행 WAS (POC)',
    '2',                       -- 2: AP (1:WEB, 2:AP, 3:통합)
    '127.0.0.1',
    '8081',                    -- batch-was 포트 (※ #41 TCP 전환 시 소켓 포트로 변경 예정)
    'D'                        -- D: 개발 서버 (D:개발, R:운영, T:테스트)
);

-- 2. 샘플 배치 Job 등록
--    BATCH_APP_FILE_NAME = Spring Batch JobRegistry Bean 이름

-- File2DBJob: poc-users.csv → POC_USER 적재
INSERT INTO FWK_BATCH_APP (
    BATCH_APP_ID, BATCH_APP_NAME, BATCH_APP_FILE_NAME,
    BATCH_APP_DESC, BATCH_CYCLE, RETRYABLE_YN, IMPORTANT_TYPE,
    LAST_UPDATE_USER_ID
) VALUES (
    'FILE2DB_JOB',
    'File2DB 배치',
    'file2db',                 -- File2DbJobConfig @Bean(name="file2db")
    'CSV 파일 → POC_USER 적재 (FlatFileItemReader → JdbcBatchItemWriter)',
    'O',                       -- O: 수시
    'Y',
    '2',                       -- 2: 중
    'SYSTEM'
);

-- DB2DBJob: POC_카드사용내역 → POC_카드사용내역_백업 (이용일자 파티셔닝 + 병렬처리)
INSERT INTO FWK_BATCH_APP (
    BATCH_APP_ID, BATCH_APP_NAME, BATCH_APP_FILE_NAME,
    BATCH_APP_DESC, BATCH_CYCLE, RETRYABLE_YN, IMPORTANT_TYPE,
    LAST_UPDATE_USER_ID
) VALUES (
    'DB2DB_JOB',
    'DB2DB 배치',
    'db2db',                   -- Db2DbJobConfig @Bean(name="db2db")
    'POC_카드사용내역 → POC_카드사용내역_백업 아카이브 (JdbcPagingItemReader + ColumnRangePartitioner 병렬처리)',
    'O',
    'Y',
    '2',
    'SYSTEM'
);

-- DB2ForeignJob: POC_카드사용내역 → 외부 시스템 HTTP 전문 연계
INSERT INTO FWK_BATCH_APP (
    BATCH_APP_ID, BATCH_APP_NAME, BATCH_APP_FILE_NAME,
    BATCH_APP_DESC, BATCH_CYCLE, RETRYABLE_YN, IMPORTANT_TYPE,
    LAST_UPDATE_USER_ID
) VALUES (
    'DB2FOREIGN_JOB',
    'DB2Foreign 배치',
    'db2foreign',              -- Db2ForeignJobConfig @Bean(name="db2foreign")
    'POC_카드사용내역 → 외부 시스템 HTTP 전문 연계 (JdbcPagingItemReader → RestTemplate)',
    'O',
    'Y',
    '2',
    'SYSTEM'
);

-- 3. 배치-인스턴스 매핑 등록
--    BT01 인스턴스에서 3개 Job 모두 실행 가능
INSERT INTO FWK_WAS_EXEC_BATCH (BATCH_APP_ID, INSTANCE_ID, USE_YN, LAST_UPDATE_USER_ID)
VALUES ('FILE2DB_JOB', 'BT01', 'Y', 'SYSTEM');

INSERT INTO FWK_WAS_EXEC_BATCH (BATCH_APP_ID, INSTANCE_ID, USE_YN, LAST_UPDATE_USER_ID)
VALUES ('DB2DB_JOB', 'BT01', 'Y', 'SYSTEM');

INSERT INTO FWK_WAS_EXEC_BATCH (BATCH_APP_ID, INSTANCE_ID, USE_YN, LAST_UPDATE_USER_ID)
VALUES ('DB2FOREIGN_JOB', 'BT01', 'Y', 'SYSTEM');

-- ============================================================
-- #63: 고정 길이 파일 처리 Job 등록
-- 개발자가 직접 DB에서 실행해야 함
-- ============================================================
INSERT INTO FWK_BATCH_APP (BATCH_APP_ID, BATCH_APP_NAME, BATCH_APP_FILE_NAME, BATCH_APP_DESC,
                            BATCH_CYCLE, RETRYABLE_YN, PER_WAS_YN, IMPORTANT_TYPE,
                            LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
SELECT 'FIXED_FILE2DB_JOB', '고정길이파일적재', 'fixedFile2db',
       '금융 거래내역 고정 길이 전문(61자) → POC_고정길이거래 테이블 적재',
       'O', 'Y', 'Y', '2',
       TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM FWK_BATCH_APP WHERE BATCH_APP_ID = 'FIXED_FILE2DB_JOB');

INSERT INTO FWK_WAS_EXEC_BATCH (INSTANCE_ID, BATCH_APP_ID, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
SELECT 'BT01', 'FIXED_FILE2DB_JOB', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM FWK_WAS_EXEC_BATCH WHERE INSTANCE_ID = 'BT01' AND BATCH_APP_ID = 'FIXED_FILE2DB_JOB');

COMMIT;
