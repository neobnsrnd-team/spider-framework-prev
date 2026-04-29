package com.example.spider_admin.domain.emergencynotice.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spider_admin.domain.emergencynotice.service.EmergencyNoticeService;
import com.example.spider_admin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

@WebMvcTest(EmergencyNoticeController.class)
@DisplayName("EmergencyNoticeController 테스트")
class EmergencyNoticeControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmergencyNoticeService emergencyNoticeService;

    private static final String BASE_URL = "/api/emergency-notices";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/emergency-notices")
    class GetAllTests {

        @Test
        @WithMockUser(authorities = "EMERGENCY_NOTICE:R")
        @DisplayName("[조회] R 권한으로 조회하면 200과 notices+displayType을 반환해야 한다")
        void getAll_withReadAuth_returns200() throws Exception {
            List<EmergencyNoticeResponse> notices =
                    List.of(buildResponse("EMERGENCY_KO"), buildResponse("EMERGENCY_EN"));
            given(emergencyNoticeService.getAll()).willReturn(notices);
            given(emergencyNoticeService.getDisplayType()).willReturn("N");

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.notices").isArray())
                    .andExpect(jsonPath("$.data.notices.length()").value(2))
                    .andExpect(jsonPath("$.data.displayType").value("N"));
        }

        @Test
        @WithMockUser(authorities = "EMERGENCY_NOTICE:R")
        @DisplayName("[조회] 초기 데이터 없을 시 404를 반환해야 한다")
        void getAll_notFound_returns404() throws Exception {
            given(emergencyNoticeService.getAll()).willThrow(new NotFoundException("긴급공지 초기 데이터 없음"));

            mockMvc.perform(get(BASE_URL)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
        }
    }

    // ─── 일괄 저장 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/emergency-notices")
    class SaveAllTests {

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[저장] R+W 권한으로 유효한 요청 시 200을 반환해야 한다")
        void saveAll_withWriteAuth_returns200() throws Exception {
            // 이전 테스트에서 설정된 stub 이 남아있을 경우를 대비해 명시적으로 void stub 설정
            willDoNothing().given(emergencyNoticeService).saveAll(org.mockito.ArgumentMatchers.any());

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBulkSaveRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[저장] 초기 데이터 없을 시 404를 반환해야 한다")
        void saveAll_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("FWK_PROPERTY 초기 데이터 없음"))
                    .given(emergencyNoticeService)
                    .saveAll(org.mockito.ArgumentMatchers.any());

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBulkSaveRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "EMERGENCY_NOTICE:R")
        @DisplayName("[인가] R 권한만 있는 사용자 저장 요청 시 403을 반환해야 한다")
        void saveAll_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBulkSaveRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[유효성] 유효하지 않은 displayType 입력 시 400을 반환해야 한다")
        void saveAll_invalidDisplayType_returns400() throws Exception {
            Map<String, Object> invalidRequest = Map.of(
                    "notices",
                    List.of(Map.of(
                            "propertyId", "EMERGENCY_KO",
                            "title", "제목",
                            "content", "내용")),
                    "displayType",
                    "X"); // valid: A/B/C/N only

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[유효성] notices가 null이면 400을 반환해야 한다")
        void saveAll_nullNotices_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayType\":\"N\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[유효성] 유효하지 않은 propertyId 입력 시 400을 반환해야 한다")
        void saveAll_invalidPropertyId_returns400() throws Exception {
            Map<String, Object> invalidRequest = Map.of(
                    "notices",
                    List.of(Map.of(
                            "propertyId", "INVALID_ID",
                            "title", "제목",
                            "content", "내용")),
                    "displayType",
                    "N");

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = {"EMERGENCY_NOTICE:R", "EMERGENCY_NOTICE:W"})
        @DisplayName("[유효성] 제목이 공백이면 400을 반환해야 한다")
        void saveAll_blankTitle_returns400() throws Exception {
            Map<String, Object> invalidRequest = Map.of(
                    "notices",
                    List.of(Map.of(
                            "propertyId", "EMERGENCY_KO",
                            "title", "",
                            "content", "내용")),
                    "displayType",
                    "N");

            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void saveAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildBulkSaveRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private EmergencyNoticeResponse buildResponse(String propertyId) {
        return EmergencyNoticeResponse.builder()
                .propertyId(propertyId)
                .title("긴급공지 제목")
                .content("긴급공지 내용")
                .lastUpdateDtime("20260413120000")
                .lastUpdateUserId("admin")
                .build();
    }

    private Map<String, Object> buildBulkSaveRequest() {
        return Map.of(
                "notices",
                List.of(Map.of(
                        "propertyId", "EMERGENCY_KO",
                        "title", "긴급공지 제목",
                        "content", "긴급공지 내용")),
                "displayType",
                "N");
    }
}
