package com.example.spideradmin.domain.reactcmsadminapproval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** React CMS Admin 승인 이력 응답 DTO (SPW_CMS_PAGE_HISTORY 기반) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactCmsAdminApprovalHistoryResponse {

    /** 버전 번호 (VERSION) */
    private Integer version;

    /** 승인 상태 스냅샷 (APPROVE_STATE) */
    private String approveState;

    /** 최종 수정자명 (LAST_MODIFIER_NAME) */
    private String lastModifierName;

    /** 승인 일시 (APPROVE_DATE, YYYY-MM-DD HH24:MI:SS) */
    private String approveDate;
}
