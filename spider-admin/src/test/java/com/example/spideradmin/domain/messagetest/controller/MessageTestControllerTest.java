package com.example.spideradmin.domain.messagetest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spideradmin.domain.messageparsing.dto.MessageSimulationRequest;
import com.example.spideradmin.domain.messageparsing.dto.MessageSimulationResponse;
import com.example.spideradmin.domain.messagetest.dto.MessageFieldForTestResponse;
import com.example.spideradmin.domain.messagetest.dto.MessageTestCreateRequest;
import com.example.spideradmin.domain.messagetest.dto.MessageTestResponse;
import com.example.spideradmin.domain.messagetest.dto.MessageTestUpdateRequest;
import com.example.spideradmin.domain.messagetest.service.MessageTestService;
import com.example.spideradmin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spideradmin.domain.wasinstance.service.WasInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(MessageTestController.class)
@DisplayName("MessageTestController 테스트")
class MessageTestControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageTestService messageTestService;

    @MockitoBean
    private WasInstanceService wasInstanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/message-test";

    // ─── GET /api/message-test ───────────────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[목록] 내 테스트 케이스 목록 조회 시 HTTP 200을 반환한다")
    void getMyTestCases_returns200() throws Exception {
        given(messageTestService.getMyTestCases())
                .willReturn(List.of(MessageTestResponse.builder()
                        .testSno(1L)
                        .testName("테스트1")
                        .build()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].testSno").value(1));
    }

    // ─── GET /api/message-test/fields ────────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[필드] 거래 필드 목록 조회 시 HTTP 200을 반환한다")
    void getFieldsForTest_returns200() throws Exception {
        given(messageTestService.getFieldsForTest("TRX001", "I"))
                .willReturn(List.of(MessageFieldForTestResponse.builder()
                        .fieldId("FLD01")
                        .fieldName("거래ID")
                        .dataLength(20L)
                        .build()));

        mockMvc.perform(get(BASE_URL + "/fields").param("trxId", "TRX001").param("ioType", "I"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fieldId").value("FLD01"));
    }

    // ─── GET /api/message-test/instance-ids ──────────────────────

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[인스턴스] 인스턴스 ID 목록 조회 시 HTTP 200을 반환한다")
    void getInstanceIds_returns200() throws Exception {
        given(wasInstanceService.getAllInstances())
                .willReturn(List.of(
                        WasInstanceResponse.builder().instanceId("WAS1").build(),
                        WasInstanceResponse.builder().instanceId("WAS2").build()));

        mockMvc.perform(get(BASE_URL + "/instance-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("WAS1"));
    }

    // ─── GET /api/message-test/by-trx/{trxId} ───────────────────

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[거래별] 거래ID로 테스트 케이스 조회 시 HTTP 200을 반환한다")
    void getTestCasesByTrxId_returns200() throws Exception {
        given(messageTestService.getTestCasesByTrxId("TRX001", null, null, null, null))
                .willReturn(List.of(MessageTestResponse.builder()
                        .testSno(1L)
                        .trxId("TRX001")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/by-trx/{trxId}", "TRX001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].trxId").value("TRX001"));
    }

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[거래별] headerYn 파라미터를 포함하여 조회 시 HTTP 200을 반환한다")
    void getTestCasesByTrxId_withHeaderYn_returns200() throws Exception {
        given(messageTestService.getTestCasesByTrxId("TRX001", "N", null, null, null))
                .willReturn(List.of(MessageTestResponse.builder()
                        .testSno(1L)
                        .trxId("TRX001")
                        .headerYn("N")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/by-trx/{trxId}", "TRX001").param("headerYn", "N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].headerYn").value("N"));
    }

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[거래별] 검색 필터를 포함하여 조회 시 HTTP 200을 반환한다")
    void getTestCasesByTrxId_withFilters_returns200() throws Exception {
        given(messageTestService.getTestCasesByTrxId("TRX001", "N", "로그인", null, "admin"))
                .willReturn(List.of(MessageTestResponse.builder()
                        .testSno(1L)
                        .trxId("TRX001")
                        .testName("로그인테스트")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/by-trx/{trxId}", "TRX001")
                        .param("headerYn", "N")
                        .param("testName", "로그인")
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── POST /api/message-test ──────────────────────────────────

    @Test
    @WithMockUser(authorities = {"TRX_TEST:R", "TRX_TEST:W"})
    @DisplayName("[생성] 유효한 요청으로 생성 시 HTTP 201을 반환한다")
    void createTestCase_validRequest_returns201() throws Exception {
        MessageTestCreateRequest request = MessageTestCreateRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .testName("테스트케이스")
                .build();

        MessageTestResponse response = MessageTestResponse.builder()
                .testSno(1L)
                .orgId("ORG001")
                .messageId("MSG001")
                .testName("테스트케이스")
                .build();

        given(messageTestService.createTestCase(any())).willReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.testSno").value(1));
    }

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[생성] READ 권한으로 생성 요청 시 HTTP 403을 반환한다")
    void createTestCase_readOnly_returns403() throws Exception {
        MessageTestCreateRequest request = MessageTestCreateRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .testName("테스트케이스")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── PUT /api/message-test/{testSno} ─────────────────────────

    @Test
    @WithMockUser(authorities = {"TRX_TEST:R", "TRX_TEST:W"})
    @DisplayName("[수정] 유효한 요청으로 수정 시 HTTP 200을 반환한다")
    void updateTestCase_validRequest_returns200() throws Exception {
        MessageTestUpdateRequest request = MessageTestUpdateRequest.builder()
                .testSno(1L)
                .testName("수정된테스트")
                .build();

        MessageTestResponse response =
                MessageTestResponse.builder().testSno(1L).testName("수정된테스트").build();

        given(messageTestService.updateTestCase(eq(1L), any())).willReturn(response);

        mockMvc.perform(put(BASE_URL + "/{testSno}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[수정] READ 권한으로 수정 요청 시 HTTP 403을 반환한다")
    void updateTestCase_readOnly_returns403() throws Exception {
        MessageTestUpdateRequest request =
                MessageTestUpdateRequest.builder().testSno(1L).build();

        mockMvc.perform(put(BASE_URL + "/{testSno}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/message-test/{testSno} ──────────────────────

    @Test
    @WithMockUser(authorities = {"TRX_TEST:R", "TRX_TEST:W"})
    @DisplayName("[삭제] 삭제 요청 시 HTTP 200을 반환한다")
    void deleteTestCase_returns200() throws Exception {
        willDoNothing().given(messageTestService).deleteTestCase(1L);

        mockMvc.perform(delete(BASE_URL + "/{testSno}", 1L).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[삭제] READ 권한으로 삭제 요청 시 HTTP 403을 반환한다")
    void deleteTestCase_readOnly_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{testSno}", 1L).with(csrf())).andExpect(status().isForbidden());
    }

    // ─── POST /api/message-test/simulate ─────────────────────────

    @Test
    @WithMockUser(authorities = "TRX_TEST:R")
    @DisplayName("[시뮬레이션] 시뮬레이션 실행 시 HTTP 200을 반환한다")
    void runSimulation_returns200() throws Exception {
        MessageSimulationRequest request = MessageSimulationRequest.builder()
                .orgId("ORG001")
                .trxId("TRX001")
                .instanceId("WAS1")
                .fieldData(Map.of("MSG001", Map.of("FLD01", (Object) "값1")))
                .build();

        MessageSimulationResponse response = MessageSimulationResponse.builder()
                .success(true)
                .request(Map.of("orgId", "ORG001"))
                .response(Map.of("result", "OK"))
                .build();

        given(messageTestService.runSimulation(any())).willReturn(response);

        mockMvc.perform(post(BASE_URL + "/simulate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    // ─── 인증 ───────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }
}
