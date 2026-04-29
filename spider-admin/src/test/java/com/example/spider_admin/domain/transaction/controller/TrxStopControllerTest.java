package com.example.spider_admin.domain.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.transaction.dto.TrxStopListResponse;
import com.example.spider_admin.domain.transaction.service.TrxStopService;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrxStopController.class)
@DisplayName("TrxStopController 테스트")
class TrxStopControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrxStopService trxStopService;

    private static final String BASE_URL = "/api/trx-stop";

    // ─── 목록 조회 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/trx-stop/page")
    class SearchPageTests {

        @Test
        @WithMockUser(authorities = "TRX_STOP:R")
        @DisplayName("[조회] 검색 조건으로 조회하면 200과 PageResponse를 반환해야 한다")
        void search_withCondition_returns200() throws Exception {
            PageResponse<TrxStopListResponse> page =
                    PageResponse.of(List.of(buildListResponse("E2E-TRX-001")), 1L, 0, 20);
            given(trxStopService.searchTrxStopList(any(), any())).willReturn(page);

            mockMvc.perform(get(BASE_URL + "/page").param("page", "1").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @WithMockUser(authorities = "TRX_STOP:R")
        @DisplayName("[조회] 검색 결과가 없으면 200과 빈 목록을 반환해야 한다")
        void search_emptyResult_returns200() throws Exception {
            given(trxStopService.searchTrxStopList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 20));

            mockMvc.perform(get(BASE_URL + "/page").param("page", "1").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void search_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/page")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 엑셀 내보내기 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/trx-stop/export")
    class ExportTests {

        @Test
        @WithMockUser(authorities = "TRX_STOP:R")
        @DisplayName("[엑셀] 내보내기 시 200과 xlsx Content-Type을 반환해야 한다")
        void export_returns200WithXlsxHeader() throws Exception {
            given(trxStopService.exportTrxStop(any(), any(), any(), any(), any(), any()))
                    .willReturn(new byte[] {1, 2, 3});

            mockMvc.perform(get(BASE_URL + "/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                    .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void export_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 거래중지 일괄 변경 ────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/trx-stop/batch")
    class BatchUpdateTrxStopTests {

        @Test
        @WithMockUser(authorities = "TRX_STOP:W")
        @DisplayName("[일괄중지] 유효한 요청 시 200을 반환해야 한다")
        void batchUpdate_valid_returns200() throws Exception {
            willDoNothing().given(trxStopService).batchUpdateTrxStop(any());

            mockMvc.perform(put(BASE_URL + "/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBatchStopBody())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = "TRX_STOP:W")
        @DisplayName("[일괄중지] 존재하지 않는 거래ID가 포함된 경우 404를 반환해야 한다")
        void batchUpdate_notFoundTrxId_returns404() throws Exception {
            willThrow(new NotFoundException("trxId: NOT-EXIST"))
                    .given(trxStopService)
                    .batchUpdateTrxStop(any());

            mockMvc.perform(put(BASE_URL + "/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBatchStopBody())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "TRX_STOP:W")
        @DisplayName("[유효성] trxIds가 비어 있으면 400을 반환해야 한다")
        void batchUpdate_emptyTrxIds_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trxIds\":[],\"trxStopYn\":\"Y\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "TRX_STOP:R")
        @DisplayName("[인가] R 권한 사용자 요청 시 403을 반환해야 한다")
        void batchUpdate_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBatchStopBody())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void batchUpdate_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/batch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBatchStopBody())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 운영모드 일괄 변경 ────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/trx-stop/batch-oper-mode")
    class BatchUpdateOperModeTests {

        @Test
        @WithMockUser(authorities = "TRX_STOP:W")
        @DisplayName("[운영모드] 유효한 요청 시 200을 반환해야 한다")
        void batchOperMode_valid_returns200() throws Exception {
            willDoNothing().given(trxStopService).batchUpdateOperMode(any());

            mockMvc.perform(put(BASE_URL + "/batch-oper-mode")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operModeType\":\"D\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = "TRX_STOP:R")
        @DisplayName("[인가] R 권한 사용자 요청 시 403을 반환해야 한다")
        void batchOperMode_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/batch-oper-mode")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operModeType\":\"D\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void batchOperMode_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/batch-oper-mode")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"operModeType\":\"D\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxStopListResponse buildListResponse(String trxId) {
        return TrxStopListResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .operModeType("O")
                .trxType("A")
                .retryTrxYn("N")
                .trxStopYn("N")
                .accessUserCount(0)
                .build();
    }

    private Map<String, Object> buildBatchStopBody() {
        return Map.of("trxIds", List.of("E2E-TRX-001", "E2E-TRX-002"), "trxStopYn", "Y");
    }
}
