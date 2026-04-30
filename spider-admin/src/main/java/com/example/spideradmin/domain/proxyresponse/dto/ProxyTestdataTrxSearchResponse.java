package com.example.spideradmin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 거래조회 모달용 응답 DTO
 * FWK_TRX_MESSAGE + FWK_ORG + FWK_TRX 조인 결과
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataTrxSearchResponse {

    private String orgId;

    private String orgName;

    private String trxId;

    private String trxName;

    private String ioType;

    private String messageId;
}
