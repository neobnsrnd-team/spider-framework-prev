package com.example.spider_admin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 실행 이력 목록용 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataTimesResponse {

    private Long tranSeq;

    private String userId;

    private String tranTime;

    private String tranResult;

    private String tranResultName;

    private String tranReason;

    private Long successCount;

    private Long failCount;

    private Long totalCount;
}
