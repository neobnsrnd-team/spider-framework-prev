package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import com.example.spideradmin.global.exception.InvalidInputException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>다중 응답 타입 코드</h3>
 * <p>전문의 다중 응답 처리 방식을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MultiResType implements BaseEnum {
    NONE("0", "없음"),
    SINGLE("1", "1개"),
    MULTIPLE("M", "여러개");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 MultiResType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (0, 1, M)
     * @return 해당하는 MultiResType, null인 경우 null 반환
     * @throws InvalidInputException 유효하지 않은 코드 값인 경우
     */
    public static MultiResType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (MultiResType type : MultiResType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new InvalidInputException("multiResTypeCode: " + code);
    }
}
