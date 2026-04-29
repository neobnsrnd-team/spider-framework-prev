package com.example.spider_admin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 상세 이력 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataHisResponse {

    private Long tranSeq;

    private String tranId;

    private String tranType;

    private String tranTypeName;

    private String tranName;

    private String tranResult;

    private String tranResultName;

    private String tranTime;
}
