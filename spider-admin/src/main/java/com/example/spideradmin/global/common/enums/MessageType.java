package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>전문 유형 코드</h3>
 * <p>전문의 데이터 형식을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MessageType implements BaseEnum {
    JSON("J", "Json"),
    XML("X", "XML"),
    FIXED_LENGTH("F", "Fixed Length"),
    ISO8583("I", "ISO8583"),
    CSV_FILE("C", "CSV_FILE"),
    DELIMITER("D", "Delimiter");

    private final String code;
    private final String description;

    public static MessageType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (MessageType type : MessageType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
