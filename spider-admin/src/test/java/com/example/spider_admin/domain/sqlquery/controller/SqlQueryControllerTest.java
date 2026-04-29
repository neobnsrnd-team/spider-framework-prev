package com.example.spider_admin.domain.sqlquery.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.spider_admin.domain.sqlquery.dto.SqlQueryResponse;
import com.example.spider_admin.domain.sqlquery.service.SqlQueryService;
import com.example.spider_admin.global.client.SpiderLinkReloadClient;
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

@WebMvcTest(SqlQueryController.class)
@DisplayName("SqlQueryController 테스트")
class SqlQueryControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SqlQueryService sqlQueryService;

    @MockitoBean
    private SpiderLinkReloadClient spiderLinkReloadClient;

    // ─── GET /api/sql-queries/page ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/sql-queries/page")
    class GetPageTests {

        @Test
        @DisplayName("인증된 사용자가 조회하면 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void withAuth_returns200() throws Exception {
            PageResponse<SqlQueryResponse> page = PageResponse.of(List.of(buildResponse("Q001")), 1L, 0, 10);
            given(sqlQueryService.getSqlQueriesWithSearch(any())).willReturn(page);

            mockMvc.perform(get("/api/sql-queries/page").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].queryId").value("Q001"));
        }

        @Test
        @DisplayName("미인증 사용자가 조회하면 401을 반환해야 한다")
        void withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/sql-queries/page")).andExpect(status().isUnauthorized());
        }
    }

    // ─── GET /api/sql-queries/export ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/sql-queries/export")
    class ExportTests {

        @Test
        @DisplayName("인증된 사용자가 엑셀 내보내기 하면 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void withAuth_returns200() throws Exception {
            given(sqlQueryService.exportExcel(any())).willReturn(new byte[] {0x50, 0x4B});

            mockMvc.perform(get("/api/sql-queries/export"))
                    .andExpect(status().isOk())
                    .andExpect(
                            header().string("Content-Disposition", org.hamcrest.Matchers.containsString("SqlQuery")));
        }

        @Test
        @DisplayName("검색 조건이 적용된 엑셀 내보내기가 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void withSearchParams_returns200() throws Exception {
            given(sqlQueryService.exportExcel(any())).willReturn(new byte[] {0x50, 0x4B});

            mockMvc.perform(get("/api/sql-queries/export")
                            .param("queryId", "Q001")
                            .param("queryName", "테스트")
                            .param("useYn", "Y"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                                    "Content-Type",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @DisplayName("미인증 사용자가 내보내기 하면 401을 반환해야 한다")
        void withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/sql-queries/export")).andExpect(status().isUnauthorized());
        }
    }

    // ─── GET /api/sql-queries/{queryId} ─────────────────────────────

    @Nested
    @DisplayName("GET /api/sql-queries/{queryId}")
    class GetByIdTests {

        @Test
        @DisplayName("존재하는 ID 조회 시 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void exists_returns200() throws Exception {
            given(sqlQueryService.getById("Q001")).willReturn(buildResponse("Q001"));

            mockMvc.perform(get("/api/sql-queries/Q001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.queryId").value("Q001"));
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 404를 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void notExists_returns404() throws Exception {
            given(sqlQueryService.getById("NOT-EXIST")).willThrow(new NotFoundException("queryId: NOT-EXIST"));

            mockMvc.perform(get("/api/sql-queries/NOT-EXIST")).andExpect(status().isNotFound());
        }
    }

    // ─── POST /api/sql-queries ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sql-queries")
    class CreateTests {

        @Test
        @DisplayName("정상 등록 시 201을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void create_success_returns201() throws Exception {
            willReturn(buildResponse("Q-NEW")).given(sqlQueryService).create(any());

            mockMvc.perform(post("/api/sql-queries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody("Q-NEW"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.queryId").value("Q-NEW"));
        }

        @Test
        @DisplayName("중복 ID 등록 시 409를 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void create_duplicate_returns409() throws Exception {
            given(sqlQueryService.create(any())).willThrow(new DuplicateException("queryId: Q-DUP"));

            mockMvc.perform(post("/api/sql-queries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody("Q-DUP"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("필수 필드 누락 시 400을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/sql-queries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"queryName\":\"이름만\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("R 권한만 있으면 403을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void readOnly_returns403() throws Exception {
            mockMvc.perform(post("/api/sql-queries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateBody("Q-NEW"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── PUT /api/sql-queries/{queryId} ─────────────────────────────

    @Nested
    @DisplayName("PUT /api/sql-queries/{queryId}")
    class UpdateTests {

        @Test
        @DisplayName("정상 수정 시 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void success_returns200() throws Exception {
            given(sqlQueryService.update(eq("Q001"), any())).willReturn(buildResponse("Q001"));

            mockMvc.perform(put("/api/sql-queries/Q001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.queryId").value("Q001"));
        }

        @Test
        @DisplayName("존재하지 않는 ID 수정 시 404를 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void notExists_returns404() throws Exception {
            willThrow(new NotFoundException("queryId: NOT-EXIST"))
                    .given(sqlQueryService)
                    .update(eq("NOT-EXIST"), any());

            mockMvc.perform(put("/api/sql-queries/NOT-EXIST")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("R 권한만 있으면 403을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void readOnly_returns403() throws Exception {
            mockMvc.perform(put("/api/sql-queries/Q001")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateBody())))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── DELETE /api/sql-queries/{queryId} ───────────────────────────

    @Nested
    @DisplayName("DELETE /api/sql-queries/{queryId}")
    class DeleteTests {

        @Test
        @DisplayName("정상 삭제 시 200을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void success_returns200() throws Exception {
            willDoNothing().given(sqlQueryService).delete("Q001");

            mockMvc.perform(delete("/api/sql-queries/Q001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않는 ID 삭제 시 404를 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:W")
        void notExists_returns404() throws Exception {
            willThrow(new NotFoundException("queryId: NOT-EXIST"))
                    .given(sqlQueryService)
                    .delete("NOT-EXIST");

            mockMvc.perform(delete("/api/sql-queries/NOT-EXIST").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("R 권한만 있으면 403을 반환해야 한다")
        @WithMockUser(authorities = "SQL_QUERY:R")
        void readOnly_returns403() throws Exception {
            mockMvc.perform(delete("/api/sql-queries/Q001").with(csrf())).andExpect(status().isForbidden());
        }
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private SqlQueryResponse buildResponse(String queryId) {
        return SqlQueryResponse.builder()
                .queryId(queryId)
                .queryName("테스트 쿼리")
                .sqlGroupId("GRP01")
                .sqlGroupName("테스트 그룹")
                .dbId("DB01")
                .dbName("테스트 DB")
                .useYn("Y")
                .lastUpdateDtime("20260317120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private static Map<String, Object> buildCreateBody(String queryId) {
        return Map.of(
                "queryId", queryId,
                "queryName", "테스트 쿼리",
                "sqlGroupId", "GRP01",
                "dbId", "DB01",
                "useYn", "Y",
                "sqlQuery", "SELECT 1 FROM DUAL");
    }

    private static Map<String, Object> buildUpdateBody() {
        return Map.of(
                "queryName", "수정된 쿼리",
                "sqlGroupId", "GRP02",
                "dbId", "DB02",
                "useYn", "N");
    }
}
