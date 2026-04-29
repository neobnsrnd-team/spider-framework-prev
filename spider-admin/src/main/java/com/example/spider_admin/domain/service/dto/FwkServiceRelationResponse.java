package com.example.spider_admin.domain.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스-컴포넌트 연결 응답 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceRelationResponse {

    private String serviceId;
    private Integer serviceSeqNo;
    private String componentId;
    private String componentName;
    private String bizGroupId;

    /** 컴포넌트 파라미터 목록 (FWK_COMPONENT_PARAM + FWK_RELATION_PARAM 조인) */
    private List<FwkServiceRelationParamResponse> params;
}
