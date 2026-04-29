package com.example.spider_admin.domain.cmsabtest.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 실험 종료 시 우승 페이지를 지정하기 위한 요청 DTO. */
public class CmsAbPromoteRequest {

    private String winnerPageId;
}
