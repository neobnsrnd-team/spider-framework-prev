package com.example.spider_admin.domain.cmsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CMS 템플릿 목록 응답 DTO (SPW_CMS_PAGE, PAGE_TYPE = 'TEMPLATE')
 *
 * <p>페이지 생성 모달의 템플릿 선택 드롭다운에 사용된다.
 * pageId: 선택 시 createTemplateId hidden input에 저장되는 값 (SPW_CMS_PAGE.PAGE_ID 참조)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsTemplateResponse {

    /** 템플릿 페이지 ID (새 페이지 생성 시 TEMPLATE_ID로 저장되는 값) */
    private String pageId;

    /** 템플릿 이름 (선택 목록에 표시) */
    private String pageName;

    /** 템플릿 레이아웃 (mobile / web / responsive) */
    private String viewMode;
}
