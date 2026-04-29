package com.example.spider_admin.domain.cmsabtest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsabtest.dto.CmsAbGroupResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbPageResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbTestDashboardResponse;
import com.example.spider_admin.domain.cmsabtest.service.CmsAbTestService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsAbTestController.class)
@DisplayName("CmsAbTestController tests")
class CmsAbTestControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CmsAbTestService cmsAbTestService;

    private static final String URL = "/api/cms-admin/ab-tests";
    private static final String GROUP_ID = "main-banner";
    private static final String PAGE_ID = "PAGE-001";

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("GET /api/cms-admin/ab-tests requires CMS:R and returns dashboard")
    void findDashboard_withCmsR_returns200() throws Exception {
        given(cmsAbTestService.findDashboard(any(), any()))
                .willReturn(CmsAbTestDashboardResponse.builder()
                        .pages(PageResponse.of(
                                List.of(CmsAbPageResponse.builder()
                                        .pageId(PAGE_ID)
                                        .pageName("Page")
                                        .abGroupId(GROUP_ID)
                                        .abWeight(BigDecimal.valueOf(5))
                                        .build()),
                                1,
                                0,
                                10))
                        .availablePages(List.of(CmsAbPageResponse.builder()
                                .pageId(PAGE_ID)
                                .pageName("Page")
                                .abGroupId(GROUP_ID)
                                .abWeight(BigDecimal.valueOf(5))
                                .build()))
                        .groups(List.of(
                                CmsAbGroupResponse.builder().groupId(GROUP_ID).build()))
                        .build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pages.content[0].pageId").value(PAGE_ID))
                .andExpect(jsonPath("$.data.pages.totalElements").value(1))
                .andExpect(jsonPath("$.data.groups[0].groupId").value(GROUP_ID));
    }

    @Test
    @DisplayName("GET /api/cms-admin/ab-tests rejects unauthenticated users")
    void findDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CMS:R", "CMS:W"})
    @DisplayName("GET /api/cms-admin/ab-tests allows derived CMS write authority")
    void findDashboard_withDerivedCmsW_returns200() throws Exception {
        given(cmsAbTestService.findDashboard(any(), any()))
                .willReturn(CmsAbTestDashboardResponse.builder()
                        .pages(PageResponse.of(List.of(), 0, 0, 10))
                        .availablePages(List.of())
                        .groups(List.of())
                        .build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/cms-admin/ab-tests saves group with CMS:W")
    void saveGroup_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAbTestService).saveGroup(any(), any());

        mockMvc.perform(post(URL)
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("POST /api/cms-admin/ab-tests rejects users without CMS:W")
    void saveGroup_withoutCmsW_returns403() throws Exception {
        mockMvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/cms-admin/ab-tests/{groupId}/weights updates weights")
    void updateWeights_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAbTestService).updateWeights(any(), any(), any());

        mockMvc.perform(patch(URL + "/" + GROUP_ID + "/weights")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("pages", groupBody().get("pages")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/cms-admin/ab-tests/{groupId}/promote promotes winner")
    void promote_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAbTestService).promote(eq(GROUP_ID), eq(PAGE_ID), any());

        mockMvc.perform(post(URL + "/" + GROUP_ID + "/promote")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("winnerPageId", PAGE_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/cms-admin/ab-tests/{groupId}/promote returns 404 for missing group")
    void promote_groupNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("missing")).given(cmsAbTestService).promote(any(), any(), any());

        mockMvc.perform(post(URL + "/" + GROUP_ID + "/promote")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("winnerPageId", PAGE_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/cms-admin/ab-tests?groupId deletes group")
    void clearGroup_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAbTestService).clearGroup(any(), any());

        mockMvc.perform(delete(URL).param("groupId", GROUP_ID).with(csrf()).with(user(customUserDetails("CMS:W"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/cms-admin/ab-tests?pageId clears page")
    void clearPage_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAbTestService).clearPage(any(), any());

        mockMvc.perform(delete(URL).param("pageId", PAGE_ID).with(csrf()).with(user(customUserDetails("CMS:W"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Map<String, Object> groupBody() {
        return Map.of(
                "groupId",
                GROUP_ID,
                "pages",
                List.of(Map.of("pageId", PAGE_ID, "weight", 5), Map.of("pageId", "PAGE-002", "weight", 5)));
    }

    private CustomUserDetails customUserDetails(String authority) {
        AuthenticatedUser user = new AuthenticatedUser("test-admin", "admin", "ROLE_ADMIN", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}
