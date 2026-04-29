package com.example.spider_admin.domain.cmsabtest.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** A/B 테스트 대시보드 조회 조건을 담는 요청 DTO. */
public class CmsAbTestListRequest {

    private String search;
    private String sortBy;
    private String sortDirection;
}
