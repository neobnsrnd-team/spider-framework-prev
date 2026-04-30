package com.example.spideradmin.domain.reactcmsadminapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** React CMS Admin 승인 확정 DTO — APPROVE_STATE: PENDING → APPROVED */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminApprovalApproveRequest {

    /** 노출 시작일 (YYYY-MM-DD, 선택) */
    private String beginningDate;

    /** 노출 종료일 (YYYY-MM-DD, 선택) */
    private String expiredDate;
}
