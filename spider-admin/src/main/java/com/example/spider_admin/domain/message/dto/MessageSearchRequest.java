package com.example.spider_admin.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 검색 조건 요청 DTO
 * GET /api/messages/page 요청에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSearchRequest {

    /**
     * 검색 대상 필드 (messageId, messageName, messageType 등)
     */
    private String searchField;

    /**
     * 검색어
     */
    private String searchValue;

    /**
     * 기관 ID 필터
     */
    private String orgIdFilter;

    /**
     * 헤더전문 여부 필터 (Y/N)
     */
    private String headerYnFilter;

    /**
     * 상위전문 ID 필터
     */
    private String parentMessageIdFilter;

    /**
     * 전문 타입 필터
     */
    private String messageTypeFilter;
}
