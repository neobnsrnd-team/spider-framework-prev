package com.example.spider_admin.domain.transdata.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>이행 유형 코드</h3>
 * <p>이행 데이터의 유형을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum TranType implements BaseEnum {
    TRX("TRX", "거래"),
    MESSAGE("MESSAGE", "전문"),
    CODE("CODE", "코드그룹"),
    WEBAPP("WEBAPP", "웹앱"),
    SERVICE("SERVICE", "서비스"),
    COMPONENT("COMPONENT", "컴포넌트"),
    SQL("SQL", "SQL"),
    PROPERTY("PROPERTY", "프로퍼티"),
    ERROR("ERROR", "오류코드");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 TranType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값
     * @return 해당하는 TranType, null이거나 매칭되지 않으면 null 반환
     */
    public static TranType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (TranType type : TranType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
