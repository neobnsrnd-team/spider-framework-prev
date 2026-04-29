package com.example.spider_admin.domain.transdata.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>이행 결과 코드</h3>
 * <p>이행 데이터의 처리 결과를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum TranResult implements BaseEnum {
    SUCCESS("S", "성공"),
    FAILURE("F", "실패");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 TranResult Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (S, F)
     * @return 해당하는 TranResult, null이거나 매칭되지 않으면 null 반환
     */
    public static TranResult fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (TranResult result : TranResult.values()) {
            if (result.code.equalsIgnoreCase(code)) {
                return result;
            }
        }
        return null;
    }
}
