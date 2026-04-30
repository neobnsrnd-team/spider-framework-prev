package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>시스템 공통 Y/N 코드</h3>
 * <p>사용 여부, 표시 여부 등 Y/N 값을 가지는 필드에 사용됩니다.</p>
 * <p>여러 도메인에서 공용으로 사용됩니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum YesNo implements BaseEnum {
    YES("Y", "예"),
    NO("N", "아니오");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 YesNo Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (Y, N)
     * @return 해당하는 YesNo
     * @throws IllegalArgumentException code가 null이거나 유효하지 않은 경우
     */
    public static YesNo fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("YesNo code는 null이거나 빈 값일 수 없습니다.");
        }
        for (YesNo yn : YesNo.values()) {
            if (yn.code.equalsIgnoreCase(code)) {
                return yn;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 YesNo code입니다: " + code + ". 유효한 값: Y(예), N(아니오)");
    }

    /**
     * Boolean 값으로부터 YesNo Enum을 반환합니다.
     * @param value Boolean 값
     * @return true면 YES, false면 NO, null이면 null
     */
    public static YesNo fromBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? YES : NO;
    }

    /**
     * YesNo를 Boolean으로 변환합니다.
     * @return YES면 true, NO면 false
     */
    public boolean toBoolean() {
        return this == YES;
    }
}
