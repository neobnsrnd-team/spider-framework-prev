-- =============================================================
-- Spider-Admin Oracle DDL — FWK_PROPERTY 배포 관리 행 방식 전환
-- =============================================================
-- 생성일: 2026-04-20
-- 01_create_tables.sql ~ 03_insert_initial_data.sql 실행 후 실행
-- ※ 아래 쿼리는 모두 개발자가 DB에서 직접 실행해야 합니다.
-- =============================================================

-- =============================================================
-- 긴급공지 관리 (emergency-notice-manage)
-- FWK_PROPERTY 배포 관리: 컬럼 추가 방식 → 행 추가(key-value) 방식으로 전환
-- FWK_PROPERTY는 key-value store 패턴 테이블이므로 새 속성은 컬럼이 아닌 행으로 관리한다.
-- =============================================================

-- 1단계: 기존 컬럼 방식 롤백 — ALTER TABLE로 추가했던 3개 컬럼 삭제
ALTER TABLE FWK_PROPERTY DROP COLUMN START_DTIME;
ALTER TABLE FWK_PROPERTY DROP COLUMN END_DTIME;
ALTER TABLE FWK_PROPERTY DROP COLUMN DEPLOY_STATUS;

-- 2단계: 배포 관리 속성을 'notice' 그룹의 행으로 추가
INSERT INTO FWK_PROPERTY (PROPERTY_GROUP_ID, PROPERTY_ID, PROPERTY_NAME, PROPERTY_DESC, VALID_DATA, DEFAULT_VALUE, LAST_UPDATE_USER_ID, LAST_UPDATE_DTIME)
VALUES ('notice', 'DEPLOY_STATUS', '배포상태', 'DRAFT(미배포) / DEPLOYED(배포 중) / ENDED(배포 종료)', 'DRAFT, DEPLOYED, ENDED', 'DRAFT', 'Admin', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'));

INSERT INTO FWK_PROPERTY (PROPERTY_GROUP_ID, PROPERTY_ID, PROPERTY_NAME, PROPERTY_DESC, DEFAULT_VALUE, LAST_UPDATE_USER_ID, LAST_UPDATE_DTIME)
VALUES ('notice', 'START_DTIME', '배포시작일시', '배포 시작 일시 (yyyyMMddHHmmss)', NULL, 'Admin', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'));

INSERT INTO FWK_PROPERTY (PROPERTY_GROUP_ID, PROPERTY_ID, PROPERTY_NAME, PROPERTY_DESC, DEFAULT_VALUE, LAST_UPDATE_USER_ID, LAST_UPDATE_DTIME)
VALUES ('notice', 'END_DTIME', '배포종료일시', '배포 종료 일시 (yyyyMMddHHmmss)', NULL, 'Admin', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'));

-- =============================================================
-- SPW_CMS_PAGE — 실제 DB 상태 반영 ALTER
-- =============================================================
-- 생성일: 2026-04-20
-- ※ 쿼리 수행은 개발자가 DB에서 직접 수행해야 합니다.
-- ※ 04_ab_test.sql 의 AB_GROUP_ID / AB_WEIGHT ALTER는 01_create_tables.sql 에
--    이미 포함되어 있으므로 실행하지 않아도 됩니다.
-- =============================================================

-- ① 컬럼 크기 변경 (VARCHAR2 길이 확장)
ALTER TABLE SPW_CMS_PAGE MODIFY (
    CREATE_USER_ID           VARCHAR2(100),   -- VARCHAR2(20) → 100
    CREATE_USER_NAME         VARCHAR2(200),   -- VARCHAR2(100) → 200
    LAST_MODIFIER_ID         VARCHAR2(100),   -- VARCHAR2(20) → 100
    LAST_MODIFIER_NAME       VARCHAR2(200),   -- VARCHAR2(100) → 200
    APPROVER_ID              VARCHAR2(100),   -- VARCHAR2(20) → 100
    APPROVER_NAME            VARCHAR2(200),   -- VARCHAR2(100) → 200
    FINAL_APPROVAL_STATE     VARCHAR2(20),    -- VARCHAR2(1) → 20
    FINAL_APPROVAL_USER_ID   VARCHAR2(100),   -- VARCHAR2(20) → 100
    FINAL_APPROVAL_USER_NAME VARCHAR2(200)    -- VARCHAR2(100) → 200
);

-- ② 날짜형 변경
ALTER TABLE SPW_CMS_PAGE MODIFY (
    CREATE_DATE          TIMESTAMP(6),  -- DATE → TIMESTAMP(6)
    FINAL_APPROVAL_DTIME TIMESTAMP(6)   -- VARCHAR2(14) → TIMESTAMP(6)
);

-- ③ VARCHAR2 → CLOB 변환
--    ※ 컬럼에 기존 데이터가 있을 경우 Oracle 12c 이상 환경에서만 직접 변환 가능합니다.
ALTER TABLE SPW_CMS_PAGE MODIFY (PAGE_DESC        CLOB);  -- VARCHAR2(500)
ALTER TABLE SPW_CMS_PAGE MODIFY (PAGE_DESC_DETAIL CLOB);  -- VARCHAR2(2000)
ALTER TABLE SPW_CMS_PAGE MODIFY (USER_GUIDE       CLOB);  -- VARCHAR2(500)
ALTER TABLE SPW_CMS_PAGE MODIFY (REJECTED_REASON  CLOB);  -- VARCHAR2(2000)

-- ④ VARCHAR2(1) → CHAR(1) 변환 및 NOT NULL 제약 추가
--    ※ IS_PUBLIC 에 NULL 행이 있으면 먼저 UPDATE SPW_CMS_PAGE SET IS_PUBLIC='N' WHERE IS_PUBLIC IS NULL; 실행
ALTER TABLE SPW_CMS_PAGE MODIFY (
    USE_YN    CHAR(1)              NOT NULL,  -- VARCHAR2(1) → CHAR(1), NOT NULL 유지
    IS_PUBLIC CHAR(1) DEFAULT 'N'  NOT NULL   -- VARCHAR2(1) nullable → CHAR(1) NOT NULL
);

-- ⑤ 기존 컬럼 NOT NULL 추가
--    ※ NULL 데이터가 있으면 UPDATE 로 채운 뒤 실행하세요.
--      PAGE_NAME : UPDATE SPW_CMS_PAGE SET PAGE_NAME=PAGE_ID WHERE PAGE_NAME IS NULL;
--      VIEW_MODE : UPDATE SPW_CMS_PAGE SET VIEW_MODE='responsive' WHERE VIEW_MODE IS NULL;
ALTER TABLE SPW_CMS_PAGE MODIFY (PAGE_NAME VARCHAR2(200) NOT NULL);
ALTER TABLE SPW_CMS_PAGE MODIFY (VIEW_MODE VARCHAR2(20)  NOT NULL);

-- ⑥ CHECK 제약 추가
ALTER TABLE SPW_CMS_PAGE ADD CONSTRAINT CHK_SPW_PAGE_APPROVE_STATE
    CHECK (APPROVE_STATE IN ('WORK', 'PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE SPW_CMS_PAGE ADD CONSTRAINT CHK_SPW_PAGE_USE_YN
    CHECK (USE_YN IN ('Y', 'N'));

ALTER TABLE SPW_CMS_PAGE ADD CONSTRAINT CHK_SPW_PAGE_IS_PUBLIC
    CHECK (IS_PUBLIC IN ('Y', 'N'));

ALTER TABLE SPW_CMS_PAGE ADD CONSTRAINT CHK_SPW_PAGE_VIEW_MODE
    CHECK (VIEW_MODE IN ('mobile', 'web', 'responsive'));

-- ⑦ 신규 컬럼 추가
ALTER TABLE SPW_CMS_PAGE ADD (
    TARGET_CD VARCHAR2(50)  -- 대상 코드
);

-- PAGE_TYPE: NOT NULL 컬럼 — DEFAULT 'PAGE' 로 기존 행을 초기화하면서 추가
ALTER TABLE SPW_CMS_PAGE ADD (
    PAGE_TYPE VARCHAR2(20) DEFAULT 'PAGE' NOT NULL
);

ALTER TABLE SPW_CMS_PAGE ADD CONSTRAINT CHK_SPW_PAGE_TYPE
    CHECK (PAGE_TYPE IN ('PAGE', 'TEMPLATE', 'REACT'));

-- =============================================================
-- bizApp — POC_USER 비밀번호 BCrypt 마이그레이션
-- 추가일: 2026-04-23
-- 주의: 개발자가 DB에서 직접 실행해야 한다
-- BCrypt 강도(strength): 10, 해시 길이: 60자
--
-- [순서]
-- 1. PASSWORD 컬럼 크기를 60자로 확장 (BCrypt 해시 길이 수용)
-- 2. BCrypt 해시 생성: new BCryptPasswordEncoder().encode("test12!") 실행 후 출력값 복사
-- 3. 아래 UPDATE 문의 '<BCrypt hash of test12!>' 를 실제 해시값으로 교체 후 실행
-- =============================================================

-- Step 1: PASSWORD 컬럼 크기 확장 (VARCHAR2(20) → VARCHAR2(60))
ALTER TABLE D_SPIDERLINK.POC_USER MODIFY PASSWORD VARCHAR2(60);

-- Step 2: 평문 비밀번호를 BCrypt 해시로 교체
-- BCryptPasswordEncoder(strength=10).encode("test12!") 생성값
UPDATE D_SPIDERLINK.POC_USER
SET PASSWORD = '$2a$10$fTvJ5wu/HMA8gkiVez/M/um83ToooOeUG2fhmZVfRCTgABTHZFLIu'
WHERE PASSWORD = 'test12!';

-- =============================================================
-- CMS 배포 서버 인스턴스 포트 변경: 3001(Next.js 직접) → 8080(nginx)
-- =============================================================
-- 생성일: 2026-04-22
-- ※ 아래 쿼리는 개발자가 DB에서 직접 실행해야 합니다.
-- =============================================================

-- 미리보기 URL이 http://{ip}:{INSTANCE_PORT}/cms/deployed/{pageId}.html 형태로 구성되므로
-- INSTANCE_PORT를 8080으로 변경해야 nginx를 통해 정적 파일에 접근할 수 있다.
UPDATE FWK_CMS_SERVER_INSTANCE
SET INSTANCE_PORT = 8080,
    INSTANCE_DESC = '운영 배포 서버 (133.186.135.23:8080)'
WHERE INSTANCE_ID = 'prod-operation-01';

COMMIT;

-- =============================================================
-- #147 CMS 배포 관리 — 만료수동처리 기능 추가
-- ⚠ 개발자가 DB에서 직접 실행해야 합니다.
-- =============================================================

-- SPW_CMS_PAGE.BEGINNING_DATE / EXPIRED_DATE / IS_PUBLIC / FILE_PATH_BACK 컬럼은
-- 01_create_tables.sql 에 이미 정의되어 있으므로 별도 ALTER 불필요.
-- FWK_CMS_FILE_SEND_HIS 의 만료 전용 이력 조회 예시 (운영 확인용):
--   SELECT * FROM FWK_CMS_FILE_SEND_HIS
--   WHERE FILE_ID LIKE '%_expired.html'
--   ORDER BY LAST_MODIFIED_DTIME DESC;

-- =============================================================
-- FWK_SQL_QUERY_HIS — 실제 DB 테이블 확인 결과
-- =============================================================
-- 확인일: 2026-04-22
-- FWK_SQL_QUERY_HIS 테이블은 구버전부터 이미 존재하는 테이블입니다.
-- PK: (VERSION_ID VARCHAR2(50), QUERY_ID VARCHAR2(50))
-- 신버전에서는 VERSION_ID = System.currentTimeMillis() 문자열 사용
-- ※ 잘못 생성된 시퀀스를 아래 쿼리로 제거하세요 (개발자 직접 실행):
DROP SEQUENCE SEQ_FWK_SQL_QUERY_HIS;

-- =============================================================
-- #148 FWK_MESSAGE_INSTANCE — 컬럼 크기 일괄 수정
-- ⚠ 개발자가 DB에서 직접 실행해야 합니다.
-- =============================================================
-- ORG_ID         VARCHAR2(10) → VARCHAR2(20) : "biz-transfer"(12자) 등 appName 수용
-- REQ_RES_TYPE   VARCHAR2(1)  → VARCHAR2(3)  : "REQ"/"RES" 3자 수용
-- TRX_TRACKING_NO VARCHAR2(30) → VARCHAR2(40) : UUID(36자) 수용, TRX_ID와 통일
-- INSTANCE_ID    VARCHAR2(4)  → VARCHAR2(20) : "biz-transfer:19200"(16자) 수용
ALTER TABLE FWK_MESSAGE_INSTANCE MODIFY (
    ORG_ID          VARCHAR2(20),
    REQ_RES_TYPE    VARCHAR2(3),
    TRX_TRACKING_NO VARCHAR2(40),
    INSTANCE_ID     VARCHAR2(20)
);

COMMIT;

-- =============================================================
-- FWK_SQL_QUERY_HIS 컬럼 크기 확장
-- =============================================================
-- 배경: 이력 저장 시 QUERY_NAME(50), QUERY_DESC(200) 초과 데이터 유실 방지
--       메인 테이블(FWK_SQL_QUERY) 기준으로 컬럼 크기를 일치시킴
-- ※ 개발자가 DB에서 직접 실행해야 합니다.
ALTER TABLE FWK_SQL_QUERY_HIS MODIFY (QUERY_NAME VARCHAR2(200));
ALTER TABLE FWK_SQL_QUERY_HIS MODIFY (QUERY_DESC VARCHAR2(500));

COMMIT;

-- =============================================================
-- #150 MetaDrivenCommandHandler Biz 타입('B') 데모 컴포넌트 데이터
-- =============================================================
-- 배경: MetaDrivenCommandHandler 에 Biz 클래스 리플렉션 호출(COMPONENT_TYPE='B') 추가.
--       외부시스템 TCP 호출이 필요한 서비스 스텝은 아래와 같이 FWK_COMPONENT 에 등록한다.
-- ⚠ 개발자가 DB에서 직접 실행해야 합니다.
--
-- COMPONENT_TYPE 값 정리:
--   S = SELECT (MyBatis selectOne)
--   U = UPDATE/INSERT/DELETE (MyBatis update, auto-commit)
--   B = Biz 클래스 리플렉션 호출 (COMPONENT_CLASS_NAME = 스프링 빈 클래스 전체 경로)
--
-- TcpCallBiz 사용 시 접속 대상은 spider-link application.yml 의 tcp.ext.host / tcp.ext.port 로 설정.

-- 외부 인증AP(biz-auth) 로그인 TCP 호출 컴포넌트 예시
INSERT INTO FWK_COMPONENT (
    COMPONENT_ID, COMPONENT_NAME, COMPONENT_DESC,
    COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME,
    USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID
) VALUES (
    'EXT_TCP_AUTH_LOGIN',
    '외부 인증AP 로그인 TCP 호출',
    'TcpCallBiz 를 통해 biz-auth(포트 19100)로 AUTH_LOGIN 커맨드 전송',
    'B',
    'com.example.spiderlink.infra.tcp.biz.TcpCallBiz',
    'AUTH_LOGIN',
    'Y',
    TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),
    'Admin'
);

COMMIT;

-- =============================================================
-- #169 FWK_MESSAGE_INSTANCE TRX_TRACKING_NO / MESSAGE_SNO 컬럼 길이 확장
-- =============================================================
-- 배경: MessageInstanceRecorder 가 requestId(UUID, 36자)를 TRX_TRACKING_NO 에 그대로 저장하는데
--       컬럼이 VARCHAR2(30) 으로 정의되어 있어 6자리가 잘려서 저장됨.
--       → REQ/RES 거래 체인 추적 오류 및 Admin 거래추적로그조회 불일치 발생 가능.
--       MESSAGE_SNO 는 현재 시퀀스(NEXTVAL) 사용으로 즉각 문제는 없으나 향후 UUID 전환 대비 함께 확장.
-- ⚠ 개발자가 DB에서 직접 실행해야 합니다.
ALTER TABLE FWK_MESSAGE_INSTANCE MODIFY TRX_TRACKING_NO VARCHAR2(36);
ALTER TABLE FWK_MESSAGE_INSTANCE MODIFY MESSAGE_SNO     VARCHAR2(36);

COMMIT;

-- =============================================================
-- #237 MetaDrivenCommandHandler 전면 전환 — biz-auth / biz-transfer FWK 등록
-- =============================================================
-- 배경: biz-auth / biz-transfer 에서 하드코딩 핸들러 제거 후
--       MetaDrivenCommandHandler + TcpCallBiz B-타입 컴포넌트로 대체.
--       TcpCallBiz 는 tcp.ext.host/port(=mock-core:19300)로 TCP 전문 중계.
-- ⚠ 개발자가 DB에서 직접 실행해야 합니다.
-- 실행 순서: 1→2→3→4→5→6 순서 준수 (FK 의존성)
-- =============================================================

-- -----------------------------------------------------------------
-- (선택) 기존 DEMO_* 데드데이터 정리 — 필요 시 실행
-- FWK_LISTENER_TRX_MESSAGE 의 DEMO_AUTH_LOGIN / DEMO_AUTH_ME / DEMO_PAYABLE_AMT 는
-- MetaDrivenCommandHandler 가 활성화되면서 불필요해진 구형 등록값.
-- 삭제 시 FK 순서 준수: RELATION_PARAM → SERVICE_RELATION → SERVICE → LISTENER_TRX_MESSAGE
-- -----------------------------------------------------------------
-- DELETE FROM FWK_RELATION_PARAM   WHERE SERVICE_ID IN ('SVC_AUTH_LOGIN','SVC_AUTH_ME','SVC_PAYABLE_AMT');
-- DELETE FROM FWK_SERVICE_RELATION  WHERE SERVICE_ID IN ('SVC_AUTH_LOGIN','SVC_AUTH_ME','SVC_PAYABLE_AMT');
-- DELETE FROM FWK_SERVICE           WHERE SERVICE_ID IN ('SVC_AUTH_LOGIN','SVC_AUTH_ME','SVC_PAYABLE_AMT');
-- DELETE FROM FWK_LISTENER_TRX_MESSAGE WHERE GW_ID='DEMO_GW' AND REQ_ID_CODE IN ('DEMO_AUTH_LOGIN','DEMO_AUTH_ME','DEMO_PAYABLE_AMT');
-- DELETE FROM FWK_COMPONENT WHERE COMPONENT_ID = 'EXT_TCP_AUTH_LOGIN';
-- COMMIT;

-- -----------------------------------------------------------------
-- 1. FWK_COMPONENT: B-타입 TcpCallBiz 컴포넌트 정의
--    COMPONENT_CLASS_NAME = TcpCallBiz 스프링 빈 전체 경로
--    COMPONENT_METHOD_NAME = mock-core 로 전송할 TCP 커맨드명
-- -----------------------------------------------------------------
INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_USER_AUTH',    'mock-core 사용자 인증 TCP 호출',          'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_USER_AUTH',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_USER_QUERY',   'mock-core 사용자 조회 TCP 호출',           'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_USER_QUERY',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_CARD_LIST',    'mock-core 카드목록 조회 TCP 호출',         'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_CARD_LIST',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 'mock-core 이용내역 조회 TCP 호출',         'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_TRANSACTIONS',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_PAYMENT_STMT', 'mock-core 이용대금명세서 조회 TCP 호출',   'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_PAYMENT_STMT',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

INSERT INTO FWK_COMPONENT (COMPONENT_ID, COMPONENT_NAME, COMPONENT_TYPE, COMPONENT_CLASS_NAME, COMPONENT_METHOD_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('BIZ_TCP_CORE_PAYABLE_AMT',  'mock-core 즉시결제가능금액 조회 TCP 호출', 'B',
        'com.example.spiderlink.infra.tcp.biz.TcpCallBiz', 'CORE_PAYABLE_AMT',
        'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

-- -----------------------------------------------------------------
-- 2. FWK_COMPONENT_PARAM: 각 컴포넌트가 받는 파라미터 키 정의
--    PARAM_KEY = TcpCallBiz 가 mock-core로 전송하는 payload 필드명
-- -----------------------------------------------------------------
-- AUTH_LOGIN → CORE_USER_AUTH
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_USER_AUTH', 1, 'userId');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_USER_AUTH', 2, 'password');

-- AUTH_ME → CORE_USER_QUERY
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_USER_QUERY', 1, 'userId');

-- TRANSFER_CARD_LIST → CORE_CARD_LIST
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_CARD_LIST', 1, 'userId');

-- TRANSFER_TRANSACTIONS → CORE_TRANSACTIONS (userId 필수, 나머지 선택)
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 1, 'userId');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 2, 'cardId');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 3, 'fromDate');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 4, 'toDate');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_TRANSACTIONS', 5, 'usageType');

-- TRANSFER_PAYMENT_STMT → CORE_PAYMENT_STMT (userId 필수, 나머지 선택)
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_PAYMENT_STMT', 1, 'userId');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_PAYMENT_STMT', 2, 'yearMonth');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_PAYMENT_STMT', 3, 'paymentDay');

-- TRANSFER_PAYABLE_AMT → CORE_PAYABLE_AMT
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_PAYABLE_AMT', 1, 'userId');
INSERT INTO FWK_COMPONENT_PARAM (COMPONENT_ID, PARAM_SEQ_NO, PARAM_KEY) VALUES ('BIZ_TCP_CORE_PAYABLE_AMT', 2, 'cardId');

COMMIT;

-- -----------------------------------------------------------------
-- 3. FWK_LISTENER_TRX_MESSAGE: 커맨드 → TRX_ID 매핑 (GW_ID=DEMO_GW)
--    REQ_ID_CODE = biz-channel 이 TCP 로 전송하는 실제 커맨드명
-- -----------------------------------------------------------------
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'AUTH_LOGIN',           'AUTH_LOGIN',           'DEMO', 'I');
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'AUTH_ME',              'AUTH_ME',              'DEMO', 'I');
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'TRANSFER_CARD_LIST',   'TRANSFER_CARD_LIST',   'DEMO', 'I');
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'TRANSFER_TRANSACTIONS','TRANSFER_TRANSACTIONS', 'DEMO', 'I');
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'TRANSFER_PAYMENT_STMT','TRANSFER_PAYMENT_STMT', 'DEMO', 'I');
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('DEMO_GW', 'TRANSFER_PAYABLE_AMT', 'TRANSFER_PAYABLE_AMT',  'DEMO', 'I');

COMMIT;

-- -----------------------------------------------------------------
-- 4. FWK_SERVICE: 거래별 서비스 정의 (TRX_ID → SERVICE_ID)
-- -----------------------------------------------------------------
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_AUTH_LOGIN',    '인증AP 로그인 → mock-core TCP',              'AUTH_LOGIN',           'DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_AUTH_ME',       '인증AP 사용자 조회 → mock-core TCP',          'AUTH_ME',              'DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_CARD_LIST',     '이체AP 카드목록 조회 → mock-core TCP',        'TRANSFER_CARD_LIST',   'DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_TRANSACTIONS',  '이체AP 이용내역 조회 → mock-core TCP',        'TRANSFER_TRANSACTIONS','DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_PAYMENT_STMT',  '이체AP 이용대금명세서 조회 → mock-core TCP',  'TRANSFER_PAYMENT_STMT','DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');
INSERT INTO FWK_SERVICE (SERVICE_ID, SERVICE_NAME, TRX_ID, ORG_ID, IO_TYPE, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('SVC_BIZ_PAYABLE_AMT',   '이체AP 즉시결제가능금액 조회 → mock-core TCP','TRANSFER_PAYABLE_AMT', 'DEMO', 'I', 'Y', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), 'Admin');

COMMIT;

-- -----------------------------------------------------------------
-- 5. FWK_SERVICE_RELATION: 서비스 → 컴포넌트 실행 단계 (단일 B-타입 스텝)
-- -----------------------------------------------------------------
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_AUTH_LOGIN',   1, 'BIZ_TCP_CORE_USER_AUTH');
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_AUTH_ME',      1, 'BIZ_TCP_CORE_USER_QUERY');
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_CARD_LIST',    1, 'BIZ_TCP_CORE_CARD_LIST');
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS');
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_PAYMENT_STMT', 1, 'BIZ_TCP_CORE_PAYMENT_STMT');
INSERT INTO FWK_SERVICE_RELATION (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID) VALUES ('SVC_BIZ_PAYABLE_AMT',  1, 'BIZ_TCP_CORE_PAYABLE_AMT');

COMMIT;

-- -----------------------------------------------------------------
-- 6. FWK_RELATION_PARAM: 인입 payload 키 → TcpCallBiz 파라미터 바인딩
--    PARAM_VALUE = 수신 payload(컨텍스트)에서 꺼낼 키 이름
--    선택 파라미터가 payload에 없으면 "" 로 전달 → mock-core 에서 null 처리
-- -----------------------------------------------------------------
-- AUTH_LOGIN
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_AUTH_LOGIN', 1, 'BIZ_TCP_CORE_USER_AUTH', 1, 'userId');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_AUTH_LOGIN', 1, 'BIZ_TCP_CORE_USER_AUTH', 2, 'password');

-- AUTH_ME
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_AUTH_ME', 1, 'BIZ_TCP_CORE_USER_QUERY', 1, 'userId');

-- TRANSFER_CARD_LIST
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_CARD_LIST', 1, 'BIZ_TCP_CORE_CARD_LIST', 1, 'userId');

-- TRANSFER_TRANSACTIONS
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS', 1, 'userId');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS', 2, 'cardId');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS', 3, 'fromDate');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS', 4, 'toDate');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_TRANSACTIONS', 1, 'BIZ_TCP_CORE_TRANSACTIONS', 5, 'usageType');

-- TRANSFER_PAYMENT_STMT
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_PAYMENT_STMT', 1, 'BIZ_TCP_CORE_PAYMENT_STMT', 1, 'userId');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_PAYMENT_STMT', 1, 'BIZ_TCP_CORE_PAYMENT_STMT', 2, 'yearMonth');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_PAYMENT_STMT', 1, 'BIZ_TCP_CORE_PAYMENT_STMT', 3, 'paymentDay');

-- TRANSFER_PAYABLE_AMT
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_PAYABLE_AMT', 1, 'BIZ_TCP_CORE_PAYABLE_AMT', 1, 'userId');
INSERT INTO FWK_RELATION_PARAM (SERVICE_ID, SERVICE_SEQ_NO, COMPONENT_ID, PARAM_SEQ_NO, PARAM_VALUE)
VALUES ('SVC_BIZ_PAYABLE_AMT', 1, 'BIZ_TCP_CORE_PAYABLE_AMT', 2, 'cardId');

COMMIT;
