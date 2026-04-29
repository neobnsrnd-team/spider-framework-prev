package com.example.spider_admin.domain.cmsabtest.dto;

import java.util.ArrayList;
import java.util.List;
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
/** 특정 A/B 그룹의 식별자와 소속 페이지 목록을 반환하는 응답 DTO. */
public class CmsAbGroupResponse {

    private String groupId;

    @Builder.Default
    private List<CmsAbGroupPageResponse> pages = new ArrayList<>();
}
