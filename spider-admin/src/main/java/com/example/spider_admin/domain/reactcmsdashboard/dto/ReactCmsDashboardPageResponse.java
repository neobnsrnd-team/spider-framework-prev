package com.example.spider_admin.domain.reactcmsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS 사용자 대시보드 페이지 응답 DTO (SPW_CMS_PAGE 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactCmsDashboardPageResponse {

    /** 페이지 ID (PAGE_ID) */
    private String pageId;

    /** 페이지명 (PAGE_NAME) */
    private String pageName;

    /** 뷰 모드 (VIEW_MODE: React CMS는 'mobile' 고정) */
    private String viewMode;

    /** 승인 상태 (APPROVE_STATE: WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /**
     * 파일 존재 여부 (Y / N)
     * PAGE_HTML IS NOT NULL AND DBMS_LOB.GETLENGTH(PAGE_HTML) > 0 조건으로 SQL에서 판단
     */
    private String hasFile;

    /** 만료 여부 (Y / N) — EXPIRED_DATE < SYSDATE */
    private String isExpired;

    /** 노출 시작일 (BEGINNING_DATE, YYYY-MM-DD) */
    private String beginningDate;

    /** 노출 종료일 (EXPIRED_DATE, YYYY-MM-DD) */
    private String expiredDate;

    /** 반려 사유 (REJECTED_REASON) */
    private String rejectedReason;

    /** 최종 수정일시 (LAST_MODIFIED_DTIME, YYYY-MM-DD HH24:MI:SS) */
    private String lastModifiedDtime;
}
