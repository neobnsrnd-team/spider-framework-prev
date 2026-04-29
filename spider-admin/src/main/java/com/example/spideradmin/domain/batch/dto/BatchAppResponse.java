package com.example.spideradmin.domain.batch.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 배치 App 응답 DTO
 */
@Data
@Builder
public class BatchAppResponse {

    private String batchAppId;
    private String batchAppName;
    private String batchAppFileName;
    private String batchAppDesc;
    private String preBatchAppId;
    private String batchCycle;
    private String cronText;
    private String retryableYn;
    private String perWasYn;
    private String importantType;
    private String properties;
    private String trxId;
    private String orgId;
    private String ioType;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    /** FWK_BATCH_APP.SLA_SECONDS — 최대 허용 실행 시간(초), null이면 SLA 미적용 */
    private Integer slaSeconds;
}
