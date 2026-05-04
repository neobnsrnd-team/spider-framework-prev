/**
 * UI text labels used in E2E tests — Single source of truth.
 *
 * When i18n is applied, update this file to match the test locale.
 * All test files import from here instead of hardcoding display text.
 */

export const LABEL = {
    // Common actions
    SEARCH: '조회',
    REGISTER: '등록',
    SAVE: '저장',
    DELETE: '삭제',
    CLOSE: '닫기',
    EXCEL: '엑셀',
    PRINT: '출력',
    RESET: '초기화',
    MENU: '메뉴',

    // User page
    USER_CREATE_TITLE: '사용자 등록',
    USER_DETAIL_TITLE: '사용자 상세',
    USER_ID_COLUMN: '사용자ID',

    // Monitor page
    MONITOR_CREATE_TITLE: '현황판 등록',
    MONITOR_EDIT_TITLE: '현황판 수정',
    MONITOR_DETAIL_TITLE: '현황판 상세',
    MONITOR_ID_COLUMN: '모니터ID',
    MONITOR_NAME_COLUMN: '모니터명',

    // Batch App page
    BATCH_APP_CREATE_TITLE: '배치 APP 등록',
    BATCH_APP_EDIT_TITLE: '배치 APP 수정',
    BATCH_APP_ID_COLUMN: '배치 APP ID',

    // User menu modal
    MENU_PERMISSION_TITLE: '사용자 메뉴 권한 관리',
    AVAILABLE_MENU_LABEL: '할당 가능 메뉴',
    ASSIGNED_MENU_LABEL: '할당된 메뉴',
    NO_AVAILABLE_MENUS: '추가 가능한 메뉴가 없습니다',

    // Article page
    ARTICLE_CREATE_TITLE: '게시글 등록',
    ARTICLE_DETAIL_TITLE: '게시글 상세',
    ARTICLE_EDIT_TITLE: '게시글 수정',
    NEW_ARTICLE: '새 글 작성',
    TITLE_COLUMN: '제목',
    AUTHOR_COLUMN: '작성자',
    VIEWS_COLUMN: '조회수',
    DATE_COLUMN: '작성일',
    EDIT: '수정',
    NO_DATA: '조회된 데이터가 없습니다',

    // Board manage page
    BOARD_CREATE_TITLE: '게시판 등록',
    BOARD_EDIT_TITLE: '게시판 수정',

    // Board auth page
    BOARD_AUTH_CREATE_TITLE: '권한 부여',
    BOARD_AUTH_EDIT_TITLE: '권한 수정',

    // App mapping page
    APP_MAPPING_CREATE_TITLE: '요청처리 App 맵핑관리 등록',
    APP_MAPPING_DETAIL_TITLE: '요청처리 App 맵핑관리 상세조회',

    // Error code page
    ERROR_CREATE_TITLE: '오류코드 등록',
    ERROR_EDIT_TITLE: '오류코드 수정',
    ERROR_CODE_COLUMN: '오류코드',
    ERROR_TITLE_COLUMN: '오류제목',
    HANDLER_ADD: '추가',
    HANDLER_REMOVE: '제거',
    SAVE_CHANGES: '변경사항저장',

    // Code manage page
    CODE_CREATE_TITLE: '코드 등록',
    CODE_EDIT_TITLE: '코드 수정',
    CODE_GROUP_COLUMN: '코드그룹',
    CODE_COLUMN: '코드',
    CODE_NAME_COLUMN: '코드명',

    // Code group manage page
    CODE_GROUP_CREATE_TITLE: '코드 그룹 추가',
    CODE_GROUP_EDIT_TITLE: '코드 그룹 수정',
    CODE_GROUP_ID_COLUMN: '코드그룹ID',
    CODE_GROUP_NAME_COLUMN: '코드그룹명',
    CODE_GROUP_DELETE_ALL: '전체삭제',
    CODE_GROUP_ADD_ROW: '행 추가',
    CODE_GROUP_DELETE_SELECTED: '선택행 삭제',
    ADD: '+ 추가',

    // WAS Instance page
    WAS_INSTANCE_ID_COLUMN: '인스턴스ID',
    WAS_INSTANCE_NAME_COLUMN: '인스턴스명',

    // Menu manage page
    MENU_CREATE_TITLE: '메뉴 등록',
    MENU_EDIT_TITLE: '메뉴 수정',
    MENU_ID_COLUMN: '메뉴ID',
    MENU_NAME_COLUMN: '메뉴명',

    // Role manage page
    ROLE_ID_COLUMN: '권한ID',
    ROLE_NAME_COLUMN: '권한명',
    ROLE_ADD_ROW: '행 추가',
    ROLE_DELETE_ROW: '선택 행 삭제',
    ROLE_SAVE_CHANGES: '변경사항 저장',
    ROLE_MENU_PERMISSION: '메뉴 권한',

    // Validator page
    VALIDATOR_CREATE_TITLE: 'Validator 등록',
    VALIDATOR_DETAIL_TITLE: 'Validator 상세',
    VALIDATOR_ID_COLUMN: 'Validator ID',
    VALIDATOR_NAME_COLUMN: 'Validator 명',

    // Validation page
    VALIDATION_CREATE_TITLE: 'Validator 컴포넌트 등록',
    VALIDATION_DETAIL_TITLE: 'Validator 컴포넌트 상세',
    VALIDATION_ID_COLUMN: '키워드',

    // WAS Group page
    WAS_GROUP_ID_COLUMN: 'WAS 그룹 ID',
    WAS_GROUP_NAME_COLUMN: 'WAS 그룹명',

    // Property DB page
    PROPERTY_GROUP_ID_COLUMN: '프로퍼티그룹ID',
    PROPERTY_GROUP_NAME_COLUMN: '프로퍼티그룹명',
    PROPERTY_GROUP_CREATE_TITLE: '신규 프로퍼티 그룹 생성',
    PROPERTY_GROUP_DETAIL_TITLE: '프로퍼티 상세보기',

    // XML Property page
    XML_PROPERTY_FILE_COLUMN: 'Property 파일',
    XML_PROPERTY_CREATE_TITLE: 'XML Property 파일 등록',

    // Profile (개인정보수정) page
    PROFILE_TITLE: '상세정보',
    PROFILE_SAVE: '저장',

    // Trans data popup (이행데이터 생성) page
    TRANS_DATA_SEARCH: '조회',
    TRANS_DATA_EXECUTE: '이행실행',
    TRANS_DATA_EXECUTE_CONFIRM_TITLE: '이행 실행 확인',
    TRANS_DATA_EXECUTE_BTN: '이행 실행',
    TRANS_DATA_DELETE: '삭제',
    TRANS_DATA_TAB_TRX: '거래',
    TRANS_DATA_TAB_MESSAGE: '메세지',
    TRANS_DATA_TAB_CODE: '코드그룹',
    TRANS_DATA_TAB_WEBAPP: 'WebApp',
    TRANS_DATA_TAB_ERROR: '오류코드',
    TRANS_DATA_TAB_SERVICE: 'Service',
    TRANS_DATA_TAB_COMPONENT: 'Component',
    TRANS_DATA_TAB_PROPERTY: 'Property',
    TRANS_DATA_NO_DATA: '조회된 데이터가 없습니다.',

    // Trans data exec page (이행데이터 반영)
    TRANS_EXEC_DETAIL_TITLE: '이행 상세 정보',

    // Trans data list page (이행데이터 조회)
    TRANS_LIST_FILE_PREVIEW_TITLE: '파일 미리보기',

    // Admin action log page (관리자 작업이력 로그)
    ADMIN_ACTION_LOG_DETAIL_TITLE: '관리자 작업이력 상세',

    // Connect org manage page (연계기관 관리)
    ORG_ADD_ROW: '행 추가',
    ORG_DELETE_SELECTED: '선택행 삭제',
    ORG_SAVE_CHANGES: '변경사항 저장',
    ORG_GATEWAY_BTN: 'GATEWAY',
    ORG_HANDLER_BTN: '핸들러',
    ORG_GATEWAY_MODAL_TITLE: '기관통신 GateWay 맵핑관리',
    ORG_HANDLER_MODAL_TITLE: '전문처리 핸들러 관리',

    // Org trans manage page (기관통신 Gateway 맵핑 관리)
    GW_MAPPING_CREATE: '게이트웨이 생성',
    GW_MAPPING_SAVE: '변경사항 저장',
    GW_MAPPING_DELETE: '선택 삭제',
    GW_MAPPING_ADD_ROW: '행 추가',
    GW_MAPPING_DELETE_SELECTED: '선택행 삭제',
    GW_MAPPING_NO_DATA: '조회된 데이터가 없습니다.',

    // Listener connector manage page (리스너 응답커넥터 맵핑 관리)
    LCM_REGISTER: '등록',
    LCM_MODAL_TITLE_CREATE: '리스너-커넥터 매핑 등록',
    LCM_MODAL_TITLE_EDIT: '리스너-커넥터 매핑 수정',

    // CRUD 상태 텍스트 (인라인 편집 테이블 공통)
    CRUD_CREATE: '추가',
    CRUD_UPDATE: '수정',
    CRUD_DELETE: '삭제',

    // Proxy testdata (당발 대응답 관리) page
    PROXY_TESTDATA_REGISTER_TITLE: '당발 대응답 등록',
    PROXY_TESTDATA_DETAIL_TITLE: '당발 대응답 상세',
    PROXY_SETTING_TITLE: '대응답 설정',
    PROXY_SETTING: '설정',
    PROXY_TRX_SEARCH: '거래조회',
    SAVE_AS_NEW: '신규저장',
    UPDATE: '수정',
    CANCEL: '취소',

    // Gateway manage page
    GW_MANAGE_SAVE: '변경사항 저장',
    GW_ID_COLUMN: 'GATEWAY',
    GW_MANAGE_ADD_ROW: '행 추가',
    GW_MANAGE_DELETE_SELECTED: '선택행 삭제',
    GW_ORG_VIEW: '기관보기',

    // SQL Query manage page (SQL Query 관리)
    SQL_QUERY_CREATE_TITLE: 'SQL Query 등록',
    SQL_QUERY_DETAIL_TITLE: 'SQL Query 상세',
    SQL_QUERY_ID_COLUMN: 'Query ID',
    SQL_QUERY_NAME_COLUMN: 'Query 명',

    // DataSource manage page (데이터소스 관리)
    DS_CREATE_TITLE: '데이터소스 등록',
    DS_DETAIL_TITLE: '데이터소스 상세',
    DS_ID_COLUMN: 'DB ID',
    DS_NAME_COLUMN: 'DB 명',

    // WAS별 Gateway 모니터링 (was_status_monitor) page
    WAS_MONITOR_INSTANCE_COL: '인스턴스 명',
    WAS_MONITOR_GW_COL: 'G/W 명',
    WAS_MONITOR_STATUS_COL: '상태',
    WAS_MONITOR_TEST_BTN: '테스트 실행',
    WAS_MONITOR_COUNTDOWN_SUFFIX: '후 자동 새로고침',

    // Message manage page (전문 관리)
    MSG_CREATE_TITLE: '전문 등록',
    MSG_DETAIL_TITLE: '전문 관리',
    MSG_ID_COLUMN: '전문ID',
    MSG_NAME_COLUMN: '전문명',
    MSG_ORG_COLUMN: '기관명',
    MSG_RELOAD: 'Reload',
    MSG_EXCEL_UPLOAD: '엑셀 UPLOAD',
    MSG_EXCEL_UPLOAD_TITLE: '전문 엑셀 업로드',
    MSG_RELOAD_TITLE: 'Reload 대상 WAS 선택',

    // Message handler page (전문처리핸들러)
    HANDLER_ADD_ROW: '행 추가',
    HANDLER_DELETE_SELECTED: '선택행 삭제',
    HANDLER_SAVE_CHANGES: '변경사항 저장',
    HANDLER_RELOAD_TO_WAS: 'Reload to WAS',
    HANDLER_ORG_COLUMN: '기관명',
    HANDLER_TRX_TYPE_COLUMN: '거래유형',
    HANDLER_COLUMN: '전문처리핸들러',

    // Message parsing page (전문로그파싱)
    PARSE_BUTTON: '전문파싱',
    PARSE_ORG_LABEL: '기관명',
    PARSE_RESULT_TITLE: '파싱결과',

    // Access user manage page (중지거래 접근허용자 관리)
    ACCESS_USER_ADD_ROW: '행 추가',
    ACCESS_USER_DELETE_SELECTED: '선택행 삭제',
    ACCESS_USER_SAVE_CHANGES: '변경사항 저장',
    ACCESS_USER_RELOAD: 'Reload',
    ACCESS_USER_RELOAD_MODAL_TITLE: '접근허용자 정보 Reload',

    // Message test page (전문 테스트)
    MSG_TEST_TITLE: '전문 테스트',
    MSG_TEST_TRX_SEARCH: '거래조회',
    MSG_TEST_SAVE_TESTCASE: '테스트 케이스 저장',
    MSG_TEST_LOAD_TESTCASE: '테스트 케이스 선택',
    MSG_TEST_SIMULATION: '시뮬레이션',

    // Log level manage page
    LOG_LEVEL_SAVE: '저장',
    LOG_LEVEL_REFRESH: '새로고침',
    LOG_LEVEL_NO_DATA: '조회된 데이터가 없습니다',
} as const;
