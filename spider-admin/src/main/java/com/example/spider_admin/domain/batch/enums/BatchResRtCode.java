package com.example.spider_admin.domain.batch.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>배치 실행 결과 상태 코드</h3>
 * <p>배치 작업의 실행 상태를 정의합니다.</p>
 *
 * <h4>상태 흐름:</h4>
 * <pre>
 * STARTED (시작됨) → SUCCESS (정상 종료) / UNKNOWN_COUNT (대상건수 알수없음)
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum BatchResRtCode implements BaseEnum {

    /**
     * 배치 실행 시작됨
     */
    STARTED("0", "시작됨"),

    /**
     * 배치 정상 종료
     */
    SUCCESS("1", "정상 종료"),

    /*비 정상 종료*/
    ABNORMAL_TERMINATION("9", "비정상 종료"),

    /**
     * 대상 건수 알 수 없음
     */
    UNKNOWN_COUNT("-1", "대상건수 알수없음");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 BatchResRtCode Enum을 찾습니다.
     *
     * @param code DB에 저장된 코드 값 (0, 1, -1, 9)
     * @return 해당하는 BatchResRtCode, null이거나 매칭되지 않으면 null 반환
     */
    public static BatchResRtCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (BatchResRtCode rtCode : BatchResRtCode.values()) {
            if (rtCode.code.equals(code.trim())) {
                return rtCode;
            }
        }
        return null;
    }

    /**
     * 실행 중인 상태인지 확인합니다.
     * <p>STARTED 상태일 때 true</p>
     */
    public boolean isExecuting() {
        return this == STARTED;
    }
}
