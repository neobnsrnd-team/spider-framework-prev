package com.example.spider_admin.domain.transdata.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 실행 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataGenerationRequest {

    private String tranReason;

    private boolean trxOnly;

    @NotEmpty
    @Valid
    private List<TransDataItemRequest> items;
}
