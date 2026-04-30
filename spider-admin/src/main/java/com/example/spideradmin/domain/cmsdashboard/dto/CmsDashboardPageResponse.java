package com.example.spideradmin.domain.cmsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 사용자 대시보드 페이지 응답 DTO (SPW_CMS_PAGE 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsDashboardPageResponse {

    /** 페이지 ID (PAGE_ID) */
    private String pageId;

    /** 페이지명 (PAGE_NAME) */
    private String pageName;

    /** 뷰 모드 (VIEW_MODE: mobile / web / responsive) */
    private String viewMode;

    /** 승인 상태 (APPROVE_STATE: WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /**
     * 파일 존재 여부 (Y / N)
     * admin은 파일시스템 접근 불가 → PAGE_HTML IS NOT NULL AND PAGE_HTML != '' 조건으로 SQL에서 판단
     * (CMS 원본의 FILE_PATH 파일시스템 폴백은 미지원)
     */
    private String hasFile;

    /** 만료 여부 (Y / N) — EXPIRED_DATE &lt; SYSDATE */
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
