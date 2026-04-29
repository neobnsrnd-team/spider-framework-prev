package com.example.spider_admin.domain.transaction.dto;

import lombok.*;

/**
 * 거래중지이력 응답 DTO (거래명 포함)
 * FWK_TRX와 조인하여 거래명을 포함한 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxStopHistoryWithTrxNameResponse {

    /**
     * 구분유형 (T: 거래, S: 서비스)
     */
    private String gubunType;

    /**
     * 거래ID
     */
    private String trxId;

    /**
     * 거래명 (FWK_TRX 조인)
     */
    private String trxName;

    /**
     * 거래중지시간 (yyyyMMddHHmmss)
     */
    private String trxStopUpdateDtime;

    /**
     * 거래중지사유
     */
    private String trxStopReason;

    /**
     * 거래중지여부 (Y/N)
     */
    private String trxStopYn;

    /**
     * 최종수정자
     */
    private String lastUpdateUserId;
}
