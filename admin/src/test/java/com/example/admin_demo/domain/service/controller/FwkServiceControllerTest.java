package com.example.admin_demo.domain.service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.admin_demo.domain.service.dto.FwkServiceDetailResponse;
import com.example.admin_demo.domain.service.dto.FwkServiceResponse;
import com.example.admin_demo.domain.service.service.FwkServiceService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FwkServiceController.class)
@DisplayName("FwkServiceController 테스트")
class FwkServiceControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FwkServiceService fwkServiceService;

    private static final String BASE_URL = "/api/fwk-services";

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/fwk-services/page")
    class GetPageTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[조회] 검색 조건으로 조회하면 200과 PageResponse를 반환해야 한다")
        void search_withCondition_returns200() throws Exception {
            PageResponse<FwkServiceResponse> page = PageResponse.of(List.of(buildResponse("SVC-001")), 1L, 0, 10);
            given(fwkServiceService.getServicesWithSearch(any())).willReturn(page);

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
    @DisplayName("GET /api/fwk-services/{serviceId}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[조회] 존재하는 ID 조회 시 200과 FwkServiceDetailResponse를 반환해야 한다")
        void getById_found_returns200() throws Exception {
            given(fwkServiceService.getById("SVC-001")).willReturn(buildDetailResponse("SVC-001"));

            mockMvc.perform(get(BASE_URL + "/SVC-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceId").value("SVC-001"))
                    .andExpect(jsonPath("$.data.relations").isArray());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[조회] 존재하지 않는 ID 조회 시 404를 반환해야 한다")
        void getById_notFound_returns404() throws Exception {
            given(fwkServiceService.getById("NOT-EXIST")).willThrow(new NotFoundException("serviceId: NOT-EXIST"));

            mockMvc.perform(get(BASE_URL + "/NOT-EXIST")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void getById_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/SVC-001")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/fwk-services")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[등록] 유효한 요청으로 등록하면 201과 FwkServiceDetailResponse를 반환해야 한다")
        void create_valid_returns201() throws Exception {
            given(fwkServiceService.create(any())).willReturn(buildDetailResponse("SVC-NEW"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceId").value("SVC-NEW"));
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[등록] 중복 ID 등록 시 409를 반환해야 한다")
        void create_duplicate_returns409() throws Exception {
            given(fwkServiceService.create(any())).willThrow(new DuplicateException("serviceId: SVC-DUP"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void create_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"serviceName\":\"이름만있음\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[인가] R 권한 사용자 등록 요청 시 403을 반환해야 한다")
        void create_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isForbidden());
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
    @DisplayName("PUT /api/fwk-services/{serviceId}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[수정] 존재하는 ID 수정 시 200과 FwkServiceDetailResponse를 반환해야 한다")
        void update_found_returns200() throws Exception {
            willReturn(buildDetailResponse("SVC-001")).given(fwkServiceService).update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/SVC-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceId").value("SVC-001"));
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 404를 반환해야 한다")
        void update_notFound_returns404() throws Exception {
            willThrow(new NotFoundException("serviceId: NOT-EXIST"))
                    .given(fwkServiceService)
                    .update(anyString(), any());

            mockMvc.perform(put(BASE_URL + "/NOT-EXIST")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[유효성] 필수 필드 누락 시 400을 반환해야 한다")
        void update_missingRequiredField_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/SVC-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[인가] R 권한 사용자 수정 요청 시 403을 반환해야 한다")
        void update_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/SVC-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void update_unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/SVC-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/fwk-services/{serviceId}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[삭제] 존재하는 ID 삭제 시 200을 반환해야 한다")
        void delete_found_returns200() throws Exception {
            given(fwkServiceService.getById("SVC-001")).willReturn(buildDetailResponse("SVC-001"));

            mockMvc.perform(delete(BASE_URL + "/SVC-001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        void delete_notFound_returns404() throws Exception {
            given(fwkServiceService.getById("NOT-EXIST")).willReturn(buildDetailResponse("NOT-EXIST"));
            willThrow(new NotFoundException("serviceId: NOT-EXIST"))
                    .given(fwkServiceService)
                    .delete(anyString(), anyString());

            mockMvc.perform(delete(BASE_URL + "/NOT-EXIST").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[인가] R 권한 사용자 삭제 요청 시 403을 반환해야 한다")
        void delete_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/SVC-001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void delete_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/SVC-001").with(csrf())).andExpect(status().isUnauthorized());
        }
    }

    // ─── USE_YN 일괄 변경 ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/fwk-services/use-yn/bulk")
    class BulkUpdateUseYnTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[일괄변경] 유효한 요청 시 200을 반환해야 한다")
        void bulkUpdate_valid_returns200() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/use-yn/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"serviceIds\":[\"SVC-001\",\"SVC-002\"],\"useYn\":\"N\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:W")
        @DisplayName("[유효성] serviceIds가 비어있으면 400을 반환해야 한다")
        void bulkUpdate_emptyList_returns400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/use-yn/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"serviceIds\":[],\"useYn\":\"N\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[인가] R 권한 사용자 일괄변경 요청 시 403을 반환해야 한다")
        void bulkUpdate_readOnlyUser_returns403() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/use-yn/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"serviceIds\":[\"SVC-001\"],\"useYn\":\"N\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void bulkUpdate_unauthenticated_returns401() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/use-yn/bulk")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"serviceIds\":[\"SVC-001\"],\"useYn\":\"N\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── Excel 내보내기 ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/fwk-services/export")
    class ExportTests {

        @Test
        @WithMockUser(authorities = "FWK_SERVICE:R")
        @DisplayName("[내보내기] 조회 시 200과 xlsx 파일을 반환해야 한다")
        void export_returns200WithXlsx() throws Exception {
            given(fwkServiceService.exportFwkServices(any())).willReturn(new byte[] {1, 2, 3});

            mockMvc.perform(get(BASE_URL + "/export"))
                    .andExpect(status().isOk())
                    .andExpect(
                            header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401을 반환해야 한다")
        void export_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private FwkServiceResponse buildResponse(String serviceId) {
        return FwkServiceResponse.builder()
                .serviceId(serviceId)
                .serviceName("테스트 서비스")
                .serviceType("B")
                .className("com.example.TestService")
                .methodName("execute")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .lastUpdateDtime("20260317120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private FwkServiceDetailResponse buildDetailResponse(String serviceId) {
        return FwkServiceDetailResponse.builder()
                .serviceId(serviceId)
                .serviceName("테스트 서비스")
                .serviceType("B")
                .className("com.example.TestService")
                .methodName("execute")
                .useYn("Y")
                .lastUpdateDtime("20260317120000")
                .lastUpdateUserId("e2e-admin")
                .relations(List.of())
                .build();
    }

    private Map<String, Object> buildCreateRequest() {
        return Map.of(
                "serviceId", "SVC-NEW",
                "serviceName", "테스트 서비스",
                "useYn", "Y");
    }

    private Map<String, Object> buildUpdateRequest() {
        return Map.of(
                "serviceName", "수정된 서비스",
                "useYn", "Y");
    }
}
