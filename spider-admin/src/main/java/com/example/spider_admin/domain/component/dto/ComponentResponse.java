package com.example.spider_admin.domain.component.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 컴포넌트 응답 DTO (목록 조회 시 params=null, 단건 조회 시 params 포함) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentResponse {

    private String componentId;
    private String componentName;
    private String componentDesc;
    private String componentType;
    private String componentClassName;
    private String componentMethodName;
    private String componentCreateType;
    private String bizGroupId;
    private String useYn;
    private String lastUpdateDtime;
    private String lastUpdateUserId;

    /** 단건 조회/등록/수정 응답 시 파라미터 목록 포함, 목록 조회 시 null */
    private List<ComponentParamResponse> params;
}
