package com.example.spideradmin.domain.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new TRX
 * POST /api/trx
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxCreateRequest {

    @NotBlank(message = "트랜잭션 ID는 필수입니다")
    @Size(max = 40, message = "트랜잭션 ID는 40자 이내여야 합니다")
    private String trxId;

    @Size(max = 1, message = "운영모드타입은 1자 이내여야 합니다")
    private String operModeType;

    @Size(max = 1, message = "트랜잭션중지여부는 1자 이내여야 합니다")
    private String trxStopYn;

    @Size(max = 50, message = "트랜잭션명은 50자 이내여야 합니다")
    private String trxName;

    @Size(max = 200, message = "트랜잭션설명은 200자 이내여야 합니다")
    private String trxDesc;

    @NotBlank(message = "트랜잭션타입은 필수입니다")
    @Size(max = 1, message = "트랜잭션타입은 1자 이내여야 합니다")
    private String trxType;

    @NotBlank(message = "재시도여부는 필수입니다")
    @Size(max = 1, message = "재시도여부는 1자 이내여야 합니다")
    private String retryTrxYn;

    @NotNull(message = "최대재시도횟수는 필수입니다")
    private Integer maxRetryCount;

    @Size(max = 4, message = "재시도주기는 4자 이내여야 합니다")
    private String retryMiCycle;

    @Size(max = 10, message = "업무그룹ID는 10자 이내여야 합니다")
    private String bizGroupId;

    @Size(max = 1, message = "영업일트랜잭션여부는 1자 이내여야 합니다")
    private String bizdayTrxYn;

    @Size(max = 4, message = "영업일트랜잭션시작시간은 4자 이내여야 합니다")
    private String bizdayTrxStartTime;

    @Size(max = 4, message = "영업일트랜잭션종료시간은 4자 이내여야 합니다")
    private String bizdayTrxEndTime;

    @Size(max = 1, message = "토요일트랜잭션여부는 1자 이내여야 합니다")
    private String saturdayTrxYn;

    @Size(max = 4, message = "토요일트랜잭션시작시간은 4자 이내여야 합니다")
    private String saturdayTrxStartTime;

    @Size(max = 4, message = "토요일트랜잭션종료시간은 4자 이내여야 합니다")
    private String saturdayTrxEndTime;

    @Size(max = 1, message = "공휴일트랜잭션여부는 1자 이내여야 합니다")
    private String holidayTrxYn;

    @Size(max = 4, message = "공휴일트랜잭션시작시간은 4자 이내여야 합니다")
    private String holidayTrxStartTime;

    @Size(max = 4, message = "공휴일트랜잭션종료시간은 4자 이내여야 합니다")
    private String holidayTrxEndTime;

    @Size(max = 200, message = "트랜잭션중지사유는 200자 이내여야 합니다")
    private String trxStopReason;
}
