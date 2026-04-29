package com.example.spider_admin.domain.cmsabtest.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/** 그룹 상세 조회 시 각 페이지의 가중치와 성과 지표를 보여주기 위한 응답 DTO. */
public class CmsAbGroupPageResponse {

    private String pageId;
    private String pageName;
    private String viewMode;
    private String isPublic;
    private BigDecimal abWeight;
    private long viewCount;
    private long clickCount;
}
