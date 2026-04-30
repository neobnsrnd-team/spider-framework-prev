package com.example.spideradmin.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 헤더 전문 목록 응답 DTO
 * 상위전문ID select box용 (HEADER_YN = 'Y')
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeaderMessageResponse {

    private String messageId;

    private String messageName;
}
