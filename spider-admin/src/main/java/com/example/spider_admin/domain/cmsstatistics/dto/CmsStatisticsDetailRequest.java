package com.example.spider_admin.domain.cmsstatistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 통계 컴포넌트 클릭 상세 조회 요청 DTO */
@Getter
@Setter
@NoArgsConstructor
public class CmsStatisticsDetailRequest {

    /** 페이지 ID */
    private String pageId;

    /** 조회 시작일 (YYYY-MM-DD) */
    private String startDate;

    /** 조회 종료일 (YYYY-MM-DD) */
    private String endDate;
}
