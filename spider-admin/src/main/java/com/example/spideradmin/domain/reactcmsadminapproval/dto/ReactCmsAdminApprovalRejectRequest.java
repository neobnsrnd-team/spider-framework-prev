package com.example.spideradmin.domain.reactcmsadminapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** React CMS Admin 반려 DTO — APPROVE_STATE: PENDING → REJECTED */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminApprovalRejectRequest {

    /** 반려 사유 (REJECTED_REASON) */
    private String rejectedReason;
}
