package com.example.spider_admin.global.common.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>사용 여부 플래그</h3>
 * <p>전역 공용 사용 여부(Y/N) 플래그입니다.</p>
 * <ul>
 *   <li>Y: 사용 (YES)</li>
 *   <li>N: 미사용 (NO)</li>
 * </ul>
 * <p>여러 Entity에서 useYn, stopYn 등의 필드에 사용됩니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UseYnFlag implements BaseEnum {

    /**
     * 사용 (YES)
     */
    YES("Y", "사용"),

    /**
     * 미사용 (NO)
     */
    NO("N", "미사용");

    /**
     * DB에 저장되는 코드 값
     */
    private final String code;

    /**
     * 코드에 대한 설명
     */
    private final String description;

    /**
     * DB 코드 값으로부터 UseYnFlag Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (Y, N)
     * @return 해당하는 UseYnFlag, null이거나 매칭되지 않으면 null 반환
     */
    public static UseYnFlag fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (UseYnFlag flag : UseYnFlag.values()) {
            if (flag.code.equalsIgnoreCase(code)) {
                return flag;
            }
        }
        return null;
    }

    /**
     * 사용 여부가 true인지 확인합니다.
     * @return YES이면 true, NO이면 false
     */
    public boolean isTrue() {
        return this == YES;
    }

    /**
     * 사용 여부가 false인지 확인합니다.
     * @return NO이면 true, YES이면 false
     */
    public boolean isFalse() {
        return this == NO;
    }
}
