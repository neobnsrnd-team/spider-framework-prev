package com.example.spideradmin.domain.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WAS별 Gateway 상태 리스트 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasGatewayStatusResponse {

    private String instanceId;
    private String instanceName;
    private String instanceType;
    private String instanceIp;
    private String instancePort;

    private String gwId;
    private String gwName;
    private String gwProperties;
    private Integer threadCount;
    private String ioType;

    private String wasInstanceStatus;
    private Integer activeCountIdle;
    private String lastUpdateDtime;

    private String systemId;
    private String operModeType;
    private String systemIp;
    private String systemPort;
    private String stopYn;
    private String systemDesc;
    private String appliedWasInstance;
}
