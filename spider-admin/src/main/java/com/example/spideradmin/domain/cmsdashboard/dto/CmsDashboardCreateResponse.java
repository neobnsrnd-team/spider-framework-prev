package com.example.spideradmin.domain.cmsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CMS 새 페이지 생성 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsDashboardCreateResponse {

    /** 생성된 페이지 ID */
    private String pageId;

    /** CMS 에디터 URL — {CMS_USER_URL}/cms/edit?bank={pageId} */
    private String editorUrl;
}
