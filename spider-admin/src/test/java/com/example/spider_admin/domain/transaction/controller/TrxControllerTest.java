package com.example.spider_admin.domain.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.transaction.dto.TrxResponse;
import com.example.spider_admin.domain.transaction.service.TrxService;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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

@WebMvcTest(TrxController.class)
@DisplayName("TrxController 테스트")
class TrxControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrxService trxService;

    private static final String BASE_URL = "/api/trx";

    // ─── 단건 조회 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/trx/{trxId}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "TRX:R")
        @DisplayName("[조회] 존재하는 ID 조회 시 200과 TrxResponse를 반환해야 한다")
        void getById_found_returns200() throws Exception {
            given(trxService.getTrxById("E2E-TRX-001")).willReturn(buildResponse("E2E-TRX-001"));

            mockMvc.perform(get(BASE_URL + "/E2E-TRX-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.trxId").value("E2E-TRX-001"));
        }

        @Test
        @WithMockUser(authorities = "TRX:R")
        @DisplayName("[조회] 존재하지 않는 ID 조회 시 404를 반환해야 한다")
        void getById_notFound_returns404() throws Exception {
            given(trxService.getTrxById("NOT-EXIST")).willThrow(new NotFoundException("trxId: NOT-EXIST"));

            mockMvc.perform(get(BASE_URL + "/NOT-EXIST")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getById_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/E2E-TRX-001")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 등록 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/trx")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[등록] 유효한 요청으로 등록하면 201을 반환해야 한다")
        void create_valid_returns201() throws Exception {
            given(trxService.createTrx(any())).willReturn(buildCreateResponse("TRX-NEW"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[등록] 중복 ID 등록 시 409를 반환해야 한다")
        void create_duplicate_returns409() throws Exception {
            given(trxService.createTrx(any())).willThrow(new DuplicateException("trxId: TRX-DUP"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "TRX:R")
        @DisplayName("[인가] R 권한 사용자 등록 요청 시 403을 반환해야 한다")
        void create_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void create_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 수정 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/trx/{trxId}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[수정] 존재하는 ID 수정 시 200과 TrxResponse를 반환해야 한다")
        void update_found_returns200() throws Exception {
            willReturn(buildResponse("E2E-TRX-001")).given(trxService).updateTrx(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/E2E-TRX-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.trxId").value("E2E-TRX-001"));
        }

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 404를 반환해야 한다")
        void update_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("trxId: NOT-EXIST"))
                    .given(trxService)
                    .updateTrx(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/NOT-EXIST")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void update_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/E2E-TRX-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "TRX:R")
        @DisplayName("[인가] R 권한 사용자 수정 요청 시 403을 반환해야 한다")
        void update_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/E2E-TRX-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void update_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/E2E-TRX-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 삭제 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/trx/{trxId}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[삭제] 존재하는 ID 삭제 시 200을 반환해야 한다")
        void delete_found_returns200() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/E2E-TRX-001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "TRX:W")
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        void delete_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("trxId: NOT-EXIST"))
                    .given(trxService)
                    .deleteTrx("NOT-EXIST");

            mockMvc.perform(delete(BASE_URL + "/NOT-EXIST").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "TRX:R")
        @DisplayName("[인가] R 권한 사용자 삭제 요청 시 403을 반환해야 한다")
        void delete_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/E2E-TRX-001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void delete_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/E2E-TRX-001").with(csrf())).andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxResponse buildResponse(String trxId) {
        return TrxResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxType("A")
                .retryTrxYn("N")
                .maxRetryCount(0)
                .trxStopYn("N")
                .operModeType("O")
                .build();
    }

    private com.example.spider_admin.domain.transaction.dto.TrxCreateResponse buildCreateResponse(String trxId) {
        return com.example.spider_admin.domain.transaction.dto.TrxCreateResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxType("A")
                .retryTrxYn("N")
                .maxRetryCount(0)
                .trxStopYn("N")
                .operModeType("O")
                .build();
    }

    private Map<String, Object> buildCreateBody() {
        return Map.of(
                "trxId", "TRX-NEW",
                "trxType", "A",
                "retryTrxYn", "N",
                "maxRetryCount", 0);
    }

    private Map<String, Object> buildUpdateBody() {
        return Map.of(
                "trxName", "수정된 거래명",
                "trxType", "A",
                "retryTrxYn", "N",
                "maxRetryCount", 3);
    }
}
