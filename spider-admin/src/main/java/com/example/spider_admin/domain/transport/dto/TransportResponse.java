package com.example.spider_admin.domain.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportResponse {

    private String orgId;
    private String trxType;
    private String ioType;
    private String reqResType;
    private String gwId;
}
