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
/** A/B 테스트 대상 페이지 목록과 현재 실험 상태를 표현하는 응답 DTO. */
public class CmsAbPageResponse {

    private String pageId;
    private String pageName;
    private String viewMode;
    private String approveState;
    private String isPublic;
    private String createUserName;
    private String lastModifiedDtime;
    private String abGroupId;
    private BigDecimal abWeight;
    private long viewCount;
    private long clickCount;
}
