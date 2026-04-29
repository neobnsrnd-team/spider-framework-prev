package com.example.spider_admin.domain.batch.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>배치 중요도 타입</h3>
 * <p>배치 작업의 중요도 수준을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ImportantType implements BaseEnum {
    HIGH("1", "높음"),
    NORMAL("2", "보통"),
    LOW("3", "낮음");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 ImportantType Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (1, 2, 3)
     * @return 해당하는 ImportantType, null이거나 매칭되지 않으면 null 반환
     */
    public static ImportantType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (ImportantType type : ImportantType.values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }
}
