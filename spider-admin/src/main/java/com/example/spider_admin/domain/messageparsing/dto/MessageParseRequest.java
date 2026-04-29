package com.example.spider_admin.domain.messageparsing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 전문 파싱 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MessageParseRequest {

    /**
     * 기관 ID
     */
    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    /**
     * 전문 ID
     */
    @NotBlank(message = "전문 ID는 필수입니다")
    private String messageId;

    /**
     * 파싱할 전문 원본 데이터 (고정 길이 문자열)
     */
    @NotBlank(message = "전문 데이터는 필수입니다")
    private String rawString;

    /**
     * 변환 대상 기관 ID (지정 전문 JSON 생성 시 사용, optional)
     */
    private String targetOrgId;

    /**
     * 변환 대상 전문 ID (지정 전문 JSON 생성 시 사용, optional)
     */
    private String targetMessageId;
}
