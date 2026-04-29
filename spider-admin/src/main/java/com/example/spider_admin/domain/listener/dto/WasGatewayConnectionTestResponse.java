package com.example.spider_admin.domain.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 연결 테스트 결과 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasGatewayConnectionTestResponse {
    private boolean connected;
    private Long latencyMs;
    private String message;
    private String checkedAt;
    private String targetIp;
    private Integer targetPort;
}
