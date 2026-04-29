package com.example.spider_admin.domain.validation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.validation.dto.ValidationResponse;
import com.example.spider_admin.domain.validation.service.ValidationService;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ValidationController.class)
@DisplayName("ValidationController 테스트")
class ValidationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidationService validationService;

    private static final String BASE_URL = "/api/validations";

    // ─── 페이지 조회 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[조회] 검색 조건이 있으면 HTTP 200과 PageResponse를 반환한다")
    void getValidationsWithPagination_returns200() throws Exception {
        PageResponse<ValidationResponse> page = PageResponse.of(List.of(buildResponse("VLD001")), 1L, 0, 10);
        given(validationService.getValidationsWithSearch(any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/page")
                        .param("page", "1")
                        .param("size", "10")
                        .param("validationId", "VLD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[조회] 조건 없이 조회 시 HTTP 200과 빈 목록을 반환한다")
    void getValidationsWithPagination_noCondition_returns200WithEmpty() throws Exception {
        given(validationService.getValidationsWithSearch(any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(BASE_URL + "/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ─── 단건 조회 ────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[단건] 존재하는 ID 조회 시 HTTP 200과 상세 정보를 반환한다")
    void getById_exists_returns200() throws Exception {
        given(validationService.getById("VLD001")).willReturn(buildResponse("VLD001"));

        mockMvc.perform(get(BASE_URL + "/VLD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.validationId").value("VLD001"));
    }

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[단건] 존재하지 않는 ID 조회 시 HTTP 404를 반환한다")
    void getById_notFound_returns404() throws Exception {
        given(validationService.getById("NO_SUCH")).willThrow(new NotFoundException("validationId: NO_SUCH"));

        mockMvc.perform(get(BASE_URL + "/NO_SUCH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── 엑셀 내보내기 ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[엑셀] 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportValidations_returns200WithXlsxHeader() throws Exception {
        given(validationService.exportValidations(isNull(), isNull(), isNull(), isNull()))
                .willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[등록] 유효한 데이터로 등록 시 HTTP 201을 반환한다")
    void create_validData_returns201() throws Exception {
        given(validationService.create(any())).willReturn(buildResponse("VLD_NEW"));

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validationId\":\"VLD_NEW\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.validationId").value("VLD_NEW"));
    }

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[등록] 중복 ID 등록 시 HTTP 409를 반환한다")
    void create_duplicateId_returns409() throws Exception {
        given(validationService.create(any())).willThrow(new DuplicateException("validationId: VLD001"));

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validationId\":\"VLD001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = "VALIDATION:R")
    @DisplayName("[등록] WRITE 권한 없으면 HTTP 403을 반환한다")
    void create_noWriteAuthority_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validationId\":\"VLD001\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── 수정 ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[수정] 유효한 데이터로 수정 시 HTTP 200을 반환한다")
    void update_validData_returns200() throws Exception {
        given(validationService.update(eq("VLD001"), any())).willReturn(buildResponse("VLD001"));

        mockMvc.perform(put(BASE_URL + "/VLD001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[수정] 존재하지 않는 ID 수정 시 HTTP 404를 반환한다")
    void update_notFound_returns404() throws Exception {
        given(validationService.update(anyString(), any())).willThrow(new NotFoundException("validationId: NO_SUCH"));

        mockMvc.perform(put(BASE_URL + "/NO_SUCH")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[삭제] 존재하는 ID 삭제 시 HTTP 200을 반환한다")
    void delete_exists_returns200() throws Exception {
        willDoNothing().given(validationService).delete("VLD001");

        mockMvc.perform(delete(BASE_URL + "/VLD001").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"VALIDATION:R", "VALIDATION:W"})
    @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 HTTP 404를 반환한다")
    void delete_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("validationId: NO_SUCH"))
                .given(validationService)
                .delete("NO_SUCH");

        mockMvc.perform(delete(BASE_URL + "/NO_SUCH").with(csrf())).andExpect(status().isNotFound());
    }

    // ─── 인증/인가 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void getPage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/page")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 비인증 엑셀 내보내기 요청 시 HTTP 401을 반환한다")
    void export_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export")).andExpect(status().isUnauthorized());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private ValidationResponse buildResponse(String validationId) {
        return ValidationResponse.builder()
                .validationId(validationId)
                .validationDesc("테스트 설명")
                .javaClassName("com.test.Validation")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
