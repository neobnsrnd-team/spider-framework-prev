package com.example.spideradmin.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스-컴포넌트 연결 파라미터 응답 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceRelationParamResponse {

    private Integer paramSeqNo;
    private String paramKey;
    private String paramDesc;
    private String paramValue;
}
