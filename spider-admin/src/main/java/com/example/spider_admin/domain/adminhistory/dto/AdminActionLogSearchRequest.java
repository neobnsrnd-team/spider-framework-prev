package com.example.spider_admin.domain.adminhistory.dto;

import lombok.*;

/**
 * 관리자 작업이력 로그 검색 요청 DTO (FWK_USER_ACCESS_HIS 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActionLogSearchRequest {

    /** 사용자ID (USER_ID, LIKE 검색) */
    private String userId;

    /** 접근 IP (ACCESS_IP, LIKE 검색) */
    private String accessIp;

    /** 접근 URL (ACCESS_URL, LIKE 검색) */
    private String accessUrl;

    /** 시작 일시 (ACCESS_DTIME >=, yyyyMMddHHmmss) */
    private String startDate;

    /** 종료 일시 (ACCESS_DTIME <=, yyyyMMddHHmmss) */
    private String endDate;

    /** 정렬 필드 */
    private String sortBy;

    /** 정렬 방향 (ASC, DESC) */
    private String sortDirection;
}
