package com.example.spider_admin.domain.batch.constant;

import java.time.format.DateTimeFormatter;

/**
 * 배치 관련 상수 정의
 */
public final class BatchConstants {

    private BatchConstants() {
        // 인스턴스화 방지
    }

    /**
     * 날짜/시간 포맷터 (yyyyMMddHHmmss)
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 로그 날짜/시간 포맷터 (yyyyMMddHHmmssSSS) - 밀리초 포함
     */
    public static final DateTimeFormatter LOG_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
}
