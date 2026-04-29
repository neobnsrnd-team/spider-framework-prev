package com.example.spider_admin.domain.gwsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemResponse {

    private String gwId;
    private String systemId;
    private String operModeType;
    private String ip;
    private String port;
    private String stopYn;
    private String systemDesc;
    private String appliedWasInstance;
}
