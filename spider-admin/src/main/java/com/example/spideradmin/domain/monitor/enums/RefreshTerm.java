package com.example.spideradmin.domain.monitor.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>모니터 새로고침 주기 코드</h3>
 * <p>모니터 현황판의 자동 새로고침 주기를 정의합니다.</p>
 * <p>분(minute) 단위로 저장됩니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum RefreshTerm implements BaseEnum {
    NONE("", "선택"),
    ONE_MINUTE("1", "1분"),
    FIVE_MINUTES("5", "5분"),
    TEN_MINUTES("10", "10분"),
    THIRTY_MINUTES("30", "30분"),
    ONE_HOUR("60", "1시간"),
    THREE_HOURS("180", "3시간"),
    SIX_HOURS("360", "6시간");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 RefreshTerm Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (분 단위 문자열)
     * @return 해당하는 RefreshTerm, null이거나 매칭되지 않으면 null 반환
     */
    public static RefreshTerm fromCode(String code) {
        if (code == null) {
            return null;
        }
        // 빈 문자열은 NONE으로 처리
        if (code.isBlank()) {
            return NONE;
        }
        for (RefreshTerm term : RefreshTerm.values()) {
            if (term.code.equalsIgnoreCase(code)) {
                return term;
            }
        }
        return null;
    }

    /**
     * 분 단위 정수 값으로부터 RefreshTerm Enum을 찾습니다.
     * @param minutes 분 단위 정수 값
     * @return 해당하는 RefreshTerm, 매칭되지 않으면 null 반환
     */
    public static RefreshTerm fromMinutes(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return NONE;
        }
        return fromCode(String.valueOf(minutes));
    }

    /**
     * 분 단위 정수 값을 반환합니다.
     * @return 분 단위 정수 값, NONE이면 0
     */
    public int getMinutes() {
        return code.isEmpty() ? 0 : Integer.parseInt(code);
    }
}
