package com.example.spider_admin.domain.bizapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.bizapp.dto.BizAppResponse;
import com.example.spider_admin.domain.bizapp.service.BizAppService;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
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

@WebMvcTest(BizAppController.class)
@DisplayName("BizAppController 테스트")
class BizAppControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BizAppService bizAppService;

    private static final String BASE_URL = "/api/biz-apps";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/biz-apps/page")
    class GetPageTests {

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[조회] 검색 조건으로 조회하면 200과 PageResponse를 반환해야 한다")
        void search_withCondition_returns200() throws Exception {
            PageResponse<BizAppResponse> page = PageResponse.of(List.of(buildResponse("APP-001")), 1L, 0, 10);
            given(bizAppService.getBizAppsWithSearch(any())).willReturn(page);

            mockMvc.perform(get(BASE_URL + "/page").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void search_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/page")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 단건 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/biz-apps/{bizAppId}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[조회] 존재하는 ID 조회 시 200과 BizAppResponse를 반환해야 한다")
        void getById_found_returns200() throws Exception {
            given(bizAppService.getById("APP-001")).willReturn(buildResponse("APP-001"));

            mockMvc.perform(get(BASE_URL + "/APP-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bizAppId").value("APP-001"));
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[조회] 존재하지 않는 ID 조회 시 404를 반환해야 한다")
        void getById_notFound_returns404() throws Exception {
            given(bizAppService.getById("NOT-EXIST")).willThrow(new NotFoundException("bizAppId: NOT-EXIST"));

            mockMvc.perform(get(BASE_URL + "/NOT-EXIST")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getById_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/APP-001")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/biz-apps")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[등록] 유효한 요청으로 등록하면 201과 BizAppResponse를 반환해야 한다")
        void create_valid_returns201() throws Exception {
            given(bizAppService.create(any())).willReturn(buildResponse("APP-NEW"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bizAppId").value("APP-NEW"));
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[등록] 중복 ID 등록 시 409를 반환해야 한다")
        void create_duplicate_returns409() throws Exception {
            given(bizAppService.create(any())).willThrow(new DuplicateException("bizAppId: APP-DUP"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[인가] R 권한 사용자 등록 요청 시 403을 반환해야 한다")
        void create_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void create_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bizAppName\":\"이름만있음\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void create_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 수정 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/biz-apps/{bizAppId}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[수정] 존재하는 ID 수정 시 200과 BizAppResponse를 반환해야 한다")
        void update_found_returns200() throws Exception {
            willReturn(buildResponse("APP-001")).given(bizAppService).update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/APP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bizAppId").value("APP-001"));
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 404를 반환해야 한다")
        void update_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("bizAppId: NOT-EXIST"))
                    .given(bizAppService)
                    .update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/NOT-EXIST")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void update_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/APP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[인가] R 권한 사용자 수정 요청 시 403을 반환해야 한다")
        void update_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/APP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void update_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/APP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/biz-apps/{bizAppId}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[삭제] 존재하는 ID 삭제 시 200을 반환해야 한다")
        void delete_found_returns200() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/APP-001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:W")
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        void delete_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("bizAppId: NOT-EXIST"))
                    .given(bizAppService)
                    .delete("NOT-EXIST");

            mockMvc.perform(delete(BASE_URL + "/NOT-EXIST").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "BIZ_APP:R")
        @DisplayName("[인가] R 권한 사용자 삭제 요청 시 403을 반환해야 한다")
        void delete_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/APP-001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void delete_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/APP-001").with(csrf())).andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private BizAppResponse buildResponse(String bizAppId) {
        return BizAppResponse.builder()
                .bizAppId(bizAppId)
                .bizAppName("테스트 App")
                .bizAppDesc("설명")
                .dupCheckYn("Y")
                .queName("QUEUE_01")
                .queNameDisplay("큐 01")
                .logYn("Y")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private Map<String, Object> buildCreateRequest() {
        return Map.of(
                "bizAppId", "APP-NEW",
                "bizAppName", "테스트 App",
                "bizAppDesc", "설명",
                "dupCheckYn", "Y",
                "queName", "QUEUE_01",
                "logYn", "Y");
    }

    private Map<String, Object> buildUpdateRequest() {
        return Map.of(
                "bizAppName", "수정된 App",
                "bizAppDesc", "수정된 설명",
                "dupCheckYn", "N",
                "queName", "QUEUE_02",
                "logYn", "N");
    }
}
