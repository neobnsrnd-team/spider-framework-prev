package com.example.spider_admin.domain.cmsapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 승인 관리 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsApprovalListRequest {

    /** 승인 상태 필터 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 검색어 (PAGE_NAME, CREATE_USER_NAME) */
    private String search;

    /** 작성자 ID 필터 */
    private String createUserId;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
