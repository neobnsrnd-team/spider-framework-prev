package com.example.spideradmin.domain.errorcode.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 오류 레벨 Enum
 * 오류의 심각도를 나타내는 열거형
 */
@Getter
@RequiredArgsConstructor
public enum ErrorLevel implements BaseEnum {
    SAFE("1", "안전"),
    CAUTION("2", "주의"),
    WARNING("3", "경계");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 ErrorLevel Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (1, 2, 3)
     * @return 해당하는 ErrorLevel, null이거나 매칭되지 않으면 null 반환
     */
    public static ErrorLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (ErrorLevel level : ErrorLevel.values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }

    /**
     * 코드로 설명 조회
     * @param code DB에 저장된 코드 값
     * @return 코드에 해당하는 설명, 없으면 코드 그대로 반환
     */
    public static String getDescriptionByCode(String code) {
        ErrorLevel level = fromCode(code);
        return level != null ? level.description : code;
    }
}
