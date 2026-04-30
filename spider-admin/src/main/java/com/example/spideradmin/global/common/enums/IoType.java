package com.example.spideradmin.global.common.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import com.example.spideradmin.global.exception.InvalidInputException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>I/O 타입 코드</h3>
 * <p>전문의 입출력 타입을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum IoType implements BaseEnum {
    REQUEST_IN("I", "수동 (REQUEST-IN)"),
    REQUEST_OUT("O", "기동 (REQUEST-OUT)"),
    REQUEST("Q", "요구 (REQUEST)"),
    RESPONSE("S", "응답 (RESPONSE)");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 IoType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (I, O, Q, S)
     * @return 해당하는 IoType, null인 경우 null 반환
     * @throws InvalidInputException 유효하지 않은 코드 값인 경우
     */
    public static IoType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (IoType type : IoType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new InvalidInputException("ioTypeCode: " + code);
    }
}
