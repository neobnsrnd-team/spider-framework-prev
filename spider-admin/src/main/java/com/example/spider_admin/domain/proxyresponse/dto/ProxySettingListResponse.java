package com.example.spider_admin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 대응답 설정 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxySettingListResponse {

    /**
     * 테스트 일련번호
     */
    private Long testSno;

    /**
     * 테스트명
     */
    private String testName;

    /**
     * 대응답 값
     */
    private String proxyValue;

    /**
     * 기본 대응답 여부 (Y/N)
     */
    private String defaultProxyYn;

    /**
     * 등록자 (사용자 ID)
     */
    private String userId;

    /**
     * 대응답 필드 ID
     */
    private String proxyField;
}
