package com.example.spider_admin.domain.cmsabtest.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 그룹 저장 또는 가중치 수정 시 페이지별 노출 비율을 전달하는 DTO. */
public class CmsAbPageWeightRequest {

    private String pageId;

    private BigDecimal weight;
}
