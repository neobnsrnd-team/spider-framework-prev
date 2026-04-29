package com.example.spider_admin.domain.article.constant;

import com.example.spider_admin.global.common.enums.YesNo;
import java.time.format.DateTimeFormatter;

/**
 * 게시글 관련 상수 정의
 */
public final class ArticleConstants {

    private ArticleConstants() {
        // 인스턴스화 방지
    }

    /**
     * 날짜/시간 포맷터 (yyyyMMddHHmmss)
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 상단 고정 여부 기본값 (N: 고정 안함)
     */
    public static final YesNo DEFAULT_TOP_YN = YesNo.NO;

    /**
     * 초기 위치값 (답글 순서)
     */
    public static final int INITIAL_POSITION = 0;

    /**
     * 원글의 단계값
     */
    public static final int STEP_ORIGINAL = 0;

    /**
     * 답글의 단계값
     */
    public static final int STEP_REPLY = 1;

    /**
     * 초기 조회수
     */
    public static final int INITIAL_READ_COUNT = 0;

    /**
     * 초기 다운로드 횟수
     */
    public static final int INITIAL_DOWNLOAD_COUNT = 0;

    /**
     * 게시글 내용 최대 글자수
     */
    public static final int MAX_CONTENT_LENGTH = 10000;

    /**
     * 게시글 내용 글자수 초과 에러 메시지
     */
    public static final String CONTENT_SIZE_MESSAGE = "내용은 최대 10,000자까지 입력 가능합니다";
}
