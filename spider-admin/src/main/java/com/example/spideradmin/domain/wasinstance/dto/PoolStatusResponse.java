package com.example.spideradmin.domain.wasinstance.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AP 서버 소켓 풀 현황 응답 DTO.
 *
 * <p>spider-link {@code GET /api/internal/pool/status} 응답을 래핑한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolStatusResponse {

    private String instanceId;
    private String instanceName;
    private boolean success;
    private String errorMessage;
    /** key: "host:port", value: 소켓 풀 통계 */
    private Map<String, PoolInfo> pools;

    /** 단일 소켓 풀 통계 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoolInfo {
        private String host;
        private int port;
        private int active;
        private int idle;
        private int total;
        private int maxActive;
    }
}
