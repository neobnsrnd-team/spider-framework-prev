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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PageController.class)
@DisplayName("PageController — 서비스관리 라우트 테스트")
class PageControllerServiceMenuTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    // ─── 일반 요청 (home 레이아웃 반환) ──────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("[라우트] /fwk-services 요청 시 home 뷰를 반환해야 한다")
    void fwkServices_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/fwk-services")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /components 요청 시 home 뷰를 반환해야 한다")
    void components_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/components")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /validation 요청 시 home 뷰를 반환해야 한다")
    void validation_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/validation")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /biz-apps 요청 시 home 뷰를 반환해야 한다")
    void bizApps_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/biz-apps")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /sql-queries 요청 시 home 뷰를 반환해야 한다")
    void sqlQueries_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/sql-queries")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /datasources 요청 시 home 뷰를 반환해야 한다")
    void datasources_normalRequest_returnsHome() throws Exception {
        mockMvc.perform(get("/datasources")).andExpect(status().isOk()).andExpect(view().name("home"));
    }

    // ─── 탭 요청 (fragment 반환) ──────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("[라우트] /fwk-services 탭 요청 시 content fragment를 반환해야 한다")
    void fwkServices_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/fwk-services").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/service-manage/service-manage :: content"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /components 탭 요청 시 content fragment를 반환해야 한다")
    void components_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/components").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/component-manage/component-manage :: content"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /validation 탭 요청 시 content fragment를 반환해야 한다")
    void validation_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/validation").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/validation-manage/validation-manage :: content"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /biz-apps 탭 요청 시 content fragment를 반환해야 한다")
    void bizApps_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/biz-apps").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/bizapp-manage/bizapp-manage :: content"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /sql-queries 탭 요청 시 content fragment를 반환해야 한다")
    void sqlQueries_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/sql-queries").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/sqlquery-manage/sqlquery-manage :: content"));
    }

    @Test
    @WithMockUser
    @DisplayName("[라우트] /datasources 탭 요청 시 content fragment를 반환해야 한다")
    void datasources_tabRequest_returnsFragment() throws Exception {
        mockMvc.perform(get("/datasources").header("X-Tab-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/datasource-manage/datasource-manage :: content"));
    }

    // ─── 비인증 요청 ──────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 사용자의 /fwk-services 요청은 인증 오류를 반환해야 한다")
    void fwkServices_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/fwk-services")).andExpect(status().is4xxClientError());
    }
}
