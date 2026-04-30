package com.example.spideradmin.domain.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayResponse {

    private String gwId;
    private String gwName;
    private Integer threadCount;
    private String gwProperties;
    private String gwDesc;
    private String gwAppName;
    private String ioType;
}
