package com.example.spideradmin.domain.trxmessage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for TrxMessage with Trx information
 * FWK_TRX_MESSAGE와 FWK_TRX를 JOIN한 결과를 담는 Response DTO
 * Used in GET /api/trx/messages/page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxMessageWithTrxResponse {

    // FWK_TRX_MESSAGE fields
    private String trxId;
    private String orgId;
    private String ioType;
    private String messageId;
    private String stdMessageId;
    private String resMessageId;
    private String stdResMessageId;
    private String proxyResYn;
    private String multiResYn;
    private String multiResType;
    private String hexLogYn;
    private Integer timeoutSec;
    private String resTypeFieldId;
    private Integer executeSeq;
    private String proxyResType;
    private String legacyMessageId;
    private String targetServiceUri;

    // FWK_TRX fields (JOIN)
    private String trxName;
    private String trxStopYn;
    private String trxType; // 거래구분 (업무 분류)

    // JOIN된 FWK_MESSAGE 정보
    private String messageName;
    private String stdMessageName;
    private String resMessageName;
    private String stdResMessageName;
}
