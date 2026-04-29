package com.example.spider_admin.domain.user.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>사용자 상태 코드</h3>
 * <p>사용자 계정의 상태를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserState implements BaseEnum {
    NORMAL("1", "정상"),
    DELETED("2", "삭제"),
    SUSPENDED("3", "정지"),
    LOCKED("4", "잠금");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 UserState Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (1, 2, 3)
     * @return 해당하는 UserState, null이거나 매칭되지 않으면 null 반환
     */
    public static UserState fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (UserState state : UserState.values()) {
            if (state.code.equalsIgnoreCase(code)) {
                return state;
            }
        }
        return null;
    }
}
