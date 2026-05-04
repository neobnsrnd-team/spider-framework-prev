package com.example.spideradmin.domain.batch.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>배치 실행 주기</h3>
 * <p>배치 작업의 실행 주기를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum BatchCycle implements BaseEnum {
    DAILY("D", "매일"),
    MONTHLY("M", "월"),
    WEEKLY("W", "매주"),
    QUARTERLY("Q", "분기"),
    HALF_YEARLY("H", "반기"),
    YEARLY("Y", "매년"),
    OCCASIONAL("O", "수시"),
    MANUAL("P", "수동");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 BatchCycle Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (D, M, W, Q, H, Y, O, P)
     * @return 해당하는 BatchCycle, null이거나 매칭되지 않으면 null 반환
     */
    public static BatchCycle fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (BatchCycle cycle : BatchCycle.values()) {
            if (cycle.code.equalsIgnoreCase(code.trim())) {
                return cycle;
            }
        }
        return null;
    }
}
