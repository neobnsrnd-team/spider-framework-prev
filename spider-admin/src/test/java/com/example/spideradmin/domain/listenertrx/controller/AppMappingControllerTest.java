package com.example.spideradmin.domain.listenertrx.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.spideradmin.domain.listenertrx.dto.AppMappingResponse;
import com.example.spideradmin.domain.listenertrx.dto.AppMappingUpsertRequest;
import com.example.spideradmin.domain.listenertrx.service.AppMappingService;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(AppMappingController.class)
@DisplayName("AppMappingController 테스트")
class AppMappingControllerTest {

    /**
     * @PreAuthorize 동작을 위한 최소 보안 설정.
     * SecurityConfig 전체를 로드하지 않고 메서드 보안만 활성화한다.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppMappingService appMappingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/interface-mnt/app-mappings";
    private static final String GW_ID = "GW-001";
    private static final String REQ_ID_CODE = "REQ-001";

    // ─── 성공 케이스 ─────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[조회] 목록 조회 시 HTTP 200과 PageResponse를 반환한다")
    void searchMappings_withReadAuthority_returns200() throws Exception {
        PageResponse<AppMappingResponse> page = PageResponse.of(List.of(buildResponse(GW_ID, REQ_ID_CODE)), 1L, 0, 10);

        given(appMappingService.searchMappings(any(), any(), any(), any(), any(), any()))
                .willReturn(page);

        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].gwId").value(GW_ID))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[조회] 단건 조회 시 HTTP 200과 매핑 정보를 반환한다")
    void getMappingDetail_exists_returns200() throws Exception {
        given(appMappingService.getMappingByPk(GW_ID, REQ_ID_CODE)).willReturn(buildResponse(GW_ID, REQ_ID_CODE));

        mockMvc.perform(get(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gwId").value(GW_ID))
                .andExpect(jsonPath("$.data.reqIdCode").value(REQ_ID_CODE));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[생성] 유효한 요청으로 등록 시 HTTP 201을 반환한다")
    void createMapping_validRequest_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[수정] 유효한 요청으로 수정 시 HTTP 200을 반환한다")
    void updateMapping_validRequest_returns200() throws Exception {
        willDoNothing().given(appMappingService).updateMapping(eq(GW_ID), eq(REQ_ID_CODE), any());

        mockMvc.perform(put(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[삭제] 존재하는 매핑 삭제 시 HTTP 200을 반환한다")
    void deleteMapping_exists_returns200() throws Exception {
        willDoNothing().given(appMappingService).deleteMapping(GW_ID, REQ_ID_CODE);

        mockMvc.perform(delete(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[엑셀] 엑셀 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportMappings_returns200WithXlsxContentType() throws Exception {
        given(appMappingService.exportAppMappings(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")));
    }

    // ─── 실패 케이스 — 인증/인가 ─────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void searchMappings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[인가] READ 권한으로 등록 요청 시 HTTP 403을 반환한다")
    void createMapping_withReadOnlyAuthority_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[인가] READ 권한으로 수정 요청 시 HTTP 403을 반환한다")
    void updateMapping_withReadOnlyAuthority_returns403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[인가] READ 권한으로 삭제 요청 시 HTTP 403을 반환한다")
    void deleteMapping_withReadOnlyAuthority_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── 실패 케이스 — 비즈니스 로직 ─────────────────────────────────

    @Test
    @WithMockUser(authorities = "APP_MAPPING:R")
    @DisplayName("[조회] 존재하지 않는 매핑 단건 조회 시 HTTP 404를 반환한다")
    void getMappingDetail_notFound_returns404() throws Exception {
        given(appMappingService.getMappingByPk(GW_ID, REQ_ID_CODE))
                .willThrow(new NotFoundException("gwId=" + GW_ID + ", reqIdCode=" + REQ_ID_CODE));

        mockMvc.perform(get(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[생성] gwId 누락 시 HTTP 400을 반환한다")
    void createMapping_missingGwId_returns400() throws Exception {
        AppMappingUpsertRequest request =
                AppMappingUpsertRequest.builder().reqIdCode(REQ_ID_CODE).build();

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[생성] reqIdCode 누락 시 HTTP 400을 반환한다")
    void createMapping_missingReqIdCode_returns400() throws Exception {
        AppMappingUpsertRequest request =
                AppMappingUpsertRequest.builder().gwId(GW_ID).build();

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[생성] 중복 PK로 등록 시 HTTP 400을 반환한다")
    void createMapping_duplicatePk_returns400() throws Exception {
        willThrow(new InvalidInputException("이미 존재하는 매핑입니다: gwId=" + GW_ID))
                .given(appMappingService)
                .createMapping(any());

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[수정] 존재하지 않는 매핑 수정 시 HTTP 404를 반환한다")
    void updateMapping_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("gwId=" + GW_ID))
                .given(appMappingService)
                .updateMapping(eq(GW_ID), eq(REQ_ID_CODE), any());

        mockMvc.perform(put(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(GW_ID, REQ_ID_CODE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = {"APP_MAPPING:R", "APP_MAPPING:W"})
    @DisplayName("[삭제] 존재하지 않는 매핑 삭제 시 HTTP 404를 반환한다")
    void deleteMapping_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("gwId=" + GW_ID))
                .given(appMappingService)
                .deleteMapping(GW_ID, REQ_ID_CODE);

        mockMvc.perform(delete(BASE_URL + "/{gwId}/{reqIdCode}", GW_ID, REQ_ID_CODE)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────

    private AppMappingResponse buildResponse(String gwId, String reqIdCode) {
        return AppMappingResponse.builder()
                .gwId(gwId)
                .reqIdCode(reqIdCode)
                .gwName("테스트 게이트웨이")
                .build();
    }

    private AppMappingUpsertRequest buildRequest(String gwId, String reqIdCode) {
        return AppMappingUpsertRequest.builder().gwId(gwId).reqIdCode(reqIdCode).build();
    }
}
