package com.example.spider_admin.domain.messagehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandlerResponse {

    private String orgId;
    private String trxType;
    private String ioType;
    private String operModeType;
    private String handler;
    private String handlerDesc;
    private String stopYn;
}
