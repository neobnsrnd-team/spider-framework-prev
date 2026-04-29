package com.example.spider_admin.domain.transdata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 대상 항목 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataItemRequest {

    @NotBlank
    private String tranId;

    @NotBlank
    private String tranName;

    @NotBlank
    private String tranType;
}
