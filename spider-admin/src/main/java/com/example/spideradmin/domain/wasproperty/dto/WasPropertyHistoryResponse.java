package com.example.spideradmin.domain.wasproperty.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * WAS 프로퍼티 이력 데이터 응답 DTO
 */
@Getter
@Builder
public class WasPropertyHistoryResponse {
    private String instanceId;
    private String propertyGroupId;
    private String propertyId;
    private Integer version;
    private String propertyValue;
    private String propertyDesc;
    private String reason;
    private String lastUpdateUserId;
    private String lastUpdateDtime;
}
