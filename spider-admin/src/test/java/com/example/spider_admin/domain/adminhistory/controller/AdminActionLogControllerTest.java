package com.example.spider_admin.domain.adminhistory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.adminhistory.dto.AdminActionLogResponse;
import com.example.spider_admin.domain.adminhistory.service.AdminActionLogService;
import com.example.spider_admin.global.dto.PageResponse;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminActionLogController.class)
@DisplayName("AdminActionLogController 테스트")
class AdminActionLogControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminActionLogService adminActionLogService;

    private static final String BASE_URL = "/api/admin-action-logs";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "USER_ACCESS_HIS:R")
    @DisplayName("[조회] 검색 조건 있을 때 HTTP 200과 PageResponse를 반환한다")
    void searchLogs_withCondition_returns200() throws Exception {
        PageResponse<AdminActionLogResponse> page = PageResponse.of(List.of(buildResponse()), 1L, 0, 10);

        given(adminActionLogService.searchLogs(any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "10").param("userId", "e2e-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "USER_ACCESS_HIS:R")
    @DisplayName("[조회] 검색 조건 없을 때 HTTP 200과 빈 목록을 반환한다")
    void searchLogs_noCondition_returns200WithEmpty() throws Exception {
        PageResponse<AdminActionLogResponse> empty = PageResponse.of(List.of(), 0L, 0, 10);

        given(adminActionLogService.searchLogs(any(), any())).willReturn(empty);

        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(authorities = "USER_ACCESS_HIS:R")
    @DisplayName("[조회] page 파라미터 기본값(1)이 적용된다")
    void searchLogs_defaultPage_usesPage1() throws Exception {
        given(adminActionLogService.searchLogs(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
    }

    // ─── 엑셀 내보내기 ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "USER_ACCESS_HIS:R")
    @DisplayName("[엑셀] 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportLogs_returns200WithXlsxHeader() throws Exception {
        given(adminActionLogService.exportLogs(any())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export").param("userId", "TEST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
    }

    // ─── 인증/인가 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void searchLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 엑셀 내보내기 비인증 요청 시 HTTP 401을 반환한다")
    void exportLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private AdminActionLogResponse buildResponse() {
        return AdminActionLogResponse.builder()
                .userId("e2e-admin")
                .accessDtime("20260312120000")
                .accessIp("127.0.0.1")
                .accessUrl("[GET] /api/users")
                .resultMessage("정상처리")
                .build();
    }
}
