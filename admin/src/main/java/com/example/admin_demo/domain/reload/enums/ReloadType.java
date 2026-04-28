package com.example.admin_demo.domain.reload.enums;

import com.example.admin_demo.global.common.base.BaseEnum;
import lombok.Getter;

/**
 * 운영정보 Reload 대상 항목 Enum
 * <p>WAS에 전송할 gubun 값과 화면 표시 정보를 관리합니다.</p>
 */
@Getter
public enum ReloadType implements BaseEnum {
    TRX("trx", "거래 정보", "거래정보를 Reload한다."),
    SWIFT_MESSAGE("swift_message", "swift전문 정보", "swift전문정보를 Reload한다."),
    REQUEST_APP_MAPPING("request_app_mapping", "요청처리 APP맵핑 정보", "요청처리 App맵핑정보를 Reload한다."),
    MANAGE_MONITOR("manage_monitor", "관리자 모니터링 정보", "관리자 모니터링정보를 Reload한다."),
    CODE("code", "코드 정보", "코드정보를 Reload한다."),
    BATCH("batch_reload", "배치 정보", "배치 정보를 Reload한다."),
    SERVICE("service_reload_all", "서비스 정보", "서비스 정보를 Reload한다."),
    ERROR("error", "오류 코드 정보", "오류코드 정보를 Reload한다."),
    XML_PROPERTY("xml_property", "XML Property 정보", "XML Property 정보를 Reload한다.", false),
    MESSAGE("message", "전문 정보", "전문정보를 Reload한다."),
    SQL_QUERY("sql_query", "SQL Query 정보", "SQL Query 정보를 Reload한다.", false);

    private final String code;
    private final String description;
    private final String detail;
    /** 운영정보 Reload 페이지 목록에 표시 여부 */
    private final boolean visible;

    ReloadType(String code, String description, String detail) {
        this(code, description, detail, true);
    }

    ReloadType(String code, String description, String detail, boolean visible) {
        this.code = code;
        this.description = description;
        this.detail = detail;
        this.visible = visible;
    }

    public static ReloadType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (ReloadType type : ReloadType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
