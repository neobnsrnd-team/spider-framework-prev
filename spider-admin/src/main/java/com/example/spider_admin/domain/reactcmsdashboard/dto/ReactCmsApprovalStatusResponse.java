package com.example.spider_admin.domain.reactcmsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** React CMS 페이지 승인 상태 응답 DTO */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReactCmsApprovalStatusResponse {

    /** 승인 상태 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 반려 사유 — REJECTED 상태일 때만 값이 존재 */
    private String rejectedReason;
}
