package com.example.spideradmin.domain.reload.dto;

import java.util.List;
import lombok.*;

/**
 * Reload 실행 결과 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReloadResultResponse {

    private String reloadType;
    private List<WasReloadResult> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WasReloadResult {
        private String instanceId;
        private String instanceName;
        private boolean success;
        private String errorMessage;
    }
}
