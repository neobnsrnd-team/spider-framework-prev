-- =============================================================================
-- feat/#296: 헤더 오프셋 파싱 기반 멀티 프로토콜 지원 및 FWK_GATEWAY 동적 생성
-- 작성자: 서민국 / 2026-05-06
--
-- [주의] 이 SQL은 개발자가 DB에서 직접 실행해야 합니다.
--        GatewayLoader 활성화(spider.gateway.dynamic.enabled=true) 전 반드시 실행하세요.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- [1] FWK_GATEWAY — GW_PROPERTIES에 포트/코덱/헤더 전문ID 추가
--
-- GW_PROPERTIES 형식: key=value;key=value
--   port          : TCP 수신 포트
--   codec         : JSON (현재 지원)
--   pool-size     : 핸들러 스레드 풀 크기
--   queue         : 요청 대기 큐 크기
--   header-msg-id : 헤더 오프셋 파싱에 사용할 FWK_MESSAGE.MESSAGE_ID (HEADER_YN='Y')
--   org-id        : 기관 ID (FWK_MESSAGE 조회 키)
-- -----------------------------------------------------------------------------

-- 기존 DEMO_GW 업데이트 (standalone 데모용, 포트 9995)
UPDATE FWK_GATEWAY
   SET GW_PROPERTIES = 'port=9995;codec=JSON;pool-size=5;queue=20;header-msg-id=DEMO_GW_HEADER;org-id=DEMO'
 WHERE GW_ID = 'DEMO_GW';

-- biz-auth 전용 게이트웨이 (포트 19100)
INSERT INTO FWK_GATEWAY (GW_ID, GW_NAME, IO_TYPE, GW_PROPERTIES)
VALUES (
    'BIZ_AUTH_GW',
    'biz-auth 인증AP 게이트웨이',
    'I',
    'port=19100;codec=JSON;pool-size=5;queue=20;header-msg-id=DEMO_GW_HEADER;org-id=DEMO'
);

-- biz-transfer 전용 게이트웨이 (포트 19200)
INSERT INTO FWK_GATEWAY (GW_ID, GW_NAME, IO_TYPE, GW_PROPERTIES)
VALUES (
    'BIZ_TRANSFER_GW',
    'biz-transfer 이체AP 게이트웨이',
    'I',
    'port=19200;codec=JSON;pool-size=10;queue=50;header-msg-id=DEMO_GW_HEADER;org-id=DEMO'
);

COMMIT;

-- -----------------------------------------------------------------------------
-- [1-B] FWK_GATEWAY — BankingHeaderMessageCodec 사용 예시 (실제 뱅킹 클라이언트 연동 시)
--
-- header-length      : 헤더 고정 길이 (byte). 이 값이 존재하면 BankingHeaderMessageCodec 선택
-- length-field-offset: 헤더 내 길이 필드 시작 위치 (0-based byte offset)
-- length-field-length: 길이 필드 크기 (ASCII 문자 수, 예: 8 → "00000256")
-- is-total-length    : true면 길이 필드가 전체(헤더+바디) 길이, false면 바디만의 길이
--
-- 아래는 예시 쿼리이며 실제 적용 시 기관 전문 규격에 맞게 값을 조정해야 합니다.
-- [주의] 기존 POC 데모 환경(4byte prefix)은 header-length 파라미터가 없으므로 영향 없음
-- -----------------------------------------------------------------------------

-- 실제 뱅킹용 BIZ_AUTH_GW 예시 (header-length 추가 시 BankingHeaderMessageCodec 자동 선택)
-- UPDATE FWK_GATEWAY
--    SET GW_PROPERTIES = 'port=19100;pool-size=5;queue=20;header-length=56;length-field-offset=0;length-field-length=8;is-total-length=false;header-msg-id=DEMO_GW_HEADER;org-id=DEMO'
--  WHERE GW_ID = 'BIZ_AUTH_GW';

-- 실제 뱅킹용 BIZ_TRANSFER_GW 예시
-- UPDATE FWK_GATEWAY
--    SET GW_PROPERTIES = 'port=19200;pool-size=10;queue=50;header-length=56;length-field-offset=0;length-field-length=8;is-total-length=false;header-msg-id=DEMO_GW_HEADER;org-id=DEMO'
--  WHERE GW_ID = 'BIZ_TRANSFER_GW';

-- -----------------------------------------------------------------------------
-- [2] FWK_MESSAGE — 헤더 전문 정의 (HEADER_YN='Y')
--
-- DEMO_GW_HEADER: 모든 Demo 게이트웨이가 공통으로 사용하는 고정길이 헤더 구조.
--   4byte 바이너리 길이 프리픽스(codec 처리) 이후부터의 헤더 필드를 정의한다.
-- -----------------------------------------------------------------------------
INSERT INTO FWK_MESSAGE (
    ORG_ID, MESSAGE_ID, MESSAGE_NAME, MESSAGE_TYPE,
    HEADER_YN, VALIDATION_USE_YN, PRE_LOAD_YN, LOCK_YN, TRX_TYPE
)
VALUES (
    'DEMO',
    'DEMO_GW_HEADER',
    'Demo 게이트웨이 공통 헤더',
    'F',       -- F=고정길이
    'Y',       -- 헤더 전문 식별자
    'N', 'N', 'N', '1'
);

COMMIT;

-- -----------------------------------------------------------------------------
-- [3] FWK_MESSAGE_FIELD — 헤더 필드 정의 (오프셋 기반 파싱 기준)
--
-- 4byte 바이너리 길이 프리픽스 이후 byte[] 기준:
--   offset  0 ~ 19 : REQ_ID_CODE (20byte, C타입) — 라우팅 키, FWK_LISTENER_TRX_MESSAGE 조회
--   offset 20 ~ 55 : REQUEST_ID  (36byte, C타입) — 요청 고유번호 (UUID)
-- 헤더 총합: 56byte
-- -----------------------------------------------------------------------------

-- 필드 1: 거래코드 — 라우팅의 핵심 키
INSERT INTO FWK_MESSAGE_FIELD (
    ORG_ID, MESSAGE_ID, MESSAGE_FIELD_ID, MESSAGE_FIELD_NAME,
    SORT_ORDER, DATA_TYPE, DATA_LENGTH, REQUIRED_YN, LOG_YN
)
VALUES (
    'DEMO', 'DEMO_GW_HEADER', 'REQ_ID_CODE', '거래코드',
    1, 'C', 20, 'Y', 'Y'
);

-- 필드 2: 요청 고유번호 (UUID 36자리)
INSERT INTO FWK_MESSAGE_FIELD (
    ORG_ID, MESSAGE_ID, MESSAGE_FIELD_ID, MESSAGE_FIELD_NAME,
    SORT_ORDER, DATA_TYPE, DATA_LENGTH, REQUIRED_YN, LOG_YN
)
VALUES (
    'DEMO', 'DEMO_GW_HEADER', 'REQUEST_ID', '요청고유번호',
    2, 'C', 36, 'Y', 'Y'
);

COMMIT;

-- -----------------------------------------------------------------------------
-- [4] FWK_LISTENER_TRX_MESSAGE — BIZ_AUTH_GW, BIZ_TRANSFER_GW 커맨드 매핑 추가
--
-- REQ_ID_CODE = biz-channel 이 TCP 로 전송하는 실제 커맨드명 (BizCommands 상수값)
-- TRX_ID      = FWK_TRX + FWK_SERVICE 가 등록된 거래 ID (04_alter_tables.sql 기준)
-- MetaDrivenCommandHandler 가 spider.gateway.id 값을 GW_ID 로 사용하므로
-- biz-auth 는 BIZ_AUTH_GW, biz-transfer 는 BIZ_TRANSFER_GW 로 등록한다.
-- -----------------------------------------------------------------------------

-- BIZ_AUTH_GW 커맨드 매핑 (biz-channel → biz-auth)
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_AUTH_GW', 'AUTH_LOGIN', 'AUTH_LOGIN', 'DEMO', 'I');

INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_AUTH_GW', 'AUTH_ME',    'AUTH_ME',    'DEMO', 'I');

-- BIZ_TRANSFER_GW 커맨드 매핑 (biz-channel → biz-transfer)
INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_TRANSFER_GW', 'TRANSFER_CARD_LIST',    'TRANSFER_CARD_LIST',    'DEMO', 'I');

INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_TRANSFER_GW', 'TRANSFER_TRANSACTIONS', 'TRANSFER_TRANSACTIONS', 'DEMO', 'I');

INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_TRANSFER_GW', 'TRANSFER_PAYMENT_STMT', 'TRANSFER_PAYMENT_STMT', 'DEMO', 'I');

INSERT INTO FWK_LISTENER_TRX_MESSAGE (GW_ID, REQ_ID_CODE, TRX_ID, ORG_ID, IO_TYPE)
VALUES ('BIZ_TRANSFER_GW', 'TRANSFER_PAYABLE_AMT',  'TRANSFER_PAYABLE_AMT',  'DEMO', 'I');

COMMIT;
