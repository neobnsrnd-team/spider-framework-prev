package com.example.spideradmin.domain.component.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 컴포넌트 파라미터 응답 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentParamResponse {

    private String componentId;
    private Integer paramSeqNo;
    private String paramKey;
    private String paramDesc;
    private String defaultParamValue;
}
