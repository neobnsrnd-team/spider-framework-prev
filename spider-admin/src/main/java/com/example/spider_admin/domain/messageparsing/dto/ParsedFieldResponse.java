package com.example.spider_admin.domain.messageparsing.dto;

import lombok.*;

/**
 * 파싱된 필드 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ParsedFieldResponse {

    /**
     * 메시지 ID (헤더/바디 구분)
     */
    private String messageId;

    /**
     * 필드 ID
     */
    private String fieldId;

    /**
     * 필드명
     */
    private String fieldName;

    /**
     * 원본 값 (파싱 전)
     */
    private String rawValue;

    /**
     * 파싱된 값 (정렬/스케일 처리 후)
     */
    private String parsedValue;

    /**
     * 시작 위치 (바이트 단위)
     */
    private int startPosition;

    /**
     * 데이터 길이 (바이트 단위)
     */
    private long dataLength;

    /**
     * 소수점 자리수
     */
    private Integer scale;

    /**
     * 정렬 방식 (L=Left, R=Right)
     */
    private String align;

    /**
     * 데이터 타입 (C=문자, N=숫자 등)
     */
    private String dataType;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;
}
