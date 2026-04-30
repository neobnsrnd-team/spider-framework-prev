package com.example.spideradmin.domain.cmsdeployment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 배포 대상 페이지 목록 조회 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDeployPageRequest {

    /** 검색어 (페이지명 또는 작성자명) */
    private String search;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
