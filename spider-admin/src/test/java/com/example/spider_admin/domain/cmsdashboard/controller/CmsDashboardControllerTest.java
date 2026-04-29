package com.example.spider_admin.domain.cmsdashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsdashboard.dto.CmsDashboardPageResponse;
import com.example.spider_admin.domain.cmsdashboard.dto.CmsTemplateResponse;
import com.example.spider_admin.domain.cmsdashboard.service.CmsDashboardService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsDashboardController.class)
@TestPropertySource(properties = {"cms.user-url=http://localhost:3000/"})
@DisplayName("CmsDashboardController 테스트")
class CmsDashboardControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CmsDashboardService cmsDashboardService;

    @Test
    @DisplayName("[템플릿] CMS:R 권한으로 템플릿 목록 조회 시 200과 목록을 반환한다")
    void findTemplateList_withCmsR_returns200() throws Exception {
        given(cmsDashboardService.findTemplateList())
                .willReturn(List.of(
                        CmsTemplateResponse.builder()
                                .pageId("tmpl-1")
                                .pageName("기본 템플릿")
                                .build(),
                        CmsTemplateResponse.builder()
                                .pageId("tmpl-2")
                                .pageName("이벤트 템플릿")
                                .build()));

        mockMvc.perform(get("/api/cms-dashboard/templates").with(user(customUser("cmsUser01", "cms_user", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].pageId").value("tmpl-1"))
                .andExpect(jsonPath("$.data[0].pageName").value("기본 템플릿"));
    }

    @Test
    @DisplayName("[템플릿] 템플릿이 없을 경우 빈 배열을 반환한다")
    void findTemplateList_empty_returnsEmptyList() throws Exception {
        given(cmsDashboardService.findTemplateList()).willReturn(List.of());

        mockMvc.perform(get("/api/cms-dashboard/templates").with(user(customUser("cmsUser01", "cms_user", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("[템플릿] CMS:W 권한으로 템플릿 목록 조회 시 403을 반환한다")
    void findTemplateList_withCmsW_returns403() throws Exception {
        mockMvc.perform(get("/api/cms-dashboard/templates").with(user(customUser("cmsAdmin01", "cms_admin", "CMS:W"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[조회] CMS:R 권한으로 내 페이지 목록 조회 시 200을 반환한다")
    void findMyPageList_withCmsR_returns200() throws Exception {
        PageResponse<CmsDashboardPageResponse> pageResponse = PageResponse.of(
                List.of(CmsDashboardPageResponse.builder().pageId("page-1").build()), 1L, 0, 10);
        given(cmsDashboardService.findMyPageList(any(), eq("cmsUser01"), any())).willReturn(pageResponse);

        mockMvc.perform(get("/api/cms-dashboard/pages").with(user(customUser("cmsUser01", "cms_user", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].pageId").value("page-1"));
    }

    @Test
    @DisplayName("[권한] CMS:W 권한으로 내 페이지 목록 조회 시 403을 반환한다")
    void findMyPageList_withCmsW_returns403() throws Exception {
        mockMvc.perform(get("/api/cms-dashboard/pages").with(user(customUser("cmsAdmin01", "cms_admin", "CMS:W"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[생성] CMS:R 권한으로 새 페이지 생성 시 200을 반환한다")
    void createPage_withCmsR_returns200() throws Exception {
        given(cmsDashboardService.createPage(any(), eq("cmsUser01"), eq("cmsUser01")))
                .willReturn("page-1");

        mockMvc.perform(post("/api/cms-dashboard/pages")
                        .with(csrf())
                        .with(user(customUser("cmsUser01", "cms_user", "CMS:R")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("pageName", "새 페이지", "viewMode", "mobile", "templateId", "blank"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageId").value("page-1"))
                .andExpect(jsonPath("$.data.editorUrl").value("http://localhost:3000/cms/edit?bank=page-1"));
    }

    @Test
    @DisplayName("[권한] CMS:W 권한으로 새 페이지 생성 시 403을 반환한다")
    void createPage_withCmsW_returns403() throws Exception {
        mockMvc.perform(post("/api/cms-dashboard/pages")
                        .with(csrf())
                        .with(user(customUser("cmsAdmin01", "cms_admin", "CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pageName", "새 페이지"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[삭제] CMS:R 권한으로 본인 페이지 삭제 시 200을 반환한다")
    void deletePage_withCmsR_returns200() throws Exception {
        willDoNothing().given(cmsDashboardService).deletePage("page-1", "cmsUser01");

        mockMvc.perform(delete("/api/cms-dashboard/pages/page-1")
                        .with(csrf())
                        .with(user(customUser("cmsUser01", "cms_user", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[승인요청] CMS:R 권한으로 승인 요청 시 200을 반환한다")
    void requestApproval_withCmsR_returns200() throws Exception {
        willDoNothing().given(cmsDashboardService).requestApproval(eq("page-1"), any(), eq("cmsUser01"));

        mockMvc.perform(patch("/api/cms-dashboard/pages/page-1/approve-request")
                        .with(csrf())
                        .with(user(customUser("cmsUser01", "cms_user", "CMS:R")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approverId", "cmsAdmin01",
                                "beginningDate", "2099-04-17",
                                "expiredDate", "2099-04-18"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private CustomUserDetails customUser(String userId, String roleId, String authority) {
        AuthenticatedUser user = new AuthenticatedUser(userId, userId, roleId, "{noop}password", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}
