package com.example.spideradmin.domain.service.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 연결 컴포넌트 저장 요청 DTO (replace 전략) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceRelationSaveRequest {

    @Valid
    private List<FwkServiceRelationItemRequest> relations;
}
