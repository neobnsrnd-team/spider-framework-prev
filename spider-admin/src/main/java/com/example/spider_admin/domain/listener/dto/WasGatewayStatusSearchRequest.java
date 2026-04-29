package com.example.spider_admin.domain.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WAS Gateway 상태 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WasGatewayStatusSearchRequest {

    /**
     * 인스턴스 ID (LIKE 검색)
     */
    private String instanceId;

    /**
     * Gateway ID (LIKE 검색)
     */
    private String gwId;

    /**
     * 운영 모드 구분 (정확히 일치)
     */
    private String operModeType;

    /**
     * 중지 여부 (정확히 일치)
     */
    private String stopYn;
}
