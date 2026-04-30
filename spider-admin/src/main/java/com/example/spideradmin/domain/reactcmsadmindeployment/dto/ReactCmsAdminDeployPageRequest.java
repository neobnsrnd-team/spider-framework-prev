package com.example.spideradmin.domain.reactcmsadmindeployment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 배포 대상 페이지 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminDeployPageRequest {

    /** 검색어 (페이지명 또는 작성자명) */
    private String search;
}
