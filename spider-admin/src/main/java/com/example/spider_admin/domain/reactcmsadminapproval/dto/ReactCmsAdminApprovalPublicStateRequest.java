package com.example.spider_admin.domain.reactcmsadminapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** React CMS Admin 공개 상태 변경 DTO */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminApprovalPublicStateRequest {

    /** 공개 여부 (IS_PUBLIC: Y / N) */
    private String isPublic;
}
