package com.example.admin_demo.domain.cmsabtest.dto;

import com.example.admin_demo.global.dto.PageResponse;
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
/** A/B 테스트 대시보드에 필요한 페이지 목록, 그룹 정보, 선택 후보를 함께 반환하는 응답 DTO. */
public class CmsAbTestDashboardResponse {

    private PageResponse<CmsAbPageResponse> pages;
    private List<CmsAbPageResponse> availablePages;
    private List<CmsAbGroupResponse> groups;
}
