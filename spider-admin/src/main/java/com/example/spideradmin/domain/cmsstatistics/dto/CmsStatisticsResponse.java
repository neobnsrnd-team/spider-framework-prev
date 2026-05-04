package com.example.spideradmin.domain.cmsstatistics.dto;

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

    /**
     * 뷰모드 — 배포 미리보기 래퍼의 디바이스 프레임 폭 결정에 사용.
     */
    private String viewMode;

    /** 최근 정상 배포 URL (배포 이력이 없으면 null) */
    private String deployedUrl;

    /** 조회 수 (EVENT_TYPE='VIEW') */
    private Long viewCount;

    /** 클릭 수 (EVENT_TYPE='CLICK') */
    private Long clickCount;
}
