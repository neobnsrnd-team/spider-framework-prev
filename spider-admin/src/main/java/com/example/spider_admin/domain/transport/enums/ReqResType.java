package com.example.spider_admin.domain.transport.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>요구/응답 타입 코드</h3>
 * <p>FWK_TRANSPORT 테이블의 REQ_RES_TYPE 컬럼 값을 정의합니다.</p>
 * <ul>
 *   <li>Q: 요구 (REQUEST)</li>
 *   <li>S: 응답 (RESPONSE)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ReqResType implements BaseEnum {
    REQUEST("Q", "요구 (REQUEST)"),
    RESPONSE("S", "응답 (RESPONSE)");

    private final String code;
    private final String description;

    public static ReqResType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (ReqResType type : ReqResType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
