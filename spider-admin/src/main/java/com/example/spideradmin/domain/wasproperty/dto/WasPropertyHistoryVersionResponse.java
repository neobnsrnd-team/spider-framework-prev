package com.example.spideradmin.domain.wasproperty.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * WAS 프로퍼티 이력 버전 응답 DTO
 */
@Getter
@Builder
public class WasPropertyHistoryVersionResponse {
    private Integer version;
    private String reason;
    private String lastUpdateUserId;
    private String lastUpdateDtime;
}
