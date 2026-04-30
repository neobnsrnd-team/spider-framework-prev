package com.example.spideradmin.domain.wasproperty.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * WAS 인스턴스 간단 응답 DTO (인스턴스 선택용)
 */
@Getter
@Builder
public class WasInstanceSimpleResponse {
    private String instanceId;
    private String instanceName;
}
