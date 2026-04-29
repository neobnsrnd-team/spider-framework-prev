package com.example.spider_admin.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 응답 DTO (목록 조회용 — 연결 컴포넌트 미포함) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceResponse {

    private String serviceId;
    private String serviceName;
    private String serviceDesc;
    private String className;
    private String methodName;
    private String serviceType;
    private String bizGroupId;
    private String orgId;
    private String ioType;
    private String workSpaceId;
    private String trxId;
    private String useYn;
    private String preProcessAppId;
    private String postProcessAppId;
    private String timeCheckYn;
    private String startTime;
    private String endTime;
    private String bizdayServiceYn;
    private String bizdayStartTime;
    private String bizdayEndTime;
    private String saturdayServiceYn;
    private String saturdayStartTime;
    private String saturdayEndTime;
    private String holidayServiceYn;
    private String holidayStartTime;
    private String holidayEndTime;
    private String loginOnlyYn;
    private String secureSignYn;
    private String reqChannelCode;
    private String bankStatusCheckYn;
    private String bankCodeField;
    private String lastUpdateDtime;
    private String lastUpdateUserId;

    /** 연결 컴포넌트 수 (목록 조회 시 집계) */
    private Integer componentCount;

    /** 서비스거래 접근 허용자 수 (목록 조회 시 집계) */
    private Integer accessUserCount;
}
