package com.example.spider_admin.domain.reactcmsadminapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 승인 관리 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminApprovalListRequest {

    /** 승인 상태 필터 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 검색어 (PAGE_NAME, CREATE_USER_NAME) */
    private String search;

    /** 정렬 기준 컬럼명 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
