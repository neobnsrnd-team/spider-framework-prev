package com.example.spideradmin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 당발 대응답 테스트 검색 조건 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataSearchRequest {

    /**
     * 기관 ID 필터 (exact match)
     */
    private String orgIdFilter;

    /**
     * 거래 ID 필터 (LIKE)
     */
    private String trxIdFilter;

    /**
     * 테스트명 필터 (LIKE)
     */
    private String testNameFilter;

    /**
     * 등록자 필터 (LIKE)
     */
    private String userIdFilter;
}
