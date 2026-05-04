-- =============================================================
-- Spider-Admin MySQL DDL — 초기 데이터 INSERT 스크립트
-- =============================================================
-- 생성일: 2026-03-05
-- 01_create_tables.sql, 02_create_indexes.sql 실행 후 실행
-- =============================================================


-- =============================================================
-- 1. 역할 (FWK_ROLE)
-- =============================================================
INSERT INTO FWK_ROLE (ROLE_ID, ROLE_NAME, USE_YN, ROLE_DESC, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('ADMIN', '관리자', 'Y', '시스템 관리자', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_ROLE (ROLE_ID, ROLE_NAME, USE_YN, ROLE_DESC, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('itdev', 'IT 개발팀', 'Y', 'IT 개발팀 역할', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');


-- =============================================================
-- 2. 초기 관리자 계정 (FWK_USER)
-- =============================================================
-- 평문 비밀번호: 1q2w3e4r!@
INSERT INTO FWK_USER (USER_ID, USER_NAME, PASSWD, ROLE_ID, USER_STATE_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('admin', '관리자', 'PW는 BCrypt 인코딩 필요', 'ADMIN', '1', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_USER (USER_ID, USER_NAME, PASSWD, ROLE_ID, USER_STATE_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('cmsAdmin01', 'CMS 관리자', '$2a$10$Wb1dr5GFcbmpYC03AqKMC.8QGlMyVZkeRgFrE3khAb6y.PtXLyiG2', 'ADMIN', '1', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_USER (USER_ID, USER_NAME, PASSWD, ROLE_ID, USER_STATE_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('cmsUser01', 'CMS 제작자', '$2a$10$Wb1dr5GFcbmpYC03AqKMC.8QGlMyVZkeRgFrE3khAb6y.PtXLyiG2', 'itdev', '1', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');


-- =============================================================
-- 3. v3_ 메뉴 데이터 (FWK_MENU — 56건)
-- =============================================================

-- ROOT
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_acl_manage', NULL, 0, '*', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 1. 거래전문 관리
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_msg_manage', 'v3_acl_manage', 1, '거래전문 관리', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_msg_trx_manage', 'v3_msg_manage', 1, '거래 관리', '/transactions', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_message_manage', 'v3_msg_manage', 2, '전문 관리', '/messages', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_message_parsing_test', 'v3_msg_manage', 3, '전문로그파싱', '/message-parsing', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_message_parsing_json', 'v3_msg_manage', 4, '전문로그파싱 JSON', '/message-parsing/json', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_db_log', 'v3_msg_manage', 5, '거래추적로그조회', '/message-instances', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trans_data_popup', 'v3_msg_manage', 6, '이행데이터 생성', '/trans-data/generation', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trans_data_exec', 'v3_msg_manage', 7, '이행데이터 반영', '/trans-data', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trans_data_list', 'v3_msg_manage', 8, '이행데이터 조회', '/trans-data/files', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trx', 'v3_msg_manage', 9, '거래중지', '/transactions/trx-stop', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_req_message_test', 'v3_msg_manage', 10, '전문 테스트', '/message-tests', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_proxy_testdata', 'v3_msg_manage', 11, '당발 대응답', '/proxy-responses', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trx_validator', 'v3_msg_manage', 12, '거래 validator', '/validators', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 2. Interface 관리
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_interface_manage', 'v3_acl_manage', 2, 'Interface 관리', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_connect_org_manage', 'v3_interface_manage', 1, '연계기관', '/orgs', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_gw_manage', 'v3_interface_manage', 2, 'Gateway', '/gateways', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_org_trans_manage', 'v3_interface_manage', 3, 'Gateway맵핑', '/gw-systems', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_msg_handle_manage', 'v3_interface_manage', 4, '핸들러', '/message-handlers', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_listener_connector_manage', 'v3_interface_manage', 5, '리스너', '/listener-trxs', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_was_status', 'v3_interface_manage', 6, 'Gateway정보', '/was-gateway-statuses', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_app_mapping_manage', 'v3_interface_manage', 7, 'App맵핑', '/transports', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_was_status_monitor', 'v3_interface_manage', 8, 'Gateway모니터링', '/was-status-monitors', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 3. Code 관리
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cd_manage', 'v3_acl_manage', 3, 'Code 관리', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_neb_code_group_manage', 'v3_cd_manage', 1, '코드그룹', '/code-groups', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_code_manage', 'v3_cd_manage', 2, '코드', '/codes', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_code_mapping_manage', 'v3_cd_manage', 3, '코드맵핑', '/codes/org-codes', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 4. 운영 정보
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_opt_manage', 'v3_acl_manage', 4, '운영 정보', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_system_oper_manage', 'v3_opt_manage', 1, 'Reload', '/reload', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_property_db_manage', 'v3_opt_manage', 2, '프로퍼티 DB', '/properties', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_xml_property_manage', 'v3_opt_manage', 3, 'XML Property', '/xml-properties', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 5. 배치
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_batch_manage', 'v3_acl_manage', 5, '배치', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_batch_app_manage', 'v3_batch_manage', 1, '배치 App', '/batches/apps', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_batch_his_list', 'v3_batch_manage', 2, '수행내역', '/batches/history', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 6. 오류
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_error_manage', 'v3_acl_manage', 6, '오류', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_error_cause_his', 'v3_error_manage', 1, '오류현황', '/error-histories', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_error_code', 'v3_error_manage', 2, '오류코드', '/error-codes', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 7. WAS
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_was_manage', 'v3_acl_manage', 7, 'WAS', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_was_group_manage', 'v3_was_manage', 1, 'WAS 그룹', '/was-groups', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_was_instance', 'v3_was_manage', 2, 'WAS 인스턴스', '/was-instances', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 8. 관리자 모니터링
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_admin_monitor', 'v3_acl_manage', 8, '관리자 모니터링', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_system_biz_reg', 'v3_admin_monitor', 1, '현황판', '/monitors', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_access_user_id', 'v3_admin_monitor', 2, '중지거래 접근허용자', '/stop-transaction-accessors', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_trx_stop_his', 'v3_admin_monitor', 3, '거래중지이력', '/admin-histories/trx-stop-history', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_user_page_log', 'v3_admin_monitor', 4, '작업이력 로그', '/admin-histories', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 9. 사용자관리
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_fwk_user_manage', 'v3_acl_manage', 9, '사용자관리', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_user_manage', 'v3_fwk_user_manage', 1, '사용자', '/users', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_menu_manage', 'v3_fwk_user_manage', 2, '메뉴', '/menus', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_role_manage', 'v3_fwk_user_manage', 3, '역할', '/roles', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_my_info_manage', 'v3_fwk_user_manage', 4, '개인정보', '/users/profile', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- 10. 게시판
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_BOARD', 'v3_acl_manage', 10, '게시판', NULL, 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_board_manage', 'v3_BOARD', 1, '게시판 관리', '/boards', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_BOARD_AUTH', 'v3_BOARD', 2, '게시판 권한', '/boards/auth', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_FWKB_NOTICE_BOARD', 'v3_BOARD', 3, '공지사항', '/articles/notice-board', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_FWKB_TEST_BOARD', 'v3_BOARD', 4, '게시판', '/articles/test-board', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');


-- =============================================================
-- 3-1. 게시판 초기 데이터 (FWK_BOARD, FWK_BOARD_AUTH)
-- =============================================================
INSERT INTO FWK_BOARD (BOARD_ID, BOARD_NAME, BOARD_TYPE, ADMIN_ID, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('notice-board', '공지사항', 'N', 'admin', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_BOARD (BOARD_ID, BOARD_NAME, BOARD_TYPE, ADMIN_ID, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('test-board', '게시판', 'F', 'admin', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_BOARD_AUTH (USER_ID, BOARD_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('admin', 'notice-board', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_BOARD_AUTH (USER_ID, BOARD_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('admin', 'test-board', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

-- =============================================================
-- 4. admin 사용자에 전체 메뉴 WRITE 권한 부여 (FWK_USER_MENU)
-- =============================================================
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_acl_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_msg_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_msg_trx_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_message_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_message_parsing_test', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_message_parsing_json', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_db_log', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trans_data_popup', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trans_data_exec', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trans_data_list', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trx', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_req_message_test', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_proxy_testdata', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trx_validator', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_interface_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_connect_org_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_gw_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_org_trans_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_msg_handle_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_listener_connector_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_was_status', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_app_mapping_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_was_status_monitor', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cd_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_neb_code_group_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_code_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_code_mapping_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_opt_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_system_oper_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_property_db_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_xml_property_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_batch_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_batch_app_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_batch_his_list', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_error_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_error_cause_his', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_error_code', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_was_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_was_group_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_was_instance', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_admin_monitor', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_system_biz_reg', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_access_user_id', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_trx_stop_his', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_user_page_log', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_fwk_user_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_user_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_menu_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_role_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_my_info_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_BOARD', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_board_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_BOARD_AUTH', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_FWKB_NOTICE_BOARD', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_FWKB_TEST_BOARD', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');


-- =============================================================
-- 5. ADMIN 역할에 전체 메뉴 WRITE 권한 부여 (FWK_ROLE_MENU)
-- =============================================================
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_acl_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_msg_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_msg_trx_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_message_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_message_parsing_test', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_message_parsing_json', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_db_log', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trans_data_popup', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trans_data_exec', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trans_data_list', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_req_message_test', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_proxy_testdata', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx_validator', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_interface_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_connect_org_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_gw_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_org_trans_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_msg_handle_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_listener_connector_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_status', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_app_mapping_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_status_monitor', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cd_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_neb_code_group_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_code_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_code_mapping_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_opt_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_system_oper_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_property_db_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_xml_property_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_batch_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_batch_app_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_batch_his_list', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_error_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_error_cause_his', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_error_code', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_group_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_instance', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_admin_monitor', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_system_biz_reg', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_access_user_id', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx_stop_his', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_user_page_log', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_fwk_user_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_user_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_menu_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_role_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_my_info_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_BOARD', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_board_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_BOARD_AUTH', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_FWKB_NOTICE_BOARD', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_FWKB_TEST_BOARD', 'W');

-- CMS extension menus
INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_manage', 'v3_acl_manage', 12, 'CMS', '/cms', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_dashboard', 'v3_cms_manage', 1, 'CMS 작업자 대시보드', '/cms/dashboard', 'N', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_approve', 'v3_cms_manage', 2, 'CMS 관리자 승인 관리', '/cms/approve', 'N', 'N', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_edit', 'v3_cms_manage', 3, 'CMS 편집', '/cms/edit', 'N', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_files', 'v3_cms_manage', 4, 'CMS 파일 관리', '/cms/files', 'N', 'N', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_pages', 'v3_cms_manage', 10, 'CMS 페이지 관리', '/cms-admin/pages', 'N', 'N', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_approvals', 'v3_cms_manage', 11, 'CMS 승인 관리', '/cms-admin/approvals', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_files', 'v3_cms_manage', 12, 'CMS 리소스 검토', '/cms-admin/files', 'N', 'N', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_ab_tests', 'v3_cms_manage', 13, 'CMS A/B 관리', '/cms-admin/ab-tests', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_deployments', 'v3_cms_manage', 14, 'CMS 배포 관리', '/cms-admin/deployments', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_statistics', 'v3_cms_manage', 15, 'CMS 통계', '/cms-admin/statistics', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_MENU (MENU_ID, PRIOR_MENU_ID, SORT_ORDER, MENU_NAME, MENU_URL, DISPLAY_YN, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('v3_cms_admin_components', 'v3_cms_manage', 16, 'CMS 컴포넌트 관리', '/cms-admin/components', 'Y', 'Y', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_admin_approvals', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_admin_ab_tests', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_admin_deployments', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_admin_statistics', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_cms_admin_components', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('itdev', 'v3_cms_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('itdev', 'v3_cms_dashboard', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('itdev', 'v3_cms_edit', 'W');

INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_admin_approvals', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_admin_ab_tests', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_admin_deployments', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_admin_statistics', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('admin', 'v3_cms_admin_components', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_acl_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_admin_approvals', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_admin_ab_tests', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_admin_deployments', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_admin_statistics', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsAdmin01', 'v3_cms_admin_components', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsUser01', 'v3_cms_manage', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsUser01', 'v3_cms_dashboard', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) VALUES ('cmsUser01', 'v3_cms_edit', 'W', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), 'system');

COMMIT;
