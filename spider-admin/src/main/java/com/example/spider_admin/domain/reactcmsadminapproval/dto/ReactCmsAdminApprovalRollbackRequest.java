package com.example.spider_admin.domain.reactcmsadminapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** React CMS Admin 롤백 DTO — 지정 버전의 이력으로 SPW_CMS_PAGE 복원 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminApprovalRollbackRequest {

    /** 롤백 대상 버전 번호 (SPW_CMS_PAGE_HISTORY.VERSION) */
    private Integer version;
}
