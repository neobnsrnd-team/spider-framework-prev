package com.example.spider_admin.domain.transaction.dto;

import com.example.spider_admin.domain.trxmessage.dto.TrxMessageResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래 + 전문 목록 Response DTO
 * 거래 상세 조회 시 관련 전문 목록 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxWithMessagesResponse {

    // 거래 기본 정보
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

    // 관련 전문 목록
    private List<TrxMessageResponse> messages;
}
