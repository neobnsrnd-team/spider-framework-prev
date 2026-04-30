package com.example.spideradmin.domain.cmsapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 반려 DTO — APPROVE_STATE: PENDING → REJECTED
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsRejectRequest {

    /** 반려 사유 (REJECTED_REASON) */
    private String rejectedReason;
}
