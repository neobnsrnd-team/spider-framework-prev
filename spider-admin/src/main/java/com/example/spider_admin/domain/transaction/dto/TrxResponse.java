package com.example.spider_admin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for TRX information
 * Used in GET, POST, PUT operations response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxResponse {

    private String trxId;
    private String operModeType;
    private String trxStopYn;
    private String trxName;
    private String trxDesc;
    private String trxType;
    private String retryTrxYn;
    private Integer maxRetryCount;
    private String retryMiCycle;
    private String bizGroupId;
    private String bizdayTrxYn;
    private String bizdayTrxStartTime;
    private String bizdayTrxEndTime;
    private String saturdayTrxYn;
    private String saturdayTrxStartTime;
    private String saturdayTrxEndTime;
    private String holidayTrxYn;
    private String holidayTrxStartTime;
    private String holidayTrxEndTime;
    private String trxStopReason;
}
