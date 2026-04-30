package com.example.spideradmin.domain.listenertrx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 요청처리 App 맵핑 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMappingResponse {

    private String gwId;
    private String gwName;
    private String reqIdCode;
    private String orgId;
    private String orgName;
    private String trxId;
    private String trxName;
    private String ioType;
    private String bizAppId;
}
