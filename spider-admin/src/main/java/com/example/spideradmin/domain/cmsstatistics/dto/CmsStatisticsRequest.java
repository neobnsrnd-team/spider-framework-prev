package com.example.spideradmin.domain.cmsstatistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 통계 목록 조회 요청 DTO */
@Getter
@Setter
@NoArgsConstructor
public class CmsStatisticsRequest {

    /** 페이지 ID 필터 (선택) */
    private String pageId;

    /** 조회 시작일 (YYYY-MM-DD) */
    private String startDate;

    /** 조회 종료일 (YYYY-MM-DD) */
    private String endDate;
}
