-- =============================================================
-- 단위 테스트용 샘플 데이터
-- schema.sql 테이블 정의에 맞춰 정리
-- =============================================================

INSERT INTO FWK_GATEWAY (GW_ID, GW_NAME, THREAD_COUNT, GW_PROPERTIES, GW_DESC, GW_APP_NAME, IO_TYPE) VALUES
('GW001', 'Gateway-1', 4, 'PROP1', 'Desc1', 'App1', 'I'),
('GW002', 'Gateway-2', 6, 'PROP2', 'Desc2', 'App2', 'O'),
('GW003', 'Gateway-3', 2, 'PROP3', 'Desc3', 'App3', 'I'),
('GW004', 'Gateway-4', 8, 'PROP4', 'Desc4', 'App4', 'O'),
('GW005', 'Gateway-5', 1, 'PROP5', 'Desc5', 'App5', 'I'),
('GW006', 'Gateway-6', 3, 'PROP6', 'Desc6', 'App6', 'O'),
('GW007', 'Gateway-7', 5, 'PROP7', 'Desc7', 'App7', 'I'),
('GW008', 'Gateway-8', 7, 'PROP8', 'Desc8', 'App8', 'O'),
('GW009', 'Gateway-9', 4, 'PROP9', 'Desc9', 'App9', 'I'),
('GW010', 'Gateway-10', 2, 'PROP10', 'Desc10', 'App10', 'O');

INSERT INTO FWK_ORG (ORG_ID, ORG_NAME, ORG_DESC, START_TIME, END_TIME, XML_ROOT_TAG) VALUES
('ORG01', 'Org One', 'Desc1', '090000', '180000', 'ROOT1'),
('ORG02', 'Org Two', 'Desc2', '090000', '180000', 'ROOT2'),
('ORG03', 'Org Three', 'Desc3', '090000', '180000', 'ROOT3'),
('ORG04', 'Org Four', 'Desc4', '090000', '180000', 'ROOT4');

INSERT INTO FWK_MESSAGE_HANDLER (ORG_ID, TRX_TYPE, IO_TYPE, OPER_MODE_TYPE, HANDLER, HANDLER_DESC, STOP_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES
('ORG01', '1', 'I', 'D', 'HandlerA', 'DescA', 'N', '20260101000000', 'tester'),
('ORG02', '2', 'O', 'R', 'HandlerB', 'DescB', 'N', '20260101000000', 'tester'),
('ORG03', '3', 'I', 'T', 'HandlerC', 'DescC', 'Y', '20260101000000', 'tester'),
('ORG04', '1', 'O', 'R', 'HandlerD', 'DescD', 'N', '20260101000000', 'tester'),
('ORG01', '2', 'I', 'D', 'HandlerE', 'DescE', 'N', '20260101000000', 'tester'),
('ORG02', '3', 'O', 'T', 'HandlerF', 'DescF', 'N', '20260101000000', 'tester');

INSERT INTO FWK_LISTENER_CONNECTOR_MAPPING (LISTENER_GW_ID, LISTENER_SYSTEM_ID, IDENTIFIER, CONNECTOR_GW_ID, CONNECTOR_SYSTEM_ID, DESCRIPTION) VALUES
('GW001', 'SYS1', 'ID1', 'GW010', 'SYS9', 'desc1'),
('GW002', 'SYS2', 'ID2', 'GW009', 'SYS8', 'desc2'),
('GW003', 'SYS3', 'ID3', 'GW008', 'SYS7', 'desc3'),
('GW004', 'SYS4', 'ID4', 'GW007', 'SYS6', 'desc4'),
('GW005', 'SYS5', 'ID5', 'GW006', 'SYS5', 'desc5'),
('GW001', 'SYS6', 'ID6', 'GW005', 'SYS4', 'desc6'),
('GW002', 'SYS7', 'ID7', 'GW004', 'SYS3', 'desc7'),
('GW003', 'SYS8', 'ID8', 'GW003', 'SYS2', 'desc8');
