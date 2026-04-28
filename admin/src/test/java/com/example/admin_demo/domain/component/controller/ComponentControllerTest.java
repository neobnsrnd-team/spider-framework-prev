package com.example.admin_demo.domain.component.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.admin_demo.domain.component.dto.ComponentParamResponse;
import com.example.admin_demo.domain.component.dto.ComponentResponse;
import com.example.admin_demo.domain.component.service.ComponentService;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.DuplicateException;
import com.example.admin_demo.global.exception.NotFoundException;
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

@WebMvcTest(ComponentController.class)
@DisplayName("ComponentController 테스트")
class ComponentControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ComponentService componentService;

    private static final String BASE_URL = "/api/components";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/components/page")
    class GetPageTests {

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[조회] 검색 조건으로 조회하면 200과 PageResponse를 반환해야 한다")
        void search_withCondition_returns200() throws Exception {
            PageResponse<ComponentResponse> page = PageResponse.of(List.of(buildResponse("CMP-001")), 1L, 0, 10);
            given(componentService.getComponentsWithSearch(any())).willReturn(page);

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
    @DisplayName("GET /api/components/{componentId}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[조회] 존재하는 ID 조회 시 200과 파라미터 포함 ComponentResponse를 반환해야 한다")
        void getById_found_returns200WithParams() throws Exception {
            ComponentResponse response = buildResponseWithParams("CMP-001");
            given(componentService.getById("CMP-001")).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/CMP-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.componentId").value("CMP-001"))
                    .andExpect(jsonPath("$.data.params").isArray());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[조회] 존재하지 않는 ID 조회 시 404를 반환해야 한다")
        void getById_notFound_returns404() throws Exception {
            given(componentService.getById("NOT-EXIST")).willThrow(new NotFoundException("componentId: NOT-EXIST"));

            mockMvc.perform(get(BASE_URL + "/NOT-EXIST")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getById_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/CMP-001")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/components")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[등록] 유효한 요청으로 등록하면 201과 ComponentResponse를 반환해야 한다")
        void create_valid_returns201() throws Exception {
            given(componentService.create(any())).willReturn(buildResponseWithParams("CMP-NEW"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.componentId").value("CMP-NEW"));
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[등록] 중복 ID 등록 시 409를 반환해야 한다")
        void create_duplicate_returns409() throws Exception {
            given(componentService.create(any())).willThrow(new DuplicateException("componentId: CMP-DUP"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[인가] R 권한 사용자 등록 요청 시 403을 반환해야 한다")
        void create_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void create_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"componentName\":\"이름만있음\"}"))
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
    @DisplayName("PUT /api/components/{componentId}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[수정] 존재하는 ID 수정 시 200과 ComponentResponse를 반환해야 한다")
        void update_found_returns200() throws Exception {
            willReturn(buildResponseWithParams("CMP-001"))
                    .given(componentService)
                    .update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/CMP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.componentId").value("CMP-001"));
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 404를 반환해야 한다")
        void update_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("componentId: NOT-EXIST"))
                    .given(componentService)
                    .update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/NOT-EXIST")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void update_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/CMP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[인가] R 권한 사용자 수정 요청 시 403을 반환해야 한다")
        void update_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/CMP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void update_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/CMP-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/components/{componentId}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[삭제] 존재하는 ID 삭제 시 200을 반환해야 한다")
        void delete_found_returns200() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/CMP-001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:W")
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        void delete_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("componentId: NOT-EXIST"))
                    .given(componentService)
                    .delete(anyString(), anyString());

            mockMvc.perform(delete(BASE_URL + "/NOT-EXIST").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "COMPONENT:R")
        @DisplayName("[인가] R 권한 사용자 삭제 요청 시 403을 반환해야 한다")
        void delete_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/CMP-001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void delete_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/CMP-001").with(csrf())).andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private ComponentResponse buildResponse(String componentId) {
        return ComponentResponse.builder()
                .componentId(componentId)
                .componentName("테스트 컴포넌트")
                .componentDesc("설명")
                .componentType("J")
                .componentClassName("com.example.TestComponent")
                .componentMethodName("execute")
                .componentCreateType("A")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .lastUpdateDtime("20260316120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private ComponentResponse buildResponseWithParams(String componentId) {
        return ComponentResponse.builder()
                .componentId(componentId)
                .componentName("테스트 컴포넌트")
                .componentDesc("설명")
                .componentType("J")
                .componentClassName("com.example.TestComponent")
                .componentMethodName("execute")
                .componentCreateType("A")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .lastUpdateDtime("20260316120000")
                .lastUpdateUserId("e2e-admin")
                .params(List.of(ComponentParamResponse.builder()
                        .componentId(componentId)
                        .paramSeqNo(1)
                        .paramKey("KEY_1")
                        .paramDesc("파라미터 1")
                        .defaultParamValue("DEFAULT_1")
                        .build()))
                .build();
    }

    private Map<String, Object> buildCreateRequest() {
        return Map.of(
                "componentId", "CMP-NEW",
                "componentName", "테스트 컴포넌트",
                "componentType", "J",
                "componentClassName", "com.example.TestComponent",
                "componentMethodName", "execute",
                "useYn", "Y");
    }

    private Map<String, Object> buildUpdateRequest() {
        return Map.of(
                "componentName", "수정된 컴포넌트",
                "componentType", "J",
                "componentClassName", "com.example.UpdatedComponent",
                "componentMethodName", "execute",
                "useYn", "Y");
    }
}
