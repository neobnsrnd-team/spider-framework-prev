-- =============================================================
-- Spider-Admin Oracle DDL — 인덱스 생성 스크립트
-- =============================================================
-- 생성일: 2026-03-05
-- PK 인덱스는 CREATE TABLE에서 자동 생성되므로 여기서 제외
-- 비즈니스 조회용 인덱스만 포함
-- =============================================================


-- -------------------------------------------------------------
-- FWK_USER_ACCESS_HIS: 접근 이력 조회용
-- -------------------------------------------------------------
CREATE INDEX IDX_USER_ACC_HIS_DTIME ON FWK_USER_ACCESS_HIS (ACCESS_DTIME, USER_ID);
CREATE INDEX IDX_USER_ACC_HIS_USER  ON FWK_USER_ACCESS_HIS (USER_ID, ACCESS_DTIME);


-- -------------------------------------------------------------
-- FWK_CUST_MENU_APP: URL + 앱 복합 유니크
-- -------------------------------------------------------------
CREATE UNIQUE INDEX UK_CUST_MENU_APP_URL_WEBAPP ON FWK_CUST_MENU_APP (MENU_URL, WEB_APP_ID);


-- -------------------------------------------------------------
-- FWK_BATCH_HIS: 배치 이력 조회용 (LOCAL 인덱스)
-- 참고: 파티셔닝 미적용 환경에서는 LOCAL 키워드를 제거하고 실행
-- 파티셔닝 DDL: docs/sql/FWK_BATCH_HIS_PARTITION.sql 참조
-- -------------------------------------------------------------
-- CREATE INDEX IDX_BATCH_HIS_LOGDTIME   ON FWK_BATCH_HIS (LOG_DTIME)    LOCAL;
-- CREATE INDEX IDX_BATCH_HIS_RESRTCODE  ON FWK_BATCH_HIS (RES_RT_CODE)  LOCAL;

-- 파티셔닝 미적용 환경용 (일반 인덱스)
CREATE INDEX IDX_BATCH_HIS_LOGDTIME  ON FWK_BATCH_HIS (LOG_DTIME);
CREATE INDEX IDX_BATCH_HIS_RESRTCODE ON FWK_BATCH_HIS (RES_RT_CODE);
