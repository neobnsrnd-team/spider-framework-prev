package com.example.spider_admin.domain.cmsapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 승인 확정 DTO — APPROVE_STATE: PENDING → APPROVED */
@Getter
@Setter
@NoArgsConstructor
public class CmsApproveRequest {

    private String beginningDate;

    private String expiredDate;
}
