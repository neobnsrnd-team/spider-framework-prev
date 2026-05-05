package com.example.spideradmin.domain.wasinstance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WAS 인스턴스 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasInstanceResponse {

    private String instanceId;

    private String instanceName;

    private String instanceDesc;

    private String wasConfigId;

    private String instanceType;

    private String ip;

    private String port;

    private String operModeType;
}
