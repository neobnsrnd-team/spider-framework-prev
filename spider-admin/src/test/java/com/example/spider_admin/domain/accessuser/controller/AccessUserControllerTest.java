package com.example.spider_admin.domain.accessuser.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.accessuser.dto.AccessUserResponse;
import com.example.spider_admin.domain.accessuser.service.AccessUserService;
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

@WebMvcTest(AccessUserController.class)
@DisplayName("AccessUserController 테스트")
class AccessUserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessUserService accessUserService;

    private static final String BASE_URL = "/api/access-users";

    // ─── 페이지 조회 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ACCESS_USER:R")
    @DisplayName("[조회] 검색 조건이 있으면 HTTP 200과 PageResponse를 반환한다")
    void getAccessUsersWithPagination_withCondition_returns200() throws Exception {
        PageResponse<AccessUserResponse> page = PageResponse.of(List.of(buildResponse()), 1L, 0, 10);
        given(accessUserService.searchAccessUsersWithPagination(any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/page")
                        .param("page", "1")
                        .param("size", "10")
                        .param("trxId", "TRX001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ACCESS_USER:R")
    @DisplayName("[조회] 검색 조건이 없으면 HTTP 200과 빈 목록을 반환한다")
    void getAccessUsersWithPagination_noCondition_returns200WithEmpty() throws Exception {
        given(accessUserService.searchAccessUsersWithPagination(any(), any()))
                .willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(BASE_URL + "/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ─── 엑셀 내보내기 ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ACCESS_USER:R")
    @DisplayName("[엑셀] 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportAccessUsers_returns200WithXlsxHeader() throws Exception {
        given(accessUserService.exportAccessUsers(isNull(), isNull(), isNull())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
    }

    @Test
    @WithMockUser(authorities = "ACCESS_USER:R")
    @DisplayName("[엑셀] 검색 조건과 함께 내보내기 시 HTTP 200을 반환한다")
    void exportAccessUsers_withParams_returns200() throws Exception {
        given(accessUserService.exportAccessUsers(any(), any(), any())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export")
                        .param("trxId", "TRX001")
                        .param("gubunType", "T")
                        .param("custUserId", "USR001"))
                .andExpect(status().isOk());
    }

    // ─── 인증/인가 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void getPage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/page")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 엑셀 내보내기 비인증 요청 시 HTTP 401을 반환한다")
    void export_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private AccessUserResponse buildResponse() {
        return AccessUserResponse.builder()
                .gubunType("T")
                .trxId("TRX001")
                .custUserId("USR001")
                .useYn("Y")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
