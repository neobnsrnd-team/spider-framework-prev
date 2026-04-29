package com.example.spider_admin.domain.cmsapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 노출 기간 수정 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDisplayPeriodRequest {

    /** 노출 시작일 (BEGINNING_DATE, YYYY-MM-DD) */
    private String beginningDate;

    /** 노출 종료일 (EXPIRED_DATE, YYYY-MM-DD) */
    private String expiredDate;
}
