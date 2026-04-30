package com.example.spideradmin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 당발 대응답 테스트 상세 응답 DTO
 * 상세 모달에서 사용 (JOIN된 기관명, 거래명 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataDetailResponse {

    private Long testSno;

    private String orgId;

    /** 기관명 (JOIN) */
    private String orgName;

    private String trxId;

    /** 거래명 (JOIN) */
    private String trxName;

    private String messageId;

    private String testName;

    private String testDesc;

    private String testData;

    private String testGroupId;

    private String userId;
}
