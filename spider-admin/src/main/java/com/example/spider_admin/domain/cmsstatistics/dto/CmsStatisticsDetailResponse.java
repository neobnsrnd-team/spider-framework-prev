package com.example.spider_admin.domain.cmsstatistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 통계 컴포넌트 클릭 상세 응답 DTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsStatisticsDetailResponse {

    /** 컴포넌트 ID */
    private String componentId;

    /** 클릭 수 */
    private Long clickCount;
}
