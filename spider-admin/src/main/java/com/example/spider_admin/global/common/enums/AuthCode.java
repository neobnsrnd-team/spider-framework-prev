package com.example.spider_admin.global.common.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>시스템 공통 권한 코드</h3>
 * <p>메뉴 및 기능에 대한 접근 권한을 정의합니다.</p>
 * <p>여러 도메인(user, board 등)에서 공용으로 사용됩니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuthCode implements BaseEnum {
    READ("R", "읽기"),
    WRITE("W", "쓰기/읽기");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 AuthCode Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (R, W)
     * @return 해당하는 AuthCode, null이거나 매칭되지 않으면 null 반환
     */
    public static AuthCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AuthCode auth : AuthCode.values()) {
            if (auth.code.equalsIgnoreCase(code)) {
                return auth;
            }
        }
        return null;
    }
}
