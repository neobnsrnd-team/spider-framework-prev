package com.example.spider_admin.domain.cmsstatistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 통계 목록 응답 DTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsStatisticsResponse {

    /** 페이지 ID */
    private String pageId;

    /** 페이지명 */
    private String pageName;

    /** 조회 수 (EVENT_TYPE='VIEW') */
    private Long viewCount;

    /** 클릭 수 (EVENT_TYPE='CLICK') */
    private Long clickCount;
}
