package com.example.spider_admin.domain.reactcmsadmindeployment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 배포 이력 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminDeployHistoryRequest {

    /** 페이지 ID 검색 (FILE_ID LIKE '{pageId}_v%.html') */
    private String pageId;
}
