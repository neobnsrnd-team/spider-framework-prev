package com.example.spider_admin.domain.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 상세 응답 DTO (단건 조회용 — 연결 컴포넌트 포함) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceDetailResponse {

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

    /** 연결 컴포넌트 관계 목록 */
    private List<FwkServiceRelationResponse> relations;
}
