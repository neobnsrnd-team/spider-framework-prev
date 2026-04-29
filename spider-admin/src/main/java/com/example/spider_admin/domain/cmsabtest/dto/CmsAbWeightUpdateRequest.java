package com.example.spider_admin.domain.cmsabtest.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 기존 A/B 그룹의 페이지 가중치만 수정할 때 사용하는 요청 DTO. */
public class CmsAbWeightUpdateRequest {

    private List<CmsAbPageWeightRequest> pages = new ArrayList<>();
}
