package com.example.spideradmin.domain.property.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로퍼티 데이터 타입
 */
@Getter
@RequiredArgsConstructor
public enum PropertyDataType implements BaseEnum {
    STRING("C", "String"),
    NUMBER("N", "Number"),
    BOOLEAN("B", "Boolean");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 PropertyDataType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (C, N, B)
     * @return 해당하는 PropertyDataType, null이거나 매칭되지 않으면 null 반환
     */
    public static PropertyDataType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (PropertyDataType type : PropertyDataType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
