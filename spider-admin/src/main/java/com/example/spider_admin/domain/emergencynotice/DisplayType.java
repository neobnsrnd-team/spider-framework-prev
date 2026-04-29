package com.example.spider_admin.domain.emergencynotice;

/**
 * 긴급공지 노출 타입
 *
 * <p>FWK_PROPERTY 'notice'.USE_YN 행의 DEFAULT_VALUE에 저장된다.
 * Jackson 직렬화/역직렬화 시 enum name() 그대로 사용 ("N" ↔ DisplayType.N).
 */
public enum DisplayType {
    /** 전체 노출 */
    A,
    /** 기업 전용 노출 */
    B,
    /** 개인 전용 노출 */
    C,
    /** 사용안함 */
    N
}
