package com.example.spideradmin.domain.cmsdashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 새 페이지 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDashboardCreateRequest {

    /** 페이지명 */
    private String pageName;

    /** 뷰 모드 (mobile / web / responsive) */
    private String viewMode;

    /** 생성 템플릿 ID (blank: 빈 페이지) */
    private String templateId;
}
