package com.example.spideradmin.domain.errorhandle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 핸들러 APP 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandleAppResponse {

    private String handleAppId;
    private String handleAppName;
    private String handleAppDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String sysParamValue;
    private String paramDesc;
    private String handleAppFile;
}
