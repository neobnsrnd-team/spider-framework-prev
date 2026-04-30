package com.example.spideradmin.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스-컴포넌트 연결 파라미터 저장 요청 항목 (배치 INSERT 용 — serviceSeqNo·componentId 포함) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceRelationParamRequest {

    private Integer serviceSeqNo;
    private String componentId;
    private Integer paramSeqNo;
    private String paramValue;
}
