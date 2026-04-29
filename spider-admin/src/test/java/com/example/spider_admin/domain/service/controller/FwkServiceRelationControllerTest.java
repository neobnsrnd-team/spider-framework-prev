package com.example.spider_admin.domain.service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.service.dto.FwkServiceRelationParamResponse;
import com.example.spider_admin.domain.service.dto.FwkServiceRelationResponse;
import com.example.spider_admin.domain.service.service.FwkServiceRelationService;
import com.example.spider_admin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FwkServiceRelationController.class)
@DisplayName("FwkServiceRelationController 테스트")
class FwkServiceRelationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FwkServiceRelationService fwkServiceRelationService;

    private static final String BASE_URL = "/api/fwk-services";

    // ─── 연결 컴포넌트 조회 ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/fwk-services/{serviceId}/relations")
    class GetRelationsTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[조회] 존재하는 서비스의 연결 컴포넌트를 조회하면 200과 목록을 반환해야 한다")
        void getRelations_found_returns200() throws Exception {
            given(fwkServiceRelationService.getRelations("SVC-001"))
                    .willReturn(List.of(buildRelationResponse("SVC-001", 1)));

            mockMvc.perform(get(BASE_URL + "/SVC-001/relations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].serviceId").value("SVC-001"))
                    .andExpect(jsonPath("$.data[0].params").isArray());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[조회] 존재하지 않는 서비스 ID 조회 시 404를 반환해야 한다")
        void getRelations_notFound_returns404() throws Exception {
            given(fwkServiceRelationService.getRelations("NOT-EXIST"))
                    .willThrow(new NotFoundException("serviceId: NOT-EXIST"));

            mockMvc.perform(get(BASE_URL + "/NOT-EXIST/relations")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getRelations_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/SVC-001/relations")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 연결 컴포넌트 저장 ───────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/fwk-services/{serviceId}/relations")
    class SaveRelationsTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[저장] 유효한 요청으로 저장하면 200과 갱신된 목록을 반환해야 한다")
        void saveRelations_valid_returns200() throws Exception {
            given(fwkServiceRelationService.saveRelations(anyString(), any()))
                    .willReturn(List.of(buildRelationResponse("SVC-001", 1)));

            mockMvc.perform(put(BASE_URL + "/SVC-001/relations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    "{\"relations\":[{\"serviceSeqNo\":1,\"componentId\":\"CMP-001\",\"params\":[]}]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[저장] 존재하지 않는 서비스 ID로 저장 시 404를 반환해야 한다")
        void saveRelations_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("serviceId: NOT-EXIST"))
                    .given(fwkServiceRelationService)
                    .saveRelations(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/NOT-EXIST/relations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"relations\":[]}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[인가] R 권한 사용자 저장 요청 시 403을 반환해야 한다")
        void saveRelations_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/SVC-001/relations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"relations\":[]}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void saveRelations_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/SVC-001/relations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"relations\":[]}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private FwkServiceRelationResponse buildRelationResponse(String serviceId, int seqNo) {
        return FwkServiceRelationResponse.builder()
                .serviceId(serviceId)
                .serviceSeqNo(seqNo)
                .componentId("CMP-001")
                .componentName("테스트 컴포넌트")
                .params(List.of(FwkServiceRelationParamResponse.builder()
                        .paramSeqNo(1)
                        .paramKey("KEY_1")
                        .paramDesc("파라미터 1")
                        .paramValue("value1")
                        .build()))
                .build();
    }
}
