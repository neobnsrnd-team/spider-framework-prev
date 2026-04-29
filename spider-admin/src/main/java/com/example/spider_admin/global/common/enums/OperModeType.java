package com.example.spider_admin.global.common.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import com.example.spider_admin.global.exception.InvalidInputException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>운영 모드 타입 코드</h3>
 * <p>운영 환경의 모드를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OperModeType implements BaseEnum {
    TEST("T", "테스트서버"),
    REAL("R", "운영서버"),
    DEV("D", "개발서버");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 OperModeType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (T, R, D)
     * @return 해당하는 OperModeType, null인 경우 null 반환
     * @throws InvalidInputException 유효하지 않은 코드 값인 경우
     */
    public static OperModeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (OperModeType type : OperModeType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new InvalidInputException("operModeTypeCode: " + code);
    }
}
