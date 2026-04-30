package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>구분유형 코드</h3>
 * <p>중지거래 접근허용자의 구분 유형을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DistinctionType implements BaseEnum {
    ALL("", "전체"),
    TRANSACTION("T", "거래"),
    SERVICE("S", "서비스");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 DistinctionType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값
     * @return 해당하는 DistinctionType
     * @throws IllegalArgumentException code가 null이거나 유효하지 않은 경우
     */
    public static DistinctionType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("DistinctionType code는 null일 수 없습니다.");
        }
        if (code.isBlank()) {
            return ALL;
        }
        for (DistinctionType type : DistinctionType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 DistinctionType code입니다: " + code + ". 유효한 값: T(거래), S(서비스)");
    }
}
