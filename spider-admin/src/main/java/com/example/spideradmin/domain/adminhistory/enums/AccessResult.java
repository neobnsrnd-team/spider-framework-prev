package com.example.spideradmin.domain.adminhistory.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>접근 결과 코드</h3>
 * <p>사용자 접근 이력의 결과 상태를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AccessResult implements BaseEnum {
    SUCCESS("success", "성공"),
    ERROR("error", "오류");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 AccessResult Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (success, error)
     * @return 해당하는 AccessResult, null이거나 매칭되지 않으면 null 반환
     */
    public static AccessResult fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AccessResult result : AccessResult.values()) {
            if (result.code.equalsIgnoreCase(code)) {
                return result;
            }
        }
        return null;
    }
}
