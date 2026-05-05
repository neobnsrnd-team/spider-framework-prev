package com.example.spideradmin.domain.datasource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spideradmin.domain.datasource.dto.DataSourceCreateRequest;
import com.example.spideradmin.domain.datasource.dto.DataSourceResponse;
import com.example.spideradmin.domain.datasource.dto.DataSourceUpdateRequest;
import com.example.spideradmin.domain.datasource.service.DataSourceService;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

@WebMvcTest(DataSourceController.class)
@DisplayName("DataSourceController 테스트")
class DataSourceControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DataSourceService dataSourceService;

    private static final String BASE_URL = "/api/datasources";

    // ─── GET /page ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/datasources/page")
    class GetPage {

        @Test
        @WithMockUser(authorities = "DATASOURCE:R")
        @DisplayName("[조회] 인증된 요청 시 200과 PageResponse를 반환해야 한다")
        void getPage_authenticated_returns200() throws Exception {
            PageResponse<DataSourceResponse> page = PageResponse.of(List.of(buildResponse("DS-001")), 1L, 0, 10);
            given(dataSourceService.getDataSourcesWithSearch(any())).willReturn(page);

            mockMvc.perform(get(BASE_URL + "/page").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("[인증] 비인증 요청 시 401 또는 302를 반환해야 한다")
        void getPage_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/page"))
                    .andExpect(status().is(org.hamcrest.Matchers.either(org.hamcrest.Matchers.is(401))
                            .or(org.hamcrest.Matchers.is(302))));
        }

        @Test
        @WithMockUser(authorities = "OTHER:R")
        @DisplayName("[인가] DATASOURCE:R 권한 없으면 403을 반환해야 한다")
        void getPage_noAuthority_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/page")).andExpect(status().isForbidden());
        }
    }

    // ─── GET /{dbId} ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/datasources/{dbId}")
    class GetById {

        @Test
        @WithMockUser(authorities = "DATASOURCE:R")
        @DisplayName("[조회] 존재하는 ID 조회 시 200과 비밀번호 마스킹 Response를 반환해야 한다")
        void getById_exists_returns200WithMaskedPassword() throws Exception {
            DataSourceResponse response = buildResponse("DS-001");
            response.setDbPassword("****");
            given(dataSourceService.getById("DS-001")).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/DS-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dbId").value("DS-001"))
                    .andExpect(jsonPath("$.data.dbPassword").value("****"));
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:R")
        @DisplayName("[조회] 존재하지 않는 ID 조회 시 404를 반환해야 한다")
        void getById_notExists_returns404() throws Exception {
            given(dataSourceService.getById("NONE")).willThrow(new NotFoundException("dbId: NONE"));

            mockMvc.perform(get(BASE_URL + "/NONE")).andExpect(status().isNotFound());
        }
    }

    // ─── GET /export ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/datasources/export")
    class Export {

        @Test
        @WithMockUser(authorities = "DATASOURCE:R")
        @DisplayName("[내보내기] 인증된 요청 시 200과 xlsx Content-Type을 반환해야 한다")
        void export_authenticated_returns200WithXlsx() throws Exception {
            given(dataSourceService.exportDataSources(isNull(), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(new byte[] {0x50, 0x4B, 0x03, 0x04});

            mockMvc.perform(get(BASE_URL + "/export"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String contentType = result.getResponse().getContentType();
                        assert contentType != null
                                && contentType.contains(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    });
        }
    }

    // ─── POST ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/datasources")
    class Create {

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[등록] 유효한 요청 시 201과 생성된 데이터를 반환해야 한다")
        void create_valid_returns201() throws Exception {
            DataSourceCreateRequest dto =
                    DataSourceCreateRequest.builder().dbId("DS-NEW").jndiYn("N").build();
            given(dataSourceService.create(any())).willReturn(buildResponse("DS-NEW"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.dbId").value("DS-NEW"));
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[등록] 중복 DB ID로 등록 시 409를 반환해야 한다")
        void create_duplicate_returns409() throws Exception {
            DataSourceCreateRequest dto =
                    DataSourceCreateRequest.builder().dbId("DS-DUP").jndiYn("N").build();
            given(dataSourceService.create(any())).willThrow(new DuplicateException("dbId: DS-DUP"));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[등록] 필수 필드(dbId) 누락 시 400을 반환해야 한다")
        void create_missingDbId_returns400() throws Exception {
            DataSourceCreateRequest dto = DataSourceCreateRequest.builder().build();

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:R")
        @DisplayName("[인가] DATASOURCE:W 권한 없으면 403을 반환해야 한다")
        void create_noWriteAuthority_returns403() throws Exception {
            DataSourceCreateRequest dto =
                    DataSourceCreateRequest.builder().dbId("DS-NEW").jndiYn("N").build();

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/datasources/{dbId}")
    class Update {

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[수정] 존재하는 ID 수정 시 200과 수정된 데이터를 반환해야 한다")
        void update_exists_returns200() throws Exception {
            DataSourceUpdateRequest dto =
                    DataSourceUpdateRequest.builder().dbName("Updated").build();
            given(dataSourceService.update(anyString(), any())).willReturn(buildResponse("DS-001"));

            mockMvc.perform(put(BASE_URL + "/DS-001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dbId").value("DS-001"));
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 404를 반환해야 한다")
        void update_notExists_returns404() throws Exception {
            DataSourceUpdateRequest dto =
                    DataSourceUpdateRequest.builder().dbName("X").build();
            given(dataSourceService.update(anyString(), any())).willThrow(new NotFoundException("dbId: NONE"));

            mockMvc.perform(put(BASE_URL + "/NONE")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/datasources/{dbId}")
    class DeleteDs {

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[삭제] 존재하는 ID 삭제 시 200을 반환해야 한다")
        void delete_exists_returns200() throws Exception {
            willDoNothing().given(dataSourceService).delete("DS-001");

            mockMvc.perform(delete(BASE_URL + "/DS-001").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(authorities = "DATASOURCE:W")
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        void delete_notExists_returns404() throws Exception {
            willThrow(new NotFoundException("dbId: NONE"))
                    .given(dataSourceService)
                    .delete("NONE");

            mockMvc.perform(delete(BASE_URL + "/NONE").with(csrf())).andExpect(status().isNotFound());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private DataSourceResponse buildResponse(String dbId) {
        return DataSourceResponse.builder()
                .dbId(dbId)
                .dbName("테스트 DB")
                .dbUserId("sa")
                .dbPassword("****")
                .jndiYn("N")
                .lastUpdateDtime("20260101000000")
                .lastUpdateUserId("system")
                .build();
    }
}
