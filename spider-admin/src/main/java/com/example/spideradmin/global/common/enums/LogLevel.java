package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>로그 레벨 코드</h3>
 * <p>전문의 로그 레벨을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum LogLevel implements BaseEnum {
    HEADER_ONLY("H", "H (헤더만 로깅)"),
    ALL("A", "A (전체 로깅)"),
    NONE("N", "N (사용안함)");

    private final String code;
    private final String description;

    public static LogLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (LogLevel level : LogLevel.values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return null;
    }
}
