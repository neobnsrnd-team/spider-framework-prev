package com.example.spider_admin.domain.cmsdeployment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployHistoryResponse;
import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployPageResponse;
import com.example.spider_admin.domain.cmsdeployment.service.CmsDeployService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(CmsDeployController.class)
@DisplayName("CmsDeployController 테스트")
class CmsDeployControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CmsDeployService cmsDeployService;

    private static final String PAGE_ID = "PAGE-001";
    private static final String PAGES_URL = "/api/cms-admin/deployments/pages";
    private static final String HISTORY_URL = "/api/cms-admin/deployments";
    private static final String PUSH_URL = "/api/cms-admin/deployments/push";

    // ─── GET /api/cms-admin/deployments/pages ────────────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[조회] CMS:R 권한으로 목록 조회 시 200과 PageResponse를 반환한다")
    void findApprovedPageList_withCmsR_returns200() throws Exception {
        PageResponse<CmsDeployPageResponse> page = PageResponse.of(List.of(buildPageResponse()), 1L, 0, 10);
        given(cmsDeployService.findApprovedPageList(any(), any())).willReturn(page);

        mockMvc.perform(get(PAGES_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[조회] 결과가 없으면 200과 빈 목록을 반환한다")
    void findApprovedPageList_empty_returns200() throws Exception {
        given(cmsDeployService.findApprovedPageList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(PAGES_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("[인증] 비인증 목록 조회 시 401을 반환한다")
    void findApprovedPageList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(PAGES_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CMS:R", "CMS:W"})
    @DisplayName("[인가] 파생된 CMS:W 권한으로 목록 조회 시 200을 반환한다")
    void findApprovedPageList_withDerivedCmsW_returns200() throws Exception {
        given(cmsDeployService.findApprovedPageList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(PAGES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── GET /api/cms-admin/deployments ──────────────────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[이력조회] CMS:R 권한으로 배포 이력 조회 시 200을 반환한다")
    void findHistoryList_withCmsR_returns200() throws Exception {
        PageResponse<CmsDeployHistoryResponse> page = PageResponse.of(List.of(buildHistoryResponse()), 1L, 0, 20);
        given(cmsDeployService.findHistoryList(any(), any())).willReturn(page);

        mockMvc.perform(get(HISTORY_URL)
                        .param("pageId", PAGE_ID)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("[인증] 비인증 이력 조회 시 401을 반환한다")
    void findHistoryList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(HISTORY_URL)).andExpect(status().isUnauthorized());
    }

    // ─── POST /api/cms-admin/deployments/push ────────────────────────

    @Test
    @DisplayName("[배포] CMS:W 권한으로 배포 시 200을 반환한다")
    void push_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsDeployService).push(any(), any());

        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[배포] 승인된 페이지가 없으면 404를 반환한다")
    void push_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsDeployService)
                .push(any(), any());

        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[배포] 서버 오류 발생 시 500을 반환한다")
    void push_serverError_returns500() throws Exception {
        willThrow(new InternalException("배포 서버 오류")).given(cmsDeployService).push(any(), any());

        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("[배포] pageId가 없으면 400을 반환한다")
    void push_pageIdBlank_returns400() throws Exception {
        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 ID는 필수입니다."));
    }

    @Test
    @DisplayName("[인증] 비인증 배포 요청 시 401을 반환한다")
    void push_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[인가] CMS:R 권한으로 배포 요청 시 403을 반환한다")
    void push_withCmsR_returns403() throws Exception {
        mockMvc.perform(post(PUSH_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageId", PAGE_ID))))
                .andExpect(status().isForbidden());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CustomUserDetails customUserDetails(String authority) {
        AuthenticatedUser user = new AuthenticatedUser("test-admin", "테스트 관리자", "ROLE_ADMIN", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }

    private CmsDeployPageResponse buildPageResponse() {
        return CmsDeployPageResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .createUserName("홍길동")
                .deployedUrl("http://133.186.135.23:8080/cms/deployed/" + PAGE_ID + ".html")
                .build();
    }

    private CmsDeployHistoryResponse buildHistoryResponse() {
        return CmsDeployHistoryResponse.builder()
                .instanceId("INST-001")
                .instanceName("배포서버-1")
                .instanceIp("133.186.135.23")
                .instancePort("8080")
                .fileId(PAGE_ID + "_v1.html")
                .fileSize(1024L)
                .fileCrcValue("abc123def456abcd")
                .lastModifierId("admin")
                .lastModifiedDtime("20260416120000")
                .build();
    }
}
