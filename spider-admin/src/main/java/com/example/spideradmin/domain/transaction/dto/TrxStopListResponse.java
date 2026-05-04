package com.example.spideradmin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래중지 페이지 목록 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxStopListResponse {

    private String trxId;
    private String trxName;
    private String operModeType;
    private String trxType;
    private String retryTrxYn;
    private String trxStopYn;
    private String trxStopReason;
    private Integer accessUserCount;
}
