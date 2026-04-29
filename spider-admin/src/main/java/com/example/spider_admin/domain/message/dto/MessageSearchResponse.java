package com.example.spider_admin.domain.message.dto;

import lombok.*;

/**
 * 전문 검색 결과 DTO
 *
 * <p>전문 검색 API에서 반환되는 전문 정보를 담는 DTO입니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MessageSearchResponse {

    /**
     * 기관 ID
     */
    private String orgId;

    /**
     * 전문 ID
     */
    private String messageId;

    /**
     * 전문명
     */
    private String messageName;

    /**
     * 전문 설명
     */
    private String messageDesc;
}
