package com.example.spider_admin.domain.transdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.transdata.service.TransDataService;
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

@WebMvcTest(TransDataController.class)
@DisplayName("TransDataController 테스트")
class TransDataControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransDataService transDataService;

    // ─── 엑셀 내보내기 ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRANS_DATA_EXEC:R")
    @DisplayName("[엑셀] 엑셀 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportTransDataTimes_returns200WithXlsx() throws Exception {
        given(transDataService.exportTransDataTimes(any(), any(), any())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/trans/export").param("userId", "test-user").param("tranResult", "S"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")));
    }

    @Test
    @WithMockUser(authorities = "TRANS_DATA_EXEC:R")
    @DisplayName("[엑셀] 검색 조건 없이 엑셀 내보내기 시에도 HTTP 200을 반환한다")
    void exportTransDataTimes_noParams_returns200() throws Exception {
        given(transDataService.exportTransDataTimes(any(), any(), any())).willReturn(new byte[] {1});

        mockMvc.perform(get("/api/trans/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")));
    }

    // ─── 인증/인가 ─────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void exportTransDataTimes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/trans/export")).andExpect(status().isUnauthorized());
    }
}
