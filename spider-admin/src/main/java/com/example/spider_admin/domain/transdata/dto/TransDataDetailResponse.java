package com.example.spider_admin.domain.transdata.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 상세 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataDetailResponse {

    private String tranSeq;

    private String tranResult;

    private String tranResultName;

    private String tranReason;

    private List<TransDataHisResponse> details;
}
