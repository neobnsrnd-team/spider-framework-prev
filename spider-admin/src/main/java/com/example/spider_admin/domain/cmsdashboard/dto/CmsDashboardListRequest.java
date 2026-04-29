package com.example.spider_admin.domain.cmsdashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 사용자 대시보드 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDashboardListRequest {

    /** 승인 상태 필터 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 검색어 (PAGE_NAME) */
    private String search;

    /** 뷰 모드 필터 (mobile / web / responsive) */
    private String viewMode;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
