package com.example.spideradmin.global.page.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.spideradmin.domain.board.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PageController.class)
@TestPropertySource(properties = {"cms.user-url=http://localhost:3000/", "cms.preview-url=http://localhost:3000/view"})
@DisplayName("PageController CMS 권한 정책 테스트")
class PageControllerCmsPolicyTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[대시보드] CMS:R 권한으로 /cms/dashboard 접근 시 home 뷰를 반환한다")
    void cmsDashboard_withCmsR_returnsHome() throws Exception {
        mockMvc.perform(get("/cms/dashboard")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[대시보드] CMS:W 단독 권한으로 /cms/dashboard 접근 시 403을 반환한다")
    void cmsDashboard_withCmsWOnly_returns403() throws Exception {
        mockMvc.perform(get("/cms/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"CMS:R", "CMS:W"})
    @DisplayName("[대시보드] CMS:R과 CMS:W를 함께 가진 경우 /cms/dashboard 접근 시 home 뷰를 반환한다")
    void cmsDashboard_withCmsRAndCmsW_returnsHome() throws Exception {
        mockMvc.perform(get("/cms/dashboard")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[관리자] CMS:W 권한으로 /cms-admin/approvals 접근 시 home 뷰를 반환한다")
    void cmsAdminApprovals_withCmsW_returnsHome() throws Exception {
        mockMvc.perform(get("/cms-admin/approvals")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[관리자] CMS:R 권한으로 /cms-admin/approvals 접근 시 403을 반환한다")
    void cmsAdminApprovals_withCmsR_returns403() throws Exception {
        mockMvc.perform(get("/cms-admin/approvals")).andExpect(status().isForbidden());
    }
}
