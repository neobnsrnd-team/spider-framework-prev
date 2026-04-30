package com.example.spideradmin.domain.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스-컴포넌트 연결 항목 저장 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceRelationItemRequest {

    private Integer serviceSeqNo;

    @NotBlank(message = "컴포넌트 ID는 필수입니다")
    private String componentId;

    private List<FwkServiceRelationParamRequest> params;
}
