package com.example.spider_admin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 대응답 설정 검색 조건 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxySettingSearchRequest {

    /**
     * 기관 ID (exact match, 필수)
     */
    private String orgId;

    /**
     * 거래 ID (exact match, 필수)
     */
    private String trxId;

    /**
     * 테스트 그룹 ID (optional)
     */
    private String testGroupId;

    /**
     * 테스트명 (LIKE, optional)
     */
    private String testName;

    /**
     * 등록자 (LIKE, optional)
     */
    private String userId;
}
