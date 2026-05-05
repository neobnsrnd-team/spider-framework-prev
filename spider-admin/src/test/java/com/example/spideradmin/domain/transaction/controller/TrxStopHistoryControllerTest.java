package com.example.spideradmin.domain.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spideradmin.domain.transaction.dto.TrxStopHistoryWithTrxNameResponse;
import com.example.spideradmin.domain.transaction.service.TrxStopHistoryService;
import com.example.spideradmin.global.dto.PageResponse;
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

@WebMvcTest(TrxStopHistoryController.class)
@DisplayName("TrxStopHistoryController 테스트")
class TrxStopHistoryControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrxStopHistoryService trxStopHistoryService;

    private static final String BASE_URL = "/api/trx-stop-histories";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_STOP_HISTORY:R")
    @DisplayName("[조회] 검색 조건이 있을 때 HTTP 200과 PageResponse를 반환한다")
    void searchHistories_withCondition_returns200() throws Exception {
        PageResponse<TrxStopHistoryWithTrxNameResponse> page =
                PageResponse.of(List.of(buildResponse("E2E-TRX-001")), 1L, 0, 10);

        given(trxStopHistoryService.searchHistories(any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "10").param("gubunType", "T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "TRX_STOP_HISTORY:R")
    @DisplayName("[조회] 검색 결과가 없을 때 HTTP 200과 빈 목록을 반환한다")
    void searchHistories_emptyResult_returns200() throws Exception {
        PageResponse<TrxStopHistoryWithTrxNameResponse> empty = PageResponse.of(List.of(), 0L, 0, 10);

        given(trxStopHistoryService.searchHistories(any(), any())).willReturn(empty);

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ─── 거래ID별 이력 조회 ────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_STOP_HISTORY:R")
    @DisplayName("[거래ID 조회] 유효한 거래ID로 조회 시 HTTP 200과 배열을 반환한다")
    void getByTrxId_validId_returns200WithList() throws Exception {
        given(trxStopHistoryService.findByTrxId("E2E-TRX-001")).willReturn(List.of(buildResponse("E2E-TRX-001")));

        mockMvc.perform(get(BASE_URL + "/trx/E2E-TRX-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].trxId").value("E2E-TRX-001"));
    }

    // ─── 엑셀 내보내기 ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_STOP_HISTORY:R")
    @DisplayName("[엑셀] 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportExcel_returns200WithXlsxHeader() throws Exception {
        given(trxStopHistoryService.exportExcel(any())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export").param("gubunType", "T"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
    }

    // ─── 인증/인가 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 목록 조회 시 HTTP 401을 반환한다")
    void searchHistories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 비인증 거래ID 조회 시 HTTP 401을 반환한다")
    void getByTrxId_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/trx/ANY")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 비인증 엑셀 내보내기 시 HTTP 401을 반환한다")
    void exportExcel_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxStopHistoryWithTrxNameResponse buildResponse(String trxId) {
        return TrxStopHistoryWithTrxNameResponse.builder()
                .gubunType("T")
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxStopUpdateDtime("20260301090000")
                .trxStopReason("E2E 테스트 중지")
                .trxStopYn("Y")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
