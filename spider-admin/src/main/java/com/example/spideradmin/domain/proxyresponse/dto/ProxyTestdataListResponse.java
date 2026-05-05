package com.example.spideradmin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 당발 대응답 테스트 목록 응답 DTO
 * JOIN된 기관명 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataListResponse {

    /**
     * 테스트 일련번호 (PK)
     */
    private Long testSno;

    /**
     * 기관 ID
     */
    private String orgId;

    /**
     * 기관명 (JOIN)
     */
    private String orgName;

    /**
     * 거래 ID
     */
    private String trxId;

    /**
     * 테스트명
     */
    private String testName;

    /**
     * 테스트설명
     */
    private String testDesc;

    /**
     * 등록자 (사용자 ID)
     */
    private String userId;

    /**
     * 대응답
     */
    private String testData;
}
