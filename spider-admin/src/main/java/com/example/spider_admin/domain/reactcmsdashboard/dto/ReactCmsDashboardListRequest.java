package com.example.spider_admin.domain.reactcmsdashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS 사용자 대시보드 목록 조회 요청 DTO
 *
 * <p>VIEW_MODE는 React CMS에서 'mobile' 고정이므로 필터 항목에서 제외한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsDashboardListRequest {

    /** 승인 상태 필터 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 검색어 (PAGE_NAME) */
    private String search;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
