package com.example.spideradmin.domain.trxmessage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래-전문 매핑 Response DTO
 * 복합키 (TRX_ID, ORG_ID, IO_TYPE) 모두 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxMessageResponse {

    // 복합 Primary Key
    private String trxId;
    private String orgId;
    private String ioType; // I, O, Q, S

    // 전문 정보
    private String messageId;
    private String stdMessageId;
    private String resMessageId;
    private String stdResMessageId;

    // 설정 정보
    private String proxyResYn;
    private byte[] proxyResData;
    private Integer executeSeq;
    private String proxyResType;
    private String hexLogYn;
    private String multiResYn;
    private String resTypeFieldId;
    private String multiResType;

    // 시스템 필드
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private Integer timeoutSec;
    private String legacyMessageId;

    // JOIN된 전문명 (optional, for display)
    private String messageName;
}
