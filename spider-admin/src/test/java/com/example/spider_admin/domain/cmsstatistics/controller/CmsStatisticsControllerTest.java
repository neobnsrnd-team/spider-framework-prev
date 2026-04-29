package com.example.spider_admin.domain.cmsstatistics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailResponse;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsResponse;
import com.example.spider_admin.domain.cmsstatistics.service.CmsStatisticsService;
import com.example.spider_admin.global.dto.PageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsStatisticsController.class)
@DisplayName("CmsStatisticsController 테스트")
class CmsStatisticsControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsStatisticsService cmsStatisticsService;

    private static final String PAGE_ID = "PAGE-001";
    private static final String LIST_URL = "/api/cms-admin/statistics";
    private static final String DETAIL_URL = "/api/cms-admin/statistics/detail";

    // ─── GET /api/cms-admin/statistics ───────────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[조회] CMS:R 권한으로 통계 목록 조회 시 200과 PageResponse를 반환한다")
    void findStatList_withCmsR_returns200() throws Exception {
        PageResponse<CmsStatisticsResponse> page = PageResponse.of(List.of(buildStatResponse()), 1L, 0, 10);
        given(cmsStatisticsService.findStatList(any(), any())).willReturn(page);

        mockMvc.perform(get(LIST_URL)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-04-16")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[조회] 결과가 없으면 200과 빈 목록을 반환한다")
    void findStatList_empty_returns200() throws Exception {
        given(cmsStatisticsService.findStatList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(LIST_URL).param("startDate", "2026-01-01").param("endDate", "2026-04-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("[인증] 비인증 통계 목록 조회 시 401을 반환한다")
    void findStatList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(LIST_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CMS:R", "CMS:W"})
    @DisplayName("[인가] 파생된 CMS:W 권한으로 통계 목록 조회 시 200을 반환한다")
    void findStatList_withDerivedCmsW_returns200() throws Exception {
        given(cmsStatisticsService.findStatList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(LIST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── GET /api/cms-admin/statistics/detail ────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[상세조회] CMS:R 권한으로 컴포넌트 클릭 상세 조회 시 200을 반환한다")
    void findDetailList_withCmsR_returns200() throws Exception {
        List<CmsStatisticsDetailResponse> data = List.of(buildDetailResponse("BTN-001", 5L));
        given(cmsStatisticsService.findDetailList(any())).willReturn(data);

        mockMvc.perform(get(DETAIL_URL)
                        .param("pageId", PAGE_ID)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-04-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].componentId").value("BTN-001"))
                .andExpect(jsonPath("$.data[0].clickCount").value(5));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[상세조회] 클릭 데이터가 없으면 200과 빈 배열을 반환한다")
    void findDetailList_empty_returns200() throws Exception {
        given(cmsStatisticsService.findDetailList(any())).willReturn(List.of());

        mockMvc.perform(get(DETAIL_URL)
                        .param("pageId", PAGE_ID)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-04-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("[인증] 비인증 상세 조회 시 401을 반환한다")
    void findDetailList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(DETAIL_URL)).andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────

    private CmsStatisticsResponse buildStatResponse() {
        return CmsStatisticsResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .viewCount(50L)
                .clickCount(20L)
                .build();
    }

    private CmsStatisticsDetailResponse buildDetailResponse(String componentId, long clickCount) {
        return CmsStatisticsDetailResponse.builder()
                .componentId(componentId)
                .clickCount(clickCount)
                .build();
    }
}
