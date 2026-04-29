package com.example.spider_admin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 거래조회 모달 검색 조건 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataTrxSearchRequest {

    /**
     * 기관 ID (exact match)
     */
    private String orgId;

    /**
     * 거래 ID (LIKE)
     */
    private String trxId;

    /**
     * 거래명 (LIKE)
     */
    private String trxName;
}
